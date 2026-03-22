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

@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.logicalsapien.sapienssh.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.logicalsapien.sapienssh.data.entity.Host
import com.logicalsapien.sapienssh.service.TerminalManager
import com.logicalsapien.sapienssh.ui.navigation.BottomNavBar
import com.logicalsapien.sapienssh.ui.navigation.ConnectBotNavHost
import com.logicalsapien.sapienssh.ui.navigation.NavDestinations
import com.logicalsapien.sapienssh.ui.navigation.Screen
import com.logicalsapien.sapienssh.ui.theme.SapienSSHTheme
import com.logicalsapien.sapienssh.util.IconStyle

val LocalTerminalManager = compositionLocalOf<TerminalManager?> {
    null
}

@Composable
fun ConnectBotApp(
    appUiState: AppUiState,
    navController: NavHostController,
    makingShortcut: Boolean,
    authRequired: Boolean,
    isAuthenticated: Boolean,
    onAuthenticationSuccess: () -> Unit,
    onRetryMigration: () -> Unit,
    onSelectShortcut: (Host, String?, IconStyle) -> Unit,
    onNavigateToConsole: (Host) -> Unit,
    modifier: Modifier = Modifier
) {
    SapienSSHTheme {
        when (appUiState) {
            is AppUiState.Loading -> {
                LoadingScreen(modifier = modifier)
            }

            is AppUiState.MigrationInProgress -> {
                MigrationScreen(
                    uiState = MigrationUiState.InProgress(appUiState.state),
                    onRetry = onRetryMigration,
                    modifier = modifier
                )
            }

            is AppUiState.MigrationFailed -> {
                MigrationScreen(
                    uiState = MigrationUiState.Failed(
                        appUiState.error,
                        appUiState.debugLog
                    ),
                    onRetry = onRetryMigration,
                    modifier = modifier
                )
            }

            is AppUiState.Ready -> {
                if (authRequired && !isAuthenticated && !makingShortcut) {
                    AuthenticationScreen(
                        onAuthenticationSuccess = onAuthenticationSuccess,
                        modifier = modifier
                    )
                } else {
                    CompositionLocalProvider(LocalTerminalManager provides appUiState.terminalManager) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        val bottomNavRoutes = Screen.bottomNavItems.map { it.route }
                        val showBottomBar = currentRoute in bottomNavRoutes

                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    BottomNavBar(
                                        navController = navController,
                                        currentRoute = currentRoute
                                    )
                                }
                            }
                        ) { innerPadding ->
                            ConnectBotNavHost(
                                navController = navController,
                                startDestination = NavDestinations.HOST_LIST,
                                makingShortcut = makingShortcut,
                                onSelectShortcut = onSelectShortcut,
                                onNavigateToConsole = onNavigateToConsole,
                                modifier = modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
