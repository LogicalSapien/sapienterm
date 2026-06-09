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
import androidx.core.content.edit
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
 * Manages automatic local backups of SapienTerm data.
 *
 * Backups are plain JSON files written to the app's internal storage
 * (`filesDir/autobackups/`). No extra permissions are required.
 * Old backups are pruned to keep only the configured retention count.
 */
class AutoBackupManager(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val hostRepository: HostRepository,
    private val quickCommandRepository: QuickCommandRepository,
    private val credentialRepository: CredentialRepository,
    private val connectionGroupRepository: ConnectionGroupRepository,
    private val dispatchers: CoroutineDispatchers
) {
    private val backupDir: File
        get() = File(context.filesDir, BACKUP_DIR).also { it.mkdirs() }

    val isEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTO_BACKUP_ENABLED, false)

    val retentionCount: Int
        get() = prefs.getInt(PREF_RETENTION_COUNT, DEFAULT_RETENTION)

    val lastBackupTime: Long
        get() = prefs.getLong(PREF_LAST_BACKUP_TIME, 0L)

    /**
     * Run a backup if auto-backup is enabled and enough time has elapsed
     * since the last backup.
     */
    suspend fun runIfDue() {
        if (!isEnabled) return
        val elapsed = System.currentTimeMillis() - lastBackupTime
        if (elapsed < BACKUP_INTERVAL_MS) {
            Timber.d("Auto-backup skipped, last backup %d min ago", elapsed / 60_000)
            return
        }
        runBackup()
    }

    /**
     * Force a backup immediately regardless of schedule.
     */
    suspend fun runBackup(): Boolean = withContext(dispatchers.io) {
        try {
            val exportData = buildExportData()
            val jsonString = exportData.toJson().toString(2)
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
            val file = File(backupDir, "autobackup_$timestamp.json")
            file.writeText(jsonString, Charsets.UTF_8)

            prefs.edit { putLong(PREF_LAST_BACKUP_TIME, System.currentTimeMillis()) }
            pruneOldBackups()

            Timber.i("Auto-backup saved: ${file.name}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Auto-backup failed")
            false
        }
    }

    /**
     * List available auto-backup files, newest first.
     */
    fun listBackups(): List<BackupFileInfo> {
        return backupDir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupFileInfo(it.name, it.lastModified(), it.length()) }
            ?: emptyList()
    }

    /**
     * Restore from a specific auto-backup file.
     *
     * @return parsed [ExportData] for preview/confirmation before actual import
     */
    suspend fun readBackup(fileName: String): ExportData = withContext(dispatchers.io) {
        val file = File(backupDir, fileName)
        if (!file.exists()) throw ImportException("Backup file not found: $fileName")
        val json = file.readText(Charsets.UTF_8)
        ExportData.fromJson(org.json.JSONObject(json))
    }

    /**
     * Get the absolute path to a backup file (for the ImportManager to consume).
     */
    fun getBackupFile(fileName: String): File = File(backupDir, fileName)

    fun setEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(PREF_AUTO_BACKUP_ENABLED, enabled) }
    }

    fun setRetentionCount(count: Int) {
        prefs.edit { putInt(PREF_RETENTION_COUNT, count.coerceIn(1, 30)) }
    }

    private fun pruneOldBackups() {
        val files = backupDir.listFiles { f -> f.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return
        val keep = retentionCount
        if (files.size > keep) {
            files.drop(keep).forEach { file ->
                Timber.d("Pruning old backup: ${file.name}")
                file.delete()
            }
        }
    }

    private suspend fun buildExportData(): ExportData {
        val allGroups = connectionGroupRepository.getAll()
        val groupNameById = allGroups.associateBy({ it.id }, { it.name })

        val allCredentials = credentialRepository.getAll()
        val credentialLabelById = allCredentials.associateBy({ it.id }, { it.label })

        val connections = hostRepository.getHosts().map { host ->
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

        val groups = allGroups.map { group ->
            ExportConnectionGroup(
                name = group.name,
                color = group.color,
                sortOrder = group.sortOrder
            )
        }

        val quickCommands = quickCommandRepository.getAll().map { cmd ->
            ExportQuickCommand(
                title = cmd.title,
                command = cmd.command,
                category = cmd.category,
                sortOrder = cmd.sortOrder
            )
        }

        // Auto-backup does NOT include credential secrets (they require passphrase encryption).
        // Labels and types are included so the user knows what's missing after restore.
        val credentials = allCredentials.map { cred ->
            ExportCredential(
                label = cred.label,
                type = cred.type.name,
                password = null,
                privateKey = null,
                publicKey = cred.publicKey,
                passphrase = null
            )
        }

        return ExportData(
            exportDate = Instant.now().toString(),
            connections = connections.ifEmpty { null },
            quickCommands = quickCommands.ifEmpty { null },
            credentials = credentials.ifEmpty { null },
            groups = groups.ifEmpty { null }
        )
    }

    companion object {
        private const val BACKUP_DIR = "autobackups"
        private const val PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val PREF_RETENTION_COUNT = "auto_backup_retention"
        private const val PREF_LAST_BACKUP_TIME = "auto_backup_last_time"
        const val DEFAULT_RETENTION = 3
        private const val BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
}

data class BackupFileInfo(
    val fileName: String,
    val timestamp: Long,
    val sizeBytes: Long
)
