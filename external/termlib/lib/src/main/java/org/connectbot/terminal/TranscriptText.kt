/*
 * ConnectBot Terminal
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package org.connectbot.terminal

import androidx.compose.ui.graphics.Color

/**
 * Converts terminal lines to plain text using the same newline rules as selection copy:
 * soft-wrapped visual lines are joined without a newline; hard breaks get `\n`.
 */
internal fun terminalLinesToPlainText(lines: List<TerminalLine>): String {
    if (lines.isEmpty()) return ""
    return buildString {
        for (i in lines.indices) {
            val line = lines[i]
            val lineText = buildString {
                line.cells.forEach { cell ->
                    append(cell.char)
                    cell.combiningChars.forEach { append(it) }
                }
            }.trimEnd()
            append(lineText)
            if (i < lines.lastIndex && !line.softWrapped) append('\n')
        }
    }
}

/**
 * Joins soft-wrapped [TerminalLine]s into logical rows (same boundaries as [terminalLinesToPlainText]).
 */
internal fun mergeSoftWrappedPhysicalLines(lines: List<TerminalLine>): List<TerminalLine> {
    if (lines.isEmpty()) return emptyList()
    val out = mutableListOf<TerminalLine>()
    var acc: TerminalLine? = null
    for (line in lines) {
        if (acc == null) {
            acc = line
        } else {
            val a = acc
            if (a.softWrapped) {
                acc = concatTerminalLinesHorizontally(a, line)
            } else {
                out.add(a)
                acc = line
            }
        }
    }
    acc?.let { out.add(it) }
    return out
}

private fun concatTerminalLinesHorizontally(a: TerminalLine, b: TerminalLine): TerminalLine {
    return TerminalLine(
        row = a.row,
        cells = a.cells + b.cells,
        lastModified = maxOf(a.lastModified, b.lastModified),
        semanticSegments = emptyList(),
        softWrapped = b.softWrapped
    )
}

internal fun terminalLinesToTranscriptStyledLines(lines: List<TerminalLine>): List<TranscriptStyledLine> {
    return mergeSoftWrappedPhysicalLines(lines).map { it.toTranscriptStyledLine() }
}

private fun trimTrailingEmptyCells(cells: List<TerminalLine.Cell>): List<TerminalLine.Cell> {
    if (cells.isEmpty()) return emptyList()
    var end = cells.lastIndex
    while (end >= 0) {
        val cell = cells[end]
        val isEmpty = (cell.char == '\u0000' || cell.char == ' ') && cell.combiningChars.isEmpty()
        if (!isEmpty) break
        end--
    }
    if (end < 0) return emptyList()
    if (end == cells.lastIndex) return cells
    return cells.subList(0, end + 1)
}

private data class TranscriptStyleKey(
    val fg: Color,
    val bg: Color,
    val bold: Boolean,
    val italic: Boolean,
    val underline: Boolean,
    val strike: Boolean
)

private fun TerminalLine.Cell.toTranscriptStyleKey(): TranscriptStyleKey {
    val fg = if (reverse) bgColor else fgColor
    val bg = if (reverse) fgColor else bgColor
    return TranscriptStyleKey(fg, bg, bold, italic, underline > 0, strike)
}

private fun cellDisplayText(cell: TerminalLine.Cell): String = buildString {
    val c = cell.char
    if (c == '\u0000') {
        append(' ')
    } else {
        append(c)
    }
    cell.combiningChars.forEach { append(it) }
}

private fun buildStyleRuns(cells: List<TerminalLine.Cell>): List<TranscriptStyleRun> {
    if (cells.isEmpty()) return emptyList()
    val out = mutableListOf<TranscriptStyleRun>()
    var curKey: TranscriptStyleKey? = null
    var curText = StringBuilder()
    fun flush() {
        val k = curKey ?: return
        if (curText.isEmpty()) return
        out.add(
            TranscriptStyleRun(
                text = curText.toString(),
                foreground = k.fg,
                background = k.bg,
                bold = k.bold,
                italic = k.italic,
                underline = k.underline,
                strikeThrough = k.strike
            )
        )
        curText = StringBuilder()
    }
    for (cell in cells) {
        val chunk = cellDisplayText(cell)
        if (chunk.isEmpty()) continue
        val key = cell.toTranscriptStyleKey()
        if (curKey == null) {
            curKey = key
            curText.append(chunk)
        } else if (curKey == key) {
            curText.append(chunk)
        } else {
            flush()
            curKey = key
            curText.append(chunk)
        }
    }
    flush()
    return out
}

internal fun TerminalLine.toTranscriptStyledLine(): TranscriptStyledLine {
    val plainForFilter = terminalLinesToPlainText(listOf(this))
    val trimmedCells = trimTrailingEmptyCells(cells)
    if (trimmedCells.isEmpty()) {
        return TranscriptStyledLine(emptyList(), softWrapped, plainForFilter)
    }
    val runs = buildStyleRuns(trimmedCells)
    return TranscriptStyledLine(runs, softWrapped, plainTextForFilter = plainForFilter)
}
