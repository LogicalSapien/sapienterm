/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SapienThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun previewWrapperProvidesInjectedTokens() {
        compose.setContent {
            SapienThemePreview(tokens = SoftProTheme) {
                val tokens = SapienTheme.tokens
                Text(text = "theme=${tokens.id.storageString}")
            }
        }
        compose.onNodeWithText("theme=soft_pro").assertIsDisplayed()
    }

    @Test
    fun previewWrapperProvidesCompactDensity() {
        compose.setContent {
            SapienThemePreview(density = DensityTokens.Compact) {
                val density = SapienTheme.density
                Text(text = "density=${density.mode.storageString}")
            }
        }
        compose.onNodeWithText("density=compact").assertIsDisplayed()
    }

    @Test
    fun registryReturnsAllThreeThemes() {
        assertEquals(3, ThemeRegistry.all.size)
    }
}
