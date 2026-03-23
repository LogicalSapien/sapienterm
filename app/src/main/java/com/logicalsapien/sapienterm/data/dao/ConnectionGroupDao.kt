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
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.logicalsapien.sapienterm.data.entity.ConnectionGroup

/**
 * Data Access Object for connection groups.
 */
@Dao
interface ConnectionGroupDao {
    /**
     * Observe all connection groups, ordered by sort order then name.
     */
    @Query("SELECT * FROM connection_groups ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<ConnectionGroup>>

    /**
     * Insert a new connection group.
     * @return The ID of the newly inserted group
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: ConnectionGroup): Long

    /**
     * Update an existing connection group.
     */
    @Update
    suspend fun update(group: ConnectionGroup)

    /**
     * Delete a connection group.
     */
    @Delete
    suspend fun delete(group: ConnectionGroup)

    /**
     * Get a single connection group by ID (one-time query).
     */
    @Query("SELECT * FROM connection_groups WHERE id = :id")
    suspend fun getById(id: Long): ConnectionGroup?
}
