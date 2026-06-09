/*
 * ConnectBot Terminal
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.connectbot.terminal

import androidx.compose.ui.graphics.Color

/**
 * Styled transcript row for read-only views (e.g. full-session reader). Matches terminal cell colors.
 *
 * @property runs Text segments with the same styling; consecutive cells with identical display style are merged.
 * @property softWrapped If true, the next physical line continues this row without a hard newline (same rules as copy).
 * @property plainTextForFilter Same text as plain copy would use for this row (for search/filter).
 */
data class TranscriptStyledLine(
    val runs: List<TranscriptStyleRun>,
    val softWrapped: Boolean,
    val plainTextForFilter: String
)

/**
 * One styled segment of terminal output.
 */
data class TranscriptStyleRun(
    val text: String,
    val foreground: Color,
    val background: Color,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strikeThrough: Boolean
)
