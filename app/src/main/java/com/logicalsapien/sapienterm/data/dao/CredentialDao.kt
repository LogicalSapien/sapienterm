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
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.logicalsapien.sapienterm.data.entity.Credential
import com.logicalsapien.sapienterm.data.entity.CredentialType

/**
 * Data Access Object for credentials.
 *
 * Provides both Flow-based reactive queries for UI observation
 * and suspend functions for one-time operations.
 */
@Dao
interface CredentialDao {
    /**
     * Observe all credentials, ordered by label.
     * Emits new list whenever credentials are added, updated, or deleted.
     */
    @Query("SELECT * FROM credentials ORDER BY label ASC")
    fun observeAll(): Flow<List<Credential>>

    /**
     * Observe credentials filtered by type.
     */
    @Query("SELECT * FROM credentials WHERE type = :type ORDER BY label ASC")
    fun observeByType(type: CredentialType): Flow<List<Credential>>

    /**
     * Insert a new credential.
     * @return The ID of the newly inserted credential
     */
    @Insert
    suspend fun insert(credential: Credential): Long

    /**
     * Update an existing credential.
     */
    @Update
    suspend fun update(credential: Credential)

    /**
     * Delete a credential.
     */
    @Delete
    suspend fun delete(credential: Credential)

    /**
     * Delete all credentials.
     */
    @Query("DELETE FROM credentials")
    suspend fun deleteAll()

    /**
     * Get a single credential by ID (one-time query).
     */
    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getById(id: Long): Credential?

    /**
     * Get all credentials (one-time query).
     */
    @Query("SELECT * FROM credentials ORDER BY label ASC")
    suspend fun getAll(): List<Credential>
}
