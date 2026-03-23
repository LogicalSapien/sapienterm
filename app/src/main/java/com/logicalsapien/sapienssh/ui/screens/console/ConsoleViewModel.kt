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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.logicalsapien.sapienssh.data.CommandHistoryRepository
import com.logicalsapien.sapienssh.data.QuickCommandRepository
import com.logicalsapien.sapienssh.data.entity.CommandHistory
import com.logicalsapien.sapienssh.data.entity.QuickCommand
import com.logicalsapien.sapienssh.di.CoroutineDispatchers
import com.logicalsapien.sapienssh.service.TerminalBridge
import com.logicalsapien.sapienssh.service.TerminalManager
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
    private val commandHistoryRepository: CommandHistoryRepository
) : ViewModel() {
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

    /** Tracks the current bridge's host ID for reactive history queries. */
    private val _currentHostId = MutableStateFlow(hostId)

    /** Command history for the current bridge's host, updated reactively. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val commandHistory: StateFlow<List<CommandHistory>> = _currentHostId
        .flatMapLatest { id ->
            if (id > 0L) commandHistoryRepository.observeForHost(id)
            else flowOf(emptyList())
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

            // First, try to find or create the bridge for this host
            if (hostId != -1L) {
                viewModelScope.launch {
                    ensureBridgeExists()
                }
            }
        }
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
                error = null
            )
        }
        // Keep _currentHostId in sync with the selected bridge
        val selectedBridge = _uiState.value.bridges.getOrNull(_uiState.value.currentBridgeIndex)
        selectedBridge?.host?.id?.let { _currentHostId.value = it }
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
            // Update the host ID so command history reacts to bridge changes
            val bridge = _uiState.value.bridges.getOrNull(index)
            bridge?.host?.id?.let { _currentHostId.value = it }
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
     */
    fun disconnectBridge(bridge: TerminalBridge) {
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
        currentBridge?.injectString(command + "\n")
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
}
