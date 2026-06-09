/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
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

package com.logicalsapien.sapienterm.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.logicalsapien.sapienterm.data.dao.ColorSchemeDao
import com.logicalsapien.sapienterm.data.dao.CommandHistoryDao
import com.logicalsapien.sapienterm.data.dao.ConnectionGroupDao
import com.logicalsapien.sapienterm.data.dao.CredentialDao
import com.logicalsapien.sapienterm.data.dao.HostDao
import com.logicalsapien.sapienterm.data.dao.KnownHostDao
import com.logicalsapien.sapienterm.data.dao.PortForwardDao
import com.logicalsapien.sapienterm.data.dao.ProfileDao
import com.logicalsapien.sapienterm.data.dao.PubkeyDao
import com.logicalsapien.sapienterm.data.dao.PromptTemplateDao
import com.logicalsapien.sapienterm.data.dao.QuickCommandDao
import com.logicalsapien.sapienterm.data.entity.ColorPalette
import com.logicalsapien.sapienterm.data.entity.ColorScheme
import com.logicalsapien.sapienterm.data.entity.CommandHistory
import com.logicalsapien.sapienterm.data.entity.ConnectionGroup
import com.logicalsapien.sapienterm.data.entity.Credential
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.KnownHost
import com.logicalsapien.sapienterm.data.entity.PortForward
import com.logicalsapien.sapienterm.data.entity.Profile
import com.logicalsapien.sapienterm.data.entity.Pubkey
import com.logicalsapien.sapienterm.data.entity.PromptTemplate
import com.logicalsapien.sapienterm.data.entity.QuickCommand

/**
 * ConnectBot Room database.
 *
 * This database contains all the tables needed to run ConnectBot:
 * - hosts: SSH/Telnet connection configurations
 * - pubkeys: SSH key pairs with security-conscious backup controls
 * - port_forwards: SSH port forwarding rules
 * - known_hosts: SSH host key verification data
 * - color_schemes: Terminal color scheme metadata
 * - color_palette: Terminal color overrides
 * - profiles: Terminal profile configurations
 *
 * Migration Strategy:
 * - Version 1: Initial Room schema (migrated from HostDatabase v27 + PubkeyDatabase v2)
 * - Version 2: Added jump_host_id column for ProxyJump support (AutoMigration)
 * - Version 3: Added unique index on known_hosts (hostname, port) (AutoMigration)
 * - Version 4: Changed known_hosts index to (host_id, host_key) (AutoMigration)
 * - Version 5: Added profiles table and profile_id column to hosts (manual migration)
 * - Version 6: Added force_size_rows and force_size_columns to profiles (AutoMigration)
 * - Version 7: Added ip_version column to hosts for IP version preference (AutoMigration)
 * - Version 8: Added quick_commands table for reusable terminal commands (manual migration)
 * - Version 9: Added credentials table for reusable authentication credentials (manual migration)
 * - Version 10: Added credential_id column to hosts for linking credentials to connections (manual migration)
 * - Version 11: Added connection_groups table, group_id column to hosts, and command_history table (manual migration)
 * - Version 12: Added fixed_cols and fixed_rows to hosts (manual migration)
 * - Version 13: Added bottom_bar_preset_override to hosts (manual migration)
 * - Version 14: Added session_keyboard_override to hosts (manual migration)
 * - Future versions: Use Room AutoMigration when possible for simple schema changes
 *
 * Security Considerations:
 * - Pubkeys table supports per-key backup control via allowBackup field
 * - Custom BackupAgent filters pubkeys during backup/restore operations
 */
@Database(
    entities = [
        Host::class,
        Pubkey::class,
        PortForward::class,
        KnownHost::class,
        ColorScheme::class,
        ColorPalette::class,
        Profile::class,
        QuickCommand::class,
        Credential::class,
        ConnectionGroup::class,
        CommandHistory::class,
        PromptTemplate::class,
    ],
    version = 16,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 4),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 15, to = 16),
    ]
)
@TypeConverters(Converters::class)
abstract class ConnectBotDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun pubkeyDao(): PubkeyDao
    abstract fun portForwardDao(): PortForwardDao
    abstract fun knownHostDao(): KnownHostDao
    abstract fun colorSchemeDao(): ColorSchemeDao
    abstract fun profileDao(): ProfileDao
    abstract fun quickCommandDao(): QuickCommandDao
    abstract fun credentialDao(): CredentialDao
    abstract fun connectionGroupDao(): ConnectionGroupDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun promptTemplateDao(): PromptTemplateDao

    companion object {
        /**
         * Current database schema version.
         * This is also used for JSON export/import versioning.
         */
        const val SCHEMA_VERSION = 16

        /**
         * Migration from version 4 to 5: Add profiles table and profile_id to hosts.
         * Also creates profiles from existing host settings and migrates hosts to use them.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create profiles table
                // Note: No foreign key to color_schemes because built-in color schemes use negative IDs
                // and are virtual (not stored in the database). Only custom schemes have positive IDs.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon_color` TEXT,
                        `color_scheme_id` INTEGER NOT NULL DEFAULT -1,
                        `font_family` TEXT,
                        `font_size` INTEGER NOT NULL DEFAULT 10,
                        `del_key` TEXT NOT NULL DEFAULT 'del',
                        `encoding` TEXT NOT NULL DEFAULT 'UTF-8',
                        `emulation` TEXT NOT NULL DEFAULT 'xterm-256color'
                    )
                    """.trimIndent()
                )

                // Create index on profile name
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_profiles_name` ON `profiles` (`name`)")

                // Insert default profile with color_scheme_id = -1 (Default built-in scheme)
                db.execSQL(
                    """
                    INSERT INTO `profiles` (`id`, `name`, `color_scheme_id`, `font_family`, `font_size`, `del_key`, `encoding`, `emulation`)
                    VALUES (1, 'Default', -1, NULL, 10, 'del', 'UTF-8', 'xterm-256color')
                    """.trimIndent()
                )

                // Create profiles from unique host settings combinations
                // Use a data class key: (color_scheme_id, font_size, del_key, encoding)
                // This groups hosts with identical terminal settings into shared profiles
                db.execSQL(
                    """
                    CREATE TEMP TABLE temp_profile_settings AS
                    SELECT DISTINCT color_scheme_id, font_size, del_key, encoding
                    FROM hosts
                    WHERE NOT (color_scheme_id = 1 AND font_size = 10 AND del_key = 'DEL' AND encoding = 'UTF-8')
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO `profiles` (`name`, `color_scheme_id`, `font_size`, `del_key`, `encoding`)
                    SELECT
                        'Migrated Profile ' || ROWID,
                        color_scheme_id,
                        font_size,
                        del_key,
                        encoding
                    FROM temp_profile_settings
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE temp_profile_settings")

                // Recreate hosts table without the old columns (encoding, font_size, color_scheme_id, del_key, font_family)
                // and add profile_id column. SQLite doesn't support DROP COLUMN before 3.35.0,
                // so we need to recreate the table.

                // Create new hosts table with correct schema
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hosts_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `protocol` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `hostname` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `host_key_algo` TEXT,
                        `last_connect` INTEGER NOT NULL,
                        `color` TEXT,
                        `use_keys` INTEGER NOT NULL,
                        `use_auth_agent` TEXT,
                        `post_login` TEXT,
                        `pubkey_id` INTEGER NOT NULL,
                        `want_session` INTEGER NOT NULL,
                        `compression` INTEGER NOT NULL,
                        `stay_connected` INTEGER NOT NULL,
                        `quick_disconnect` INTEGER NOT NULL,
                        `scrollback_lines` INTEGER NOT NULL,
                        `use_ctrl_alt_as_meta_key` INTEGER NOT NULL,
                        `jump_host_id` INTEGER,
                        `profile_id` INTEGER
                    )
                    """.trimIndent()
                )

                // Copy data from old table to new table, mapping old columns to profile_id
                db.execSQL(
                    """
                    INSERT INTO `hosts_new` (
                        `id`, `nickname`, `protocol`, `username`, `hostname`, `port`,
                        `host_key_algo`, `last_connect`, `color`, `use_keys`, `use_auth_agent`,
                        `post_login`, `pubkey_id`, `want_session`, `compression`, `stay_connected`,
                        `quick_disconnect`, `scrollback_lines`, `use_ctrl_alt_as_meta_key`,
                        `jump_host_id`, `profile_id`
                    )
                    SELECT
                        h.id, h.nickname, h.protocol, h.username, h.hostname, h.port,
                        h.host_key_algo, h.last_connect, h.color, h.use_keys, h.use_auth_agent,
                        h.post_login, h.pubkey_id, h.want_session, h.compression, h.stay_connected,
                        h.quick_disconnect, h.scrollback_lines, h.use_ctrl_alt_as_meta_key,
                        h.jump_host_id,
                        COALESCE((
                            SELECT p.id FROM profiles p
                            WHERE p.color_scheme_id = h.color_scheme_id
                              AND p.font_size = h.font_size
                              AND p.del_key = h.del_key
                              AND p.encoding = h.encoding
                            LIMIT 1
                        ), 1) as profile_id
                    FROM hosts h
                    """.trimIndent()
                )

                // Drop old table
                db.execSQL("DROP TABLE `hosts`")

                // Rename new table to hosts
                db.execSQL("ALTER TABLE `hosts_new` RENAME TO `hosts`")

                // Recreate indices
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hosts_nickname` ON `hosts` (`nickname`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hosts_protocol_username_hostname_port` ON `hosts` (`protocol`, `username`, `hostname`, `port`)")
            }
        }

        /**
         * Migration from version 7 to 8: Add quick_commands table.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quick_commands` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `command` TEXT NOT NULL,
                        `category` TEXT,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `sort_order` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 8 to 9: Add credentials table.
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `credentials` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `label` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `encrypted_password` BLOB,
                        `encrypted_private_key` BLOB,
                        `public_key` TEXT,
                        `encrypted_passphrase` BLOB,
                        `created_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Migration from version 9 to 10: Add credential_id column to hosts.
         * This links hosts to saved credentials from the Credentials vault.
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hosts ADD COLUMN credential_id INTEGER DEFAULT NULL")
            }
        }

        /**
         * Migration from version 10 to 11: Add connection_groups table, group_id to hosts,
         * and command_history table.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hosts ADD COLUMN fixed_cols INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE hosts ADD COLUMN fixed_rows INTEGER DEFAULT NULL")
            }
        }

        /**
         * Migration from version 12 to 13: Per-host terminal bottom bar layout override.
         */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hosts ADD COLUMN bottom_bar_preset_override TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration from version 13 to 14: Per-host software keyboard session start override.
         */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hosts ADD COLUMN session_keyboard_override TEXT DEFAULT NULL")
            }
        }

        /**
         * Migration from version 14 to 15: pinned flag on hosts for the Search tab.
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE hosts ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create connection_groups table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `connection_groups` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` TEXT,
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Add group_id column to hosts table
                db.execSQL("ALTER TABLE hosts ADD COLUMN group_id INTEGER DEFAULT NULL")

                // Create command_history table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `command_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `host_id` INTEGER NOT NULL,
                        `command` TEXT NOT NULL,
                        `executed_at` INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // Create index on host_id for command_history
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_command_history_host_id` ON `command_history` (`host_id`)")
            }
        }
    }
}
