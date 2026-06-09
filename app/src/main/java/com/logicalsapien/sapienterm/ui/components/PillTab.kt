/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.ui.theme.SapienTheme

@Composable
fun PillTab(
    label: String,
    dotColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    val tokens = SapienTheme.tokens
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(tokens.primary)
                else Modifier.border(1.dp, tokens.surfaceBorder, shape).background(tokens.surface)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) tokens.textOnPrimary else tokens.textPrimary)
        if (onClose != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(14.dp).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text("×", color = if (selected) tokens.textOnPrimary else tokens.textMuted)
            }
        }
    }
}
