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

package com.logicalsapien.sapienterm.ui

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.service.TerminalManager
import com.logicalsapien.sapienterm.ui.components.SapienBottomNavBar
import com.logicalsapien.sapienterm.ui.navigation.SapienTermNavHost
import com.logicalsapien.sapienterm.ui.navigation.NavDestinations
import com.logicalsapien.sapienterm.ui.navigation.Screen
import com.logicalsapien.sapienterm.ui.theme.SapienTermTheme
import com.logicalsapien.sapienterm.ui.theme.SapienTheme as SapienTokenTheme
import com.logicalsapien.sapienterm.util.IconStyle

val LocalTerminalManager = compositionLocalOf<TerminalManager?> {
    null
}

@Composable
fun SapienTermApp(
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
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    var themeMode by remember {
        mutableStateOf(prefs.getString("theme_mode", "auto") ?: "auto")
    }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "theme_mode") {
                themeMode = sharedPreferences.getString("theme_mode", "auto") ?: "auto"
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    SapienTermTheme(darkTheme = darkTheme) {
        SapienTokenTheme(darkTheme = darkTheme) {
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
                        val inConsole = currentRoute?.startsWith("${NavDestinations.CONSOLE}/") == true
                        val showBottomBar = currentRoute in bottomNavRoutes && !inConsole

                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    SapienBottomNavBar(
                                        navController = navController,
                                        currentRoute = currentRoute
                                    )
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            SapienTermNavHost(
                                navController = navController,
                                startDestination = NavDestinations.HOME,
                                makingShortcut = makingShortcut,
                                onSelectShortcut = onSelectShortcut,
                                onNavigateToConsole = onNavigateToConsole,
                                modifier = modifier.padding(
                                    bottom = innerPadding.calculateBottomPadding()
                                )
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
