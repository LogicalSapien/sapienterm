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

package com.logicalsapien.sapienterm.data.export

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cryptographic utilities for export file encryption using PBKDF2 + AES-256-GCM.
 *
 * The encrypted output format is:
 * [salt (16 bytes)][IV (12 bytes)][ciphertext with GCM tag]
 *
 * The passphrase is stretched via PBKDF2WithHmacSHA256 with 100,000 iterations
 * to derive a 256-bit AES key. AES-256-GCM provides authenticated encryption
 * which protects against both eavesdropping and tampering.
 */
object CryptoUtils {

    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val GCM_TAG_LENGTH = 128
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val SECRET_KEY_ALGORITHM = "AES"

    /**
     * Encrypt data with a user-provided passphrase.
     *
     * Steps:
     * 1. Generate random 16-byte salt
     * 2. Derive 256-bit key via PBKDF2WithHmacSHA256 (100,000 iterations)
     * 3. Generate random 12-byte IV
     * 4. Encrypt with AES-256-GCM
     * 5. Return: salt (16) + IV (12) + ciphertext (includes GCM tag)
     *
     * @param data The plaintext data to encrypt
     * @param passphrase The user's passphrase for key derivation
     * @return The encrypted output as salt + IV + ciphertext
     * @throws ExportCryptoException if encryption fails
     */
    fun encrypt(data: ByteArray, passphrase: String): ByteArray {
        try {
            val random = SecureRandom()

            // 1. Generate random salt
            val salt = ByteArray(SALT_LENGTH)
            random.nextBytes(salt)

            // 2. Derive key from passphrase
            val key = deriveKey(passphrase, salt)

            // 3. Generate random IV
            val iv = ByteArray(IV_LENGTH)
            random.nextBytes(iv)

            // 4. Encrypt with AES-256-GCM
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val secretKey = SecretKeySpec(key, SECRET_KEY_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val ciphertext = cipher.doFinal(data)

            // 5. Combine: salt + IV + ciphertext
            val result = ByteArray(SALT_LENGTH + IV_LENGTH + ciphertext.size)
            System.arraycopy(salt, 0, result, 0, SALT_LENGTH)
            System.arraycopy(iv, 0, result, SALT_LENGTH, IV_LENGTH)
            System.arraycopy(ciphertext, 0, result, SALT_LENGTH + IV_LENGTH, ciphertext.size)

            return result
        } catch (e: Exception) {
            throw ExportCryptoException("Encryption failed", e)
        }
    }

    /**
     * Decrypt data with a user-provided passphrase.
     *
     * Reverses the encryption process:
     * 1. Extract salt (first 16 bytes)
     * 2. Extract IV (next 12 bytes)
     * 3. Extract ciphertext (remaining bytes)
     * 4. Derive key from passphrase + salt
     * 5. Decrypt with AES-256-GCM
     *
     * @param data The encrypted data (salt + IV + ciphertext)
     * @param passphrase The user's passphrase for key derivation
     * @return The decrypted plaintext
     * @throws ExportCryptoException if decryption fails (wrong passphrase, corrupt data, etc.)
     */
    fun decrypt(data: ByteArray, passphrase: String): ByteArray {
        try {
            if (data.size < SALT_LENGTH + IV_LENGTH) {
                throw ExportCryptoException("Encrypted data is too short")
            }

            // 1. Extract salt
            val salt = data.copyOfRange(0, SALT_LENGTH)

            // 2. Extract IV
            val iv = data.copyOfRange(SALT_LENGTH, SALT_LENGTH + IV_LENGTH)

            // 3. Extract ciphertext
            val ciphertext = data.copyOfRange(SALT_LENGTH + IV_LENGTH, data.size)

            // 4. Derive key from passphrase + salt
            val key = deriveKey(passphrase, salt)

            // 5. Decrypt with AES-256-GCM
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            val secretKey = SecretKeySpec(key, SECRET_KEY_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            return cipher.doFinal(ciphertext)
        } catch (e: ExportCryptoException) {
            throw e
        } catch (e: Exception) {
            throw ExportCryptoException("Decryption failed -- wrong passphrase or corrupt data", e)
        }
    }

    /**
     * Derive a 256-bit AES key from a passphrase and salt using PBKDF2.
     */
    private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        return factory.generateSecret(spec).encoded
    }
}

/**
 * Exception thrown when export/import cryptographic operations fail.
 */
class ExportCryptoException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
