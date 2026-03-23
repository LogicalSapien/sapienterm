/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.data.entity.ConnectionGroup
import com.logicalsapien.sapienssh.data.entity.Host
import com.logicalsapien.sapienssh.ui.LocalTerminalManager
import com.logicalsapien.sapienssh.ui.PreviewScreen
import com.logicalsapien.sapienssh.ui.components.DisconnectAllDialog
import com.logicalsapien.sapienssh.ui.components.ShortcutCustomizationDialog
import com.logicalsapien.sapienssh.ui.theme.SapienSSHTheme
import com.logicalsapien.sapienssh.ui.theme.StatusGreen
import com.logicalsapien.sapienssh.ui.theme.StatusRed
import com.logicalsapien.sapienssh.util.IconStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    onNavigateToConsole: (Host) -> Unit,
    onNavigateToEditHost: (Host?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPubkeys: () -> Unit,
    onNavigateToPortForwards: (Host) -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToHelp: () -> Unit,
    modifier: Modifier = Modifier,
    makingShortcut: Boolean = false,
    onSelectShortcut: (Host, String?, IconStyle) -> Unit = { _, _, _ -> },
    viewModel: HostListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val terminalManager = LocalTerminalManager.current

    LaunchedEffect(terminalManager) {
        terminalManager?.let { viewModel.setTerminalManager(it) }
    }

    val uiState by viewModel.uiState.collectAsState()
    val healthStatusMap by viewModel.healthStatus.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroupId by viewModel.selectedGroupId.collectAsState()
    val scope = rememberCoroutineScope()

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && uiState.exportedJson != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(uiState.exportedJson!!.toByteArray())
                    }
                    val exportResult = uiState.exportResult
                    if (exportResult != null) {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.export_hosts_success,
                                exportResult.hostCount,
                                exportResult.profileCount
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.export_hosts_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.clearExportedJson()
            }
        } else {
            viewModel.clearExportedJson()
        }
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().readText()
                    }
                    if (jsonString != null) {
                        viewModel.importHosts(jsonString)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.import_hosts_failed, e.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Show errors as Toast notifications
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    // Handle export result - launch file picker when JSON is ready
    LaunchedEffect(uiState.exportedJson) {
        if (uiState.exportedJson != null) {
            exportLauncher.launch(context.getString(R.string.export_hosts_filename))
        }
    }

    // Handle import result
    LaunchedEffect(uiState.importResult) {
        uiState.importResult?.let { result ->
            Toast.makeText(
                context,
                context.getString(
                    R.string.import_hosts_success,
                    result.hostsImported,
                    result.hostsSkipped,
                    result.profilesImported,
                    result.profilesSkipped
                ),
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearImportResult()
        }
    }

    var shortcutHost by remember { mutableStateOf<Host?>(null) }

    if (shortcutHost != null) {
        ShortcutCustomizationDialog(
            host = shortcutHost!!,
            onDismiss = { shortcutHost = null },
            onConfirm = { color, iconStyle ->
                onSelectShortcut(shortcutHost!!, color, iconStyle)
                shortcutHost = null
            }
        )
    }

    HostListScreenContent(
        uiState = uiState,
        healthStatusMap = healthStatusMap,
        groups = groups,
        selectedGroupId = selectedGroupId,
        makingShortcut = makingShortcut,
        onNavigateToConsole = onNavigateToConsole,
        onSelectShortcut = { host -> shortcutHost = host },
        onNavigateToEditHost = onNavigateToEditHost,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToPubkeys = onNavigateToPubkeys,
        onNavigateToPortForwards = onNavigateToPortForwards,
        onNavigateToProfiles = onNavigateToProfiles,
        onNavigateToHelp = onNavigateToHelp,
        onToggleSortOrder = viewModel::toggleSortOrder,
        onDeleteHost = viewModel::deleteHost,
        onDuplicateHost = viewModel::duplicateHost,
        onRenameHost = viewModel::renameHost,
        onForgetHostKeys = viewModel::forgetHostKeys,
        onDisconnectHost = viewModel::disconnectHost,
        onDisconnectAll = viewModel::disconnectAll,
        onExportHosts = viewModel::exportHosts,
        onImportHosts = { importLauncher.launch(arrayOf("application/json")) },
        onSelectGroup = viewModel::selectGroup,
        onCreateGroup = viewModel::createGroup,
        onRenameGroup = viewModel::renameGroup,
        onDeleteGroup = viewModel::deleteGroup,
        onMoveHostToGroup = viewModel::moveHostToGroup,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreenContent(
    uiState: HostListUiState,
    healthStatusMap: Map<Long, HealthStatus> = emptyMap(),
    groups: List<ConnectionGroup> = emptyList(),
    selectedGroupId: Long? = null,
    makingShortcut: Boolean = false,
    onNavigateToConsole: (Host) -> Unit,
    onSelectShortcut: (Host) -> Unit = {},
    onNavigateToEditHost: (Host?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPubkeys: () -> Unit,
    onNavigateToPortForwards: (Host) -> Unit,
    onNavigateToProfiles: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onToggleSortOrder: () -> Unit,
    onDeleteHost: (Host) -> Unit,
    onDuplicateHost: (Host) -> Unit,
    onRenameHost: (Host, String) -> Unit = { _, _ -> },
    onForgetHostKeys: (Host) -> Unit,
    onDisconnectHost: (Host) -> Unit,
    onDisconnectAll: () -> Unit,
    onExportHosts: () -> Unit = {},
    onImportHosts: () -> Unit = {},
    onSelectGroup: (Long?) -> Unit = {},
    onCreateGroup: (String) -> Unit = {},
    onRenameGroup: (ConnectionGroup, String) -> Unit = { _, _ -> },
    onDeleteGroup: (ConnectionGroup) -> Unit = {},
    onMoveHostToGroup: (Long, Long?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDisconnectAllDialog by remember { mutableStateOf(false) }
    var hostToDelete by remember { mutableStateOf<Host?>(null) }
    var hostToRename by remember { mutableStateOf<Host?>(null) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<ConnectionGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<ConnectionGroup?>(null) }
    var hostToMoveToGroup by remember { mutableStateOf<Host?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Filter hosts by selected group
    val filteredHosts = remember(uiState.hosts, selectedGroupId) {
        if (selectedGroupId == null) {
            uiState.hosts
        } else {
            uiState.hosts.filter { it.groupId == selectedGroupId }
        }
    }

    // Show snackbar when there's an error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (!makingShortcut) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.button_more_options))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            if (uiState.sortedByColor) {
                                                R.string.list_menu_sortname
                                            } else {
                                                R.string.list_menu_sortcolor
                                            }
                                        )
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onToggleSortOrder()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_menu_settings)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.profile_list_title)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToProfiles()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_menu_pubkeys)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToPubkeys()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_menu_export_hosts)) },
                                onClick = {
                                    showMenu = false
                                    onExportHosts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_menu_import_hosts)) },
                                onClick = {
                                    showMenu = false
                                    onImportHosts()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.list_menu_disconnect)) },
                                onClick = {
                                    showMenu = false
                                    showDisconnectAllDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.title_help)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToHelp()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!makingShortcut) {
                FloatingActionButton(
                    onClick = { onNavigateToEditHost(null) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.hostpref_add_host))
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Group filter chips row
            if (groups.isNotEmpty() && !makingShortcut) {
                GroupFilterChips(
                    groups = groups,
                    selectedGroupId = selectedGroupId,
                    onSelectGroup = onSelectGroup,
                    onCreateGroup = { showCreateGroupDialog = true },
                    onRenameGroup = { groupToRename = it },
                    onDeleteGroup = { groupToDelete = it }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    filteredHosts.isEmpty() -> {
                        // Enhanced empty state with icon
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cable,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_connections_cta),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { onNavigateToEditHost(null) }) {
                                Text(stringResource(R.string.hostpref_add_host))
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 16.dp,
                                bottom = 104.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                items = filteredHosts,
                                key = { it.id }
                            ) { host ->
                                val connectionState = uiState.connectionStates[host.id] ?: ConnectionState.UNKNOWN

                                if (makingShortcut) {
                                    // In shortcut mode, just show plain cards without swipe
                                    ConnectionCard(
                                        host = host,
                                        connectionState = connectionState,
                                        onClick = { onSelectShortcut(host) },
                                        healthStatus = healthStatusMap[host.id]
                                    )
                                } else {
                                    SwipeToDismissHostItem(
                                        host = host,
                                        connectionState = connectionState,
                                        healthStatus = healthStatusMap[host.id],
                                        onClick = { onNavigateToConsole(host) },
                                        onEdit = { onNavigateToEditHost(host) },
                                        onDelete = { hostToDelete = host },
                                        onClone = {
                                            onDuplicateHost(host)
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.connection_cloned)
                                                )
                                            }
                                        },
                                        onRename = { hostToRename = host },
                                        onMoveToGroup = { hostToMoveToGroup = host }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisconnectAllDialog) {
        DisconnectAllDialog(
            onDismiss = { showDisconnectAllDialog = false },
            onConfirm = {
                showDisconnectAllDialog = false
                onDisconnectAll()
            }
        )
    }

    // Delete confirmation dialog
    hostToDelete?.let { host ->
        HostDeleteDialog(
            host = host,
            onDismiss = { hostToDelete = null },
            onConfirm = {
                hostToDelete = null
                onDeleteHost(host)
            }
        )
    }

    // Rename dialog
    hostToRename?.let { host ->
        HostRenameDialog(
            host = host,
            onDismiss = { hostToRename = null },
            onConfirm = { newName ->
                hostToRename = null
                onRenameHost(host, newName)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.connection_renamed)
                    )
                }
            }
        )
    }

    // Create group dialog
    if (showCreateGroupDialog) {
        GroupNameDialog(
            title = stringResource(R.string.group_create_title),
            initialName = "",
            onDismiss = { showCreateGroupDialog = false },
            onConfirm = { name ->
                showCreateGroupDialog = false
                onCreateGroup(name)
            }
        )
    }

    // Rename group dialog
    groupToRename?.let { group ->
        GroupNameDialog(
            title = stringResource(R.string.group_rename_title),
            initialName = group.name,
            onDismiss = { groupToRename = null },
            onConfirm = { newName ->
                groupToRename = null
                onRenameGroup(group, newName)
            }
        )
    }

    // Delete group confirmation dialog
    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            text = {
                Text(stringResource(R.string.group_delete_confirm, group.name))
            },
            confirmButton = {
                TextButton(onClick = {
                    groupToDelete = null
                    onDeleteGroup(group)
                }) {
                    Text(stringResource(R.string.button_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text(stringResource(R.string.button_no))
                }
            }
        )
    }

    // Move host to group dialog
    hostToMoveToGroup?.let { host ->
        MoveToGroupDialog(
            groups = groups,
            currentGroupId = host.groupId,
            onDismiss = { hostToMoveToGroup = null },
            onSelect = { groupId ->
                hostToMoveToGroup = null
                onMoveHostToGroup(host.id, groupId)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissHostItem(
    host: Host,
    connectionState: ConnectionState,
    healthStatus: HealthStatus? = null,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClone: () -> Unit = {},
    onRename: () -> Unit = {},
    onMoveToGroup: () -> Unit = {}
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped left -> delete (show confirmation dialog)
                    onDelete()
                    false // Don't actually dismiss; let the dialog handle it
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped right -> edit
                    onEdit()
                    false // Reset after triggering edit
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val backgroundColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> StatusRed.copy(alpha = 0.2f)
                    SwipeToDismissBoxValue.StartToEnd -> StatusGreen.copy(alpha = 0.2f)
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
                },
                label = "swipe_bg_color"
            )

            val iconTint by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> StatusRed
                    SwipeToDismissBoxValue.StartToEnd -> StatusGreen
                    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurface
                },
                label = "swipe_icon_tint"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.Settled -> Alignment.CenterStart
                }
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.swipe_to_delete),
                                tint = iconTint
                            )
                            Text(
                                text = stringResource(R.string.swipe_to_delete),
                                style = MaterialTheme.typography.labelSmall,
                                color = iconTint
                            )
                        }
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.swipe_to_edit),
                                tint = iconTint
                            )
                            Text(
                                text = stringResource(R.string.swipe_to_edit),
                                style = MaterialTheme.typography.labelSmall,
                                color = iconTint
                            )
                        }
                    }
                    SwipeToDismissBoxValue.Settled -> { /* nothing */ }
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        ConnectionCard(
            host = host,
            connectionState = connectionState,
            onClick = onClick,
            healthStatus = healthStatus,
            onClone = onClone,
            onRename = onRename,
            onEdit = onEdit,
            onDelete = onDelete,
            onMoveToGroup = onMoveToGroup
        )
    }
}

@Composable
private fun HostRenameDialog(
    host: Host,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nickname by remember { mutableStateOf(host.nickname) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_host_title)) },
        text = {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(nickname) },
                enabled = nickname.isNotBlank() && nickname != host.nickname
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun HostDeleteDialog(
    host: Host,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_host_delete)) },
        text = {
            Text(stringResource(R.string.delete_host_confirm, host.nickname))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.button_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_no))
            }
        }
    )
}

/**
 * Horizontal scrollable row of filter chips for connection groups.
 * Shows "All" chip, one chip per group, and a "+" chip to create a new group.
 * Long-press on a group chip shows rename/delete options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupFilterChips(
    groups: List<ConnectionGroup>,
    selectedGroupId: Long?,
    onSelectGroup: (Long?) -> Unit,
    onCreateGroup: () -> Unit,
    onRenameGroup: (ConnectionGroup) -> Unit,
    onDeleteGroup: (ConnectionGroup) -> Unit
) {
    var groupMenuTarget by remember { mutableStateOf<ConnectionGroup?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All" chip — always first
        FilterChip(
            selected = selectedGroupId == null,
            onClick = { onSelectGroup(null) },
            label = { Text(stringResource(R.string.group_all)) }
        )

        // One chip per group
        groups.forEach { group ->
            Box {
                FilterChip(
                    selected = selectedGroupId == group.id,
                    onClick = { onSelectGroup(group.id) },
                    label = { Text(group.name) },
                    modifier = Modifier.combinedClickable(
                        onClick = { onSelectGroup(group.id) },
                        onLongClick = { groupMenuTarget = group }
                    )
                )

                // Context menu for long-pressed group chip
                DropdownMenu(
                    expanded = groupMenuTarget == group,
                    onDismissRequest = { groupMenuTarget = null }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.group_rename)) },
                        onClick = {
                            groupMenuTarget = null
                            onRenameGroup(group)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.group_delete)) },
                        onClick = {
                            groupMenuTarget = null
                            onDeleteGroup(group)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }

        // "+" chip to create a new group
        FilterChip(
            selected = false,
            onClick = onCreateGroup,
            label = { Text("+") },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/**
 * Dialog for creating or renaming a connection group.
 */
@Composable
private fun GroupNameDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.group_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for moving a host to a group.
 * Shows a list of groups plus a "No group" option.
 */
@Composable
private fun MoveToGroupDialog(
    groups: List<ConnectionGroup>,
    currentGroupId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_move_to)) },
        text = {
            Column {
                // "No group" option
                TextButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.group_no_group),
                            color = if (currentGroupId == null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                // Group options
                groups.forEach { group ->
                    TextButton(
                        onClick = { onSelect(group.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = group.name,
                                color = if (currentGroupId == group.id) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@PreviewScreen
@Composable
private fun HostListScreenEmptyPreview() {
    SapienSSHTheme {
        HostListScreenContent(
            uiState = HostListUiState(
                hosts = emptyList(),
                isLoading = false
            ),
            onNavigateToConsole = {},
            onNavigateToEditHost = {},
            onNavigateToSettings = {},
            onNavigateToPubkeys = {},
            onNavigateToPortForwards = {},
            onNavigateToProfiles = {},
            onNavigateToHelp = {},
            onToggleSortOrder = {},
            onDeleteHost = {},
            onDuplicateHost = {},
            onForgetHostKeys = {},
            onDisconnectHost = {},
            onDisconnectAll = {}
        )
    }
}

@PreviewScreen
@Composable
private fun HostListScreenLoadingPreview() {
    SapienSSHTheme {
        HostListScreenContent(
            uiState = HostListUiState(
                hosts = emptyList(),
                isLoading = true
            ),
            onNavigateToConsole = {},
            onNavigateToEditHost = {},
            onNavigateToSettings = {},
            onNavigateToPubkeys = {},
            onNavigateToPortForwards = {},
            onNavigateToProfiles = {},
            onNavigateToHelp = {},
            onToggleSortOrder = {},
            onDeleteHost = {},
            onDuplicateHost = {},
            onForgetHostKeys = {},
            onDisconnectHost = {},
            onDisconnectAll = {}
        )
    }
}

@PreviewScreen
@Composable
private fun HostListScreenErrorPreview() {
    SapienSSHTheme {
        HostListScreenContent(
            uiState = HostListUiState(
                hosts = emptyList(),
                isLoading = false,
                error = "Failed to load hosts from database"
            ),
            onNavigateToConsole = {},
            onNavigateToEditHost = {},
            onNavigateToSettings = {},
            onNavigateToPubkeys = {},
            onNavigateToPortForwards = {},
            onNavigateToProfiles = {},
            onNavigateToHelp = {},
            onToggleSortOrder = {},
            onDeleteHost = {},
            onDuplicateHost = {},
            onForgetHostKeys = {},
            onDisconnectHost = {},
            onDisconnectAll = {}
        )
    }
}

@PreviewScreen
@Composable
private fun HostListScreenPopulatedPreview() {
    SapienSSHTheme {
        HostListScreenContent(
            uiState = HostListUiState(
                hosts = listOf(
                    Host(
                        id = 1,
                        nickname = "Production Server",
                        protocol = "ssh",
                        username = "root",
                        hostname = "prod.example.com",
                        port = 22,
                        color = "#4CAF50",
                        lastConnect = System.currentTimeMillis() - 7200000
                    ),
                    Host(
                        id = 2,
                        nickname = "Development",
                        protocol = "ssh",
                        username = "developer",
                        hostname = "dev.example.com",
                        port = 2222,
                        color = "#2196F3",
                        lastConnect = System.currentTimeMillis() - 86400000
                    ),
                    Host(
                        id = 3,
                        nickname = "Local VM",
                        protocol = "ssh",
                        username = "admin",
                        hostname = "192.168.1.100",
                        port = 22,
                        color = "#FF9800"
                    )
                ),
                connectionStates = mapOf(
                    1L to ConnectionState.CONNECTED,
                    2L to ConnectionState.DISCONNECTED,
                    3L to ConnectionState.UNKNOWN
                ),
                isLoading = false
            ),
            onNavigateToConsole = {},
            onNavigateToEditHost = {},
            onNavigateToSettings = {},
            onNavigateToPubkeys = {},
            onNavigateToPortForwards = {},
            onNavigateToProfiles = {},
            onNavigateToHelp = {},
            onToggleSortOrder = {},
            onDeleteHost = {},
            onDuplicateHost = {},
            onForgetHostKeys = {},
            onDisconnectHost = {},
            onDisconnectAll = {}
        )
    }
}
