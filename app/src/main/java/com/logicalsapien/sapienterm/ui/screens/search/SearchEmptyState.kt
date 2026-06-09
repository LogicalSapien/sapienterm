/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.ui.components.HostAccentCard
import com.logicalsapien.sapienterm.ui.screens.hostlist.ConnectionState
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun SearchEmptyState(
    recents: List<Host>,
    pinned: List<Host>,
    onHostTap: (Host) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(SapienTheme.density.cardGap)
    ) {
        if (pinned.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_pinned)) }
            items(items = pinned, key = { "p-${it.id}" }) { host ->
                HostAccentCard(
                    host = host,
                    connectionState = ConnectionState.UNKNOWN,
                    onTap = { onHostTap(host) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        if (recents.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.search_section_recent)) }
            items(items = recents, key = { "r-${it.id}" }) { host ->
                HostAccentCard(
                    host = host,
                    connectionState = ConnectionState.UNKNOWN,
                    onTap = { onHostTap(host) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
        item {
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text(
                    text = stringResource(R.string.search_hint_suggestions),
                    color = tokens.textMuted
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    val tokens = SapienTheme.tokens
    Text(
        text = label,
        color = tokens.textMuted,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
