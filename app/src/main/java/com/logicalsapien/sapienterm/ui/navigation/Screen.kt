/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
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

package com.logicalsapien.sapienterm.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.logicalsapien.sapienterm.R

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    object Connections : Screen(NavDestinations.HOST_LIST, R.string.nav_connections, Icons.Default.Dns)
    object QuickCommands : Screen(NavDestinations.QUICK_COMMANDS, R.string.nav_quick_commands, Icons.Default.Code)
    object Credentials : Screen(NavDestinations.CREDENTIALS, R.string.nav_credentials, Icons.Default.Key)
    object Settings : Screen(NavDestinations.SETTINGS, R.string.nav_settings, Icons.Default.Settings)

    companion object {
        val bottomNavItems = listOf(Connections, QuickCommands, Credentials, Settings)
    }
}
