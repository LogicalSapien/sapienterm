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

package com.logicalsapien.sapienterm.service

import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.apache.harmony.niochar.charset.additional.IBM437
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import com.logicalsapien.sapienterm.transport.AbsTransport
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.LinkedList

/**
 * Coroutine-based relay that handles incoming data from the transport to the terminal buffer.
 * Handles charset decoding and East Asian character width calculations.
 *
 * @author Kenny Root
 */
class Relay(
    private val bridge: TerminalBridge,
    private val transport: AbsTransport,
    private val dispatchers: CoroutineDispatchers,
    encoding: String
) {

    private var currentCharset: Charset? = null
    private var decoder: CharsetDecoder? = null

    private val encoder: CharsetEncoder = StandardCharsets.UTF_8.newEncoder().apply {
        onMalformedInput(CodingErrorAction.REPLACE)
        onUnmappableCharacter(CodingErrorAction.REPLACE)
    }

    private val sourceBuffer = ByteBuffer.allocate(BUFFER_SIZE)
    private val charBuffer = CharBuffer.allocate(BUFFER_SIZE)
    private val destBuffer = ByteBuffer.allocate(BUFFER_SIZE)

    /**
     * Ring buffer of the last [LINE_BUFFER_CAPACITY] lines of decoded output text.
     * Used by [CliPromptDetector][com.logicalsapien.sapienterm.util.CliPromptDetector]
     * to detect interactive CLI prompts.
     *
     * Access is synchronized on the [lineBuffer] instance itself.
     */
    private val lineBuffer = LinkedList<String>()
    private val currentLine = StringBuilder()

    /**
     * Session history: accumulates up to [SESSION_HISTORY_CAPACITY] plain-text lines
     * from the start of this session. Never reset during the session.
     * Access is synchronized on [lineBuffer].
     */
    private val sessionHistory = ArrayDeque<String>(SESSION_HISTORY_CAPACITY + 1)

    /**
     * Small rolling buffer used to detect DEC private mode 2004 (bracketed paste) enable/disable
     * sequences (`\e[?2004h` / `\e[?2004l`) in the remote output stream.
     * We keep just enough characters to match the longest sequence (8 chars).
     */
    private val escapeScanBuf = StringBuilder(16)

    /**
     * Completed when the relay receives its first chunk of data from the server.
     */
    val firstDataReceived = kotlinx.coroutines.CompletableDeferred<Unit>()

    /**
     * Returns a snapshot of the recent output lines (up to [LINE_BUFFER_CAPACITY]).
     * Thread-safe.
     */
    fun getRecentLines(): List<String> {
        synchronized(lineBuffer) {
            val result = ArrayList<String>(lineBuffer.size + 1)
            result.addAll(lineBuffer)
            if (currentLine.isNotEmpty()) {
                result.add(stripAnsiEscapes(currentLine.toString()))
            }
            return result
        }
    }

    /**
     * Returns a snapshot of session history (up to [SESSION_HISTORY_CAPACITY] lines)
     * accumulated since the session started, plus the current incomplete line if any.
     */
    fun getSessionHistory(): List<String> {
        synchronized(lineBuffer) {
            val result = ArrayList<String>(sessionHistory.size + 1)
            result.addAll(sessionHistory)
            if (currentLine.isNotEmpty()) {
                result.add(stripAnsiEscapes(currentLine.toString()))
            }
            return result
        }
    }

    init {
        setCharset(encoding)
    }

    /**
     * Set the character set for decoding incoming data.
     *
     * @param encoding the character set name (e.g., "UTF-8", "CP437")
     */
    fun setCharset(encoding: String) {
        Timber.d("changing charset to $encoding")

        val charset = if (encoding == "CP437") {
            IBM437("IBM437", arrayOf("IBM437", "CP437"))
        } else {
            Charset.forName(encoding)
        }

        if (charset == currentCharset) {
            return
        }

        val newCd = charset.newDecoder().apply {
            onUnmappableCharacter(CodingErrorAction.REPLACE)
            onMalformedInput(CodingErrorAction.REPLACE)
        }

        currentCharset = charset
        decoder = newCd
    }

    /**
     * Get the current character set.
     *
     * @return the current Charset
     */
    fun getCharset(): Charset? = currentCharset

    /**
     * Start relaying data from transport to terminal buffer.
     * This is a suspend function that runs on IO dispatcher.
     */
    suspend fun start() = withContext(dispatchers.io) {
        decoder?.reset()
        encoder.reset()
        sourceBuffer.clear()
        charBuffer.clear()
        destBuffer.clear()

        var endOfInput = false

        try {
            while (isActive && !endOfInput) {
                val currentDecoder = decoder ?: continue

                val offset = sourceBuffer.position()
                val length = sourceBuffer.remaining()

                val bytesRead = if (length > 0) {
                    transport.read(sourceBuffer.array(), offset, length)
                } else {
                    0
                }

                if (bytesRead == -1) {
                    endOfInput = true
                } else {
                    // Advance position to reflect new data
                    sourceBuffer.position(offset + bytesRead)
                    if (bytesRead > 0) {
                        firstDataReceived.complete(Unit)
                    }
                }

                sourceBuffer.flip()

                while (sourceBuffer.hasRemaining() || endOfInput) {
                    val decodeResult = currentDecoder.decode(sourceBuffer, charBuffer, endOfInput)

                    charBuffer.flip()

                    // Capture decoded characters into the line buffer for prompt detection
                    captureDecodedChars(charBuffer)

                    encoder.encode(charBuffer, destBuffer, endOfInput)
                    destBuffer.flip()

                    if (destBuffer.hasRemaining()) {
                        bridge.terminalEmulator.writeInput(destBuffer.array(), 0, destBuffer.limit())
                    }
                    destBuffer.clear()
                    charBuffer.compact()

                    if (decodeResult.isUnderflow) {
                        if (endOfInput) {
                            while (true) {
                                val flushResult = encoder.flush(destBuffer)
                                destBuffer.flip()
                                if (destBuffer.hasRemaining()) {
                                    bridge.terminalEmulator.writeInput(
                                        destBuffer.array(),
                                        0,
                                        destBuffer.limit()
                                    )
                                }
                                destBuffer.clear()

                                if (flushResult.isUnderflow) break
                            }
                            return@withContext
                        }

                        // Need more data to continue decoding
                        break
                    }

                    if (decodeResult.isOverflow) {
                        // Our buffer is full; let the inner loop run again to clear it
                        continue
                    }
                }

                // Move any remaining un-decoded bytes to the start of the buffer.
                sourceBuffer.compact()
            }
        } catch (e: IOException) {
            Timber.e(e, "Problem while handling incoming data in relay")
        }
    }

    /**
     * Scans decoded characters for newlines and appends them to the line buffer.
     * Also detects `\e[?2004h` / `\e[?2004l` (bracketed paste enable/disable) and
     * updates [TerminalBridge.remoteBracketedPasteEnabled] accordingly.
     * This reads from the current position to the limit without consuming the buffer.
     */
    private fun captureDecodedChars(buffer: CharBuffer) {
        val pos = buffer.position()
        val lim = buffer.limit()
        if (pos >= lim) return

        synchronized(lineBuffer) {
            for (i in pos until lim) {
                val c = buffer.get(i)

                // Feed into escape scan buffer and check for bracketed paste mode changes.
                escapeScanBuf.append(c)
                if (escapeScanBuf.length > ESCAPE_SCAN_BUF_SIZE) {
                    escapeScanBuf.deleteCharAt(0)
                }
                val s = escapeScanBuf.toString()
                if (s.endsWith(BP_ENABLE)) {
                    if (!bridge.remoteBracketedPasteEnabled) {
                        Timber.d("Relay: remote enabled bracketed paste (\\e[?2004h)")
                    }
                    bridge.remoteBracketedPasteEnabled = true
                } else if (s.endsWith(BP_DISABLE)) {
                    if (bridge.remoteBracketedPasteEnabled) {
                        Timber.d("Relay: remote disabled bracketed paste (\\e[?2004l)")
                    }
                    bridge.remoteBracketedPasteEnabled = false
                }

                when (c) {
                    '\n' -> {
                        val line = stripAnsiEscapes(currentLine.toString())
                        lineBuffer.add(line)
                        currentLine.clear()
                        if (lineBuffer.size > LINE_BUFFER_CAPACITY) lineBuffer.removeFirst()
                        sessionHistory.addLast(line)
                        if (sessionHistory.size > SESSION_HISTORY_CAPACITY) sessionHistory.removeFirst()
                    }
                    '\r' -> {
                        // Carriage return: reset current line (common in \r\n sequences)
                        // Don't emit a line -- the \n will do that
                    }
                    else -> {
                        // Cap line length to avoid unbounded growth
                        if (currentLine.length < MAX_LINE_LENGTH) {
                            currentLine.append(c)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CB.Relay"
        private const val BUFFER_SIZE = 4096
        private const val LINE_BUFFER_CAPACITY = 15
        private const val SESSION_HISTORY_CAPACITY = 500
        private const val MAX_LINE_LENGTH = 512

        // DEC private mode 2004: bracketed paste enable/disable sequences
        private const val BP_ENABLE  = "\u001b[?2004h"
        private const val BP_DISABLE = "\u001b[?2004l"
        private const val ESCAPE_SCAN_BUF_SIZE = 16

        /**
         * Strips ANSI/VT100 escape sequences from a string so that pattern
         * matching operates on visible text only.
         */
        private val ANSI_ESCAPE_REGEX = Regex("""\x1B(?:\[[0-9;]*[A-Za-z]|\][^\x07]*\x07|\][^\x1B]*\x1B\\|[()][AB012])""")

        fun stripAnsiEscapes(text: String): String {
            return ANSI_ESCAPE_REGEX.replace(text, "")
        }
    }
}
