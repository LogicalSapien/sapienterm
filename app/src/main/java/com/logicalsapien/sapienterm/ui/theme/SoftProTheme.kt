/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

val SoftProTheme: ThemeTokens = ThemeTokens(
    id = ThemeId.SOFT_PRO,
    displayName = "Soft Pro",

    background = Color(0xFFFAFBFF),
    surface = Color(0xFFFFFFFF),
    surfaceBorder = Color(0x11141E3C),
    useShadowElevation = true,

    textPrimary = Color(0xFF1A1F2E),
    textMuted = Color(0xFF6B7280),
    textOnPrimary = Color(0xFFFFFFFF),

    primary = Color(0xFF7C3AED),
    primarySubtle = Color(0x337C3AED),

    statusOnline = Color(0xFF10B981),
    statusIdle = Color(0xFF9CA3AF),
    statusError = Color(0xFFEF4444),

    cornerSmall = 16.dp,
    cornerMedium = 20.dp,
    cornerLarge = 24.dp,

    fontFamilyBody = FontFamily.SansSerif,
    fontFamilyHeading = FontFamily.SansSerif,
    fontFamilyMono = FontFamily.Monospace,

    categoryColors = mapOf(
        HostCategoryColor.GRAY to Color(0xFF9CA3AF),
        HostCategoryColor.RED to Color(0xFFEF4444),
        HostCategoryColor.ORANGE to Color(0xFFF97316),
        HostCategoryColor.YELLOW to Color(0xFFEAB308),
        HostCategoryColor.GREEN to Color(0xFF10B981),
        HostCategoryColor.BLUE to Color(0xFF0EA5E9),
        HostCategoryColor.PURPLE to Color(0xFF7C3AED),
        HostCategoryColor.VIOLET to Color(0xFFA855F7)
    )
)
