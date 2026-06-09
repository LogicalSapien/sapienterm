/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.components.SearchField
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

/**
 * Phase 5 settings hub: wordmark header + search field + grouped category cards.
 * Typing in the search field switches to a flat filtered list of entries
 * built by [settingsSearchIndex]; tapping a result navigates to that entry's
 * category sub-screen.
 */
@Composable
fun SettingsHubScreen(
    onOpenCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    val index = remember { settingsSearchIndex() }
    var query by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background)
            .statusBarsPadding()
    ) {
        // Header: SapienTerm wordmark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = tokens.textPrimary)) { append("Sapien") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = tokens.primary)) { append("Term") }
                },
                fontFamily = tokens.fontFamilyHeading
            )
            Text(
                text = stringResource(R.string.title_settings),
                color = tokens.textMuted,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        SearchField(
            value = query,
            onValueChange = { query = it },
            autoFocus = false,
            placeholderText = stringResource(R.string.settings_hub_search_hint)
        )

        if (query.isBlank()) {
            PalettePicker()
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SettingsCategory.values()) { category ->
                    SettingsCategoryCard(
                        icon = category.icon,
                        title = stringResource(category.titleRes),
                        description = stringResource(category.descriptionRes),
                        onClick = { onOpenCategory(category) }
                    )
                }
            }
        } else {
            SettingsSearchResults(
                query = query,
                index = index,
                onPick = onOpenCategory
            )
        }
    }
}

@Composable
private fun SettingsSearchResults(
    query: String,
    index: List<SettingsSearchEntry>,
    onPick: (SettingsCategory) -> Unit
) {
    val tokens = SapienTheme.tokens
    val lowered = query.trim().lowercase()
    // Resolve titles in composition so the filter can match on localized text.
    val titled: List<Pair<SettingsSearchEntry, String>> = index.map { entry ->
        entry to stringResource(entry.titleRes)
    }
    val matches: List<Pair<SettingsSearchEntry, String>> =
        if (lowered.isEmpty()) emptyList()
        else titled.filter { (entry, title) ->
            title.lowercase().contains(lowered) ||
                entry.keywords.any { it.lowercase().contains(lowered) }
        }
    if (matches.isEmpty()) {
        Text(
            text = stringResource(R.string.settings_hub_no_results),
            color = tokens.textMuted,
            modifier = Modifier.padding(24.dp)
        )
        return
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        items(matches) { (entry, title) ->
            Surface(
                color = tokens.surface,
                shape = RoundedCornerShape(tokens.cornerMedium),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPick(entry.category) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = title,
                        color = tokens.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(entry.category.titleRes),
                        color = tokens.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            HorizontalDivider(color = tokens.surfaceBorder.copy(alpha = 0.4f))
        }
    }
}
