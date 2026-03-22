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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.logicalsapien.sapienssh.data.CredentialRepository
import com.logicalsapien.sapienssh.data.entity.Credential
import com.logicalsapien.sapienssh.data.entity.CredentialType
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CredentialsViewModel @Inject constructor(
    private val repository: CredentialRepository
) : ViewModel() {

    val sshKeys: StateFlow<List<Credential>> = repository.observeByType(CredentialType.SSH_KEY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passwords: StateFlow<List<Credential>> = repository.observeByType(CredentialType.PASSWORD)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun addPassword(label: String, password: String) {
        viewModelScope.launch {
            try {
                repository.savePassword(label, password)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save password credential")
                _error.value = "Failed to save password: ${e.message}"
            }
        }
    }

    fun deleteCredential(credential: Credential) {
        viewModelScope.launch {
            try {
                repository.delete(credential)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete credential")
                _error.value = "Failed to delete credential: ${e.message}"
            }
        }
    }

    fun importKeyFromBytes(
        label: String,
        privateKeyBytes: ByteArray,
        publicKey: String,
        passphrase: String?
    ) {
        viewModelScope.launch {
            try {
                repository.saveKey(label, privateKeyBytes, publicKey, passphrase)
            } catch (e: Exception) {
                Timber.e(e, "Failed to import SSH key")
                _error.value = "Failed to import key: ${e.message}"
            }
        }
    }

    /**
     * Generate an SSH key pair and save it.
     *
     * Currently supports RSA (2048/4096) and Ed25519 key generation using
     * standard Java crypto APIs.
     */
    fun generateKey(
        label: String,
        algorithm: String,
        bits: Int,
        passphrase: String?
    ) {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val keyPair = when (algorithm) {
                    "RSA" -> {
                        val kpg = java.security.KeyPairGenerator.getInstance("RSA")
                        kpg.initialize(bits)
                        kpg.generateKeyPair()
                    }
                    "Ed25519" -> {
                        // Ed25519 requires API 33+ or BouncyCastle/sshlib
                        // Try standard Java crypto first
                        try {
                            val kpg = java.security.KeyPairGenerator.getInstance("Ed25519")
                            kpg.generateKeyPair()
                        } catch (e: java.security.NoSuchAlgorithmException) {
                            // Fallback: Ed25519 not available on this platform
                            _error.value = "Ed25519 key generation is not supported on this device. Try RSA instead."
                            _isGenerating.value = false
                            return@launch
                        }
                    }
                    else -> {
                        _error.value = "Unsupported algorithm: $algorithm"
                        _isGenerating.value = false
                        return@launch
                    }
                }

                // Encode private key in PKCS8 PEM format
                val privateKeyBytes = keyPair.private.encoded
                val publicKeyBase64 = android.util.Base64.encodeToString(
                    keyPair.public.encoded,
                    android.util.Base64.NO_WRAP
                )
                val publicKeyString = "$algorithm $publicKeyBase64 $label"

                repository.saveKey(label, privateKeyBytes, publicKeyString, passphrase)
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate SSH key")
                _error.value = "Failed to generate key: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Get the decrypted password for display/copy.
     */
    suspend fun getDecryptedPassword(credentialId: Long): String? {
        return try {
            repository.getDecryptedPassword(credentialId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt password")
            _error.value = "Failed to decrypt password"
            null
        }
    }
}
