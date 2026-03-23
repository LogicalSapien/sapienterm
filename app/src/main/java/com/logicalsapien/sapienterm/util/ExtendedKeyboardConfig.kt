/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
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

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Represents a key that can appear in the extended keyboard strip.
 *
 * @param id Unique identifier used for persistence
 * @param label Display label on the key button
 * @param defaultEnabled Whether this key is shown by default
 */
enum class ExtendedKey(val id: String, val label: String, val defaultEnabled: Boolean = true) {
    ESC("esc", "Esc"),
    TAB("tab", "Tab"),
    CTRL("ctrl", "Ctrl"),
    ALT("alt", "Alt"),
    ARROW_UP("arrow_up", "\u2191"),
    ARROW_DOWN("arrow_down", "\u2193"),
    ARROW_LEFT("arrow_left", "\u2190"),
    ARROW_RIGHT("arrow_right", "\u2192"),
    PIPE("pipe", "|"),
    DASH("dash", "-"),
    SLASH("slash", "/"),
    TILDE("tilde", "~"),
    HOME("home", "Home"),
    END("end", "End"),
    PGUP("pgup", "PgUp"),
    PGDN("pgdn", "PgDn");

    companion object {
        fun fromId(id: String): ExtendedKey? = entries.find { it.id == id }
    }
}

/**
 * Configuration for which keys are visible in the extended keyboard strip.
 * Loads and saves enabled keys to SharedPreferences as a comma-separated string.
 */
data class ExtendedKeyboardConfig(
    val enabledKeys: Set<ExtendedKey> = ExtendedKey.entries.toSet()
) {
    fun isKeyEnabled(key: ExtendedKey): Boolean = key in enabledKeys

    companion object {
        private const val PREF_KEY = "extended_keyboard_keys"

        /**
         * Load configuration from SharedPreferences.
         * If no value is stored, all keys are enabled by default.
         */
        fun load(prefs: SharedPreferences): ExtendedKeyboardConfig {
            val stored = prefs.getString(PREF_KEY, null)
            if (stored == null) {
                // Default: all keys enabled
                return ExtendedKeyboardConfig()
            }
            if (stored.isBlank()) {
                // Explicitly empty: no keys enabled
                return ExtendedKeyboardConfig(enabledKeys = emptySet())
            }
            val enabledIds = stored.split(",").filter { it.isNotBlank() }.toSet()
            val enabledKeys = ExtendedKey.entries.filter { it.id in enabledIds }.toSet()
            return ExtendedKeyboardConfig(enabledKeys = enabledKeys)
        }

        /**
         * Save configuration to SharedPreferences.
         */
        fun save(prefs: SharedPreferences, config: ExtendedKeyboardConfig) {
            val value = config.enabledKeys.joinToString(",") { it.id }
            prefs.edit { putString(PREF_KEY, value) }
        }
    }
}
