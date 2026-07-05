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

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.logicalsapien.sapienterm.BuildConfig
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.CustomBottomBarLayout
import com.logicalsapien.sapienterm.ui.ObservePermissionOnResume
import com.logicalsapien.sapienterm.ui.PreviewScreen
import com.logicalsapien.sapienterm.ui.common.getLocalizedFontDisplayName
import com.logicalsapien.sapienterm.ui.components.FontDownloadProgressDialog
import com.logicalsapien.sapienterm.ui.screens.console.TerminalBottomBarPreset
import com.logicalsapien.sapienterm.ui.theme.SapienTermTheme
import com.logicalsapien.sapienterm.ui.theme.SapienTheme
import com.logicalsapien.sapienterm.util.ExtendedKey
import com.logicalsapien.sapienterm.util.LocalFontProvider
import com.logicalsapien.sapienterm.util.NotificationPermissionHelper
import com.logicalsapien.sapienterm.util.SessionKeyboardPolicy
import com.logicalsapien.sapienterm.util.TerminalFont
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    filterCategory: SettingsCategory? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission launcher for notification permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Check the actual permission status instead of relying on the launcher result.
        // If user went to settings and granted permission, the result will be false but
        // the actual permission may be granted.
        val actuallyGranted = NotificationPermissionHelper.isNotificationPermissionGranted(context)
        viewModel.onNotificationPermissionResult(actuallyGranted)
    }

    // Listen for permission request events
    LaunchedEffect(Unit) {
        viewModel.requestNotificationPermission.collect {
            if (NotificationPermissionHelper.isNotificationPermissionGranted(context)) {
                // Permission already granted
                viewModel.onNotificationPermissionResult(true)
            } else {
                // Request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // No permission needed on older versions
                    viewModel.onNotificationPermissionResult(true)
                }
            }
        }
    }

    // State for showing permission denied dialog
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    var showCustomBarEditor by remember { mutableStateOf(false) }
    var customBarEditorInitial by remember { mutableStateOf<CustomBottomBarLayout?>(null) }
    var showBottomBarIconLegend by remember { mutableStateOf(false) }

    if (showCustomBarEditor) {
        CustomBottomBarLayoutEditorDialog(
            initial = customBarEditorInitial,
            onDismiss = { showCustomBarEditor = false },
            onSave = { name, ids, existingId ->
                if (existingId == null) {
                    viewModel.createCustomBottomBarLayout(name, ids)
                } else {
                    viewModel.saveCustomBottomBarLayout(
                        CustomBottomBarLayout(id = existingId, name = name, actionIds = ids)
                    )
                }
                showCustomBarEditor = false
            },
            onOpenIconLegend = { showBottomBarIconLegend = true }
        )
    }

    if (showBottomBarIconLegend) {
        BottomBarIconLegendDialog(onDismiss = { showBottomBarIconLegend = false })
    }

    // Listen for permission denied dialog events
    LaunchedEffect(Unit) {
        viewModel.showPermissionDeniedDialog.collect {
            showPermissionDeniedDialog = true
        }
    }

    // Re-check permission status when screen resumes (e.g., user grants/revokes in Settings)
    ObservePermissionOnResume { isGranted ->
        viewModel.onNotificationPermissionResult(isGranted)
    }

    // Show permission denied dialog if needed
    if (showPermissionDeniedDialog) {
        NotificationPermissionDeniedDialog(
            onDismiss = {
                showPermissionDeniedDialog = false
            },
            onOpenSettings = {
                showPermissionDeniedDialog = false
                // Open app settings so user can grant notification permission
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    // Export/Import dialog state
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportPassphraseDialog by remember { mutableStateOf(false) }
    var importPassphrase by remember { mutableStateOf<String?>(null) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    // File picker for import
    val importFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Check if file needs passphrase (encrypted .sapienterm files)
            val path = uri.lastPathSegment ?: uri.path ?: ""
            if (path.endsWith(".sapienterm", ignoreCase = true)) {
                // Encrypted file - ask for passphrase first
                viewModel.clearImportState()
                importPassphrase = null
                showImportPassphraseDialog = true
                // Store the URI via ViewModel for later use
                viewModel.previewImport(uri, null) // Will trigger passphrase needed
            } else {
                // Plain JSON - preview directly
                importPassphrase = null
                viewModel.previewImport(uri, null)
            }
        }
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ExportSuccess -> {
                    showExportDialog = false
                    // Launch share sheet
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.export_dialog_title))
                    )
                    Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
                }

                is SettingsEvent.ExportError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.export_error, event.message),
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SettingsEvent.ImportSuccess -> {
                    val result = event.result
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.import_success,
                            result.connectionsImported,
                            result.quickCommandsImported,
                            result.credentialsImported,
                            result.groupsImported
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SettingsEvent.ImportError -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.import_error, event.message),
                        Toast.LENGTH_LONG
                    ).show()
                }

                is SettingsEvent.ImportWrongPassphrase -> {
                    Toast.makeText(context, R.string.import_wrong_passphrase, Toast.LENGTH_LONG).show()
                    showImportPassphraseDialog = true
                }

                is SettingsEvent.AutoBackupSuccess -> {
                    Toast.makeText(context, R.string.auto_backup_success, Toast.LENGTH_SHORT).show()
                }

                is SettingsEvent.AutoBackupError -> {
                    Toast.makeText(context, R.string.auto_backup_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Export dialog
    if (showExportDialog) {
        ExportDialog(
            isExporting = uiState.exportInProgress,
            onExport = { connections, quickCommands, credentials, passphrase, profiles, preferences ->
                viewModel.exportData(connections, quickCommands, credentials, passphrase, profiles, preferences)
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Import passphrase dialog
    if (showImportPassphraseDialog || uiState.importNeedsPassphrase) {
        ImportPassphraseDialog(
            isLoading = uiState.importInProgress,
            onSubmit = { passphrase ->
                importPassphrase = passphrase
                showImportPassphraseDialog = false
                uiState.importFileUri?.let { uri ->
                    viewModel.previewImport(uri, passphrase)
                }
            },
            onDismiss = {
                showImportPassphraseDialog = false
                viewModel.clearImportState()
            }
        )
    }

    // Auto-backup restore dialog
    if (showRestoreDialog) {
        AutoBackupRestoreDialog(
            backupFiles = uiState.autoBackupFiles,
            onRestore = { fileName ->
                showRestoreDialog = false
                viewModel.restoreAutoBackup(fileName)
            },
            onDismiss = { showRestoreDialog = false }
        )
    }

    // Import preview dialog
    if (uiState.importPreview != null) {
        ImportPreviewDialog(
            preview = uiState.importPreview!!,
            isImporting = uiState.importInProgress,
            onImport = { mode ->
                uiState.importFileUri?.let { uri ->
                    viewModel.importData(uri, importPassphrase, mode)
                }
            },
            onDismiss = {
                viewModel.clearImportState()
            }
        )
    }

    SettingsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onAuthOnLaunchChange = viewModel::updateAuthOnLaunch,
        onMemkeysChange = viewModel::updateMemkeys,
        onConnPersistChange = viewModel::updateConnPersist,
        onWifilockChange = viewModel::updateWifilock,
        onBackupkeysChange = viewModel::updateBackupkeys,
        onScrollbackChange = viewModel::updateScrollback,
        onBottomBarPresetChange = viewModel::updateBottomBarPreset,
        onSessionKeyboardChange = viewModel::updateSessionKeyboard,
        onOpenCustomBottomBarCreate = {
            customBarEditorInitial = null
            showCustomBarEditor = true
        },
        onOpenCustomBottomBarEdit = { layout ->
            customBarEditorInitial = layout
            showCustomBarEditor = true
        },
        onShowBottomBarIconLegend = { showBottomBarIconLegend = true },
        onDeleteCustomBottomBarLayout = viewModel::deleteCustomBottomBarLayout,
        onAddCustomTerminalType = viewModel::addCustomTerminalType,
        onRemoveCustomTerminalType = viewModel::removeCustomTerminalType,
        onFontFamilyChange = viewModel::updateFontFamily,
        onAddCustomFont = viewModel::addCustomFont,
        onRemoveCustomFont = viewModel::removeCustomFont,
        onClearFontError = viewModel::clearFontValidationError,
        onImportLocalFont = viewModel::importLocalFont,
        onDeleteLocalFont = viewModel::deleteLocalFont,
        onClearImportError = viewModel::clearFontImportError,
        onDefaultProfileChange = viewModel::updateDefaultProfile,
        onThemeModeChange = viewModel::updateThemeMode,
        onLanguageChange = viewModel::updateLanguage,
        onRotationChange = viewModel::updateRotation,
        onFullscreenChange = viewModel::updateFullscreen,
        onTitleBarHideChange = viewModel::updateTitleBarHide,
        onPgUpDnGestureChange = viewModel::updatePgUpDnGesture,
        onVolumeFontChange = viewModel::updateVolumeFont,
        onKeepAliveChange = viewModel::updateKeepAlive,
        onAlwaysVisibleChange = viewModel::updateAlwaysVisible,
        onShiftFkeysChange = viewModel::updateShiftFkeys,
        onCtrlFkeysChange = viewModel::updateCtrlFkeys,
        onStickyModifiersChange = viewModel::updateStickyModifiers,
        onKeyModeChange = viewModel::updateKeyMode,
        onCameraChange = viewModel::updateCamera,
        onBumpyArrowsChange = viewModel::updateBumpyArrows,
        onBellChange = viewModel::updateBell,
        onBellVolumeChange = viewModel::updateBellVolume,
        onBellVibrateChange = viewModel::updateBellVibrate,
        onBellNotificationChange = viewModel::updateBellNotification,
        onExtendedKeyboardKeysChange = viewModel::updateExtendedKeyboardKeys,
        onExportClick = { showExportDialog = true },
        onImportClick = {
            importFilePicker.launch(
                arrayOf(
                    "application/json",
                    "application/octet-stream",
                    "*/*"
                )
            )
        },
        onAutoBackupEnabledChange = viewModel::updateAutoBackupEnabled,
        onAutoBackupRetentionChange = viewModel::updateAutoBackupRetention,
        onBackupNowClick = viewModel::runAutoBackupNow,
        onRestoreBackupClick = { showRestoreDialog = true },
        filterCategory = filterCategory,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onAuthOnLaunchChange: (Boolean) -> Unit,
    onMemkeysChange: (Boolean) -> Unit,
    onConnPersistChange: (Boolean) -> Unit,
    onWifilockChange: (Boolean) -> Unit,
    onBackupkeysChange: (Boolean) -> Unit,
    onScrollbackChange: (String) -> Unit,
    onBottomBarPresetChange: (String) -> Unit,
    onSessionKeyboardChange: (String) -> Unit,
    onOpenCustomBottomBarCreate: () -> Unit,
    onOpenCustomBottomBarEdit: (CustomBottomBarLayout) -> Unit,
    onShowBottomBarIconLegend: () -> Unit = {},
    onDeleteCustomBottomBarLayout: (String) -> Unit,
    onAddCustomTerminalType: (String) -> Unit,
    onRemoveCustomTerminalType: (String) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onAddCustomFont: (String) -> Unit,
    onRemoveCustomFont: (String) -> Unit,
    onClearFontError: () -> Unit,
    onImportLocalFont: (Uri, String) -> Unit,
    onDeleteLocalFont: (String) -> Unit,
    onClearImportError: () -> Unit,
    onDefaultProfileChange: (Long) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRotationChange: (String) -> Unit,
    onFullscreenChange: (Boolean) -> Unit,
    onTitleBarHideChange: (Boolean) -> Unit,
    onPgUpDnGestureChange: (Boolean) -> Unit,
    onVolumeFontChange: (Boolean) -> Unit,
    onKeepAliveChange: (Boolean) -> Unit,
    onAlwaysVisibleChange: (Boolean) -> Unit,
    onShiftFkeysChange: (Boolean) -> Unit,
    onCtrlFkeysChange: (Boolean) -> Unit,
    onStickyModifiersChange: (String) -> Unit,
    onKeyModeChange: (String) -> Unit,
    onCameraChange: (String) -> Unit,
    onBumpyArrowsChange: (Boolean) -> Unit,
    onBellChange: (Boolean) -> Unit,
    onBellVolumeChange: (Float) -> Unit,
    onBellVibrateChange: (Boolean) -> Unit,
    onBellNotificationChange: (Boolean) -> Unit,
    onExtendedKeyboardKeysChange: (Set<String>) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onAutoBackupEnabledChange: (Boolean) -> Unit = {},
    onAutoBackupRetentionChange: (Int) -> Unit = {},
    onBackupNowClick: () -> Unit = {},
    onRestoreBackupClick: () -> Unit = {},
    filterCategory: SettingsCategory? = null,
    modifier: Modifier = Modifier
) {
    // Returns true when the section with the given header-title string-resource
    // should be rendered. When [filterCategory] is null, show everything.
    val showSection: (Int) -> Boolean = { titleRes ->
        filterCategory == null || titleRes in filterCategory.sectionTitleRes
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (filterCategory != null) {
                            stringResource(filterCategory.titleRes)
                        } else {
                            stringResource(R.string.title_settings)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.canAuthenticate && showSection(R.string.settings_section_security)) {
                item {
                    SettingsSection(title = stringResource(R.string.settings_section_security)) {
                        SwitchPreference(
                            title = stringResource(R.string.pref_auth_on_launch_title),
                            summary = stringResource(R.string.pref_auth_on_launch_summary),
                            checked = uiState.authOnLaunch,
                            onCheckedChange = onAuthOnLaunchChange,
                            omitOuterCard = true
                        )
                    }
                }
            }

            if (showSection(R.string.settings_section_session)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_session)) {
                    SwitchPreference(
                        title = stringResource(R.string.pref_memkeys_title),
                        summary = stringResource(R.string.pref_memkeys_summary),
                        checked = uiState.memkeys,
                        onCheckedChange = onMemkeysChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_conn_persist_title),
                        summary = stringResource(R.string.pref_conn_persist_summary),
                        checked = uiState.connPersist,
                        onCheckedChange = onConnPersistChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_wifilock_title),
                        summary = stringResource(R.string.pref_wifilock_summary),
                        checked = uiState.wifilock,
                        onCheckedChange = onWifilockChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_backupkeys_title),
                        summary = stringResource(R.string.pref_backupkeys_summary),
                        checked = uiState.backupkeys,
                        onCheckedChange = onBackupkeysChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    TextPreference(
                        title = stringResource(R.string.pref_scrollback_title),
                        summary = stringResource(R.string.pref_scrollback_summary),
                        value = uiState.scrollback,
                        onValueChange = onScrollbackChange,
                        omitOuterCard = true
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_bottom_bar)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_bottom_bar)) {
                    val barEntries =
                        listOf(
                            stringResource(R.string.pref_terminal_bottom_bar_option_default) to "default",
                            stringResource(R.string.pref_terminal_bottom_bar_option_tmux) to "tmux"
                        ) + uiState.customBottomBarLayouts.map { layout ->
                            layout.name to "${TerminalBottomBarPreset.CUSTOM_PREFIX}${layout.id}"
                        }
                    ListPreference(
                        title = stringResource(R.string.pref_terminal_bottom_bar_title),
                        summary = terminalBottomBarPresetSummary(uiState.bottomBarPreset, uiState.customBottomBarLayouts),
                        value = uiState.bottomBarPreset,
                        entries = barEntries,
                        onValueChange = onBottomBarPresetChange,
                        leadingIcon = Icons.Default.Terminal,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    ListPreference(
                        title = stringResource(R.string.pref_session_keyboard_android_ime_title),
                        summary = when (uiState.sessionKeyboard) {
                            SessionKeyboardPolicy.STORED_ON -> stringResource(R.string.pref_session_keyboard_summary_on)
                            SessionKeyboardPolicy.STORED_OFF -> stringResource(R.string.pref_session_keyboard_summary_off)
                            else -> stringResource(R.string.pref_session_keyboard_summary_auto)
                        },
                        value = uiState.sessionKeyboard,
                        entries = listOf(
                            stringResource(R.string.pref_session_keyboard_option_auto) to SessionKeyboardPolicy.STORED_AUTO,
                            stringResource(R.string.pref_session_keyboard_option_on) to SessionKeyboardPolicy.STORED_ON,
                            stringResource(R.string.pref_session_keyboard_option_off) to SessionKeyboardPolicy.STORED_OFF
                        ),
                        onValueChange = onSessionKeyboardChange,
                        leadingIcon = Icons.Default.Keyboard,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    Text(
                        text = stringResource(R.string.pref_session_keyboard_android_ime_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    FilledTonalButton(
                        onClick = onOpenCustomBottomBarCreate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.settings_bottom_bar_new_layout))
                    }
                    Text(
                        text = stringResource(R.string.pref_custom_bottom_bar_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    OutlinedButton(
                        onClick = onShowBottomBarIconLegend,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.pref_bottom_bar_icon_legend_button))
                    }
                    if (uiState.customBottomBarLayouts.isNotEmpty()) {
                        GroupedItemDivider()
                    }
                    uiState.customBottomBarLayouts.forEachIndexed { index, layout ->
                        if (index > 0) {
                            GroupedItemDivider()
                        }
                        ListItem(
                            headlineContent = { Text(layout.name) },
                            supportingContent = {
                                Text(
                                    layout.actions.joinToString("  ") { it.displayLabel }
                                )
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onOpenCustomBottomBarEdit(layout) }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.pref_custom_bottom_bar_edit_title)
                                        )
                                    }
                                    IconButton(onClick = { onDeleteCustomBottomBarLayout(layout.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.pref_custom_bottom_bar_delete)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onOpenCustomBottomBarEdit(layout) }
                        )
                    }
                }
            }
            }

            if (showSection(R.string.settings_section_profiles)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_profiles)) {
                    val selectedProfile = if (uiState.defaultProfileId == 0L) {
                        null
                    } else {
                        uiState.availableProfiles.find { it.id == uiState.defaultProfileId }
                    }
                    val noneLabel = stringResource(R.string.pref_default_profile_none)
                    val profileEntries = listOf(noneLabel to "0") +
                        uiState.availableProfiles.map { it.name to it.id.toString() }
                    ListPreference(
                        title = stringResource(R.string.pref_default_profile_title),
                        summary = selectedProfile?.name ?: noneLabel,
                        value = uiState.defaultProfileId.toString(),
                        entries = profileEntries,
                        onValueChange = { onDefaultProfileChange(it.toLong()) },
                        omitOuterCard = true
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_display)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_display)) {
                    ListPreference(
                        title = "Theme",
                        summary = when (uiState.themeMode) {
                            "dark" -> "Dark"
                            "light" -> "Light"
                            else -> "Follow system"
                        },
                        value = uiState.themeMode,
                        entries = listOf(
                            "Follow system" to "auto",
                            "Dark" to "dark",
                            "Light" to "light"
                        ),
                        onValueChange = onThemeModeChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    val context = LocalContext.current
                    val systemDefaultLabel = stringResource(R.string.pref_language_system_default)
                    val languageEntries = remember {
                        listOf("" to systemDefaultLabel) + buildAvailableLanguageList(context)
                    }
                    val currentLabel = if (uiState.language.isEmpty()) {
                        systemDefaultLabel
                    } else {
                        languageEntries.find { it.first == uiState.language }?.second
                            ?: uiState.language
                    }
                    ListPreference(
                        title = stringResource(R.string.pref_language_title),
                        summary = currentLabel,
                        value = uiState.language,
                        entries = languageEntries.map { (tag, label) -> label to tag },
                        onValueChange = onLanguageChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    ListPreference(
                        title = stringResource(R.string.pref_rotation_title),
                        summary = when (uiState.rotation) {
                            "Default" -> stringResource(R.string.list_rotation_default)
                            "Force landscape" -> stringResource(R.string.list_rotation_land)
                            "Force portrait" -> stringResource(R.string.list_rotation_port)
                            "Automatic" -> stringResource(R.string.list_rotation_auto)
                            else -> uiState.rotation
                        },
                        value = uiState.rotation,
                        entries = listOf(
                            stringResource(R.string.list_rotation_default) to "Default",
                            stringResource(R.string.list_rotation_land) to "Force landscape",
                            stringResource(R.string.list_rotation_port) to "Force portrait",
                            stringResource(R.string.list_rotation_auto) to "Automatic"
                        ),
                        onValueChange = onRotationChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_fullscreen_title),
                        summary = stringResource(R.string.pref_fullscreen_summary),
                        checked = uiState.fullscreen,
                        onCheckedChange = onFullscreenChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_titlebarhide_title),
                        summary = stringResource(R.string.pref_titlebarhide_summary),
                        checked = uiState.titlebarhide,
                        onCheckedChange = onTitleBarHideChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_pg_updn_gesture_title),
                        summary = stringResource(R.string.pref_pg_updn_gesture_summary),
                        checked = uiState.pgupdngesture,
                        onCheckedChange = onPgUpDnGestureChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_volumefont_title),
                        summary = stringResource(R.string.pref_volumefont_summary),
                        checked = uiState.volumefont,
                        onCheckedChange = onVolumeFontChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_keepalive_title),
                        summary = stringResource(R.string.pref_keepalive_summary),
                        checked = uiState.keepalive,
                        onCheckedChange = onKeepAliveChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    val presetEntries = if (BuildConfig.HAS_DOWNLOADABLE_FONTS) {
                        TerminalFont.entries.map { it.displayName to it.name }
                    } else {
                        listOf(TerminalFont.SYSTEM_DEFAULT.displayName to TerminalFont.SYSTEM_DEFAULT.name)
                    }
                    val customEntries = if (BuildConfig.HAS_DOWNLOADABLE_FONTS) {
                        uiState.customFonts.map { it to TerminalFont.createCustomFontValue(it) }
                    } else {
                        emptyList()
                    }
                    val localEntries = uiState.localFonts.map { (displayName, fileName) ->
                        displayName to LocalFontProvider.createLocalFontValue(fileName)
                    }
                    val allEntries = presetEntries + customEntries + localEntries
                    ListPreference(
                        title = stringResource(R.string.pref_fontfamily_title),
                        summary = getLocalizedFontDisplayName(uiState.fontFamily),
                        value = uiState.fontFamily,
                        entries = allEntries,
                        onValueChange = onFontFamilyChange,
                        leadingIcon = Icons.Default.FontDownload,
                        omitOuterCard = true
                    )
                    if (BuildConfig.HAS_DOWNLOADABLE_FONTS) {
                        GroupedItemDivider()
                        AddCustomFontPreference(
                            customFonts = uiState.customFonts,
                            validationInProgress = uiState.fontValidationInProgress,
                            validationError = uiState.fontValidationError,
                            onAddFont = onAddCustomFont,
                            onRemoveFont = onRemoveCustomFont,
                            onClearError = onClearFontError,
                            omitOuterCard = true
                        )
                    }
                    GroupedItemDivider()
                    LocalFontPreference(
                        localFonts = uiState.localFonts,
                        importInProgress = uiState.fontImportInProgress,
                        importError = uiState.fontImportError,
                        onImportFont = onImportLocalFont,
                        onDeleteFont = onDeleteLocalFont,
                        onClearError = onClearImportError,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    AddCustomTerminalTypePreference(
                        customTerminalTypes = uiState.customTerminalTypes,
                        onAddTerminalType = onAddCustomTerminalType,
                        onRemoveTerminalType = onRemoveCustomTerminalType,
                        omitOuterCard = true
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_keyboard)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_keyboard)) {
                    SwitchPreference(
                        title = stringResource(R.string.pref_alwaysvisible_title),
                        summary = stringResource(R.string.pref_alwaysvisible_summary),
                        checked = uiState.alwaysvisible,
                        onCheckedChange = onAlwaysVisibleChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_shiftfkeys_title),
                        summary = stringResource(R.string.pref_shiftfkeys_summary),
                        checked = uiState.shiftfkeys,
                        onCheckedChange = onShiftFkeysChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_ctrlfkeys_title),
                        summary = stringResource(R.string.pref_ctrlfkeys_summary),
                        checked = uiState.ctrlfkeys,
                        onCheckedChange = onCtrlFkeysChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    ExtendedKeyboardKeysPreference(
                        enabledKeyIds = uiState.extendedKeyboardKeys,
                        onKeysChange = onExtendedKeyboardKeysChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    ListPreference(
                        title = stringResource(R.string.pref_stickymodifiers_title),
                        summary = when (uiState.stickymodifiers) {
                            "no" -> stringResource(R.string.no)
                            "alt" -> stringResource(R.string.only_alt)
                            "yes" -> stringResource(R.string.yes)
                            else -> uiState.stickymodifiers
                        },
                        value = uiState.stickymodifiers,
                        entries = listOf(
                            stringResource(R.string.no) to "no",
                            stringResource(R.string.only_alt) to "alt",
                            stringResource(R.string.yes) to "yes"
                        ),
                        onValueChange = onStickyModifiersChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    ListPreference(
                        title = stringResource(R.string.pref_keymode_title),
                        summary = when (uiState.keymode) {
                            "Use right-side keys" -> stringResource(R.string.list_keymode_right)
                            "Use left-side keys" -> stringResource(R.string.list_keymode_left)
                            "none" -> stringResource(R.string.list_keymode_none)
                            else -> uiState.keymode
                        },
                        value = uiState.keymode,
                        entries = listOf(
                            stringResource(R.string.list_keymode_right) to "Use right-side keys",
                            stringResource(R.string.list_keymode_left) to "Use left-side keys",
                            stringResource(R.string.list_keymode_none) to "none"
                        ),
                        onValueChange = onKeyModeChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    val cameraSummary = when (uiState.camera) {
                        "Ctrl+A then Space" -> stringResource(R.string.list_camera_ctrlaspace_description)
                        "Ctrl+A" -> stringResource(R.string.list_camera_ctrla_description)
                        "Esc" -> stringResource(R.string.list_camera_esc_description)
                        "Esc+A" -> stringResource(R.string.list_camera_esc_a_description)
                        "None" -> stringResource(R.string.list_camera_none_description)
                        "text_input" -> stringResource(R.string.list_camera_text_input_description)
                        else -> uiState.camera
                    }
                    ListPreference(
                        title = stringResource(R.string.pref_camera_title),
                        summary = cameraSummary,
                        value = uiState.camera,
                        entries = listOf(
                            stringResource(R.string.list_camera_ctrlaspace) to "Ctrl+A then Space",
                            stringResource(R.string.list_camera_ctrla) to "Ctrl+A",
                            stringResource(R.string.list_camera_esc) to "Esc",
                            stringResource(R.string.list_camera_esc_a) to "Esc+A",
                            stringResource(R.string.list_camera_none) to "None",
                            stringResource(R.string.list_camera_text_input) to "text_input"
                        ),
                        onValueChange = onCameraChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_bumpyarrows_title),
                        summary = stringResource(R.string.pref_bumpyarrows_summary),
                        checked = uiState.bumpyarrows,
                        onCheckedChange = onBumpyArrowsChange,
                        omitOuterCard = true
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_bell)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_bell)) {
                    SwitchPreference(
                        title = stringResource(R.string.pref_bell_title),
                        summary = stringResource(R.string.pref_bell_summary),
                        checked = uiState.bell,
                        onCheckedChange = onBellChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SliderPreference(
                        title = stringResource(R.string.pref_bell_volume_title),
                        value = uiState.bellVolume,
                        onValueChange = onBellVolumeChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_bell_vibrate_title),
                        summary = stringResource(R.string.pref_bell_vibrate_summary),
                        checked = uiState.bellVibrate,
                        onCheckedChange = onBellVibrateChange,
                        omitOuterCard = true
                    )
                    GroupedItemDivider()
                    SwitchPreference(
                        title = stringResource(R.string.pref_bell_notification_title),
                        summary = stringResource(R.string.pref_bell_notification_summary),
                        checked = uiState.bellNotification,
                        onCheckedChange = onBellNotificationChange,
                        omitOuterCard = true
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_data)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_data)) {
                    SwitchPreference(
                        title = stringResource(R.string.pref_auto_backup_title),
                        summary = stringResource(R.string.pref_auto_backup_summary),
                        checked = uiState.autoBackupEnabled,
                        onCheckedChange = onAutoBackupEnabledChange,
                        omitOuterCard = true
                    )
                    if (uiState.autoBackupEnabled) {
                        GroupedItemDivider()
                        val retentionEntries = listOf(
                            "3" to "3",
                            "5" to "5",
                            "7" to "7",
                            "10" to "10"
                        )
                        ListPreference(
                            title = stringResource(R.string.pref_auto_backup_retention_title),
                            summary = uiState.autoBackupRetention.toString(),
                            value = uiState.autoBackupRetention.toString(),
                            entries = retentionEntries,
                            onValueChange = { onAutoBackupRetentionChange(it.toInt()) },
                            omitOuterCard = true
                        )
                        GroupedItemDivider()
                        val lastBackupText = if (uiState.autoBackupLastTime > 0L) {
                            val instant = java.time.Instant.ofEpochMilli(uiState.autoBackupLastTime)
                            val formatter = java.time.format.DateTimeFormatter
                                .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
                                .withZone(java.time.ZoneId.systemDefault())
                            stringResource(R.string.pref_auto_backup_last, formatter.format(instant))
                        } else {
                            stringResource(R.string.pref_auto_backup_never)
                        }
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.pref_auto_backup_now)) },
                            supportingContent = {
                                Text(
                                    lastBackupText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable(
                                enabled = !uiState.autoBackupInProgress,
                                onClick = onBackupNowClick
                            )
                        )
                        GroupedItemDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.pref_auto_backup_restore)) },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.pref_auto_backup_restore_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable(onClick = onRestoreBackupClick)
                        )
                    }
                    GroupedItemDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.pref_export_title)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.pref_export_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable(onClick = onExportClick)
                    )
                    GroupedItemDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.pref_import_title)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.pref_import_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable(onClick = onImportClick)
                    )
                }
            }
            }

            if (showSection(R.string.settings_section_about)) {
            item {
                SettingsSection(title = stringResource(R.string.settings_section_about)) {
                    val context = LocalContext.current
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.about_engine),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    GroupedItemDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.about_branding)) }
                    )
                    GroupedItemDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.about_license)) },
                        modifier = Modifier.clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(context.getString(R.string.about_license_url))
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (uiState.fontDownloadInProgress) {
        FontDownloadProgressDialog()
    }
}

private fun buildAvailableLanguageList(context: android.content.Context): List<Pair<String, String>> {
    val localeTags = mutableListOf<String>()
    val parser = context.resources.getXml(R.xml._generated_res_locale_config)
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
            val tag = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android",
                "name"
            )
            if (tag != null) {
                localeTags.add(tag)
            }
        }
    }
    parser.close()
    return localeTags
        .map { tag ->
            val locale = Locale.forLanguageTag(tag)
            val canonicalTag = locale.toLanguageTag()
            val nativeName = locale.getDisplayName(locale)
                .replaceFirstChar { it.titlecase(locale) }
            canonicalTag to nativeName
        }
        .distinctBy { it.first }
        .sortedBy { it.second.lowercase() }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = SapienTheme.tokens
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = tokens.textMuted,
            modifier = Modifier.padding(horizontal = 4.dp).padding(top = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(tokens.cornerMedium),
            colors = CardDefaults.cardColors(
                containerColor = tokens.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun GroupedItemDivider() {
    val tokens = SapienTheme.tokens
    HorizontalDivider(
        color = tokens.surfaceBorder.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SwitchPreference(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    val tokens = SapienTheme.tokens
    val row: @Composable () -> Unit = {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            headlineContent = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.textPrimary
                )
            },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = tokens.textOnPrimary,
                        checkedTrackColor = tokens.primary,
                        uncheckedThumbColor = tokens.textMuted,
                        uncheckedTrackColor = tokens.surface,
                        uncheckedBorderColor = tokens.surfaceBorder
                    )
                )
            },
            modifier = Modifier.clickable { onCheckedChange(!checked) }
        )
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { row() }
    } else {
        Card(
            shape = RoundedCornerShape(tokens.cornerMedium),
            colors = CardDefaults.cardColors(containerColor = tokens.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            row()
        }
    }
}

@Composable
private fun TextPreference(
    title: String,
    summary: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    val tokens = SapienTheme.tokens
    var showDialog by remember { mutableStateOf(false) }

    val row: @Composable () -> Unit = {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            headlineContent = {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tokens.textPrimary)
            },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textMuted
                )
            },
            modifier = Modifier.clickable { showDialog = true }
        )
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { row() }
    } else {
        Card(
            shape = RoundedCornerShape(tokens.cornerMedium),
            colors = CardDefaults.cardColors(containerColor = tokens.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            row()
        }
    }

    if (showDialog) {
        TextPreferenceDialog(
            title = title,
            value = value,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TextPreferenceDialog(
    title: String,
    value: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var textValue by remember { mutableStateOf(value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textValue) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}

@Composable
private fun ListPreference(
    title: String,
    summary: String,
    value: String,
    entries: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    omitOuterCard: Boolean = false
) {
    val tokens = SapienTheme.tokens
    var showDialog by remember { mutableStateOf(false) }

    val row: @Composable () -> Unit = {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            leadingContent = {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = tokens.primary
                    )
                }
            },
            headlineContent = {
                Text(title, style = MaterialTheme.typography.titleSmall, color = tokens.textPrimary)
            },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.primary
                )
            },
            modifier = Modifier.clickable { showDialog = true }
        )
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { row() }
    } else {
        Card(
            shape = RoundedCornerShape(tokens.cornerMedium),
            colors = CardDefaults.cardColors(containerColor = tokens.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            row()
        }
    }

    if (showDialog) {
        ListPreferenceDialog(
            title = title,
            value = value,
            entries = entries,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ListPreferenceDialog(
    title: String,
    value: String,
    entries: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entries.forEach { (label, entryValue) ->
                    val isSelected = entryValue == value
                    Surface(
                        onClick = { onConfirm(entryValue) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onConfirm(entryValue) }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}

@Composable
private fun ListPreferenceWithCustom(
    title: String,
    summary: String,
    value: String,
    entries: List<Pair<String, String>>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    customLabel: String = "Custom..."
) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable { showDialog = true }
        )
    }

    if (showDialog) {
        ListPreferenceWithCustomDialog(
            title = title,
            value = value,
            entries = entries,
            customLabel = customLabel,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onValueChange(newValue)
                showDialog = false
            }
        )
    }
}

@Composable
private fun ListPreferenceWithCustomDialog(
    title: String,
    value: String,
    entries: List<Pair<String, String>>,
    customLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customValue by remember { mutableStateOf(value) }

    if (showCustomInput) {
        AlertDialog(
            onDismissRequest = {
                showCustomInput = false
                onDismiss()
            },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it },
                    label = { Text(stringResource(R.string.dialog_custom_value_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customValue.isNotBlank()) {
                            onConfirm(customValue)
                        }
                    },
                    enabled = customValue.isNotBlank()
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCustomInput = false
                    onDismiss()
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    entries.forEach { (label, entryValue) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            modifier = Modifier.clickable { onConfirm(entryValue) }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ListItem(
                        headlineContent = {
                            Text(
                                text = customLabel,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            customValue = value
                            showCustomInput = true
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.delete_neg))
                }
            }
        )
    }
}

@Composable
private fun SliderPreference(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    val sliderBody: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${(value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { sliderBody() }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            sliderBody()
        }
    }
}

@Composable
private fun AddCustomTerminalTypePreference(
    customTerminalTypes: List<String>,
    onAddTerminalType: (String) -> Unit,
    onRemoveTerminalType: (String) -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newTerminalType by remember { mutableStateOf("") }

    val columnContent: @Composable () -> Unit = {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_customterminal_title)) },
                supportingContent = {
                    Text(
                        stringResource(R.string.pref_customterminal_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showAddDialog = true }
            )

            customTerminalTypes.forEach { terminalType ->
                ListItem(
                    headlineContent = { Text(terminalType) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onRemoveTerminalType(terminalType) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.button_remove)
                            )
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { columnContent() }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            columnContent()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                newTerminalType = ""
            },
            title = { Text(stringResource(R.string.dialog_customterminal_title)) },
            text = {
                OutlinedTextField(
                    value = newTerminalType,
                    onValueChange = { newTerminalType = it },
                    label = { Text(stringResource(R.string.dialog_customterminal_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTerminalType.isNotBlank()) {
                            onAddTerminalType(newTerminalType.trim())
                            showAddDialog = false
                            newTerminalType = ""
                        }
                    },
                    enabled = newTerminalType.isNotBlank()
                ) {
                    Text(stringResource(R.string.button_add))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    newTerminalType = ""
                }) {
                    Text(stringResource(R.string.delete_neg))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomFontPreference(
    customFonts: List<String>,
    validationInProgress: Boolean,
    validationError: String?,
    onAddFont: (String) -> Unit,
    onRemoveFont: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newFontName by remember { mutableStateOf("") }

    // Show error snackbar if there's an error
    LaunchedEffect(validationError) {
        if (validationError != null) {
            // Error is shown in dialog, will be cleared when dialog closes
        }
    }

    val fontColumn: @Composable () -> Unit = {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_customfont_title)) },
                supportingContent = {
                    Text(
                        stringResource(R.string.pref_customfont_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable { showAddDialog = true }
            )

            customFonts.forEach { fontName ->
                ListItem(
                    headlineContent = { Text(fontName) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onRemoveFont(fontName) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.button_remove)
                            )
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { fontColumn() }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            fontColumn()
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!validationInProgress) {
                    showAddDialog = false
                    newFontName = ""
                    onClearError()
                }
            },
            title = { Text(stringResource(R.string.dialog_customfont_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFontName,
                        onValueChange = {
                            newFontName = it
                            onClearError()
                        },
                        label = { Text(stringResource(R.string.dialog_customfont_hint)) },
                        singleLine = true,
                        enabled = !validationInProgress,
                        isError = validationError != null,
                        supportingText = if (validationError != null) {
                            { Text(validationError, color = MaterialTheme.colorScheme.error) }
                        } else if (validationInProgress) {
                            { Text(stringResource(R.string.font_validating)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFontName.isNotBlank()) {
                            onAddFont(newFontName.trim())
                        }
                    },
                    enabled = newFontName.isNotBlank() && !validationInProgress
                ) {
                    Text(stringResource(R.string.button_add))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newFontName = ""
                        onClearError()
                    },
                    enabled = !validationInProgress
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Close dialog when font is successfully added
    LaunchedEffect(customFonts.size) {
        if (showAddDialog && !validationInProgress && validationError == null && newFontName.isNotBlank()) {
            // Font was added successfully
            showAddDialog = false
            newFontName = ""
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalFontPreference(
    localFonts: List<Pair<String, String>>,
    importInProgress: Boolean,
    importError: String?,
    onImportFont: (Uri, String) -> Unit,
    onDeleteFont: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }
    var fontDisplayName by remember { mutableStateOf("") }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingUri = uri
            fontDisplayName = ""
            showNameDialog = true
        }
    }

    val localColumn: @Composable () -> Unit = {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.pref_localfont_title)) },
                supportingContent = {
                    Text(
                        if (importInProgress) {
                            stringResource(R.string.font_importing)
                        } else {
                            stringResource(R.string.pref_localfont_summary)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.clickable(enabled = !importInProgress) {
                    fontPickerLauncher.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-otf"))
                }
            )

            localFonts.forEach { (displayName, fileName) ->
                ListItem(
                    headlineContent = { Text(displayName) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = { onDeleteFont(fileName) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.button_remove)
                            )
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { localColumn() }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            localColumn()
        }
    }

    // Dialog to get display name for imported font
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!importInProgress) {
                    showNameDialog = false
                    pendingUri = null
                    fontDisplayName = ""
                    onClearError()
                }
            },
            title = { Text(stringResource(R.string.dialog_localfont_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = fontDisplayName,
                        onValueChange = {
                            fontDisplayName = it
                            onClearError()
                        },
                        label = { Text(stringResource(R.string.dialog_localfont_hint)) },
                        singleLine = true,
                        enabled = !importInProgress,
                        isError = importError != null,
                        supportingText = if (importError != null) {
                            { Text(importError, color = MaterialTheme.colorScheme.error) }
                        } else if (importInProgress) {
                            { Text(stringResource(R.string.font_importing)) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUri?.let { uri ->
                            if (fontDisplayName.isNotBlank()) {
                                onImportFont(uri, fontDisplayName.trim())
                            }
                        }
                    },
                    enabled = fontDisplayName.isNotBlank() && !importInProgress
                ) {
                    Text(stringResource(R.string.button_import))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNameDialog = false
                        pendingUri = null
                        fontDisplayName = ""
                        onClearError()
                    },
                    enabled = !importInProgress
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    // Close dialog when font is successfully imported
    LaunchedEffect(localFonts.size) {
        if (showNameDialog && !importInProgress && importError == null && fontDisplayName.isNotBlank()) {
            showNameDialog = false
            pendingUri = null
            fontDisplayName = ""
        }
    }
}

@Composable
private fun terminalBottomBarPresetSummary(value: String, layouts: List<CustomBottomBarLayout>): String {
    val v = value.trim()
    if (v.startsWith(TerminalBottomBarPreset.CUSTOM_PREFIX)) {
        val id = v.removePrefix(TerminalBottomBarPreset.CUSTOM_PREFIX)
        return layouts.find { it.id == id }?.name ?: v
    }
    return when (v) {
        "tmux", "zellij", "multiplexer" -> stringResource(R.string.pref_terminal_bottom_bar_summary_tmux)
        else -> stringResource(R.string.pref_terminal_bottom_bar_summary_default)
    }
}

@PreviewScreen
@Composable
private fun SettingsScreenPreview() {
    SapienTermTheme {
        SettingsScreenContent(
            uiState = SettingsUiState(
                authOnLaunch = false,
                canAuthenticate = true,
                memkeys = true,
                connPersist = true,
                wifilock = false,
                backupkeys = true,
                scrollback = "500",
                rotation = "Default",
                titlebarhide = false,
                fullscreen = true,
                pgupdngesture = true,
                volumefont = true,
                keepalive = true,
                alwaysvisible = true,
                shiftfkeys = false,
                ctrlfkeys = false,
                stickymodifiers = "yes",
                keymode = "Use right-side keys",
                camera = "Ctrl+A then Space",
                bumpyarrows = true,
                bell = true,
                bellVolume = 0.75f,
                bellVibrate = true,
                bellNotification = false,
                fontFamily = "JETBRAINS_MONO",
                customFonts = listOf("Cascadia Code", "Hack"),
                customTerminalTypes = listOf("rxvt-unicode", "tmux-256color"),
                localFonts = listOf("My Custom Font" to "my_custom_font.ttf"),
                fontValidationInProgress = false,
                fontValidationError = null,
                fontImportInProgress = false,
                fontImportError = null,
                bottomBarPreset = "default",
                sessionKeyboard = SessionKeyboardPolicy.STORED_AUTO,
                customBottomBarLayouts = emptyList()
            ),
            onNavigateBack = {},
            onAuthOnLaunchChange = {},
            onMemkeysChange = {},
            onConnPersistChange = {},
            onWifilockChange = {},
            onBackupkeysChange = {},
            onScrollbackChange = {},
            onBottomBarPresetChange = {},
            onSessionKeyboardChange = {},
            onOpenCustomBottomBarCreate = {},
            onOpenCustomBottomBarEdit = {},
            onShowBottomBarIconLegend = {},
            onDeleteCustomBottomBarLayout = {},
            onAddCustomTerminalType = {},
            onRemoveCustomTerminalType = {},
            onFontFamilyChange = {},
            onAddCustomFont = {},
            onRemoveCustomFont = {},
            onClearFontError = {},
            onImportLocalFont = { _, _ -> },
            onDeleteLocalFont = {},
            onClearImportError = {},
            onDefaultProfileChange = {},
            onThemeModeChange = {},
            onLanguageChange = {},
            onRotationChange = {},
            onFullscreenChange = {},
            onTitleBarHideChange = {},
            onPgUpDnGestureChange = {},
            onVolumeFontChange = {},
            onKeepAliveChange = {},
            onAlwaysVisibleChange = {},
            onShiftFkeysChange = {},
            onCtrlFkeysChange = {},
            onStickyModifiersChange = {},
            onKeyModeChange = {},
            onCameraChange = {},
            onBumpyArrowsChange = {},
            onBellChange = {},
            onBellVolumeChange = {},
            onBellVibrateChange = {},
            onBellNotificationChange = {},
            onExtendedKeyboardKeysChange = {},
            onExportClick = {},
            onImportClick = {}
        )
    }
}

@Composable
private fun ExtendedKeyboardKeysPreference(
    enabledKeyIds: Set<String>,
    onKeysChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    omitOuterCard: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }

    // Build summary from enabled keys
    val enabledCount = enabledKeyIds.size
    val totalCount = ExtendedKey.entries.size
    val summary = if (enabledCount == totalCount) {
        stringResource(R.string.pref_extended_keyboard_summary)
    } else {
        "$enabledCount / $totalCount keys enabled"
    }

    val listItem: @Composable () -> Unit = {
        ListItem(
            headlineContent = { Text(stringResource(R.string.pref_extended_keyboard_title)) },
            supportingContent = {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable { showDialog = true }
        )
    }
    if (omitOuterCard) {
        Column(modifier = modifier) { listItem() }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = modifier
        ) {
            listItem()
        }
    }

    if (showDialog) {
        // Local mutable copy of enabled keys for the dialog
        var selectedKeys by remember { mutableStateOf(enabledKeyIds.toMutableSet()) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.pref_extended_keyboard_title)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    ExtendedKey.entries.forEach { key ->
                        val isChecked = key.id in selectedKeys
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedKeys = selectedKeys.toMutableSet().apply {
                                        if (isChecked) remove(key.id) else add(key.id)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedKeys = selectedKeys.toMutableSet().apply {
                                        if (checked) add(key.id) else remove(key.id)
                                    }
                                }
                            )
                            Text(
                                text = key.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onKeysChange(selectedKeys.toSet())
                    showDialog = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun NotificationPermissionDeniedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_permission_denied_title)) },
        text = { Text(stringResource(R.string.notification_permission_denied_message)) },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun AutoBackupRestoreDialog(
    backupFiles: List<com.logicalsapien.sapienterm.data.export.BackupFileInfo>,
    onRestore: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFile by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_backup_restore_title)) },
        text = {
            if (backupFiles.isEmpty()) {
                Text(stringResource(R.string.auto_backup_no_backups))
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    backupFiles.forEach { backup ->
                        val instant = java.time.Instant.ofEpochMilli(backup.timestamp)
                        val formatter = java.time.format.DateTimeFormatter
                            .ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM)
                            .withZone(java.time.ZoneId.systemDefault())
                        val dateStr = formatter.format(instant)
                        val sizeStr = android.text.format.Formatter.formatShortFileSize(
                            LocalContext.current,
                            backup.sizeBytes
                        )
                        val isSelected = selectedFile == backup.fileName

                        Surface(
                            onClick = { selectedFile = backup.fileName },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFile = backup.fileName }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Text(
                                        text = sizeStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.auto_backup_restore_confirm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedFile?.let { onRestore(it) } },
                enabled = selectedFile != null
            ) {
                Text(stringResource(R.string.import_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_neg))
            }
        }
    )
}
