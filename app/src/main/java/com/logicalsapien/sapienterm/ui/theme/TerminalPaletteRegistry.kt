/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

object TerminalPaletteRegistry {
    fun paletteFor(id: ThemeId): TerminalPalette = when (id) {
        ThemeId.NEO_TERMINAL -> NeoTerminalPalette
        ThemeId.QUIET_LUXURY -> QuietLuxuryPalette
        ThemeId.SOFT_PRO -> SoftProPalette
    }

    val all: List<TerminalPalette> = listOf(NeoTerminalPalette, QuietLuxuryPalette, SoftProPalette)
}
