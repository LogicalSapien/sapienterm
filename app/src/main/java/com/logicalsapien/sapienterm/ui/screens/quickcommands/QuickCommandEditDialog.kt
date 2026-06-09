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

package com.logicalsapien.sapienterm.ui.screens.quickcommands

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.data.entity.QuickCommand

/**
 * Dialog for adding or editing a quick command.
 *
 * @param existingCommand If non-null, the dialog is in edit mode for this command
 * @param existingCategories List of existing categories for autocomplete suggestions
 * @param onDismiss Called when the dialog is dismissed
 * @param onSave Called with title, command, and category when the user saves
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickCommandEditDialog(
    existingCommand: QuickCommand? = null,
    existingCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (title: String, command: String, category: String?) -> Unit
) {
    val isEditMode = existingCommand != null

    var title by remember { mutableStateOf(existingCommand?.title ?: "") }
    var command by remember { mutableStateOf(existingCommand?.command ?: "") }
    var category by remember { mutableStateOf(existingCommand?.category ?: "") }

    var titleError by remember { mutableStateOf(false) }
    var commandError by remember { mutableStateOf(false) }

    val categorySuggestions = remember(existingCategories) {
        existingCategories
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }

    val fieldShape = RoundedCornerShape(12.dp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditMode) "Edit Quick Command" else "Add Quick Command"
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = false
                    },
                    label = { Text("Title") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Title is required") }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = command,
                    onValueChange = {
                        command = it
                        commandError = false
                    },
                    label = { Text("Command") },
                    isError = commandError,
                    supportingText = if (commandError) {
                        { Text("Command is required") }
                    } else {
                        null
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    leadingIcon = {
                        Text(
                            text = "$",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    minLines = 2,
                    maxLines = 5,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Tap a category below or type a new one",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (categorySuggestions.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categorySuggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { category = suggestion },
                                label = { Text(suggestion) },
                                colors = SuggestionChipDefaults.suggestionChipColors(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedCommand = command.trim()

                    titleError = trimmedTitle.isEmpty()
                    commandError = trimmedCommand.isEmpty()

                    if (!titleError && !commandError) {
                        onSave(
                            trimmedTitle,
                            trimmedCommand,
                            category.trim().takeIf { it.isNotEmpty() }
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
