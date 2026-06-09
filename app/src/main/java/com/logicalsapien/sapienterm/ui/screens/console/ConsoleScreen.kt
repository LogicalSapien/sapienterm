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

package com.logicalsapien.sapienterm.ui.screens.console

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.preference.PreferenceManager
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.service.PromptRequest
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.ui.LoadingScreen
import com.logicalsapien.sapienterm.ui.LocalTerminalManager
import com.logicalsapien.sapienterm.ui.components.FloatingTextInputDialog
import com.logicalsapien.sapienterm.ui.components.InlinePrompt
import com.logicalsapien.sapienterm.ui.components.ResizeDialog
import com.logicalsapien.sapienterm.ui.components.UrlScanDialog
import com.logicalsapien.sapienterm.ui.theme.HostCategoryColor
import com.logicalsapien.sapienterm.ui.theme.SapienTheme
import com.logicalsapien.sapienterm.ui.theme.terminal
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.SessionKeyboardPolicy
import com.logicalsapien.sapienterm.util.clipboardImageBytes
import com.logicalsapien.sapienterm.util.encodeImageUriToJpegBytes
import com.logicalsapien.sapienterm.util.rememberTerminalTypefaceResultFromStoredValue
import com.logicalsapien.sapienterm.util.resolveSessionKeyboardPolicy
import kotlinx.coroutines.launch
import org.connectbot.terminal.ProgressState
import org.connectbot.terminal.SelectionController
import org.connectbot.terminal.Terminal
import timber.log.Timber

/**
 * Check if a hardware keyboard is currently attached to the device.
 * Detects QWERTY and 12-key hardware keyboards, including Bluetooth keyboards.
 */
@Composable
private fun rememberHasHardwareKeyboard(): Boolean {
    val configuration = LocalConfiguration.current

    return remember(configuration) {
        val keyboardType = configuration.keyboard
        keyboardType == android.content.res.Configuration.KEYBOARD_QWERTY ||
            keyboardType == android.content.res.Configuration.KEYBOARD_12KEY
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConsoleScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPortForwards: (Long) -> Unit,
    modifier: Modifier = Modifier,
    hostId: Long = -1L,
    viewModel: ConsoleViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val terminalManager = LocalTerminalManager.current
    val uiState by viewModel.uiState.collectAsState()
    val promptHostIds by viewModel.promptHostIds.collectAsState()

    // Capture latest callback for use in effects
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    LaunchedEffect(terminalManager) {
        terminalManager?.let { viewModel.setTerminalManager(it) }
    }

    // When hostId changes (e.g., navigating to a new host with launchSingleTop),
    // tell the ViewModel to switch to (or create) the new bridge
    LaunchedEffect(hostId) {
        if (hostId != -1L) {
            viewModel.navigateToHost(hostId)
        }
    }

    // Read preferences
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var fullscreen by remember { mutableStateOf(prefs.getBoolean("fullscreen", false)) }

    // Apply terminal color theme based on app theme mode
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    var themeMode by remember {
        mutableStateOf(prefs.getString("theme_mode", "auto") ?: "auto")
    }
    var bottomBarPrefStr by remember {
        mutableStateOf(
            prefs.getString(PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET, "default") ?: "default"
        )
    }
    var sessionKeyboardPrefsEpoch by remember { mutableIntStateOf(0) }
    var customBottomBarLayoutsEpoch by remember { mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            when (key) {
                "theme_mode" -> {
                    themeMode = sp.getString("theme_mode", "auto") ?: "auto"
                }

                PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET -> {
                    bottomBarPrefStr = sp.getString(PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET, "default")
                        ?: "default"
                }

                PreferenceConstants.TERMINAL_SESSION_KEYBOARD,
                PreferenceConstants.TERMINAL_KEYBOARD_BAR_DEFAULT,
                PreferenceConstants.TERMINAL_KEYBOARD_BAR_ZELLIJ,
                PreferenceConstants.TERMINAL_KEYBOARD_BAR_TMUX -> {
                    sessionKeyboardPrefsEpoch++
                }

                PreferenceConstants.CUSTOM_BOTTOM_BAR_LAYOUTS -> {
                    customBottomBarLayoutsEpoch++
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val isLightTheme = when (themeMode) {
        "light" -> true
        "dark" -> false
        else -> !systemDark
    }
    LaunchedEffect(isLightTheme, terminalManager) {
        terminalManager?.applyThemeToAllBridges(isLightTheme)
    }

    // Keyboard state
    val hasHardwareKeyboard = rememberHasHardwareKeyboard()
    var showSoftwareKeyboard by remember { mutableStateOf(!hasHardwareKeyboard) }

    // Per-connection keyboard lock: when locked off, the keyboard won't auto-appear on
    // terminal taps or session connect. The user can still type via the floating text dialog.
    // Persisted per host ID in SharedPreferences so it survives app restarts and navigation.
    val keyboardLockedSessions = remember { mutableStateMapOf<Long, Boolean>() }
    val currentHostId = uiState.bridges.getOrNull(uiState.currentBridgeIndex)?.host?.id

    // Load lock state synchronously from prefs on first access per host (no LaunchedEffect race)
    val keyboardLockedOff = currentHostId?.let { id ->
        keyboardLockedSessions.getOrPut(id) {
            prefs.getBoolean("keyboard_disabled_$id", false)
        }
    } == true

    val termFocusRequester = remember { FocusRequester() }

    var forceSize: Pair<Int, Int>? by remember { mutableStateOf(null) }

    var showMenu by remember { mutableStateOf(false) }
    var showUrlScanDialog by remember { mutableStateOf(false) }
    var showResizeDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf(false) }
    var showRenameSessionDialog by remember { mutableStateOf(false) }
    var scannedUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectionController by remember { mutableStateOf<SelectionController?>(null) }
    var imeVisible by remember { mutableStateOf(false) }

    // Tab close confirmation state
    var bridgeToClose by remember { mutableStateOf<TerminalBridge?>(null) }
    val imageSendScope = rememberCoroutineScope()

    fun sendImageToTerminal(bytes: ByteArray) {
        val bridge = uiState.bridges.getOrNull(uiState.currentBridgeIndex)
        if (bridge == null || !bridge.canWriteToRemote()) {
            Toast.makeText(context, context.getString(R.string.terminal_send_not_ready), Toast.LENGTH_LONG).show()
            return
        }
        imageSendScope.launch {
            try {
                val filename = "sapienterm-${System.currentTimeMillis()}.jpg"
                val remotePath = bridge.uploadTerminalAttachment(bytes, filename)
                if (remotePath == null) {
                    Toast.makeText(context, "Image upload is only available for SSH sessions", Toast.LENGTH_LONG).show()
                    return@launch
                }
                bridge.sendBracketedPaste(remotePath, submitWithNewline = false)
                val sizeKb = bytes.size / 1024
                Toast.makeText(context, "Image uploaded; path pasted ($sizeKb KB)", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Timber.e(e, "Failed to upload selected image")
                Toast.makeText(context, "Image upload failed: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Image picker launcher — uploads selected image over SSH and pastes its remote path.
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = encodeImageUriToJpegBytes(context, uri)
            if (bytes != null) {
                sendImageToTerminal(bytes)
            } else {
                Toast.makeText(context, "Failed to encode image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Apply fullscreen mode and display cutout settings
    LaunchedEffect(fullscreen) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window

        try {
            if (fullscreen) {
                // Enable fullscreen mode - hide system bars
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                // Disable fullscreen mode - show system bars
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        } catch (e: IllegalArgumentException) {
            // Handle foldable device state issues
            Timber.e(e, "Error setting fullscreen mode (foldable device?)")
        }
    }

    // Navigate back if all bridges are closed (after initial loading)
    LaunchedEffect(uiState.bridges.size, uiState.isLoading) {
        if (uiState.bridges.isEmpty() && !uiState.isLoading) {
            currentOnNavigateBack()
        }
    }

    // Request focus on terminal when screen appears (e.g., returning from navigation)
    LaunchedEffect(Unit) {
        termFocusRequester.requestFocus()
    }

    // Track actual IME visibility using WindowInsets to detect user dismissing with back button
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val imeHeight = with(density) { imeInsets.getBottom(density).toDp() }
    val systemImeVisible = imeHeight > 0.dp
    var hasImeBeenVisible by remember { mutableStateOf(false) }

    // Sync our state when user dismisses IME externally (back button),
    // and actively suppress the IME whenever it appears while keyboard is locked off.
    LaunchedEffect(systemImeVisible, keyboardLockedOff) {
        if (systemImeVisible && keyboardLockedOff) {
            showSoftwareKeyboard = false
            (context as? Activity)?.let { activity ->
                val controller = WindowInsetsControllerCompat(
                    activity.window,
                    activity.window.decorView
                )
                controller.hide(WindowInsetsCompat.Type.ime())
            }
            imeVisible = false
            return@LaunchedEffect
        }
        if (systemImeVisible) {
            hasImeBeenVisible = true
        }
        if (hasImeBeenVisible && !systemImeVisible && showSoftwareKeyboard) {
            kotlinx.coroutines.delay(150)
            if (!systemImeVisible && showSoftwareKeyboard) {
                showSoftwareKeyboard = false
            }
        }
        imeVisible = systemImeVisible
    }

    // Get current prompt state to check if biometric prompt is active
    val currentBridgeForPrompt = uiState.bridges.getOrNull(uiState.currentBridgeIndex)
    val promptState by currentBridgeForPrompt?.promptManager?.promptState?.collectAsState()
        ?: remember { mutableStateOf(null) }
    var wasBiometricPromptActive by remember { mutableStateOf(false) }
    val isBiometricPromptActive = promptState is PromptRequest.BiometricPrompt

    val currentBridge = uiState.bridges.getOrNull(uiState.currentBridgeIndex)
    val (stripBuiltinPreset, customLayoutId) = remember(
        currentBridge?.host?.bottomBarPresetOverride,
        bottomBarPrefStr,
        uiState.revision,
        customBottomBarLayoutsEpoch
    ) {
        TerminalBottomBarPreset.resolvedSelection(
            currentBridge?.host?.bottomBarPresetOverride,
            bottomBarPrefStr
        )
    }
    val customBottomStrip = remember(customLayoutId, uiState.revision, customBottomBarLayoutsEpoch) {
        viewModel.customBottomBarActionsFor(customLayoutId)
    }
    val sessionKeyboardPolicy = remember(
        currentBridge?.host?.id,
        currentBridge?.host?.sessionKeyboardOverride,
        sessionKeyboardPrefsEpoch
    ) {
        val host = currentBridge?.host
        if (host != null) {
            resolveSessionKeyboardPolicy(host, prefs)
        } else {
            SessionKeyboardPolicy.AUTO
        }
    }

    // Show software keyboard after biometric prompt completes
    LaunchedEffect(isBiometricPromptActive, sessionKeyboardPolicy, hasHardwareKeyboard, keyboardLockedOff) {
        if (wasBiometricPromptActive && !isBiometricPromptActive && !keyboardLockedOff) {
            showSoftwareKeyboard = sessionKeyboardPolicy.shouldShowSoftwareKeyboard(hasHardwareKeyboard)
        }
        wasBiometricPromptActive = isBiometricPromptActive
    }

    // These values are computed from bridge state and will recompute when uiState.revision changes
    val sessionOpen = currentBridge?.isSessionOpen == true
    val disconnected = currentBridge?.isDisconnected == true
    val canForwardPorts = currentBridge?.canFowardPorts() == true
    val snackbarHostState = remember { SnackbarHostState() }

    // Software keyboard when session becomes open (policy: auto / on / off per bar preset and host).
    // Delay so the keyboard opens well within the 2-second resize-settling window.
    LaunchedEffect(currentBridge, sessionOpen, hasHardwareKeyboard, keyboardLockedOff, sessionKeyboardPolicy) {
        if (sessionOpen && !keyboardLockedOff) {
            kotlinx.coroutines.delay(800)
            showSoftwareKeyboard = sessionKeyboardPolicy.shouldShowSoftwareKeyboard(hasHardwareKeyboard)
        }
    }

    // Force keyboard hidden whenever lock is active
    LaunchedEffect(keyboardLockedOff) {
        if (keyboardLockedOff) {
            showSoftwareKeyboard = false
        }
    }

    // Reset selection controller when bridge changes
    LaunchedEffect(currentBridge) {
        selectionController = null
        currentBridge?.keyHandler?.selectionController = null
    }

    // Compute forceSize synchronously so the Terminal composable gets the
    // correct size from the very first frame — no async LaunchedEffect needed.
    forceSize = remember(currentBridge) {
        currentBridge?.let { bridge ->
            val hostCols = bridge.host.fixedCols
            val hostRows = bridge.host.fixedRows
            if (hostCols != null && hostRows != null && hostCols > 0 && hostRows > 0) {
                Pair(hostRows, hostCols)
            } else {
                val rows = bridge.profileForceSizeRows
                val cols = bridge.profileForceSizeColumns
                if (rows != null && cols != null) Pair(rows, cols) else null
            }
        }
    }

    // Show snackbar for network status messages
    LaunchedEffect(Unit) {
        viewModel.networkStatusMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Show snackbar when there's an error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true
            )
        }
    }

    fun handleTerminalInteraction() {
        // Bring up the soft keyboard when the terminal area is tapped
        // (unless a hardware keyboard is connected or keyboard is locked off for this session)
        if (!hasHardwareKeyboard && !keyboardLockedOff) {
            if (!showSoftwareKeyboard) {
                hasImeBeenVisible = false
            }
            showSoftwareKeyboard = true
            termFocusRequester.requestFocus()
        }
    }

    // Only request terminal focus when keyboard is not locked — requesting focus on the
    // ImeInputView can flash the keyboard even when showSoftKeyboard=false.
    fun safeRequestTerminalFocus() {
        if (!keyboardLockedOff) termFocusRequester.requestFocus()
    }

    // When keyboard is locked off, ignore IME insets entirely so a brief
    // keyboard flash on app resume doesn't resize the terminal and leave
    // half the screen empty.
    val imeInsetForLayout = if (keyboardLockedOff) WindowInsets(0) else WindowInsets.imeAnimationTarget

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(imeInsetForLayout)
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .windowInsetsPadding(imeInsetForLayout)
        ) {
            // Session tab bar with integrated back + menu buttons — the only top element
            if (uiState.bridges.isNotEmpty() && !uiState.isLoading) {
                Box {
                    SessionTabBar(
                        bridges = uiState.bridges,
                        currentIndex = uiState.currentBridgeIndex,
                        onSelectTab = { viewModel.selectBridge(it) },
                        onCloseTab = { bridge ->
                            if (!bridge.isSessionOpen || bridge.isDisconnected) {
                                viewModel.disconnectBridge(bridge)
                            } else {
                                bridgeToClose = bridge
                            }
                        },
                        onRenameTab = { index, newName -> viewModel.renameTab(index, newName) },
                        onNavigateBack = onNavigateBack,
                        onMenuClick = {
                            viewModel.refreshMenuState()
                            showMenu = true
                        },
                        promptHostIds = promptHostIds
                    )

                    // Overflow menu anchored to the tab bar
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            safeRequestTerminalFocus()
                        }
                    ) {
                        // Paste text
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.console_menu_paste)) },
                            onClick = {
                                showMenu = false
                                currentBridge?.let { bridge ->
                                    val clipboard =
                                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip =
                                        clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    bridge.injectString(clip)
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                            },
                            enabled = currentBridge != null
                        )

                        // Paste image from clipboard by uploading it and inserting the remote path.
                        DropdownMenuItem(
                            text = { Text("Paste Image") },
                            onClick = {
                                showMenu = false
                                val bytes = clipboardImageBytes(context)
                                if (bytes != null) {
                                    sendImageToTerminal(bytes)
                                } else {
                                    Toast.makeText(context, "No image on clipboard", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentPaste, contentDescription = null)
                            },
                            enabled = currentBridge != null
                        )

                        // Select image from gallery and send it into the remote terminal.
                        DropdownMenuItem(
                            text = { Text("Select Image") },
                            onClick = {
                                showMenu = false
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            },
                            enabled = currentBridge != null
                        )

                        // Reconnect (shown only when disconnected)
                        if (disconnected) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.console_menu_reconnect)) },
	                                onClick = {
	                                    showMenu = false
	                                    viewModel.reconnect(currentBridge)
	                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )
                        }

                        // Disconnect/Close
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (!sessionOpen && disconnected) {
                                        stringResource(R.string.console_menu_close)
                                    } else {
                                        stringResource(R.string.list_host_disconnect)
                                    }
                                )
                            },
                            onClick = {
                                showMenu = false
                                showDisconnectDialog = true
                            },
                            enabled = currentBridge != null,
                            leadingIcon = {
                                Icon(Icons.Default.LinkOff, null)
                            }
                        )

                        // Rename session
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.console_menu_rename_session)) },
                            onClick = {
                                showMenu = false
                                showRenameSessionDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            },
                            enabled = currentBridge != null
                        )

                        // URL Scan
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.console_menu_urlscan)) },
                            onClick = {
                                showMenu = false
                                currentBridge?.let { bridge ->
                                    scannedUrls = bridge.scanForURLs()
                                    showUrlScanDialog = true
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            },
                            enabled = currentBridge != null
                        )

                        // Resize
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.console_menu_resize)) },
                            onClick = {
                                showMenu = false
                                showResizeDialog = true
                            },
                            enabled = sessionOpen
                        )

                        // Port Forwards (if available)
                        if (canForwardPorts) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.console_menu_portforwards)) },
                                onClick = {
                                    showMenu = false
                                    currentBridge.host.id.let {
                                        onNavigateToPortForwards(it)
                                    }
                                },
                                enabled = sessionOpen
                            )
                        }

                        // Fullscreen toggle
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pref_fullscreen_title)) },
                            onClick = {
                                fullscreen = !fullscreen
                                prefs.edit { putBoolean("fullscreen", fullscreen) }
                            },
                            trailingIcon = {
                                Checkbox(
                                    checked = fullscreen,
                                    onCheckedChange = null
                                )
                            }
                        )
                    }
                }

                // Progress indicator for OSC 9;4 progress reporting
                val progressState = uiState.progressState
                if (progressState != null && progressState != ProgressState.HIDDEN) {
                    val progressColor = when (progressState) {
                        ProgressState.ERROR -> MaterialTheme.colorScheme.error
                        ProgressState.WARNING -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }

                    if (progressState == ProgressState.INDETERMINATE) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = progressColor
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { uiState.progressValue / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = progressColor
                        )
                    }
                }

                // Ambient glow — thin host-colored gradient between the tab bar and terminal
                val glowHost = uiState.bridges.getOrNull(uiState.currentBridgeIndex)?.host
                val glowColor = if (glowHost != null) {
                    SapienTheme.tokens.colorFor(HostCategoryColor.fromStorageString(glowHost.color))
                } else {
                    SapienTheme.tokens.statusIdle
                }
                AmbientGlow(color = glowColor)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when {
                                keyEvent.key == Key.C && keyEvent.isCtrlPressed && keyEvent.isShiftPressed -> {
                                    selectionController?.copySelection()
                                    true
                                }

                                keyEvent.key == Key.V && keyEvent.isCtrlPressed && keyEvent.isShiftPressed -> {
                                    currentBridge?.let { bridge ->
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip =
                                            clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                        bridge.injectString(clip)
                                    }
                                    true
                                }

                                keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.Equals -> {
                                    currentBridge?.increaseFontSize()
                                    true
                                }

                                keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.Minus -> {
                                    currentBridge?.decreaseFontSize()
                                    true
                                }

                                keyEvent.key == Key.VolumeUp -> {
                                    currentBridge?.sendAltScrollUp()
                                    true
                                }

                                keyEvent.key == Key.VolumeDown -> {
                                    currentBridge?.sendAltScrollDown()
                                    true
                                }

                                else -> false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingScreen(modifier = Modifier.fillMaxSize())
                    }

                    uiState.bridges.isNotEmpty() -> {
                        // Use key() to force full recreation of the Terminal composable
                        // when switching tabs. Without this, Compose may reuse the
                        // existing Terminal instance which keeps the old bridge's
                        // terminalEmulator and keyHandler bound, so keyboard input
                        // goes to the wrong session.
                        key(uiState.currentBridgeIndex) {
                            val bridge = uiState.bridges[uiState.currentBridgeIndex]

                            // Request focus on the terminal after tab switch
                            // so keyboard input goes to the newly active bridge
                            LaunchedEffect(Unit) {
                                safeRequestTerminalFocus()
                            }

                            // Terminal view fills entire space
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                // Get font from profile (stored in bridge)
                                val fontResult = rememberTerminalTypefaceResultFromStoredValue(bridge.fontFamily)
                                val coroutineScope = rememberCoroutineScope()
                                // Observe font size changes for reactive updates
                                val fontSize by bridge.fontSizeFlow.collectAsState()
                                // Observe terminal colors so the Canvas background matches the colour scheme
                                val termColors by bridge.terminalColorsFlow.collectAsState()

                                // Show snackbar if font loading failed
                                LaunchedEffect(fontResult.loadFailed, fontResult.isLoading) {
                                    if (fontResult.loadFailed && !fontResult.isLoading) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "Failed to load font '${fontResult.requestedFontName}'. Using system default."
                                            )
                                        }
                                    }
                                }

                                Terminal(
                                    terminalEmulator = bridge.terminalEmulator,
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    typeface = fontResult.typeface,
                                    initialFontSize = fontSize.sp,
                                    backgroundColor = termColors.background,
                                    foregroundColor = termColors.foreground,
                                    // Disable keyboard while the floating input is open so the dialog's
                                    // TextField gets the IME input connection cleanly.
                                    keyboardEnabled = !showTextInputDialog,
                                    showSoftKeyboard = showSoftwareKeyboard && !keyboardLockedOff,
                                    focusRequester = termFocusRequester,
                                    forcedSize = forceSize,
                                    modifierManager = bridge.keyHandler,
                                    shouldMaskLineAsImage = { lineText ->
                                        bridge.shouldMaskFloatingImageEcho(lineText)
                                    },
                                    preferHostMouseWheel = true,
                                    onSelectionControllerAvailable = { ctrl ->
                                        selectionController = ctrl
                                        bridge.keyHandler.selectionController = ctrl
                                    },
                                    onTerminalTap = {
                                        bridge.exitAltScreenScrollMode()
                                        handleTerminalInteraction()
                                    },
                                    onImeVisibilityChanged = { visible ->
                                        imeVisible = visible
                                    },
                                    onHyperlinkClick = { url ->
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            url.toUri()
                                        )
                                        context.startActivity(intent)
                                    },
                                    onScrollFallback = { lines ->
                                        repeat(kotlin.math.abs(lines)) {
                                            if (lines > 0) bridge.sendAltScrollUp()
                                            else bridge.sendAltScrollDown()
                                        }
                                    },
                                    onBeforeKeyInput = {
                                        bridge.exitAltScreenScrollMode()
                                    },
                                    onAltScrollJumpToBottom = {
                                        bridge.jumpAltScrollToBottom()
                                    }
                                )

                                // Set up text input request callback from bridge (for camera button)
                                SideEffect {
                                    bridge.onTextInputRequested = {
                                        showTextInputDialog = true
                                    }
                                }

                                // Show inline prompts from the current bridge (non-modal at bottom)
                                val promptState by bridge.promptManager.promptState.collectAsState()

                                InlinePrompt(
                                    promptRequest = promptState,
                                    onResponse = { response ->
                                        bridge.promptManager.respond(response)
                                    },
                                    onCancel = {
                                        bridge.promptManager.cancelPrompt()
                                    },
                                    onDismiss = {
                                        safeRequestTerminalFocus()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                )

                                // Show reconnect/close overlay when session is disconnected
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = disconnected,
                                    enter = slideInVertically(initialOffsetY = { it }),
                                    exit = slideOutVertically(targetOffsetY = { it }),
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    val terminalColors = MaterialTheme.colorScheme.terminal
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(terminalColors.overlayBackground)
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.alert_disconnect_msg),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = terminalColors.overlayText,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { showDisconnectDialog = true }) {
                                                Text(
                                                    stringResource(R.string.console_menu_close),
                                                    color = terminalColors.overlayText
                                                )
                                            }
                                            Button(
                                                onClick = { viewModel.reconnect(bridge) },
                                                modifier = Modifier.padding(start = 8.dp)
                                            ) {
                                                Text(stringResource(R.string.console_menu_reconnect))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // end Box

            // CLI prompt action bar (shown when interactive prompts are detected)
            if (currentBridge != null) {
                val detectedPrompt by viewModel.detectedPrompt.collectAsState()
                CliPromptBar(
                    detectedPrompt = detectedPrompt,
                    onOptionSelected = { option ->
                        viewModel.sendPromptResponse(option.sendValue)
                        handleTerminalInteraction()
                    },
                    onDismiss = { viewModel.dismissPrompt() }
                )
            }

            // Unified terminal bottom bar (Termius-style) - replaces the
            // separate QuickCommandToolbar, ExtendedKeyboardStrip, and keyboard toggle
            if (currentBridge != null) {
                val quickCommands by viewModel.quickCommands.collectAsState()
                val commandHistory by viewModel.commandHistory.collectAsState()
                val promptTemplates by viewModel.promptTemplates.collectAsState()

                TerminalBottomBar(
                    bridge = currentBridge,
                    quickCommands = quickCommands,
                    commandHistory = commandHistory,
                    showSoftwareKeyboard = systemImeVisible,
                    keyboardLockedOff = keyboardLockedOff,
                    onToggleKeyboard = {
                        if (!systemImeVisible) {
                            hasImeBeenVisible = false
                            showSoftwareKeyboard = true
                            safeRequestTerminalFocus()
                        } else {
                            showSoftwareKeyboard = false
                        }
                    },
                    onToggleKeyboardLock = {
                        currentHostId?.let { id ->
                            val newState = !keyboardLockedOff
                            keyboardLockedSessions[id] = newState
                            prefs.edit().putBoolean("keyboard_disabled_$id", newState).apply()
                        }
                    },
                    onVoiceInput = {
                        showTextInputDialog = true
                    },
                    onSendQuickCommand = { command ->
                        viewModel.sendQuickCommand(command)
                        handleTerminalInteraction()
                    },
                    onHistoryCommandClick = { command ->
                        viewModel.sendQuickCommand(command)
                        handleTerminalInteraction()
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    onInteraction = { handleTerminalInteraction() },
                    bottomBarStripBuiltin = stripBuiltinPreset,
                    customShortcutStrip = customBottomStrip
                )
            }
        } // end Column

        // Dialogs
        if (showUrlScanDialog) {
            UrlScanDialog(
                urls = scannedUrls,
                onDismiss = { showUrlScanDialog = false },
                onUrlClick = { url ->
                    // Open URL in browser
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        url.toUri()
                    )
                    context.startActivity(intent)
                }
            )
        }

        if (showResizeDialog && currentBridge != null) {
            ResizeDialog(
                currentBridge = currentBridge,
                isForced = forceSize != null,
                onDismiss = { showResizeDialog = false },
                onResize = { width, height ->
                    // Resize the terminal emulator
                    forceSize = Pair(height, width)
                },
                onDisableForceSize = {
                    // Disable force size for this session
                    forceSize = null
                }
            )
        }

        if (showDisconnectDialog && currentBridge != null) {
            HostDisconnectDialog(
                host = currentBridge.host,
                onDismiss = { showDisconnectDialog = false },
                onConfirm = {
                    showDisconnectDialog = false
                    currentBridge.dispatchDisconnect(true)
                }
            )
        }

        // Tab close confirmation dialog
        bridgeToClose?.let { bridge ->
            TabCloseConfirmDialog(
                bridgeName = getTabDisplayName(bridge),
                onDismiss = { bridgeToClose = null },
                onConfirm = {
                    bridgeToClose = null
                    viewModel.disconnectBridge(bridge)
                }
            )
        }

        if (showTextInputDialog && currentBridge != null) {
            FloatingTextInputDialog(
                bridge = currentBridge,
                initialText = "",
                onDismiss = {
                    showTextInputDialog = false
                    safeRequestTerminalFocus()
                }
            )
        }

        if (showRenameSessionDialog && currentBridge != null) {
            val currentName = getTabDisplayName(currentBridge)
            RenameSessionDialog(
                currentName = currentName,
                onDismiss = {
                    showRenameSessionDialog = false
                    safeRequestTerminalFocus()
                },
                onConfirm = { newName ->
                    viewModel.renameTab(uiState.currentBridgeIndex, newName)
                    showRenameSessionDialog = false
                    safeRequestTerminalFocus()
                }
            )
        }
    }
}

@Composable
private fun HostDisconnectDialog(
    host: Host,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(stringResource(R.string.disconnect_host_alert, host.nickname))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.button_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_no))
            }
        }
    )
}

@Composable
private fun TabCloseConfirmDialog(
    bridgeName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(stringResource(R.string.disconnect_tab_confirm, bridgeName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.button_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_no))
            }
        }
    )
}

@Composable
private fun RenameSessionDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.rename_tab_title))
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.rename_tab_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}
