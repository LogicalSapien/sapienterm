/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 *
 * Light-mode counterparts to the three dark palettes. Each light variant
 * keeps the same [ThemeId] and accent family as its dark twin but swaps
 * the surface/text colors so the same palette identity works in both
 * modes.
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val NeoTerminalThemeLight: ThemeTokens = ThemeTokens(
    id = ThemeId.NEO_TERMINAL,
    displayName = "Neo-Terminal",

    background = Color(0xFFFCFDFB),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFE1E6DE),
    useShadowElevation = false,

    textPrimary = Color(0xFF050A06),
    textMuted = Color(0xFF535B52),
    textOnPrimary = Color(0xFFFFFFFF),

    primary = Color(0xFF3FA14E),
    primarySubtle = Color(0x333FA14E),

    statusOnline = Color(0xFF3FA14E),
    statusIdle = Color(0xFF9AA49D),
    statusError = Color(0xFFD93A3A),

    cornerSmall = 6.dp,
    cornerMedium = 8.dp,
    cornerLarge = 12.dp,

    fontFamilyBody = FontFamily.Monospace,
    fontFamilyHeading = FontFamily.Monospace,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF6E7781),
        HostCategoryColor.RED to Color(0xFFD93A3A),
        HostCategoryColor.ORANGE to Color(0xFFB46100),
        HostCategoryColor.YELLOW to Color(0xFFB58800),
        HostCategoryColor.GREEN to Color(0xFF3FA14E),
        HostCategoryColor.BLUE to Color(0xFF2E78D2),
        HostCategoryColor.PURPLE to Color(0xFF6D3BC9),
        HostCategoryColor.VIOLET to Color(0xFF8140E8)
    )
)

val QuietLuxuryThemeLight: ThemeTokens = ThemeTokens(
    id = ThemeId.QUIET_LUXURY,
    displayName = "Quiet Luxury",

    background = Color(0xFFFDFCF8),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFE6DFCE),
    useShadowElevation = true,

    textPrimary = Color(0xFF15160E),
    textMuted = Color(0xFF5C5A50),
    textOnPrimary = Color(0xFFFFFFFF),

    primary = Color(0xFF5E7A4E),
    primarySubtle = Color(0x225E7A4E),

    statusOnline = Color(0xFF5E7A4E),
    statusIdle = Color(0xFFA89F8A),
    statusError = Color(0xFFB5483F),

    cornerSmall = 6.dp,
    cornerMedium = 10.dp,
    cornerLarge = 14.dp,

    fontFamilyBody = FontFamily.SansSerif,
    fontFamilyHeading = FontFamily.Serif,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF7A7668),
        HostCategoryColor.RED to Color(0xFFB5483F),
        HostCategoryColor.ORANGE to Color(0xFFC47A2A),
        HostCategoryColor.YELLOW to Color(0xFFBFA13F),
        HostCategoryColor.GREEN to Color(0xFF5E7A4E),
        HostCategoryColor.BLUE to Color(0xFF4E6A8A),
        HostCategoryColor.PURPLE to Color(0xFF6B4D8A),
        HostCategoryColor.VIOLET to Color(0xFF855D9F)
    )
)

val SoftProThemeLight: ThemeTokens = ThemeTokens(
    id = ThemeId.SOFT_PRO,
    displayName = "Soft Pro",

    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0xFFE4E5EC),
    useShadowElevation = true,

    textPrimary = Color(0xFF0F1019),
    textMuted = Color(0xFF5A5D69),
    textOnPrimary = Color(0xFFFFFFFF),

    primary = Color(0xFF6A4BD6),
    primarySubtle = Color(0x226A4BD6),

    statusOnline = Color(0xFF2CAA63),
    statusIdle = Color(0xFF9EA2B1),
    statusError = Color(0xFFD7413F),

    cornerSmall = 8.dp,
    cornerMedium = 12.dp,
    cornerLarge = 16.dp,

    fontFamilyBody = FontFamily.SansSerif,
    fontFamilyHeading = FontFamily.SansSerif,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF6A6D7A),
        HostCategoryColor.RED to Color(0xFFD7413F),
        HostCategoryColor.ORANGE to Color(0xFFBF6A0A),
        HostCategoryColor.YELLOW to Color(0xFFBD9500),
        HostCategoryColor.GREEN to Color(0xFF2CAA63),
        HostCategoryColor.BLUE to Color(0xFF2F6BE0),
        HostCategoryColor.PURPLE to Color(0xFF6A4BD6),
        HostCategoryColor.VIOLET to Color(0xFF8651D6)
    )
)
