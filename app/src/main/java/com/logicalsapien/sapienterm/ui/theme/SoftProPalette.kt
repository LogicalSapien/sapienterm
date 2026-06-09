/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color

/** Soft Pro — deep violet-tinted dark with saturated ANSI. */
val SoftProPalette: TerminalPalette = TerminalPalette(
    id = ThemeId.SOFT_PRO,
    background = Color(0xFF111018),
    foreground = Color(0xFFEDEBF5),
    cursor = Color(0xFF7C3AED),
    selectionBackground = Color(0xFF352E55),
    selectionForeground = Color(0xFFEDEBF5),
    ansi = listOf(
        Color(0xFF1A1826),
        Color(0xFFFF5277),
        Color(0xFF50E3A4),
        Color(0xFFFFC857),
        Color(0xFF5D8CFF),
        Color(0xFFA57BFF),
        Color(0xFF5DD6E8),
        Color(0xFFCFC9E4),
        Color(0xFF5A5475),
        Color(0xFFFF7D99),
        Color(0xFF84F0C5),
        Color(0xFFFFDB8F),
        Color(0xFF8CB0FF),
        Color(0xFFC3A5FF),
        Color(0xFF9EECF5),
        Color(0xFFFFFFFF)
    )
)
