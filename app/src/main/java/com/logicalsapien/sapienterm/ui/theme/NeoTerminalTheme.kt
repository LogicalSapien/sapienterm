/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val NeoTerminalTheme: ThemeTokens = ThemeTokens(
    id = ThemeId.NEO_TERMINAL,
    displayName = "Neo-Terminal",

    background = Color(0xFF07080A),
    surface = Color(0xFF0D0F14),
    surfaceBorder = Color(0xFF1A1D26),
    useShadowElevation = false,

    textPrimary = Color(0xFFE6E9EF),
    textMuted = Color(0xFF6B7289),
    textOnPrimary = Color(0xFF07080A),

    primary = Color(0xFFA6FF7A),
    primarySubtle = Color(0x33A6FF7A),

    statusOnline = Color(0xFFA6FF7A),
    statusIdle = Color(0xFF52606A),
    statusError = Color(0xFFFF6B6B),

    cornerSmall = 6.dp,
    cornerMedium = 8.dp,
    cornerLarge = 12.dp,

    fontFamilyBody = FontFamily.Monospace,
    fontFamilyHeading = FontFamily.Monospace,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF8A9BA8),
        HostCategoryColor.RED to Color(0xFFFF6B6B),
        HostCategoryColor.ORANGE to Color(0xFFF59E0B),
        HostCategoryColor.YELLOW to Color(0xFFEAD94C),
        HostCategoryColor.GREEN to Color(0xFFA6FF7A),
        HostCategoryColor.BLUE to Color(0xFF5EB1FF),
        HostCategoryColor.PURPLE to Color(0xFF8B5CF6),
        HostCategoryColor.VIOLET to Color(0xFFB06BFF)
    )
)
