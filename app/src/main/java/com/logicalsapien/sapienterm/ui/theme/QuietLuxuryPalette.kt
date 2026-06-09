/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color

/** Quiet Luxury — warm paper background, sage foreground, softened ANSI. */
val QuietLuxuryPalette: TerminalPalette = TerminalPalette(
    id = ThemeId.QUIET_LUXURY,
    background = Color(0xFFF6F2EA),
    foreground = Color(0xFF2B3A34),
    cursor = Color(0xFF7FB89A),
    selectionBackground = Color(0xFFD5E0D5),
    selectionForeground = Color(0xFF2B3A34),
    ansi = listOf(
        Color(0xFF2B3A34),
        Color(0xFFB5524C),
        Color(0xFF6B8E5A),
        Color(0xFFC09A3A),
        Color(0xFF3A6B93),
        Color(0xFF8A5A8A),
        Color(0xFF4A8A8A),
        Color(0xFF5A6057),
        Color(0xFF8A9088),
        Color(0xFFD37B76),
        Color(0xFF9AC089),
        Color(0xFFE3BE66),
        Color(0xFF5F94BC),
        Color(0xFFB685B7),
        Color(0xFF73B0B0),
        Color(0xFFA9ADA4)
    )
)
