/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Credential
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.data.search.SearchResults
import com.logicalsapien.sapienterm.ui.components.CommandAccentCard
import com.logicalsapien.sapienterm.ui.components.HostAccentCard
import com.logicalsapien.sapienterm.ui.components.KeyAccentCard
import com.logicalsapien.sapienterm.ui.components.SapienCard
import com.logicalsapien.sapienterm.ui.screens.hostlist.ConnectionState
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

private const val PREVIEW_ROWS = 3

@Composable
fun SearchResultsList(
    query: String,
    results: SearchResults,
    onHostTap: (Host) -> Unit,
    onCommandTap: (QuickCommand) -> Unit,
    onCredentialTap: (Credential) -> Unit,
    onQuickCreateHost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens

    if (results.isEmpty) {
        LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
            item {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = tokens.textMuted,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            item {
                SapienCard(modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onQuickCreateHost(query) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(tokens.cornerMedium))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = tokens.primary
                            )
                        }
                        Text(
                            text = stringResource(R.string.search_quick_create_host, query),
                            color = tokens.textPrimary
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(SapienTheme.density.cardGap)
    ) {
        group(
            labelRes = R.string.search_section_hosts,
            items = results.hosts,
            key = { "h-${it.id}" }
        ) { host ->
            HostAccentCard(
                host = host,
                connectionState = ConnectionState.UNKNOWN,
                onTap = { onHostTap(host) }
            )
        }
        group(
            labelRes = R.string.search_section_commands,
            items = results.commands,
            key = { "c-${it.id}" }
        ) { cmd ->
            CommandAccentCard(command = cmd, onTap = { onCommandTap(cmd) })
        }
        group(
            labelRes = R.string.search_section_keys,
            items = results.credentials,
            key = { "k-${it.id}" }
        ) { cred ->
            KeyAccentCard(credential = cred, onTap = { onCredentialTap(cred) })
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

private fun <T> LazyListScope.group(
    labelRes: Int,
    items: List<T>,
    key: (T) -> Any,
    row: @Composable (T) -> Unit
) {
    if (items.isEmpty()) return
    item { SectionHeader(labelRes = labelRes, count = items.size) }
    val preview = items.take(PREVIEW_ROWS)
    items(items = preview, key = key) { row(it) }
    if (items.size > PREVIEW_ROWS) {
        val extra = items.drop(PREVIEW_ROWS)
        item { ShowAllRow(extraCount = extra.size, extra = extra, key = key, row = row) }
    }
    item { Spacer(Modifier.height(16.dp)) }
}

@Composable
private fun SectionHeader(labelRes: Int, count: Int) {
    val tokens = SapienTheme.tokens
    Text(
        text = stringResource(R.string.search_section_count, stringResource(labelRes), count),
        color = tokens.textMuted,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun <T> ShowAllRow(
    extraCount: Int,
    extra: List<T>,
    key: (T) -> Any,
    row: @Composable (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tokens = SapienTheme.tokens
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SapienTheme.density.cardGap)) {
        if (expanded) {
            extra.forEach { item ->
                androidx.compose.runtime.key(key(item)) { row(item) }
            }
        }
        Text(
            text = if (expanded) stringResource(R.string.search_show_less) else stringResource(R.string.search_show_all, extraCount),
            color = tokens.primary,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable { expanded = !expanded }
        )
    }
}
