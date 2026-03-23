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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.service.TerminalBridge

/**
 * Maximum number of characters for a tab display name before truncation.
 */
private const val MAX_TAB_NAME_LENGTH = 18

/**
 * Compute a short, readable display name for a single terminal session tab.
 *
 * Priority order:
 * 1. Custom tab name (user-set rename) -- returned as-is (already short by
 *    convention).
 * 2. First segment of the hostname (e.g. "macbook" from
 *    "macbook.cheetah-balance.ts.net"). If the hostname is an IP address the
 *    full IP is used.
 * 3. Fallback: host nickname or "Terminal".
 *
 * The result is truncated to [MAX_TAB_NAME_LENGTH] characters so tabs stay
 * compact even with long hostnames.
 */
internal fun getTabDisplayName(bridge: TerminalBridge): String {
    bridge.customTabName?.let { return it }

    val host = bridge.host
    val nickname = host.nickname

    // Derive a short hostname: take the first DNS label unless it looks like
    // an IP address (contains only digits and dots).
    val rawHostname = host.hostname
    val shortHost = when {
        rawHostname.isBlank() -> null
        rawHostname.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) -> rawHostname
        rawHostname.contains('.') -> rawHostname.substringBefore('.')
        else -> rawHostname
    }

    // Pick the best raw name (before truncation)
    val rawName = when {
        // Prefer a meaningful nickname that isn't the app name
        nickname.isNotBlank()
            && !nickname.equals("SapienTerm", ignoreCase = true)
            && !nickname.equals("SapienTerm", ignoreCase = true) -> {
            // Even for nicknames, try to shorten "user@long.host.name" style
            if ('@' in nickname && shortHost != null) shortHost else nickname
        }
        shortHost != null -> shortHost
        host.username.isNotBlank() -> host.username
        nickname.isNotBlank() -> nickname
        else -> "Terminal"
    }

    // Truncate long names with an ellipsis
    return if (rawName.length > MAX_TAB_NAME_LENGTH) {
        rawName.take(MAX_TAB_NAME_LENGTH - 1) + "\u2026"
    } else {
        rawName
    }
}

/**
 * Build display names for a list of bridges, appending a disambiguation
 * suffix (" (2)", " (3)", ...) when two or more bridges would otherwise
 * have the same tab name.
 */
private fun buildTabDisplayNames(bridges: List<TerminalBridge>): List<String> {
    val rawNames = bridges.map { getTabDisplayName(it) }

    // Count how many times each raw name appears
    val counts = mutableMapOf<String, Int>()
    val seen = mutableMapOf<String, Int>()

    for (name in rawNames) {
        counts[name] = (counts[name] ?: 0) + 1
    }

    return rawNames.map { name ->
        if ((counts[name] ?: 0) > 1) {
            val idx = (seen[name] ?: 0) + 1
            seen[name] = idx
            "$name ($idx)"
        } else {
            name
        }
    }
}

/**
 * Horizontal scrollable tab bar for switching between terminal sessions.
 *
 * Each tab shows the connection nickname (or custom name) and a close button.
 * The active tab is highlighted with the primary color.
 * Long press on a tab opens a rename dialog.
 *
 * Includes a back arrow icon at the start and a menu (three-dots) icon at
 * the end, so this bar replaces the TopAppBar entirely.
 */
@Composable
fun SessionTabBar(
    bridges: List<TerminalBridge>,
    currentIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTabRequested: (TerminalBridge) -> Unit,
    onRenameTab: (Int, String) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var renameDialogIndex by remember { mutableStateOf<Int?>(null) }

    // Pre-compute display names with disambiguation for duplicate hostnames
    val displayNames = remember(bridges) { buildTabDisplayNames(bridges) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back arrow button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.button_back),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scrollable tabs
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            bridges.forEachIndexed { index, bridge ->
                val isSelected = index == currentIndex
                val displayName = displayNames.getOrElse(index) { getTabDisplayName(bridge) }
                SessionTab(
                    nickname = displayName,
                    isSelected = isSelected,
                    onClick = { onSelectTab(index) },
                    onLongClick = { renameDialogIndex = index },
                    onClose = { onCloseTabRequested(bridge) }
                )
            }
        }

        // Menu (three-dots) button
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.button_more_options),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Rename dialog
    renameDialogIndex?.let { index ->
        val bridge = bridges.getOrNull(index) ?: return@let
        val currentName = getTabDisplayName(bridge)
        RenameTabDialog(
            currentName = currentName,
            onDismiss = { renameDialogIndex = null },
            onConfirm = { newName ->
                onRenameTab(index, newName)
                renameDialogIndex = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionTab(
    nickname: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close_tab),
                    modifier = Modifier.size(14.dp),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

@Composable
private fun RenameTabDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.rename_tab_title))
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.rename_tab_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}
