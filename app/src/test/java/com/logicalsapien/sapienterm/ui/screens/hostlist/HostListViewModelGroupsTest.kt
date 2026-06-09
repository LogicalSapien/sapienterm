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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Tests for [HostListViewModel] connection group behavior: observing groups,
 * create/delete/move, and group selection used to filter the host list in the UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HostListViewModelGroupsTest {

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
    private var viewModel: HostListViewModel? = null

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HostListViewModel {
        whenever(sharedPreferences.getBoolean(PreferenceConstants.SORT_BY_COLOR, false))
            .thenReturn(false)
        return HostListViewModel(context, repository, groupRepository, dispatchers, sharedPreferences, securePasswordStorage)
            .also {
                it.healthCheckJob?.cancel()
                viewModel = it
            }
    }

    @Test
    fun groups_areCollectedFromRepositoryFlow() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        val emitted = listOf(
            ConnectionGroup(id = 1L, name = "Alpha"),
            ConnectionGroup(id = 2L, name = "Beta")
        )
        groupsFlow.value = emitted
        runCurrent()

        assertEquals(emitted, viewModel.groups.value)
    }

    @Test
    fun createGroup_callsGroupRepositoryAdd() = runTest {
        whenever { groupRepository.add(any()) }.thenReturn(1L)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.createGroup("Prod")
        runCurrent()

        val captor = argumentCaptor<ConnectionGroup>()
        verifyBlocking(groupRepository) { add(captor.capture()) }
        assertEquals("Prod", captor.firstValue.name)
    }

    @Test
    fun deleteGroup_ungroupsHostsThenDeletesGroup() = runTest {
        whenever { groupRepository.delete(any()) }.thenReturn(Unit)
        whenever { repository.saveHost(any()) }.thenAnswer { inv -> inv.arguments[0] as Host }

        val group = ConnectionGroup(id = 10L, name = "G")
        val h1 = Host(id = 1L, nickname = "a", groupId = group.id)
        val h2 = Host(id = 2L, nickname = "b", groupId = group.id)
        val h3 = Host(id = 3L, nickname = "c", groupId = null)

        hostsFlow.value = listOf(h1, h2, h3)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.deleteGroup(group)
        runCurrent()

        verifyBlocking(repository) { saveHost(eq(h1.copy(groupId = null))) }
        verifyBlocking(repository) { saveHost(eq(h2.copy(groupId = null))) }
        verifyBlocking(groupRepository) { delete(eq(group)) }
    }

    @Test
    fun deleteGroup_clearsSelectedGroupWhenItWasSelected() = runTest {
        whenever { groupRepository.delete(any()) }.thenReturn(Unit)
        whenever { repository.saveHost(any()) }.thenAnswer { inv -> inv.arguments[0] as Host }

        val group = ConnectionGroup(id = 77L, name = "X")
        hostsFlow.value = emptyList()
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectGroup(group.id)
        viewModel.deleteGroup(group)
        runCurrent()

        assertNull(viewModel.selectedGroupId.value)
    }

    @Test
    fun moveHostToGroup_updatesHostGroupIdViaSaveHost() = runTest {
        whenever { repository.saveHost(any()) }.thenAnswer { inv -> inv.arguments[0] as Host }

        val host = Host(id = 5L, nickname = "srv", groupId = null)
        hostsFlow.value = listOf(host)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.moveHostToGroup(host.id, 99L)
        runCurrent()

        verifyBlocking(repository) { saveHost(eq(host.copy(groupId = 99L))) }
    }

    @Test
    fun moveHostToGroup_ungroupsWhenGroupIdNull() = runTest {
        whenever { repository.saveHost(any()) }.thenAnswer { inv -> inv.arguments[0] as Host }

        val host = Host(id = 5L, nickname = "srv", groupId = 1L)
        hostsFlow.value = listOf(host)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.moveHostToGroup(host.id, null)
        runCurrent()

        verifyBlocking(repository) { saveHost(eq(host.copy(groupId = null))) }
    }

    @Test
    fun moveHostToGroup_noOpWhenHostIdUnknown() = runTest {
        hostsFlow.value = emptyList()
        val viewModel = createViewModel()
        runCurrent()

        viewModel.moveHostToGroup(999L, 1L)
        runCurrent()

        verifyBlocking(repository, never()) { saveHost(any()) }
    }

    @Test
    fun selectGroup_setsFilterKeyUsedByUiToShowSubsetOfHosts() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectGroup(42L)
        assertEquals(42L, viewModel.selectedGroupId.value)

        viewModel.selectGroup(null)
        assertNull(viewModel.selectedGroupId.value)
    }
}
