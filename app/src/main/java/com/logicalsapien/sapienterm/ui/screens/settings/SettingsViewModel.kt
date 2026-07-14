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

package com.logicalsapien.sapienterm.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logicalsapien.sapienterm.data.CustomBottomBarLayout
import com.logicalsapien.sapienterm.data.CustomBottomBarLayoutStore
import com.logicalsapien.sapienterm.data.ProfileRepository
import com.logicalsapien.sapienterm.data.entity.Profile
import com.logicalsapien.sapienterm.data.export.AutoBackupManager
import com.logicalsapien.sapienterm.data.export.BackupFileInfo
import com.logicalsapien.sapienterm.data.export.ExportCryptoException
import com.logicalsapien.sapienterm.data.export.ExportData
import com.logicalsapien.sapienterm.data.export.ExportManager
import com.logicalsapien.sapienterm.data.export.ImportException
import com.logicalsapien.sapienterm.data.export.ImportManager
import com.logicalsapien.sapienterm.data.export.ImportMode
import com.logicalsapien.sapienterm.data.export.ImportResult
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import com.logicalsapien.sapienterm.ui.screens.console.TerminalBottomBarPreset
import com.logicalsapien.sapienterm.util.ExtendedKey
import com.logicalsapien.sapienterm.util.ExtendedKeyboardConfig
import com.logicalsapien.sapienterm.util.LocalFontProvider
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.SessionKeyboardPolicy
import com.logicalsapien.sapienterm.util.TerminalFontProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val authOnLaunch: Boolean = false,
    val canAuthenticate: Boolean = false,
    val memkeys: Boolean = true,
    val connPersist: Boolean = false,
    val wifilock: Boolean = true,
    val backupkeys: Boolean = false,
    val scrollback: String = "140",
    val rotation: String = "Default",
    val titlebarhide: Boolean = false,
    val fullscreen: Boolean = false,
    val pgupdngesture: Boolean = false,
    val volumefont: Boolean = true,
    val keepalive: Boolean = true,
    val alwaysvisible: Boolean = false,
    val shiftfkeys: Boolean = false,
    val ctrlfkeys: Boolean = false,
    val stickymodifiers: String = "no",
    val keymode: String = "none",
    val camera: String = "Ctrl+A then Space",
    val bumpyarrows: Boolean = true,
    val bell: Boolean = true,
    val bellVolume: Float = 0.5f,
    val bellVibrate: Boolean = true,
    val bellNotification: Boolean = false,
    val fontFamily: String = "SYSTEM_DEFAULT",
    val customFonts: List<String> = emptyList(),
    val customTerminalTypes: List<String> = emptyList(),
    val localFonts: List<Pair<String, String>> = emptyList(),
    val fontValidationInProgress: Boolean = false,
    val fontValidationError: String? = null,
    val fontImportInProgress: Boolean = false,
    val fontImportError: String? = null,
    val fontDownloadInProgress: Boolean = false,
    val language: String = "",
    val defaultProfileId: Long = 0L,
    val availableProfiles: List<Profile> = emptyList(),
    // Extended keyboard strip
    val extendedKeyboardKeys: Set<String> = emptySet(),
    val themeMode: String = "auto", // "auto", "dark", "light"
    /** [PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET] value (`default`, `tmux`, or `custom:…`). */
    val bottomBarPreset: String = "default",
    /** Software (Android) IME on session start: `auto` / `on` / `off`. */
    val sessionKeyboard: String = SessionKeyboardPolicy.STORED_AUTO,
    val customBottomBarLayouts: List<CustomBottomBarLayout> = emptyList(),
    // Export/Import state
    val exportInProgress: Boolean = false,
    val importInProgress: Boolean = false,
    val importPreview: ExportData? = null,
    val importFileUri: Uri? = null,
    val importNeedsPassphrase: Boolean = false,
    // Auto-backup state
    val autoBackupEnabled: Boolean = false,
    val autoBackupRetention: Int = AutoBackupManager.DEFAULT_RETENTION,
    val autoBackupLastTime: Long = 0L,
    val autoBackupFiles: List<BackupFileInfo> = emptyList(),
    val autoBackupInProgress: Boolean = false
)

/**
 * Sealed class for one-shot events from the ViewModel to the UI.
 */
sealed class SettingsEvent {
    data class ExportSuccess(val uri: Uri) : SettingsEvent()
    data class ExportError(val message: String) : SettingsEvent()
    data class ImportSuccess(val result: ImportResult) : SettingsEvent()
    data class ImportError(val message: String) : SettingsEvent()
    object ImportWrongPassphrase : SettingsEvent()
    object AutoBackupSuccess : SettingsEvent()
    object AutoBackupError : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SharedPreferences,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatchers,
    private val exportManager: ExportManager,
    private val importManager: ImportManager,
    private val autoBackupManager: AutoBackupManager,
    private val customBottomBarLayoutStore: CustomBottomBarLayoutStore
) : ViewModel() {
    private val fontProvider = TerminalFontProvider(context, dispatchers.io)
    private val localFontProvider = LocalFontProvider(context)
    private val _uiState = MutableStateFlow(loadSettings())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _requestNotificationPermission = Channel<Unit>(Channel.CONFLATED)
    val requestNotificationPermission = _requestNotificationPermission.receiveAsFlow()

    private val _showPermissionDeniedDialog = Channel<Unit>(Channel.CONFLATED)
    val showPermissionDeniedDialog = _showPermissionDeniedDialog.receiveAsFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Persist permission denial state in SharedPreferences
    private var wasPermissionDenied: Boolean
        get() = prefs.getBoolean(PreferenceConstants.NOTIFICATION_PERMISSION_DENIED, false)
        set(value) {
            prefs.edit { putBoolean(PreferenceConstants.NOTIFICATION_PERMISSION_DENIED, value) }
        }

    init {
        loadProfiles()
        refreshAutoBackupFiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            val profiles = profileRepository.getAll()
            _uiState.update { it.copy(availableProfiles = profiles) }
        }
    }

    /** Migrates legacy `zellij` / `multiplexer` to `tmux`. */
    private fun normalizeBottomBarPresetPref(): String {
        val raw = prefs.getString(PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET, "default") ?: "default"
        return when (raw) {
            "multiplexer", "zellij" -> {
                prefs.edit { putString(PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET, "tmux") }
                "tmux"
            }

            else -> raw
        }
    }

    /** One-time: merge legacy per-preset IME keys into [PreferenceConstants.TERMINAL_SESSION_KEYBOARD]. */
    private fun migrateSessionKeyboardFromLegacy() {
        if (prefs.contains(PreferenceConstants.TERMINAL_SESSION_KEYBOARD)) return
        val v = prefs.getString(PreferenceConstants.TERMINAL_KEYBOARD_BAR_DEFAULT, SessionKeyboardPolicy.STORED_AUTO)
            ?: SessionKeyboardPolicy.STORED_AUTO
        prefs.edit { putString(PreferenceConstants.TERMINAL_SESSION_KEYBOARD, v) }
    }

    private fun loadSettings(): SettingsUiState {
        migrateSessionKeyboardFromLegacy()
        val customFontsString = prefs.getString("customFonts", "") ?: ""
        val customFonts = if (customFontsString.isBlank()) {
            emptyList()
        } else {
            customFontsString.split(",").filter { it.isNotBlank() }
        }
        val customTerminalTypesString = prefs.getString("customTerminalTypes", "") ?: ""
        val customTerminalTypes = if (customTerminalTypesString.isBlank()) {
            emptyList()
        } else {
            customTerminalTypesString.split(",").filter { it.isNotBlank() }
        }
        val localFonts = localFontProvider.getImportedFonts()

        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLanguage = if (appLocales.isEmpty) "" else appLocales.toLanguageTags()

        val canAuthenticate = BiometricManager.from(context)
            .canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS

        val extendedConfig = ExtendedKeyboardConfig.load(prefs)
        val extendedKeyIds = extendedConfig.enabledKeys.map { it.id }.toSet()

        return SettingsUiState(
            authOnLaunch = prefs.getBoolean(PreferenceConstants.AUTH_ON_LAUNCH, false),
            canAuthenticate = canAuthenticate,
            memkeys = prefs.getBoolean("memkeys", true),
            connPersist = prefs.getBoolean(PreferenceConstants.CONNECTION_PERSIST, false),
            wifilock = prefs.getBoolean("wifilock", true),
            backupkeys = prefs.getBoolean("backupkeys", false),
            scrollback = prefs.getString("scrollback", "140") ?: "140",
            rotation = prefs.getString("rotation", "Default") ?: "Default",
            titlebarhide = prefs.getBoolean("titlebarhide", false),
            fullscreen = prefs.getBoolean("fullscreen", false),
            pgupdngesture = prefs.getBoolean("pgupdngesture", false),
            volumefont = prefs.getBoolean("volumefont", true),
            keepalive = prefs.getBoolean("keepalive", true),
            alwaysvisible = prefs.getBoolean("alwaysvisible", false),
            shiftfkeys = prefs.getBoolean("shiftfkeys", false),
            ctrlfkeys = prefs.getBoolean("ctrlfkeys", false),
            stickymodifiers = prefs.getString("stickymodifiers", "no") ?: "no",
            keymode = prefs.getString("keymode", "none") ?: "none",
            camera = prefs.getString("camera", "Ctrl+A then Space") ?: "Ctrl+A then Space",
            bumpyarrows = prefs.getBoolean("bumpyarrows", true),
            bell = prefs.getBoolean("bell", true),
            bellVolume = prefs.getFloat("bellVolume", 0.5f),
            bellVibrate = prefs.getBoolean("bellVibrate", true),
            bellNotification = prefs.getBoolean("bellNotification", false),
            fontFamily = prefs.getString("fontFamily", "SYSTEM_DEFAULT") ?: "SYSTEM_DEFAULT",
            customFonts = customFonts,
            customTerminalTypes = customTerminalTypes,
            localFonts = localFonts,
            language = currentLanguage,
            defaultProfileId = prefs.getLong("defaultProfileId", 0L),
            extendedKeyboardKeys = extendedKeyIds,
            themeMode = prefs.getString("theme_mode", "auto") ?: "auto",
            bottomBarPreset = normalizeBottomBarPresetPref(),
            sessionKeyboard = prefs.getString(
                PreferenceConstants.TERMINAL_SESSION_KEYBOARD,
                SessionKeyboardPolicy.STORED_AUTO
            ) ?: SessionKeyboardPolicy.STORED_AUTO,
            customBottomBarLayouts = customBottomBarLayoutStore.loadAll(),
            autoBackupEnabled = autoBackupManager.isEnabled,
            autoBackupRetention = autoBackupManager.retentionCount,
            autoBackupLastTime = autoBackupManager.lastBackupTime
        )
    }

    fun updateAuthOnLaunch(value: Boolean) {
        if (value) {
            val canAuth = BiometricManager.from(context)
                .canAuthenticate(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
                Timber.d("Cannot enable auth on launch: canAuthenticate=$canAuth")
                return
            }
        }
        updateBooleanPref(PreferenceConstants.AUTH_ON_LAUNCH, value) { copy(authOnLaunch = value) }
    }

    fun updateMemkeys(value: Boolean) {
        updateBooleanPref("memkeys", value) { copy(memkeys = value) }
    }

    fun updateConnPersist(value: Boolean) {
        // If turning ON (from OFF), request notification permission
        val currentValue = _uiState.value.connPersist
        if (!currentValue && value) {
            // Turning ON - check if permission was previously denied
            if (wasPermissionDenied) {
                // Permission was denied before, show dialog to go to settings
                viewModelScope.launch {
                    _showPermissionDeniedDialog.send(Unit)
                }
            } else {
                // First time or permission not denied yet - optimistically update to ON
                // and request permission. If denied, onNotificationPermissionResult will revert to OFF.
                updateBooleanPref(PreferenceConstants.CONNECTION_PERSIST, true) { copy(connPersist = true) }
                viewModelScope.launch {
                    _requestNotificationPermission.send(Unit)
                }
            }
        } else {
            // Turning OFF or already ON - just update the preference
            updateBooleanPref(PreferenceConstants.CONNECTION_PERSIST, value) { copy(connPersist = value) }
        }
    }

    /**
     * Called with the result of the notification permission request.
     * If permission is granted, enable connPersist. If denied, keep it OFF.
     */
    fun onNotificationPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            Timber.d("Notification permission granted, enabling connPersist")
            wasPermissionDenied = false
            updateBooleanPref(PreferenceConstants.CONNECTION_PERSIST, true) { copy(connPersist = true) }
        } else {
            // Permission denied - keep it OFF and mark as denied
            Timber.d("Notification permission denied, keeping connPersist OFF")
            wasPermissionDenied = true
            updateBooleanPref(PreferenceConstants.CONNECTION_PERSIST, false) { copy(connPersist = false) }
        }
    }

    fun updateWifilock(value: Boolean) {
        updateBooleanPref("wifilock", value) { copy(wifilock = value) }
    }

    fun updateBackupkeys(value: Boolean) {
        updateBooleanPref("backupkeys", value) { copy(backupkeys = value) }
    }

    fun updateFullscreen(value: Boolean) {
        updateBooleanPref("fullscreen", value) { copy(fullscreen = value) }
    }

    fun updateKeepAlive(value: Boolean) {
        updateBooleanPref("keepalive", value) { copy(keepalive = value) }
    }

    fun updateBell(value: Boolean) {
        updateBooleanPref("bell", value) { copy(bell = value) }
    }

    fun updateBellVibrate(value: Boolean) {
        updateBooleanPref("bellVibrate", value) { copy(bellVibrate = value) }
    }

    fun updateBellNotification(value: Boolean) {
        updateBooleanPref("bellNotification", value) { copy(bellNotification = value) }
    }

    fun updateTitleBarHide(value: Boolean) {
        updateBooleanPref("titlebarhide", value) { copy(titlebarhide = value) }
    }

    fun updatePgUpDnGesture(value: Boolean) {
        updateBooleanPref("pgupdngesture", value) { copy(pgupdngesture = value) }
    }

    fun updateVolumeFont(value: Boolean) {
        updateBooleanPref("volumefont", value) { copy(volumefont = value) }
    }

    fun updateAlwaysVisible(value: Boolean) {
        updateBooleanPref("alwaysvisible", value) { copy(alwaysvisible = value) }
    }

    fun updateShiftFkeys(value: Boolean) {
        updateBooleanPref("shiftfkeys", value) { copy(shiftfkeys = value) }
    }

    fun updateCtrlFkeys(value: Boolean) {
        updateBooleanPref("ctrlfkeys", value) { copy(ctrlfkeys = value) }
    }

    fun updateBumpyArrows(value: Boolean) {
        updateBooleanPref("bumpyarrows", value) { copy(bumpyarrows = value) }
    }

    fun updateExtendedKeyboardKeys(enabledKeyIds: Set<String>) {
        viewModelScope.launch {
            val enabledKeys = enabledKeyIds.mapNotNull { ExtendedKey.fromId(it) }.toSet()
            val config = ExtendedKeyboardConfig(enabledKeys = enabledKeys)
            ExtendedKeyboardConfig.save(prefs, config)
            _uiState.update { it.copy(extendedKeyboardKeys = enabledKeyIds) }
        }
    }

    fun updateScrollback(value: String) {
        updateStringPref("scrollback", value) { copy(scrollback = value) }
    }

    fun updateBottomBarPreset(value: String) {
        updateStringPref(PreferenceConstants.TERMINAL_BOTTOM_BAR_PRESET, value) {
            copy(bottomBarPreset = value)
        }
    }

    fun updateSessionKeyboard(value: String) {
        updateStringPref(PreferenceConstants.TERMINAL_SESSION_KEYBOARD, value) {
            copy(sessionKeyboard = value)
        }
    }

    fun saveCustomBottomBarLayout(layout: CustomBottomBarLayout) {
        customBottomBarLayoutStore.save(layout)
        _uiState.update { it.copy(customBottomBarLayouts = customBottomBarLayoutStore.loadAll()) }
    }

    fun createCustomBottomBarLayout(name: String, actionIds: List<String>) {
        customBottomBarLayoutStore.createNew(name, actionIds)
        _uiState.update { it.copy(customBottomBarLayouts = customBottomBarLayoutStore.loadAll()) }
    }

    fun deleteCustomBottomBarLayout(id: String) {
        val current = _uiState.value.bottomBarPreset
        if (current == TerminalBottomBarPreset.CUSTOM_PREFIX + id) {
            updateBottomBarPreset("default")
        }
        customBottomBarLayoutStore.delete(id)
        _uiState.update { it.copy(customBottomBarLayouts = customBottomBarLayoutStore.loadAll()) }
    }

    fun updateStickyModifiers(value: String) {
        updateStringPref("stickymodifiers", value) { copy(stickymodifiers = value) }
    }

    fun updateKeyMode(value: String) {
        updateStringPref("keymode", value) { copy(keymode = value) }
    }

    fun updateCamera(value: String) {
        updateStringPref("camera", value) { copy(camera = value) }
    }

    fun updateRotation(value: String) {
        updateStringPref("rotation", value) { copy(rotation = value) }
    }

    fun updateThemeMode(mode: String) {
        updateStringPref("theme_mode", mode) { copy(themeMode = mode) }
    }

    fun updateLanguage(languageTag: String) {
        val localeList = if (languageTag.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
        _uiState.update { it.copy(language = languageTag) }
    }

    fun updateBellVolume(value: Float) {
        updateFloatPref("bellVolume", value) { copy(bellVolume = value) }
    }

    fun updateFontFamily(value: String) {
        updateStringPref("fontFamily", value) { copy(fontFamily = value) }
        // Preload the font so it's cached when the Terminal opens
        preloadFont(value)
    }

    fun updateDefaultProfile(profileId: Long) {
        viewModelScope.launch {
            prefs.edit { putLong("defaultProfileId", profileId) }
            _uiState.update { it.copy(defaultProfileId = profileId) }
        }
    }

    fun addCustomTerminalType(terminalType: String) {
        if (terminalType.isBlank()) return
        val currentTypes = _uiState.value.customTerminalTypes
        if (currentTypes.contains(terminalType)) return

        viewModelScope.launch {
            val updatedTypes = currentTypes + terminalType
            val typesString = updatedTypes.joinToString(",")
            prefs.edit { putString("customTerminalTypes", typesString) }
            _uiState.update { it.copy(customTerminalTypes = updatedTypes) }
        }
    }

    fun removeCustomTerminalType(terminalType: String) {
        viewModelScope.launch {
            val currentTypes = _uiState.value.customTerminalTypes.toMutableList()
            if (currentTypes.remove(terminalType)) {
                val typesString = currentTypes.joinToString(",")
                prefs.edit { putString("customTerminalTypes", typesString) }
                _uiState.update { it.copy(customTerminalTypes = currentTypes) }
            }
        }
    }

    private fun preloadFont(storedValue: String) {
        if (LocalFontProvider.isLocalFont(storedValue)) return
        val googleFontName = com.logicalsapien.sapienterm.util.TerminalFont.getGoogleFontName(storedValue)
        if (googleFontName.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(fontDownloadInProgress = true) }
            fontProvider.loadFontByNameSuspend(googleFontName)
            _uiState.update { it.copy(fontDownloadInProgress = false) }
        }
    }

    fun addCustomFont(fontName: String) {
        if (fontName.isBlank()) return
        val currentFonts = _uiState.value.customFonts
        if (currentFonts.contains(fontName)) {
            _uiState.update { it.copy(fontValidationError = "Font already added") }
            return
        }

        // Validate font by attempting to load it
        _uiState.update { it.copy(fontValidationInProgress = true, fontValidationError = null) }

        fontProvider.loadFontByName(fontName) { typeface ->
            viewModelScope.launch {
                if (typeface != Typeface.MONOSPACE) {
                    // Font loaded successfully, add it to the list
                    val updatedFonts = currentFonts + fontName
                    val fontsString = updatedFonts.joinToString(",")
                    prefs.edit { putString("customFonts", fontsString) }
                    _uiState.update {
                        it.copy(
                            customFonts = updatedFonts,
                            fontValidationInProgress = false,
                            fontValidationError = null
                        )
                    }
                } else {
                    // Font failed to load
                    _uiState.update {
                        it.copy(
                            fontValidationInProgress = false,
                            fontValidationError = "Font not found in Google Fonts"
                        )
                    }
                }
            }
        }
    }

    fun clearFontValidationError() {
        _uiState.update { it.copy(fontValidationError = null) }
    }

    fun removeCustomFont(fontName: String) {
        viewModelScope.launch {
            val currentFonts = _uiState.value.customFonts.toMutableList()
            if (currentFonts.remove(fontName)) {
                val fontsString = currentFonts.joinToString(",")
                prefs.edit { putString("customFonts", fontsString) }
                _uiState.update { it.copy(customFonts = currentFonts) }

                // If the removed font was the selected font, reset to system default
                if (_uiState.value.fontFamily == "custom:$fontName") {
                    updateFontFamily("SYSTEM_DEFAULT")
                }
            }
        }
    }

    fun importLocalFont(uri: Uri, displayName: String) {
        if (displayName.isBlank()) return

        _uiState.update { it.copy(fontImportInProgress = true, fontImportError = null) }

        viewModelScope.launch {
            val fileName = withContext(dispatchers.io) {
                localFontProvider.importFont(uri, displayName)
            }

            if (fileName != null) {
                val updatedLocalFonts = localFontProvider.getImportedFonts()
                _uiState.update {
                    it.copy(
                        localFonts = updatedLocalFonts,
                        fontImportInProgress = false,
                        fontImportError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        fontImportInProgress = false,
                        fontImportError = "Failed to import font"
                    )
                }
            }
        }
    }

    fun deleteLocalFont(fileName: String) {
        viewModelScope.launch {
            val deleted = withContext(dispatchers.io) {
                localFontProvider.deleteFont(fileName)
            }

            if (deleted) {
                val updatedLocalFonts = localFontProvider.getImportedFonts()
                _uiState.update { it.copy(localFonts = updatedLocalFonts) }

                // If the removed font was the selected font, reset to system default
                if (_uiState.value.fontFamily == "${LocalFontProvider.LOCAL_PREFIX}$fileName") {
                    updateFontFamily("SYSTEM_DEFAULT")
                }
            }
        }
    }

    fun clearFontImportError() {
        _uiState.update { it.copy(fontImportError = null) }
    }

    // ── Export / Import ─────────────────────────────────────────────────

    fun exportData(
        includeConnections: Boolean,
        includeQuickCommands: Boolean,
        includeCredentials: Boolean,
        passphrase: String?,
        includeProfiles: Boolean = true,
        includePreferences: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(exportInProgress = true) }
            try {
                val uri = exportManager.exportData(
                    includeConnections = includeConnections,
                    includeQuickCommands = includeQuickCommands,
                    includeCredentials = includeCredentials,
                    passphrase = passphrase,
                    includeProfiles = includeProfiles,
                    includePreferences = includePreferences
                )
                _events.send(SettingsEvent.ExportSuccess(uri))
            } catch (e: Exception) {
                Timber.e(e, "Export failed")
                _events.send(SettingsEvent.ExportError(e.message ?: "Unknown error"))
            } finally {
                _uiState.update { it.copy(exportInProgress = false) }
            }
        }
    }

    fun previewImport(uri: Uri, passphrase: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(importInProgress = true, importFileUri = uri) }
            try {
                val preview = importManager.previewImport(uri, passphrase)
                _uiState.update {
                    it.copy(
                        importInProgress = false,
                        importPreview = preview,
                        importNeedsPassphrase = false
                    )
                }
            } catch (e: ExportCryptoException) {
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportWrongPassphrase)
            } catch (e: ImportException) {
                val msg = e.message ?: "Unknown error"
                if (msg.contains("passphrase", ignoreCase = true) ||
                    msg.contains("encrypted", ignoreCase = true)
                ) {
                    _uiState.update {
                        it.copy(
                            importInProgress = false,
                            importNeedsPassphrase = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(importInProgress = false) }
                    _events.send(SettingsEvent.ImportError(msg))
                }
            } catch (e: Exception) {
                Timber.e(e, "Import preview failed")
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportError(e.message ?: "Unknown error"))
            }
        }
    }

    fun importData(uri: Uri, passphrase: String?, mode: ImportMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(importInProgress = true) }
            try {
                val result = importManager.importData(uri, passphrase, mode)
                _uiState.update {
                    it.copy(
                        importInProgress = false,
                        importPreview = null,
                        importFileUri = null,
                        importNeedsPassphrase = false
                    )
                }
                _events.send(SettingsEvent.ImportSuccess(result))
            } catch (e: ExportCryptoException) {
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportWrongPassphrase)
            } catch (e: Exception) {
                Timber.e(e, "Import failed")
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportError(e.message ?: "Unknown error"))
            }
        }
    }

    fun clearImportState() {
        _uiState.update {
            it.copy(
                importPreview = null,
                importFileUri = null,
                importNeedsPassphrase = false
            )
        }
    }

    // ── Auto-backup ─────────────────────────────────────────────────

    fun updateAutoBackupEnabled(enabled: Boolean) {
        autoBackupManager.setEnabled(enabled)
        _uiState.update { it.copy(autoBackupEnabled = enabled) }
        if (enabled) {
            runAutoBackupNow()
        }
    }

    fun updateAutoBackupRetention(count: Int) {
        autoBackupManager.setRetentionCount(count)
        _uiState.update { it.copy(autoBackupRetention = count) }
    }

    fun runAutoBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(autoBackupInProgress = true) }
            val success = autoBackupManager.runBackup()
            _uiState.update {
                it.copy(
                    autoBackupInProgress = false,
                    autoBackupLastTime = autoBackupManager.lastBackupTime,
                    autoBackupFiles = autoBackupManager.listBackups()
                )
            }
            if (success) {
                _events.send(SettingsEvent.AutoBackupSuccess)
            } else {
                _events.send(SettingsEvent.AutoBackupError)
            }
        }
    }

    fun restoreAutoBackup(fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(importInProgress = true) }
            try {
                val file = autoBackupManager.getBackupFile(fileName)
                val result = importManager.importFromFile(file, ImportMode.MERGE)
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportSuccess(result))
            } catch (e: Exception) {
                Timber.e(e, "Auto-backup restore failed")
                _uiState.update { it.copy(importInProgress = false) }
                _events.send(SettingsEvent.ImportError(e.message ?: "Unknown error"))
            }
        }
    }

    private fun refreshAutoBackupFiles() {
        viewModelScope.launch(dispatchers.io) {
            val files = autoBackupManager.listBackups()
            _uiState.update { it.copy(autoBackupFiles = files) }
        }
    }

    private fun updateBooleanPref(key: String, value: Boolean, updateState: SettingsUiState.() -> SettingsUiState) {
        viewModelScope.launch {
            prefs.edit { putBoolean(key, value) }
            _uiState.update { it.updateState() }
        }
    }

    private fun updateStringPref(key: String, value: String, updateState: SettingsUiState.() -> SettingsUiState) {
        viewModelScope.launch {
            prefs.edit { putString(key, value) }
            _uiState.update { it.updateState() }
        }
    }

    private fun updateFloatPref(key: String, value: Float, updateState: SettingsUiState.() -> SettingsUiState) {
        viewModelScope.launch {
            prefs.edit { putFloat(key, value) }
            _uiState.update { it.updateState() }
        }
    }
}
