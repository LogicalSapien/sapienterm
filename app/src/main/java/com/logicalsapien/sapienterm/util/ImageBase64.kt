package com.logicalsapien.sapienterm.util

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream

private const val IMAGE_MAX_DIMENSION = 2048
private const val IMAGE_JPEG_QUALITY = 80

/** Minimum length to treat pasted content as a likely base64 image payload (not typed text). */
private const val MIN_BASE64_HEURISTIC_LEN = 200

/**
 * Compose / IME may insert these placeholder tokens instead of huge paste payloads (e.g. image
 * data). They must not be committed as real field text.
 */
private val PASTE_PLACEHOLDER_REGEX =
    Regex("""\[\s*Pasted text\s+#\d+\s+\+\d+\s+lines\s*\]""", RegexOption.IGNORE_CASE)

fun containsPastePlaceholderTokens(s: String): Boolean = PASTE_PLACEHOLDER_REGEX.containsMatchIn(s)

fun stripPastePlaceholderTokens(s: String): String = s.replace(PASTE_PLACEHOLDER_REGEX, "")

/**
 * Heuristic: long strings that are almost entirely base64 alphabet are probably
 * pasted image data (or copied from “Paste Image” as plain text).
 */
fun isLikelyBase64Payload(s: String): Boolean {
    if (s.length < MIN_BASE64_HEURISTIC_LEN) return false
    var match = 0
    for (ch in s) {
        if (ch.isLetterOrDigit() || ch == '+' || ch == '/' || ch == '=') match++
    }
    return match * 100 / s.length >= 95
}

/**
 * Read an image from a content [Uri], resize to fit within 1024 px on the
 * longest side, compress as JPEG, and return the base64-encoded string.
 * Returns `null` on any failure.
 */
fun encodeImageUriToJpegBytes(context: Context, uri: Uri): ByteArray? {
    return try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            val input = context.contentResolver.openInputStream(uri) ?: return null
            BitmapFactory.decodeStream(input).also { input.close() }
        }

        val maxDim = IMAGE_MAX_DIMENSION
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY, out)
        out.toByteArray()
    } catch (_: Exception) {
        null
    }
}

/**
 * Read an image from a content [Uri], resize/compress as JPEG, and return base64 text.
 */
fun encodeImageUri(context: Context, uri: Uri): String? {
    val bytes = encodeImageUriToJpegBytes(context, uri) ?: return null
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

/**
 * If the system clipboard contains an image, base64-encode it and return
 * the string. Returns `null` when the clipboard holds text or is empty.
 */
fun clipboardImageBytes(context: Context): ByteArray? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null

    val item = clip.getItemAt(0)
    // Prefer a content URI when present — some apps omit image/* on the clip description.
    item.uri?.let { uri ->
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType != null && mimeType.startsWith("image/")) {
            return encodeImageUriToJpegBytes(context, uri)
        }
    }

    val hasImage = (0 until clip.description.mimeTypeCount).any { i ->
        clip.description.getMimeType(i).startsWith("image/")
    }
    if (!hasImage) return null

    val uri = item.uri ?: return null
    val mimeType = context.contentResolver.getType(uri)
    if (mimeType != null && !mimeType.startsWith("image/")) return null

    return encodeImageUriToJpegBytes(context, uri)
}

/**
 * If the system clipboard contains an image, base64-encode it and return
 * the string. Returns `null` when the clipboard holds text or is empty.
 */
fun clipboardImageBase64(context: Context): String? {
    val bytes = clipboardImageBytes(context) ?: return null
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}
