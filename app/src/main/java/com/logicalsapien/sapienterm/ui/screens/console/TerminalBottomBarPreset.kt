/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.ui.screens.console

/** Layout preset for the session bottom bar; persisted under `PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET`. */
enum class TerminalBottomBarPreset(val prefValue: String) {
    /** Commands, arrows, Enter, modifiers — no extra ^B/^D strip keys. */
    DEFAULT("default"),

    /** Adds **^B** (tmux prefix) and **^D** after the arrow keys. */
    TMUX("tmux");

    companion object {
        const val CUSTOM_PREFIX: String = "custom:"

        fun fromPref(value: String?): TerminalBottomBarPreset {
            val v = value?.trim().orEmpty()
            if (v.startsWith(CUSTOM_PREFIX)) return DEFAULT
            return when (v) {
                TMUX.prefValue,
                "zellij",
                "multiplexer" -> TMUX

                else -> DEFAULT
            }
        }

        /**
         * Builtin strip preset and optional custom layout id (`custom:` is stripped).
         * When [second] is non-null, the UI shows a user-defined shortcut strip instead of tmux keys.
         */
        fun parseStored(value: String?): Pair<TerminalBottomBarPreset, String?> {
            val v = value?.trim().orEmpty()
            if (v.startsWith(CUSTOM_PREFIX)) {
                val id = v.removePrefix(CUSTOM_PREFIX).trim()
                return Pair(DEFAULT, id.takeIf { it.isNotEmpty() })
            }
            return Pair(fromPref(value), null)
        }

        private fun resolvedRaw(hostOverride: String?, globalPref: String?): String {
            val h = hostOverride?.trim()
            if (!h.isNullOrEmpty()) return h
            val g = globalPref?.trim()
            return if (g.isNullOrEmpty()) "default" else g
        }

        /**
         * @param hostOverride Per-connection value from `hosts.bottom_bar_preset_override`; `null` or blank uses [globalPrefValue].
         */
        fun resolvedSelection(hostOverride: String?, globalPrefValue: String?): Pair<TerminalBottomBarPreset, String?> = parseStored(resolvedRaw(hostOverride, globalPrefValue))

        /**
         * Default shortcut strip shown when no custom layout is active.
         * Covers the keys every SSH user reaches for most: tab-completion, interrupt,
         * escape, arrow navigation, and enter. Ctrl is a sticky modifier toggle.
         */
        val DEFAULT_STRIP: List<BottomBarShortcutAction> = listOf(
            BottomBarShortcutAction.TAB,
            BottomBarShortcutAction.CTRL_TOGGLE,
            BottomBarShortcutAction.ESC,
            BottomBarShortcutAction.ARROW_UP,
            BottomBarShortcutAction.ARROW_DOWN,
            BottomBarShortcutAction.ARROW_LEFT,
            BottomBarShortcutAction.ARROW_RIGHT,
            BottomBarShortcutAction.CTRL_C,
            BottomBarShortcutAction.ENTER
        )
    }
}
