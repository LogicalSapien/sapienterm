/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.service.TerminalManager
import com.logicalsapien.sapienterm.ui.LocalTerminalManager
import com.logicalsapien.sapienterm.ui.components.SearchField
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun SearchScreen(
    onNavigateToConsole: (Host) -> Unit,
    onNavigateToEditHost: (Host?) -> Unit,
    onNavigateToCredentials: () -> Unit,
    onQuickCreateHost: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val tokens = SapienTheme.tokens
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val pinned by viewModel.pinned.collectAsState()

    val terminalManager = LocalTerminalManager.current
    var activeManager by remember { mutableStateOf<TerminalManager?>(null) }
    LaunchedEffect(terminalManager) {
        activeManager = terminalManager
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = tokens.textPrimary)) { append("Sapien") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = tokens.primary)) { append("Term") }
                },
                fontFamily = tokens.fontFamilyHeading
            )
        }
        SearchField(
            value = query,
            onValueChange = viewModel::setQuery
        )
        if (query.isBlank()) {
            SearchEmptyState(
                recents = recents,
                pinned = pinned,
                onHostTap = onNavigateToConsole
            )
        } else {
            SearchResultsList(
                query = query,
                results = results,
                onHostTap = onNavigateToConsole,
                onCommandTap = { cmd ->
                    val bridges = activeManager?.bridgesFlow?.value
                    if (!bridges.isNullOrEmpty()) {
                        bridges.last().injectString(cmd.command + "\r")
                    }
                },
                onCredentialTap = { onNavigateToCredentials() },
                onQuickCreateHost = onQuickCreateHost
            )
        }
    }
}
