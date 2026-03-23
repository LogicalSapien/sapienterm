/*
 * SapienSSH: simple, powerful, open-source SSH client for Android
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

package com.logicalsapien.sapienssh.util

/**
 * Type of interactive CLI prompt detected in terminal output.
 */
enum class PromptType {
    YES_NO,
    NUMBERED,
    ENTER,
    LETTER_CHOICE
}

/**
 * A single option that can be sent to the terminal in response to a detected prompt.
 *
 * @param label Display text shown on the tappable chip (e.g. "Yes", "1", "a")
 * @param sendValue The string to send to the terminal when tapped (e.g. "y\n", "1\n")
 */
data class PromptOption(
    val label: String,
    val sendValue: String
)

/**
 * Represents a CLI prompt detected in terminal output.
 *
 * @param type The category of prompt
 * @param options The available response options
 */
data class DetectedPrompt(
    val type: PromptType,
    val options: List<PromptOption>
)

/**
 * Detects common CLI interactive prompt patterns in recent terminal output lines.
 *
 * Supported patterns:
 * - Yes/No: `(y/n)`, `(Y/N)`, `[y/N]`, `[Y/n]`, `(yes/no)`
 * - Numbered options: `1) Option`, `2) Option` or `1. Option`, `2. Option`
 * - Enter to continue: `Press Enter to continue`, `Hit enter`, `Press any key`
 * - Letter choices: `(a/b/c)`, `[a/b/c/d]`
 *
 * Detection is intentionally conservative to avoid false positives.
 */
object CliPromptDetector {

    // Yes/No patterns: (y/n), (Y/N), [y/N], [Y/n], (yes/no), [yes/no]
    private val YES_NO_PATTERN = Regex(
        """[\[\(]\s*[Yy](?:es)?\s*/\s*[Nn](?:o)?\s*[\]\)]|[\[\(]\s*[Nn](?:o)?\s*/\s*[Yy](?:es)?\s*[\]\)]"""
    )

    // Enter to continue patterns
    private val ENTER_PATTERN = Regex(
        """(?i)press\s+(?:enter|return)\s+to\s+continue|hit\s+enter|press\s+any\s+key"""
    )

    // Letter choice patterns: (a/b/c), [a/b/c/d], (a/b), etc.
    // Must have 2-8 single lowercase letters separated by /
    private val LETTER_CHOICE_PATTERN = Regex(
        """[\[\(]([a-z](?:\s*/\s*[a-z]){1,7})[\]\)]"""
    )

    // Numbered option line: "1) Something" or "1. Something" or "  1) Something"
    private val NUMBERED_OPTION_PATTERN = Regex(
        """^\s*(\d{1,2})\s*[).\]]\s+\S"""
    )

    // Prompt line that typically follows numbered options
    private val NUMBERED_PROMPT_PATTERN = Regex(
        """(?i)(?:enter|select|choose|pick|type)\s+.*(?:choice|option|number|selection)|(?:choice|option|selection)\s*[:\?]|#\?\s*$|>\s*$"""
    )

    /**
     * Scan recent terminal output lines for recognizable CLI prompt patterns.
     *
     * @param recentLines The last few lines of terminal output (typically 5-10 lines)
     * @return A [DetectedPrompt] if a pattern is found, or null if none detected
     */
    fun detect(recentLines: List<String>): DetectedPrompt? {
        if (recentLines.isEmpty()) return null

        // Only look at non-blank lines
        val lines = recentLines.map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        // Check the last line (or last 2 lines) for prompt patterns.
        // Most prompts appear at the end of output.
        val lastLine = lines.last()
        val lastTwoLines = lines.takeLast(2).joinToString(" ")

        // 1. Yes/No detection (check last line and last two lines)
        detectYesNo(lastLine)?.let { return it }
        if (lines.size >= 2) {
            detectYesNo(lastTwoLines)?.let { return it }
        }

        // 2. Enter to continue
        detectEnter(lastLine)?.let { return it }
        if (lines.size >= 2) {
            detectEnter(lastTwoLines)?.let { return it }
        }

        // 3. Letter choice (check last line and last two lines)
        detectLetterChoice(lastLine)?.let { return it }
        if (lines.size >= 2) {
            detectLetterChoice(lastTwoLines)?.let { return it }
        }

        // 4. Numbered options (need to scan multiple lines)
        detectNumberedOptions(lines)?.let { return it }

        return null
    }

    private fun detectYesNo(text: String): DetectedPrompt? {
        val match = YES_NO_PATTERN.find(text) ?: return null
        val matched = match.value.lowercase()

        // Determine default (capitalized letter = default)
        val original = match.value
        val yesLabel = if (original.contains('Y')) "Yes" else "Yes"
        val noLabel = if (original.contains('N')) "No" else "No"

        // Determine what to send based on whether it's yes/no or y/n
        val usesFullWords = matched.contains("yes") || matched.contains("no")
        val yesSend = if (usesFullWords) "yes\n" else "y\n"
        val noSend = if (usesFullWords) "no\n" else "n\n"

        return DetectedPrompt(
            type = PromptType.YES_NO,
            options = listOf(
                PromptOption(label = yesLabel, sendValue = yesSend),
                PromptOption(label = noLabel, sendValue = noSend)
            )
        )
    }

    private fun detectEnter(text: String): DetectedPrompt? {
        if (!ENTER_PATTERN.containsMatchIn(text)) return null

        return DetectedPrompt(
            type = PromptType.ENTER,
            options = listOf(
                PromptOption(label = "Enter", sendValue = "\n")
            )
        )
    }

    private fun detectLetterChoice(text: String): DetectedPrompt? {
        val match = LETTER_CHOICE_PATTERN.find(text) ?: return null
        val lettersStr = match.groupValues[1]
        val letters = lettersStr.split("/").map { it.trim() }

        // Sanity check: all should be single characters
        if (letters.any { it.length != 1 }) return null
        // Must have at least 2 choices
        if (letters.size < 2) return null

        return DetectedPrompt(
            type = PromptType.LETTER_CHOICE,
            options = letters.map { letter ->
                PromptOption(label = letter, sendValue = "$letter\n")
            }
        )
    }

    private fun detectNumberedOptions(lines: List<String>): DetectedPrompt? {
        if (lines.size < 2) return null

        // Look for consecutive numbered lines in the last 10 lines
        val lastLines = lines.takeLast(10)
        val numberedEntries = mutableListOf<Int>()
        var lastNumberedIndex = -1

        for ((index, line) in lastLines.withIndex()) {
            val match = NUMBERED_OPTION_PATTERN.find(line)
            if (match != null) {
                val number = match.groupValues[1].toIntOrNull()
                if (number != null) {
                    numberedEntries.add(number)
                    lastNumberedIndex = index
                }
            }
        }

        // Need at least 2 consecutive numbered options
        if (numberedEntries.size < 2) return null

        // The numbered options should be sequential (1,2,3... or 0,1,2...)
        val sorted = numberedEntries.sorted()
        val isSequential = sorted.zipWithNext().all { (a, b) -> b - a == 1 }
        if (!isSequential) return null

        // The last line or line after numbered options should look like a prompt,
        // or the numbered options should be the last content
        val lastLine = lastLines.last()
        val hasPromptLine = NUMBERED_PROMPT_PATTERN.containsMatchIn(lastLine)
        val numberedOptionsAreLast = lastNumberedIndex >= lastLines.size - 2

        if (!hasPromptLine && !numberedOptionsAreLast) return null

        // Cap at 9 options to keep the UI manageable
        val optionNumbers = sorted.take(9)

        return DetectedPrompt(
            type = PromptType.NUMBERED,
            options = optionNumbers.map { number ->
                PromptOption(label = "$number", sendValue = "$number\n")
            }
        )
    }
}
