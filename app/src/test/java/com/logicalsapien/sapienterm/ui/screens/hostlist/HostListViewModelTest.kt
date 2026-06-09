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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

/**
 * Tests for HostListViewModel, focusing on sort order preference persistence.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HostListViewModelTest {

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

    private fun createViewModel(sortedByColor: Boolean = false): HostListViewModel {
        whenever(sharedPreferences.getBoolean(PreferenceConstants.SORT_BY_COLOR, false))
            .thenReturn(sortedByColor)
        return HostListViewModel(context, repository, groupRepository, dispatchers, sharedPreferences, securePasswordStorage)
            .also {
                it.healthCheckJob?.cancel()
                viewModel = it
            }
    }

    /**
     * Tests that sort order preference is loaded from SharedPreferences on initialization.
     *
     * Scenario: User previously selected "sort by color" and restarts the app.
     * Expected: The ViewModel initializes with sortedByColor=true.
     */
    @Test
    fun init_loadsSortOrderFromPreferences_whenSortedByColorTrue() = runTest {
        val viewModel = createViewModel(sortedByColor = true)
        runCurrent()

        assertTrue("sortedByColor should be true from preferences", viewModel.uiState.value.sortedByColor)
        verify(sharedPreferences).getBoolean(PreferenceConstants.SORT_BY_COLOR, false)
    }

    /**
     * Tests that sort order defaults to false when preference is not set.
     *
     * Scenario: Fresh install or preference never set.
     * Expected: The ViewModel initializes with sortedByColor=false.
     */
    @Test
    fun init_loadsSortOrderFromPreferences_whenSortedByColorFalse() = runTest {
        val viewModel = createViewModel(sortedByColor = false)
        runCurrent()

        assertFalse("sortedByColor should be false from preferences", viewModel.uiState.value.sortedByColor)
        verify(sharedPreferences).getBoolean(PreferenceConstants.SORT_BY_COLOR, false)
    }

    /**
     * Tests that toggleSortOrder persists the new value to SharedPreferences.
     *
     * Scenario: User toggles sort order from name to color.
     * Expected: The preference is saved to SharedPreferences.
     */
    @Test
    fun toggleSortOrder_persistsPreference_whenTogglingToTrue() = runTest {
        val viewModel = createViewModel(sortedByColor = false)
        runCurrent()

        viewModel.toggleSortOrder()
        runCurrent()

        assertTrue("sortedByColor should be true after toggle", viewModel.uiState.value.sortedByColor)
        verify(editor).putBoolean(PreferenceConstants.SORT_BY_COLOR, true)
        verify(editor).apply()
    }

    /**
     * Tests that toggleSortOrder persists false when toggling back to name sort.
     *
     * Scenario: User toggles sort order from color back to name.
     * Expected: The preference is saved to SharedPreferences with false.
     */
    @Test
    fun toggleSortOrder_persistsPreference_whenTogglingToFalse() = runTest {
        val viewModel = createViewModel(sortedByColor = true)
        runCurrent()

        viewModel.toggleSortOrder()
        runCurrent()

        assertFalse("sortedByColor should be false after toggle", viewModel.uiState.value.sortedByColor)
        verify(editor).putBoolean(PreferenceConstants.SORT_BY_COLOR, false)
        verify(editor).apply()
    }

    /**
     * Tests that toggling sort order switches the data source.
     *
     * Scenario: User toggles to sort by color.
     * Expected: The ViewModel observes hosts sorted by color from the repository.
     */
    @Test
    fun toggleSortOrder_switchesToColorSortedHosts() = runTest {
        val viewModel = createViewModel(sortedByColor = false)
        runCurrent()

        viewModel.toggleSortOrder()
        runCurrent()

        verify(repository).observeHostsSortedByColor()
    }

    /**
     * Tests that the ViewModel uses alphabetical sort when sortedByColor is false.
     *
     * Scenario: Default state or user prefers alphabetical sorting.
     * Expected: The ViewModel observes hosts sorted alphabetically from the repository.
     */
    @Test
    fun init_usesAlphabeticalSort_whenSortedByColorFalse() = runTest {
        createViewModel(sortedByColor = false)
        runCurrent()

        verify(repository).observeHosts()
    }

    /**
     * Tests that the ViewModel uses color sort on init when preference is true.
     *
     * Scenario: User previously selected color sort.
     * Expected: The ViewModel observes hosts sorted by color from the repository.
     */
    @Test
    fun init_usesColorSort_whenSortedByColorTrue() = runTest {
        createViewModel(sortedByColor = true)
        runCurrent()

        verify(repository).observeHostsSortedByColor()
    }

    @Test
    fun selectGroup_setsSelectedGroupId() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectGroup(42L)

        assertEquals(42L, viewModel.selectedGroupId.value)
    }

    @Test
    fun selectGroup_withNull_clearsSelection() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectGroup(42L)
        viewModel.selectGroup(null)

        assertNull(viewModel.selectedGroupId.value)
    }

    @Test
    fun createGroup_callsRepository() = runTest {
        whenever { groupRepository.add(any()) }.thenReturn(1L)
        val viewModel = createViewModel()
        runCurrent()

        viewModel.createGroup("Test Group")
        runCurrent()

        val captor = argumentCaptor<ConnectionGroup>()
        verifyBlocking(groupRepository) { add(captor.capture()) }
        assertEquals("Test Group", captor.firstValue.name)
    }

    @Test
    fun deleteGroup_clearsSelectionIfSelected() = runTest {
        whenever { groupRepository.delete(any()) }.thenReturn(Unit)
        val group = ConnectionGroup(id = 99L, name = "ToDelete")
        val viewModel = createViewModel()
        runCurrent()

        viewModel.selectGroup(group.id)
        viewModel.deleteGroup(group)
        runCurrent()

        assertNull(viewModel.selectedGroupId.value)
        verifyBlocking(groupRepository) {
            delete(eq(group))
        }
    }

    @Test
    fun groups_observedFromRepository() = runTest {
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
}
