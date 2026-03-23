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

package com.logicalsapien.sapienssh.ui.screens.console

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.data.entity.Host
import com.logicalsapien.sapienssh.service.PromptRequest
import com.logicalsapien.sapienssh.service.TerminalBridge
import org.connectbot.terminal.ProgressState
import org.connectbot.terminal.SelectionController
import org.connectbot.terminal.Terminal
import com.logicalsapien.sapienssh.ui.LoadingScreen
import com.logicalsapien.sapienssh.ui.LocalTerminalManager
import com.logicalsapien.sapienssh.ui.components.FloatingTextInputDialog
import com.logicalsapien.sapienssh.ui.components.InlinePrompt
import com.logicalsapien.sapienssh.ui.components.ResizeDialog
import com.logicalsapien.sapienssh.ui.components.UrlScanDialog
import com.logicalsapien.sapienssh.ui.theme.terminal
import com.logicalsapien.sapienssh.util.PreferenceConstants
import com.logicalsapien.sapienssh.util.rememberTerminalTypefaceResultFromStoredValue
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
    val volumeKeysChangeFontSize = remember { prefs.getBoolean(PreferenceConstants.VOLUME_FONT, true) }

    // Keyboard state
    val hasHardwareKeyboard = rememberHasHardwareKeyboard()
    var showSoftwareKeyboard by remember { mutableStateOf(!hasHardwareKeyboard) }

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

    // Sync our state when user dismisses IME externally (back button)
    LaunchedEffect(systemImeVisible) {
        if (systemImeVisible) {
            hasImeBeenVisible = true
        }
        // Only sync to hidden state after IME has been visible at least once.
        // This prevents canceling the keyboard before it has a chance to show.
        if (hasImeBeenVisible && !systemImeVisible && showSoftwareKeyboard) {
            showSoftwareKeyboard = false
        }
        imeVisible = systemImeVisible
    }

    // Get current prompt state to check if biometric prompt is active
    val currentBridgeForPrompt = uiState.bridges.getOrNull(uiState.currentBridgeIndex)
    val promptState by currentBridgeForPrompt?.promptManager?.promptState?.collectAsState()
        ?: remember { mutableStateOf(null) }
    var wasBiometricPromptActive by remember { mutableStateOf(false) }
    val isBiometricPromptActive = promptState is PromptRequest.BiometricPrompt

    // Show software keyboard after biometric prompt completes (unless hardware keyboard is connected)
    LaunchedEffect(isBiometricPromptActive) {
        if (wasBiometricPromptActive && !isBiometricPromptActive && !hasHardwareKeyboard) {
            showSoftwareKeyboard = true
        }
        wasBiometricPromptActive = isBiometricPromptActive
    }

    val currentBridge = uiState.bridges.getOrNull(uiState.currentBridgeIndex)
    // These values are computed from bridge state and will recompute when uiState.revision changes
    val sessionOpen = currentBridge?.isSessionOpen == true
    val disconnected = currentBridge?.isDisconnected == true
    val canForwardPorts = currentBridge?.canFowardPorts() == true
    val snackbarHostState = remember { SnackbarHostState() }

    // Show software keyboard when session becomes open (if no hardware keyboard)
    // Also show when switching to a different bridge that's already open
    LaunchedEffect(currentBridge, sessionOpen, hasHardwareKeyboard) {
        if (sessionOpen && !hasHardwareKeyboard) {
            showSoftwareKeyboard = true
        }
    }

    // Reset selection controller when bridge changes
    LaunchedEffect(currentBridge) {
        selectionController = null
    }

    // Initialize forceSize from profile when bridge changes
    LaunchedEffect(currentBridge) {
        currentBridge?.let { bridge ->
            val rows = bridge.profileForceSizeRows
            val cols = bridge.profileForceSizeColumns
            if (rows != null && cols != null) {
                forceSize = Pair(rows, cols)
            } else {
                forceSize = null
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
        // (unless a hardware keyboard is connected)
        if (!hasHardwareKeyboard) {
            showSoftwareKeyboard = true
            termFocusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .union(WindowInsets.imeAnimationTarget)
    ) { innerPadding ->
        // Terminal content with keyboard overlay
        // This Box is transparent to accessibility - it's just for layout
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
                .windowInsetsPadding(WindowInsets.imeAnimationTarget)
        ) {
            // Session tab bar with integrated back + menu buttons — the only top element
            if (uiState.bridges.isNotEmpty() && !uiState.isLoading) {
                Box {
                    SessionTabBar(
                        bridges = uiState.bridges,
                        currentIndex = uiState.currentBridgeIndex,
                        onSelectTab = { viewModel.selectBridge(it) },
                        onCloseTabRequested = { bridge -> bridgeToClose = bridge },
                        onRenameTab = { index, newName -> viewModel.renameTab(index, newName) },
                        onNavigateBack = onNavigateBack,
                        onMenuClick = {
                            viewModel.refreshMenuState()
                            showMenu = true
                        }
                    )

                    // Overflow menu anchored to the tab bar
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            termFocusRequester.requestFocus()
                        }
                    ) {
                        // Paste
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

                        // Reconnect (shown only when disconnected)
                        if (disconnected) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.console_menu_reconnect)) },
                                onClick = {
                                    showMenu = false
                                    currentBridge?.let { viewModel.reconnect(it) }
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
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when {
                            // Ctrl+Shift+C: copy selection
                            keyEvent.key == Key.C && keyEvent.isCtrlPressed && keyEvent.isShiftPressed -> {
                                selectionController?.copySelection()
                                true
                            }

                            // Ctrl+Shift+V: paste clipboard content
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

                            // Ctrl+Shift+= (Ctrl++): increase font size
                            keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.Equals -> {
                                currentBridge?.increaseFontSize()
                                true
                            }

                            // Ctrl+Shift+-: decrease font size
                            keyEvent.isCtrlPressed && keyEvent.isShiftPressed && keyEvent.key == Key.Minus -> {
                                currentBridge?.decreaseFontSize()
                                true
                            }

                            // Volume keys: change font size
                            volumeKeysChangeFontSize && keyEvent.key == Key.VolumeUp -> {
                                currentBridge?.increaseFontSize()
                                true
                            }

                            volumeKeysChangeFontSize && keyEvent.key == Key.VolumeDown -> {
                                currentBridge?.decreaseFontSize()
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
                            termFocusRequester.requestFocus()
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
                                keyboardEnabled = true,
                                showSoftKeyboard = showSoftwareKeyboard,
                                focusRequester = termFocusRequester,
                                forcedSize = forceSize,
                                modifierManager = bridge.keyHandler,
                                onSelectionControllerAvailable = { selectionController = it },
                                onTerminalTap = { handleTerminalInteraction() },
                                onImeVisibilityChanged = { visible ->
                                    imeVisible = visible
                                },
                                onHyperlinkClick = { url ->
                                    // Open OSC8 hyperlink in browser
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        url.toUri()
                                    )
                                    context.startActivity(intent)
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
                                    termFocusRequester.requestFocus()
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

                TerminalBottomBar(
                    bridge = currentBridge,
                    quickCommands = quickCommands,
                    commandHistory = commandHistory,
                    showSoftwareKeyboard = showSoftwareKeyboard,
                    onToggleKeyboard = {
                        showSoftwareKeyboard = !showSoftwareKeyboard
                        if (showSoftwareKeyboard) {
                            termFocusRequester.requestFocus()
                        }
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
                    onInteraction = { handleTerminalInteraction() }
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
            // TODO: Get selected text from TerminalEmulator when selection is implemented
            val selectedText = ""

            FloatingTextInputDialog(
                bridge = currentBridge,
                initialText = selectedText,
                onDismiss = {
                    showTextInputDialog = false
                    termFocusRequester.requestFocus()
                }
            )
        }

        if (showRenameSessionDialog && currentBridge != null) {
            val currentName = getTabDisplayName(currentBridge)
            RenameSessionDialog(
                currentName = currentName,
                onDismiss = {
                    showRenameSessionDialog = false
                    termFocusRequester.requestFocus()
                },
                onConfirm = { newName ->
                    viewModel.renameTab(uiState.currentBridgeIndex, newName)
                    showRenameSessionDialog = false
                    termFocusRequester.requestFocus()
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

/**
 * Confirmation dialog shown when the user taps the X button on a session tab.
 */
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
