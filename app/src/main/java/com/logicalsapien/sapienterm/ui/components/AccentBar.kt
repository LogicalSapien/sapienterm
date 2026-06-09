/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Vertical color bar used as a left-edge accent on host/command/key cards. */
@Composable
fun AccentBar(color: Color, modifier: Modifier = Modifier, width: Dp = 5.dp) {
    Box(modifier = modifier.fillMaxHeight().width(width).background(color))
}
