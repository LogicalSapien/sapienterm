/*
 * ConnectBot Terminal
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package org.connectbot.terminal

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptTextTest {

    private val fg = Color.White
    private val bg = Color.Black

    private fun line(text: String, softWrapped: Boolean = false): TerminalLine {
        val cells = text.map { ch ->
            TerminalLine.Cell(char = ch, fgColor = fg, bgColor = bg)
        }
        return TerminalLine(row = 0, cells = cells, softWrapped = softWrapped)
    }

    @Test
    fun empty() {
        assertEquals("", terminalLinesToPlainText(emptyList()))
    }

    @Test
    fun softWrappedJoinsWithoutNewline() {
        val lines = listOf(
            line("hello ", softWrapped = true),
            line("world")
        )
        assertEquals("hello world", terminalLinesToPlainText(lines))
    }

    @Test
    fun hardBreakInsertsNewline() {
        val lines = listOf(
            line("a"),
            line("b", softWrapped = false)
        )
        assertEquals("a\nb", terminalLinesToPlainText(lines))
    }
}
