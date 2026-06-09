/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2025 SapienTerm contributors
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

package com.logicalsapien.sapienterm.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.logicalsapien.sapienterm.data.entity.CommandHistory

/**
 * Data Access Object for command history.
 */
@Dao
interface CommandHistoryDao {
    /**
     * Observe command history for a specific host, most recent first.
     */
    @Query("SELECT * FROM command_history WHERE host_id = :hostId ORDER BY executed_at DESC LIMIT 100")
    fun observeForHost(hostId: Long): Flow<List<CommandHistory>>

    /**
     * Observe unique commands for a specific host, most recent first.
     */
    @Query("SELECT DISTINCT command FROM command_history WHERE host_id = :hostId ORDER BY executed_at DESC LIMIT 50")
    fun observeUniqueCommandsForHost(hostId: Long): Flow<List<String>>

    /**
     * Observe all unique commands across all hosts, most recent first.
     */
    @Query("SELECT DISTINCT command FROM command_history ORDER BY executed_at DESC LIMIT 100")
    fun observeAllUniqueCommands(): Flow<List<String>>

    /**
     * Insert a new command history entry.
     * @return The ID of the newly inserted entry
     */
    @Insert
    suspend fun insert(entry: CommandHistory): Long

    /**
     * Delete all command history for a specific host.
     */
    @Query("DELETE FROM command_history WHERE host_id = :hostId")
    suspend fun deleteForHost(hostId: Long)

    /**
     * Delete all command history.
     */
    @Query("DELETE FROM command_history")
    suspend fun deleteAll()
}
