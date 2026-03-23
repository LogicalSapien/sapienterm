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

package com.logicalsapien.sapienterm.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienterm.R

@Composable
fun ExportDialog(
    isExporting: Boolean,
    onExport: (includeConnections: Boolean, includeQuickCommands: Boolean, includeCredentials: Boolean, passphrase: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var includeConnections by remember { mutableStateOf(true) }
    var includeQuickCommands by remember { mutableStateOf(true) }
    var includeCredentials by remember { mutableStateOf(true) }
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }

    val nothingSelected = !includeConnections && !includeQuickCommands && !includeCredentials
    val passphraseMismatch = includeCredentials && passphrase != confirmPassphrase
    val passphraseEmpty = includeCredentials && passphrase.isEmpty()
    val exportEnabled = !nothingSelected && !passphraseMismatch && !passphraseEmpty && !isExporting

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        title = { Text(stringResource(R.string.export_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Checkboxes
                CheckboxRow(
                    label = stringResource(R.string.export_include_connections),
                    checked = includeConnections,
                    onCheckedChange = { includeConnections = it },
                    enabled = !isExporting
                )
                CheckboxRow(
                    label = stringResource(R.string.export_include_quick_commands),
                    checked = includeQuickCommands,
                    onCheckedChange = { includeQuickCommands = it },
                    enabled = !isExporting
                )
                CheckboxRow(
                    label = stringResource(R.string.export_include_credentials),
                    checked = includeCredentials,
                    onCheckedChange = { includeCredentials = it },
                    enabled = !isExporting
                )

                // Passphrase fields when credentials is checked
                if (includeCredentials) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.export_credentials_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(stringResource(R.string.export_passphrase_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = { confirmPassphrase = it },
                        label = { Text(stringResource(R.string.export_passphrase_confirm_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        enabled = !isExporting,
                        isError = passphraseMismatch && confirmPassphrase.isNotEmpty(),
                        supportingText = if (passphraseMismatch && confirmPassphrase.isNotEmpty()) {
                            { Text(stringResource(R.string.export_passphrase_mismatch)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.export_in_progress),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onExport(
                        includeConnections,
                        includeQuickCommands,
                        includeCredentials,
                        if (includeCredentials) passphrase else null
                    )
                },
                enabled = exportEnabled
            ) {
                Text(stringResource(R.string.export_button))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun CheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
