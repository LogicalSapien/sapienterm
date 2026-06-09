/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2026 LogicalSapien
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.logicalsapien.sapienterm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LogoBackground = Color(0xFF0A1628)
private val LogoChevron = Color.White
private val LogoCursor = Color(0xFF00BCD4)

/**
 * Draws the SapienTerm >_ logo mark programmatically.
 * A rounded navy square with a white ">" chevron and a teal "_" cursor.
 */
@Composable
fun SapienTermLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cornerPx = w * 0.22f

        drawRoundRect(
            color = LogoBackground,
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )

        val strokeWidth = w * 0.08f
        val stroke = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        val chevronPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.24f, w * 0.30f)
            lineTo(w * 0.44f, w * 0.50f)
            lineTo(w * 0.24f, w * 0.70f)
        }
        drawPath(chevronPath, color = LogoChevron, style = stroke)

        drawLine(
            color = LogoCursor,
            start = Offset(w * 0.50f, h * 0.66f),
            end = Offset(w * 0.74f, h * 0.66f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Full branding row: logo mark + "SapienTerm" with styled typography.
 * "Sapien" in regular weight, "Term" in bold + primary color.
 */
@Composable
fun SapienTermBranding(
    modifier: Modifier = Modifier,
    logoSize: Dp = 32.dp,
    subtitle: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SapienTermLogoMark(size = logoSize)

        Spacer(modifier = Modifier.width(10.dp))

        val primaryColor = MaterialTheme.colorScheme.primary

        androidx.compose.foundation.layout.Column {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                        append("Sapien")
                    }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor)) {
                        append("Term")
                    }
                },
                style = MaterialTheme.typography.titleLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
