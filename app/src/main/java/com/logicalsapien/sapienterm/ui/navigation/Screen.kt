/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
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

package com.logicalsapien.sapienterm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.logicalsapien.sapienterm.R

sealed class Screen(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    object Home : Screen(NavDestinations.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home)
    object Search : Screen(NavDestinations.SEARCH, R.string.nav_search, Icons.Outlined.Search, Icons.Filled.Search)
    object Settings : Screen(NavDestinations.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Search, Settings)
    }
}
