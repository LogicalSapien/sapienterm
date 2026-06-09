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

package com.logicalsapien.sapienterm.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.service.TerminalBridge
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.clipboardImageBytes
import com.logicalsapien.sapienterm.util.containsPastePlaceholderTokens
import com.logicalsapien.sapienterm.util.encodeImageUriToJpegBytes
import com.logicalsapien.sapienterm.util.isLikelyBase64Payload
import com.logicalsapien.sapienterm.util.stripPastePlaceholderTokens
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

private const val PREF_FLOATING_INPUT_X = "floating_input_x"
private const val PREF_FLOATING_INPUT_Y = "floating_input_y"
private const val DEFAULT_X_RATIO = 0.05f // 5% from left
private const val DEFAULT_Y_RATIO = 0.3f // 30% from top

/** Bulk paste (e.g. base64) vs a few typed characters */
private const val MIN_PASTE_AS_IMAGE_LEN = 200

/** Placeholders already shown in the field — must not run “whole field is base64” handling on these. */
private val IMAGE_PLACEHOLDER_IN_TEXT = Regex("""\[Image #\d+\]""")

/**
 * Floating, draggable text input dialog with Compose TextField for full IME support.
 * Features:
 * - Draggable window that can be positioned anywhere
 * - Full IME support with swipe typing, voice input, predictions
 * - Persistent positioning saved in SharedPreferences
 * - Material Design 3 styling with blue accent
 * - Full text selection support
 * - Image paste from clipboard and gallery picker (uploaded to SSH, then pasted as a remote path)
 */
@Composable
fun FloatingTextInputDialog(
    bridge: TerminalBridge,
    initialText: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // Calculate screen dimensions
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Window dimensions (90% of screen width)
    val windowWidthDp = configuration.screenWidthDp * 0.9f
    val windowWidthPx = with(density) { windowWidthDp.dp.toPx() }

    // Load saved position or use defaults
    val savedX = prefs.getFloat(PREF_FLOATING_INPUT_X, DEFAULT_X_RATIO)
    val savedY = prefs.getFloat(PREF_FLOATING_INPUT_Y, DEFAULT_Y_RATIO)

    // Current position in pixels
    var offsetX by remember { mutableFloatStateOf(screenWidthPx * savedX) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * savedY) }

    // Text state and focus
    var text by remember { mutableStateOf(initialText) }
    val focusRequester = remember { FocusRequester() }

    /** Prevents double-tap / duplicate callbacks from sending twice. */
    var sending by remember { mutableStateOf(false) }

    var bracketedPaste by remember {
        mutableStateOf(prefs.getBoolean(PreferenceConstants.TERMINAL_BRACKETED_PASTE_SEND, false))
    }

    // Image store: maps image number -> JPEG bytes. The text field shows [Image #N]
    // placeholders; on Send they are uploaded and replaced with remote paths.
    val imageStore = remember { mutableStateMapOf<Int, ByteArray>() }
    var nextImageId by remember { mutableIntStateOf(1) }

    fun attachImage(bytes: ByteArray) {
        if (imageStore.values.any { it.contentEquals(bytes) }) {
            Toast.makeText(context, "Image already attached", Toast.LENGTH_SHORT).show()
            return
        }
        val id = nextImageId++
        imageStore[id] = bytes
        val sizeKb = bytes.size / 1024
        text = text + "[Image #$id]"
        Toast.makeText(context, "[Image #$id] attached ($sizeKb KB)", Toast.LENGTH_SHORT).show()
    }

    /**
     * IME often delivers a large paste in several [onValueChange] steps. The suffix-only path would
     * store a truncated payload and leave a base64 prefix in the field. When the full buffer looks
     * like one base64 image, replace the entire field with a single placeholder.
     */
    fun attachImageReplacingWholeField(bytes: ByteArray) {
        if (imageStore.values.any { it.contentEquals(bytes) }) {
            Toast.makeText(context, "Image already attached", Toast.LENGTH_SHORT).show()
            return
        }
        val id = nextImageId++
        imageStore[id] = bytes
        val sizeKb = bytes.size / 1024
        text = "[Image #$id]"
        Toast.makeText(context, "[Image #$id] attached ($sizeKb KB)", Toast.LENGTH_SHORT).show()
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bytes = encodeImageUriToJpegBytes(context, uri)
            if (bytes != null) {
                attachImage(bytes)
            } else {
                Toast.makeText(context, "Failed to encode image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun pasteImageFromClipboard() {
        val bytes = clipboardImageBytes(context)
        if (bytes != null) {
            attachImage(bytes)
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(context).toString()
                when {
                    containsPastePlaceholderTokens(pasted) -> {
                        Toast.makeText(
                            context,
                            "Use Paste Image after copying an image, or Select Image.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    isLikelyBase64Payload(pasted) -> {
                        Toast.makeText(
                            context,
                            "Base64 image text is not sent directly. Use Paste Image or Select Image.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> text = text + pasted
                }
            } else {
                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun uploadImagesAndResolvePlaceholders(): String? {
        var resolved = text
        for (id in imageStore.keys.sortedDescending()) {
            val bytes = imageStore[id] ?: continue
            val filename = "sapienterm-${System.currentTimeMillis()}-$id.jpg"
            val remotePath = bridge.uploadTerminalAttachment(bytes, filename)
            if (remotePath == null) {
                Toast.makeText(context, "Image upload is only available for SSH sessions", Toast.LENGTH_LONG).show()
                return null
            }
            resolved = resolved.replace("[Image #$id]", remotePath)
        }
        return resolved
    }

    // Request focus when shown
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Save position when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            prefs.edit {
                putFloat(PREF_FLOATING_INPUT_X, offsetX / screenWidthPx)
                putFloat(PREF_FLOATING_INPUT_Y, offsetY / screenHeightPx)
            }
        }
    }

    fun sendText() {
        if (text.isEmpty() || sending) return
        if (!bridge.canWriteToRemote()) {
            Toast.makeText(
                context,
                context.getString(R.string.terminal_send_not_ready),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        sending = true
        coroutineScope.launch {
            try {
                val resolved = uploadImagesAndResolvePlaceholders() ?: return@launch
                if (bracketedPaste || imageStore.isNotEmpty()) {
                    bridge.sendBracketedPaste(resolved, submitWithNewline = true)
                } else {
                    bridge.sendCommand(resolved)
                }
                text = ""
                imageStore.clear()
                nextImageId = 1
                onDismiss()
            } catch (e: Exception) {
                Timber.e(e, "Failed to send floating text input")
                Toast.makeText(context, "Send failed: ${e.message ?: "unknown error"}", Toast.LENGTH_LONG).show()
            } finally {
                sending = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { _, _ -> }
                }
        ) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .width(windowWidthDp.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                // Draggable header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX = (offsetX + dragAmount.x).coerceIn(
                                    0f,
                                    screenWidthPx - windowWidthPx
                                )
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    0f,
                                    screenHeightPx - with(density) { 200.dp.toPx() }
                                )
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.terminal_text_input_dialog_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.button_close),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = bracketedPaste,
                        onCheckedChange = { v ->
                            bracketedPaste = v
                            prefs.edit {
                                putBoolean(PreferenceConstants.TERMINAL_BRACKETED_PASTE_SEND, v)
                            }
                        }
                    )
                    Text(
                        text = stringResource(R.string.terminal_bracketed_paste_send_title),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                // TextField + Send row (top-align FAB so Send sits higher vs the tall field)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = text,
                        onValueChange = { new ->
                            if (containsPastePlaceholderTokens(new)) {
                                val cleaned = stripPastePlaceholderTokens(new)
                                val img = clipboardImageBytes(context)
                                if (img != null) {
                                    text = cleaned
                                    attachImage(img)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Image could not be read from clipboard. Use Paste Image or Select Image below.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    text = cleaned
                                }
                            } else {
                                // Whole buffer is one base64 image (handles multi-chunk IME pastes).
                                if (!IMAGE_PLACEHOLDER_IN_TEXT.containsMatchIn(new) &&
                                    isLikelyBase64Payload(new)
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Base64 image text is not sent directly. Use Paste Image or Select Image.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    text = ""
                                } else {
                                    // Paste only at end after non-base64 prefix (e.g. typed text + image).
                                    val addedIfSuffix =
                                        if (new.startsWith(text) && new.length > text.length + MIN_PASTE_AS_IMAGE_LEN) {
                                            new.removePrefix(text)
                                        } else {
                                            null
                                        }
                                    if (addedIfSuffix != null && isLikelyBase64Payload(addedIfSuffix)) {
                                        Toast.makeText(
                                            context,
                                            "Base64 image text is not sent directly. Use Paste Image or Select Image.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        text = new
                                    }
                                }
                            }
                        },
                        placeholder = {
                            Text(stringResource(R.string.terminal_text_input_dialog_label))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        keyboardActions = KeyboardActions(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp)
                            .focusRequester(focusRequester)
                    )

                    FloatingActionButton(
                        onClick = { sendText() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(if (text.isNotEmpty() && !sending) 1f else 0.38f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.button_send)
                        )
                    }
                }

                // Image action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 6.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { pasteImageFromClipboard() },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste image",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Paste Image",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Select image",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Select Image",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
