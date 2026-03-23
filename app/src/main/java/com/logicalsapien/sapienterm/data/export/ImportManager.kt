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
import android.net.Uri
import com.logicalsapien.sapienterm.data.CredentialRepository
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.QuickCommandRepository
import com.logicalsapien.sapienterm.data.entity.CredentialType
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

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
    val credentialsImported: Int = 0
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
     */
    private suspend fun performImport(exportData: ExportData, mode: ImportMode): ImportResult {
        if (mode == ImportMode.REPLACE) {
            deleteExistingData(exportData)
        }

        val connectionsImported = importConnections(exportData.connections, mode)
        val quickCommandsImported = importQuickCommands(exportData.quickCommands, mode)
        val credentialsImported = importCredentials(exportData.credentials, mode)

        return ImportResult(
            connectionsImported = connectionsImported,
            quickCommandsImported = quickCommandsImported,
            credentialsImported = credentialsImported
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
        if (exportData.quickCommands != null) {
            quickCommandRepository.deleteAll()
        }
        if (exportData.credentials != null) {
            credentialRepository.deleteAll()
        }
    }

    /**
     * Import connections, skipping duplicates in MERGE mode.
     * Duplicates are detected by hostname + port + username.
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
                ipVersion = conn.ipVersion
            )
            try {
                hostRepository.saveHost(host)
                count++
            } catch (e: Exception) {
                Timber.w(e, "Failed to import connection: ${conn.name}")
            }
        }
        return count
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
