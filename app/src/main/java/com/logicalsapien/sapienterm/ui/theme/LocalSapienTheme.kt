/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The active theme token set. Default is [NeoTerminalTheme] if no [SapienTheme] wraps the tree —
 * which should not happen in production; preview composables still get sensible defaults.
 */
val LocalSapienTheme = staticCompositionLocalOf { NeoTerminalTheme }

/**
 * The active density tokens. Default is [DensityTokens.Comfortable].
 */
val LocalSapienDensity = staticCompositionLocalOf { DensityTokens.Comfortable }

/**
 * Convenience accessors. Use `SapienTheme.current` inside composables.
 */
object SapienTheme {
    val tokens: ThemeTokens
        @Composable @ReadOnlyComposable
        get() = LocalSapienTheme.current

    val density: DensityTokens
        @Composable @ReadOnlyComposable
        get() = LocalSapienDensity.current
}
