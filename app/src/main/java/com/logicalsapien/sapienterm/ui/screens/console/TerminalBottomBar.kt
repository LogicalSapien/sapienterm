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

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.CommandHistory
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.service.ModifierLevel
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.service.TerminalKeyListener
import org.connectbot.terminal.VTermKey

/**
 * Height of the terminal bottom bar in dp.
 * 48dp matches Material minimum touch target so strip icons are not vertically clipped.
 */
private const val BOTTOM_BAR_HEIGHT_DP = 48

/**
 * Theme-aware teal accent colors used for active/highlighted states.
 * Returns darker teal on light backgrounds for better visibility.
 */
@Composable
private fun tealText(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun tealActive(): Color = MaterialTheme.colorScheme.primary

/**
 * A unified Termius-inspired terminal bottom bar that replaces the separate
 * QuickCommandToolbar, ExtendedKeyboardStrip, and KeyboardToggleBar.
 *
 * Layout: optional **horizontally scrollable** custom shortcut strip only; **pinned** **More** (⋯) and **keyboard** toggle.
 * All other session actions (commands, history, arrows, Enter, Ctrl, text input, tmux ^B/^D when applicable, …)
 * live in the **More** panel above the bar.
 *
 * Tapping Commands or More toggles a popup panel above the bar.
 * Only one panel can be visible at a time.
 *
 * @param bridge The terminal bridge for sending key events
 * @param quickCommands List of saved quick commands
 * @param commandHistory List of command history entries for the current host
 * @param showSoftwareKeyboard Whether the software keyboard is currently shown
 * @param keyboardLockedOff Whether the keyboard is locked off for this session
 * @param onToggleKeyboard Callback to toggle the software keyboard
 * @param onToggleKeyboardLock Callback to toggle the keyboard lock for this session
 * @param onVoiceInput Callback to open voice-friendly text input dialog
 * @param onSendQuickCommand Callback to send a quick command string
 * @param onHistoryCommandClick Callback when a history command is tapped for re-send
 * @param onClearHistory Callback to clear command history for current host
 * @param onInteraction Callback when any key/button is tapped (for auto-hide timer resets)
 * @param bottomBarStripBuiltin When [customShortcutStrip] is null: adds Zellij/tmux keys per preset; ignored when [customShortcutStrip] is non-null.
 * @param customShortcutStrip Optional user-defined shortcuts (replaces builtin Zellij/tmux strip).
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalBottomBar(
    bridge: TerminalBridge,
    quickCommands: List<QuickCommand>,
    commandHistory: List<CommandHistory>,
    showSoftwareKeyboard: Boolean,
    keyboardLockedOff: Boolean,
    onToggleKeyboard: () -> Unit,
    onToggleKeyboardLock: () -> Unit,
    onVoiceInput: () -> Unit,
    onSendQuickCommand: (String) -> Unit,
    onHistoryCommandClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBarStripBuiltin: TerminalBottomBarPreset = TerminalBottomBarPreset.DEFAULT,
    customShortcutStrip: List<BottomBarShortcutAction>? = null
) {
    val keyHandler = bridge.keyHandler
    val modifierState by keyHandler.modifierState.collectAsState()

    var activePanel by remember { mutableStateOf(BottomBarPanel.NONE) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Popup panels appear ABOVE the bar
        // Commands panel (with search/filter)
        AnimatedVisibility(
            visible = activePanel == BottomBarPanel.COMMANDS,
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
                commandHistory = commandHistory,
                onCommandClick = { command ->
                    onSendQuickCommand(command)
                    activePanel = BottomBarPanel.NONE
                    onInteraction()
                }
            )
        }

        // History panel
        AnimatedVisibility(
            visible = activePanel == BottomBarPanel.HISTORY,
            enter = expandVertically(
                animationSpec = tween(200),
                expandFrom = Alignment.Bottom
            ),
            exit = shrinkVertically(
                animationSpec = tween(200),
                shrinkTowards = Alignment.Bottom
            )
        ) {
            HistoryPanel(
                commandHistory = commandHistory,
                onCommandClick = { command ->
                    onHistoryCommandClick(command)
                    activePanel = BottomBarPanel.NONE
                    onInteraction()
                },
                onClearHistory = onClearHistory
            )
        }

        // More keys panel
        AnimatedVisibility(
            visible = activePanel == BottomBarPanel.MORE_KEYS,
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
                bottomBarStripBuiltin = bottomBarStripBuiltin,
                customShortcutStrip = customShortcutStrip,
                onInteraction = onInteraction,
                onOpenCommandsPanel = {
                    activePanel =
                        if (activePanel == BottomBarPanel.COMMANDS) {
                            BottomBarPanel.NONE
                        } else {
                            BottomBarPanel.COMMANDS
                        }
                },
                onOpenHistoryPanel = {
                    activePanel =
                        if (activePanel == BottomBarPanel.HISTORY) {
                            BottomBarPanel.NONE
                        } else {
                            BottomBarPanel.HISTORY
                        }
                },
                onOpenFloatingTextInput = {
                    activePanel = BottomBarPanel.NONE
                    onVoiceInput()
                },
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barScrollState = rememberScrollState()
                val scrollValue = barScrollState.value
                val scrollMax = barScrollState.maxValue
                val edgeFadeColor = MaterialTheme.colorScheme.surfaceVariant
                val stripOverflowHint = stringResource(R.string.bottom_bar_strip_scroll_overflow_state)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .horizontalScroll(barScrollState)
                        .height(BOTTOM_BAR_HEIGHT_DP.dp)
                        .semantics {
                            if (scrollMax > 0 && (scrollValue > 0 || scrollValue < scrollMax)) {
                                stateDescription = stripOverflowHint
                            }
                        }
                        .drawWithContent {
                            drawContent()
                            val edge = 20.dp.toPx()
                            val w = size.width
                            val h = size.height
                            if (scrollValue > 0) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(edgeFadeColor.copy(alpha = 0.92f), Color.Transparent),
                                        startX = 0f,
                                        endX = edge
                                    ),
                                    topLeft = Offset.Zero,
                                    size = Size(edge, h)
                                )
                            }
                            if (scrollMax > 0 && scrollValue < scrollMax) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, edgeFadeColor.copy(alpha = 0.92f)),
                                        startX = w - edge,
                                        endX = w
                                    ),
                                    topLeft = Offset(w - edge, 0f),
                                    size = Size(edge, h)
                                )
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Only shortcuts from the optional custom layout. Everything else (Cmds, arrows, Enter, Ctrl, …)
                    // is opened from the pinned More (⋯) panel. MORE_KEYS is always pinned here, so skip in strip.
                    if (customShortcutStrip != null) {
                        for (action in customShortcutStrip) {
                            if (action == BottomBarShortcutAction.MORE_KEYS) continue
                            IconButton(
                                modifier = Modifier.size(BOTTOM_BAR_HEIGHT_DP.dp),
                                onClick = {
                                    performCustomBottomBarAction(
                                        action = action,
                                        bridge = bridge,
                                        keyHandler = keyHandler,
                                        onOpenTextInput = onVoiceInput,
                                        onOpenCommandsPanel = {
                                            activePanel =
                                                if (activePanel == BottomBarPanel.COMMANDS) {
                                                    BottomBarPanel.NONE
                                                } else {
                                                    BottomBarPanel.COMMANDS
                                                }
                                        },
                                        onOpenHistoryPanel = {
                                            activePanel =
                                                if (activePanel == BottomBarPanel.HISTORY) {
                                                    BottomBarPanel.NONE
                                                } else {
                                                    BottomBarPanel.HISTORY
                                                }
                                        },
                                        onOpenMorePanel = {
                                            activePanel =
                                                if (activePanel == BottomBarPanel.MORE_KEYS) {
                                                    BottomBarPanel.NONE
                                                } else {
                                                    BottomBarPanel.MORE_KEYS
                                                }
                                        }
                                    )
                                    onInteraction()
                                }
                            ) {
                                BottomBarShortcutVisual(
                                    action = action,
                                    panel = activePanel,
                                    modifierState = modifierState,
                                    tintActive = tealActive(),
                                    tintIdle = tealText()
                                )
                            }
                        }
                    }
                }

                // Modifier active badges — fade in/out when Ctrl/Alt/Shift is pressed or locked
                listOf(
                    modifierState.ctrlState to "Ctrl",
                    modifierState.altState to "Alt",
                    modifierState.shiftState to "Shift",
                ).forEach { (level, label) ->
                    AnimatedVisibility(
                        visible = level != ModifierLevel.OFF,
                        enter = fadeIn(tween(120)),
                        exit = fadeOut(tween(120))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = tealActive().copy(alpha = 0.22f),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = if (level == ModifierLevel.LOCKED) label.uppercase() else label,
                                color = tealActive(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                IconButton(
                    modifier = Modifier.size(BOTTOM_BAR_HEIGHT_DP.dp),
                    onClick = {
                        activePanel = if (activePanel == BottomBarPanel.MORE_KEYS) {
                            BottomBarPanel.NONE
                        } else {
                            BottomBarPanel.MORE_KEYS
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = stringResource(R.string.bottom_bar_action_more_keys),
                        tint = if (activePanel == BottomBarPanel.MORE_KEYS) tealActive() else tealText(),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Keyboard toggle — pinned next to More (never scrolled away).
                IconButton(
                    modifier = Modifier.size(BOTTOM_BAR_HEIGHT_DP.dp),
                    onClick = {
                        if (keyboardLockedOff) {
                            onToggleKeyboardLock()
                            onToggleKeyboard()
                        } else if (showSoftwareKeyboard) {
                            onToggleKeyboardLock()
                            onToggleKeyboard()
                        } else {
                            onToggleKeyboard()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (keyboardLockedOff) {
                            Icons.Default.KeyboardHide
                        } else if (showSoftwareKeyboard) {
                            Icons.Default.KeyboardHide
                        } else {
                            Icons.Default.Keyboard
                        },
                        contentDescription = when {
                            keyboardLockedOff -> stringResource(R.string.bottom_bar_a11y_kb_disabled)
                            showSoftwareKeyboard -> stringResource(R.string.bottom_bar_a11y_kb_hide_lock)
                            else -> stringResource(R.string.bottom_bar_a11y_kb_show)
                        },
                        tint = if (keyboardLockedOff) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Popup panel showing saved Quick Commands as a vertical list with a search bar.
 * As the user types, filters quick commands by title/command matching the query.
 * Below filtered quick commands, shows a "Recent" section with matching history commands.
 */
@Composable
private fun CommandsPanel(
    quickCommands: List<QuickCommand>,
    commandHistory: List<CommandHistory>,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredQuickCommands = remember(quickCommands, searchQuery) {
        if (searchQuery.isBlank()) {
            quickCommands
        } else {
            quickCommands.filter { cmd ->
                cmd.title.contains(searchQuery, ignoreCase = true) ||
                    cmd.command.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredHistory = remember(commandHistory, searchQuery) {
        if (searchQuery.isBlank()) {
            commandHistory
        } else {
            commandHistory.filter { entry ->
                entry.command.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    // Deduplicate history commands for display
    val uniqueHistoryCommands = remember(filteredHistory) {
        filteredHistory.distinctBy { it.command }
    }

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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search commands...",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tealActive(),
                    cursorColor = tealText()
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(44.dp)
            )

            if (filteredQuickCommands.isEmpty() && uniqueHistoryCommands.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No quick commands saved"
                        } else {
                            "No matching commands"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(vertical = 4.dp)
                ) {
                    // Quick commands section
                    if (filteredQuickCommands.isNotEmpty()) {
                        items(
                            items = filteredQuickCommands,
                            key = { "qc_${it.id}" }
                        ) { command ->
                            CommandItem(
                                command = command,
                                onClick = { onCommandClick(command.command) }
                            )
                        }
                    }

                    // Recent history section
                    if (uniqueHistoryCommands.isNotEmpty()) {
                        item(key = "history_header") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        items(
                            items = uniqueHistoryCommands.take(10),
                            key = { "hist_${it.id}" }
                        ) { entry ->
                            HistoryCommandItem(
                                command = entry.command,
                                executedAt = entry.executedAt,
                                onClick = { onCommandClick(entry.command) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Popup panel showing command history for the current host.
 * Appears above the bottom bar when the history button is toggled.
 */
@Composable
private fun HistoryPanel(
    commandHistory: List<CommandHistory>,
    onCommandClick: (String) -> Unit,
    onClearHistory: () -> Unit,
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
        if (commandHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No command history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .padding(vertical = 4.dp)
                ) {
                    items(
                        items = commandHistory,
                        key = { it.id }
                    ) { entry ->
                        HistoryCommandItem(
                            command = entry.command,
                            executedAt = entry.executedAt,
                            onClick = { onCommandClick(entry.command) }
                        )
                    }
                }

                // Clear history button
                TextButton(
                    onClick = onClearHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Clear history",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * A single history command item showing the command text and relative timestamp.
 */
@Composable
private fun HistoryCommandItem(
    command: String,
    executedAt: Long,
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
        Text(
            text = command,
            style = MaterialTheme.typography.bodyMedium,
            color = tealText(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatRelativeTime(executedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Formats a timestamp as a relative time string (e.g. "2 min ago").
 */
private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
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
                color = tealText(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Popup panel: primary session actions (formerly on the bar) plus extended keys.
 * Appears above the bottom bar when the more button (⋯) is toggled.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoreKeysPanel(
    bridge: TerminalBridge,
    bottomBarStripBuiltin: TerminalBottomBarPreset,
    customShortcutStrip: List<BottomBarShortcutAction>?,
    onInteraction: () -> Unit,
    onOpenCommandsPanel: () -> Unit,
    onOpenHistoryPanel: () -> Unit,
    onOpenFloatingTextInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyHandler = bridge.keyHandler
    val modifierState by keyHandler.modifierState.collectAsState()
    val ctrlLevel = modifierState.ctrlState
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
        val strip = customShortcutStrip
        fun inStrip(action: BottomBarShortcutAction) = strip?.contains(action) == true

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!inStrip(BottomBarShortcutAction.COMMANDS)) {
                MoreKeyIconChip(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.bottom_bar_action_commands),
                    onClick = {
                        onOpenCommandsPanel()
                        onInteraction()
                    }
                )
            }
            if (!inStrip(BottomBarShortcutAction.HISTORY)) {
                MoreKeyIconChip(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = stringResource(R.string.bottom_bar_action_history),
                    onClick = {
                        onOpenHistoryPanel()
                        onInteraction()
                    }
                )
            }
            if (!inStrip(BottomBarShortcutAction.ARROW_UP)) {
                MoreKeyIconChip(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.pref_bottom_bar_icon_legend_main_up),
                    onClick = {
                        keyHandler.sendPressedKey(VTermKey.UP)
                        onInteraction()
                    }
                )
            }
            if (!inStrip(BottomBarShortcutAction.ARROW_DOWN)) {
                MoreKeyIconChip(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.pref_bottom_bar_icon_legend_main_down),
                    onClick = {
                        keyHandler.sendPressedKey(VTermKey.DOWN)
                        onInteraction()
                    }
                )
            }
            if (!inStrip(BottomBarShortcutAction.ENTER)) {
                MoreKeyIconChip(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                    contentDescription = stringResource(R.string.pref_bottom_bar_icon_legend_main_enter),
                    onClick = {
                        keyHandler.sendPressedKey(VTermKey.ENTER)
                        onInteraction()
                    }
                )
            }
            if (!inStrip(BottomBarShortcutAction.CTRL_TOGGLE)) {
                MoreKeyChip(
                    label = stringResource(R.string.bottom_bar_ctrl_short_label),
                    onClick = {
                        if (ctrlLevel != ModifierLevel.OFF) {
                            keyHandler.clearCtrl()
                        } else {
                            keyHandler.metaPress(TerminalKeyListener.OUR_CTRL_ON, true)
                        }
                        onInteraction()
                    },
                    isActive = ctrlLevel != ModifierLevel.OFF
                )
            }
            if (!inStrip(BottomBarShortcutAction.TEXT_INPUT)) {
                MoreKeyIconChip(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.bottom_bar_action_text_input),
                    onClick = {
                        onOpenFloatingTextInput()
                        onInteraction()
                    }
                )
            }

            if (bottomBarStripBuiltin == TerminalBottomBarPreset.TMUX && customShortcutStrip == null) {
                MoreKeyChip(
                    label = "^B",
                    onClick = {
                        bridge.sendCtrlB()
                        onInteraction()
                    }
                )
                MoreKeyChip(
                    label = "^D",
                    onClick = {
                        bridge.sendCtrlD()
                        onInteraction()
                    }
                )
            }

            if (!inStrip(BottomBarShortcutAction.ESC)) {
                MoreKeyChip(label = "Esc", onClick = {
                    keyHandler.sendEscape()
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.TAB)) {
                MoreKeyChip(label = "Tab", onClick = {
                    keyHandler.sendTab()
                    onInteraction()
                })
            }

            val altLevel = modifierState.altState
            if (!inStrip(BottomBarShortcutAction.ALT)) {
                MoreKeyChip(
                    label = "Alt",
                    onClick = {
                        keyHandler.metaPress(TerminalKeyListener.OUR_ALT_ON, true)
                        onInteraction()
                    },
                    isActive = altLevel != ModifierLevel.OFF
                )
            }

            if (!inStrip(BottomBarShortcutAction.ARROW_LEFT)) {
                MoreKeyChip(label = "\u2190", onClick = {
                    keyHandler.sendPressedKey(VTermKey.LEFT)
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.ARROW_RIGHT)) {
                MoreKeyChip(label = "\u2192", onClick = {
                    keyHandler.sendPressedKey(VTermKey.RIGHT)
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.PIPE)) {
                MoreKeyChip(label = "|", onClick = {
                    bridge.injectString("|")
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.DASH)) {
                MoreKeyChip(label = "-", onClick = {
                    bridge.injectString("-")
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.SLASH)) {
                MoreKeyChip(label = "/", onClick = {
                    bridge.injectString("/")
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.TILDE)) {
                MoreKeyChip(label = "~", onClick = {
                    bridge.injectString("~")
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.HOME)) {
                MoreKeyChip(label = "Home", onClick = {
                    keyHandler.sendPressedKey(VTermKey.HOME)
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.END)) {
                MoreKeyChip(label = "End", onClick = {
                    keyHandler.sendPressedKey(VTermKey.END)
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.PAGE_UP)) {
                MoreKeyChip(label = "PgUp", onClick = {
                    bridge.sendPageUp()
                    onInteraction()
                })
            }

            if (!inStrip(BottomBarShortcutAction.PAGE_DOWN)) {
                MoreKeyChip(label = "PgDn", onClick = {
                    bridge.sendPageDown()
                    onInteraction()
                })
            }

            val fontSize by bridge.fontSizeFlow.collectAsState()
            if (!inStrip(BottomBarShortcutAction.FONT_DEC)) {
                MoreKeyChip(label = "A\u2212", onClick = {
                    bridge.decreaseFontSize()
                    onInteraction()
                })
            }
            MoreKeyChip(label = "${fontSize.toInt()}sp", onClick = {})
            if (!inStrip(BottomBarShortcutAction.FONT_INC)) {
                MoreKeyChip(label = "A+", onClick = {
                    bridge.increaseFontSize()
                    onInteraction()
                })
            }
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
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val bgColor = if (isActive) tealActive().copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (isActive) Color.White else tealText()

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

/** Icon-only chip matching [MoreKeyChip] height and style. */
@Composable
private fun MoreKeyIconChip(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = tealText()
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
            .height(32.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
