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

package com.logicalsapien.sapienterm.ui.screens.colors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import com.logicalsapien.sapienterm.data.ColorSchemePresets
import com.logicalsapien.sapienterm.data.ColorSchemeRepository
import com.logicalsapien.sapienterm.data.entity.ColorScheme
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.clearInvocations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ColorSchemeManagerViewModelTest {

    private lateinit var repository: ColorSchemeRepository
    private lateinit var viewModel: ColorSchemeManagerViewModel

    private val builtInScheme = ColorScheme(id = -1L, name = "Default", isBuiltIn = true)
    private val customScheme = ColorScheme(id = 100L, name = "Custom", isBuiltIn = false)
    private val fallbackFg = ColorSchemePresets.default.defaultFg
    private val fallbackBg = ColorSchemePresets.default.defaultBg

    @Before
    fun setUp() {
        repository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun stubSchemes(
        schemes: List<ColorScheme>,
        colorsById: Map<Long, IntArray> = schemes.associate { it.id to ColorSchemePresets.default.colors },
        defaultsById: Map<Long, Pair<Int, Int>> = schemes.associate {
            it.id to Pair(fallbackFg, fallbackBg)
        }
    ) {
        runBlocking {
            whenever(repository.getAllSchemes()).thenReturn(schemes)
            schemes.forEach { scheme ->
                val colors = colorsById[scheme.id] ?: ColorSchemePresets.default.colors
                val defaults = defaultsById[scheme.id] ?: Pair(fallbackFg, fallbackBg)
                whenever(repository.getSchemeColors(scheme.id)).thenReturn(colors)
                whenever(repository.getSchemeDefaults(scheme.id)).thenReturn(defaults)
            }
        }
    }

    @Test
    fun `loadSchemes loads all schemes and populates preview colors and defaults`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val solarizedColors = ColorSchemePresets.solarizedDark.colors
        val solarizedDefaults = Pair(
            ColorSchemePresets.solarizedDark.defaultFg,
            ColorSchemePresets.solarizedDark.defaultBg
        )
        val solarized = ColorScheme(id = -2L, name = "Solarized", isBuiltIn = true)
        val schemes = listOf(builtInScheme, solarized, customScheme)
        stubSchemes(
            schemes = schemes,
            colorsById = mapOf(
                builtInScheme.id to ColorSchemePresets.default.colors,
                solarized.id to solarizedColors,
                customScheme.id to ColorSchemePresets.default.colors
            ),
            defaultsById = mapOf(
                builtInScheme.id to Pair(fallbackFg, fallbackBg),
                solarized.id to solarizedDefaults,
                customScheme.id to Pair(3, 4)
            )
        )

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.schemes).isEqualTo(schemes)
        assertThat(state.schemePreviewColors.keys).containsExactlyInAnyOrderElementsOf(schemes.map { it.id })
        assertThat(state.schemePreviewDefaults.keys).containsExactlyInAnyOrderElementsOf(schemes.map { it.id })
        assertThat(state.schemePreviewColors[builtInScheme.id]?.contentEquals(ColorSchemePresets.default.colors))
            .isTrue()
        assertThat(state.schemePreviewColors[solarized.id]?.contentEquals(solarizedColors)).isTrue()
        assertThat(state.schemePreviewColors[customScheme.id]?.contentEquals(ColorSchemePresets.default.colors))
            .isTrue()
        assertThat(state.schemePreviewDefaults[builtInScheme.id]).isEqualTo(Pair(fallbackFg, fallbackBg))
        assertThat(state.schemePreviewDefaults[solarized.id]).isEqualTo(solarizedDefaults)
        assertThat(state.schemePreviewDefaults[customScheme.id]).isEqualTo(Pair(3, 4))
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `getSchemeColors failure uses preset default palette for that scheme`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val schemes = listOf(builtInScheme, customScheme)
        runBlocking {
            whenever(repository.getAllSchemes()).thenReturn(schemes)
            whenever(repository.getSchemeColors(builtInScheme.id))
                .thenThrow(RuntimeException("missing palette"))
            whenever(repository.getSchemeColors(customScheme.id))
                .thenReturn(ColorSchemePresets.solarizedDark.colors)
            whenever(repository.getSchemeDefaults(builtInScheme.id))
                .thenReturn(Pair(fallbackFg, fallbackBg))
            whenever(repository.getSchemeDefaults(customScheme.id))
                .thenReturn(Pair(1, 2))
        }

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        val preview = viewModel.uiState.value.schemePreviewColors
        assertThat(preview[builtInScheme.id]?.contentEquals(ColorSchemePresets.default.colors)).isTrue()
        assertThat(preview[customScheme.id]?.contentEquals(ColorSchemePresets.solarizedDark.colors)).isTrue()
    }

    @Test
    fun `getSchemeDefaults failure uses preset default fg and bg for that scheme`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        val schemes = listOf(builtInScheme)
        runBlocking {
            whenever(repository.getAllSchemes()).thenReturn(schemes)
            whenever(repository.getSchemeColors(builtInScheme.id))
                .thenReturn(ColorSchemePresets.default.colors)
            whenever(repository.getSchemeDefaults(builtInScheme.id))
                .thenThrow(RuntimeException("defaults missing"))
        }

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.schemePreviewDefaults[builtInScheme.id])
            .isEqualTo(Pair(ColorSchemePresets.default.defaultFg, ColorSchemePresets.default.defaultBg))
    }

    @Test
    fun `loadSchemes failure clears preview maps and sets error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        runBlocking {
            whenever(repository.getAllSchemes()).thenThrow(RuntimeException("db unavailable"))
        }

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.schemePreviewColors).isEmpty()
        assertThat(state.schemePreviewDefaults).isEmpty()
        assertThat(state.error).isEqualTo("db unavailable")
        assertThat(state.isLoading).isFalse()
    }

    @Test
    fun `createNewScheme calls repository and closes dialog on success`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        runBlocking {
            whenever(repository.schemeNameExists("New")).thenReturn(false)
        }

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.showNewSchemeDialog()
        assertThat(viewModel.uiState.value.showNewSchemeDialog).isTrue()

        viewModel.createNewScheme("New", "desc", builtInScheme.id)
        advanceUntilIdle()

        verify(repository).createCustomScheme("New", "desc", builtInScheme.id)
        assertThat(viewModel.uiState.value.showNewSchemeDialog).isFalse()
        verify(repository, times(2)).getAllSchemes()
    }

    @Test
    fun `createNewScheme sets dialog error when name is blank`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.createNewScheme("   ", "d", builtInScheme.id)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.dialogError).isEqualTo("Scheme name cannot be empty")
        verify(repository, never()).createCustomScheme(any(), any(), any())
    }

    @Test
    fun `createNewScheme sets dialog error when name already exists`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        runBlocking { whenever(repository.schemeNameExists("Dup")).thenReturn(true) }

        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.createNewScheme("Dup", "", builtInScheme.id)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.dialogError).isEqualTo("A scheme with this name already exists")
        verify(repository, never()).createCustomScheme(any(), any(), any())
    }

    @Test
    fun `deleteScheme removes custom scheme clears selection and closes dialog`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme, customScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.selectScheme(customScheme.id)
        viewModel.showDeleteDialog()
        assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()

        viewModel.deleteScheme(customScheme.id)
        advanceUntilIdle()

        verify(repository).deleteCustomScheme(customScheme.id)
        assertThat(viewModel.uiState.value.selectedSchemeId).isNull()
        assertThat(viewModel.uiState.value.showDeleteDialog).isFalse()
        verify(repository, times(2)).getAllSchemes()
    }

    @Test
    fun `deleteScheme does not delete built-in scheme`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme, customScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteScheme(builtInScheme.id)
        advanceUntilIdle()

        verify(repository, never()).deleteCustomScheme(builtInScheme.id)
        assertThat(viewModel.uiState.value.dialogError).isEqualTo("Cannot delete built-in schemes")
    }

    @Test
    fun `refresh reloads schemes from repository`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        clearInvocations(repository)
        viewModel.refresh()
        advanceUntilIdle()

        verify(repository, times(1)).getAllSchemes()
    }

    @Test
    fun `selectScheme toggles selection`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme, customScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.selectScheme(customScheme.id)
        assertThat(viewModel.uiState.value.selectedSchemeId).isEqualTo(customScheme.id)

        viewModel.selectScheme(customScheme.id)
        assertThat(viewModel.uiState.value.selectedSchemeId).isNull()
    }

    @Test
    fun `new scheme dialog show and hide`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.showNewSchemeDialog()
        assertThat(viewModel.uiState.value.showNewSchemeDialog).isTrue()
        assertThat(viewModel.uiState.value.dialogError).isNull()

        viewModel.hideNewSchemeDialog()
        assertThat(viewModel.uiState.value.showNewSchemeDialog).isFalse()
        assertThat(viewModel.uiState.value.dialogError).isNull()
    }

    @Test
    fun `delete dialog show and hide`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.showDeleteDialog()
        assertThat(viewModel.uiState.value.showDeleteDialog).isTrue()
        assertThat(viewModel.uiState.value.dialogError).isNull()

        viewModel.hideDeleteDialog()
        assertThat(viewModel.uiState.value.showDeleteDialog).isFalse()
        assertThat(viewModel.uiState.value.dialogError).isNull()
    }

    @Test
    fun `showNewSchemeDialog clears previous dialog error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        stubSchemes(listOf(builtInScheme))
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()

        viewModel.createNewScheme("", "d", builtInScheme.id)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.dialogError).isNotNull()

        viewModel.showNewSchemeDialog()
        assertThat(viewModel.uiState.value.dialogError).isNull()
    }

    @Test
    fun `clearError removes load error`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        runBlocking {
            whenever(repository.getAllSchemes()).thenThrow(RuntimeException("fail"))
        }
        viewModel = ColorSchemeManagerViewModel(repository)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isNotNull()

        viewModel.clearError()
        assertThat(viewModel.uiState.value.error).isNull()
    }
}
