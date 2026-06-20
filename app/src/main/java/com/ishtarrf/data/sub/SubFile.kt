package com.ishtarrf.data.sub

import com.ishtarrf.domain.Modulation
import kotlin.math.abs
import kotlin.math.roundToLong

/** Result of parsing a Flipper/Bruce-style RAW `.sub` file. */
data class ParsedSub(
    val frequencyHz: Long?,
    val pulsesUs: List<Int>,
    val startNegative: Boolean,
)

/**
 * Read/write Flipper/Bruce-compatible RAW `.sub` files.
 *
 * On-wire/firmware pulses are unsigned µs (alternating ON/OFF by index); the
 * `.sub` file stores them signed (positive = HIGH/ON, negative = LOW/OFF).
 * This mirrors `export_flipper_sub` / `parse_flipper_sub` in the desktop app.
 */
object SubFile {

    private const val WRAP = 64
    private val FREQ_REGEX = Regex("""(?m)^\s*Frequency:\s*([0-9]+)""")
    private val INT_REGEX = Regex("""-?\d+""")

    fun presetFor(mod: Modulation): String = when (mod) {
        Modulation.OOK -> "FuriHalSubGhzPresetOok270Async"
        Modulation.FSK2 -> "FuriHalSubGhzPreset2FSKDev"
    }

    fun toSigned(pulsesUs: List<Int>, startNegative: Boolean): List<Int> {
        val s = if (startNegative) -1 else 1
        return pulsesUs.mapIndexed { i, d ->
            val v = maxOf(1, d)
            v * (if (i % 2 == 0) s else -s)
        }
    }

    fun export(
        freqMhz: Double,
        pulsesUs: List<Int>,
        startNegative: Boolean,
        preset: String,
    ): String {
        val freqHz = (freqMhz * 1_000_000.0).roundToLong()
        val signed = toSigned(pulsesUs, startNegative)
        val sb = StringBuilder()
        sb.appendLine("Filetype: IshtarRF SubGhz RAW File")
        sb.appendLine("Version: 1")
        sb.appendLine("Frequency: $freqHz")
        sb.appendLine("Preset: $preset")
        sb.appendLine("Protocol: RAW")
        signed.chunked(WRAP).forEachIndexed { i, chunk ->
            val body = chunk.joinToString(" ")
            if (i == 0) sb.appendLine("RAW_Data: $body") else sb.appendLine(" $body")
        }
        return sb.toString()
    }

    fun parse(text: String): ParsedSub {
        val freqHz = FREQ_REGEX.find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val rawPos = text.indexOf("RAW_Data")
        val signed: List<Int> =
            if (rawPos >= 0) {
                INT_REGEX.findAll(text.substring(rawPos))
                    .mapNotNull { it.value.toIntOrNull() }
                    .toList()
            } else {
                emptyList()
            }
        if (signed.isEmpty()) return ParsedSub(freqHz, emptyList(), false)
        return ParsedSub(
            frequencyHz = freqHz,
            pulsesUs = signed.map { abs(it) },
            startNegative = signed.first() < 0,
        )
    }
}
