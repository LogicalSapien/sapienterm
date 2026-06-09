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

package com.logicalsapien.sapienterm.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.logicalsapien.sapienterm.data.dao.ConnectionGroupDao
import com.logicalsapien.sapienterm.data.entity.ConnectionGroup
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing connection groups.
 * Connection groups allow users to organize hosts into color-coded folders.
 *
 * @param connectionGroupDao The DAO for accessing connection group data
 * @param dispatchers Coroutine dispatchers for IO operations
 */
@Singleton
class ConnectionGroupRepository @Inject constructor(
    private val connectionGroupDao: ConnectionGroupDao,
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Observe all connection groups.
     */
    fun observeAll(): Flow<List<ConnectionGroup>> = connectionGroupDao.observeAll()

    /**
     * Add a new connection group.
     *
     * @param group The connection group to add
     * @return The ID of the newly created group
     */
    suspend fun add(group: ConnectionGroup): Long = withContext(dispatchers.io) {
        connectionGroupDao.insert(group)
    }

    /**
     * Update an existing connection group.
     */
    suspend fun update(group: ConnectionGroup) = withContext(dispatchers.io) {
        connectionGroupDao.update(group)
    }

    /**
     * Delete a connection group.
     */
    suspend fun delete(group: ConnectionGroup) = withContext(dispatchers.io) {
        connectionGroupDao.delete(group)
    }

    /**
     * Get a connection group by ID.
     */
    suspend fun getById(id: Long): ConnectionGroup? = withContext(dispatchers.io) {
        connectionGroupDao.getById(id)
    }

    /**
     * Get all connection groups (one-time query).
     */
    suspend fun getAll(): List<ConnectionGroup> = withContext(dispatchers.io) {
        connectionGroupDao.getAll()
    }

    /**
     * Delete all connection groups.
     */
    suspend fun deleteAll() = withContext(dispatchers.io) {
        connectionGroupDao.deleteAll()
    }
}
