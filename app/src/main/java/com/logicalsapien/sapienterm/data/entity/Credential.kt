/*
 * SapienTerm: modern SSH client for Android
 * Copyright 2025 SapienTerm contributors
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

package com.logicalsapien.sapienterm.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Credential entity for storing reusable authentication credentials.
 *
 * Credentials allow users to save passwords or SSH key material for
 * reuse across multiple host connections. Sensitive fields (password,
 * private key, passphrase) are stored encrypted using Android Keystore
 * AES-256-GCM encryption.
 *
 * @property id Database ID of the credential
 * @property label User-visible name for this credential
 * @property type The type of credential (PASSWORD or SSH_KEY)
 * @property encryptedPassword AES-256-GCM encrypted password bytes (IV + ciphertext)
 * @property encryptedPrivateKey AES-256-GCM encrypted private key bytes (IV + ciphertext)
 * @property publicKey Public key string (not encrypted, used for display/matching)
 * @property encryptedPassphrase AES-256-GCM encrypted passphrase bytes (IV + ciphertext)
 * @property createdAt Timestamp when the credential was created
 */
@Entity(tableName = "credentials")
data class Credential(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val label: String,

    val type: CredentialType,

    @ColumnInfo(name = "encrypted_password", typeAffinity = ColumnInfo.BLOB)
    val encryptedPassword: ByteArray? = null,

    @ColumnInfo(name = "encrypted_private_key", typeAffinity = ColumnInfo.BLOB)
    val encryptedPrivateKey: ByteArray? = null,

    @ColumnInfo(name = "public_key")
    val publicKey: String? = null,

    @ColumnInfo(name = "encrypted_passphrase", typeAffinity = ColumnInfo.BLOB)
    val encryptedPassphrase: ByteArray? = null,

    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Credential

        if (id != other.id) return false
        if (label != other.label) return false
        if (type != other.type) return false
        if (encryptedPassword != null) {
            if (other.encryptedPassword == null) return false
            if (!encryptedPassword.contentEquals(other.encryptedPassword)) return false
        } else if (other.encryptedPassword != null) {
            return false
        }
        if (encryptedPrivateKey != null) {
            if (other.encryptedPrivateKey == null) return false
            if (!encryptedPrivateKey.contentEquals(other.encryptedPrivateKey)) return false
        } else if (other.encryptedPrivateKey != null) {
            return false
        }
        if (publicKey != other.publicKey) return false
        if (encryptedPassphrase != null) {
            if (other.encryptedPassphrase == null) return false
            if (!encryptedPassphrase.contentEquals(other.encryptedPassphrase)) return false
        } else if (other.encryptedPassphrase != null) {
            return false
        }
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (encryptedPassword?.contentHashCode() ?: 0)
        result = 31 * result + (encryptedPrivateKey?.contentHashCode() ?: 0)
        result = 31 * result + (publicKey?.hashCode() ?: 0)
        result = 31 * result + (encryptedPassphrase?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

/**
 * Type of credential stored.
 */
enum class CredentialType {
    /** A password credential */
    PASSWORD,

    /** An SSH key pair credential (private key + optional passphrase) */
    SSH_KEY
}
