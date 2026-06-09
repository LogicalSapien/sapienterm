/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val QuietLuxuryTheme: ThemeTokens = ThemeTokens(
    id = ThemeId.QUIET_LUXURY,
    displayName = "Quiet Luxury",

    background = Color(0xFF0F1612),
    surface = Color(0xFF141C18),
    surfaceBorder = Color(0xFF243028),
    useShadowElevation = false,

    textPrimary = Color(0xFFE8EFE9),
    textMuted = Color(0xFF8A9A90),
    textOnPrimary = Color(0xFF0F1612),

    primary = Color(0xFF7FB89A),
    primarySubtle = Color(0x337FB89A),

    statusOnline = Color(0xFF7FB89A),
    statusIdle = Color(0xFF52606A),
    statusError = Color(0xFFC77B7B),

    cornerSmall = 14.dp,
    cornerMedium = 16.dp,
    cornerLarge = 20.dp,

    fontFamilyBody = FontFamily.SansSerif,
    fontFamilyHeading = FontFamily.SansSerif,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF6B7871),
        HostCategoryColor.RED to Color(0xFFC77B7B),
        HostCategoryColor.ORANGE to Color(0xFFCB9766),
        HostCategoryColor.YELLOW to Color(0xFFC4B06B),
        HostCategoryColor.GREEN to Color(0xFF7FB89A),
        HostCategoryColor.BLUE to Color(0xFF7AA2C4),
        HostCategoryColor.PURPLE to Color(0xFF9B86C2),
        HostCategoryColor.VIOLET to Color(0xFFB48FCB)
    )
)
