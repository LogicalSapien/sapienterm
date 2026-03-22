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

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.logicalsapien.sapienssh.data.entity.Credential
import com.logicalsapien.sapienssh.data.entity.CredentialType
import java.security.MessageDigest

/**
 * Compute a SHA-256 fingerprint from a public key string.
 * Returns a colon-separated hex fingerprint, or null if unavailable.
 */
private fun computeFingerprint(publicKey: String?): String? {
    if (publicKey.isNullOrBlank()) return null
    return try {
        // Public key string is typically "algorithm base64data comment"
        val parts = publicKey.trim().split(" ")
        val keyData = if (parts.size >= 2) {
            android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT)
        } else {
            publicKey.toByteArray(Charsets.UTF_8)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(keyData)
        "SHA256:" + android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
    } catch (_: Exception) {
        null
    }
}

/**
 * Card composable for displaying a credential.
 *
 * Shows an icon (Key for SSH_KEY, Lock for PASSWORD), the label,
 * an optional fingerprint for SSH keys, and the creation date in relative format.
 */
@Composable
fun CredentialCard(
    credential: Credential,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (credential.type) {
        CredentialType.SSH_KEY -> Icons.Default.Key
        CredentialType.PASSWORD -> Icons.Default.Lock
    }

    val fingerprint = remember(credential.publicKey) {
        if (credential.type == CredentialType.SSH_KEY) {
            computeFingerprint(credential.publicKey)
        } else {
            null
        }
    }

    val relativeTime = remember(credential.createdAt) {
        DateUtils.getRelativeTimeSpanString(
            credential.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with teal accent
            Icon(
                imageVector = icon,
                contentDescription = if (credential.type == CredentialType.SSH_KEY) "SSH Key" else "Password",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Label
                Text(
                    text = credential.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Fingerprint for SSH keys
                if (fingerprint != null) {
                    Text(
                        text = fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Created date
                Text(
                    text = relativeTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
