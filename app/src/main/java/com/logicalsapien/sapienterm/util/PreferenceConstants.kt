/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2007 Kenny Root
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
package com.logicalsapien.sapienterm.util

/**
 * @author Kenny Root
 */
object PreferenceConstants {
    const val MEMKEYS: String = "memkeys"

    const val SCROLLBACK: String = "scrollback"

    const val ROTATION: String = "rotation"

    const val BACKUP_KEYS: String = "backupkeys"
    const val BACKUP_KEYS_DEFAULT: Boolean = false

    const val ROTATION_DEFAULT: String = "Default"
    const val ROTATION_LANDSCAPE: String = "Force landscape"
    const val ROTATION_PORTRAIT: String = "Force portrait"

    const val FULLSCREEN: String = "fullscreen"
    const val TITLEBARHIDE: String = "titlebarhide"
    const val PG_UPDN_GESTURE: String = "pgupdngesture"

    const val KEYMODE: String = "keymode"
    const val KEY_ALWAYS_VISIBLE: String = "alwaysvisible"

    const val KEYMODE_RIGHT: String = "Use right-side keys"
    const val KEYMODE_LEFT: String = "Use left-side keys"
    const val KEYMODE_NONE: String = "none"

    const val CAMERA: String = "camera"

    const val CAMERA_CTRLA_SPACE: String = "Ctrl+A then Space"
    const val CAMERA_CTRLA: String = "Ctrl+A"
    const val CAMERA_ESC: String = "Esc"
    const val CAMERA_ESC_A: String = "Esc+A"

    const val KEEP_ALIVE: String = "keepalive"

    const val WIFI_LOCK: String = "wifilock"

    const val BUMPY_ARROWS: String = "bumpyarrows"

    const val SORT_BY_COLOR: String = "sortByColor"

    const val BELL: String = "bell"
    const val BELL_VOLUME: String = "bellVolume"
    const val BELL_VIBRATE: String = "bellVibrate"
    const val BELL_NOTIFICATION: String = "bellNotification"
    const val DEFAULT_BELL_VOLUME: Float = 0.25f

    const val CONNECTION_PERSIST: String = "connPersist"
    const val CONNECTION_PERSIST_PROMPT_SHOWN: String = "connPersistPromptShown"
    const val NOTIFICATION_PERMISSION_DENIED: String = "notificationPermissionDenied"

    const val SHIFT_FKEYS: String = "shiftfkeys"
    const val CTRL_FKEYS: String = "ctrlfkeys"
    const val VOLUME_FONT: String = "volumefont"
    const val STICKY_MODIFIERS: String = "stickymodifiers"
    const val YES: String = "yes"
    const val NO: String = "no"
    const val ALT: String = "alt"

    /* Backup identifiers */
    const val BACKUP_PREF_KEY: String = "prefs"

    /* Security */
    const val AUTH_ON_LAUNCH: String = "authOnLaunch"

    /* Font settings */
    const val FONT_FAMILY: String = "fontFamily"
    const val FONT_FAMILY_DEFAULT: String = "SYSTEM_DEFAULT"
    const val CUSTOM_FONTS: String = "customFonts"

    /* Extended keyboard strip */
    const val EXTENDED_KEYBOARD_KEYS: String = "extended_keyboard_keys"

    /** Stored values: `default`, `tmux`, or `custom:…` (legacy `zellij` / `multiplexer` migrate to `tmux`). */
    const val TERMINAL_BOTTOM_BAR_PRESET: String = "terminal_bottom_bar_preset"

    /**
     * Software (Android) IME when a session opens: `auto`, `on`, `off`.
     * Single global default; per-host override in [com.logicalsapien.sapienterm.data.entity.Host.sessionKeyboardOverride].
     */
    const val TERMINAL_SESSION_KEYBOARD: String = "terminal_session_keyboard"

    /** @deprecated Migrated into [TERMINAL_SESSION_KEYBOARD]; kept for one-time migration only. */
    const val TERMINAL_KEYBOARD_BAR_DEFAULT: String = "terminal_keyboard_bar_default"
    const val TERMINAL_KEYBOARD_BAR_ZELLIJ: String = "terminal_keyboard_bar_zellij"
    const val TERMINAL_KEYBOARD_BAR_TMUX: String = "terminal_keyboard_bar_tmux"

    /** JSON array of user-defined bottom bar layouts; see [com.logicalsapien.sapienterm.data.CustomBottomBarLayout]. */
    const val CUSTOM_BOTTOM_BAR_LAYOUTS: String = "custom_bottom_bar_layouts"

    /**
     * When true, floating text input sends using **bracketed paste** (OSC paste markers)
     * instead of plain newline-terminated text — helps interactive CLIs (e.g. Cursor-style agents, readline).
     */
    const val TERMINAL_BRACKETED_PASTE_SEND: String = "terminal_bracketed_paste_send"

    /** Selected SapienTerm theme — one of ThemeId.storageString. */
    const val APPEARANCE_THEME_ID: String = "appearance_theme_id"

    /** Default density — one of DensityMode.storageString. */
    const val APPEARANCE_DENSITY: String = "appearance_density"
}
