/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarPanel
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarShortcutAction
import com.logicalsapien.sapienterm.ui.screens.console.BottomBarShortcutVisual

@Composable
fun BottomBarIconLegendDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pref_bottom_bar_icon_legend_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = stringResource(R.string.pref_bottom_bar_icon_legend_fixed_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_commands))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_history))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_up))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_down))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_enter))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_ctrl))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_more))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_edit))
                LegendLine(stringResource(R.string.pref_bottom_bar_icon_legend_main_keyboard))
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Text(
                    text = stringResource(R.string.pref_bottom_bar_icon_legend_custom_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                for (action in BottomBarShortcutAction.entries) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BottomBarShortcutVisual(
                            action = action,
                            panel = BottomBarPanel.NONE,
                            modifierState = null,
                            tintActive = MaterialTheme.colorScheme.primary,
                            tintIdle = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = stringResource(action.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_ok))
            }
        }
    )
}

@Composable
private fun LegendLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    )
}
