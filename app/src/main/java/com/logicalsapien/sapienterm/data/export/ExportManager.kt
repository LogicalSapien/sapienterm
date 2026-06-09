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

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.logicalsapien.sapienterm.data.ConnectionGroupRepository
import com.logicalsapien.sapienterm.data.CredentialRepository
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.QuickCommandRepository
import com.logicalsapien.sapienterm.data.entity.CredentialType
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Manages exporting SapienTerm data (connections, quick commands, credentials)
 * to shareable backup files.
 *
 * Encrypted exports (.sapienterm) use PBKDF2 + AES-256-GCM via [CryptoUtils].
 * Plain exports (.json) contain no credential secrets and are unencrypted.
 */
class ExportManager(
    private val context: Context,
    private val hostRepository: HostRepository,
    private val quickCommandRepository: QuickCommandRepository,
    private val credentialRepository: CredentialRepository,
    private val connectionGroupRepository: ConnectionGroupRepository,
    private val dispatchers: CoroutineDispatchers
) {

    /**
     * Export selected data to a backup file.
     *
     * If credentials are included, a passphrase is required and the output is an
     * encrypted `.sapienterm` file. Otherwise, the output is a plain `.json` file.
     *
     * @param includeConnections Whether to include host connections
     * @param includeQuickCommands Whether to include quick commands
     * @param includeCredentials Whether to include credentials (requires passphrase)
     * @param passphrase Passphrase for encrypting the export (required if credentials included)
     * @return URI of the created export file for sharing
     * @throws IllegalArgumentException if credentials included without passphrase
     * @throws ExportException if export fails
     */
    suspend fun exportData(
        includeConnections: Boolean,
        includeQuickCommands: Boolean,
        includeCredentials: Boolean,
        passphrase: String?
    ): Uri = withContext(dispatchers.io) {
        if (includeCredentials && passphrase.isNullOrEmpty()) {
            throw IllegalArgumentException("Passphrase is required when exporting credentials")
        }

        try {
            val exportData = buildExportData(
                includeConnections,
                includeQuickCommands,
                includeCredentials
            )

            val jsonString = exportData.toJson().toString(2)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)

            val fileBytes = if (includeCredentials && !passphrase.isNullOrEmpty()) {
                CryptoUtils.encrypt(jsonBytes, passphrase)
            } else {
                jsonBytes
            }
            val isEncrypted = includeCredentials && !passphrase.isNullOrEmpty()

            val extension = if (isEncrypted) "sapienterm" else "json"
            val mimeType = if (isEncrypted) "application/octet-stream" else "application/json"
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
            val fileName = "sapienterm_backup_$timestamp.$extension"

            writeToFile(fileName, mimeType, fileBytes)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Export failed")
            throw ExportException("Failed to export data", e)
        }
    }

    /**
     * Build the [ExportData] model from repository data.
     */
    private suspend fun buildExportData(
        includeConnections: Boolean,
        includeQuickCommands: Boolean,
        includeCredentials: Boolean
    ): ExportData {
        val allGroups = connectionGroupRepository.getAll()
        val groupNameById = allGroups.associateBy({ it.id }, { it.name })

        val allCredentials = credentialRepository.getAll()
        val credentialLabelById = allCredentials.associateBy({ it.id }, { it.label })

        val connections = if (includeConnections) {
            hostRepository.getHosts().map { host ->
                ExportConnection(
                    name = host.nickname,
                    hostname = host.hostname,
                    port = host.port,
                    username = host.username,
                    protocol = host.protocol,
                    color = host.color,
                    useKeys = host.useKeys,
                    useAuthAgent = host.useAuthAgent,
                    postLogin = host.postLogin,
                    wantSession = host.wantSession,
                    compression = host.compression,
                    stayConnected = host.stayConnected,
                    quickDisconnect = host.quickDisconnect,
                    scrollbackLines = host.scrollbackLines,
                    useCtrlAltAsMetaKey = host.useCtrlAltAsMetaKey,
                    ipVersion = host.ipVersion,
                    groupName = host.groupId?.let { groupNameById[it] },
                    credentialLabel = host.credentialId?.let { credentialLabelById[it] }
                )
            }
        } else {
            null
        }

        val groups = if (includeConnections && allGroups.isNotEmpty()) {
            allGroups.map { group ->
                ExportConnectionGroup(
                    name = group.name,
                    color = group.color,
                    sortOrder = group.sortOrder
                )
            }
        } else {
            null
        }

        val quickCommands = if (includeQuickCommands) {
            quickCommandRepository.getAll().map { cmd ->
                ExportQuickCommand(
                    title = cmd.title,
                    command = cmd.command,
                    category = cmd.category,
                    sortOrder = cmd.sortOrder
                )
            }
        } else {
            null
        }

        val credentials = if (includeCredentials) {
            allCredentials.map { cred ->
                val password = if (cred.type == CredentialType.PASSWORD) {
                    credentialRepository.getDecryptedPassword(cred.id)
                } else {
                    null
                }
                val privateKey = if (cred.type == CredentialType.SSH_KEY) {
                    credentialRepository.getDecryptedPrivateKey(cred.id)?.let {
                        android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP)
                    }
                } else {
                    null
                }
                val passphrase = if (cred.type == CredentialType.SSH_KEY) {
                    credentialRepository.getDecryptedPassphrase(cred.id)
                } else {
                    null
                }

                ExportCredential(
                    label = cred.label,
                    type = cred.type.name,
                    password = password,
                    privateKey = privateKey,
                    publicKey = cred.publicKey,
                    passphrase = passphrase
                )
            }
        } else {
            null
        }

        return ExportData(
            exportDate = Instant.now().toString(),
            connections = connections,
            quickCommands = quickCommands,
            credentials = credentials,
            groups = groups
        )
    }

    /**
     * Write file bytes to Downloads using MediaStore (API 29+) or external files dir.
     */
    private fun writeToFile(fileName: String, mimeType: String, data: ByteArray): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeToMediaStore(fileName, mimeType, data)
        } else {
            writeToExternalFiles(fileName, data)
        }
    }

    /**
     * Write to Downloads via MediaStore (Android 10+).
     */
    private fun writeToMediaStore(fileName: String, mimeType: String, data: ByteArray): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw ExportException("Failed to create file in Downloads")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(data)
        } ?: throw ExportException("Failed to write export file")

        return uri
    }

    /**
     * Write to app's external files directory (Android 9 and below).
     */
    private fun writeToExternalFiles(fileName: String, data: ByteArray): Uri {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw ExportException("External files directory not available")

        val file = File(dir, fileName)
        file.writeBytes(data)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}

/**
 * Exception thrown when export operations fail.
 */
class ExportException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
