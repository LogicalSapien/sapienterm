/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.CustomBottomBarLayout
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarPanel
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarShortcutAction
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarShortcutVisual
import com.logicalsapien.sapienterm.ui.theme.SapienTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomBottomBarLayoutEditorDialog(
    initial: CustomBottomBarLayout?,
    onDismiss: () -> Unit,
    onSave: (name: String, actionIds: List<String>, existingLayoutId: String?) -> Unit,
    onOpenIconLegend: () -> Unit = {}
) {
    val tokens = SapienTheme.tokens
    var name by remember(initial?.id) {
        mutableStateOf(initial?.name.orEmpty())
    }
    var orderedIds by remember(initial?.id) {
        mutableStateOf(initial?.actionIds.orEmpty().toMutableList())
    }
    var addMenuExpanded by remember { mutableStateOf(false) }
    val availableToAdd = remember(orderedIds) {
        BottomBarShortcutAction.entries.filter { a ->
            orderedIds.none { it == a.id } &&
                a != BottomBarShortcutAction.MORE_KEYS
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    stringResource(R.string.pref_custom_bottom_bar_create_title)
                } else {
                    stringResource(R.string.pref_custom_bottom_bar_edit_title)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.pref_custom_bottom_bar_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.pref_custom_bottom_bar_shortcuts_label),
                    style = MaterialTheme.typography.labelMedium
                )
                BottomBarLivePreview(actionIds = orderedIds)
                TextButton(onClick = onOpenIconLegend) {
                    Text(stringResource(R.string.pref_bottom_bar_icon_legend_button))
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    orderedIds.forEachIndexed { index, id ->
                        val action = BottomBarShortcutAction.fromId(id) ?: return@forEachIndexed
                        key(id) {
                            val removeDesc =
                                stringResource(R.string.pref_custom_bottom_bar_remove_chip_a11y, action.displayLabel)
                            Row(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = tokens.surfaceBorder,
                                        shape = RoundedCornerShape(tokens.cornerSmall)
                                    )
                                    .background(tokens.surface, RoundedCornerShape(tokens.cornerSmall))
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .heightIn(min = 48.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (index <= 0) return@IconButton
                                        orderedIds = orderedIds.toMutableList().apply {
                                            val j = index - 1
                                            val t = this[index]
                                            this[index] = this[j]
                                            this[j] = t
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ExpandLess,
                                        contentDescription = stringResource(R.string.pref_custom_bottom_bar_move_up),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (index >= orderedIds.lastIndex) return@IconButton
                                        orderedIds = orderedIds.toMutableList().apply {
                                            val j = index + 1
                                            val t = this[index]
                                            this[index] = this[j]
                                            this[j] = t
                                        }
                                    },
                                    enabled = index < orderedIds.lastIndex,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ExpandMore,
                                        contentDescription = stringResource(R.string.pref_custom_bottom_bar_move_down),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .heightIn(min = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BottomBarShortcutVisual(
                                        action = action,
                                        panel = BottomBarPanel.NONE,
                                        modifierState = null,
                                        tintActive = MaterialTheme.colorScheme.primary,
                                        tintIdle = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = action.displayLabel,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = stringResource(action.descriptionRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        orderedIds = orderedIds.toMutableList().apply { remove(id) }
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = removeDesc,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (orderedIds.size < CustomBottomBarLayout.MAX_ACTIONS && availableToAdd.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = addMenuExpanded,
                        onExpandedChange = { addMenuExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stringResource(R.string.pref_custom_bottom_bar_add_key),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addMenuExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = addMenuExpanded,
                            onDismissRequest = { addMenuExpanded = false }
                        ) {
                            for (action in availableToAdd) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = action.displayLabel,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = stringResource(action.descriptionRes),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (orderedIds.size < CustomBottomBarLayout.MAX_ACTIONS) {
                                            orderedIds = orderedIds.toMutableList().apply { add(action.id) }
                                        }
                                        addMenuExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val n = name.trim().ifBlank { "Custom" }
                    val ids = orderedIds.toList()
                    if (ids.isEmpty()) return@TextButton
                    onSave(n, ids, initial?.id)
                },
                enabled = orderedIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.portforward_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
private fun BottomBarLivePreview(actionIds: List<String>) {
    val tokens = SapienTheme.tokens
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.pref_custom_bottom_bar_preview_label),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(tokens.surface, RoundedCornerShape(tokens.cornerSmall))
                .border(1.dp, tokens.surfaceBorder, RoundedCornerShape(tokens.cornerSmall))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (actionIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.pref_custom_bottom_bar_preview_empty),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted
                )
            } else {
                actionIds.forEach { id ->
                    val action = BottomBarShortcutAction.fromId(id) ?: return@forEach
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomBarShortcutVisual(
                            action = action,
                            panel = BottomBarPanel.NONE,
                            modifierState = null,
                            tintActive = tokens.primary,
                            tintIdle = tokens.textPrimary
                        )
                    }
                }
            }
        }
    }
}
