/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logicalsapien.sapienterm.ui.screens.console

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.service.ModifierLevel
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.service.TerminalKeyListener
import com.logicalsapien.sapienterm.util.ExtendedKey
import com.logicalsapien.sapienterm.util.ExtendedKeyboardConfig
import org.connectbot.terminal.VTermKey

/**
 * Height of the extended keyboard strip in dp.
 */
const val EXTENDED_KEYBOARD_STRIP_HEIGHT_DP = 38

private const val STRIP_KEY_MIN_WIDTH_DP = 40
private const val STRIP_OPACITY = 0.65f

/**
 * A Termius-inspired extended keyboard strip that provides quick access to special keys.
 * Sits as a horizontal scrollable row of compact key buttons above the soft keyboard.
 *
 * Modifier keys (Ctrl, Alt) toggle on/off -- when active, the next regular key press
 * combines with the modifier (handled by TerminalKeyListener's existing metaPress system).
 *
 * @param bridge The terminal bridge for sending key events
 * @param config Configuration of which keys are enabled
 * @param onInteraction Callback when any key is tapped (resets auto-hide timer)
 * @param modifier Optional modifier
 */
@Composable
fun ExtendedKeyboardStrip(
    bridge: TerminalBridge,
    config: ExtendedKeyboardConfig,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyHandler = bridge.keyHandler
    val modifierState by keyHandler.modifierState.collectAsState()

    val enabledKeys = ExtendedKey.entries.filter { config.isKeyEnabled(it) }
    if (enabledKeys.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = STRIP_OPACITY),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(EXTENDED_KEYBOARD_STRIP_HEIGHT_DP.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            enabledKeys.forEach { key ->
                when (key) {
                    ExtendedKey.CTRL -> {
                        val level = modifierState.ctrlState
                        StripModifierKey(
                            label = key.label,
                            level = level,
                            onClick = {
                                keyHandler.metaPress(TerminalKeyListener.OUR_CTRL_ON, true)
                                onInteraction()
                            }
                        )
                    }
                    ExtendedKey.ALT -> {
                        val level = modifierState.altState
                        StripModifierKey(
                            label = key.label,
                            level = level,
                            onClick = {
                                keyHandler.metaPress(TerminalKeyListener.OUR_ALT_ON, true)
                                onInteraction()
                            }
                        )
                    }
                    ExtendedKey.ESC -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendEscape()
                            onInteraction()
                        })
                    }
                    ExtendedKey.TAB -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendTab()
                            onInteraction()
                        })
                    }
                    ExtendedKey.ARROW_UP -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.UP)
                            onInteraction()
                        })
                    }
                    ExtendedKey.ARROW_DOWN -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.DOWN)
                            onInteraction()
                        })
                    }
                    ExtendedKey.ARROW_LEFT -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.LEFT)
                            onInteraction()
                        })
                    }
                    ExtendedKey.ARROW_RIGHT -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.RIGHT)
                            onInteraction()
                        })
                    }
                    ExtendedKey.PIPE -> {
                        StripKey(label = key.label, onClick = {
                            bridge.injectString("|")
                            onInteraction()
                        })
                    }
                    ExtendedKey.DASH -> {
                        StripKey(label = key.label, onClick = {
                            bridge.injectString("-")
                            onInteraction()
                        })
                    }
                    ExtendedKey.SLASH -> {
                        StripKey(label = key.label, onClick = {
                            bridge.injectString("/")
                            onInteraction()
                        })
                    }
                    ExtendedKey.TILDE -> {
                        StripKey(label = key.label, onClick = {
                            bridge.injectString("~")
                            onInteraction()
                        })
                    }
                    ExtendedKey.HOME -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.HOME)
                            onInteraction()
                        })
                    }
                    ExtendedKey.END -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.END)
                            onInteraction()
                        })
                    }
                    ExtendedKey.PGUP -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.PAGEUP)
                            onInteraction()
                        })
                    }
                    ExtendedKey.PGDN -> {
                        StripKey(label = key.label, onClick = {
                            keyHandler.sendPressedKey(VTermKey.PAGEDOWN)
                            onInteraction()
                        })
                    }
                }
            }
        }
    }
}

/**
 * A single key button in the extended keyboard strip.
 */
@Composable
private fun StripKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tealText = Color(0xFF80CBC4) // Teal-tinted text

    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = STRIP_KEY_MIN_WIDTH_DP.dp)
            .height(EXTENDED_KEYBOARD_STRIP_HEIGHT_DP.dp),
        shape = RectangleShape,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        color = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tealText
            )
        }
    }
}

/**
 * A modifier key (Ctrl/Alt) in the extended keyboard strip.
 * Shows visual feedback for toggle state (off / transient / locked).
 */
@Composable
private fun StripModifierKey(
    label: String,
    level: ModifierLevel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tealText = Color(0xFF80CBC4)
    val tealActive = Color(0xFF00897B)

    val bgColor = when (level) {
        ModifierLevel.OFF -> Color.Transparent
        ModifierLevel.TRANSIENT -> tealActive.copy(alpha = 0.5f)
        ModifierLevel.LOCKED -> tealActive.copy(alpha = 0.8f)
    }

    val textColor = when (level) {
        ModifierLevel.OFF -> tealText
        ModifierLevel.TRANSIENT -> Color.White
        ModifierLevel.LOCKED -> Color.White
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = STRIP_KEY_MIN_WIDTH_DP.dp)
            .height(EXTENDED_KEYBOARD_STRIP_HEIGHT_DP.dp),
        shape = RectangleShape,
        border = BorderStroke(
            width = if (level != ModifierLevel.OFF) 1.dp else 0.5.dp,
            color = if (level != ModifierLevel.OFF) tealActive else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        color = bgColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}
