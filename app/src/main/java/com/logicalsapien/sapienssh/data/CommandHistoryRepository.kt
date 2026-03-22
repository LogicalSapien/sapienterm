/*
 * SapienSSH: modern SSH client for Android
 * Copyright 2025 SapienSSH contributors
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

package com.logicalsapien.sapienssh.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.logicalsapien.sapienssh.data.dao.CommandHistoryDao
import com.logicalsapien.sapienssh.data.entity.CommandHistory
import com.logicalsapien.sapienssh.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing command history.
 * Tracks commands executed on each host for recall and auto-completion.
 *
 * @param commandHistoryDao The DAO for accessing command history data
 * @param dispatchers Coroutine dispatchers for IO operations
 */
@Singleton
class CommandHistoryRepository @Inject constructor(
    private val commandHistoryDao: CommandHistoryDao,
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Observe command history for a specific host.
     */
    fun observeForHost(hostId: Long): Flow<List<CommandHistory>> =
        commandHistoryDao.observeForHost(hostId)

    /**
     * Observe unique commands for a specific host.
     */
    fun observeUniqueCommandsForHost(hostId: Long): Flow<List<String>> =
        commandHistoryDao.observeUniqueCommandsForHost(hostId)

    /**
     * Observe all unique commands across all hosts.
     */
    fun observeAllUniqueCommands(): Flow<List<String>> =
        commandHistoryDao.observeAllUniqueCommands()

    /**
     * Record a command execution.
     *
     * @param hostId The host where the command was executed
     * @param command The command string that was executed
     * @return The ID of the newly created history entry
     */
    suspend fun recordCommand(hostId: Long, command: String): Long = withContext(dispatchers.io) {
        commandHistoryDao.insert(
            CommandHistory(
                hostId = hostId,
                command = command
            )
        )
    }

    /**
     * Delete all command history for a specific host.
     */
    suspend fun deleteForHost(hostId: Long) = withContext(dispatchers.io) {
        commandHistoryDao.deleteForHost(hostId)
    }

    /**
     * Delete all command history.
     */
    suspend fun deleteAll() = withContext(dispatchers.io) {
        commandHistoryDao.deleteAll()
    }
}
