/*
 * SapienSSH: modern SSH client for Android
 * Copyright 2025 SapienSSH contributors
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

package com.logicalsapien.sapienssh.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides AES-256-GCM encryption/decryption using Android Keystore.
 *
 * The encryption key is generated and stored in the hardware-backed Android
 * Keystore. It never leaves the secure hardware module and cannot be exported.
 * The key is created on first use and reused for all subsequent operations.
 *
 * Encrypted output format: [12-byte IV][ciphertext with GCM tag]
 */
@Singleton
class KeystoreEncryption @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    /**
     * Get or create the AES-256-GCM secret key in Android Keystore.
     *
     * @return The secret key for encryption/decryption
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )

        return keyGenerator.generateKey()
    }

    /**
     * Encrypt plaintext bytes using AES-256-GCM.
     *
     * The output contains the 12-byte IV prepended to the ciphertext (which
     * includes the GCM authentication tag). This format allows the IV to be
     * extracted during decryption without needing to store it separately.
     *
     * @param plaintext The data to encrypt
     * @return The IV + ciphertext, or null if encryption fails
     */
    fun encrypt(plaintext: ByteArray): ByteArray? = try {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plaintext)

        // Combine IV and encrypted data: [IV (12 bytes)][ciphertext + GCM tag]
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        combined
    } catch (e: Exception) {
        Timber.e(e, "Failed to encrypt data")
        null
    }

    /**
     * Decrypt ciphertext bytes using AES-256-GCM.
     *
     * Expects the input format produced by [encrypt]: a 12-byte IV followed
     * by the ciphertext with GCM authentication tag.
     *
     * @param ciphertext The IV + ciphertext to decrypt
     * @return The decrypted plaintext, or null if decryption fails
     */
    fun decrypt(ciphertext: ByteArray): ByteArray? = try {
        val secretKey = getOrCreateSecretKey()

        // Extract IV and encrypted data
        val iv = ciphertext.copyOfRange(0, GCM_IV_LENGTH)
        val encryptedBytes = ciphertext.copyOfRange(GCM_IV_LENGTH, ciphertext.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        cipher.doFinal(encryptedBytes)
    } catch (e: Exception) {
        Timber.e(e, "Failed to decrypt data")
        null
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "sapienssh_credential_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }
}
