/*
 * SapienTerm — theme token system
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0.
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A complete design-token set for one theme. Every SapienTerm surface should be
 * describable entirely in terms of these tokens. To add a new theme, build a new
 * [ThemeTokens] val and register it in [ThemeId].
 */
data class ThemeTokens(
    val id: ThemeId,
    val displayName: String,

    // Surface & text
    val background: Color,
    val surface: Color,
    val surfaceBorder: Color, // Color.Transparent if theme uses shadows instead
    val useShadowElevation: Boolean,

    val textPrimary: Color,
    val textMuted: Color,
    val textOnPrimary: Color,

    // Accents
    val primary: Color,
    val primarySubtle: Color, // low-alpha tint, for selected backgrounds

    // Status
    val statusOnline: Color,
    val statusIdle: Color,
    val statusError: Color,

    // Shape
    val cornerSmall: Dp,
    val cornerMedium: Dp,
    val cornerLarge: Dp,

    // Type
    val fontFamilyBody: FontFamily,
    val fontFamilyHeading: FontFamily,
    val fontFamilyMono: FontFamily,

    // Per-category color palette
    val categoryColors: Map<HostCategoryColor, Color>
) {
    /** Convenience lookup that defaults to [HostCategoryColor.GRAY] if a color is somehow missing. */
    fun colorFor(category: HostCategoryColor): Color =
        categoryColors[category] ?: categoryColors.getValue(HostCategoryColor.GRAY)

    companion object {
        val ZeroCorner: Dp = 0.dp
    }
}
