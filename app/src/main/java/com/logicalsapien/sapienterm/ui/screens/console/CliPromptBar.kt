/*
 * SapienTerm: simple, powerful, open-source SSH client for Android
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

package com.logicalsapien.sapienterm.ui.screens.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.util.DetectedPrompt
import com.logicalsapien.sapienterm.util.PromptOption
import com.logicalsapien.sapienterm.util.PromptType
import kotlinx.coroutines.delay

/**
 * Teal accent colors matching the terminal bottom bar style.
 */
private val PromptChipColor = Color(0xFF00897B)
private val PromptChipTextColor = Color.White
private val PromptBarBackground = Color(0xFF263238)

/**
 * Auto-dismiss delay for the prompt bar, in milliseconds.
 */
private const val AUTO_DISMISS_MS = 10_000L

/**
 * A compact bar of tappable chips shown above the TerminalBottomBar when a
 * CLI interactive prompt is detected in the terminal output.
 *
 * Examples:
 * - Yes/No prompts: `[ Yes ] [ No ]`
 * - Numbered options: `[ 1 ] [ 2 ] [ 3 ]`
 * - Enter to continue: `[ Enter ]`
 * - Letter choices: `[ a ] [ b ] [ c ]`
 *
 * The bar auto-dismisses after [AUTO_DISMISS_MS] or when the user taps an option.
 *
 * @param detectedPrompt The detected prompt to display, or null to hide the bar
 * @param onOptionSelected Called when the user taps an option chip
 * @param onDismiss Called when the bar is auto-dismissed
 * @param modifier Optional modifier
 */
@Composable
fun CliPromptBar(
    detectedPrompt: DetectedPrompt?,
    onOptionSelected: (PromptOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Track the prompt for auto-dismiss
    var currentPrompt by remember { mutableStateOf(detectedPrompt) }

    // Update when external state changes
    LaunchedEffect(detectedPrompt) {
        currentPrompt = detectedPrompt
    }

    // Auto-dismiss after timeout
    LaunchedEffect(currentPrompt) {
        if (currentPrompt != null) {
            delay(AUTO_DISMISS_MS)
            currentPrompt = null
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = currentPrompt != null,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
        modifier = modifier
    ) {
        val prompt = currentPrompt ?: return@AnimatedVisibility

        Surface(
            color = PromptBarBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Label indicating the prompt type
                val label = when (prompt.type) {
                    PromptType.YES_NO -> "Confirm:"
                    PromptType.NUMBERED -> "Select:"
                    PromptType.ENTER -> "Continue:"
                    PromptType.LETTER_CHOICE -> "Choose:"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 2.dp)
                )

                // Option chips
                prompt.options.forEach { option ->
                    PromptChip(
                        option = option,
                        onClick = {
                            onOptionSelected(option)
                            currentPrompt = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * A single tappable chip for a prompt option.
 */
@Composable
private fun PromptChip(
    option: PromptOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = PromptChipColor,
        modifier = modifier.height(26.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = PromptChipTextColor
            )
        }
    }
}
