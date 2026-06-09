/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.hosteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.theme.HostCategoryColor
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

/**
 * Hero identity card at the top of the host editor. Shows the currently
 * selected color-category as a large circle (tappable to cycle through the
 * palette), the nickname, and a live preview of user@host:port.
 *
 * Tapping the circle cycles to the next [HostCategoryColor]; anything more
 * elaborate (picker dialog, inline row of swatches) can slot in later
 * without changing the surrounding editor layout.
 */
@Composable
fun HostIdentityStrip(
    colorStorage: String,
    nickname: String,
    username: String,
    hostname: String,
    port: String,
    protocol: String,
    onCycleColor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    val haptics = LocalHapticFeedback.current
    val current = HostCategoryColor.fromStorageString(colorStorage)
    val currentColor = tokens.colorFor(current)

    val preview = buildPreview(
        protocol = protocol,
        username = username,
        hostname = hostname,
        port = port
    )

    Surface(
        color = tokens.surface,
        shape = RoundedCornerShape(tokens.cornerLarge),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(currentColor, CircleShape)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val values = HostCategoryColor.values()
                        val next = values[(current.ordinal + 1) % values.size]
                        onCycleColor(next.storageString)
                    }
            )
            Column(
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    text = nickname.ifBlank { stringResource(R.string.hostpref_add_host) },
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (preview.isNotBlank()) {
                    Text(
                        text = preview,
                        color = tokens.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun buildPreview(
    protocol: String,
    username: String,
    hostname: String,
    port: String
): String {
    if (protocol == "local") return "local"
    if (hostname.isBlank()) return ""
    val user = username.takeIf { it.isNotBlank() }?.let { "$it@" } ?: ""
    val portSuffix = port.takeIf { it.isNotBlank() && it != "22" }?.let { ":$it" } ?: ""
    return "$user$hostname$portSuffix"
}

