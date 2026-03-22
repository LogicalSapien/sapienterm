/*
 * SapienSSH: modern SSH client for Android
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

package com.logicalsapien.sapienssh.ui.screens.credentials

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.data.entity.Credential
import com.logicalsapien.sapienssh.data.entity.CredentialType
import com.logicalsapien.sapienssh.ui.theme.StatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialsScreen(
    modifier: Modifier = Modifier,
    viewModel: CredentialsViewModel = hiltViewModel()
) {
    val sshKeys by viewModel.sshKeys.collectAsState()
    val passwords by viewModel.passwords.collectAsState()
    val error by viewModel.error.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBottomSheet by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showKeyGenerateDialog by remember { mutableStateOf(false) }
    var credentialToDelete by remember { mutableStateOf<Credential?>(null) }
    var credentialToView by remember { mutableStateOf<Credential?>(null) }
    var showImportLabelDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

    val sheetState = rememberModalBottomSheetState()

    // Show snackbar for errors
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // File picker for importing keys
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            pendingImportUri = it
            showImportLabelDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Credentials") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Credential")
            }
        },
        modifier = modifier
    ) { padding ->
        val isEmpty = sshKeys.isEmpty() && passwords.isEmpty()

        if (isEmpty) {
            // Global empty state
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No credentials yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add passwords or SSH keys for quick authentication",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showBottomSheet = true }) {
                        Text("Add Credential")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SSH Keys section
                if (sshKeys.isNotEmpty()) {
                    item(key = "ssh_keys_header") {
                        SectionHeader(title = "SSH Keys")
                    }

                    items(
                        items = sshKeys,
                        key = { "ssh_${it.id}" }
                    ) { credential ->
                        SwipeToDismissCredentialItem(
                            credential = credential,
                            onClick = { credentialToView = credential },
                            onDelete = { credentialToDelete = credential }
                        )
                    }
                }

                // Passwords section
                if (passwords.isNotEmpty()) {
                    item(key = "passwords_header") {
                        if (sshKeys.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        SectionHeader(title = "Passwords")
                    }

                    items(
                        items = passwords,
                        key = { "pwd_${it.id}" }
                    ) { credential ->
                        SwipeToDismissCredentialItem(
                            credential = credential,
                            onClick = { credentialToView = credential },
                            onDelete = { credentialToDelete = credential }
                        )
                    }
                }
            }
        }
    }

    // Bottom sheet
    if (showBottomSheet) {
        AddCredentialSheet(
            sheetState = sheetState,
            onDismiss = { showBottomSheet = false },
            onAction = { action ->
                showBottomSheet = false
                when (action) {
                    AddCredentialAction.GENERATE_KEY -> showKeyGenerateDialog = true
                    AddCredentialAction.IMPORT_KEY -> {
                        filePickerLauncher.launch(KeyImportHelper.ACCEPTED_MIME_TYPES)
                    }
                    AddCredentialAction.ADD_PASSWORD -> showPasswordDialog = true
                }
            }
        )
    }

    // Add password dialog
    if (showPasswordDialog) {
        AddPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onSave = { label, password ->
                viewModel.addPassword(label, password)
                showPasswordDialog = false
            }
        )
    }

    // Key generate dialog
    if (showKeyGenerateDialog) {
        KeyGenerateDialog(
            onDismiss = { showKeyGenerateDialog = false },
            onGenerate = { label, algorithm, bits, passphrase ->
                viewModel.generateKey(label, algorithm, bits, passphrase)
                showKeyGenerateDialog = false
            }
        )
    }

    // Import label dialog
    if (showImportLabelDialog && pendingImportUri != null) {
        ImportKeyLabelDialog(
            suggestedLabel = KeyImportHelper.suggestLabel(pendingImportUri!!),
            onDismiss = {
                showImportLabelDialog = false
                pendingImportUri = null
            },
            onConfirm = { label, passphrase ->
                val uri = pendingImportUri!!
                val parsed = KeyImportHelper.parseKeyFile(context, uri)
                if (parsed != null) {
                    viewModel.importKeyFromBytes(
                        label = label,
                        privateKeyBytes = parsed.privateKeyBytes,
                        publicKey = "${parsed.keyType} key",
                        passphrase = passphrase
                    )
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Failed to parse key file")
                    }
                }
                showImportLabelDialog = false
                pendingImportUri = null
            }
        )
    }

    // Delete confirmation dialog
    credentialToDelete?.let { cred ->
        AlertDialog(
            onDismissRequest = { credentialToDelete = null },
            title = { Text("Delete Credential") },
            text = {
                Text("Delete \"${cred.label}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCredential(cred)
                        credentialToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { credentialToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Credential detail dialog
    credentialToView?.let { cred ->
        CredentialDetailDialog(
            credential = cred,
            viewModel = viewModel,
            onDismiss = { credentialToView = null }
        )
    }

    // Generating progress dialog
    if (isGenerating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Generating Key") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please wait...")
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissCredentialItem(
    credential: Credential,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection

            val backgroundColor by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> StatusRed.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "swipe_bg_color"
            )

            val iconTint by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> StatusRed
                    else -> MaterialTheme.colorScheme.onSurface
                },
                label = "swipe_icon_tint"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = iconTint
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint
                        )
                    }
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        CredentialCard(
            credential = credential,
            onClick = onClick
        )
    }
}

/**
 * Dialog for viewing credential details.
 * Passwords are masked with a copy button and visibility toggle.
 * SSH keys show type and fingerprint information.
 */
@Composable
private fun CredentialDetailDialog(
    credential: Credential,
    viewModel: CredentialsViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    var decryptedPassword by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Decrypt password on first show if this is a password credential
    LaunchedEffect(credential.id) {
        if (credential.type == CredentialType.PASSWORD) {
            decryptedPassword = viewModel.getDecryptedPassword(credential.id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(credential.label) },
        text = {
            Column {
                // Type
                Text(
                    text = if (credential.type == CredentialType.SSH_KEY) "SSH Key" else "Password",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (credential.type) {
                    CredentialType.PASSWORD -> {
                        val displayPassword = decryptedPassword ?: "..."
                        OutlinedTextField(
                            value = displayPassword,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Password") },
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (passwordVisible) "Hide" else "Show"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = {
                                decryptedPassword?.let {
                                    clipboardManager.setText(AnnotatedString(it))
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text("Copy Password")
                        }
                    }

                    CredentialType.SSH_KEY -> {
                        credential.publicKey?.let { pubKey ->
                            Text(
                                text = "Public Key",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pubKey,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(pubKey))
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Copy Public Key")
                            }
                        }

                        if (credential.encryptedPassphrase != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This key has a passphrase",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog asking for a label (and optional passphrase) when importing a key file.
 */
@Composable
private fun ImportKeyLabelDialog(
    suggestedLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (label: String, passphrase: String?) -> Unit
) {
    var label by remember { mutableStateOf(suggestedLabel) }
    var passphrase by remember { mutableStateOf("") }
    var passphraseVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import SSH Key") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase (if encrypted)") },
                    singleLine = true,
                    visualTransformation = if (passphraseVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                            Icon(
                                imageVector = if (passphraseVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (passphraseVisible) "Hide" else "Show"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(label.trim(), passphrase.takeIf { it.isNotBlank() })
                },
                enabled = label.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
