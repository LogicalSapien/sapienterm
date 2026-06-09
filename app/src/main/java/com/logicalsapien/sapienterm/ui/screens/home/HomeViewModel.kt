/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicalsapien.sapienterm.data.prefs.AppearancePreferences
import com.logicalsapien.sapienterm.ui.theme.DensityMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appearancePreferences: AppearancePreferences
) : ViewModel() {

    private val _selectedSegment = MutableStateFlow(HomeSegment.HOSTS)
    val selectedSegment: StateFlow<HomeSegment> = _selectedSegment.asStateFlow()

    private val _hostFilterChipId = MutableStateFlow("all")
    val hostFilterChipId: StateFlow<String> = _hostFilterChipId.asStateFlow()

    fun setHostFilter(id: String) {
        _hostFilterChipId.value = id
    }

    val density: StateFlow<DensityMode> = appearancePreferences.density.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = DensityMode.DEFAULT
    )

    fun setSegment(segment: HomeSegment) {
        _selectedSegment.value = segment
    }

    fun cycleDensity() {
        viewModelScope.launch {
            val current = appearancePreferences.density.first()
            val next = when (current) {
                DensityMode.COMFORTABLE -> DensityMode.COMPACT
                DensityMode.COMPACT -> DensityMode.COMFORTABLE
            }
            appearancePreferences.setDensity(next)
        }
    }
}
