/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.logicalsapien.sapienterm.di.AppearanceDataStore
import com.logicalsapien.sapienterm.ui.theme.DensityMode
import com.logicalsapien.sapienterm.ui.theme.ThemeId
import com.logicalsapien.sapienterm.util.PreferenceConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearancePreferences @Inject constructor(
    @AppearanceDataStore private val dataStore: DataStore<Preferences>
) {
    private val themeKey = stringPreferencesKey(PreferenceConstants.APPEARANCE_THEME_ID)
    private val densityKey = stringPreferencesKey(PreferenceConstants.APPEARANCE_DENSITY)

    val themeId: Flow<ThemeId> = dataStore.data.map { prefs ->
        ThemeId.fromStorageString(prefs[themeKey])
    }

    val density: Flow<DensityMode> = dataStore.data.map { prefs ->
        DensityMode.fromStorageString(prefs[densityKey])
    }

    suspend fun setThemeId(id: ThemeId) {
        dataStore.edit { it[themeKey] = id.storageString }
    }

    suspend fun setDensity(mode: DensityMode) {
        dataStore.edit { it[densityKey] = mode.storageString }
    }
}
