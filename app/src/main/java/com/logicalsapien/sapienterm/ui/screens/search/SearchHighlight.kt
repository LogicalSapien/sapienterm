/*
 * Copyright 2026 LogicalSapien — Apache 2.0
 */
package com.logicalsapien.sapienterm.ui.screens.search

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Return [text] as an [AnnotatedString] where every case-insensitive match of
 * [query] is styled with [accent] + semibold. Empty or blank query returns the
 * plain text unchanged.
 */
fun highlightedAnnotatedString(text: String, query: String, accent: Color): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty() || text.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        val haystack = text.lowercase()
        val lowerNeedle = needle.lowercase()
        while (cursor < text.length) {
            val idx = haystack.indexOf(lowerNeedle, cursor)
            if (idx < 0) {
                append(text.substring(cursor))
                break
            }
            if (idx > cursor) append(text.substring(cursor, idx))
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
                append(text.substring(idx, idx + needle.length))
            }
            cursor = idx + needle.length
        }
    }
}
