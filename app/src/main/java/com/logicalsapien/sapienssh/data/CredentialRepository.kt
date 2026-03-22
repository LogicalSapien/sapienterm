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

package com.logicalsapien.sapienssh.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.logicalsapien.sapienssh.data.dao.CredentialDao
import com.logicalsapien.sapienssh.data.entity.Credential
import com.logicalsapien.sapienssh.data.entity.CredentialType
import com.logicalsapien.sapienssh.di.CoroutineDispatchers
import com.logicalsapien.sapienssh.util.KeystoreEncryption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing credentials with transparent encryption/decryption.
 *
 * All sensitive fields (password, private key, passphrase) are encrypted before
 * being stored in the database and decrypted when retrieved. Public methods work
 * with plaintext values -- encryption/decryption is handled internally.
 *
 * @param credentialDao The DAO for accessing credential data
 * @param keystoreEncryption The encryption utility using Android Keystore
 * @param dispatchers Coroutine dispatchers for IO operations
 */
@Singleton
class CredentialRepository @Inject constructor(
    private val credentialDao: CredentialDao,
    private val keystoreEncryption: KeystoreEncryption,
    private val dispatchers: CoroutineDispatchers
) {

    // ============================================================================
    // Observe Operations
    // ============================================================================

    /**
     * Observe all credentials.
     */
    fun observeAll(): Flow<List<Credential>> = credentialDao.observeAll()

    /**
     * Observe credentials filtered by type.
     *
     * @param type The credential type to filter by
     */
    fun observeByType(type: CredentialType): Flow<List<Credential>> = credentialDao.observeByType(type)

    // ============================================================================
    // Read Operations
    // ============================================================================

    /**
     * Get a credential by ID.
     *
     * @param id The credential ID
     * @return The credential, or null if not found
     */
    suspend fun getById(id: Long): Credential? = withContext(dispatchers.io) {
        credentialDao.getById(id)
    }

    /**
     * Get all credentials.
     *
     * @return List of all credentials
     */
    suspend fun getAll(): List<Credential> = withContext(dispatchers.io) {
        credentialDao.getAll()
    }

    // ============================================================================
    // Save Operations (plaintext in, encrypted storage)
    // ============================================================================

    /**
     * Save a password credential.
     *
     * @param label User-visible name for the credential
     * @param password The plaintext password to encrypt and store
     * @return The ID of the saved credential
     * @throws IllegalStateException if encryption fails
     */
    suspend fun savePassword(label: String, password: String): Long = withContext(dispatchers.io) {
        val encryptedPassword = keystoreEncryption.encrypt(password.toByteArray(Charsets.UTF_8))
            ?: throw IllegalStateException("Failed to encrypt password")

        val credential = Credential(
            label = label,
            type = CredentialType.PASSWORD,
            encryptedPassword = encryptedPassword
        )
        credentialDao.insert(credential)
    }

    /**
     * Save an SSH key credential.
     *
     * @param label User-visible name for the credential
     * @param privateKey The plaintext private key bytes to encrypt and store
     * @param publicKey The public key string (stored unencrypted for display)
     * @param passphrase Optional plaintext passphrase to encrypt and store
     * @return The ID of the saved credential
     * @throws IllegalStateException if encryption fails
     */
    suspend fun saveKey(
        label: String,
        privateKey: ByteArray,
        publicKey: String,
        passphrase: String?
    ): Long = withContext(dispatchers.io) {
        val encryptedPrivateKey = keystoreEncryption.encrypt(privateKey)
            ?: throw IllegalStateException("Failed to encrypt private key")

        val encryptedPassphrase = if (passphrase != null) {
            keystoreEncryption.encrypt(passphrase.toByteArray(Charsets.UTF_8))
                ?: throw IllegalStateException("Failed to encrypt passphrase")
        } else {
            null
        }

        val credential = Credential(
            label = label,
            type = CredentialType.SSH_KEY,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKey = publicKey,
            encryptedPassphrase = encryptedPassphrase
        )
        credentialDao.insert(credential)
    }

    // ============================================================================
    // Decrypt Operations (encrypted storage out to plaintext)
    // ============================================================================

    /**
     * Get the decrypted password for a credential.
     *
     * @param credentialId The credential ID
     * @return The plaintext password, or null if not found or decryption fails
     */
    suspend fun getDecryptedPassword(credentialId: Long): String? = withContext(dispatchers.io) {
        val credential = credentialDao.getById(credentialId) ?: return@withContext null
        val encrypted = credential.encryptedPassword ?: return@withContext null
        val decrypted = keystoreEncryption.decrypt(encrypted) ?: return@withContext null
        String(decrypted, Charsets.UTF_8)
    }

    /**
     * Get the decrypted private key for a credential.
     *
     * @param credentialId The credential ID
     * @return The plaintext private key bytes, or null if not found or decryption fails
     */
    suspend fun getDecryptedPrivateKey(credentialId: Long): ByteArray? = withContext(dispatchers.io) {
        val credential = credentialDao.getById(credentialId) ?: return@withContext null
        val encrypted = credential.encryptedPrivateKey ?: return@withContext null
        keystoreEncryption.decrypt(encrypted)
    }

    /**
     * Get the decrypted passphrase for a credential.
     *
     * @param credentialId The credential ID
     * @return The plaintext passphrase, or null if not found or decryption fails
     */
    suspend fun getDecryptedPassphrase(credentialId: Long): String? = withContext(dispatchers.io) {
        val credential = credentialDao.getById(credentialId) ?: return@withContext null
        val encrypted = credential.encryptedPassphrase ?: return@withContext null
        val decrypted = keystoreEncryption.decrypt(encrypted) ?: return@withContext null
        String(decrypted, Charsets.UTF_8)
    }

    // ============================================================================
    // Delete Operations
    // ============================================================================

    /**
     * Delete a credential.
     *
     * @param credential The credential to delete
     */
    suspend fun delete(credential: Credential) = withContext(dispatchers.io) {
        credentialDao.delete(credential)
    }

    /**
     * Delete all credentials.
     */
    suspend fun deleteAll() = withContext(dispatchers.io) {
        credentialDao.deleteAll()
    }
}
