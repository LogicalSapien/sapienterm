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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.net.NetworkCapabilities
import androidx.compose.ui.graphics.Color
import com.logicalsapien.sapienterm.R
import com.logicalsapien.sapienterm.data.entity.Host
import com.logicalsapien.sapienterm.data.entity.PortForward
import com.logicalsapien.sapienterm.di.CoroutineDispatchers
import com.logicalsapien.sapienterm.transport.AbsTransport
import com.logicalsapien.sapienterm.transport.SSH
import com.logicalsapien.sapienterm.transport.TransportFactory
import com.logicalsapien.sapienterm.util.HostConstants
import com.logicalsapien.sapienterm.util.PreferenceConstants
import com.logicalsapien.sapienterm.util.lineEchoMatchesFloatingImagePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.connectbot.terminal.ProgressState
import org.connectbot.terminal.TerminalEmulator
import org.connectbot.terminal.TerminalEmulatorFactory
import org.connectbot.terminal.TranscriptStyledLine
import timber.log.Timber
import java.io.IOException
import java.nio.charset.Charset
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

/**
 * Provides a bridge between a MUD terminal buffer and a possible TerminalView.
 * This separation allows us to keep the TerminalBridge running in a background
 * service. A TerminalView shares down a bitmap that we can use for rendering
 * when available.
 *
 * This class also provides SSH hostkey verification prompting, and password
 * prompting.
 */
@Suppress("DEPRECATION") // for ClipboardManager
class TerminalBridge {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private sealed class TransportOperation {
        data class WriteData(val data: ByteArray) : TransportOperation()
        data class SetDimensions(val columns: Int, val rows: Int, val width: Int, val height: Int) : TransportOperation()
    }

    private val transportOperations = Channel<TransportOperation>(Channel.UNLIMITED)

    /** Enqueue bytes for the IO writer; logs if the channel is closed (e.g. bridge cleaned up). */
    private fun offerTransportWrite(data: ByteArray, context: String = "write") {
        val result = transportOperations.trySend(TransportOperation.WriteData(data))
        if (!result.isSuccess) {
            Timber.w(result.exceptionOrNull(), "Transport queue failed ($context)")
        }
    }

    /**
     * Set to true by [Relay] when the remote program sends `\e[?2004h` (bracketed paste enable),
     * false on `\e[?2004l` (disable). Shells and TUIs that support bracketed paste advertise
     * it this way. Kept as session state for terminal paste behavior.
     */
    @Volatile
    var remoteBracketedPasteEnabled: Boolean = false

    // Styled session history: accumulates up to STYLED_HISTORY_CAPACITY lines with full
    // color/bold/italic. Populated by a background poller that diffs terminal snapshots.
    private val styledHistoryLock = Any()
    private val _styledHistory = ArrayDeque<TranscriptStyledLine>(STYLED_HISTORY_CAPACITY + 1)

    // Returns scrollback history + current screen lines (deduplication-safe).
    // The accumulator only tracks scrollback; screen lines are appended live here
    // so they never get double-counted when they later scroll into scrollback.
    fun getStyledHistory(): List<TranscriptStyledLine> {
        val scrollback = synchronized(styledHistoryLock) { _styledHistory.toList() }
        val allLines = terminalEmulator.getFullTranscriptStyledLines()
        // Use dimensions.rows (same snapshot) instead of scrollbackSize (live) to avoid
        // a race where the live scrollback count exceeds the snapshot's scrollback, causing
        // drop(sbSize) to exceed allLines.size and wipe out all screen lines every other poll.
        val screenRowCount = terminalEmulator.dimensions.rows
        val sbSize = (allLines.size - screenRowCount).coerceAtLeast(0)
        val screenLines = allLines.drop(sbSize).dropLastWhile { it.plainTextForFilter.isBlank() }
        return scrollback + screenLines
    }

    private var styledHistoryAccumulatorStarted = false

    private fun startStyledHistoryAccumulator() {
        if (styledHistoryAccumulatorStarted) {
            Timber.w("StyledHistory accumulator already running — ignoring duplicate start")
            return
        }
        styledHistoryAccumulatorStarted = true
        scope.launch(dispatchers.main) {
            // Track only scrollback (monotonically growing buffer).
            // Screen lines are appended live in getStyledHistory(), so they are
            // never re-added when they scroll off into the scrollback buffer.
            relay?.firstDataReceived?.await()
            delay(2000L)

            var prevScrollbackSize = -1

            while (isActive) {
                val sbSize = terminalEmulator.scrollbackSize
                val allLines = terminalEmulator.getFullTranscriptStyledLines()

                synchronized(styledHistoryLock) {
                    if (prevScrollbackSize < 0) {
                        // First poll: seed with all existing scrollback lines.
                        for (i in 0 until sbSize) {
                            _styledHistory.addLast(allLines[i])
                            if (_styledHistory.size > STYLED_HISTORY_CAPACITY) _styledHistory.removeFirst()
                        }
                        prevScrollbackSize = sbSize
                    } else {
                        // Add only lines that newly entered scrollback since last poll.
                        val newCount = sbSize - prevScrollbackSize
                        if (newCount > 0) {
                            for (i in prevScrollbackSize until sbSize) {
                                _styledHistory.addLast(allLines[i])
                                if (_styledHistory.size > STYLED_HISTORY_CAPACITY) _styledHistory.removeFirst()
                            }
                            prevScrollbackSize = sbSize
                        }
                    }
                }
                delay(500L)
            }
        }
    }

    /**
     * Whether the transport reports an open session — PTY stdin should accept bytes.
     * If false, [AbsTransport.write] may no-op and nothing reaches the remote.
     */
    fun canWriteToRemote(): Boolean {
        val t = transport ?: return false
        return try {
            t.isConnected() && t.isSessionOpen()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun uploadTerminalAttachment(bytes: ByteArray, filename: String): String? =
        kotlinx.coroutines.withContext(dispatchers.io) {
            transport?.uploadTerminalAttachment(bytes, filename)
        }

    private var pendingResize: TransportOperation.SetDimensions? = null
    private var resizeJob: Job? = null

    /**
     * When > 0, resize events are buffered and only the last one is sent
     * after this timestamp. Prevents tmux dot-flood during connection setup.
     */
    @Volatile
    private var resizeSuppressedUntil: Long = 0L
    private var resizeFlushJob: Job? = null

    var color: IntArray = IntArray(0)

    var defaultFg = HostConstants.DEFAULT_FG_COLOR
    var defaultBg = HostConstants.DEFAULT_BG_COLOR

    // Store color scheme info for reapplication
    private var currentColorSchemeId: Long = -1L
    private var fullColorPalette: IntArray = IntArray(0)
    private var isUsingLightOverride = false

    // Profile observation
    private var currentProfileId: Long? = null
    private var profileObservationJob: Job? = null

    val manager: TerminalManager

    var host: Host

    private val dispatchers: CoroutineDispatchers

    /* package */
    var transport: AbsTransport? = null

    val defaultPaint: Paint

    var relay: Relay? = null
        private set

    private val emulation: String?
    private val scrollback: Int
    private val encoding: String

    /** Font family from profile for terminal display */
    val fontFamily: String?

    /** Force size rows from profile (null = auto-size) */
    var profileForceSizeRows: Int? = null
        private set

    /** Force size columns from profile (null = auto-size) */
    var profileForceSizeColumns: Int? = null
        private set

    // Terminal emulator from ConnectBot Terminal library
    val terminalEmulator: TerminalEmulator

    /**
     * Callback invoked when text input dialog is requested (e.g., from camera button)
     */
    var onTextInputRequested: (() -> Unit)? = null

    /**
     * Custom display name for the session tab. When null, the host nickname is used.
     */
    var customTabName: String? = null

    private val _bellEvents = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 10)
    val bellEvents: SharedFlow<Unit> = _bellEvents.asSharedFlow()

    private val _networkStatusMessages = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 10)
    val networkStatusMessages: SharedFlow<String> = _networkStatusMessages.asSharedFlow()

    // Progress state for OSC 9;4 progress reporting
    data class ProgressInfo(val state: ProgressState, val progress: Int)
    private val _progressState = MutableStateFlow<ProgressInfo?>(null)
    val progressState: StateFlow<ProgressInfo?> = _progressState.asStateFlow()

    /** Current terminal background / foreground colours (updated when colour scheme changes). */
    data class TerminalColors(val background: Color, val foreground: Color)
    private val _terminalColors = MutableStateFlow(TerminalColors(Color.Black, Color.White))
    val terminalColorsFlow: StateFlow<TerminalColors> = _terminalColors.asStateFlow()


    private var disconnected = false
    private var awaitingClose = false

    private var forcedSize = false

    // Network state tracking for grace period
    private data class NetworkState(
        val ipAddresses: Set<String>,
        val networkId: String,
        val networkType: Int
    )

    private var lastKnownNetworkState: NetworkState? = null
    private var networkGracePeriodJob: Job? = null
    private var inGracePeriod: Boolean = false

    /** Periodic SSH transport ping when keep-alive is enabled in settings */
    private var sshKeepAliveJob: Job? = null

    private val keyListener: TerminalKeyListener

    var charWidth = -1
    var charHeight = -1
    private var charTop = -1

    private var fontSizeSp: Float = DEFAULT_FONT_SIZE_SP.toFloat()
    private val _fontSizeFlow = MutableStateFlow(-1f)
    val fontSizeFlow: StateFlow<Float> = _fontSizeFlow.asStateFlow()

    @Deprecated("Use fontSizeFlow instead")
    private val fontSizeChangedListeners: MutableList<FontSizeChangedListener>

    private val localOutput: MutableList<String>

    /**
     * Flag indicating if we should perform a full-screen redraw during our next
     * rendering pass.
     */
    private var fullRedraw = false

    val promptManager = PromptManager()

    private val disconnectListeners = CopyOnWriteArrayList<BridgeDisconnectedListener>()

    /**
     * Base64 payloads last sent from [FloatingTextInputDialog]. Used only so the terminal
     * canvas can draw `[image]` for those echoed lines instead of megabytes of characters.
     */
    private val floatingImageEchoMasks = ArrayDeque<String>()
    private val floatingImageEchoMasksLock = Any()
    private val maxFloatingImageEchoMasks = 24

    /**
     * Register image payloads about to be sent via the floating text box so their shell echo
     * can be display-masked as `[image]` (see [shouldMaskFloatingImageEcho]).
     */
    fun registerFloatingImagePayloadsForEchoMask(base64Strings: Collection<String>) {
        synchronized(floatingImageEchoMasksLock) {
            for (s in base64Strings) {
                if (s.length < 100) continue
                if (floatingImageEchoMasks.none { it == s }) {
                    floatingImageEchoMasks.addLast(s)
                    while (floatingImageEchoMasks.size > maxFloatingImageEchoMasks) {
                        floatingImageEchoMasks.removeFirst()
                    }
                }
            }
        }
    }

    /**
     * Returns true when this screen line is part of the shell echo of a payload registered from
     * the floating text dialog. Long base64 is wrapped across many rows; each row is matched as
     * a substring (or as the base64 suffix after a prompt on the first line).
     */
    fun shouldMaskFloatingImageEcho(lineText: String): Boolean {
        synchronized(floatingImageEchoMasksLock) {
            return floatingImageEchoMasks.any { lineEchoMatchesFloatingImagePayload(lineText, it) }
        }
    }

    /**
     * Create new terminal bridge with following parameters. We will immediately
     * launch thread to start SSH connection and handle any hostkey verification
     * and password authentication.
     */
    constructor(manager: TerminalManager, host: Host, dispatchers: CoroutineDispatchers) {
        this.manager = manager
        this.host = host
        this.dispatchers = dispatchers

        // Load profile for this host (always returns a profile, defaulting to Default profile)
        val profile = manager.profileRepository.getByIdOrDefaultBlocking(host.profileId)
        currentProfileId = host.profileId

        emulation = profile.emulation
        scrollback = manager.getScrollback()

        // create our default paint
        defaultPaint = Paint()
        defaultPaint.isAntiAlias = true
        defaultPaint.typeface = Typeface.MONOSPACE
        defaultPaint.isFakeBoldText = true // more readable?

        localOutput = mutableListOf()

        fontSizeChangedListeners = mutableListOf()

        // Store encoding and font family from profile for later use
        encoding = profile.encoding
        fontFamily = profile.fontFamily

        // Store force size from profile
        profileForceSizeRows = profile.forceSizeRows
        profileForceSizeColumns = profile.forceSizeColumns

        // Use settings from profile
        var fontSizeSp = profile.fontSize
        if (fontSizeSp <= 0) {
            fontSizeSp = DEFAULT_FONT_SIZE_SP
        }
        setFontSize(fontSizeSp.toFloat())

        // Load color scheme from profile
        currentColorSchemeId = profile.colorSchemeId
        fullColorPalette = manager.colorRepository.getColorsForSchemeBlocking(profile.colorSchemeId)
        val defaults = manager.colorRepository.getDefaultColorsForSchemeBlocking(profile.colorSchemeId)
        defaultFg = defaults[0]
        defaultBg = defaults[1]

        // Get actual RGB colors for the default foreground/background indices
        val defaultFgColor = fullColorPalette[defaultFg]
        val defaultBgColor = fullColorPalette[defaultBg]

        // Use host's fixed size if set, otherwise pre-calculate from screen
        val hostFixed = if (host.fixedCols != null && host.fixedRows != null &&
            host.fixedCols > 0 && host.fixedRows > 0
        ) {
            Pair(host.fixedCols, host.fixedRows)
        } else {
            null
        }
        val (estCols, estRows) = hostFixed ?: estimateTerminalDimensions()

        val maxScrollbackLines =
            (if (host.scrollbackLines > 0) host.scrollbackLines else scrollback)
                .coerceIn(1, 256_000)
                .coerceAtLeast(2048)

        terminalEmulator = TerminalEmulatorFactory.create(
            initialRows = estRows,
            initialCols = estCols,
            defaultForeground = Color(defaultFgColor),
            defaultBackground = Color(defaultBgColor),
            maxScrollbackLines = maxScrollbackLines,
            onKeyboardInput = { data ->
                offerTransportWrite(data, "keyboard")
            },
            onBell = {
                scope.launch {
                    _bellEvents.emit(Unit)
                }
                manager.sendActivityNotification(host)
            },
            onResize = { info ->
                val op = TransportOperation.SetDimensions(info.columns, info.rows, 0, 0)
                pendingResize = op
                resizeJob?.cancel()

                val now = System.currentTimeMillis()
                if (now >= resizeSuppressedUntil) {
                    resizeJob = scope.launch {
                        kotlinx.coroutines.delay(RESIZE_DEBOUNCE_MS)
                        pendingResize?.let { transportOperations.trySend(it) }
                        pendingResize = null
                    }
                }
                // else: settling period active, just buffer — flush job will send
            },
            onClipboardCopy = { text ->
                // OSC 52 clipboard support - copy remote text to local clipboard
                Timber.i("OSC 52 clipboard copy: ${text.length} chars")
                val clipboard = manager.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("terminal", text))
            },
            onProgressChange = { state, progress ->
                // OSC 9;4 progress reporting - update progress state
                Timber.d("OSC 9;4 progress: state=$state, progress=$progress")
                _progressState.value = ProgressInfo(state, progress)
            }
        )

        // Apply color scheme to terminal emulator
        val ansiColors = fullColorPalette.sliceArray(0 until 16)
        terminalEmulator.applyColorScheme(ansiColors, defaultFgColor, defaultBgColor)
        _terminalColors.value = TerminalColors(Color(defaultBgColor), Color(defaultFgColor))

        keyListener = TerminalKeyListener(manager, this, encoding)

        // Start the transport operation processor to serialize all writes
        startTransportOperationProcessor()

        // Start observing profile changes for live updates
        startProfileObservation()
    }

    /**
     * Processes transport operations serially to maintain strict ordering.
     * This ensures keyboard input and other writes happen in the correct order.
     */
    private fun startTransportOperationProcessor() {
        scope.launch(dispatchers.io) {
            for (operation in transportOperations) {
                try {
                    when (operation) {
                        is TransportOperation.WriteData -> {
                            transport?.write(operation.data)
                            // Ensure each write reaches the PTY immediately (interactive CLIs / agent TUIs buffer otherwise).
                            transport?.flush()
                        }

                        is TransportOperation.SetDimensions -> {
                            transport?.setDimensions(
                                operation.columns,
                                operation.rows,
                                operation.width,
                                operation.height
                            )
                        }

                    }
                } catch (e: IOException) {
                    Timber.e(e, "Error processing transport operation")
                } catch (e: Exception) {
                    Timber.e(e, "Unexpected error processing transport operation")
                }
            }
        }
    }

    /**
     * Start observing profile changes to apply live updates.
     * Observes both profile attribute changes and host profile_id changes.
     */
    private fun startProfileObservation() {
        profileObservationJob = scope.launch {
            // For temporary hosts (negative IDs), only observe the current profile
            if (host.id <= 0) {
                currentProfileId?.let { profileId ->
                    manager.profileRepository.observeById(profileId)
                        .filterNotNull()
                        .collectLatest { profile ->
                            Timber.d("Profile ${profile.id} changed, applying updates")
                            applyProfileSettings(profile)
                        }
                }
                return@launch
            }

            // Keep in-memory [host] in sync with DB (e.g. per-host bottom bar override from host editor).
            launch {
                manager.hostRepository.observeHost(host.id)
                    .filterNotNull()
                    .collect { updated -> host = updated }
            }

            // For saved hosts, observe the host to detect profile_id changes
            // and switch profile observation when it changes
            manager.hostRepository.observeHost(host.id)
                .filterNotNull()
                .map { it.profileId }
                .distinctUntilChanged()
                .collectLatest { newProfileId ->
                    if (newProfileId != currentProfileId) {
                        Timber.d("Host profile changed from $currentProfileId to $newProfileId")
                        currentProfileId = newProfileId
                    }

                    // Observe the current profile for attribute changes
                    val profileToObserve = newProfileId ?: 1L // Default to profile 1 if null
                    manager.profileRepository.observeById(profileToObserve)
                        .filterNotNull()
                        .collectLatest { profile ->
                            Timber.d("Profile ${profile.id} changed, applying updates")
                            applyProfileSettings(profile)
                        }
                }
        }
    }

    /**
     * Apply changes when the host's profile_id changes.
     */
    private suspend fun applyProfileChanges(newProfileId: Long?) {
        val profile = if (newProfileId != null) {
            manager.profileRepository.getById(newProfileId)
        } else {
            null
        } ?: manager.profileRepository.getDefault()

        applyProfileSettings(profile)
    }

    /**
     * Apply profile settings to the terminal.
     */
    private fun applyProfileSettings(profile: com.logicalsapien.sapienterm.data.entity.Profile) {
        // Apply font size
        val newFontSize = if (profile.fontSize > 0) profile.fontSize else DEFAULT_FONT_SIZE_SP
        if (newFontSize.toFloat() != fontSizeSp) {
            setFontSize(newFontSize.toFloat())
        }

        // Apply color scheme if changed
        if (profile.colorSchemeId != currentColorSchemeId) {
            currentColorSchemeId = profile.colorSchemeId
            fullColorPalette = manager.colorRepository.getColorsForSchemeBlocking(profile.colorSchemeId)
            val defaults = manager.colorRepository.getDefaultColorsForSchemeBlocking(profile.colorSchemeId)
            defaultFg = defaults[0]
            defaultBg = defaults[1]

            // Apply to terminal emulator
            val defaultFgColor = fullColorPalette[defaultFg]
            val defaultBgColor = fullColorPalette[defaultBg]
            val ansiColors = fullColorPalette.sliceArray(0 until 16)
            terminalEmulator.applyColorScheme(ansiColors, defaultFgColor, defaultBgColor)
            _terminalColors.value = TerminalColors(Color(defaultBgColor), Color(defaultFgColor))
        }

        // Update force size from profile
        profileForceSizeRows = profile.forceSizeRows
        profileForceSizeColumns = profile.forceSizeColumns

        // Note: encoding and fontFamily changes require reconnection to take effect
        // as they are deeply integrated into the terminal initialization
    }

    /**
     * Apply a light or dark color override to the terminal. In light mode we
     * use a clean ANSI palette with pure-white background and near-black
     * foreground so the terminal surface stays uniform — no cream/parchment
     * bands from tinted ANSI whites that were producing horizontal stripes.
     */
    fun applyThemeOverride(light: Boolean) {
        if (light && !isUsingLightOverride) {
            isUsingLightOverride = true
            val cleanAnsi = intArrayOf(
                0xFF000000.toInt(), 0xFFCC0000.toInt(), 0xFF4E9A06.toInt(), 0xFFC4A000.toInt(),
                0xFF3465A4.toInt(), 0xFF75507B.toInt(), 0xFF06989A.toInt(), 0xFFFFFFFF.toInt(),
                0xFF555753.toInt(), 0xFFEF2929.toInt(), 0xFF8AE234.toInt(), 0xFFFCE94F.toInt(),
                0xFF729FCF.toInt(), 0xFFAD7FA8.toInt(), 0xFF34E2E2.toInt(), 0xFFFFFFFF.toInt()
            )
            val fg = 0xFF111318.toInt()  // near-black, sharp on white
            val bg = 0xFFFFFFFF.toInt()  // pure white
            terminalEmulator.applyColorScheme(cleanAnsi, fg, bg)
            _terminalColors.value = TerminalColors(Color(bg), Color(fg))
        } else if (!light && isUsingLightOverride) {
            isUsingLightOverride = false
            if (fullColorPalette.size >= 16) {
                val fg = fullColorPalette[defaultFg]
                val bg = fullColorPalette[defaultBg]
                val ansiColors = fullColorPalette.sliceArray(0 until 16)
                terminalEmulator.applyColorScheme(ansiColors, fg, bg)
                _terminalColors.value = TerminalColors(Color(bg), Color(fg))
            }
        }
    }

    /**
     * Spawn thread to open connection and start login process.
     * Also handles reconnection by resetting state flags and clearing the terminal.
     */
    fun startConnection() {
        val newTransport = TransportFactory.getTransport(host.protocol)
        if (newTransport == null) {
            Timber.w("No transport found for ${host.protocol}")
            return
        }

        val isReconnect = disconnected

        // Clean up old connection state before reconnecting
        if (isReconnect) {
            Timber.i("Reconnecting ${host.nickname} — cleaning up old state")
            // Close old transport if still lingering
            val oldTransport = transport
            if (oldTransport != null) {
                scope.launch(dispatchers.io) {
                    try {
                        oldTransport.close()
                    } catch (_: Exception) {}
                }
            }
            // Old relay will exit on its own when its transport is closed
            relay = null

            terminalEmulator.clearScreen()
            localOutput.clear()
        }

        // Reset state for reconnection so onConnected() is not blocked and the UI
        // dismisses its "Connection lost — Reconnect" overlay immediately instead
        // of waiting for the (async, possibly-failing) handshake to complete.
        synchronized(this) {
            awaitingClose = false
            disconnected = false
        }
        manager.notifyBridgeStateChanged()

        transport = newTransport
        newTransport.bridge = this
        newTransport.manager = manager
        newTransport.host = host

        if (newTransport is SSH) {
            newTransport.setCompression(host.compression)
            host.useAuthAgent?.let { newTransport.setUseAuthAgent(it) }
        }
        newTransport.setEmulation(emulation)

        outputLine(manager.res.getString(R.string.terminal_connecting, host.hostname, host.port, host.protocol))

        scope.launch(dispatchers.io) {
            try {
                if (newTransport.canForwardPorts()) {
                    try {
                        for (portForward in manager.hostRepository.getPortForwardsForHost(host.id)) {
                            newTransport.addPortForward(portForward)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load port forwards for ${host.nickname}")
                        manager.reportError(
                            ServiceError.PortForwardLoadFailed(
                                hostNickname = host.nickname,
                                reason = e.message ?: "Failed to load port forwards"
                            )
                        )
                    }
                }
                Timber.i("Starting connection to ${host.nickname}")
                newTransport.connect()
            } catch (e: Exception) {
                Timber.e(e, "Connection failed for ${host.nickname}")
                outputLine("Connection failed: ${e.message}")
                synchronized(this@TerminalBridge) {
                    disconnected = true
                }
                manager.reportError(
                    ServiceError.ConnectionFailed(
                        hostNickname = host.nickname,
                        hostname = host.hostname,
                        reason = e.message ?: "Connection failed"
                    )
                )
                manager.notifyBridgeStateChanged()
            }
        }
    }

    /**
     * @return charset in use by bridge
     */
    val charset: Charset
        get() = relay?.getCharset() ?: Charsets.UTF_8

    /**
     * Sets the encoding used by the terminal. If the connection is live,
     * then the character set is changed for the next read.
     * @param encoding the canonical name of the character encoding
     */
    fun setCharset(encoding: String) {
        relay?.setCharset(encoding)
        keyListener.setCharset(encoding)
    }

    /**
     * Convenience method for writing text into the underlying terminal buffer.
     * Should never be called once the session is established.
     */
    fun outputLine(output: String?) {
        if (output == null) return

        if (transport?.isSessionOpen() == true) {
            Timber.e(
                "Session established, cannot use outputLine!",
                IOException("outputLine call traceback")
            )
        }

        synchronized(localOutput) {
            for (line in output.split("\n".toRegex())) {
                var processedLine = line
                if (processedLine.isNotEmpty() && processedLine[processedLine.length - 1] == '\r') {
                    processedLine = processedLine.substring(0, processedLine.length - 1)
                }

                val s = processedLine + "\r\n"

                localOutput.add(s)

                terminalEmulator.writeInput(s.encodeToByteArray())
            }
        }
    }

    /**
     * Inject a specific string into this terminal. Used for post-login strings
     * and pasting clipboard.
     */
    fun injectString(string: String?) {
        if (string == null || string.isEmpty()) {
            return
        }

        offerTransportWrite(string.toByteArray(charset(encoding)), "injectString")
    }

    /**
     * Send text from the floating input dialog as a single atomic transport write.
     * Uses CR (`\r`) as the line terminator — identical to what the hardware Enter key
     * sends via [terminalEmulator.dispatchKey] / VTermKey.ENTER.  The remote PTY line
     * discipline (ICRNL) converts `\r` to `\n` for the shell, so tmux and every other
     * interactive shell see the correct "execute" signal.
     */
    fun sendCommand(command: String) {
        if (command.isEmpty()) return
        val normalized = when {
            command.endsWith("\r\n") -> command
            command.endsWith("\n") -> command.dropLast(1) + "\r"
            command.endsWith("\r") -> command
            else -> command + "\r"
        }
        val bytes = normalized.toByteArray(charset(encoding))
        offerTransportWrite(bytes, "sendCommand")
    }

    /**
     * Sends text as a **bracketed paste** (xterm `\e[200~` … `\e[201~`), optionally followed by a newline
     * so the shell/agent sees one “paste” event instead of typed characters — matches desktop terminals
     * for many TUIs and agent CLIs. Use when plain [sendCommand] misbehaves (e.g. Cursor-style agents).
     */
    fun sendBracketedPaste(payload: String, submitWithNewline: Boolean = true) {
        if (payload.isEmpty()) return
        val normalized = payload.replace("\r\n", "\n").replace('\r', '\n')
        val out = StringBuilder()
        out.append("\u001b[200~")
        out.append(normalized)
        out.append("\u001b[201~")
        val bytes = out.toString().toByteArray(charset(encoding))
        offerTransportWrite(bytes, "sendBracketedPaste")
        if (submitWithNewline) offerTransportWrite("\r".toByteArray(charset(encoding)), "sendBracketedPasteSubmit")
    }

    /**
     * Sends multiple lines as one atomic write: each line separated by \r so the PTY
     * executes them in sequence without waiting for the previous command to finish.
     * This matches what a desktop terminal does when you paste multiple lines.
     */
    fun sendMultilineCommand(text: String) {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd()
        if (normalized.isBlank()) return
        val out = StringBuilder()
        normalized.split('\n').forEachIndexed { i, line ->
            if (i > 0) out.append('\r')
            out.append(line)
        }
        out.append('\r')
        val bytes = out.toString().toByteArray(charset(encoding))
        offerTransportWrite(bytes, "sendMultilineCommand")
    }

    /**
     * Send a raw escape sequence directly to the remote server, bypassing the
     * local terminal emulator.  Used for keys like Page Up / Page Down that
     * the emulator may consume locally instead of forwarding to the remote
     * (e.g. tmux expects to receive the actual escape sequence).
     */
    fun sendEscapeSequence(seq: String) {
        offerTransportWrite(seq.toByteArray(charset(encoding)), "escapeSequence")
    }

    /** Send Page Up escape sequence (\e[5~) to the remote */
    fun sendPageUp() = sendEscapeSequence("\u001b[5~")

    /** Send Page Down escape sequence (\e[6~) to the remote */
    fun sendPageDown() = sendEscapeSequence("\u001b[6~")

    enum class MultiplexerType { TMUX, ZELLIJ }

    private val multiplexerType: MultiplexerType
        get() = when {
            terminalEmulator.terminalTitle.contains("Zellij", ignoreCase = true) -> MultiplexerType.ZELLIJ
            else -> MultiplexerType.TMUX
        }

    val usesTmuxScrollFallback: Boolean
        get() = multiplexerType == MultiplexerType.TMUX

    // True while the app deliberately entered a multiplexer scroll/copy mode.
    @Volatile
    var inAltScreenScrollMode: Boolean = false

    private fun startAltScreenMouseModeObserver() {
        // Reset flag when alt screen exits.
        scope.launch {
            terminalEmulator.isAlternateScreenFlow.collect { isAlt ->
                if (!isAlt) inAltScreenScrollMode = false
            }
        }
    }

    /**
     * Alternate-screen fallback when the app owns multiplexer scrolling.
     *
     * Tmux mouse-wheel bindings vary per pane: panes running mouse-aware TUIs can
     * receive forwarded wheel events instead of clean history movement. For tmux
     * sessions selected by the UI preset, enter copy-mode explicitly and make tap
     * / keyboard input leave it via [exitAltScreenScrollMode].
     */
    fun sendAltScrollUp() {
        when (multiplexerType) {
            MultiplexerType.TMUX -> {
                if (!inAltScreenScrollMode) {
                    inAltScreenScrollMode = true
                    sendEscapeSequence("\u0002[") // Ctrl+B then [ enters copy-mode with the default prefix.
                }
                sendEscapeSequence("\u001b[5~") // Page Up in copy-mode.
            }
            MultiplexerType.ZELLIJ -> {
                if (!inAltScreenScrollMode) {
                    inAltScreenScrollMode = true
                    sendEscapeSequence("\u0013") // Ctrl+S enters scroll mode
                }
                sendEscapeSequence("\u001b[5~")
            }
        }
    }

    /** Scroll forward on alternate screen. */
    fun sendAltScrollDown() {
        when (multiplexerType) {
            MultiplexerType.TMUX -> {
                if (inAltScreenScrollMode) sendEscapeSequence("\u001b[6~")
            }
            MultiplexerType.ZELLIJ -> sendEscapeSequence("\u001b[6~")
        }
    }

    /**
     * Exit app-entered multiplexer scroll mode on tap/key input.
     */
    fun exitAltScreenScrollMode() {
        if (!inAltScreenScrollMode) return
        inAltScreenScrollMode = false
        when (multiplexerType) {
            MultiplexerType.TMUX -> sendEscapeSequence("\u001b")
            MultiplexerType.ZELLIJ -> sendEscapeSequence("\u0013")
        }
    }

    /** Jump to live view (↓ Live button). */
    fun jumpAltScrollToBottom() {
        if (!inAltScreenScrollMode) return
        inAltScreenScrollMode = false
        when (multiplexerType) {
            MultiplexerType.TMUX -> sendEscapeSequence("\u001b")
            MultiplexerType.ZELLIJ -> sendEscapeSequence("\u0013")
        }
    }

    /**
     * Send Ctrl+S (ASCII DC3) to the remote.
     * Zellij uses this to enter/exit scroll mode.
     */
    fun sendCtrlS() = sendEscapeSequence("\u0013")

    /** Tmux default prefix (Ctrl+B, STX). */
    fun sendCtrlB() = sendEscapeSequence("\u0002")

    /**
     * Ctrl+D (EOT). Often used after a multiplexer prefix (e.g. tmux detach) or shell EOF.
     */
    fun sendCtrlD() = sendEscapeSequence("\u0004")

    /**
     * Request the parent ConsoleScreen to open the floating text input dialog.
     * Called from hardware camera button or other triggers.
     */
    fun requestOpenTextInput() {
        onTextInputRequested?.invoke()
    }

    /**
     * Internal method to request actual PTY terminal once we've finished
     * authentication. If called before authenticated, it will just fail.
     */
    fun onConnected() {
        if (awaitingClose) {
            Timber.w("onConnected() called but bridge is already awaiting close – ignoring")
            return
        }
        disconnected = false

        // We no longer need our local output.
        localOutput.clear()

        // previously tried vt100 and xterm for emulation modes
        // "screen" works the best for color and escape codes
        // TODO(Terminal): send TERM variable in response to VT control code ENQ
//        (buffer as vt320).setAnswerBack(emulation)

        // TODO(Terminal): set whether backspace is del (for local echo?)
//        if (HostConstants.DELKEY_BACKSPACE == host.delKey)
//            (buffer as vt320).setBackspace(vt320.DELETE_IS_BACKSPACE)
//        else
//            (buffer as vt320).setBackspace(vt320.DELETE_IS_DEL)

        if (isSessionOpen) {
            // create thread to relay incoming connection data to buffer
            transport?.let { t ->
                relay = Relay(this, t, dispatchers, encoding)
                scope.launch {
                    relay?.start()
                }
            }
            startStyledHistoryAccumulator()
        }

        // When fixed size is set, the Terminal composable locks to that size
        // from the first frame — no settling needed. Otherwise, suppress resize
        // events during the initial connection period and send ONE clean resize.
        val fixedCols = host.fixedCols
        val fixedRows = host.fixedRows
        val hasFixedSize = fixedCols != null && fixedRows != null &&
            fixedCols > 0 && fixedRows > 0
        if (!hasFixedSize) {
            resizeSuppressedUntil = System.currentTimeMillis() + RESIZE_SETTLE_MS
            resizeFlushJob?.cancel()
            resizeFlushJob = scope.launch {
                kotlinx.coroutines.delay(RESIZE_SETTLE_MS)
                resizeSuppressedUntil = 0L
                pendingResize?.let { transportOperations.trySend(it) }
                pendingResize = null
            }
        }

        // Send post-login string after the shell prompt appears.
        // Wait for the relay to receive its first data (server banner/MOTD),
        // then wait for output to settle before injecting the command.
        if (!host.postLogin.isNullOrEmpty()) {
            scope.launch {
                try {
                    kotlinx.coroutines.withTimeout(10_000L) {
                        relay?.firstDataReceived?.await()
                    }
                    kotlinx.coroutines.delay(800L)
                } catch (_: Exception) {
                    // Timeout or cancellation - try anyway
                }
                val command = host.postLogin!!.trimEnd()
                injectString(if (command.endsWith("\r")) command else "$command\r")
            }
        }

        // Reset fallback scroll state when alternate-screen mode exits.
        startAltScreenMouseModeObserver()

        // Capture network state after successful connection
        captureNetworkState()

        sshKeepAliveJob?.cancel()
        if (transport is SSH && manager.prefs.getBoolean(PreferenceConstants.KEEP_ALIVE, true)) {
            sshKeepAliveJob = scope.launch(dispatchers.io) {
                while (isActive && transport is SSH && transport?.isSessionOpen() == true) {
                    delay(SSH_KEEPALIVE_INTERVAL_MS)
                    (transport as? SSH)?.sendConnectionKeepalive()
                }
            }
        }

        // Notify manager so the UI recomposes with updated connection state
        manager.notifyBridgeStateChanged()
    }

    /**
     * @return whether a session is open or not
     */
    val isSessionOpen: Boolean
        get() {
            if (transport != null) {
                return transport?.isSessionOpen() == true
            }
            return false
        }

    fun setOnDisconnectedListener(disconnectListener: BridgeDisconnectedListener?) {
        disconnectListeners.clear()
        if (disconnectListener != null) {
            disconnectListeners.add(disconnectListener)
        }
    }

    /**
     * Force disconnection of this terminal bridge.
     */
    fun dispatchDisconnect(immediate: Boolean) {
        sshKeepAliveJob?.cancel()
        sshKeepAliveJob = null
        // We don't need to do this multiple times.
        synchronized(this) {
            if (disconnected && !immediate) {
                return
            }

            disconnected = true
        }

        // Cancel any pending prompts
        promptManager.cancelPrompt()

        // disconnection request hangs if we havent really connected to a host yet
        // temporary fix is to just spawn disconnection into a thread
        scope.launch(dispatchers.io) {
            transport?.let {
                if (it.isConnected()) {
                    it.close()
                }
            }
        }

        if (immediate || (host.quickDisconnect && !host.stayConnected)) {
            awaitingClose = true
            triggerDisconnectListener()
        } else {
            if (host.stayConnected) {
                manager.requestReconnect(this)
            }
            // Notify UI so the reconnect/close overlay appears (or updates)
            manager.notifyBridgeStateChanged()
        }
    }

    /**
     * Tells the TerminalManager that we can be destroyed now.
     */
    private fun triggerDisconnectListener() {
        Timber.i("Triggering disconnect for ${host.nickname}")
        if (disconnectListeners.isEmpty()) {
            cleanup()
            return
        }

        // Notify listeners synchronously — MutableStateFlow is thread-safe
        // so there's no need to post to the main dispatcher (which can be
        // lost when the scope is cancelled during cleanup).
        for (listener in disconnectListeners) {
            listener.onDisconnected(this@TerminalBridge)
        }
        cleanup()
    }

    /**
     * Estimate terminal columns/rows from screen size and font metrics.
     * Accounts for system bars, tab bar, bottom toolbar, and soft keyboard.
     * Falls back to 80x24 if metrics aren't available yet.
     */
    fun getEstimatedDimensions(): Pair<Int, Int> = estimateTerminalDimensions()

    private fun estimateTerminalDimensions(): Pair<Int, Int> {
        if (charWidth <= 0 || charHeight <= 0) return Pair(80, 24)

        try {
            val dm = manager.resources.displayMetrics
            val screenWidthPx = dm.widthPixels
            val screenHeightPx = dm.heightPixels
            val density = dm.density

            // Approximate UI chrome heights in dp → px
            val statusBarDp = 24f
            val tabBarDp = 40f
            val bottomBarDp = 48f
            // Estimate keyboard takes ~40% of screen height
            val keyboardPx = (screenHeightPx * 0.40f).toInt()

            val chromePx = ((statusBarDp + tabBarDp + bottomBarDp) * density).toInt()
            val availableWidth = screenWidthPx
            val availableHeight = screenHeightPx - chromePx - keyboardPx

            val cols = (availableWidth / charWidth).coerceIn(20, 300)
            val rows = (availableHeight / charHeight).coerceIn(4, 100)

            Timber.d("Estimated terminal size: ${cols}x$rows (screen: ${screenWidthPx}x$screenHeightPx, char: ${charWidth}x$charHeight)")
            return Pair(cols, rows)
        } catch (e: Exception) {
            Timber.w(e, "Failed to estimate terminal dimensions, using defaults")
            return Pair(80, 24)
        }
    }

    @Synchronized
    fun tryKeyVibrate() {
        manager.tryKeyVibrate()
    }

    /**
     * Request a different font size. Will make call to parentChanged() to make
     * sure we resize PTY if needed.
     *
     * @param sizeSp Size of font in sp
     */
    private fun setFontSize(sizeDp: Float) {
        setFontSize(sizeDp, false)
    }

    /**
     * Request a different font size. Will make call to parentChanged() to make
     * sure we resize PTY if needed.
     *
     * @param sizeSp Size of font in sp
     * @param isForced whether the font size was forced
     */
    private fun setFontSize(sizeSp: Float, isForced: Boolean) {
        if (sizeSp <= 0.0) {
            return
        }

        defaultPaint.textSize = sizeSp
        fontSizeSp = sizeSp
        _fontSizeFlow.value = sizeSp

        // read new metrics to get exact pixel dimensions
        val fm = defaultPaint.fontMetrics
        charTop = Math.ceil(fm.top.toDouble()).toInt()

        val widths = FloatArray(1)
        defaultPaint.getTextWidths("X", widths)
        charWidth = Math.ceil(widths[0].toDouble()).toInt()
        charHeight = Math.ceil((fm.descent - fm.top).toDouble()).toInt()

        forcedSize = isForced

//        // refresh any bitmap with new font size
//        parent?.let { parentChanged(it) }

        for (ofscl in fontSizeChangedListeners) {
            ofscl.onFontSizeChanged(sizeSp)
        }
        // Note: Font size is now stored in profiles, not hosts.
        // Runtime font size changes are session-only and not persisted.
    }

//    /**
//     * Something changed in our parent [TerminalView], maybe it's a new
//     * parent, or maybe it's an updated font size. We should recalculate
//     * terminal size information and request a PTY resize.
//     */
//    @Synchronized
//    fun parentChanged(parent: TerminalView) {
//        if (!manager.isResizeAllowed()) {
//            Timber.d("Resize is not allowed now")
//            return
//        }
//
//        this.parent = parent
//        val width = parent.width
//        val height = parent.height
//
//        // Something has gone wrong with our layout; we're 0 width or height!
//        if (width <= 0 || height <= 0)
//            return
//
//        val clipboard = parent.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//        keyListener.setClipboardManager(clipboard)
//
//        if (!forcedSize) {
//            // recalculate buffer size
//            val newColumns: Int
//            val newRows: Int
//
//            newColumns = width / charWidth
//            newRows = height / charHeight
//
//            columns = newColumns
//            rows = newRows
//            refreshOverlayFontSize()
//        }
//
//        // clear out any old buffer information
//        defaultPaint.color = android.graphics.Color.BLACK
//        canvas.drawPaint(defaultPaint)
//
//        // Stroke the border of the terminal if the size is being forced;
//        if (forcedSize) {
//            val borderX = (columns * charWidth) + 1
//            val borderY = (rows * charHeight) + 1
//
//            defaultPaint.color = android.graphics.Color.GRAY
//            defaultPaint.strokeWidth = 0.0f
//            if (width >= borderX)
//                canvas.drawLine(borderX.toFloat(), 0f, borderX.toFloat(), (borderY + 1).toFloat(), defaultPaint)
//            if (height >= borderY)
//                canvas.drawLine(0f, borderY.toFloat(), (borderX + 1).toFloat(), borderY.toFloat(), defaultPaint)
//        }
//
//        try {
//            transport?.setDimensions(columns, rows, width, height)
//        } catch (e: Exception) {
//            Timber.e(e, "Problem while trying to resize screen or PTY")
//        }
//
//        // redraw local output if we don't have a session to receive our resize request
//        if (transport == null) {
//            // TODO(Terminal): write local output directly to display
// //            synchronized(localOutput) {
// //                (buffer as vt320).reset()
// //
// //                for (line in localOutput)
// //                    (buffer as vt320).putString(line)
// //            }
//        }
//
//        parent.notifyUser(String.format("%d x %d", columns, rows))
//
//        Timber.i(String.format("parentChanged() now width=%d, height=%d", columns, rows))
//    }

    /**
     * Clean up resources when bridge is being destroyed.
     * Releases bitmap and clears parent reference to prevent memory leaks.
     */
    fun cleanup() {
        // Cancel grace period if active
        networkGracePeriodJob?.cancel()
        inGracePeriod = false

        profileObservationJob?.cancel()
        transportOperations.close()
        scope.cancel()
    }

    /**
     * @return whether underlying transport can forward ports
     */
    fun canFowardPorts(): Boolean = transport?.canForwardPorts() ?: false

    /**
     * Adds the [PortForward] to the list.
     * @param portForward the port forward bean to add
     * @return true on successful addition
     */
    fun addPortForward(portForward: PortForward): Boolean = transport?.addPortForward(portForward) ?: false

    /**
     * Removes the [PortForward] from the list.
     * @param portForward the port forward bean to remove
     * @return true on successful removal
     */
    fun removePortForward(portForward: PortForward): Boolean = transport?.removePortForward(portForward) ?: false

    /**
     * @return the list of port forwards
     */
    val portForwards: List<PortForward>
        get() = transport?.getPortForwards().orEmpty()

    /**
     * Enables a port forward member. After calling this method, the port forward should
     * be operational.
     * @param portForward member of our current port forwards list to enable
     * @return true on successful port forward setup
     */
    fun enablePortForward(portForward: PortForward): Boolean {
        return transport?.let {
            if (!it.isConnected()) {
                Timber.i("Attempt to enable port forward while not connected")
                return false
            }
            it.enablePortForward(portForward)
        } ?: false
    }

    /**
     * Disables a port forward member. After calling this method, the port forward should
     * be non-functioning.
     * @param portForward member of our current port forwards list to enable
     * @return true on successful port forward tear-down
     */
    fun disablePortForward(portForward: PortForward): Boolean {
        return transport?.let {
            if (!it.isConnected()) {
                Timber.i("Attempt to disable port forward while not connected")
                return false
            }
            it.disablePortForward(portForward)
        } ?: false
    }

    /**
     * @return whether the TerminalBridge should close
     */
    fun isAwaitingClose(): Boolean = awaitingClose

    /**
     * @return whether this connection had started and subsequently disconnected
     */
    val isDisconnected: Boolean
        get() = disconnected

    private object PatternHolder {
        val urlPattern: Pattern

        init {
            // based on http://www.ietf.org/rfc/rfc2396.txt
            val scheme = "[A-Za-z][-+.0-9A-Za-z]*"
            val unreserved = "[-._~0-9A-Za-z]"
            val pctEncoded = "%[0-9A-Fa-f]{2}"
            val subDelims = "[!${'$'}&'()*+,;:=]"
            val userinfo = "(?:$unreserved|$pctEncoded|$subDelims|:)*"
            val h16 = "[0-9A-Fa-f]{1,4}"
            val decOctet = "(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])"
            val ipv4address = "$decOctet\\.$decOctet\\.$decOctet\\.$decOctet"
            val ls32 = "(?:$h16:$h16|$ipv4address)"
            val ipv6address = "(?:(?:$h16){6}$ls32)"
            val ipvfuture = "v[0-9A-Fa-f]+.(?:$unreserved|$subDelims|:)+"
            val ipLiteral = "\\[(?:$ipv6address|$ipvfuture)\\]"
            val regName = "(?:$unreserved|$pctEncoded|$subDelims)*"
            val host = "(?:$ipLiteral|$ipv4address|$regName)"
            val port = "[0-9]*"
            val authority = "(?:$userinfo@)?$host(?::$port)?"
            val pchar = "(?:$unreserved|$pctEncoded|$subDelims|@)"
            val segment = "$pchar*"
            val pathAbempty = "(?:/$segment)*"
            val segmentNz = "$pchar+"
            val pathAbsolute = "/(?:$segmentNz(?:/$segment)*)?"
            val pathRootless = "$segmentNz(?:/$segment)*"
            val hierPart = "(?://$authority$pathAbempty|$pathAbsolute|$pathRootless)"
            val query = "(?:$pchar|/|\\?)*"
            val fragment = "(?:$pchar|/|\\?)*"
            val uriRegex = "$scheme:$hierPart(?:$query)?(?:#$fragment)?"
            urlPattern = Pattern.compile(uriRegex)
        }
    }

    /**
     * @return
     */
    fun scanForURLs(): List<String> {
        val urls = mutableListOf<String>()

        val allText = buildString {
            for (line in getRecentOutputLines()) {
                append(line)
                append(' ')
            }
            synchronized(localOutput) {
                for (line in localOutput) {
                    append(line)
                    append(' ')
                }
            }
        }

        val urlMatcher = PatternHolder.urlPattern.matcher(allText)
        while (urlMatcher.find()) {
            urlMatcher.group()?.let { urls.add(it) }
        }

        return urls.toSet().toList()
    }

    /**
     * Returns the recent output lines captured by the relay.
     * Used by the CLI prompt detector to identify interactive prompts.
     *
     * @return list of recent output lines, or empty list if relay is not active
     */
    fun getRecentOutputLines(): List<String> = relay?.getRecentLines() ?: emptyList()

    /**
     * @return
     */
    fun isUsingNetwork(): Boolean = transport?.usesNetwork() ?: false

    /**
     * Capture current network state when connection established.
     * Called after successful SSH connection.
     */
    fun captureNetworkState() {
        if (!isUsingNetwork()) return

        val networkInfo = manager.connectivityMonitor.getCurrentNetworkInfo()
        if (networkInfo != null) {
            lastKnownNetworkState = NetworkState(
                ipAddresses = networkInfo.ipAddresses,
                networkId = networkInfo.networkId,
                networkType = networkInfo.networkType
            )
            Timber.d("Captured network state: ${networkInfo.ipAddresses.size} IPs")
        }
    }

    /**
     * Called by TerminalManager when network is lost.
     * Starts 60-second grace period instead of immediate disconnect.
     */
    fun onNetworkLost() {
        if (!isUsingNetwork() || disconnected) return

        // Cancel any existing grace period (rapid network changes)
        networkGracePeriodJob?.cancel()

        inGracePeriod = true

        // Show status message to user
        scope.launch { _networkStatusMessages.emit(manager.res.getString(R.string.network_lost_grace_period)) }

        // Start 60-second timer
        networkGracePeriodJob = scope.launch {
            delay(60_000) // 60 seconds

            // Grace period expired without network restoration
            inGracePeriod = false
            lastKnownNetworkState = null
            Timber.i("Network grace period expired")
            _networkStatusMessages.emit(manager.res.getString(R.string.network_grace_period_expired))

            // Trigger normal disconnect flow
            dispatchDisconnect(immediate = false)
        }
    }

    /**
     * Called by TerminalManager when network is restored.
     * Checks if IP address changed to decide reconnect vs resume.
     */
    fun onNetworkRestored(newNetworkInfo: ConnectivityMonitor.NetworkInfo) {
        if (!inGracePeriod) return

        // Cancel grace period timer
        networkGracePeriodJob?.cancel()
        inGracePeriod = false

        val oldState = lastKnownNetworkState

        if (oldState == null) {
            // No previous state - treat as new connection
            scope.launch { _networkStatusMessages.emit(manager.res.getString(R.string.network_restored_no_previous_state)) }
            lastKnownNetworkState = NetworkState(
                ipAddresses = newNetworkInfo.ipAddresses,
                networkId = newNetworkInfo.networkId,
                networkType = newNetworkInfo.networkType
            )
            // Allow connection to continue
            return
        }

        // Check if ANY IP address matches (lenient - handles IPv4/v6 changes)
        val ipMatches = oldState.ipAddresses.intersect(newNetworkInfo.ipAddresses).isNotEmpty()

        if (ipMatches) {
            // Same IP - SSH session should still be alive, resume normally
            scope.launch { _networkStatusMessages.emit(manager.res.getString(R.string.network_restored_same_ip)) }
            lastKnownNetworkState = NetworkState(
                ipAddresses = newNetworkInfo.ipAddresses,
                networkId = newNetworkInfo.networkId,
                networkType = newNetworkInfo.networkType
            )
            // No action needed - connection continues
        } else if (
            oldState.networkType == newNetworkInfo.networkType &&
            newNetworkInfo.networkType == NetworkCapabilities.TRANSPORT_WIFI
        ) {
            // Wi-Fi often gets a new DHCP lease after a short dropout; SSH may still be fine.
            scope.launch { _networkStatusMessages.emit(manager.res.getString(R.string.network_restored_same_ip)) }
            lastKnownNetworkState = NetworkState(
                ipAddresses = newNetworkInfo.ipAddresses,
                networkId = newNetworkInfo.networkId,
                networkType = newNetworkInfo.networkType
            )
        } else {
            // IP changed across transports (e.g. WiFi → cellular) — TCP session is gone
            scope.launch { _networkStatusMessages.emit(manager.res.getString(R.string.network_restored_ip_changed)) }
            lastKnownNetworkState = null
            dispatchDisconnect(immediate = false)
        }
    }

    /**
     * @return whether bridge is in network grace period
     */
    fun isInGracePeriod(): Boolean = inGracePeriod

    /**
     * @return
     */
    val keyHandler: TerminalKeyListener
        get() = keyListener

    /**
     * Convenience function to increase the font size by a given step.
     */
    fun increaseFontSize() {
        setFontSize(fontSizeSp + FONT_SIZE_STEP, false)
    }

    /**
     * Convenience function to decrease the font size by a given step.
     */
    fun decreaseFontSize() {
        setFontSize(fontSizeSp - FONT_SIZE_STEP, false)
    }

    companion object {
        const val TAG = "CB.TerminalBridge"

        private const val SSH_KEEPALIVE_INTERVAL_MS = 60_000L
        private const val STYLED_HISTORY_CAPACITY = 500

        private const val DEFAULT_FONT_SIZE_SP = 10
        private const val FONT_SIZE_STEP = 2
        private const val RESIZE_DEBOUNCE_MS = 200L
        private const val RESIZE_SETTLE_MS = 2000L
    }
}
