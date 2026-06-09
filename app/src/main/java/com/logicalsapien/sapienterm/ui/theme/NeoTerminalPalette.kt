/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color

/** Neo-Terminal — phosphor-green on near-black with slightly desaturated ANSI. */
val NeoTerminalPalette: TerminalPalette = TerminalPalette(
    id = ThemeId.NEO_TERMINAL,
    background = Color(0xFF07080A),
    foreground = Color(0xFFE6E9EF),
    cursor = Color(0xFFA6FF7A),
    selectionBackground = Color(0xFF1F3322),
    selectionForeground = Color(0xFFE6E9EF),
    ansi = listOf(
        Color(0xFF07080A), // black
        Color(0xFFFF6B6B), // red
        Color(0xFFA6FF7A), // green (accent)
        Color(0xFFEAD94C), // yellow
        Color(0xFF5EB1FF), // blue
        Color(0xFFB06BFF), // magenta
        Color(0xFF6FE8CA), // cyan
        Color(0xFFC9CCD1), // white
        Color(0xFF52606A), // bright black
        Color(0xFFFF8F8F),
        Color(0xFFC7FFA6),
        Color(0xFFF5E680),
        Color(0xFF8ECCFF),
        Color(0xFFC89AFF),
        Color(0xFF9DF3DE),
        Color(0xFFFFFFFF)
    )
)
