package com.ishtarrf.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ishtarrf.IshtarApp
import com.ishtarrf.data.prefs.SettingsRepository
import com.ishtarrf.data.protocol.Commands
import com.ishtarrf.data.protocol.DeviceEvent
import com.ishtarrf.data.protocol.EventParser
import com.ishtarrf.data.serial.ConnectionState
import com.ishtarrf.data.serial.UsbSerialManager
import com.ishtarrf.data.sub.SubFile
import com.ishtarrf.data.sub.SubLibraryRepository
import com.ishtarrf.domain.AppTheme
import com.ishtarrf.domain.CapturedSignal
import com.ishtarrf.domain.LogEntry
import com.ishtarrf.domain.LogLevel
import com.ishtarrf.domain.Modulation
import com.ishtarrf.domain.RadioConfig
import com.ishtarrf.domain.RxMode
import com.ishtarrf.domain.ScanPoint
import com.ishtarrf.domain.SubEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val deviceAttached: Boolean = false,
    val config: RadioConfig = RadioConfig(),
    val rxMode: RxMode = RxMode.RAW_OOK,
    val isReceiving: Boolean = false,
    val lastRssi: Int? = null,
    val currentSignal: CapturedSignal? = null,
    val txHexText: String = "A10B0C0D",
    val txPulsesText: String = "350,1200,350,1200",
    val startNegative: Boolean = false,
    val log: List<LogEntry> = emptyList(),
    val theme: AppTheme = AppTheme.ISHTAR,
    val favorites: List<Double> = SettingsRepository.DEFAULT_FAVORITES,
    // library
    val folders: List<String> = listOf(SubLibraryRepository.DEFAULT),
    val selectedFolder: String = SubLibraryRepository.DEFAULT,
    val library: List<SubEntry> = emptyList(),
    // tools
    val rssiHistory: List<Int> = emptyList(),
    val rssiMonitorOn: Boolean = false,
    val scanResults: List<ScanPoint> = emptyList(),
    val toolStatus: String = "",
    val toolBusy: Boolean = false,
)

class MainViewModel(
    private val serial: UsbSerialManager,
    private val settings: SettingsRepository,
    private val library: SubLibraryRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _ui.asStateFlow()

    private var logSeq = 0L
    private var pingedFor: ConnectionState? = null

    private var rssiMonitorJob: Job? = null
    private var scanJob: Job? = null
    private var bruteJob: Job? = null
    private var scanningFreq: Double? = null

    // Auto-reconnect: re-establish a dropped link (e.g. if the ESP32 resets and
    // the USB device re-enumerates) as long as the user didn't disconnect.
    private var autoReconnect = false
    private var reconnectAttempts = 0

    init {
        // Settings
        viewModelScope.launch { settings.theme.collect { t -> _ui.update { it.copy(theme = t) } } }
        viewModelScope.launch { settings.favorites.collect { f -> _ui.update { it.copy(favorites = f) } } }

        // Serial connection lifecycle
        viewModelScope.launch {
            serial.state.collect { st ->
                _ui.update { it.copy(connection = st) }
                when (st) {
                    is ConnectionState.Connected -> {
                        reconnectAttempts = 0
                        if (pingedFor != st) {
                            pingedFor = st
                            log(LogLevel.INFO, "[+] Connected to ${st.deviceName}")
                            serial.write(Commands.ping())
                        }
                    }
                    is ConnectionState.Error -> {
                        pingedFor = null
                        _ui.update { it.copy(isReceiving = false, rssiMonitorOn = false) }
                        log(LogLevel.ERROR, "[!] ${st.message}")
                        maybeAutoReconnect()
                    }
                    is ConnectionState.Disconnected -> {
                        pingedFor = null
                        _ui.update { it.copy(isReceiving = false, rssiMonitorOn = false) }
                    }
                    ConnectionState.Connecting -> log(LogLevel.INFO, "Requesting USB permission…")
                }
            }
        }

        // Incoming device events
        viewModelScope.launch {
            serial.lines.collect { line -> handleEvent(EventParser.parse(line), line) }
        }

        refreshDevices()
        refreshLibrary()
    }

    // ---------------------------- Connection ----------------------------

    fun refreshDevices() {
        _ui.update { it.copy(deviceAttached = serial.hasDevice()) }
    }

    fun toggleConnect() {
        if (_ui.value.connection.isConnected) {
            autoReconnect = false
            serial.disconnect()
        } else {
            autoReconnect = true
            reconnectAttempts = 0
            serial.connect()
        }
    }

    private fun maybeAutoReconnect() {
        if (!autoReconnect || reconnectAttempts >= MAX_RECONNECT || !serial.hasDevice()) return
        reconnectAttempts++
        viewModelScope.launch {
            log(LogLevel.INFO, "Reconnecting… (attempt $reconnectAttempts/$MAX_RECONNECT)")
            delay(1200)
            if (autoReconnect && !_ui.value.connection.isConnected && serial.hasDevice()) {
                serial.connect()
            }
        }
    }

    /** Re-initialises the radio on the device (firmware `recover` command). */
    fun recover() {
        send(Commands.recover(), LogLevel.TX, "recover")
    }

    // ---------------------------- Radio config ----------------------------

    fun applyConfig(config: RadioConfig) {
        _ui.update { it.copy(config = config) }
        send(Commands.setConfig(config), LogLevel.TX, "set_config")
    }

    fun setRxMode(mode: RxMode) = _ui.update { it.copy(rxMode = mode) }

    fun startRx() {
        val mode = _ui.value.rxMode
        val timeout = if (mode == RxMode.RAW_OOK) 40 else 0
        _ui.update { it.copy(isReceiving = true) }
        send(Commands.rxStart(mode, timeout), LogLevel.TX, "rx_start (${mode.label})")
    }

    fun stopRx() {
        _ui.update { it.copy(isReceiving = false) }
        send(Commands.rxStop(), LogLevel.TX, "rx_stop")
    }

    fun requestRssi() = send(Commands.getRssi(), LogLevel.TX, "get_rssi")

    // ---------------------------- Transmit ----------------------------

    fun setTxHex(text: String) = _ui.update { it.copy(txHexText = text) }

    fun txHex() {
        val hex = _ui.value.txHexText.replace(" ", "")
        if (hex.isEmpty() || hex.length % 2 != 0 || !hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            log(LogLevel.ERROR, "[!] Invalid HEX")
            return
        }
        send(Commands.txBytes(hex), LogLevel.TX, "tx_bytes $hex")
    }

    fun setTxPulses(text: String) = _ui.update { it.copy(txPulsesText = text) }

    fun setStartNegative(value: Boolean) = _ui.update { it.copy(startNegative = value) }

    fun txRaw(repeat: Int, gapMs: Int, invert: Boolean) {
        val pulses = parsePulses(_ui.value.txPulsesText) ?: run {
            log(LogLevel.ERROR, "[!] Invalid pulses list")
            return
        }
        send(Commands.txRaw(pulses, repeat, gapMs, invert), LogLevel.TX, "tx_raw (${pulses.size} pulses)")
    }

    fun replayCurrent(repeat: Int, gapMs: Int, invert: Boolean) {
        val pulses = _ui.value.currentSignal?.pulsesUs
        if (pulses.isNullOrEmpty()) {
            log(LogLevel.ERROR, "[!] No signal to replay")
            return
        }
        send(Commands.txRaw(pulses, repeat, gapMs, invert), LogLevel.TX, "replay (${pulses.size} pulses)")
    }

    /** Discards the last captured/loaded RAW signal and the editable pulses. */
    fun clearSignal() {
        if (_ui.value.currentSignal == null && _ui.value.txPulsesText.isEmpty()) return
        _ui.update { it.copy(currentSignal = null, txPulsesText = "") }
        log(LogLevel.INFO, "[-] Cleared captured signal")
    }

    // ---------------------------- Library ----------------------------

    fun selectFolder(folder: String) {
        _ui.update { it.copy(selectedFolder = folder) }
        refreshLibrary()
    }

    fun refreshLibrary() {
        viewModelScope.launch(Dispatchers.IO) {
            val folders = library.folders()
            val selected = _ui.value.selectedFolder.takeIf { it in folders } ?: SubLibraryRepository.DEFAULT
            val items = library.list(selected)
            _ui.update { it.copy(folders = folders, selectedFolder = selected, library = items) }
        }
    }

    fun saveCurrentAsSub(name: String, folder: String) {
        val sig = _ui.value.currentSignal
        if (sig == null || sig.pulsesUs.isEmpty()) {
            log(LogLevel.ERROR, "[!] No RAW signal to save")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val content = SubFile.export(
                freqMhz = sig.freqMhz,
                pulsesUs = sig.pulsesUs,
                startNegative = _ui.value.startNegative,
                preset = SubFile.presetFor(sig.modulation),
            )
            val entry = library.save(folder, name, content)
            log(LogLevel.OK, "[+] Saved ${entry.name}.sub")
            refreshLibrary()
        }
    }

    fun importSub(name: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            library.save(_ui.value.selectedFolder, name, content)
            log(LogLevel.OK, "[+] Imported $name.sub")
            refreshLibrary()
        }
    }

    fun loadSignal(entry: SubEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = SubFile.parse(library.read(entry))
            if (parsed.pulsesUs.isEmpty()) {
                log(LogLevel.ERROR, "[!] Failed to parse ${entry.name}.sub")
                return@launch
            }
            val freqMhz = parsed.frequencyHz?.let { it / 1_000_000.0 } ?: _ui.value.config.freqMhz
            val signal = CapturedSignal(
                pulsesUs = parsed.pulsesUs,
                freqMhz = freqMhz,
                modulation = Modulation.OOK,
                startNegative = parsed.startNegative,
            )
            _ui.update {
                it.copy(
                    currentSignal = signal,
                    txPulsesText = parsed.pulsesUs.joinToString(","),
                    startNegative = parsed.startNegative,
                    config = it.config.copy(
                        freqMhz = freqMhz,
                        modulation = Modulation.OOK,
                    ),
                )
            }
            log(LogLevel.INFO, "[Loaded] ${entry.name}.sub  pulses=${parsed.pulsesUs.size}")
        }
    }

    fun deleteSignal(entry: SubEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            library.delete(entry)
            log(LogLevel.INFO, "[-] Deleted ${entry.name}.sub")
            refreshLibrary()
        }
    }

    fun renameSignal(entry: SubEntry, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            library.rename(entry, newName)
            refreshLibrary()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            library.createFolder(name)
            refreshLibrary()
        }
    }

    suspend fun readSub(entry: SubEntry): String = withContext(Dispatchers.IO) { library.read(entry) }

    fun fileFor(entry: SubEntry) = library.fileFor(entry)

    // ---------------------------- Settings ----------------------------

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settings.setTheme(theme) }
    }

    fun addFavorite(freqMhz: Double) {
        viewModelScope.launch {
            val next = (_ui.value.favorites + freqMhz).distinct().sorted()
            settings.setFavorites(next)
        }
    }

    fun removeFavorite(freqMhz: Double) {
        viewModelScope.launch {
            settings.setFavorites(_ui.value.favorites.filterNot { it == freqMhz })
        }
    }

    fun clearLog() = _ui.update { it.copy(log = emptyList()) }

    fun exportLog(): String = _ui.value.log.joinToString("\n") { it.message }

    // ---------------------------- Tools ----------------------------

    fun toggleRssiMonitor() {
        if (_ui.value.rssiMonitorOn) {
            rssiMonitorJob?.cancel()
            _ui.update { it.copy(rssiMonitorOn = false) }
        } else {
            _ui.update { it.copy(rssiMonitorOn = true, rssiHistory = emptyList()) }
            rssiMonitorJob = viewModelScope.launch {
                while (isActive && _ui.value.connection.isConnected) {
                    serial.write(Commands.getRssi())
                    delay(300)
                }
                _ui.update { it.copy(rssiMonitorOn = false) }
            }
        }
    }

    /**
     * Sweeps OOK RSSI across a frequency range. Approximate: for each step it
     * reconfigures the radio, asks for RSSI, and records whatever comes back.
     */
    fun startScan(startMhz: Double, endMhz: Double, stepMhz: Double) {
        if (!_ui.value.connection.isConnected) {
            log(LogLevel.ERROR, "[!] Connect first")
            return
        }
        if (stepMhz <= 0 || endMhz < startMhz) {
            log(LogLevel.ERROR, "[!] Invalid scan range")
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _ui.update { it.copy(scanResults = emptyList(), toolBusy = true, toolStatus = "Scanning…") }
            var f = startMhz
            while (f <= endMhz + 1e-6 && isActive) {
                scanningFreq = f
                serial.write(Commands.setConfig(_ui.value.config.copy(freqMhz = f, modulation = Modulation.OOK)))
                delay(70)
                serial.write(Commands.getRssi())
                delay(90)
                _ui.update { it.copy(toolStatus = "Scanning ${"%.2f".format(f)} MHz") }
                f += stepMhz
            }
            scanningFreq = null
            _ui.update {
                val peak = it.scanResults.maxByOrNull { p -> p.rssiDbm }
                it.copy(
                    toolBusy = false,
                    toolStatus = peak?.let { p -> "Peak: ${"%.2f".format(p.freqMhz)} MHz @ ${p.rssiDbm} dBm" }
                        ?: "Scan complete",
                )
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        scanningFreq = null
        _ui.update { it.copy(toolBusy = false, toolStatus = "Scan stopped") }
    }

    /**
     * Sequential OOK code brute-force using PWM bit encoding (PT2262/EV1527 style).
     * Each code is encoded as alternating high/low µs pulses and transmitted.
     * NB: only use on devices you own and frequencies you are licensed for.
     */
    fun startBruteForce(
        bits: Int,
        shortUs: Int,
        longUs: Int,
        count: Int,
        repeatEach: Int,
        gapMs: Int,
    ) {
        if (!_ui.value.connection.isConnected) {
            log(LogLevel.ERROR, "[!] Connect first")
            return
        }
        bruteJob?.cancel()
        bruteJob = viewModelScope.launch {
            _ui.update { it.copy(toolBusy = true, toolStatus = "Brute-force…") }
            serial.write(Commands.setConfig(_ui.value.config.copy(modulation = Modulation.OOK)))
            delay(60)
            val total = count.coerceIn(1, 1 shl 20)
            for (code in 0 until total) {
                if (!isActive || !_ui.value.connection.isConnected) break
                val pulses = encodePwm(code, bits, shortUs, longUs)
                serial.write(Commands.txRaw(pulses, repeatEach, gapMs, false))
                _ui.update { it.copy(toolStatus = "Brute-force ${code + 1}/$total") }
                delay((pulses.sum() / 1000L * repeatEach + 40).coerceAtLeast(60L))
            }
            _ui.update { it.copy(toolBusy = false, toolStatus = "Brute-force done") }
        }
    }

    fun stopBruteForce() {
        bruteJob?.cancel()
        _ui.update { it.copy(toolBusy = false, toolStatus = "Brute-force stopped") }
    }

    private fun encodePwm(code: Int, bits: Int, shortUs: Int, longUs: Int): List<Int> {
        val out = ArrayList<Int>(bits * 2 + 2)
        for (i in bits - 1 downTo 0) {
            val bit = (code shr i) and 1
            if (bit == 0) {
                out.add(shortUs); out.add(longUs)   // '0' : short high, long low
            } else {
                out.add(longUs); out.add(shortUs)   // '1' : long high, short low
            }
        }
        // sync gap (long low) terminates the frame
        out.add(shortUs)
        out.add(longUs * 31)
        return out
    }

    // ---------------------------- Internals ----------------------------

    private fun handleEvent(event: DeviceEvent, raw: String) {
        when (event) {
            DeviceEvent.Pong -> log(LogLevel.INFO, "[pong]")
            is DeviceEvent.Ok -> log(LogLevel.OK, "[OK] ${event.of}")
            is DeviceEvent.Error -> log(LogLevel.ERROR, "[!] ${event.msg}")
            is DeviceEvent.Rssi -> {
                recordRssi(event.valueDbm)
                if (!_ui.value.rssiMonitorOn && scanningFreq == null) {
                    log(LogLevel.RSSI, "[RSSI] ${event.valueDbm} dBm")
                }
            }
            is DeviceEvent.RxBytes -> {
                _ui.update { it.copy(lastRssi = event.rssiDbm) }
                log(LogLevel.RX, "[RX bytes] ${event.hex} @ ${event.rssiDbm} dBm")
            }
            is DeviceEvent.RxRaw -> {
                val signal = CapturedSignal(
                    pulsesUs = event.pulsesUs,
                    freqMhz = _ui.value.config.freqMhz,
                    modulation = _ui.value.config.modulation,
                    rssiDbm = event.rssiDbm,
                    durMs = event.durMs,
                )
                _ui.update {
                    it.copy(
                        currentSignal = signal,
                        lastRssi = event.rssiDbm,
                        txPulsesText = event.pulsesUs.joinToString(","),
                    )
                }
                log(
                    LogLevel.RX,
                    "[RX raw] pulses=${event.pulsesUs.size} @ ${event.rssiDbm} dBm dur=${event.durMs} ms",
                )
            }
            is DeviceEvent.Unknown -> log(LogLevel.DEVICE, "[DEV] $raw")
        }
    }

    private fun recordRssi(value: Int) {
        val freq = scanningFreq
        _ui.update { state ->
            val history = (state.rssiHistory + value).takeLast(120)
            val scan = if (freq != null) state.scanResults + ScanPoint(freq, value) else state.scanResults
            state.copy(lastRssi = value, rssiHistory = history, scanResults = scan)
        }
    }

    private fun send(command: String, level: LogLevel, label: String) {
        if (!_ui.value.connection.isConnected) {
            log(LogLevel.ERROR, "[!] Not connected")
            return
        }
        serial.write(command)
        log(level, "→ $label")
    }

    private fun log(level: LogLevel, message: String) {
        _ui.update { state ->
            val entry = LogEntry(logSeq++, level, message)
            state.copy(log = (state.log + entry).takeLast(MAX_LOG))
        }
    }

    private fun parsePulses(text: String): List<Int>? = try {
        text.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toInt() }
            .takeIf { it.isNotEmpty() }
    } catch (_: NumberFormatException) {
        null
    }

    override fun onCleared() {
        super.onCleared()
        serial.disconnect()
    }

    companion object {
        private const val MAX_LOG = 500
        private const val MAX_RECONNECT = 3

        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    val container = (application as IshtarApp).container
                    return MainViewModel(
                        serial = container.serialManager,
                        settings = container.settings,
                        library = container.library,
                    ) as T
                }
            }
    }
}
