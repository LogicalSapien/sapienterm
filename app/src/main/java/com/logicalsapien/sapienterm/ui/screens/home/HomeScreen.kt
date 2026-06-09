/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.ui.LocalTerminalManager
import com.logicalsapien.sapienterm.ui.components.SegmentOption
import com.logicalsapien.sapienterm.ui.components.SegmentedControl
import com.logicalsapien.sapienterm.ui.screens.hostlist.HostListViewModel
import com.logicalsapien.sapienterm.ui.theme.DensityTokens
import com.logicalsapien.sapienterm.ui.theme.LocalSapienDensity
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun HomeScreen(
    onNavigateToConsole: (Host) -> Unit,
    onNavigateToEditHost: (Host?) -> Unit,
    onNavigateToQuickCommands: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    hostListViewModel: HostListViewModel = hiltViewModel()
) {
    val tokens = SapienTheme.tokens
    val segment by homeViewModel.selectedSegment.collectAsState()
    val densityMode by homeViewModel.density.collectAsState()
    val densityTokens = DensityTokens.forMode(densityMode)

    // Hook the shared HostListViewModel so Sort + Disconnect All still dispatch through it.
    val terminalManager = LocalTerminalManager.current
    LaunchedEffect(terminalManager) {
        terminalManager?.let { hostListViewModel.setTerminalManager(it) }
    }

    val segments = listOf(
        SegmentOption(HomeSegment.HOSTS, stringResource(R.string.home_segment_hosts)),
        SegmentOption(HomeSegment.COMMANDS, stringResource(R.string.home_segment_commands)),
        SegmentOption(HomeSegment.KEYS, stringResource(R.string.home_segment_keys))
    )

    CompositionLocalProvider(LocalSapienDensity provides densityTokens) {
        Scaffold(
            containerColor = tokens.background,
            floatingActionButton = {
                HomeFab(
                    segment = segment,
                    onClick = {
                        when (segment) {
                            HomeSegment.HOSTS -> onNavigateToEditHost(null)
                            HomeSegment.COMMANDS -> onNavigateToQuickCommands()
                            HomeSegment.KEYS -> onNavigateToCredentials()
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(tokens.background)
                    .padding(padding)
            ) {
                HomeHeader(
                    density = densityMode,
                    onCycleDensity = homeViewModel::cycleDensity,
                    onSortByColor = hostListViewModel::toggleSortOrder,
                    onDisconnectAll = hostListViewModel::disconnectAll
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    SegmentedControl(
                        options = segments,
                        selected = segment,
                        onSelectedChange = homeViewModel::setSegment
                    )
                }
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = segment,
                    transitionSpec = {
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    },
                    label = "home_segment"
                ) { current ->
                    when (current) {
                        HomeSegment.HOSTS -> HomeHostsContent(
                            onNavigateToConsole = onNavigateToConsole,
                            onNavigateToEditHost = onNavigateToEditHost,
                            viewModel = hostListViewModel
                        )
                        HomeSegment.COMMANDS -> HomeCommandsContent()
                        HomeSegment.KEYS -> HomeKeysContent()
                    }
                }
            }
        }
    }
}
