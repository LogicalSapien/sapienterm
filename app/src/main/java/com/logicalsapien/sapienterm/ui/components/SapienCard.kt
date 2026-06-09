/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

/**
 * Standard rounded container that switches between bordered (Neo-Terminal, Quiet Luxury)
 * and soft-shadow (Soft Pro) styles based on theme tokens.
 */
@Composable
fun SapienCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = SapienTheme.tokens
    val shape = RoundedCornerShape(tokens.cornerMedium)
    val container = if (tokens.useShadowElevation) {
        modifier
            .shadow(elevation = 4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(tokens.surface)
    } else {
        modifier
            .clip(shape)
            .background(tokens.surface)
            .border(BorderStroke(1.dp, tokens.surfaceBorder), shape)
    }
    Surface(modifier = container, color = tokens.surface) {
        content()
    }
}
