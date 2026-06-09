package com.logicalsapien.sapienterm.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.logicalsapien.sapienterm.ui.theme.DensityMode
import com.logicalsapien.sapienterm.ui.theme.ThemeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {

    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        override val data = state
        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    }

    @Test
    fun `themeId flow defaults to NEO_TERMINAL when key absent`() = runTest {
        val prefs = AppearancePreferences(FakeDataStore())
        assertEquals(ThemeId.NEO_TERMINAL, prefs.themeId.first())
    }

    @Test
    fun `density flow defaults to COMFORTABLE when key absent`() = runTest {
        val prefs = AppearancePreferences(FakeDataStore())
        assertEquals(DensityMode.COMFORTABLE, prefs.density.first())
    }

    @Test
    fun `setThemeId persists and round-trips`() = runTest {
        val prefs = AppearancePreferences(FakeDataStore())
        prefs.setThemeId(ThemeId.SOFT_PRO)
        assertEquals(ThemeId.SOFT_PRO, prefs.themeId.first())
    }

    @Test
    fun `setDensity persists and round-trips`() = runTest {
        val prefs = AppearancePreferences(FakeDataStore())
        prefs.setDensity(DensityMode.COMPACT)
        assertEquals(DensityMode.COMPACT, prefs.density.first())
    }
}
