/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Terminal color palette for one theme. 16 ANSI colors plus defaults and selection.
 * Applied to the underlying termlib renderer via [TerminalEmulator.applyColorScheme].
 */
data class TerminalPalette(
    val id: ThemeId,
    val background: Color,
    val foreground: Color,
    val cursor: Color,
    val selectionBackground: Color,
    val selectionForeground: Color,
    /** 16 ANSI colors in order: black, red, green, yellow, blue, magenta, cyan, white, then 8 bright variants. */
    val ansi: List<Color>
) {
    init {
        require(ansi.size == 16) { "ANSI palette must have 16 entries, got ${ansi.size}" }
    }

    /** ARGB int array ready for [TerminalEmulator.applyColorScheme]. */
    fun ansiArgbInts(): IntArray = IntArray(16) { i -> ansi[i].toArgb() }

    private fun Color.toArgb(): Int = android.graphics.Color.argb(
        (alpha * 255f).toInt().coerceIn(0, 255),
        (red * 255f).toInt().coerceIn(0, 255),
        (green * 255f).toInt().coerceIn(0, 255),
        (blue * 255f).toInt().coerceIn(0, 255)
    )
}
