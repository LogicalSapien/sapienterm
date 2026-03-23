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

package com.logicalsapien.sapienterm.ui.screens.console

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.data.entity.QuickCommand

/**
 * Height of the Quick Commands toolbar in dp.
 */
const val QUICK_COMMAND_TOOLBAR_HEIGHT_DP = 40

/**
 * A compact toolbar displaying Quick Commands as horizontally scrollable chips.
 * Placed above the keyboard in the terminal screen for quick command execution.
 *
 * @param quickCommands List of quick commands to display
 * @param isVisible Whether the command chips are visible (toggle state)
 * @param onToggleVisibility Callback to toggle chip visibility
 * @param onCommandClick Callback when a quick command chip is tapped
 * @param modifier Optional modifier
 */
@Composable
fun QuickCommandToolbar(
    quickCommands: List<QuickCommand>,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QUICK_COMMAND_TOOLBAR_HEIGHT_DP.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle visibility button
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(QUICK_COMMAND_TOOLBAR_HEIGHT_DP.dp)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "Hide quick commands" else "Show quick commands",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Animated chip row
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(150)) + expandVertically(),
                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(),
                modifier = Modifier.weight(1f)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    items(
                        items = quickCommands,
                        key = { it.id }
                    ) { command ->
                        AssistChip(
                            onClick = { onCommandClick(command.command) },
                            label = {
                                Text(
                                    text = command.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }
    }
}
