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

package com.logicalsapien.sapienssh.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.logicalsapien.sapienssh.data.entity.QuickCommand

/**
 * Data Access Object for quick commands.
 */
@Dao
interface QuickCommandDao {
    /**
     * Observe all quick commands, ordered by sort order then creation date.
     */
    @Query("SELECT * FROM quick_commands ORDER BY sort_order ASC, created_at DESC")
    fun observeAll(): Flow<List<QuickCommand>>

    /**
     * Search quick commands by title or category.
     */
    @Query("SELECT * FROM quick_commands WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<QuickCommand>>

    /**
     * Observe all distinct categories.
     */
    @Query("SELECT DISTINCT category FROM quick_commands WHERE category IS NOT NULL ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    /**
     * Insert a new quick command.
     * @return The ID of the newly inserted command
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: QuickCommand): Long

    /**
     * Update an existing quick command.
     */
    @Update
    suspend fun update(command: QuickCommand)

    /**
     * Delete a quick command.
     */
    @Delete
    suspend fun delete(command: QuickCommand)

    /**
     * Get a single quick command by ID (one-time query).
     */
    @Query("SELECT * FROM quick_commands WHERE id = :id")
    suspend fun getById(id: Long): QuickCommand?

    /**
     * Get all quick commands (one-time query).
     */
    @Query("SELECT * FROM quick_commands")
    suspend fun getAll(): List<QuickCommand>
}
