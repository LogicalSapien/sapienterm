/*
 * SapienSSH: simple, powerful, open-source SSH client for Android
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

package com.logicalsapien.sapienssh.ui.screens.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienssh.data.entity.QuickCommand
import com.logicalsapien.sapienssh.service.ModifierLevel
import com.logicalsapien.sapienssh.service.TerminalBridge
import com.logicalsapien.sapienssh.service.TerminalKeyListener
import org.connectbot.terminal.VTermKey

/**
 * Height of the terminal bottom bar in dp.
 */
private const val BOTTOM_BAR_HEIGHT_DP = 44

/**
 * Teal accent color used for active/highlighted states.
 */
private val TealText = Color(0xFF80CBC4)
private val TealActive = Color(0xFF00897B)

/**
 * Enum representing which popup panel is currently visible above the bottom bar.
 */
private enum class ActivePanel {
    NONE,
    COMMANDS,
    MORE_KEYS
}

/**
 * A unified Termius-inspired terminal bottom bar that replaces the separate
 * QuickCommandToolbar, ExtendedKeyboardStrip, and KeyboardToggleBar.
 *
 * Layout (left to right):
 * [ Commands ] [ Up ] [ Down ] [ Ctrl ] [ More ] [ Keyboard ]
 *
 * Tapping Commands or More toggles a popup panel above the bar.
 * Only one panel can be visible at a time.
 *
 * @param bridge The terminal bridge for sending key events
 * @param quickCommands List of saved quick commands
 * @param showSoftwareKeyboard Whether the software keyboard is currently shown
 * @param onToggleKeyboard Callback to toggle the software keyboard
 * @param onSendQuickCommand Callback to send a quick command string
 * @param onInteraction Callback when any key/button is tapped (for auto-hide timer resets)
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalBottomBar(
    bridge: TerminalBridge,
    quickCommands: List<QuickCommand>,
    showSoftwareKeyboard: Boolean,
    onToggleKeyboard: () -> Unit,
    onSendQuickCommand: (String) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyHandler = bridge.keyHandler
    val modifierState by keyHandler.modifierState.collectAsState()

    var activePanel by remember { mutableStateOf(ActivePanel.NONE) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Popup panels appear ABOVE the bar
        // Commands panel
        AnimatedVisibility(
            visible = activePanel == ActivePanel.COMMANDS,
            enter = expandVertically(
                animationSpec = tween(200),
                expandFrom = Alignment.Bottom
            ),
            exit = shrinkVertically(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            CommandsPanel(
                quickCommands = quickCommands,
                onCommandClick = { command ->
                    onSendQuickCommand(command)
                    activePanel = ActivePanel.NONE
                    onInteraction()
                }
            )
        }

        // More keys panel
        AnimatedVisibility(
            visible = activePanel == ActivePanel.MORE_KEYS,
            enter = expandVertically(
                animationSpec = tween(200),
                expandFrom = Alignment.Bottom
            ),
            exit = shrinkVertically(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            MoreKeysPanel(
                bridge = bridge,
                onInteraction = onInteraction
            )
        }

        // Thin top divider line
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // The main bottom bar
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(BOTTOM_BAR_HEIGHT_DP.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Commands button
                IconButton(
                    onClick = {
                        activePanel = if (activePanel == ActivePanel.COMMANDS) {
                            ActivePanel.NONE
                        } else {
                            ActivePanel.COMMANDS
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Quick commands",
                        tint = if (activePanel == ActivePanel.COMMANDS) TealActive else TealText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. Up arrow
                IconButton(
                    onClick = {
                        keyHandler.sendPressedKey(VTermKey.UP)
                        onInteraction()
                    }
                ) {
                    Text(
                        text = "\u2191",
                        style = MaterialTheme.typography.titleMedium,
                        color = TealText
                    )
                }

                // 3. Down arrow
                IconButton(
                    onClick = {
                        keyHandler.sendPressedKey(VTermKey.DOWN)
                        onInteraction()
                    }
                ) {
                    Text(
                        text = "\u2193",
                        style = MaterialTheme.typography.titleMedium,
                        color = TealText
                    )
                }

                // 4. Ctrl modifier toggle
                val ctrlLevel = modifierState.ctrlState
                val ctrlBgColor = when (ctrlLevel) {
                    ModifierLevel.OFF -> Color.Transparent
                    ModifierLevel.TRANSIENT -> TealActive.copy(alpha = 0.5f)
                    ModifierLevel.LOCKED -> TealActive.copy(alpha = 0.8f)
                }
                val ctrlTextColor = when (ctrlLevel) {
                    ModifierLevel.OFF -> TealText
                    ModifierLevel.TRANSIENT -> Color.White
                    ModifierLevel.LOCKED -> Color.White
                }

                Surface(
                    onClick = {
                        keyHandler.metaPress(TerminalKeyListener.OUR_CTRL_ON, true)
                        onInteraction()
                    },
                    shape = RoundedCornerShape(6.dp),
                    color = ctrlBgColor,
                    modifier = Modifier.height(32.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "Ctrl",
                            style = MaterialTheme.typography.labelMedium,
                            color = ctrlTextColor
                        )
                    }
                }

                // 5. More button (three dots)
                IconButton(
                    onClick = {
                        activePanel = if (activePanel == ActivePanel.MORE_KEYS) {
                            ActivePanel.NONE
                        } else {
                            ActivePanel.MORE_KEYS
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "More keys",
                        tint = if (activePanel == ActivePanel.MORE_KEYS) TealActive else TealText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 6. Keyboard toggle
                IconButton(
                    onClick = onToggleKeyboard
                ) {
                    Icon(
                        imageVector = if (showSoftwareKeyboard) {
                            Icons.Default.KeyboardHide
                        } else {
                            Icons.Default.Keyboard
                        },
                        contentDescription = if (showSoftwareKeyboard) "Hide keyboard" else "Show keyboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Popup panel showing saved Quick Commands as a vertical list.
 * Appears above the bottom bar when the commands button is toggled.
 */
@Composable
private fun CommandsPanel(
    quickCommands: List<QuickCommand>,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (quickCommands.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No quick commands saved",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(vertical = 4.dp)
            ) {
                items(
                    items = quickCommands,
                    key = { it.id }
                ) { command ->
                    CommandItem(
                        command = command,
                        onClick = { onCommandClick(command.command) }
                    )
                }
            }
        }
    }
}

/**
 * A single quick command item in the commands panel.
 */
@Composable
private fun CommandItem(
    command: QuickCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = command.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = command.command,
                style = MaterialTheme.typography.bodySmall,
                color = TealText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Popup panel showing all extended keys in a flow/grid layout.
 * Appears above the bottom bar when the more button is toggled.
 *
 * Keys: Esc, Tab, Alt, left arrow, right arrow, |, -, /, ~, Home, End, PgUp, PgDn
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreKeysPanel(
    bridge: TerminalBridge,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyHandler = bridge.keyHandler
    val modifierState by keyHandler.modifierState.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Esc
            MoreKeyChip(label = "Esc", onClick = {
                keyHandler.sendEscape()
                onInteraction()
            })

            // Tab
            MoreKeyChip(label = "Tab", onClick = {
                keyHandler.sendTab()
                onInteraction()
            })

            // Alt modifier
            val altLevel = modifierState.altState
            MoreKeyChip(
                label = "Alt",
                isActive = altLevel != ModifierLevel.OFF,
                onClick = {
                    keyHandler.metaPress(TerminalKeyListener.OUR_ALT_ON, true)
                    onInteraction()
                }
            )

            // Left arrow
            MoreKeyChip(label = "\u2190", onClick = {
                keyHandler.sendPressedKey(VTermKey.LEFT)
                onInteraction()
            })

            // Right arrow
            MoreKeyChip(label = "\u2192", onClick = {
                keyHandler.sendPressedKey(VTermKey.RIGHT)
                onInteraction()
            })

            // Pipe
            MoreKeyChip(label = "|", onClick = {
                bridge.injectString("|")
                onInteraction()
            })

            // Dash
            MoreKeyChip(label = "-", onClick = {
                bridge.injectString("-")
                onInteraction()
            })

            // Slash
            MoreKeyChip(label = "/", onClick = {
                bridge.injectString("/")
                onInteraction()
            })

            // Tilde
            MoreKeyChip(label = "~", onClick = {
                bridge.injectString("~")
                onInteraction()
            })

            // Home
            MoreKeyChip(label = "Home", onClick = {
                keyHandler.sendPressedKey(VTermKey.HOME)
                onInteraction()
            })

            // End
            MoreKeyChip(label = "End", onClick = {
                keyHandler.sendPressedKey(VTermKey.END)
                onInteraction()
            })

            // PgUp
            MoreKeyChip(label = "PgUp", onClick = {
                keyHandler.sendPressedKey(VTermKey.PAGEUP)
                onInteraction()
            })

            // PgDn
            MoreKeyChip(label = "PgDn", onClick = {
                keyHandler.sendPressedKey(VTermKey.PAGEDOWN)
                onInteraction()
            })
        }
    }
}

/**
 * A small tappable chip for the More keys panel.
 */
@Composable
private fun MoreKeyChip(
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) TealActive.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (isActive) Color.White else TealText

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier.height(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
        }
    }
}
