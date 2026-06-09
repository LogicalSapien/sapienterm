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

package com.logicalsapien.sapienterm.ui.screens.colors

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.ColorSchemePresets
import com.logicalsapien.sapienterm.data.entity.ColorScheme
import com.logicalsapien.sapienterm.ui.PreviewScreen
import com.logicalsapien.sapienterm.ui.common.getLocalizedColorSchemeDescription
import com.logicalsapien.sapienterm.ui.theme.SapienTermTheme

/**
 * Screen for managing color schemes (create, duplicate, delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToPaletteEditor: (Long) -> Unit = {},
    viewModel: ColorSchemeManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val repository = viewModel.repository
    val scope = rememberCoroutineScope()

    var exportingSchemeId by remember { mutableLongStateOf(-1L) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    val schemeJson = repository.exportScheme(exportingSchemeId)
                    context.contentResolver.openOutputStream(fileUri)?.use { output ->
                        output.write(schemeJson.toJson().toByteArray())
                    }
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.message_export_success,
                            schemeJson.name
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.error_export_failed,
                            e.message
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { fileUri ->
            scope.launch {
                try {
                    val jsonString =
                        context.contentResolver.openInputStream(fileUri)?.use { input ->
                            input.bufferedReader().readText()
                        } ?: return@launch

                    val schemeId =
                        repository.importScheme(jsonString, allowOverwrite = false)
                    val schemes = repository.getAllSchemes()
                    val importedScheme = schemes.find { it.id == schemeId }

                    viewModel.refresh()

                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.message_import_success,
                            importedScheme?.name ?: "scheme"
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: org.json.JSONException) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_invalid_json),
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.error_import_failed,
                            e.message
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    ColorsScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToPaletteEditor = onNavigateToPaletteEditor,
        onExportScheme = { schemeId ->
            exportingSchemeId = schemeId
            scope.launch {
                try {
                    val schemes = repository.getAllSchemes()
                    val scheme = schemes.find { it.id == schemeId }
                    val fileName = "${scheme?.name?.replace(" ", "_") ?: "scheme"}.json"
                    exportLauncher.launch(fileName)
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.error_export_failed,
                            e.message
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        },
        onImportScheme = {
            importLauncher.launch(arrayOf("application/json", "text/plain"))
        },
        onShowNewSchemeDialog = viewModel::showNewSchemeDialog,
        onClearError = viewModel::clearError,
        onSelectScheme = viewModel::selectScheme,
        onShowDeleteDialog = viewModel::showDeleteDialog,
        onCreateNewScheme = viewModel::createNewScheme,
        onHideNewSchemeDialog = viewModel::hideNewSchemeDialog,
        onDeleteScheme = viewModel::deleteScheme,
        onHideDeleteDialog = viewModel::hideDeleteDialog
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorsScreenContent(
    uiState: SchemeManagerUiState,
    onNavigateBack: () -> Unit,
    onNavigateToPaletteEditor: (Long) -> Unit,
    onExportScheme: (Long) -> Unit,
    onImportScheme: () -> Unit,
    onShowNewSchemeDialog: () -> Unit,
    onClearError: () -> Unit,
    onSelectScheme: (Long) -> Unit,
    onShowDeleteDialog: () -> Unit,
    onCreateNewScheme: (String, String, Long) -> Unit,
    onHideNewSchemeDialog: () -> Unit,
    onDeleteScheme: (Long) -> Unit,
    onHideDeleteDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnClearError by rememberUpdatedState(onClearError)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                withDismissAction = true
            )
            currentOnClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_scheme_manager)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.button_navigate_up)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onImportScheme) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(R.string.button_import_scheme)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowNewSchemeDialog,
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.button_new_scheme)
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.schemes.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.empty_custom_schemes),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    val fallbackPalette = ColorSchemePresets.default.colors
                    val fallbackFg = ColorSchemePresets.default.defaultFg
                    val fallbackBg = ColorSchemePresets.default.defaultBg
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 104.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.schemes) { scheme ->
                            SchemeItem(
                                scheme = scheme,
                                paletteColors = uiState.schemePreviewColors[scheme.id]
                                    ?: fallbackPalette,
                                previewDefaults = uiState.schemePreviewDefaults[scheme.id]
                                    ?: Pair(fallbackFg, fallbackBg),
                                onClick = { onNavigateToPaletteEditor(scheme.id) },
                                onExport = { onExportScheme(scheme.id) },
                                onDelete = {
                                    if (!scheme.isBuiltIn) {
                                        onSelectScheme(scheme.id)
                                        onShowDeleteDialog()
                                    }
                                },
                                onDuplicate = {
                                    onSelectScheme(scheme.id)
                                    onShowNewSchemeDialog()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showNewSchemeDialog) {
        val selectedScheme = uiState.selectedSchemeId?.let { id ->
            uiState.schemes.find { it.id == id }
        }
        NewSchemeDialog(
            availableSchemes = uiState.schemes,
            preselectedSchemeId = uiState.selectedSchemeId,
            suggestedName = selectedScheme?.let { "Copy of ${it.name}" },
            error = uiState.dialogError,
            onConfirm = { name, description, baseSchemeId ->
                onCreateNewScheme(name, description, baseSchemeId)
            },
            onDismiss = onHideNewSchemeDialog
        )
    }

    if (uiState.showDeleteDialog && uiState.selectedSchemeId != null) {
        val scheme = uiState.schemes.find { it.id == uiState.selectedSchemeId }
        if (scheme != null) {
            DeleteSchemeDialog(
                schemeName = scheme.name,
                onConfirm = { onDeleteScheme(scheme.id) },
                onDismiss = onHideDeleteDialog
            )
        }
    }
}

@PreviewScreen
@Composable
private fun ColorsScreenEmptyPreview() {
    SapienTermTheme {
        ColorsScreenContent(
            uiState = SchemeManagerUiState(
                schemes = emptyList(),
                isLoading = false
            ),
            onNavigateBack = {},
            onNavigateToPaletteEditor = {},
            onExportScheme = {},
            onImportScheme = {},
            onShowNewSchemeDialog = {},
            onClearError = {},
            onSelectScheme = {},
            onShowDeleteDialog = {},
            onCreateNewScheme = { _, _, _ -> },
            onHideNewSchemeDialog = {},
            onDeleteScheme = {},
            onHideDeleteDialog = {}
        )
    }
}

@PreviewScreen
@Composable
private fun ColorsScreenLoadingPreview() {
    SapienTermTheme {
        ColorsScreenContent(
            uiState = SchemeManagerUiState(
                schemes = emptyList(),
                isLoading = true
            ),
            onNavigateBack = {},
            onNavigateToPaletteEditor = {},
            onExportScheme = {},
            onImportScheme = {},
            onShowNewSchemeDialog = {},
            onClearError = {},
            onSelectScheme = {},
            onShowDeleteDialog = {},
            onCreateNewScheme = { _, _, _ -> },
            onHideNewSchemeDialog = {},
            onDeleteScheme = {},
            onHideDeleteDialog = {}
        )
    }
}

@PreviewScreen
@Composable
private fun ColorsScreenPopulatedPreview() {
    val solarized = ColorSchemePresets.solarizedDark
    val monokai = ColorSchemePresets.monokai
    val gruvbox = ColorSchemePresets.gruvboxDark
    SapienTermTheme {
        ColorsScreenContent(
            uiState = SchemeManagerUiState(
                schemes = listOf(
                    ColorScheme(
                        id = 1,
                        name = "Solarized Dark",
                        description = "Popular dark theme",
                        isBuiltIn = true
                    ),
                    ColorScheme(
                        id = 2,
                        name = "Monokai",
                        description = "Vibrant color scheme",
                        isBuiltIn = true
                    ),
                    ColorScheme(
                        id = 3,
                        name = "My Custom Theme",
                        description = "Personal customization",
                        isBuiltIn = false
                    )
                ),
                isLoading = false,
                schemePreviewColors = mapOf(
                    1L to solarized.colors,
                    2L to monokai.colors,
                    3L to gruvbox.colors
                ),
                schemePreviewDefaults = mapOf(
                    1L to Pair(solarized.defaultFg, solarized.defaultBg),
                    2L to Pair(monokai.defaultFg, monokai.defaultBg),
                    3L to Pair(gruvbox.defaultFg, gruvbox.defaultBg)
                )
            ),
            onNavigateBack = {},
            onNavigateToPaletteEditor = {},
            onExportScheme = {},
            onImportScheme = {},
            onShowNewSchemeDialog = {},
            onClearError = {},
            onSelectScheme = {},
            onShowDeleteDialog = {},
            onCreateNewScheme = { _, _, _ -> },
            onHideNewSchemeDialog = {},
            onDeleteScheme = {},
            onHideDeleteDialog = {}
        )
    }
}

private fun schemePreviewPaletteColor(palette: IntArray, index: Int): Color {
    if (palette.isEmpty()) return Color.Black
    val i = index.coerceIn(0, palette.lastIndex.coerceAtLeast(0))
    return Color(palette[i])
}

/**
 * Compact terminal sample using the scheme palette (same roles as [PaletteEditorScreen] preview).
 */
@Composable
private fun SchemeMiniTerminalPreview(
    palette: IntArray,
    foregroundColorIndex: Int,
    backgroundColorIndex: Int,
    modifier: Modifier = Modifier
) {
    fun c(index: Int) = SpanStyle(color = schemePreviewPaletteColor(palette, index))
    val fg = c(foregroundColorIndex.coerceIn(0, 15))
    val red = c(1)
    val green = c(2)
    val yellow = c(3)
    val blue = c(4)
    val cyan = c(6)
    val brightGreen = c(10)

    val lines: List<AnnotatedString> = listOf(
        buildAnnotatedString {
            withStyle(green) { append("user") }
            withStyle(fg) { append("@") }
            withStyle(cyan) { append("host") }
            withStyle(fg) { append(":") }
            withStyle(blue) { append("~/proj") }
            withStyle(fg) { append("$ ") }
            withStyle(fg) { append("ls") }
        },
        buildAnnotatedString {
            withStyle(fg) { append("-rw-r--r--  ") }
            withStyle(brightGreen) { append("README.md") }
        },
        buildAnnotatedString {
            withStyle(fg) { append("  modified:  ") }
            withStyle(yellow) { append("src/Main.kt") }
        },
        buildAnnotatedString {
            withStyle(red) { append("error: ") }
            withStyle(fg) { append("not found") }
        }
    )

    val bgIdx = backgroundColorIndex.coerceIn(0, 15)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = schemePreviewPaletteColor(palette, bgIdx),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            lines.forEach { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            }
        }
    }
}

/**
 * Individual scheme item in the list.
 */
@Composable
private fun SchemeItem(
    scheme: ColorScheme,
    paletteColors: IntArray,
    previewDefaults: Pair<Int, Int>,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(16) { i ->
                    val argb = paletteColors.getOrElse(i) { 0xff000000.toInt() }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = Color(argb),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            SchemeMiniTerminalPreview(
                palette = paletteColors,
                foregroundColorIndex = previewDefaults.first,
                backgroundColorIndex = previewDefaults.second
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val localizedDescription = getLocalizedColorSchemeDescription(scheme)
                    if (localizedDescription.isNotEmpty()) {
                        Text(
                            text = localizedDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (scheme.isBuiltIn) {
                            stringResource(R.string.label_built_in_scheme)
                        } else {
                            stringResource(R.string.label_custom_scheme)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (scheme.isBuiltIn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                Box {
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.button_more_options)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.button_export_scheme)) },
                            onClick = {
                                showMenu = false
                                onExport()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FileDownload,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.button_duplicate_scheme)) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            }
                        )

                        if (!scheme.isBuiltIn) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.button_delete_scheme)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating a new color scheme.
 */
@Composable
private fun NewSchemeDialog(
    availableSchemes: List<ColorScheme>,
    error: String?,
    onConfirm: (name: String, description: String, baseSchemeId: Long) -> Unit,
    onDismiss: () -> Unit,
    preselectedSchemeId: Long? = null,
    suggestedName: String? = null
) {
    var name by remember { mutableStateOf(suggestedName ?: "") }
    var description by remember { mutableStateOf("") }
    var selectedBaseSchemeId by remember {
        mutableLongStateOf(preselectedSchemeId ?: -1L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_new_scheme)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_scheme_name)) },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.label_scheme_description)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.label_base_scheme),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column {
                    availableSchemes.take(5).forEach { scheme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBaseSchemeId = scheme.id }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBaseSchemeId == scheme.id,
                                onClick = { selectedBaseSchemeId = scheme.id }
                            )
                            Text(
                                text = scheme.name,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description, selectedBaseSchemeId) }
            ) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

/**
 * Dialog for confirming scheme deletion.
 */
@Composable
private fun DeleteSchemeDialog(
    schemeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_delete_scheme)) },
        text = {
            Text(stringResource(R.string.dialog_message_delete_scheme, schemeName))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(R.string.button_delete_scheme),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
