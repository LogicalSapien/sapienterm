/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.logicalsapien.sapienterm.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.logicalsapien.sapienterm.util.PreferenceConstants
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomBottomBarLayoutStore @Inject constructor(
    private val prefs: SharedPreferences
) {
    fun loadAll(): List<CustomBottomBarLayout> = prefs.getString(PreferenceConstants.CUSTOM_BOTTOM_BAR_LAYOUTS, null)
        .orEmpty()
        .parseCustomBottomBarLayouts()

    fun getById(id: String): CustomBottomBarLayout? = loadAll().find { it.id == id }

    fun save(layout: CustomBottomBarLayout) {
        val list = loadAll().filter { it.id != layout.id } + layout
        persist(list)
    }

    fun delete(id: String) {
        persist(loadAll().filter { it.id != id })
    }

    fun createNew(name: String, actionIds: List<String>): CustomBottomBarLayout {
        val layout = CustomBottomBarLayout(
            id = "c_" + UUID.randomUUID().toString().replace("-", "").take(24),
            name = name.trim().ifBlank { "Custom" },
            actionIds = actionIds.take(CustomBottomBarLayout.MAX_ACTIONS)
        )
        save(layout)
        return layout
    }

    private fun persist(list: List<CustomBottomBarLayout>) {
        prefs.edit { putString(PreferenceConstants.CUSTOM_BOTTOM_BAR_LAYOUTS, list.toJsonString()) }
    }
}
