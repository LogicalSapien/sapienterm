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
import androidx.compose.material.icons.filled.Close
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
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.service.TerminalBridge

/**
 * Compute a meaningful display name for a terminal session tab.
 *
 * Priority order:
 * 1. Custom tab name (user-set rename)
 * 2. Host nickname, if it looks like a real connection identifier
 *    (not the app name and not blank)
 * 3. Fallback: "username@hostname" built from the host fields
 */
internal fun getTabDisplayName(bridge: TerminalBridge): String {
    bridge.customTabName?.let { return it }

    val nickname = bridge.host.nickname
    // If the nickname is blank or matches the app name, build a fallback
    if (nickname.isNotBlank()
        && !nickname.equals("SapienSSH", ignoreCase = true)
        && !nickname.equals("SapienSsh", ignoreCase = true)
    ) {
        return nickname
    }

    // Build fallback from username + hostname
    val host = bridge.host
    return when {
        host.username.isNotBlank() && host.hostname.isNotBlank() ->
            "${host.username}@${host.hostname}"
        host.hostname.isNotBlank() -> host.hostname
        host.username.isNotBlank() -> host.username
        // Last resort: use nickname even if it's the app name
        nickname.isNotBlank() -> nickname
        else -> "Terminal"
    }
}

/**
 * Horizontal scrollable tab bar for switching between terminal sessions.
 *
 * Each tab shows the connection nickname (or custom name) and a close button.
 * The active tab is highlighted with the primary color.
 * Long press on a tab opens a rename dialog.
 */
@Composable
fun SessionTabBar(
    bridges: List<TerminalBridge>,
    currentIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (TerminalBridge) -> Unit,
    onRenameTab: (Int, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var renameDialogIndex by remember { mutableStateOf<Int?>(null) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        bridges.forEachIndexed { index, bridge ->
            val isSelected = index == currentIndex
            val displayName = getTabDisplayName(bridge)
            SessionTab(
                nickname = displayName,
                isSelected = isSelected,
                onClick = { onSelectTab(index) },
                onLongClick = { renameDialogIndex = index },
                onClose = { onCloseTab(bridge) }
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
