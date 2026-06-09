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

package com.logicalsapien.sapienterm.ui

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.preference.PreferenceManager
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.service.TerminalManager
import com.logicalsapien.sapienterm.ui.components.DisconnectAllDialog
import com.logicalsapien.sapienterm.ui.navigation.NavDestinations
import com.logicalsapien.sapienterm.ui.theme.SapienTermTheme
import com.logicalsapien.sapienterm.util.IconStyle
import com.logicalsapien.sapienterm.util.NotificationPermissionHelper
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.ShortcutIconGenerator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

// TODO: Move back to ComponentActivity when https://issuetracker.google.com/issues/178855209 is fixed.
//       FragmentActivity subclass is required for BiometricPrompt to find the FragmentManager
//       from Compose context.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        private const val STATE_SELECTED_URI = "selectedUri"
        const val DISCONNECT_ACTION = "com.logicalsapien.sapienterm.action.DISCONNECT"
    }

    internal lateinit var appViewModel: AppViewModel
    private var bound = false
    private var requestedUri: Uri? by mutableStateOf(null)
    private var pendingHostConnection: Host? by mutableStateOf(null)
    internal var makingShortcut by mutableStateOf(false)
    private var showDisconnectAllDialog by mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Check the actual permission status instead of relying on the launcher result.
        // If user went to settings and granted permission, the result will be false but
        // the actual permission may be granted.
        val actuallyGranted = NotificationPermissionHelper.isNotificationPermissionGranted(this)

        appViewModel.onNotificationPermissionResult(actuallyGranted)?.let { uri ->
            requestedUri = uri
        }
        // Connections do not require notification permission, so a denied permission
        // does not block any pending host connection. Navigation to, and clearing of,
        // any pendingHostConnection are handled unconditionally by the LaunchedEffect
        // in the composable UI that observes this state.
        if (!actuallyGranted && pendingHostConnection != null) {
            Timber.d("Permission denied; proceeding with any pending host connection")
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? TerminalManager.TerminalBinder
            val manager = binder?.getService()
            Timber.d("onServiceConnected: manager=$manager")
            appViewModel.setTerminalManager(manager)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.d("onServiceDisconnected")
            appViewModel.setTerminalManager(null)
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        appViewModel = ViewModelProvider(this)[AppViewModel::class.java]

        if (savedInstanceState == null) {
            requestedUri = intent?.data
            makingShortcut = Intent.ACTION_CREATE_SHORTCUT == intent?.action ||
                Intent.ACTION_PICK == intent?.action
            Timber.d("onCreate: requestedUri=$requestedUri, makingShortcut=$makingShortcut")
            handleIntent(intent)
        } else {
            savedInstanceState.getString(STATE_SELECTED_URI)?.let {
                requestedUri = it.toUri()
            }
        }

        val serviceIntent = Intent(this, TerminalManager::class.java)
        bindService(serviceIntent, connection, BIND_AUTO_CREATE)

        setContent {
            val appUiState by appViewModel.uiState.collectAsState()
            val pendingDisconnectAll by appViewModel.pendingDisconnectAll.collectAsState()
            val isAuthenticated by appViewModel.isAuthenticated.collectAsState()
            // Re-read at every recomposition (e.g. after onResume) so toggling the
            // setting in Settings takes effect on next foreground without a full
            // process restart.
            val authOnLaunchEnabled = appViewModel.authOnLaunchEnabled
            val navController = rememberNavController()
            val context = LocalContext.current
            var showPermissionRationale by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                appViewModel.showPermissionRationale.collect {
                    showPermissionRationale = true
                }
            }

            LaunchedEffect(Unit) {
                appViewModel.requestPermission.collect {
                    Timber.d("Received requestPermission event, SDK_INT=${Build.VERSION.SDK_INT}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Timber.d("Launching permission request")
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        Timber.d("Skipping permission request, SDK < TIRAMISU")
                    }
                }
            }

            LaunchedEffect(requestedUri, navController, appUiState) {
                Timber.d("LaunchedEffect: requestedUri=$requestedUri, appUiState=$appUiState")
                if (appUiState is AppUiState.Ready) {
                    requestedUri?.let { uri ->
                        Timber.d("Processing URI: $uri")
                        navController.let { controller ->
                            val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                this@MainActivity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                false
                            }

                            Timber.d("shouldShowRationale=$shouldShowRationale")
                            if (appViewModel.checkAndRequestNotificationPermission(context, uri, shouldShowRationale)) {
                                Timber.d("Permission check passed, handling connection")
                                handleConnectionUri(uri, controller)
                                requestedUri = null
                            } else {
                                Timber.d("Permission check blocked, waiting for permission")
                            }
                        }
                    }
                }
            }

            LaunchedEffect(pendingDisconnectAll, appUiState) {
                appViewModel.executePendingDisconnectAllIfReady()
            }

            LaunchedEffect(Unit) {
                appViewModel.finishActivity.collect {
                    if (context is Activity) {
                        context.finish()
                    }
                }
            }

            // Re-check permission status when activity resumes (e.g., user grants/revokes in Settings)
            ObservePermissionOnResume { isGranted ->
                if (pendingHostConnection != null) {
                    // Permission state changed and we have a pending connection
                    appViewModel.onNotificationPermissionResult(isGranted)
                }
            }

            if (showDisconnectAllDialog) {
                SapienTermTheme {
                    DisconnectAllDialog(
                        onDismiss = {
                            Timber.d("User cancelled disconnectAll")
                            showDisconnectAllDialog = false
                        },
                        onConfirm = {
                            Timber.d("User confirmed disconnectAll")
                            showDisconnectAllDialog = false
                            appViewModel.setPendingDisconnectAll(true)
                        }
                    )
                }
            }

            // Navigate to console when pending host connection is set
            LaunchedEffect(pendingHostConnection, appUiState) {
                if (appUiState is AppUiState.Ready) {
                    pendingHostConnection?.let { host ->
                        Timber.d("Navigating to console for pending host: ${host.nickname}")
                        pendingHostConnection = null
                        navController.navigate("${NavDestinations.CONSOLE}/${host.id}") {
                            popUpTo(NavDestinations.HOST_LIST) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }

            if (showPermissionRationale) {
                SapienTermTheme {
                    NotificationPermissionRationaleDialog(
                        onDismiss = {
                            Timber.d("User dismissed permission rationale, proceeding anyway")
                            showPermissionRationale = false
                            // User chose "Continue anyway" - proceed without permission
                            appViewModel.pendingConnectionUri.value?.let { uri ->
                                requestedUri = uri
                                appViewModel.clearPendingConnectionUri()
                            }
                            // Also handle pending host connection
                            pendingHostConnection?.let { host ->
                                navController.navigate("${NavDestinations.CONSOLE}/${host.id}") {
                                    popUpTo(NavDestinations.HOST_LIST) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                                pendingHostConnection = null
                            }
                        },
                        onAllow = {
                            Timber.d("User chose to allow permission from rationale")
                            showPermissionRationale = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }

            // Callback to check permission before navigating to console
            val onNavigateToConsole: (Host) -> Unit = { host ->
                Timber.d("onNavigateToConsole called for host: ${host.nickname}")

                // Check if connection persistence is enabled
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val persistConnections = prefs.getBoolean(PreferenceConstants.CONNECTION_PERSIST, true)

                if (!persistConnections || NotificationPermissionHelper.isNotificationPermissionGranted(context)) {
                    // Either persistence is disabled (no permission needed) or permission granted, navigate immediately
                    // Pop any existing console screen so only one exists on the stack
                    navController.navigate("${NavDestinations.CONSOLE}/${host.id}") {
                        // Pop any existing console entry so we don't stack multiple console screens
                        popUpTo(NavDestinations.HOST_LIST) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                } else {
                    // Persistence is enabled but no permission - need to request permission
                    Timber.d("Requesting notification permission before connection")
                    pendingHostConnection = host
                    val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        this@MainActivity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        false
                    }
                    appViewModel.requestNotificationPermission(shouldShowRationale)
                }
            }

            SapienTermApp(
                appUiState = appUiState,
                navController = navController,
                makingShortcut = makingShortcut,
                authRequired = authOnLaunchEnabled,
                isAuthenticated = isAuthenticated,
                onAuthenticationSuccess = { appViewModel.onAuthenticationSuccess() },
                onRetryMigration = { appViewModel.retryMigration() },
                onSelectShortcut = { host, color, iconStyle ->
                    createShortcutAndFinish(host, color, iconStyle)
                },
                onNavigateToConsole = onNavigateToConsole
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val manager = (appViewModel.uiState.value as? AppUiState.Ready)?.terminalManager
            val bridge = manager?.visibleBridge
            if (bridge != null) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        bridge.sendPageUp()
                        return true
                    }

                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        bridge.sendPageDown()
                        return true
                    }
                }
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            val manager = (appViewModel.uiState.value as? AppUiState.Ready)?.terminalManager
            val bridge = manager?.visibleBridge
            if (bridge != null && (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleIntent(intent)

        intent.data?.let { uri ->
            requestedUri = uri
        }
    }

    override fun onStop() {
        super.onStop()
        if (::appViewModel.isInitialized) {
            appViewModel.onActivityStopped()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == DISCONNECT_ACTION) {
            Timber.d("handleIntent: DISCONNECT_ACTION, showing disconnect dialog")
            showDisconnectAllDialog = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        requestedUri?.let {
            outState.putString(STATE_SELECTED_URI, it.toString())
        }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    private fun handleConnectionUri(uri: Uri, controller: NavController) {
        Timber.d("handleConnectionUri: uri=$uri, fragment=${uri.fragment}")
        val state = appViewModel.uiState.value
        if (state !is AppUiState.Ready) {
            Timber.d("handleConnectionUri: state not ready, current state=$state")
            return
        }

        val manager = state.terminalManager

        lifecycleScope.launch {
            try {
                val nickname = uri.fragment ?: uri.authority
                Timber.d("handleConnectionUri: nickname=$nickname")
                var bridge = manager.getConnectedBridge(nickname)

                if (bridge == null) {
                    Timber.d("Creating new connection for URI: $uri with nickname: $nickname")
                    bridge = manager.openConnection(uri)
                }

                controller.navigate("${NavDestinations.CONSOLE}/${bridge.host.id}") {
                    popUpTo(NavDestinations.HOST_LIST) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling connection URI: $uri")
            }
        }
    }

    private fun createShortcutAndFinish(host: Host, color: String?, iconStyle: IconStyle) {
        val uri = host.getUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val icon = ShortcutIconGenerator.generateShortcutIcon(this, color, iconStyle)

        val shortcut = ShortcutInfoCompat.Builder(this, "host-${host.id}")
            .setShortLabel(host.nickname)
            .setLongLabel(host.nickname)
            .setIcon(icon)
            .setIntent(intent)
            .build()

        val result = ShortcutManagerCompat.createShortcutResultIntent(this, shortcut)
        setResult(RESULT_OK, result)
        finish()
    }
}

@Composable
private fun NotificationPermissionRationaleDialog(
    onDismiss: () -> Unit,
    onAllow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_title)) },
        text = { Text(stringResource(R.string.notification_permission_message)) },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.connect_anyway))
            }
        }
    )
}
