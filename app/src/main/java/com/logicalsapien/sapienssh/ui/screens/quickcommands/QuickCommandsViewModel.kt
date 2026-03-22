/*
 * SapienSSH: modern SSH client for Android
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

package com.logicalsapien.sapienssh.ui.screens.quickcommands

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.data.QuickCommandRepository
import com.logicalsapien.sapienssh.data.entity.QuickCommand
import com.logicalsapien.sapienssh.service.TerminalManager
import javax.inject.Inject

@HiltViewModel
class QuickCommandsViewModel @Inject constructor(
    private val repository: QuickCommandRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allCommands: StateFlow<List<QuickCommand>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.observeAll()
            } else {
                repository.search(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commands: StateFlow<List<QuickCommand>> = combine(
        allCommands,
        _selectedCategory
    ) { cmds, category ->
        if (category == null) {
            cmds
        } else {
            cmds.filter { it.category == category }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var terminalManager: TerminalManager? = null

    fun setTerminalManager(manager: TerminalManager) {
        terminalManager = manager
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addCommand(title: String, command: String, category: String?) {
        viewModelScope.launch {
            repository.add(
                QuickCommand(
                    title = title,
                    command = command,
                    category = category?.takeIf { it.isNotBlank() },
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateCommand(command: QuickCommand) {
        viewModelScope.launch {
            repository.update(command)
        }
    }

    fun deleteCommand(command: QuickCommand) {
        viewModelScope.launch {
            repository.delete(command)
        }
    }

    /**
     * Duplicate a quick command by creating a copy with "(copy)" appended to the title
     * and a new auto-generated id.
     */
    fun duplicateCommand(command: QuickCommand) {
        viewModelScope.launch {
            repository.add(
                command.copy(
                    id = 0,
                    title = "${command.title} (copy)",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Attempt to send a command to the active terminal session.
     * @return true if sent successfully, false if no active session
     */
    fun sendCommand(command: String): Boolean {
        val manager = terminalManager ?: return false
        val bridges = manager.bridgesFlow.value
        if (bridges.isEmpty()) return false
        // Send to the most recently active bridge (last in list)
        val bridge = bridges.last()
        bridge.injectString(command + "\n")
        return true
    }

    /**
     * Check if there is an active terminal session.
     */
    fun hasActiveSession(): Boolean {
        val manager = terminalManager ?: return false
        return manager.bridgesFlow.value.isNotEmpty()
    }
}
