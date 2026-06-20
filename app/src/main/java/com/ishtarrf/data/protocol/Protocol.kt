package com.ishtarrf.data.protocol

import com.ishtarrf.domain.RadioConfig
import com.ishtarrf.domain.RxMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Builds the newline-delimited JSON command strings the firmware expects.
 *
 * The firmware hand-parses JSON (no library): it scans for `"key"` then reads
 * the value up to the next `,` or `}`. So commands must stay flat, with exact
 * key names and plain scalar/array values — which is exactly what these emit.
 */
object Commands {

    private fun JsonObject.line(): String = toString() + "\n"

    fun ping(): String = buildJsonObject { put("cmd", "ping") }.line()

    fun recover(): String = buildJsonObject { put("cmd", "recover") }.line()

    fun setConfig(cfg: RadioConfig): String = buildJsonObject {
        put("cmd", "set_config")
        put("freq", cfg.freqMhz)
        put("mod", cfg.modulation.wire)
        put("br_kbps", cfg.bitrateKbps)
        put("dev_khz", cfg.deviationKhz)
        put("tx_power", cfg.txPowerDbm)
    }.line()

    fun rxStart(mode: RxMode, timeoutMs: Int): String = buildJsonObject {
        put("cmd", "rx_start")
        put("mode", mode.wire)
        put("timeout_ms", timeoutMs)
    }.line()

    fun rxStop(): String = buildJsonObject { put("cmd", "rx_stop") }.line()

    fun getRssi(): String = buildJsonObject { put("cmd", "get_rssi") }.line()

    fun txBytes(hex: String): String = buildJsonObject {
        put("cmd", "tx_bytes")
        put("hex", hex)
    }.line()

    fun txRaw(pulsesUs: List<Int>, repeat: Int, gapMs: Int, invert: Boolean): String =
        buildJsonObject {
            put("cmd", "tx_raw")
            putJsonArray("pulses_us") { pulsesUs.forEach { add(it) } }
            put("repeat", repeat)
            put("gap_ms", gapMs)
            // The firmware reads `invert` with its string parser (valS), so it
            // must be a quoted JSON string ("true"/"false"), not a bare boolean.
            put("invert", invert.toString())
        }.line()
}

/** A decoded message coming back from the device. */
sealed interface DeviceEvent {
    data object Pong : DeviceEvent
    data class Ok(val of: String) : DeviceEvent
    data class Error(val msg: String) : DeviceEvent
    data class Rssi(val valueDbm: Int) : DeviceEvent
    data class RxBytes(val hex: String, val rssiDbm: Int) : DeviceEvent
    data class RxRaw(val pulsesUs: List<Int>, val rssiDbm: Int, val durMs: Int) : DeviceEvent
    data class Unknown(val raw: String) : DeviceEvent
}

object EventParser {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(line: String): DeviceEvent = try {
        val o = json.parseToJsonElement(line).jsonObject
        when (o["event"]?.jsonPrimitive?.contentOrNull) {
            "pong" -> DeviceEvent.Pong
            "ok" -> DeviceEvent.Ok(o["of"]?.jsonPrimitive?.contentOrNull ?: "")
            "error" -> DeviceEvent.Error(o["msg"]?.jsonPrimitive?.contentOrNull ?: "")
            "rssi" -> DeviceEvent.Rssi(o["value_dbm"]?.jsonPrimitive?.intOrNull ?: 0)
            "rx_bytes" -> DeviceEvent.RxBytes(
                hex = o["hex"]?.jsonPrimitive?.contentOrNull ?: "",
                rssiDbm = o["rssi_dbm"]?.jsonPrimitive?.intOrNull ?: 0,
            )
            "rx_raw" -> DeviceEvent.RxRaw(
                pulsesUs = o["pulses_us"]?.jsonArray?.mapNotNull { it.jsonPrimitive.intOrNull }
                    ?: emptyList(),
                rssiDbm = o["rssi_dbm"]?.jsonPrimitive?.intOrNull ?: 0,
                durMs = o["dur_ms"]?.jsonPrimitive?.intOrNull ?: 0,
            )
            else -> DeviceEvent.Unknown(line)
        }
    } catch (_: Exception) {
        DeviceEvent.Unknown(line)
    }
}
