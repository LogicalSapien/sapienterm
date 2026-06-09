/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.ui.screens.hostlist.ConnectionState
import com.logicalsapien.sapienterm.ui.theme.HostCategoryColor
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

/**
 * Accent Bar host card — the locked Phase-1 design applied to the host list.
 * Uses [SapienTheme.tokens] for palette and [SapienTheme.density] for spacing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HostAccentCard(
    host: Host,
    connectionState: ConnectionState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null
) {
    val tokens = SapienTheme.tokens
    val density = SapienTheme.density
    val accent = tokens.colorFor(HostCategoryColor.resolveWithFallback(host.color, host.nickname))
    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> tokens.statusOnline
        ConnectionState.DISCONNECTED -> tokens.statusError
        ConnectionState.UNKNOWN -> tokens.statusIdle
    }
    val subtitle = buildSubtitle(host)
    val meta = if (host.lastConnect > 0L) {
        DateUtils.getRelativeTimeSpanString(
            host.lastConnect,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    } else null

    val haptics = LocalHapticFeedback.current
    val rowModifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = onTap,
            onLongClick = onLongPress?.let { handler ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    handler()
                }
            }
        )

    if (density.showSubtitle) {
        SapienCard(modifier = rowModifier) {
            HostCardInner(
                accent = accent,
                nickname = host.nickname,
                subtitle = subtitle,
                metadata = meta,
                statusColor = statusColor,
                tokens = tokens,
                showMetadata = density.showMetadata,
                cardPadding = density.cardPadding
            )
        }
    } else {
        // Compact: no card border, just accent bar + condensed content
        Row(
            modifier = rowModifier
                .height(density.rowHeight)
                .background(tokens.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentBar(color = accent)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = host.nickname,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = subtitle,
                    color = tokens.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor)
                )
            }
        }
    }
}

@Composable
private fun HostCardInner(
    accent: Color,
    nickname: String,
    subtitle: String,
    metadata: String?,
    statusColor: Color,
    tokens: com.logicalsapien.sapienterm.ui.theme.ThemeTokens,
    showMetadata: Boolean,
    cardPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
    ) {
        AccentBar(color = accent)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nickname,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor)
                )
            }
            Text(
                text = subtitle,
                color = tokens.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showMetadata && metadata != null) {
                Text(
                    text = metadata,
                    color = tokens.textMuted,
                    maxLines = 1
                )
            }
        }
    }
}

private fun buildSubtitle(host: Host): String = when (host.protocol.lowercase()) {
    "local" -> "local"
    else -> buildString {
        if (host.username.isNotBlank()) {
            append(host.username)
            append('@')
        }
        append(host.hostname.ifBlank { "?" })
        if (host.port > 0) {
            append(':')
            append(host.port)
        }
    }
}
