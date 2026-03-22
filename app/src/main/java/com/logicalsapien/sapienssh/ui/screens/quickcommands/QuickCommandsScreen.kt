/*
 * SapienSSH: modern SSH client for Android
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

package com.logicalsapien.sapienssh.ui.screens.quickcommands

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.data.entity.QuickCommand
import com.logicalsapien.sapienssh.ui.LocalTerminalManager
import com.logicalsapien.sapienssh.ui.theme.StatusGreen
import com.logicalsapien.sapienssh.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCommandsScreen(
    modifier: Modifier = Modifier,
    viewModel: QuickCommandsViewModel = hiltViewModel()
) {
    val terminalManager = LocalTerminalManager.current
    LaunchedEffect(terminalManager) {
        terminalManager?.let { viewModel.setTerminalManager(it) }
    }

    val commands by viewModel.commands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<QuickCommand?>(null) }
    var commandToDelete by remember { mutableStateOf<QuickCommand?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Quick Commands") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Quick Command")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search commands...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            if (categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                viewModel.selectCategory(
                                    if (selectedCategory == category) null else category
                                )
                            },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Content
            if (commands.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != null) {
                                "No matching commands found"
                            } else {
                                "Add your first Quick Command"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        if (searchQuery.isBlank() && selectedCategory == null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = { showAddDialog = true }) {
                                Text("Add Command")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 104.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = commands,
                        key = { it.id }
                    ) { command ->
                        SwipeToDismissCommandItem(
                            command = command,
                            onClick = {
                                // Tapping opens the edit dialog to view/edit the command.
                                // Sending to terminal is only done from the QuickCommandToolbar
                                // in the terminal screen.
                                commandToEdit = command
                            },
                            onEdit = { commandToEdit = command },
                            onDelete = { commandToDelete = command }
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        QuickCommandEditDialog(
            existingCategories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { title, command, category ->
                viewModel.addCommand(title, command, category)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    commandToEdit?.let { cmd ->
        QuickCommandEditDialog(
            existingCommand = cmd,
            existingCategories = categories,
            onDismiss = { commandToEdit = null },
            onSave = { title, command, category ->
                viewModel.updateCommand(
                    cmd.copy(
                        title = title,
                        command = command,
                        category = category
                    )
                )
                commandToEdit = null
            }
        )
    }

    // Delete confirmation dialog
    commandToDelete?.let { cmd ->
        AlertDialog(
            onDismissRequest = { commandToDelete = null },
            title = { Text("Delete Command") },
            text = {
                Text("Delete \"${cmd.title}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCommand(cmd)
                        commandToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { commandToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissCommandItem(
    command: QuickCommand,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
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
                                contentDescription = "Delete",
                                tint = iconTint
                            )
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelSmall,
                                color = iconTint
                            )
                        }
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = iconTint
                            )
                            Text(
                                text = "Edit",
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
        QuickCommandCard(
            command = command,
            onClick = onClick,
            onEdit = onEdit
        )
    }
}
