/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

data class QuickFilterChip(
    val id: String,
    val label: String
)

/**
 * Horizontal scrollable chip row for Home filter rows. Selected = primary fill,
 * unselected = surface with border, shape = 50% rounded.
 */
@Composable
fun QuickFilterChipRow(
    chips: List<QuickFilterChip>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items = chips, key = { it.id }) { chip ->
            val selected = chip.id == selectedId
            val shape = RoundedCornerShape(percent = 50)
            val containerModifier = if (selected) {
                Modifier
                    .clip(shape)
                    .background(tokens.primary)
            } else {
                Modifier
                    .clip(shape)
                    .background(tokens.surface)
                    .border(BorderStroke(1.dp, tokens.surfaceBorder), shape)
            }
            Box(
                modifier = containerModifier
                    .height(32.dp)
                    .clickable { onSelect(chip.id) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chip.label,
                    color = if (selected) tokens.textOnPrimary else tokens.textMuted,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
