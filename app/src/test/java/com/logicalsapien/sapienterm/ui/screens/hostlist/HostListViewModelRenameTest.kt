/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
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

package com.logicalsapien.sapienterm.ui.screens.hostlist

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.logicalsapien.sapienterm.data.ConnectionGroupRepository
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.entity.ConnectionGroup
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.SecurePasswordStorage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Tests for HostListViewModel rename, duplicate, and delete operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HostListViewModelRenameTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(
        default = testDispatcher,
        io = testDispatcher,
        main = testDispatcher
    )
    private lateinit var context: Context
    private lateinit var repository: HostRepository
    private lateinit var groupRepository: ConnectionGroupRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var hostsFlow: MutableStateFlow<List<Host>>
    private lateinit var hostsSortedByColorFlow: MutableStateFlow<List<Host>>
    private lateinit var groupsFlow: MutableStateFlow<List<ConnectionGroup>>
    private lateinit var securePasswordStorage: SecurePasswordStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        context = mock()
        repository = mock()
        groupRepository = mock()
        sharedPreferences = mock()
        editor = mock()
        hostsFlow = MutableStateFlow(emptyList())
        hostsSortedByColorFlow = MutableStateFlow(emptyList())
        groupsFlow = MutableStateFlow(emptyList())
        securePasswordStorage = mock()

        whenever(repository.observeHosts()).thenReturn(hostsFlow)
        whenever(repository.observeHostsSortedByColor()).thenReturn(hostsSortedByColorFlow)
        whenever(groupRepository.observeAll()).thenReturn(groupsFlow)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(sharedPreferences.getBoolean(PreferenceConstants.SORT_BY_COLOR, false))
            .thenReturn(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createAndPrepareViewModel(): HostListViewModel {
        return HostListViewModel(context, repository, groupRepository, dispatchers, sharedPreferences, securePasswordStorage)
            .also { it.healthCheckJob?.cancel() }
    }

    @Test
    fun renameHost_updatesNicknameInRepository() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")
        whenever(repository.findHostById(1L)).thenReturn(host)
        whenever(repository.saveHost(any())).thenReturn(host.copy(nickname = "NewName"))
        whenever(repository.getHosts(sortedByColor = false))
            .thenReturn(listOf(host.copy(nickname = "NewName")))

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "NewName")
        runCurrent()

        val captor = argumentCaptor<Host>()
        verifyBlocking(repository) { saveHost(captor.capture()) }
        assertEquals("NewName", captor.firstValue.nickname)
    }

    @Test
    fun renameHost_trimsWhitespace() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")
        whenever(repository.findHostById(1L)).thenReturn(host)
        whenever(repository.saveHost(any())).thenReturn(host.copy(nickname = "Trimmed"))
        whenever(repository.getHosts(sortedByColor = false))
            .thenReturn(listOf(host.copy(nickname = "Trimmed")))

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "  Trimmed  ")
        runCurrent()

        val captor = argumentCaptor<Host>()
        verifyBlocking(repository) { saveHost(captor.capture()) }
        assertEquals("Trimmed", captor.firstValue.nickname)
    }

    @Test
    fun renameHost_ignoresBlankNickname() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "   ")
        runCurrent()

        verify(repository, never()).saveHost(any())
    }

    @Test
    fun renameHost_updatesUiStateWithFreshHosts() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")
        val renamedHost = host.copy(nickname = "NewName")
        whenever(repository.findHostById(1L)).thenReturn(host)
        whenever(repository.saveHost(any())).thenReturn(renamedHost)
        whenever(repository.getHosts(sortedByColor = false)).thenReturn(listOf(renamedHost))

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "NewName")
        runCurrent()

        val uiHosts = vm.uiState.value.hosts
        assertTrue("UI should contain renamed host", uiHosts.any { it.nickname == "NewName" })
    }

    @Test
    fun renameHost_setsErrorOnFailure() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")
        whenever(repository.findHostById(1L)).thenReturn(host)
        whenever(repository.saveHost(any())).thenThrow(RuntimeException("Duplicate nickname"))

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "DuplicateName")
        runCurrent()

        assertNotNull("Error should be set", vm.uiState.value.error)
        assertEquals("Duplicate nickname", vm.uiState.value.error)
    }

    @Test
    fun renameHost_fallsBackToPassedHostWhenNotFoundById() = runTest {
        val host = Host(id = 1L, nickname = "OldName", protocol = "local")
        whenever(repository.findHostById(1L)).thenReturn(null)
        whenever(repository.saveHost(any())).thenReturn(host.copy(nickname = "NewName"))
        whenever(repository.getHosts(sortedByColor = false))
            .thenReturn(listOf(host.copy(nickname = "NewName")))

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameHost(host, "NewName")
        runCurrent()

        val captor = argumentCaptor<Host>()
        verifyBlocking(repository) { saveHost(captor.capture()) }
        assertEquals("NewName", captor.firstValue.nickname)
        assertEquals(1L, captor.firstValue.id)
    }

    @Test
    fun deleteHost_callsRepository() = runTest {
        val host = Host(id = 1L, nickname = "Test", protocol = "local")

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.deleteHost(host)
        runCurrent()

        verifyBlocking(repository) { deleteHost(eq(host)) }
    }

    @Test
    fun renameGroup_updatesGroupNameViaRepository() = runTest {
        val group = ConnectionGroup(id = 1L, name = "OldGroup")

        val vm = createAndPrepareViewModel()
        runCurrent()

        vm.renameGroup(group, "NewGroup")
        runCurrent()

        val captor = argumentCaptor<ConnectionGroup>()
        verifyBlocking(groupRepository) { update(captor.capture()) }
        assertEquals("NewGroup", captor.firstValue.name)
    }
}
