/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.ThemeTokens

/**
 * Small preview tile used in Settings → Appearance theme picker.
 * Renders with the *given* tokens (not [com.logicalsapien.sapienterm.ui.theme.SapienTheme.current])
 * so every tile shows its own theme.
 */
@Composable
fun ThemePreviewTile(
    tokens: ThemeTokens,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(tokens.cornerMedium)
    Column(
        modifier = modifier
            .size(width = 120.dp, height = 96.dp)
            .clip(shape)
            .background(tokens.background)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) tokens.primary else tokens.surfaceBorder,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 104.dp, height = 40.dp)
                .clip(RoundedCornerShape(tokens.cornerSmall))
                .background(tokens.surface)
                .border(1.dp, tokens.surfaceBorder, RoundedCornerShape(tokens.cornerSmall))
        )
        Box(Modifier.size(4.dp))
        Text(tokens.displayName, color = tokens.textPrimary)
    }
}
