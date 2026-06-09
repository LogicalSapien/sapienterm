/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun FabPrimary(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = SapienTheme.tokens
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(tokens.cornerLarge),
        containerColor = tokens.primary,
        contentColor = tokens.textOnPrimary
    ) { content() }
}
