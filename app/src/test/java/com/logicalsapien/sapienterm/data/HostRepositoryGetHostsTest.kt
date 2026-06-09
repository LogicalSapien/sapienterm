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

package com.logicalsapien.sapienterm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.logicalsapien.sapienterm.data.dao.HostDao
import com.logicalsapien.sapienterm.data.dao.KnownHostDao
import com.logicalsapien.sapienterm.data.dao.PortForwardDao
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.util.SecurePasswordStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Tests for [HostRepository.getHosts], verifying DAO routing and return values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HostRepositoryGetHostsTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var database: ConnectBotDatabase
    private lateinit var hostDao: HostDao
    private lateinit var portForwardDao: PortForwardDao
    private lateinit var knownHostDao: KnownHostDao
    private lateinit var securePasswordStorage: SecurePasswordStorage
    private lateinit var repository: HostRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mock()
        database = mock()
        hostDao = mock()
        portForwardDao = mock()
        knownHostDao = mock()
        securePasswordStorage = mock()
        repository = HostRepository(
            context,
            database,
            hostDao,
            portForwardDao,
            knownHostDao,
            securePasswordStorage
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getHosts_sortedByColorFalse_callsGetAllAndReturnsList() = runTest {
        val expected = listOf(
            Host(id = 1L, nickname = "alpha", hostname = "a.example.com"),
            Host(id = 2L, nickname = "beta", hostname = "b.example.com")
        )
        whenever { hostDao.getAll() }.thenReturn(expected)

        val result = repository.getHosts(sortedByColor = false)

        assertEquals(expected, result)
        verifyBlocking(hostDao, times(1)) { getAll() }
        verifyBlocking(hostDao, never()) { getAllSortedByColor() }
    }

    @Test
    fun getHosts_sortedByColorTrue_callsGetAllSortedByColorAndReturnsList() = runTest {
        val expected = listOf(
            Host(id = 3L, nickname = "gamma", hostname = "g.example.com", color = "#00FF00"),
            Host(id = 4L, nickname = "delta", hostname = "d.example.com", color = "#FF0000")
        )
        whenever { hostDao.getAllSortedByColor() }.thenReturn(expected)

        val result = repository.getHosts(sortedByColor = true)

        assertEquals(expected, result)
        verifyBlocking(hostDao, times(1)) { getAllSortedByColor() }
        verifyBlocking(hostDao, never()) { getAll() }
    }
}
