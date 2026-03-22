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

package com.logicalsapien.sapienssh.ui.screens.hostlist

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.data.entity.Host
import com.logicalsapien.sapienssh.ui.theme.StatusGreen
import com.logicalsapien.sapienssh.ui.theme.StatusGrey
import com.logicalsapien.sapienssh.ui.theme.StatusRed

/**
 * Card-based connection item for the host list.
 *
 * Displays the connection nickname, hostname:port, username, connection status dot,
 * and last connected timestamp in a Material Design 3 card with 16dp rounded corners.
 */
@Composable
fun ConnectionCard(
    host: Host,
    connectionState: ConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> StatusGreen
        ConnectionState.DISCONNECTED -> StatusRed
        ConnectionState.UNKNOWN -> StatusGrey
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status dot
            Canvas(
                modifier = Modifier.size(12.dp)
            ) {
                drawCircle(color = statusColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Connection info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Connection name (headline)
                Text(
                    text = host.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // hostname:port (supporting text)
                Text(
                    text = if (host.protocol == "local") {
                        "local"
                    } else {
                        "${host.hostname}:${host.port}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Username (if present)
                if (host.username.isNotBlank()) {
                    Text(
                        text = host.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Last connected timestamp
            Text(
                text = formatLastConnected(host.lastConnect),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
