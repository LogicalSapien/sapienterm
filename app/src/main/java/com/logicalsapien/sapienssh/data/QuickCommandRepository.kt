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
import com.logicalsapien.sapienssh.data.dao.QuickCommandDao
import com.logicalsapien.sapienssh.data.entity.QuickCommand
import com.logicalsapien.sapienssh.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing quick commands.
 * Quick commands are reusable terminal commands that can be executed from the toolbar.
 *
 * @param quickCommandDao The DAO for accessing quick command data
 * @param dispatchers Coroutine dispatchers for IO operations
 */
@Singleton
class QuickCommandRepository @Inject constructor(
    private val quickCommandDao: QuickCommandDao,
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Observe all quick commands.
     */
    fun observeAll(): Flow<List<QuickCommand>> = quickCommandDao.observeAll()

    /**
     * Search quick commands by title or category.
     */
    fun search(query: String): Flow<List<QuickCommand>> = quickCommandDao.search(query)

    /**
     * Observe all distinct categories.
     */
    fun observeCategories(): Flow<List<String>> = quickCommandDao.observeCategories()

    /**
     * Add a new quick command.
     *
     * @param command The quick command to add
     * @return The ID of the newly created command
     */
    suspend fun add(command: QuickCommand): Long = withContext(dispatchers.io) {
        quickCommandDao.insert(command)
    }

    /**
     * Update an existing quick command.
     */
    suspend fun update(command: QuickCommand) = withContext(dispatchers.io) {
        quickCommandDao.update(command)
    }

    /**
     * Delete a quick command.
     */
    suspend fun delete(command: QuickCommand) = withContext(dispatchers.io) {
        quickCommandDao.delete(command)
    }

    /**
     * Get a quick command by ID.
     */
    suspend fun getById(id: Long): QuickCommand? = withContext(dispatchers.io) {
        quickCommandDao.getById(id)
    }

    /**
     * Get all quick commands.
     */
    suspend fun getAll(): List<QuickCommand> = withContext(dispatchers.io) {
        quickCommandDao.getAll()
    }

    /**
     * Delete all quick commands.
     */
    suspend fun deleteAll() = withContext(dispatchers.io) {
        quickCommandDao.deleteAll()
    }
}
