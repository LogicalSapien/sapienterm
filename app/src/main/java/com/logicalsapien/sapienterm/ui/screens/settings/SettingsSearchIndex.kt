/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.annotation.StringRes
import com.logicalsapien.sapienterm.R

/**
 * A flat, searchable record of a single settings preference.
 *
 * The hub builds a [List] of these via [settingsSearchIndex] so the search
 * field can match by title or by free-form keywords without having to walk
 * the real settings tree.
 */
data class SettingsSearchEntry(
    val category: SettingsCategory,
    @StringRes val titleRes: Int,
    val keywords: List<String>
)

/**
 * A curated, hard-coded list of searchable settings entries. We don't try to
 * enumerate every preference in the 2000+ line Settings screen — just the
 * ones a user is likely to look for by name. New entries can be added as the
 * screen grows without touching the real settings layout.
 */
fun settingsSearchIndex(): List<SettingsSearchEntry> = listOf(
    // Appearance / Display
    SettingsSearchEntry(
        SettingsCategory.APPEARANCE,
        R.string.pref_fontfamily_title,
        listOf("font", "typeface", "terminal font")
    ),
    SettingsSearchEntry(
        SettingsCategory.APPEARANCE,
        R.string.pref_language_title,
        listOf("language", "locale", "translation")
    ),
    SettingsSearchEntry(
        SettingsCategory.APPEARANCE,
        R.string.pref_rotation_title,
        listOf("rotation", "orientation", "landscape")
    ),
    SettingsSearchEntry(
        SettingsCategory.APPEARANCE,
        R.string.pref_fullscreen_title,
        listOf("fullscreen", "full screen", "immersive")
    ),
    SettingsSearchEntry(
        SettingsCategory.APPEARANCE,
        R.string.pref_titlebarhide_title,
        listOf("title bar", "hide", "autohide")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_terminal_bottom_bar_title,
        listOf("bottom bar", "toolbar", "shortcuts", "keyboard")
    ),

    // Terminal / Session
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_memkeys_title,
        listOf("keys", "memory", "passphrase")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_conn_persist_title,
        listOf("persist", "background", "connection")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_wifilock_title,
        listOf("wifi", "wi-fi", "network", "lock")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_scrollback_title,
        listOf("scrollback", "buffer", "history")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_keepalive_title,
        listOf("keep awake", "screen", "wake lock")
    ),

    // Terminal / Keyboard
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_alwaysvisible_title,
        listOf("keyboard", "special keys", "visible")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_stickymodifiers_title,
        listOf("sticky", "modifier", "ctrl", "shift")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_shiftfkeys_title,
        listOf("shift", "f-keys", "function keys")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_ctrlfkeys_title,
        listOf("ctrl", "f-keys", "function keys")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_volumefont_title,
        listOf("volume", "font size", "keys")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_camera_title,
        listOf("camera", "shortcut", "button")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_pg_updn_gesture_title,
        listOf("page up", "page down", "gesture")
    ),

    // Terminal / Bell
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_bell_title,
        listOf("bell", "audio", "sound")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_bell_vibrate_title,
        listOf("bell", "vibrate", "haptic")
    ),
    SettingsSearchEntry(
        SettingsCategory.TERMINAL,
        R.string.pref_bell_notification_title,
        listOf("bell", "notification", "background")
    ),

    // Profiles
    SettingsSearchEntry(
        SettingsCategory.PROFILES,
        R.string.pref_default_profile_title,
        listOf("profile", "default", "template")
    ),

    // Security
    SettingsSearchEntry(
        SettingsCategory.SECURITY,
        R.string.pref_auth_on_launch_title,
        listOf("lock", "biometric", "fingerprint", "authenticate")
    ),

    // Data / backup
    SettingsSearchEntry(
        SettingsCategory.DATA,
        R.string.pref_backupkeys_title,
        listOf("backup", "keys", "pubkey")
    ),
    SettingsSearchEntry(
        SettingsCategory.DATA,
        R.string.pref_auto_backup_title,
        listOf("auto backup", "backup", "export")
    )
)
