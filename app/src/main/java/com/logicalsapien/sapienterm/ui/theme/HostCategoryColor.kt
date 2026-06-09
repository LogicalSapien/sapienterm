/*
 * SapienTerm — theme token system
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.logicalsapien.sapienterm.ui.theme

/**
 * Category colors a user can assign to a host. Stored on [Host.color] as a lowercase string.
 * Each concrete [ThemeTokens] provides a per-theme tint for every entry.
 */
enum class HostCategoryColor(
    val storageString: String,
    val displayOrder: Int
) {
    GRAY("gray", 0),
    RED("red", 1),
    ORANGE("orange", 2),
    YELLOW("yellow", 3),
    GREEN("green", 4),
    BLUE("blue", 5),
    PURPLE("purple", 6),
    VIOLET("violet", 7);

    companion object {
        fun fromStorageString(value: String?): HostCategoryColor {
            if (value == null) return GRAY
            return entries.firstOrNull { it.storageString.equals(value, ignoreCase = true) } ?: GRAY
        }

        /**
         * Resolve a category color for a host: if the user has assigned one, use it;
         * otherwise derive a stable, bright accent from [seed] (e.g. nickname) so the
         * list isn't a wall of grey bars. GRAY is reserved for explicit user choice.
         */
        fun resolveWithFallback(value: String?, seed: String): HostCategoryColor {
            val explicit = value?.let { s ->
                entries.firstOrNull { it.storageString.equals(s, ignoreCase = true) }
            }
            if (explicit != null) return explicit
            val bright = listOf(BLUE, GREEN, PURPLE, ORANGE, VIOLET, RED, YELLOW)
            val idx = (seed.hashCode().ushr(1)) % bright.size
            return bright[idx]
        }
    }
}
