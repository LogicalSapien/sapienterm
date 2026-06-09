/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.ui.LocalTerminalManager
import com.logicalsapien.sapienterm.ui.components.EmptyState
import com.logicalsapien.sapienterm.ui.components.HostAccentCard
import com.logicalsapien.sapienterm.ui.components.QuickFilterChip
import com.logicalsapien.sapienterm.ui.components.QuickFilterChipRow
import com.logicalsapien.sapienterm.ui.screens.hostlist.ConnectionState
import com.logicalsapien.sapienterm.ui.screens.hostlist.HostListViewModel
import com.logicalsapien.sapienterm.ui.theme.HostCategoryColor
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

private const val RECENT_WINDOW_MS = 24L * 60 * 60 * 1000

@Composable
fun HomeHostsContent(
    onNavigateToConsole: (Host) -> Unit,
    onNavigateToEditHost: (Host?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HostListViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val tokens = SapienTheme.tokens
    val terminalManager = LocalTerminalManager.current
    LaunchedEffect(terminalManager) {
        terminalManager?.let { viewModel.setTerminalManager(it) }
    }
    val uiState by viewModel.uiState.collectAsState()

    val selectedChipId by homeViewModel.hostFilterChipId.collectAsState()
    var menuHost by remember { mutableStateOf<Host?>(null) }
    var hostToDelete by remember { mutableStateOf<Host?>(null) }

    // Build chips based on colors present in current host set
    val presentColors = remember(uiState.hosts) {
        uiState.hosts
            .map { HostCategoryColor.fromStorageString(it.color) }
            .toSet()
            .sortedBy { it.displayOrder }
    }
    val chips = remember(presentColors) {
        buildList {
            add(QuickFilterChip("all", "All"))
            add(QuickFilterChip("favourites", "Favourites"))
            add(QuickFilterChip("recent", "Recent"))
            add(QuickFilterChip("online", "Online"))
            presentColors.forEach { color ->
                add(QuickFilterChip("color:${color.storageString}", color.name.lowercase().replaceFirstChar { it.uppercase() }))
            }
        }
    }

    val now = remember { System.currentTimeMillis() }
    val filteredHosts = remember(uiState.hosts, uiState.connectionStates, selectedChipId) {
        when {
            selectedChipId == "all" -> uiState.hosts
            selectedChipId == "favourites" -> uiState.hosts.filter { it.pinned }
            selectedChipId == "recent" -> uiState.hosts
                .filter { it.lastConnect > 0 && (now - it.lastConnect) <= RECENT_WINDOW_MS }
                .sortedByDescending { it.lastConnect }
            selectedChipId == "online" -> uiState.hosts.filter {
                uiState.connectionStates[it.id] == ConnectionState.CONNECTED
            }
            selectedChipId.startsWith("color:") -> {
                val target = selectedChipId.removePrefix("color:")
                uiState.hosts.filter {
                    HostCategoryColor.fromStorageString(it.color).storageString == target
                }
            }
            else -> uiState.hosts
        }
    }

    hostToDelete?.let { host ->
        AlertDialog(
            onDismissRequest = { hostToDelete = null },
            title = { Text(stringResource(R.string.list_host_delete)) },
            text = { Text(stringResource(R.string.delete_host_confirm, host.nickname)) },
            confirmButton = {
                TextButton(onClick = {
                    hostToDelete = null
                    viewModel.deleteHost(host)
                }) { Text(stringResource(R.string.button_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { hostToDelete = null }) { Text(stringResource(R.string.button_no)) }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        QuickFilterChipRow(
            chips = chips,
            selectedId = selectedChipId,
            onSelect = { homeViewModel.setHostFilter(it) }
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                filteredHosts.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.Dns,
                        title = stringResource(R.string.home_empty_hosts),
                        hint = stringResource(R.string.home_empty_hosts_hint),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(SapienTheme.density.cardGap),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items = filteredHosts, key = { it.id }) { host ->
                            val state = uiState.connectionStates[host.id] ?: ConnectionState.UNKNOWN
                            Box {
                                HostAccentCard(
                                    host = host,
                                    connectionState = state,
                                    onTap = { onNavigateToConsole(host) },
                                    onLongPress = { menuHost = host }
                                )
                                if (host.pinned) {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = SapienTheme.tokens.colorFor(
                                            HostCategoryColor.resolveWithFallback(host.color, host.nickname)
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 6.dp, end = 10.dp)
                                            .size(14.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuHost?.id == host.id,
                                    onDismissRequest = { menuHost = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(
                                            if (host.pinned) R.string.host_action_unpin else R.string.host_action_pin
                                        )) },
                                        onClick = {
                                            menuHost = null
                                            viewModel.togglePin(host)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.list_host_duplicate)) },
                                        onClick = {
                                            menuHost = null
                                            viewModel.duplicateHost(host)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.list_host_edit)) },
                                        onClick = {
                                            menuHost = null
                                            onNavigateToEditHost(host)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.list_host_delete)) },
                                        onClick = {
                                            menuHost = null
                                            hostToDelete = host
                                        }
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(96.dp)) }
                    }
                }
            }
        }
    }
}
