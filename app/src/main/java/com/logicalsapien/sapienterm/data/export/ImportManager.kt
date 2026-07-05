/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2025 SapienTerm contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logicalsapien.sapienterm.data.export

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.logicalsapien.sapienterm.data.ConnectionGroupRepository
import com.logicalsapien.sapienterm.data.CredentialRepository
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.ProfileRepository
import com.logicalsapien.sapienterm.data.QuickCommandRepository
import com.logicalsapien.sapienterm.data.entity.ConnectionGroup
import com.logicalsapien.sapienterm.data.entity.CredentialType
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.PortForward
import com.logicalsapien.sapienterm.data.entity.Profile
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Import mode determines how imported data interacts with existing data.
 */
enum class ImportMode {
    /** Keep existing data, skip duplicates. */
    MERGE,

    /** Delete all existing data before importing. */
    REPLACE
}

/**
 * Result of an import operation with counts of imported items.
 */
data class ImportResult(
    val connectionsImported: Int = 0,
    val quickCommandsImported: Int = 0,
    val credentialsImported: Int = 0,
    val groupsImported: Int = 0,
    val profilesImported: Int = 0,
    val preferencesRestored: Boolean = false
)

/**
 * Manages importing SapienTerm backup data from `.sapienterm` (encrypted) or
 * `.json` (plain) files.
 *
 * Handles file type detection, decryption, JSON parsing, duplicate detection,
 * and re-encryption of credentials via Android Keystore.
 */
class ImportManager(
    private val context: Context,
    private val hostRepository: HostRepository,
    private val quickCommandRepository: QuickCommandRepository,
    private val credentialRepository: CredentialRepository,
    private val connectionGroupRepository: ConnectionGroupRepository,
    private val profileRepository: ProfileRepository,
    private val prefs: SharedPreferences,
    private val dispatchers: CoroutineDispatchers
) {

    /**
     * Import data from a backup file.
     *
     * @param uri URI of the file to import
     * @param passphrase Passphrase for decrypting `.sapienterm` files (null for plain JSON)
     * @param mode Import mode (MERGE or REPLACE)
     * @return Counts of imported items
     * @throws ImportException if import fails
     * @throws ExportCryptoException if decryption fails (wrong passphrase)
     */
    suspend fun importData(
        uri: Uri,
        passphrase: String?,
        mode: ImportMode
    ): ImportResult = withContext(dispatchers.io) {
        try {
            val exportData = parseFile(uri, passphrase)
            performImport(exportData, mode)
        } catch (e: ExportCryptoException) {
            throw e
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Import failed")
            throw ImportException("Failed to import data", e)
        }
    }

    /**
     * Preview the contents of a backup file without importing.
     *
     * @param uri URI of the file to preview
     * @param passphrase Passphrase for decrypting `.sapienterm` files
     * @return Parsed export data for preview
     * @throws ImportException if parsing fails
     * @throws ExportCryptoException if decryption fails
     */
    suspend fun previewImport(
        uri: Uri,
        passphrase: String?
    ): ExportData = withContext(dispatchers.io) {
        try {
            parseFile(uri, passphrase)
        } catch (e: ExportCryptoException) {
            throw e
        } catch (e: ImportException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Preview failed")
            throw ImportException("Failed to read backup file", e)
        }
    }

    /**
     * Import data from a local [File] (e.g. auto-backup) without going through ContentResolver.
     */
    suspend fun importFromFile(
        file: File,
        mode: ImportMode
    ): ImportResult = withContext(dispatchers.io) {
        try {
            val jsonString = file.readText(Charsets.UTF_8)
            val json = JSONObject(jsonString)
            val exportData = ExportData.fromJson(json)
            performImport(exportData, mode)
        } catch (e: Exception) {
            Timber.e(e, "Import from file failed")
            throw ImportException("Failed to import data", e)
        }
    }

    /**
     * Read and parse a backup file, handling encryption detection.
     */
    private fun parseFile(uri: Uri, passphrase: String?): ExportData {
        val fileBytes = readFileBytes(uri)

        val isEncrypted = isEncryptedFile(uri)

        val jsonString = if (isEncrypted) {
            if (passphrase.isNullOrEmpty()) {
                throw ImportException("Passphrase is required for encrypted backup files")
            }
            val decryptedBytes = CryptoUtils.decrypt(fileBytes, passphrase)
            String(decryptedBytes, Charsets.UTF_8)
        } else {
            // Try parsing as plain JSON first
            val text = String(fileBytes, Charsets.UTF_8)
            try {
                JSONObject(text)
                text
            } catch (e: Exception) {
                // Not valid JSON -- may be encrypted without proper extension
                if (!passphrase.isNullOrEmpty()) {
                    try {
                        val decryptedBytes = CryptoUtils.decrypt(fileBytes, passphrase)
                        String(decryptedBytes, Charsets.UTF_8)
                    } catch (e2: Exception) {
                        throw ImportException("File is not a valid backup file", e)
                    }
                } else {
                    throw ImportException("File is not valid JSON. If encrypted, provide a passphrase.", e)
                }
            }
        }

        val json = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            throw ImportException("Invalid backup file format", e)
        }

        val exportData = ExportData.fromJson(json)

        // Validate basic structure
        if (exportData.app != "SapienTerm") {
            Timber.w("Import file app identifier: ${exportData.app}")
        }

        return exportData
    }

    /**
     * Perform the actual import based on mode.
     * Order matters: groups and credentials are imported first so that connections
     * can be linked to them by name/label.
     */
    private suspend fun performImport(exportData: ExportData, mode: ImportMode): ImportResult {
        if (mode == ImportMode.REPLACE) {
            deleteExistingData(exportData)
        }

        val profilesImported = importProfiles(exportData.profiles, mode)
        val groupsImported = importGroups(exportData.groups, mode)
        val credentialsImported = importCredentials(exportData.credentials, mode)
        val connectionsImported = importConnections(exportData.connections, mode)
        val quickCommandsImported = importQuickCommands(exportData.quickCommands, mode)
        val preferencesRestored = importPreferences(exportData.preferences)

        return ImportResult(
            connectionsImported = connectionsImported,
            quickCommandsImported = quickCommandsImported,
            credentialsImported = credentialsImported,
            groupsImported = groupsImported,
            profilesImported = profilesImported,
            preferencesRestored = preferencesRestored
        )
    }

    /**
     * Delete existing data that overlaps with the import data.
     * Only deletes data types that are present in the import.
     */
    private suspend fun deleteExistingData(exportData: ExportData) {
        if (exportData.connections != null) {
            hostRepository.deleteAllHosts()
        }
        if (exportData.groups != null) {
            connectionGroupRepository.deleteAll()
        }
        if (exportData.quickCommands != null) {
            quickCommandRepository.deleteAll()
        }
        if (exportData.credentials != null) {
            credentialRepository.deleteAll()
        }
        if (exportData.profiles != null) {
            profileRepository.getAll().forEach { profileRepository.delete(it.id) }
        }
    }

    /**
     * Import connection groups, skipping duplicates in MERGE mode.
     * Duplicates are detected by name.
     */
    private suspend fun importGroups(
        groups: List<ExportConnectionGroup>?,
        mode: ImportMode
    ): Int {
        if (groups.isNullOrEmpty()) return 0

        val existingNames = if (mode == ImportMode.MERGE) {
            connectionGroupRepository.getAll().map { it.name }.toSet()
        } else {
            emptySet()
        }

        var count = 0
        for (group in groups) {
            if (mode == ImportMode.MERGE && group.name in existingNames) {
                Timber.d("Skipping duplicate group: ${group.name}")
                continue
            }

            try {
                connectionGroupRepository.add(
                    ConnectionGroup(
                        name = group.name,
                        color = group.color,
                        sortOrder = group.sortOrder
                    )
                )
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to import group: ${group.name}")
            }
        }
        return count
    }

    /**
     * Import connections, skipping duplicates in MERGE mode.
     * Duplicates are detected by hostname + port + username.
     * Links connections to groups (by name) and credentials (by label) when available.
     */
    private suspend fun importConnections(
        connections: List<ExportConnection>?,
        mode: ImportMode
    ): Int {
        if (connections.isNullOrEmpty()) return 0

        val existingHosts = if (mode == ImportMode.MERGE) {
            hostRepository.getHosts().map { Triple(it.hostname, it.port, it.username) }.toSet()
        } else {
            emptySet()
        }

        val groupIdByName = connectionGroupRepository.getAll()
            .associateBy({ it.name }, { it.id })
        val credentialIdByLabel = credentialRepository.getAll()
            .associateBy({ it.label }, { it.id })
        val profileIdByName = profileRepository.getAll()
            .associateBy({ it.name }, { it.id })

        var count = 0
        for (conn in connections) {
            val key = Triple(conn.hostname, conn.port, conn.username)
            if (mode == ImportMode.MERGE && key in existingHosts) {
                Timber.d("Skipping duplicate connection: ${conn.name}")
                continue
            }

            val host = Host(
                nickname = generateUniqueNickname(conn.name, mode),
                hostname = conn.hostname,
                port = conn.port,
                username = conn.username,
                protocol = conn.protocol,
                color = conn.color,
                useKeys = conn.useKeys,
                useAuthAgent = conn.useAuthAgent,
                postLogin = conn.postLogin,
                wantSession = conn.wantSession,
                compression = conn.compression,
                stayConnected = conn.stayConnected,
                quickDisconnect = conn.quickDisconnect,
                scrollbackLines = conn.scrollbackLines,
                useCtrlAltAsMetaKey = conn.useCtrlAltAsMetaKey,
                ipVersion = conn.ipVersion,
                groupId = conn.groupName?.let { groupIdByName[it] },
                credentialId = conn.credentialLabel?.let { credentialIdByLabel[it] },
                pinned = conn.pinned,
                profileId = conn.profileName?.let { profileIdByName[it] }
            )
            try {
                val savedHost = hostRepository.saveHost(host)
                conn.portForwards.forEach { pf ->
                    try {
                        hostRepository.savePortForward(
                            PortForward(
                                hostId = savedHost.id,
                                nickname = pf.nickname,
                                type = pf.type,
                                sourcePort = pf.sourcePort,
                                destAddr = pf.destAddr,
                                destPort = pf.destPort
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to import port forward '${pf.nickname}' for ${conn.name}")
                    }
                }
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to import connection: ${conn.name}")
            }
        }
        return count
    }

    /**
     * Import terminal profiles, skipping duplicates in MERGE mode.
     * Duplicates are detected by name.
     */
    private suspend fun importProfiles(
        profiles: List<ExportProfile>?,
        mode: ImportMode
    ): Int {
        if (profiles.isNullOrEmpty()) return 0

        val existingNames = if (mode == ImportMode.MERGE) {
            profileRepository.getAll().map { it.name }.toSet()
        } else {
            emptySet()
        }

        var count = 0
        for (profile in profiles) {
            if (profile.name == "Default") continue  // never overwrite the built-in default
            if (mode == ImportMode.MERGE && profile.name in existingNames) {
                Timber.d("Skipping duplicate profile: ${profile.name}")
                continue
            }
            try {
                profileRepository.save(
                    Profile(
                        name = profile.name,
                        iconColor = profile.iconColor,
                        fontFamily = profile.fontFamily,
                        fontSize = profile.fontSize,
                        delKey = profile.delKey,
                        encoding = profile.encoding,
                        emulation = profile.emulation,
                        forceSizeRows = profile.forceSizeRows,
                        forceSizeColumns = profile.forceSizeColumns
                    )
                )
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to import profile: ${profile.name}")
            }
        }
        return count
    }

    /**
     * Restore exported app preferences. Only keys in [ExportPreferences.EXPORTABLE_KEYS] are applied.
     */
    private fun importPreferences(preferences: ExportPreferences?): Boolean {
        if (preferences == null) return false
        val editor = prefs.edit()
        var anyApplied = false
        preferences.values
            .filter { (key, _) -> key in ExportPreferences.EXPORTABLE_KEYS }
            .forEach { (key, value) ->
                editor.putString(key, value)
                anyApplied = true
            }
        if (anyApplied) editor.apply()
        return anyApplied
    }

    /**
     * Generate a unique nickname for a host, appending a suffix if needed.
     */
    private suspend fun generateUniqueNickname(baseName: String, mode: ImportMode): String {
        if (mode == ImportMode.REPLACE) return baseName

        val existingHosts = hostRepository.getHosts()
        val existingNames = existingHosts.map { it.nickname }.toSet()

        if (baseName !in existingNames) return baseName

        var counter = 2
        while ("$baseName ($counter)" in existingNames) {
            counter++
        }
        return "$baseName ($counter)"
    }

    /**
     * Import quick commands, skipping duplicates in MERGE mode.
     * Duplicates are detected by title + command.
     */
    private suspend fun importQuickCommands(
        quickCommands: List<ExportQuickCommand>?,
        mode: ImportMode
    ): Int {
        if (quickCommands.isNullOrEmpty()) return 0

        val existingCommands = if (mode == ImportMode.MERGE) {
            quickCommandRepository.getAll().map { Pair(it.title, it.command) }.toSet()
        } else {
            emptySet()
        }

        var count = 0
        for (cmd in quickCommands) {
            val key = Pair(cmd.title, cmd.command)
            if (mode == ImportMode.MERGE && key in existingCommands) {
                Timber.d("Skipping duplicate quick command: ${cmd.title}")
                continue
            }

            val quickCommand = QuickCommand(
                title = cmd.title,
                command = cmd.command,
                category = cmd.category,
                sortOrder = cmd.sortOrder
            )
            try {
                quickCommandRepository.add(quickCommand)
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to import quick command: ${cmd.title}")
            }
        }
        return count
    }

    /**
     * Import credentials, skipping duplicates in MERGE mode.
     * Duplicates are detected by label + type.
     * Sensitive fields are re-encrypted with the device's Android Keystore.
     */
    private suspend fun importCredentials(
        credentials: List<ExportCredential>?,
        mode: ImportMode
    ): Int {
        if (credentials.isNullOrEmpty()) return 0

        val existingCredentials = if (mode == ImportMode.MERGE) {
            credentialRepository.getAll().map { Pair(it.label, it.type.name) }.toSet()
        } else {
            emptySet()
        }

        var count = 0
        for (cred in credentials) {
            val key = Pair(cred.label, cred.type)
            if (mode == ImportMode.MERGE && key in existingCredentials) {
                Timber.d("Skipping duplicate credential: ${cred.label}")
                continue
            }

            try {
                val credType = try {
                    CredentialType.valueOf(cred.type)
                } catch (e: IllegalArgumentException) {
                    Timber.w("Unknown credential type: ${cred.type}, skipping")
                    continue
                }

                when (credType) {
                    CredentialType.PASSWORD -> {
                        val password = cred.password
                        if (password != null) {
                            credentialRepository.savePassword(cred.label, password)
                            count++
                        } else {
                            Timber.w("Password credential without password: ${cred.label}")
                        }
                    }

                    CredentialType.SSH_KEY -> {
                        val privateKeyBytes = cred.privateKey?.let {
                            android.util.Base64.decode(it, android.util.Base64.NO_WRAP)
                        }
                        if (privateKeyBytes != null) {
                            credentialRepository.saveKey(
                                label = cred.label,
                                privateKey = privateKeyBytes,
                                publicKey = cred.publicKey ?: "",
                                passphrase = cred.passphrase
                            )
                            count++
                        } else {
                            Timber.w("SSH key credential without private key: ${cred.label}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to import credential: ${cred.label}")
            }
        }
        return count
    }

    /**
     * Read all bytes from a content URI.
     */
    private fun readFileBytes(uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes()
        } ?: throw ImportException("Cannot read file from URI: $uri")
    }

    /**
     * Detect whether the file is encrypted based on its extension.
     * Falls back to trying JSON parse if extension is unknown.
     */
    private fun isEncryptedFile(uri: Uri): Boolean {
        val path = uri.lastPathSegment ?: uri.path ?: ""
        return path.endsWith(".sapienterm", ignoreCase = true)
    }
}

/**
 * Exception thrown when import operations fail.
 */
class ImportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
