/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
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

package com.logicalsapien.sapienterm.ui.screens.help

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.BuildConfig
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.PreviewScreen
import com.logicalsapien.sapienterm.ui.components.SapienTermBranding
import com.logicalsapien.sapienterm.ui.theme.SapienTermTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHints: () -> Unit,
    onNavigateToEula: () -> Unit,
    onNavigateToContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeyboardShortcuts by remember { mutableStateOf(false) }
    var showLogViewer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_help)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SapienTermBranding(logoSize = 48.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Feature guide sections
            item {
                Text(
                    text = "HOW IT WORKS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Cable,
                    title = "Connections",
                    bullets = listOf(
                        "Tap + to add SSH, Telnet, or Local shell connections",
                        "Tap a connection to open a terminal session",
                        "Long-press a connection for options: Edit, Rename, Clone, Move to Group, Delete",
                        "Swipe right to edit, swipe left to delete",
                        "Use the search icon to filter connections by name, hostname, or user",
                        "Create groups to organize connections -- tap the + chip in the filter row",
                        "Long-press a group chip to rename or delete it"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Code,
                    title = "Quick Commands",
                    bullets = listOf(
                        "Save frequently-used commands for quick access",
                        "Organize commands with categories -- type a new one or tap an existing category chip when adding/editing",
                        "Filter commands by category using the chip row, or search by name",
                        "Tap a command to edit it; long-press for Clone or Delete",
                        "In a terminal session, access Quick Commands from the bottom toolbar"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Key,
                    title = "Credentials",
                    bullets = listOf(
                        "Store SSH keys and passwords securely on-device",
                        "Generate new SSH key pairs (RSA, Ed25519, ECDSA)",
                        "Import existing private keys from files",
                        "Assign credentials to connections for passwordless login",
                        "Swipe left on a credential to delete it"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Terminal,
                    title = "Terminal Session",
                    bullets = listOf(
                        "Tap the terminal area to show the keyboard",
                        "Use the bottom bar for quick actions: Tab, Ctrl, arrow keys, and more",
                        "Tap the mic icon to use voice typing",
                        "Pinch to zoom in/out to change font size",
                        "Volume keys can adjust font size (enable in Settings)",
                        "Swipe up/down with two fingers for Page Up/Down"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Keyboard,
                    title = "Keyboard Shortcuts",
                    bullets = listOf(
                        "Ctrl+Shift+V -- Paste from clipboard",
                        "Ctrl+= -- Increase font size",
                        "Ctrl+- -- Decrease font size",
                        "Camera button action is configurable in Settings"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    bullets = listOf(
                        "Switch between Dark, Light, or System theme in Settings",
                        "Choose terminal color schemes: Dracula, Nord, Solarized, Monokai, and more",
                        "Customize terminal fonts -- use built-in, Google Fonts, or import your own",
                        "Assign colors to connections for visual identification"
                    )
                )
            }

            item {
                FeatureGuideCard(
                    icon = Icons.Default.SmartDisplay,
                    title = "Smart Input Detection",
                    bullets = listOf(
                        "SapienTerm detects when the terminal is waiting for your input",
                        "Interactive prompts (yes/no, numbered choices) show tappable action chips",
                        "Password and sudo prompts are recognized automatically",
                        "AI coding tool prompts (Cursor, Claude Code, etc.) are detected",
                        "Background tabs with pending prompts show a pulsing red badge",
                        "Get Android notifications when a background terminal needs your input"
                    )
                )
            }

            item {
                TmuxTipsCard()
            }

            // Action buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "MORE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToHints,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.hints))
                    }
                    OutlinedButton(
                        onClick = { showKeyboardShortcuts = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.keyboard_shortcuts))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showLogViewer = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.view_logs))
                    }
                    OutlinedButton(
                        onClick = onNavigateToContact,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.title_contact))
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToEula,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.terms_and_conditions))
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.app_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.app_copyright),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showKeyboardShortcuts) {
        KeyboardShortcutsDialog(
            onDismiss = { showKeyboardShortcuts = false }
        )
    }

    if (showLogViewer) {
        LogViewerDialog(
            onDismiss = { showLogViewer = false }
        )
    }
}

@Composable
private fun TmuxTipsCard() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "tmux Tips",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Viewing tmux from both desktop and mobile? Use a grouped session so each client gets its own independent window size:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    CopyableCommandBlock(
                        label = "Post-login command (replace 'main' with your session name):",
                        command = "tmux new-session -t main -s mobile || tmux attach -t mobile",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Or add these to ~/.tmux.conf on your server:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    CopyableCommandBlock(
                        label = null,
                        command = "set-option -g window-size largest\nset-option -g aggressive-resize on",
                        context = context
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val tips = listOf(
                        "Grouped sessions share the same windows but resize independently",
                        "Set the post-login command in your connection's edit screen",
                        "SapienTerm debounces resize events to minimize tmux redraw glitches"
                    )
                    tips.forEach { tip ->
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "\u2022",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                            )
                            Text(
                                text = tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyableCommandBlock(
    label: String?,
    command: String,
    context: Context
) {
    if (label != null) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = command,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("command", command))
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeatureGuideCard(
    icon: ImageVector,
    title: String,
    bullets: List<String>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    bullets.forEach { bullet ->
                        Row(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = "\u2022",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp, top = 1.dp)
                            )
                            Text(
                                text = bullet,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardShortcutsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.keyboard_shortcuts)) },
        text = {
            Column {
                ShortcutRow(
                    shortcut = stringResource(R.string.paste_shortcut),
                    description = stringResource(R.string.console_menu_paste)
                )
                ShortcutRow(
                    shortcut = stringResource(R.string.increase_font_shortcut),
                    description = stringResource(R.string.increase_font_size)
                )
                ShortcutRow(
                    shortcut = stringResource(R.string.decrease_font_shortcut),
                    description = stringResource(R.string.decrease_font_size)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

@Composable
private fun ShortcutRow(
    shortcut: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = shortcut,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun LogViewerDialog(
    onDismiss: () -> Unit,
    viewModel: LogViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val logs = uiState.logs

    LaunchedEffect(Unit) {
        viewModel.loadLogs()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logs_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxHeight(0.7f)
            ) {
                Text(
                    text = stringResource(R.string.logs_bug_report_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                val scrollState = rememberScrollState()
                Text(
                    text = logs.ifEmpty { stringResource(R.string.no_logs_available) },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    copyLogsToClipboard(context, logs)
                }
            ) {
                Text(stringResource(R.string.copy_logs))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

private fun copyLogsToClipboard(context: Context, logs: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.logs_title), logs)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, R.string.logs_copied, Toast.LENGTH_SHORT).show()
}

@PreviewScreen
@Composable
private fun HelpScreenPreview() {
    SapienTermTheme {
        HelpScreen(
            onNavigateBack = {},
            onNavigateToHints = {},
            onNavigateToEula = {},
            onNavigateToContact = {}
        )
    }
}

@Preview
@Composable
private fun KeyboardShortcutsDialogPreview() {
    SapienTermTheme {
        KeyboardShortcutsDialog(
            onDismiss = {}
        )
    }
}
