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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicalsapien.sapienterm.data.CommandHistoryRepository
import com.logicalsapien.sapienterm.data.CustomBottomBarLayoutStore
import com.logicalsapien.sapienterm.data.PromptTemplateRepository
import com.logicalsapien.sapienterm.data.QuickCommandRepository
import com.logicalsapien.sapienterm.data.entity.CommandHistory
import com.logicalsapien.sapienterm.data.entity.PromptTemplate
import com.logicalsapien.sapienterm.data.entity.QuickCommand
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import com.logicalsapien.sapienterm.service.ServiceError
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.service.TerminalManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.logicalsapien.sapienterm.util.CliPromptDetector
import com.logicalsapien.sapienterm.util.DetectedPrompt
import com.logicalsapien.sapienterm.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.connectbot.terminal.ProgressState
import javax.inject.Inject

data class ConsoleUiState(
    val bridges: List<TerminalBridge> = emptyList(),
    val currentBridgeIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Add a revision counter to force recomposition when bridge state changes
    val revision: Int = 0,
    // Progress state from OSC 9;4 escape sequences
    val progressState: ProgressState? = null,
    val progressValue: Int = 0
)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val quickCommandRepository: QuickCommandRepository,
    private val commandHistoryRepository: CommandHistoryRepository,
    private val customBottomBarLayoutStore: CustomBottomBarLayoutStore,
    private val promptTemplateRepository: PromptTemplateRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val promptTemplates: StateFlow<List<PromptTemplate>> = promptTemplateRepository
        .observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { promptTemplateRepository.seedBuiltins() }
    }

    /** Resolved shortcut actions for a stored custom layout id, or null if missing / empty. */
    fun customBottomBarActionsFor(layoutId: String?): List<BottomBarShortcutAction>? {
        if (layoutId.isNullOrBlank()) return null
        val actions = customBottomBarLayoutStore.getById(layoutId)?.actions.orEmpty()
        return actions.takeIf { it.isNotEmpty() }
    }
    private var hostId: Long = savedStateHandle.get<Long>("hostId") ?: -1L
    private var terminalManager: TerminalManager? = null

    // Track whether we've already set the initial bridge for this hostId
    private var initialBridgeSelected = false

    private val _uiState = MutableStateFlow(ConsoleUiState())
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    private val _networkStatusMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val networkStatusMessages: SharedFlow<String> = _networkStatusMessages.asSharedFlow()

    /** All quick commands, observed from the database. */
    val quickCommands: StateFlow<List<QuickCommand>> = quickCommandRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isQuickCommandToolbarVisible = MutableStateFlow(true)

    /** Whether the quick command chips are visible (toggle state). */
    val isQuickCommandToolbarVisible: StateFlow<Boolean> = _isQuickCommandToolbarVisible.asStateFlow()

    /** Detected CLI prompt for the current bridge (null when none detected). */
    private val _detectedPrompt = MutableStateFlow<DetectedPrompt?>(null)
    val detectedPrompt: StateFlow<DetectedPrompt?> = _detectedPrompt.asStateFlow()

    /**
     * Set of host IDs that currently have a detected prompt.
     * Sourced from [TerminalManager.bridgePrompts] -- used for tab badges.
     */
    private val _promptHostIds = MutableStateFlow<Set<Long>>(emptySet())
    val promptHostIds: StateFlow<Set<Long>> = _promptHostIds.asStateFlow()

    /** Job for the periodic prompt detection polling loop. */
    private var promptDetectionJob: Job? = null

    /** Tracks the current bridge's host ID for reactive history queries. */
    private val _currentHostId = MutableStateFlow(hostId)

    /** Command history for the current bridge's host, updated reactively. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val commandHistory: StateFlow<List<CommandHistory>> = _currentHostId
        .flatMapLatest { id ->
            if (id > 0L) {
                commandHistoryRepository.observeForHost(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setTerminalManager(manager: TerminalManager) {
        if (terminalManager != manager) {
            terminalManager = manager
            // Observe bridges flow from TerminalManager
            viewModelScope.launch {
                manager.bridgesFlow.collect { bridges ->
                    updateBridges(bridges)
                    subscribeToActiveBridgeBells(bridges)
                    subscribeToActiveBridgeProgress(bridges)
                    subscribeToNetworkStatusMessages(bridges)
                }
            }

            // Observe host status changes (connect/disconnect events) to refresh UI
            viewModelScope.launch {
                manager.hostStatusChangedFlow.collect {
                    _uiState.update { it.copy(revision = it.revision + 1) }
                }
            }

            // Start CLI prompt detection polling
            startPromptDetection()

            // Observe per-bridge prompts from the manager for tab badges
            viewModelScope.launch {
                manager.bridgePrompts.collect { prompts ->
                    _promptHostIds.value = prompts.keys
                }
            }

            // Propagate connection/service errors to the UI
            viewModelScope.launch {
                manager.serviceErrors.collect { error ->
                    val errorMessage = formatServiceError(error)
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            }

            // First, try to find or create the bridge for this host
            if (hostId != -1L) {
                viewModelScope.launch {
                    ensureBridgeExists()
                }
            }
        }
    }

    private fun formatServiceError(error: ServiceError): String = when (error) {
        is ServiceError.KeyLoadFailed ->
            context.getString(R.string.error_key_load_failed, error.keyName, error.reason)
        is ServiceError.ConnectionFailed ->
            context.getString(
                R.string.error_connection_failed,
                error.hostNickname,
                error.hostname,
                error.reason
            )
        is ServiceError.PortForwardLoadFailed ->
            context.getString(
                R.string.error_port_forward_load_failed,
                error.hostNickname,
                error.reason
            )
        is ServiceError.HostSaveFailed ->
            context.getString(R.string.error_host_save_failed, error.hostNickname, error.reason)
        is ServiceError.ColorSchemeLoadFailed ->
            context.getString(R.string.error_color_scheme_load_failed, error.reason)
    }

    private fun subscribeToActiveBridgeBells(bridges: List<TerminalBridge>) {
        viewModelScope.launch {
            bridges.forEach { bridge ->
                launch {
                    bridge.bellEvents.collect {
                        val currentIndex = _uiState.value.currentBridgeIndex
                        val currentBridge = _uiState.value.bridges.getOrNull(currentIndex)

                        if (currentBridge == bridge) {
                            // The bridge is visible, play the beep
                            terminalManager?.playBeep()
                        } else {
                            // The bridge is not visible, send a notification
                            currentBridge?.host?.let {
                                terminalManager?.sendActivityNotification(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToActiveBridgeProgress(bridges: List<TerminalBridge>) {
        viewModelScope.launch {
            bridges.forEach { bridge ->
                launch {
                    bridge.progressState.collect { progressInfo ->
                        val currentIndex = _uiState.value.currentBridgeIndex
                        val currentBridge = _uiState.value.bridges.getOrNull(currentIndex)

                        if (currentBridge == bridge) {
                            // Update progress state for the visible bridge
                            _uiState.update {
                                if (progressInfo == null || progressInfo.state == ProgressState.HIDDEN) {
                                    it.copy(progressState = null, progressValue = 0)
                                } else {
                                    it.copy(
                                        progressState = progressInfo.state,
                                        progressValue = progressInfo.progress
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun subscribeToNetworkStatusMessages(bridges: List<TerminalBridge>) {
        viewModelScope.launch {
            bridges.forEach { bridge ->
                launch {
                    bridge.networkStatusMessages.collect { message ->
                        val currentBridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
                        if (currentBridge == bridge) {
                            _networkStatusMessages.emit(message)
                        }
                    }
                }
            }
        }
    }

    private suspend fun ensureBridgeExists() {
        withContext(dispatchers.io) {
            try {
                val allBridges = terminalManager?.bridgesFlow?.value ?: emptyList()

                // Check if we already have a bridge for this host
                val existingBridge = allBridges.find { bridge ->
                    bridge.host.id == hostId
                }

                // If no bridge exists, create one using the service method
                if (existingBridge == null) {
                    if (hostId < 0L) {
                        // Temporary host - should already exist from MainActivity/URI handling
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Temporary connection not found"
                            )
                        }
                    } else {
                        // Permanent host - create from database
                        val bridge = terminalManager?.openConnectionForHostId(hostId)
                        if (bridge == null) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to open connection: host not found"
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create connection"
                    )
                }
            }
        }
    }

    private fun updateBridges(allBridges: List<TerminalBridge>) {
        // Deduplicate bridges by host ID to prevent duplicate tabs when
        // TerminalManager emits the same bridge more than once (e.g. during
        // reconnect or when multiple bridge objects exist for the same host).
        val bridges = allBridges.distinctBy { it.host.id }

        _uiState.update {
            val newIndex = if (!initialBridgeSelected && hostId != -1L) {
                // First time: select the bridge matching the hostId from navigation
                val targetIndex = bridges.indexOfFirst { bridge -> bridge.host.id == hostId }
                if (targetIndex >= 0) {
                    initialBridgeSelected = true
                    targetIndex
                } else {
                    // Target bridge not yet available, keep current index
                    it.currentBridgeIndex.coerceIn(0, (bridges.size - 1).coerceAtLeast(0))
                }
            } else if (it.currentBridgeIndex >= bridges.size) {
                // Adjust index if it's now out of range (e.g., a bridge was closed)
                (bridges.size - 1).coerceAtLeast(0)
            } else {
                it.currentBridgeIndex
            }

            // Stop loading when the target bridge is available (or when hostId == -1)
            val targetBridgeAvailable = if (hostId != -1L) {
                bridges.any { bridge -> bridge.host.id == hostId }
            } else {
                true
            }

            it.copy(
                bridges = bridges,
                currentBridgeIndex = newIndex,
                isLoading = if (targetBridgeAvailable) false else it.isLoading,
                // Only clear error when the bridge is available — preserve connection errors
                error = if (targetBridgeAvailable) null else it.error
            )
        }
        val selectedBridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
        selectedBridge?.host?.id?.let {
            _currentHostId.value = it
            terminalManager?.visibleBridgeHostId = it
        }
    }

    /**
     * Called when navigating to an existing ConsoleScreen with a new hostId.
     * Updates the selected bridge to match the new hostId and creates
     * the connection if needed.
     */
    fun navigateToHost(newHostId: Long) {
        if (newHostId == -1L) return
        hostId = newHostId
        _currentHostId.value = newHostId
        initialBridgeSelected = false

        // Try to select the bridge for this host immediately
        val currentBridges = _uiState.value.bridges
        val targetIndex = currentBridges.indexOfFirst { it.host.id == newHostId }
        if (targetIndex >= 0) {
            initialBridgeSelected = true
            _uiState.update { it.copy(currentBridgeIndex = targetIndex) }
        } else {
            // Bridge doesn't exist yet, create it
            viewModelScope.launch {
                ensureBridgeExists()
            }
        }
    }

    fun selectBridge(index: Int) {
        if (index in _uiState.value.bridges.indices) {
            _uiState.update { it.copy(currentBridgeIndex = index) }
            val bridge = _uiState.value.bridges.getOrNull(index)
            bridge?.host?.id?.let {
                _currentHostId.value = it
                terminalManager?.visibleBridgeHostId = it
            }
            _detectedPrompt.value = null
        }
    }

    /**
     * Refresh the UI state to trigger recomposition.
     * This is needed because bridge state (isSessionOpen, isDisconnected, etc.)
     * changes asynchronously but doesn't trigger Compose recomposition automatically.
     */
    fun refreshMenuState() {
        _uiState.update { it.copy(revision = it.revision + 1) }
    }

    /**
     * Request a reconnection for the given bridge.
     */
    fun reconnect(bridge: TerminalBridge) {
        terminalManager?.requestReconnect(bridge)
    }

    /**
     * Disconnect a specific bridge (used for closing tabs).
     * Immediately removes the bridge from the UI state so the tab disappears
     * without waiting for the async StateFlow round-trip through TerminalManager.
     */
    fun disconnectBridge(bridge: TerminalBridge) {
        _uiState.update { state ->
            val newBridges = state.bridges.filterNot { it === bridge }
            val newIndex = if (state.currentBridgeIndex >= newBridges.size) {
                (newBridges.size - 1).coerceAtLeast(0)
            } else {
                state.currentBridgeIndex
            }
            state.copy(bridges = newBridges, currentBridgeIndex = newIndex)
        }
        bridge.dispatchDisconnect(true)
    }

    /**
     * Rename the tab at the given index with a custom display name.
     * The name is stored on the TerminalBridge instance (not persisted).
     */
    fun renameTab(index: Int, newName: String) {
        val bridge = _uiState.value.bridges.getOrNull(index) ?: return
        bridge.customTabName = newName.ifBlank { null }
        // Trigger recomposition
        _uiState.update { it.copy(revision = it.revision + 1) }
    }

    /**
     * Toggle visibility of the quick command toolbar chips.
     */
    fun toggleQuickCommandToolbar() {
        _isQuickCommandToolbarVisible.update { !it }
    }

    /**
     * Send a quick command string to the active terminal bridge.
     * Appends a newline so the command is executed immediately.
     * Also records the command in history.
     */
    fun sendQuickCommand(command: String) {
        val currentBridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
        currentBridge?.injectString(command + "\r")
        recordCommand(command)
    }

    /**
     * Record a command in the history for the current host.
     */
    fun recordCommand(command: String) {
        val currentHostId = _currentHostId.value
        if (currentHostId <= 0L || command.isBlank()) return
        viewModelScope.launch {
            commandHistoryRepository.recordCommand(currentHostId, command.trim())
        }
    }

    /**
     * Clear all command history for the current host.
     */
    fun clearHistory() {
        val currentHostId = _currentHostId.value
        if (currentHostId <= 0L) return
        viewModelScope.launch {
            commandHistoryRepository.deleteForHost(currentHostId)
        }
    }

    /**
     * Send a prompt option value to the active terminal bridge and dismiss the bar.
     */
    fun sendPromptResponse(value: String) {
        val currentBridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
        currentBridge?.injectString(value)
        _detectedPrompt.value = null
    }

    /**
     * Dismiss the detected prompt bar without sending anything.
     */
    fun dismissPrompt() {
        _detectedPrompt.value = null
    }

    /**
     * Start (or restart) the periodic prompt detection loop for the current bridge.
     * Polls the relay line buffer every [PROMPT_SCAN_INTERVAL_MS] and runs the
     * [CliPromptDetector] on recent output lines. Detection stops when the bridge
     * changes or the ViewModel is cleared.
     */
    private fun startPromptDetection() {
        promptDetectionJob?.cancel()
        _detectedPrompt.value = null

        promptDetectionJob = viewModelScope.launch {
            while (isActive) {
                delay(PROMPT_SCAN_INTERVAL_MS)
                val bridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
                if (bridge != null && bridge.isSessionOpen) {
                    val lines = bridge.getRecentOutputLines()
                    val detected = CliPromptDetector.detect(lines)
                    _detectedPrompt.value = detected
                } else {
                    _detectedPrompt.value = null
                }
            }
        }
    }

    companion object {
        /** How often to scan terminal output for CLI prompts, in milliseconds. */
        private const val PROMPT_SCAN_INTERVAL_MS = 1500L
    }
}
