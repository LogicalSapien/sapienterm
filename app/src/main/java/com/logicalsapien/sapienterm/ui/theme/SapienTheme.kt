/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import com.logicalsapien.sapienterm.data.prefs.AppearancePreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SapienThemeEntryPoint {
    fun appearancePreferences(): AppearancePreferences
}

/**
 * Wraps [content] with the active [ThemeTokens] and [DensityTokens] pulled from
 * [AppearancePreferences]. Place as high in the tree as possible. Safe to use
 * alongside the legacy `SapienTermTheme`.
 *
 * For previews, use [SapienThemePreview] which takes explicit tokens.
 */
@Composable
fun SapienTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        SapienThemeEntryPoint::class.java
    )
    val prefs = entryPoint.appearancePreferences()

    val themeId by prefs.themeId.collectAsState(initial = ThemeId.DEFAULT)
    val densityMode by prefs.density.collectAsState(initial = DensityMode.DEFAULT)

    val tokens = ThemeRegistry.tokensFor(themeId, dark = darkTheme)
    val density = DensityTokens.forMode(densityMode)

    CompositionLocalProvider(
        LocalSapienTheme provides tokens,
        LocalSapienDensity provides density
    ) {
        MaterialTheme(colorScheme = tokens.toMaterialColorScheme(), content = content)
    }
}

/**
 * Preview-friendly wrapper: caller supplies the tokens directly so no Hilt is required.
 */
@Composable
fun SapienThemePreview(
    tokens: ThemeTokens = NeoTerminalTheme,
    density: DensityTokens = DensityTokens.Comfortable,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSapienTheme provides tokens,
        LocalSapienDensity provides density
    ) {
        MaterialTheme(colorScheme = tokens.toMaterialColorScheme(), content = content)
    }
}

private fun ThemeTokens.toMaterialColorScheme() = if (background.luminance() < 0.5f) {
    darkColorScheme(
        primary = primary,
        onPrimary = textOnPrimary,
        primaryContainer = primarySubtle,
        onPrimaryContainer = textPrimary,
        secondary = primary,
        onSecondary = textOnPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = textMuted,
        outline = surfaceBorder,
        outlineVariant = surfaceBorder,
        error = statusError,
        onError = Color.White
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = textOnPrimary,
        primaryContainer = primarySubtle,
        onPrimaryContainer = textPrimary,
        secondary = primary,
        onSecondary = textOnPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surface,
        onSurfaceVariant = textMuted,
        outline = surfaceBorder,
        outlineVariant = surfaceBorder,
        error = statusError,
        onError = Color.White
    )
}
