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

package com.logicalsapien.sapienssh.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienssh.R
import com.logicalsapien.sapienssh.service.PromptRequest
import com.logicalsapien.sapienssh.service.PromptResponse
import com.logicalsapien.sapienssh.ui.theme.terminal

/**
 * Non-modal inline prompt that appears at the bottom of the screen,
 * similar to the old ConsoleActivity's prompt UI.
 *
 * Host key fingerprint prompts are shown as a full MD3 AlertDialog
 * with the keyboard hidden for a clean verification experience.
 */
@Composable
fun InlinePrompt(
    promptRequest: PromptRequest?,
    onResponse: (PromptResponse) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    var wasVisible by remember { mutableStateOf(false) }
    val currentOnDismissed by rememberUpdatedState(onDismiss)

    // Track when prompt becomes invisible to call onDismiss
    LaunchedEffect(promptRequest) {
        if (wasVisible && promptRequest == null) {
            currentOnDismissed()
        }
        wasVisible = promptRequest != null
    }

    // Host key fingerprint prompt is shown as a proper dialog (not inline)
    // This prevents keyboard overlap issues
    if (promptRequest is PromptRequest.HostKeyFingerprintPrompt) {
        HostKeyFingerprintDialog(
            prompt = promptRequest,
            onAccept = { onResponse(PromptResponse.BooleanResponse(true)) },
            onReject = { onResponse(PromptResponse.BooleanResponse(false)) }
        )
    }

    // Other prompts remain inline at the bottom
    val inlinePrompt = promptRequest?.takeIf { it !is PromptRequest.HostKeyFingerprintPrompt }

    AnimatedVisibility(
        visible = inlinePrompt != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        when (inlinePrompt) {
            is PromptRequest.BooleanPrompt -> {
                BooleanPromptContent(
                    instructions = inlinePrompt.instructions,
                    message = inlinePrompt.message,
                    onYes = { onResponse(PromptResponse.BooleanResponse(true)) },
                    onNo = { onResponse(PromptResponse.BooleanResponse(false)) }
                )
            }

            is PromptRequest.StringPrompt -> {
                StringPromptContent(
                    instructions = inlinePrompt.instructions,
                    hint = inlinePrompt.hint,
                    isPassword = inlinePrompt.isPassword,
                    onSubmit = { value, rememberPassword ->
                        onResponse(PromptResponse.StringResponse(value, rememberPassword))
                    },
                    onCancel = onCancel
                )
            }

            is PromptRequest.BiometricPrompt -> {
                // Biometric prompts are handled by BiometricPromptHandler in PromptDialogs.kt
                // which uses the system BiometricPrompt dialog
                BiometricPromptHandler(
                    prompt = inlinePrompt,
                    onResponse = onResponse
                )
            }

            else -> {
                /* No prompt or HostKeyFingerprintPrompt (handled above as dialog) */
            }
        }
    }
}

@Composable
private fun BooleanPromptContent(
    instructions: String?,
    message: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    val terminalColors = MaterialTheme.colorScheme.terminal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(terminalColors.overlayBackground)
            .padding(16.dp)
    ) {
        if (!instructions.isNullOrEmpty()) {
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = terminalColors.overlayText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = terminalColors.overlayText,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNo) {
                Text(stringResource(R.string.button_no), color = terminalColors.overlayText)
            }
            Button(
                onClick = onYes,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(stringResource(R.string.button_yes))
            }
        }
    }
}

@Composable
private fun StringPromptContent(
    instructions: String?,
    hint: String?,
    isPassword: Boolean,
    onSubmit: (String, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var rememberPassword by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val terminalColors = MaterialTheme.colorScheme.terminal

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(terminalColors.overlayBackground)
            .padding(16.dp)
    ) {
        if (!instructions.isNullOrEmpty()) {
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = terminalColors.overlayText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = hint?.let { { Text(it, color = terminalColors.overlayTextSecondary) } },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Unspecified
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSubmit(text, rememberPassword)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedTextColor = terminalColors.overlayText,
                unfocusedTextColor = terminalColors.overlayText,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = terminalColors.overlayText,
                focusedIndicatorColor = terminalColors.overlayTextSecondary,
                unfocusedIndicatorColor = terminalColors.overlayTextSecondary.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )

        if (isPassword) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberPassword,
                    onCheckedChange = { rememberPassword = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = terminalColors.overlayTextSecondary,
                        uncheckedColor = terminalColors.overlayTextSecondary,
                        checkmarkColor = terminalColors.overlayBackground
                    )
                )
                Text(
                    text = stringResource(R.string.remember_password),
                    style = MaterialTheme.typography.bodyMedium,
                    color = terminalColors.overlayText,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.delete_neg), color = terminalColors.overlayText)
            }
            Button(
                onClick = { onSubmit(text, rememberPassword) },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(stringResource(R.string.button_ok))
            }
        }
    }
}

/**
 * Modern Material Design 3 AlertDialog for host key fingerprint verification.
 * Hides the soft keyboard when shown so the dialog is fully visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostKeyFingerprintDialog(
    prompt: PromptRequest.HostKeyFingerprintPrompt,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val tealPrimary = Color(0xFF00897B)

    // Hide the soft keyboard when this dialog appears
    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    // Fingerprint format options
    val formats = listOf(
        stringResource(R.string.fingerprint_format_sha256) to prompt.sha256,
        stringResource(R.string.fingerprint_format_randomart) to prompt.randomArt,
        stringResource(R.string.fingerprint_format_bubblebabble) to prompt.bubblebabble,
        stringResource(R.string.fingerprint_format_md5) to prompt.md5
    )

    var selectedFormatIndex by remember { mutableIntStateOf(0) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                text = stringResource(R.string.host_key_verification_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Host info card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = prompt.hostname,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.host_key_type_and_size,
                                prompt.keyType,
                                prompt.keySize
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fingerprint format selector (MD3 dropdown)
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = formats[selectedFormatIndex].first,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(stringResource(R.string.fingerprint_format_header))
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        formats.forEachIndexed { index, (label, _) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedFormatIndex = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Fingerprint display in a card with monospace font and copy button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SelectionContainer(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 180.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = formats[selectedFormatIndex].second,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(formats[selectedFormatIndex].second)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = stringResource(R.string.fingerprint_copy_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.prompt_continue_connecting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tealPrimary
                )
            ) {
                Text(stringResource(R.string.button_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text(stringResource(R.string.button_no))
            }
        }
    )
}

@Preview
@Composable
private fun HostKeyFingerprintDialogPreview() {
    MaterialTheme {
        HostKeyFingerprintDialog(
            prompt = PromptRequest.HostKeyFingerprintPrompt(
                hostname = "example.com",
                keyType = "Ed25519",
                keySize = 256,
                serverHostKey = byteArrayOf(),
                randomArt = "+--[ED25519 256]--+\n|       .o=o..    |\n|      . oo+.o    |\n+----[SHA256]-----+",
                bubblebabble = "xebab-dybyg-fezez-baveb-duxyx",
                sha256 = "SHA256:nThbg6kXUpJWGl7E1IGOCspRomTxdCARLviKw6E5SY8",
                md5 = "16:27:ac:a5:76:28:2d:36:63:1b:56:4d:eb:df:a6:48"
            ),
            onAccept = { },
            onReject = { }
        )
    }
}
