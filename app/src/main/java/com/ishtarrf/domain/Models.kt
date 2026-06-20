package com.ishtarrf.domain

/** Modulation as understood by the firmware (`mod` field). */
enum class Modulation(val wire: String, val label: String) {
    OOK("OOK", "OOK / ASK"),
    FSK2("2-FSK", "2-FSK");

    companion object {
        fun fromWire(w: String?): Modulation = entries.firstOrNull { it.wire == w } ?: OOK
    }
}

/** RX mode passed to `rx_start`. */
enum class RxMode(val wire: String, val label: String) {
    RAW_OOK("raw_ook", "RAW (OOK)"),
    PACKET("packet", "Packet");
}

/** Mirrors the firmware's CC1101 configuration knobs. */
data class RadioConfig(
    val freqMhz: Double = 433.920,
    val modulation: Modulation = Modulation.OOK,
    val bitrateKbps: Double = 2.40,
    val deviationKhz: Double = 30.0,
    val txPowerDbm: Int = 0,
)

/**
 * A captured (or loaded) RAW signal. [pulsesUs] are unsigned microsecond
 * durations that alternate ON/OFF by index — the same representation the
 * firmware sends and expects.
 */
data class CapturedSignal(
    val pulsesUs: List<Int>,
    val freqMhz: Double,
    val modulation: Modulation = Modulation.OOK,
    val startNegative: Boolean = false,
    val rssiDbm: Int? = null,
    val durMs: Int? = null,
) {
    val totalDurationUs: Long get() = pulsesUs.sumOf { it.toLong() }
}

enum class LogLevel { INFO, OK, RX, TX, RSSI, ERROR, DEVICE }

data class LogEntry(val seq: Long, val level: LogLevel, val message: String)

/** A saved `.sub` file on disk. */
data class SubEntry(val name: String, val folder: String, val path: String)

/** One sample produced by the frequency scanner. */
data class ScanPoint(val freqMhz: Double, val rssiDbm: Int)

enum class AppTheme(val key: String, val label: String) {
    ISHTAR("ishtar", "IshtarRF"),
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromKey(k: String?): AppTheme = entries.firstOrNull { it.key == k } ?: ISHTAR
    }
}
