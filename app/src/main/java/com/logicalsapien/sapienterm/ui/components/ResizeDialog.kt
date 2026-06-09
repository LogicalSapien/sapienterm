/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.service.TerminalBridge

@Composable
fun ResizeDialog(
    currentBridge: TerminalBridge,
    isForced: Boolean,
    onDismiss: () -> Unit,
    onResize: (Int, Int) -> Unit,
    onDisableForceSize: () -> Unit
) {
    val dimensions = currentBridge.terminalEmulator.dimensions

    var widthText by remember { mutableStateOf(dimensions.columns.toString()) }
    var heightText by remember { mutableStateOf(dimensions.rows.toString()) }
    var widthError by remember { mutableStateOf(false) }
    var heightError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.console_menu_resize)) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Font size slider (visual only — doesn't affect terminal cols/rows when fixed size is active)
                val currentFontSize by currentBridge.fontSizeFlow.collectAsState()
                Column {
                    Text(
                        text = "Font Size",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = currentFontSize,
                            onValueChange = { newSize ->
                                val rounded = Math.round(newSize).toFloat()
                                if (rounded != currentFontSize) {
                                    if (rounded > currentFontSize) currentBridge.increaseFontSize()
                                    else currentBridge.decreaseFontSize()
                                }
                            },
                            valueRange = 8f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${currentFontSize.toInt()} sp",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (isForced) {
                        Text(
                            text = "With fixed size, font changes only affect visual scale — terminal dimensions stay locked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        widthText = it
                        widthError = it.toIntOrNull() == null || it.toInt() <= 0
                    },
                    label = { Text(stringResource(R.string.resize_width_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = widthError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it
                        heightError = it.toIntOrNull() == null || it.toInt() <= 0
                    },
                    label = { Text(stringResource(R.string.resize_height_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = heightError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isForced) {
                    TextButton(
                        onClick = {
                            onDisableForceSize()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.resize_disable_force_size))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val width = widthText.toIntOrNull()
                    val height = heightText.toIntOrNull()

                    if (width != null && width > 0 && height != null && height > 0) {
                        onResize(width, height)
                        onDismiss()
                    }
                },
                enabled = !widthError && !heightError &&
                    widthText.isNotEmpty() && heightText.isNotEmpty()
            ) {
                Text(stringResource(R.string.button_resize))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}
