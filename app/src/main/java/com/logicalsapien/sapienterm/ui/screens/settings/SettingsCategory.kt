/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector
import com.logicalsapien.sapienterm.R

/**
 * High-level grouping of Settings sections used by the Phase 5 hub.
 *
 * Each category maps to one or more SettingsSection blocks rendered inside
 * SettingsScreenContent. The filter key is the string-resource name of the
 * section header used in SettingsScreenContent — matching is done by
 * resource ID, not by title text, so translations work.
 */
enum class SettingsCategory(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val sectionTitleRes: List<Int>
) {
    APPEARANCE(
        id = "appearance",
        titleRes = R.string.settings_category_appearance,
        descriptionRes = R.string.settings_category_appearance_desc,
        icon = Icons.Filled.Palette,
        sectionTitleRes = listOf(
            R.string.settings_section_display,
            R.string.settings_section_bottom_bar
        )
    ),
    TERMINAL(
        id = "terminal",
        titleRes = R.string.settings_category_terminal,
        descriptionRes = R.string.settings_category_terminal_desc,
        icon = Icons.Filled.Terminal,
        sectionTitleRes = listOf(
            R.string.settings_section_session,
            R.string.settings_section_keyboard,
            R.string.settings_section_bell
        )
    ),
    PROFILES(
        id = "profiles",
        titleRes = R.string.settings_category_profiles,
        descriptionRes = R.string.settings_category_profiles_desc,
        icon = Icons.Filled.Person,
        sectionTitleRes = listOf(
            R.string.settings_section_profiles
        )
    ),
    SECURITY(
        id = "security",
        titleRes = R.string.settings_category_security,
        descriptionRes = R.string.settings_category_security_desc,
        icon = Icons.Filled.Lock,
        sectionTitleRes = listOf(
            R.string.settings_section_security
        )
    ),
    DATA(
        id = "data",
        titleRes = R.string.settings_category_data,
        descriptionRes = R.string.settings_category_data_desc,
        icon = Icons.Filled.Backup,
        sectionTitleRes = listOf(
            R.string.settings_section_data
        )
    ),
    ABOUT(
        id = "about",
        titleRes = R.string.settings_category_about,
        descriptionRes = R.string.settings_category_about_desc,
        icon = Icons.Filled.Info,
        sectionTitleRes = listOf(
            R.string.settings_section_about
        )
    );

    companion object {
        fun fromId(id: String?): SettingsCategory? =
            values().firstOrNull { it.id == id }
    }
}
