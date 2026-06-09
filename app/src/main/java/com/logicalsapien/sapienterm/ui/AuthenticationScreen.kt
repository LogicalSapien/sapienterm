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

package com.logicalsapien.sapienterm.ui

import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.ui.theme.SapienTheme
import timber.log.Timber

@Composable
fun AuthenticationScreen(
    onAuthenticationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = SapienTheme.tokens
    val context = LocalContext.current
    var showRetryButton by remember { mutableStateOf(false) }

    val promptAuth = remember(context) {
        {
            val activity = context as? FragmentActivity
            if (activity != null) {
                showRetryButton = false
                val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
                val prompt = BiometricPrompt(
                    activity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            Timber.d("Authentication succeeded")
                            onAuthenticationSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Timber.d("Authentication error: $errorCode $errString")
                            showRetryButton = true
                        }

                        override fun onAuthenticationFailed() {
                            Timber.d("Authentication failed")
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(activity.getString(R.string.auth_prompt_title))
                    .setSubtitle(activity.getString(R.string.auth_prompt_subtitle))
                    .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
                    .build()

                prompt.authenticate(promptInfo)
            } else {
                Timber.e("Context is not a FragmentActivity, cannot show BiometricPrompt")
                showRetryButton = true
            }
        }
    }

    LaunchedEffect(Unit) {
        promptAuth()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = tokens.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.auth_screen_title),
                color = tokens.textPrimary,
                textAlign = TextAlign.Center
            )
            if (showRetryButton) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { promptAuth() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.primary,
                        contentColor = tokens.textOnPrimary
                    )
                ) {
                    Text(stringResource(R.string.auth_screen_unlock_button))
                }
            }
        }
    }
}
