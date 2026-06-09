/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.components.CommandAccentCard
import com.logicalsapien.sapienterm.ui.components.EmptyState
import com.logicalsapien.sapienterm.ui.screens.quickcommands.QuickCommandsViewModel
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun HomeCommandsContent(
    modifier: Modifier = Modifier,
    viewModel: QuickCommandsViewModel = hiltViewModel()
) {
    val commands by viewModel.commands.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (commands.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Terminal,
                title = stringResource(R.string.home_empty_commands),
                hint = stringResource(R.string.home_empty_commands_hint),
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(SapienTheme.density.cardGap),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = commands, key = { it.id }) { command ->
                    CommandAccentCard(
                        command = command,
                        onTap = { viewModel.sendCommand(command.command) }
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }
    }
}
