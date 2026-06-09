/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

data class SegmentOption<T>(val value: T, val label: String)

/**
 * Pill-shaped segmented control. The selected segment fills with theme accent;
 * unselected segments are transparent with muted text.
 */
@Composable
fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelectedChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    val shape = RoundedCornerShape(tokens.cornerMedium)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .border(1.dp, tokens.surfaceBorder, shape)
            .background(tokens.surface)
    ) {
        options.forEach { opt ->
            val isSelected = opt.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { onSelectedChange(opt.value) }
                    .background(if (isSelected) tokens.primary else tokens.surface)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt.label,
                    color = if (isSelected) tokens.textOnPrimary else tokens.textMuted
                )
            }
        }
    }
}
