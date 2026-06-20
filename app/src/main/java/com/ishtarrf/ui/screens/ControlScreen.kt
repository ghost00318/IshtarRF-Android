package com.ishtarrf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ishtarrf.domain.Modulation
import com.ishtarrf.domain.RadioConfig
import com.ishtarrf.domain.RxMode
import com.ishtarrf.ui.MainViewModel
import com.ishtarrf.ui.UiState
import com.ishtarrf.ui.components.LabeledDropdown
import com.ishtarrf.ui.components.LogList
import com.ishtarrf.ui.components.NumberField
import com.ishtarrf.ui.components.SectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlScreen(state: UiState, vm: MainViewModel) {
    val connected = state.connection.isConnected

    // Local editable copies, re-seeded whenever the VM config changes externally.
    var freqText by remember(state.config.freqMhz) { mutableStateOf("%.3f".format(state.config.freqMhz)) }
    var modulation by remember(state.config.modulation) { mutableStateOf(state.config.modulation) }
    var brText by remember(state.config.bitrateKbps) { mutableStateOf("%.2f".format(state.config.bitrateKbps)) }
    var devText by remember(state.config.deviationKhz) { mutableStateOf("%.1f".format(state.config.deviationKhz)) }
    var txpText by remember(state.config.txPowerDbm) { mutableStateOf(state.config.txPowerDbm.toString()) }

    var repeat by remember { mutableStateOf("2") }
    var gap by remember { mutableStateOf("20") }
    var invert by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------------- Radio config ----------------
        SectionCard(title = "Radio Config", icon = Icons.Filled.Tune) {
            NumberField(
                label = "Frequency",
                value = freqText,
                onValueChange = { freqText = it },
                suffix = "MHz",
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.favorites.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.favorites.forEach { f ->
                        val label = "%.2f".format(f)
                        FilterChip(
                            selected = freqText.toDoubleOrNull()?.let { kotlin.math.abs(it - f) < 0.001 } == true,
                            onClick = { freqText = "%.3f".format(f) },
                            label = { Text(label) },
                        )
                    }
                }
            }
            LabeledDropdown(
                label = "Modulation",
                options = Modulation.entries,
                selected = modulation,
                optionLabel = { it.label },
                onSelect = { modulation = it },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Bitrate", brText, { brText = it }, Modifier.weight(1f), "kbps")
                NumberField("Deviation", devText, { devText = it }, Modifier.weight(1f), "kHz")
            }
            NumberField("TX Power", txpText, { txpText = it }, Modifier.fillMaxWidth(), "dBm", decimal = false)
            Button(
                onClick = {
                    vm.applyConfig(
                        RadioConfig(
                            freqMhz = freqText.toDoubleOrNull() ?: state.config.freqMhz,
                            modulation = modulation,
                            bitrateKbps = brText.toDoubleOrNull() ?: state.config.bitrateKbps,
                            deviationKhz = devText.toDoubleOrNull() ?: state.config.deviationKhz,
                            txPowerDbm = txpText.toIntOrNull() ?: state.config.txPowerDbm,
                        ),
                    )
                },
                enabled = connected,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Apply Config") }
        }

        // ---------------- RX / RSSI ----------------
        SectionCard(title = "Receive", icon = Icons.Filled.NetworkCheck) {
            LabeledDropdown(
                label = "RX Mode",
                options = RxMode.entries,
                selected = state.rxMode,
                optionLabel = { it.label },
                onSelect = vm::setRxMode,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = vm::startRx,
                    enabled = connected,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, Modifier.padding(end = 4.dp)); Text("Start RX")
                }
                OutlinedButton(
                    onClick = vm::stopRx,
                    enabled = connected,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Stop, null, Modifier.padding(end = 4.dp)); Text("Stop RX")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = vm::requestRssi, enabled = connected) { Text("Get RSSI") }
                OutlinedButton(onClick = vm::recover, enabled = connected) {
                    Icon(Icons.Filled.Autorenew, null, Modifier.padding(end = 4.dp)); Text("Recover")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    state.lastRssi?.let { "$it dBm" } ?: "— dBm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // ---------------- Transmit ----------------
        SectionCard(title = "Transmit", icon = Icons.AutoMirrored.Filled.Send) {
            OutlinedTextField(
                value = state.txHexText,
                onValueChange = vm::setTxHex,
                label = { Text("TX Bytes (HEX)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = vm::txHex, enabled = connected, modifier = Modifier.fillMaxWidth()) {
                Text("Send Bytes")
            }

            OutlinedTextField(
                value = state.txPulsesText,
                onValueChange = vm::setTxPulses,
                label = { Text("RAW pulses (µs, comma-separated)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Repeat", repeat, { repeat = it }, Modifier.weight(1f), decimal = false)
                NumberField("Gap", gap, { gap = it }, Modifier.weight(1f), "ms", decimal = false)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = invert, onCheckedChange = { invert = it })
                Spacer(Modifier.width(8.dp))
                Text("Invert (start HIGH)")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.txRaw(repeat.toIntOrNull() ?: 1, gap.toIntOrNull() ?: 0, invert) },
                    enabled = connected,
                    modifier = Modifier.weight(1f),
                ) { Text("TX RAW") }
                OutlinedButton(
                    onClick = { vm.replayCurrent(repeat.toIntOrNull() ?: 1, gap.toIntOrNull() ?: 0, invert) },
                    enabled = connected && state.currentSignal != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Replay, null, Modifier.padding(end = 4.dp)); Text("Replay")
                }
                OutlinedButton(
                    onClick = vm::clearSignal,
                    enabled = state.currentSignal != null || state.txPulsesText.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Clear captured signal")
                }
            }
        }

        // ---------------- Log preview ----------------
        SectionCard(
            title = "Event Log",
            trailing = {
                TextButton(onClick = vm::clearLog) {
                    Icon(Icons.Filled.Clear, null, Modifier.padding(end = 4.dp)); Text("Clear")
                }
            },
        ) {
            LogList(
                entries = state.log,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
        }
    }
}
