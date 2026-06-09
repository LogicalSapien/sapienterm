/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.ui.theme.HostCategoryColor
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommandAccentCard(
    command: QuickCommand,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null
) {
    val tokens = SapienTheme.tokens
    val density = SapienTheme.density
    val accent = tokens.colorFor(HostCategoryColor.GRAY)

    val clickable = modifier
        .fillMaxWidth()
        .combinedClickable(onClick = onTap, onLongClick = onLongPress)

    if (density.showSubtitle) {
        SapienCard(modifier = clickable) {
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
                AccentBar(color = accent)
                Column(
                    modifier = Modifier.weight(1f).padding(density.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = command.title,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = command.command,
                        color = tokens.textMuted,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        Row(
            modifier = clickable.height(density.rowHeight).background(tokens.surface),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentBar(color = accent)
            Row(
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = command.title,
                    color = tokens.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = command.command,
                    color = tokens.textMuted,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f)
                )
            }
        }
    }
}
