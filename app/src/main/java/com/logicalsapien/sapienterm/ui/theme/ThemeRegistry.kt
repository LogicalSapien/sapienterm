/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

/** Maps a [ThemeId] to its concrete dark and light [ThemeTokens]. */
object ThemeRegistry {
    private val darkById: Map<ThemeId, ThemeTokens> = mapOf(
        ThemeId.NEO_TERMINAL to NeoTerminalTheme,
        ThemeId.QUIET_LUXURY to QuietLuxuryTheme,
        ThemeId.SOFT_PRO to SoftProTheme
    )

    private val lightById: Map<ThemeId, ThemeTokens> = mapOf(
        ThemeId.NEO_TERMINAL to NeoTerminalThemeLight,
        ThemeId.QUIET_LUXURY to QuietLuxuryThemeLight,
        ThemeId.SOFT_PRO to SoftProThemeLight
    )

    /** Dark tokens are the canonical list for the palette picker preview. */
    val all: List<ThemeTokens> =
        darkById.values.sortedBy { it.id.displayOrder }

    fun tokensFor(id: ThemeId): ThemeTokens = darkById.getValue(id)

    fun tokensFor(id: ThemeId, dark: Boolean): ThemeTokens =
        if (dark) darkById.getValue(id) else lightById.getValue(id)
}
