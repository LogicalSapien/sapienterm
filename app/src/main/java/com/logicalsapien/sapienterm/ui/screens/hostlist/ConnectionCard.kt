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

package com.logicalsapien.sapienterm.ui.screens.hostlist

import android.graphics.Color as AndroidColor
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.ui.theme.StatusGreen
import com.logicalsapien.sapienterm.ui.theme.StatusGrey
import com.logicalsapien.sapienterm.ui.theme.StatusRed

private const val HIGH_LATENCY_MS = 200

/**
 * Card-based connection item for the host list.
 *
 * Displays the connection nickname, hostname:port, username, connection status dot,
 * and last connected timestamp in a Material Design 3 card with 16dp rounded corners.
 *
 * Supports long press to show a context menu with Clone, Rename, Edit, and Delete actions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionCard(
    host: Host,
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    healthStatus: HealthStatus? = null,
    onClone: (() -> Unit)? = null,
    onRename: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onMoveToGroup: (() -> Unit)? = null
) {
    val (statusColor, statusLabel) = when (connectionState) {
        ConnectionState.CONNECTED -> StatusGreen to "Connected"
        ConnectionState.DISCONNECTED -> StatusRed to "Disconnected"
        ConnectionState.UNKNOWN -> StatusGrey to "Offline"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val accentStripColor = remember(host.color, primaryColor) {
        val hex = host.color
        if (hex.isNullOrBlank()) {
            primaryColor
        } else {
            try {
                Color(AndroidColor.parseColor(hex))
            } catch (_: IllegalArgumentException) {
                primaryColor
            }
        }
    }

    val protocolLabel = when (host.protocol.lowercase()) {
        "local" -> "LOCAL"
        "telnet" -> "TELNET"
        else -> "SSH"
    }

    val showLatency = host.protocol != "local" && host.hostname.isNotBlank()
    val (latencyText, latencyColor) = if (showLatency) {
        when {
            healthStatus == null -> "..." to StatusGrey
            !healthStatus.isReachable -> "offline" to StatusRed
            else -> {
                val ms = healthStatus.latencyMs
                if (ms != null) {
                    val color = if (ms > HIGH_LATENCY_MS) StatusRed else StatusGreen
                    "${ms}ms" to color
                } else {
                    "..." to StatusGrey
                }
            }
        }
    } else {
        null to null
    }

    val hasContextMenu = onClone != null || onRename != null || onEdit != null || onDelete != null || onMoveToGroup != null
    var showContextMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (hasContextMenu) {
                        { showContextMenu = true }
                    } else {
                        null
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentStripColor)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = host.nickname,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = protocolLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = statusColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(statusColor)
                                    )
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (host.protocol == "local") {
                                "local"
                            } else {
                                "${host.hostname}:${host.port}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        if (latencyText != null && latencyColor != null) {
                            Text(
                                text = latencyText,
                                style = MaterialTheme.typography.labelSmall,
                                color = latencyColor,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (host.username.isNotBlank()) {
                            Text(
                                text = host.username,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Text(
                            text = formatLastConnected(host.lastConnect),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Context menu (shown on long press)
        if (hasContextMenu) {
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                onClone?.let { cloneAction ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_host_duplicate)) },
                        onClick = {
                            showContextMenu = false
                            cloneAction()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                        }
                    )
                }
                onRename?.let { renameAction ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_host_rename)) },
                        onClick = {
                            showContextMenu = false
                            renameAction()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.EditNote, contentDescription = null)
                        }
                    )
                }
                onEdit?.let { editAction ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_host_edit)) },
                        onClick = {
                            showContextMenu = false
                            editAction()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                }
                onMoveToGroup?.let { moveAction ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.group_move_to)) },
                        onClick = {
                            showContextMenu = false
                            moveAction()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                        }
                    )
                }
                onDelete?.let { deleteAction ->
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.list_host_delete)) },
                        onClick = {
                            showContextMenu = false
                            deleteAction()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Formats a last-connected timestamp as a relative time string.
 *
 * Returns "Never" for timestamps of 0 (never connected), otherwise a relative
 * time span like "2 hours ago" or "3 days ago".
 */
@Composable
private fun formatLastConnected(lastConnect: Long): String {
    if (lastConnect <= 0L) {
        return stringResource(R.string.last_connected_never)
    }
    val now = System.currentTimeMillis()
    return DateUtils.getRelativeTimeSpanString(
        lastConnect,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
