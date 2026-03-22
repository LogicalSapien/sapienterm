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

import android.content.Context
import android.net.Uri
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Result of parsing an SSH key file.
 */
data class ParsedKeyFile(
    val privateKeyBytes: ByteArray,
    val keyType: String,
    val isEncrypted: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ParsedKeyFile
        if (!privateKeyBytes.contentEquals(other.privateKeyBytes)) return false
        if (keyType != other.keyType) return false
        if (isEncrypted != other.isEncrypted) return false
        return true
    }

    override fun hashCode(): Int {
        var result = privateKeyBytes.contentHashCode()
        result = 31 * result + keyType.hashCode()
        result = 31 * result + isEncrypted.hashCode()
        return result
    }
}

/**
 * Helper object for importing SSH key files.
 *
 * Reads file content from a URI, identifies PEM-formatted keys by their headers,
 * and extracts the raw key bytes. Handles RSA, DSA, EC, and OpenSSH private keys.
 */
object KeyImportHelper {

    /** MIME types accepted for the file picker. */
    val ACCEPTED_MIME_TYPES = arrayOf("application/octet-stream", "*/*")

    /**
     * Read and parse an SSH private key file from a content URI.
     *
     * @param context Android context for content resolver access
     * @param uri The URI of the key file to import
     * @return ParsedKeyFile containing the raw key bytes and metadata, or null on failure
     */
    fun parseKeyFile(context: Context, uri: Uri): ParsedKeyFile? {
        return try {
            val content = readFileContent(context, uri) ?: return null
            parseKeyContent(content)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse key file from URI: $uri")
            null
        }
    }

    /**
     * Read file content from a content URI.
     */
    private fun readFileContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).readText()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to read file content")
            null
        }
    }

    /**
     * Parse key content from a string.
     *
     * Supports PEM-formatted keys with the following headers:
     * - -----BEGIN RSA PRIVATE KEY-----
     * - -----BEGIN DSA PRIVATE KEY-----
     * - -----BEGIN EC PRIVATE KEY-----
     * - -----BEGIN OPENSSH PRIVATE KEY-----
     * - -----BEGIN PRIVATE KEY----- (PKCS#8)
     * - -----BEGIN ENCRYPTED PRIVATE KEY-----
     *
     * @param content The raw text content of the key file
     * @return ParsedKeyFile or null if the content is not a valid key
     */
    fun parseKeyContent(content: String): ParsedKeyFile? {
        val trimmed = content.trim()

        // Check for PEM headers
        if (!trimmed.contains("-----BEGIN")) {
            // Not a PEM file -- try treating entire content as raw key bytes
            return ParsedKeyFile(
                privateKeyBytes = trimmed.toByteArray(Charsets.UTF_8),
                keyType = "Unknown",
                isEncrypted = false
            )
        }

        val keyType = when {
            trimmed.contains("BEGIN RSA PRIVATE KEY") -> "RSA"
            trimmed.contains("BEGIN DSA PRIVATE KEY") -> "DSA"
            trimmed.contains("BEGIN EC PRIVATE KEY") -> "EC"
            trimmed.contains("BEGIN OPENSSH PRIVATE KEY") -> "OpenSSH"
            trimmed.contains("BEGIN PRIVATE KEY") -> "PKCS8"
            trimmed.contains("BEGIN ENCRYPTED PRIVATE KEY") -> "PKCS8-Encrypted"
            else -> "Unknown"
        }

        val isEncrypted = trimmed.contains("ENCRYPTED") ||
            trimmed.contains("Proc-Type: 4,ENCRYPTED")

        // Store the full PEM content as the private key bytes
        // The actual PEM parsing/decoding will be handled by sshlib when the key is used
        return ParsedKeyFile(
            privateKeyBytes = trimmed.toByteArray(Charsets.UTF_8),
            keyType = keyType,
            isEncrypted = isEncrypted
        )
    }

    /**
     * Extract a suggested label from a URI filename.
     *
     * @param uri The URI of the imported file
     * @return A suggested label derived from the filename
     */
    fun suggestLabel(uri: Uri): String {
        val path = uri.lastPathSegment ?: return "Imported Key"
        val filename = path.substringAfterLast('/')
        // Remove common key file extensions
        return filename
            .removeSuffix(".pem")
            .removeSuffix(".key")
            .removeSuffix(".pub")
            .removeSuffix(".ppk")
            .ifBlank { "Imported Key" }
    }
}
