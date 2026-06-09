/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.util

import android.content.SharedPreferences
import com.logicalsapien.sapienterm.data.entity.Host

/**
 * Whether the software IME should be shown when a session opens (no hardware keyboard).
 */
enum class SessionKeyboardPolicy {
    /** Match device: show IME when no hardware keyboard (legacy behavior). */
    AUTO,

    /** Prefer showing the software keyboard when the session opens. */
    ON,

    /** Keep the software keyboard hidden until the user opens it. */
    OFF;

    fun shouldShowSoftwareKeyboard(hasHardwareKeyboard: Boolean): Boolean = when (this) {
        AUTO -> !hasHardwareKeyboard
        ON -> !hasHardwareKeyboard
        OFF -> false
    }

    companion object {
        const val STORED_AUTO: String = "auto"
        const val STORED_ON: String = "on"
        const val STORED_OFF: String = "off"

        fun fromStored(value: String?): SessionKeyboardPolicy = when (value?.trim()?.lowercase()) {
            STORED_ON -> ON
            STORED_OFF -> OFF
            else -> AUTO
        }
    }
}

/**
 * Resolves keyboard policy: per-host override wins, else the single app default ([PreferenceConstants.TERMINAL_SESSION_KEYBOARD]).
 */
fun resolveSessionKeyboardPolicy(
    host: Host?,
    prefs: SharedPreferences
): SessionKeyboardPolicy {
    val hostOverride = host?.sessionKeyboardOverride?.trim()
    if (!hostOverride.isNullOrEmpty()) {
        return SessionKeyboardPolicy.fromStored(hostOverride)
    }
    val stored = prefs.getString(PreferenceConstants.TERMINAL_SESSION_KEYBOARD, SessionKeyboardPolicy.STORED_AUTO)
        ?: SessionKeyboardPolicy.STORED_AUTO
    return SessionKeyboardPolicy.fromStored(stored)
}
