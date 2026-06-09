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

package com.logicalsapien.sapienterm.ui.screens.hosteditor

import android.content.SharedPreferences
import androidx.lifecycle.SavedStateHandle
import com.logicalsapien.sapienterm.data.CredentialRepository
import com.logicalsapien.sapienterm.data.CustomBottomBarLayoutStore
import com.logicalsapien.sapienterm.data.HostRepository
import com.logicalsapien.sapienterm.data.ProfileRepository
import com.logicalsapien.sapienterm.data.PubkeyRepository
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.util.SecurePasswordStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class HostEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var repository: HostRepository
    private lateinit var pubkeyRepository: PubkeyRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var credentialRepository: CredentialRepository
    private lateinit var prefs: SharedPreferences
    private lateinit var securePasswordStorage: SecurePasswordStorage
    private lateinit var customBottomBarLayoutStore: CustomBottomBarLayoutStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = mock()
        repository = mock()
        pubkeyRepository = mock()
        profileRepository = mock()
        credentialRepository = mock()
        prefs = mock()
        securePasswordStorage = mock()
        customBottomBarLayoutStore = mock()
        whenever(customBottomBarLayoutStore.loadAll()).thenReturn(emptyList())

        whenever(savedStateHandle.get<Long>("hostId")).thenReturn(-1L)
        whenever(prefs.getLong("defaultProfileId", 0L)).thenReturn(0L)

        kotlinx.coroutines.runBlocking {
            whenever(pubkeyRepository.getAll()).thenReturn(emptyList())
            whenever(repository.getSshHosts()).thenReturn(emptyList())
            whenever(profileRepository.getAll()).thenReturn(emptyList())
            whenever(credentialRepository.getAll()).thenReturn(emptyList())
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HostEditorViewModel = HostEditorViewModel(
        savedStateHandle,
        repository,
        pubkeyRepository,
        profileRepository,
        credentialRepository,
        prefs,
        securePasswordStorage,
        customBottomBarLayoutStore
    )

    @Test
    fun `saveHost emits saveCompleted on success`() = runTest {
        val savedHost = Host(id = 1L, nickname = "test", hostname = "host.com")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("test")
        viewModel.updateHostname("host.com")

        val completedDeferred = async { viewModel.saveCompleted.first() }
        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        completedDeferred.await()
    }

    @Test
    fun `saveHost does not emit saveCompleted on failure`() = runTest {
        whenever(repository.saveHost(any())).thenThrow(RuntimeException("DB error"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("test")
        viewModel.updateHostname("host.com")

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        assertNotNull("Error should be set", viewModel.uiState.value.error)
        assertEquals("DB error", viewModel.uiState.value.error)
    }

    @Test
    fun `saveHost preserves groupId for existing host`() = runTest {
        val existingHost = Host(
            id = 5L,
            nickname = "old",
            hostname = "host.com",
            groupId = 42L
        )
        whenever(savedStateHandle.get<Long>("hostId")).thenReturn(5L)
        whenever(repository.findHostById(5L)).thenReturn(existingHost)
        whenever(securePasswordStorage.hasPassword(5L)).thenReturn(false)
        whenever(repository.saveHost(any())).thenReturn(existingHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        val captor = argumentCaptor<Host>()
        verify(repository).saveHost(captor.capture())
        assertEquals("groupId should be preserved", 42L, captor.firstValue.groupId)
    }

    @Test
    fun `saveHost uses quickConnect as nickname when not in expanded mode`() = runTest {
        val savedHost = Host(id = 1L, nickname = "user@myhost.com:22", hostname = "myhost.com")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateQuickConnect("user@myhost.com:22")

        viewModel.saveHost(useExpandedMode = false)
        advanceUntilIdle()

        val captor = argumentCaptor<Host>()
        verify(repository).saveHost(captor.capture())
        assertEquals("user@myhost.com:22", captor.firstValue.nickname)
    }

    @Test
    fun `saveHost uses nickname field in expanded mode`() = runTest {
        val savedHost = Host(id = 1L, nickname = "My Server", hostname = "host.com")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("My Server")
        viewModel.updateHostname("host.com")
        viewModel.updateQuickConnect("user@host.com")

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        val captor = argumentCaptor<Host>()
        verify(repository).saveHost(captor.capture())
        assertEquals("My Server", captor.firstValue.nickname)
    }

    @Test
    fun `saveHost handles SSH password storage`() = runTest {
        val savedHost = Host(id = 1L, nickname = "test", hostname = "host.com", protocol = "ssh")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("test")
        viewModel.updateHostname("host.com")
        viewModel.updatePassword("secret123")

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        verify(securePasswordStorage).savePassword(1L, "secret123")
    }

    @Test
    fun `saveHost clears password for SSH when no password and no existing`() = runTest {
        val savedHost = Host(id = 1L, nickname = "test", hostname = "host.com", protocol = "ssh")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("test")
        viewModel.updateHostname("host.com")

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        verify(securePasswordStorage).deletePassword(1L)
    }

    @Test
    fun `saveHost does not handle password for local protocol`() = runTest {
        val savedHost = Host(id = 1L, nickname = "local", protocol = "local")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("local")
        viewModel.updateProtocol("local")
        viewModel.updatePassword("secret")

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        verify(securePasswordStorage, never()).savePassword(any(), any())
        verify(securePasswordStorage, never()).deletePassword(any())
    }

    @Test
    fun `saveHost strips jumpHostId for non-SSH protocol`() = runTest {
        val savedHost = Host(id = 1L, nickname = "test", protocol = "local")
        whenever(repository.saveHost(any())).thenReturn(savedHost)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateNickname("test")
        viewModel.updateProtocol("local")
        viewModel.updateJumpHostId(99L)

        viewModel.saveHost(useExpandedMode = true)
        advanceUntilIdle()

        val captor = argumentCaptor<Host>()
        verify(repository).saveHost(captor.capture())
        assertNull("jumpHostId should be null for non-SSH", captor.firstValue.jumpHostId)
    }

    @Test
    fun `updateQuickConnect parses user@host_port format`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateQuickConnect("admin@server.com:2222")

        val state = viewModel.uiState.value
        assertEquals("admin", state.username)
        assertEquals("server.com", state.hostname)
        assertEquals("2222", state.port)
    }

    @Test
    fun `updateQuickConnect handles hostname only`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateQuickConnect("server.com")

        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("server.com", state.hostname)
        assertEquals("22", state.port)
    }

    @Test
    fun `updatePort rejects non-numeric input`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updatePort("abc")

        assertEquals("22", viewModel.uiState.value.port)
    }

    @Test
    fun `loadHost populates state for existing host`() = runTest {
        val existingHost = Host(
            id = 10L,
            nickname = "My SSH",
            protocol = "ssh",
            username = "admin",
            hostname = "example.com",
            port = 2222,
            color = "blue",
            compression = true,
            stayConnected = true
        )
        whenever(savedStateHandle.get<Long>("hostId")).thenReturn(10L)
        whenever(repository.findHostById(10L)).thenReturn(existingHost)
        whenever(securePasswordStorage.hasPassword(10L)).thenReturn(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("My SSH", state.nickname)
        assertEquals("ssh", state.protocol)
        assertEquals("admin", state.username)
        assertEquals("example.com", state.hostname)
        assertEquals("2222", state.port)
        assertEquals("blue", state.color)
        assertEquals(true, state.compression)
        assertEquals(true, state.stayConnected)
        assertEquals(true, state.hasExistingPassword)
    }
}
