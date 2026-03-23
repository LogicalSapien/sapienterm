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

package com.logicalsapien.sapienssh.ui.screens.hostlist

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.data.ConnectionGroupRepository
import com.logicalsapien.sapienssh.data.HostRepository
import com.logicalsapien.sapienssh.data.entity.ConnectionGroup
import com.logicalsapien.sapienssh.data.entity.Host
import com.logicalsapien.sapienssh.di.CoroutineDispatchers
import com.logicalsapien.sapienssh.service.ServiceError
import com.logicalsapien.sapienssh.service.TerminalManager
import com.logicalsapien.sapienssh.util.PreferenceConstants
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject

enum class ConnectionState {
    UNKNOWN,
    CONNECTED,
    DISCONNECTED
}

/**
 * Health status of a host connection, checked via TCP connect.
 *
 * @property isReachable Whether the host responded within the timeout
 * @property latencyMs Round-trip latency in milliseconds (null if unreachable)
 * @property lastChecked Timestamp of the last health check
 */
data class HealthStatus(
    val isReachable: Boolean,
    val latencyMs: Long?,
    val lastChecked: Long
)

data class HostListUiState(
    val hosts: List<Host> = emptyList(),
    val connectionStates: Map<Long, ConnectionState> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val sortedByColor: Boolean = false,
    val exportedJson: String? = null,
    val exportResult: ExportResult? = null,
    val importResult: ImportResult? = null
)

data class ImportResult(
    val hostsImported: Int,
    val hostsSkipped: Int,
    val profilesImported: Int,
    val profilesSkipped: Int
)

data class ExportResult(
    val hostCount: Int,
    val profileCount: Int
)

@HiltViewModel
class HostListViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: HostRepository,
    private val groupRepository: ConnectionGroupRepository,
    private val dispatchers: CoroutineDispatchers,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private var terminalManager: TerminalManager? = null
    private val _uiState = MutableStateFlow(
        HostListUiState(
            isLoading = true,
            sortedByColor = sharedPreferences.getBoolean(PreferenceConstants.SORT_BY_COLOR, false)
        )
    )
    val uiState: StateFlow<HostListUiState> = _uiState.asStateFlow()

    private val _healthStatus = MutableStateFlow<Map<Long, HealthStatus>>(emptyMap())
    /** Health status for each saved host, updated every 30 seconds via TCP connect check. */
    val healthStatus: StateFlow<Map<Long, HealthStatus>> = _healthStatus.asStateFlow()

    private val _groups = MutableStateFlow<List<ConnectionGroup>>(emptyList())
    /** All connection groups, ordered by sort order then name. */
    val groups: StateFlow<List<ConnectionGroup>> = _groups.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    /** Currently selected group filter. Null means show all connections. */
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    private var healthCheckJob: Job? = null

    init {
        observeHosts()
        observeGroups()
        startHealthMonitor()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            groupRepository.observeAll().collect { groupList ->
                _groups.value = groupList
            }
        }
    }

    fun setTerminalManager(manager: TerminalManager) {
        if (terminalManager != manager) {
            terminalManager = manager
            // Observe host status changes from Flow
            observeHostStatusChanges()
            // Collect service errors from TerminalManager
            collectServiceErrors()
            // Update initial connection states
            updateConnectionStates(_uiState.value.hosts)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeHosts() {
        viewModelScope.launch {
            _uiState
                .map { it.sortedByColor }
                .distinctUntilChanged()
                .flatMapLatest { sortedByColor ->
                    if (sortedByColor) {
                        repository.observeHostsSortedByColor()
                    } else {
                        repository.observeHosts()
                    }
                }
                .collect { hosts ->
                    updateConnectionStates(hosts)
                    _uiState.update {
                        it.copy(hosts = hosts, isLoading = false, error = null)
                    }
                }
        }
    }

    private fun observeHostStatusChanges() {
        val manager = terminalManager ?: return
        viewModelScope.launch {
            manager.hostStatusChangedFlow.collect {
                // Update connection states when terminal manager notifies us of changes
                updateConnectionStates(_uiState.value.hosts)
            }
        }
    }

    private fun collectServiceErrors() {
        val manager = terminalManager ?: return
        viewModelScope.launch {
            manager.serviceErrors.collect { error ->
                val errorMessage = formatServiceError(error)
                _uiState.update { it.copy(error = errorMessage) }
            }
        }
    }

    private fun formatServiceError(error: ServiceError): String = when (error) {
        is ServiceError.KeyLoadFailed -> {
            context.getString(R.string.error_key_load_failed, error.keyName, error.reason)
        }

        is ServiceError.ConnectionFailed -> {
            context.getString(
                R.string.error_connection_failed,
                error.hostNickname,
                error.hostname,
                error.reason
            )
        }

        is ServiceError.PortForwardLoadFailed -> {
            context.getString(
                R.string.error_port_forward_load_failed,
                error.hostNickname,
                error.reason
            )
        }

        is ServiceError.HostSaveFailed -> {
            context.getString(R.string.error_host_save_failed, error.hostNickname, error.reason)
        }

        is ServiceError.ColorSchemeLoadFailed -> {
            context.getString(R.string.error_color_scheme_load_failed, error.reason)
        }
    }

    private fun updateConnectionStates(hosts: List<Host>) {
        val states = hosts.associate { host ->
            host.id to getConnectionState(host)
        }
        _uiState.update { it.copy(connectionStates = states) }
    }

    private fun getConnectionState(host: Host): ConnectionState {
        val manager = terminalManager ?: return ConnectionState.UNKNOWN

        // Check if connected by ID
        if (manager.bridgesFlow.value.any { it.host.id == host.id }) {
            return ConnectionState.CONNECTED
        }

        // Check if in disconnected list by comparing ID
        if (manager.disconnectedFlow.value.any { it.id == host.id }) {
            return ConnectionState.DISCONNECTED
        }

        return ConnectionState.UNKNOWN
    }

    fun toggleSortOrder() {
        val newSortedByColor = !_uiState.value.sortedByColor
        sharedPreferences.edit { putBoolean(PreferenceConstants.SORT_BY_COLOR, newSortedByColor) }
        _uiState.update { it.copy(sortedByColor = newSortedByColor) }
    }

    fun deleteHost(host: Host) {
        viewModelScope.launch {
            try {
                repository.deleteHost(host)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to delete host")
                }
            }
        }
    }

    fun duplicateHost(host: Host) {
        viewModelScope.launch {
            try {
                // Create new host with reset fields
                val newHost = host.copy(
                    id = 0L,
                    nickname = context.getString(R.string.host_duplicate_nickname, host.nickname),
                    lastConnect = 0,
                    hostKeyAlgo = null
                )
                val savedHost = repository.saveHost(newHost)

                // Copy port forwards
                val portForwards = repository.getPortForwardsForHost(host.id)
                for (pf in portForwards) {
                    repository.savePortForward(pf.copy(id = 0L, hostId = savedHost.id))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to duplicate host")
                }
            }
        }
    }

    fun renameHost(host: Host, newNickname: String) {
        viewModelScope.launch {
            try {
                val updatedHost = host.copy(nickname = newNickname)
                repository.saveHost(updatedHost)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to rename host")
                }
            }
        }
    }

    fun forgetHostKeys(host: Host) {
        viewModelScope.launch {
            try {
                repository.deleteKnownHostsForHost(host.id)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to forget host keys")
                }
            }
        }
    }

    fun disconnectAll() {
        terminalManager?.disconnectAll(immediate = true, excludeLocal = false)
    }

    fun disconnectHost(host: Host) {
        val bridge = terminalManager?.bridgesFlow?.value?.find { it.host.id == host.id }
        bridge?.dispatchDisconnect(true)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun exportHosts() {
        viewModelScope.launch {
            try {
                val (json, exportCounts) = withContext(dispatchers.io) {
                    repository.exportHostsToJson()
                }
                val exportResult = ExportResult(
                    hostCount = exportCounts.hostCount,
                    profileCount = exportCounts.profileCount
                )
                _uiState.update { it.copy(exportedJson = json, exportResult = exportResult) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to export hosts")
                }
            }
        }
    }

    fun clearExportedJson() {
        _uiState.update { it.copy(exportedJson = null, exportResult = null) }
    }

    fun importHosts(jsonString: String) {
        viewModelScope.launch {
            try {
                val importCounts = withContext(dispatchers.io) {
                    repository.importHostsFromJson(jsonString)
                }
                val importResult = ImportResult(
                    hostsImported = importCounts.hostsImported,
                    hostsSkipped = importCounts.hostsSkipped,
                    profilesImported = importCounts.profilesImported,
                    profilesSkipped = importCounts.profilesSkipped
                )
                _uiState.update { it.copy(importResult = importResult) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to import hosts")
                }
            }
        }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    // --- Connection Group methods ---

    /** Select a group to filter the host list. Null means show all. */
    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
    }

    /** Create a new connection group. */
    fun createGroup(name: String) {
        viewModelScope.launch {
            try {
                groupRepository.add(ConnectionGroup(name = name))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to create group") }
            }
        }
    }

    /** Rename an existing connection group. */
    fun renameGroup(group: ConnectionGroup, newName: String) {
        viewModelScope.launch {
            try {
                groupRepository.update(group.copy(name = newName))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to rename group") }
            }
        }
    }

    /** Delete a connection group. Hosts in the group become ungrouped. */
    fun deleteGroup(group: ConnectionGroup) {
        viewModelScope.launch {
            try {
                // Ungroup all hosts in this group first
                val hosts = _uiState.value.hosts.filter { it.groupId == group.id }
                for (host in hosts) {
                    repository.saveHost(host.copy(groupId = null))
                }
                groupRepository.delete(group)
                // If the deleted group was selected, reset filter
                if (_selectedGroupId.value == group.id) {
                    _selectedGroupId.value = null
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to delete group") }
            }
        }
    }

    /** Move a host to a group (or null to ungroup). */
    fun moveHostToGroup(hostId: Long, groupId: Long?) {
        viewModelScope.launch {
            try {
                val host = _uiState.value.hosts.find { it.id == hostId } ?: return@launch
                repository.saveHost(host.copy(groupId = groupId))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to move host") }
            }
        }
    }

    /**
     * Start the background health monitor coroutine.
     * Checks each saved host's reachability every 30 seconds via TCP connect.
     */
    private fun startHealthMonitor() {
        healthCheckJob = viewModelScope.launch {
            while (isActive) {
                val hosts = _uiState.value.hosts
                if (hosts.isNotEmpty()) {
                    val results = mutableMapOf<Long, HealthStatus>()
                    for (host in hosts) {
                        // Skip local connections
                        if (host.protocol == "local" || host.hostname.isBlank()) {
                            continue
                        }
                        val status = checkHealth(host.hostname, host.port)
                        results[host.id] = status
                    }
                    _healthStatus.value = results
                }
                delay(30_000L)
            }
        }
    }

    /**
     * Perform a TCP connect health check on a host.
     *
     * @param host The hostname to check
     * @param port The port to connect to
     * @param timeoutMs Connection timeout in milliseconds
     * @return HealthStatus with reachability and latency
     */
    private suspend fun checkHealth(
        host: String,
        port: Int,
        timeoutMs: Int = 3000
    ): HealthStatus {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                val latency = System.currentTimeMillis() - startTime
                HealthStatus(true, latency, System.currentTimeMillis())
            } catch (_: Exception) {
                HealthStatus(false, null, System.currentTimeMillis())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        healthCheckJob?.cancel()
    }
}
