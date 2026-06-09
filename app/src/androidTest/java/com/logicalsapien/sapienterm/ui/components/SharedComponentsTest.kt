/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.SapienThemePreview
import org.junit.Rule
import org.junit.Test

class SharedComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sapienCardRendersChildren() {
        compose.setContent {
            SapienThemePreview {
                SapienCard { Text("hello-card") }
            }
        }
        compose.onNodeWithText("hello-card").assertIsDisplayed()
    }

    @Test
    fun accentBarRendersInsideRow() {
        compose.setContent {
            SapienThemePreview {
                Row(modifier = Modifier.height(40.dp)) {
                    AccentBar(color = Color.Red, modifier = Modifier.width(3.dp))
                    Text("next-to-bar")
                }
            }
        }
        compose.onNodeWithText("next-to-bar").assertIsDisplayed()
    }

    @Test
    fun segmentedControlRendersAllLabels() {
        compose.setContent {
            SapienThemePreview {
                SegmentedControl(
                    options = listOf(
                        SegmentOption("a", "A"),
                        SegmentOption("b", "B")
                    ),
                    selected = "a",
                    onSelectedChange = {}
                )
            }
        }
        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun pillTabRendersLabel() {
        compose.setContent {
            SapienThemePreview {
                PillTab(
                    label = "prod",
                    dotColor = Color.Green,
                    selected = true,
                    onClick = {},
                    onClose = {}
                )
            }
        }
        compose.onNodeWithText("prod").assertIsDisplayed()
    }

    @Test
    fun themePreviewTileShowsDisplayName() {
        compose.setContent {
            SapienThemePreview {
                ThemePreviewTile(
                    tokens = com.logicalsapien.sapienterm.ui.theme.SoftProTheme,
                    selected = false,
                    onClick = {}
                )
            }
        }
        compose.onNodeWithText("Soft Pro").assertIsDisplayed()
    }

    @Test
    fun settingsToggleRowShowsTitleAndSummary() {
        compose.setContent {
            SapienThemePreview {
                com.logicalsapien.sapienterm.ui.components.settings.SettingsToggleRow(
                    title = "Do the thing",
                    summary = "Optional summary",
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }
        compose.onNodeWithText("Do the thing").assertIsDisplayed()
        compose.onNodeWithText("Optional summary").assertIsDisplayed()
    }

    @Test
    fun settingsNavRowShowsCurrentValueAndChevron() {
        compose.setContent {
            SapienThemePreview {
                com.logicalsapien.sapienterm.ui.components.settings.SettingsNavRow(
                    title = "Theme",
                    currentValue = "Neo-Terminal",
                    onClick = {}
                )
            }
        }
        compose.onNodeWithText("Theme").assertIsDisplayed()
        compose.onNodeWithText("Neo-Terminal").assertIsDisplayed()
    }

    @Test
    fun settingsActionRowShowsButtonLabel() {
        compose.setContent {
            SapienThemePreview {
                com.logicalsapien.sapienterm.ui.components.settings.SettingsActionRow(
                    title = "Export hosts",
                    actionLabel = "Export",
                    onAction = {}
                )
            }
        }
        compose.onNodeWithText("Export hosts").assertIsDisplayed()
        compose.onNodeWithText("Export").assertIsDisplayed()
    }
}
