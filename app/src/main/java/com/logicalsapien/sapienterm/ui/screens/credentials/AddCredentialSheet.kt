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

package com.logicalsapien.sapienterm.ui.screens.credentials

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Action type selected from the credential bottom sheet.
 */
enum class AddCredentialAction {
    GENERATE_KEY,
    IMPORT_KEY,
    ADD_PASSWORD
}

/**
 * Modal bottom sheet presenting options for adding a new credential:
 * - Generate SSH Key
 * - Import Key from File
 * - Add Password
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAction: (AddCredentialAction) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = "Add Credential",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Surface(
            onClick = {
                onAction(AddCredentialAction.GENERATE_KEY)
            }
        ) {
            ListItem(
                headlineContent = { Text("Generate SSH Key") },
                supportingContent = { Text("Create a new RSA or Ed25519 key pair") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Surface(
            onClick = {
                onAction(AddCredentialAction.IMPORT_KEY)
            }
        ) {
            ListItem(
                headlineContent = { Text("Import Key from File") },
                supportingContent = { Text("Import a PEM private key file") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Surface(
            onClick = {
                onAction(AddCredentialAction.ADD_PASSWORD)
            }
        ) {
            ListItem(
                headlineContent = { Text("Add Password") },
                supportingContent = { Text("Store a reusable password credential") },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
