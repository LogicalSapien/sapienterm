/*
 * SapienTerm — theme token system
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.logicalsapien.sapienterm.ui.theme

enum class ThemeId(val storageString: String, val displayOrder: Int) {
    NEO_TERMINAL("neo_terminal", 0),
    QUIET_LUXURY("quiet_luxury", 1),
    SOFT_PRO("soft_pro", 2);

    companion object {
        val DEFAULT: ThemeId = NEO_TERMINAL

        fun fromStorageString(value: String?): ThemeId {
            if (value == null) return DEFAULT
            return entries.firstOrNull { it.storageString.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}
