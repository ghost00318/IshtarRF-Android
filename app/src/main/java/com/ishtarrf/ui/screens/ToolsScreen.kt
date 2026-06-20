package com.ishtarrf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ishtarrf.domain.ScanPoint
import com.ishtarrf.ui.MainViewModel
import com.ishtarrf.ui.UiState
import com.ishtarrf.ui.components.NumberField
import com.ishtarrf.ui.components.RssiGraph
import com.ishtarrf.ui.components.SectionCard

@Composable
fun ToolsScreen(state: UiState, vm: MainViewModel) {
    val connected = state.connection.isConnected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ReplayCard(state, vm, connected)
        RssiMonitorCard(state, vm, connected)
        ScannerCard(state, vm, connected)
        BruteForceCard(state, vm, connected)
    }
}

@Composable
private fun ReplayCard(state: UiState, vm: MainViewModel, connected: Boolean) {
    var repeat by remember { mutableStateOf("3") }
    var gap by remember { mutableStateOf("20") }
    var invert by remember { mutableStateOf(false) }

    SectionCard(title = "Replay", icon = Icons.Filled.Replay) {
        val sig = state.currentSignal
        Text(
            if (sig != null) "Current: ${sig.pulsesUs.size} pulses · %.3f MHz".format(sig.freqMhz)
            else "No signal loaded — capture or load one first.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Repeat", repeat, { repeat = it }, Modifier.weight(1f), decimal = false)
            NumberField("Gap", gap, { gap = it }, Modifier.weight(1f), "ms", decimal = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = invert, onCheckedChange = { invert = it })
            Spacer(Modifier.width(8.dp))
            Text("Invert")
        }
        Button(
            onClick = { vm.replayCurrent(repeat.toIntOrNull() ?: 1, gap.toIntOrNull() ?: 0, invert) },
            enabled = connected && state.currentSignal != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Replay signal") }
    }
}

@Composable
private fun RssiMonitorCard(state: UiState, vm: MainViewModel, connected: Boolean) {
    SectionCard(
        title = "Live RSSI",
        icon = Icons.Filled.Sensors,
        trailing = {
            Text(
                state.lastRssi?.let { "$it dBm" } ?: "— dBm",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
            )
        },
    ) {
        RssiGraph(values = state.rssiHistory)
        Button(
            onClick = vm::toggleRssiMonitor,
            enabled = connected,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.rssiMonitorOn) "Stop monitor" else "Start monitor") }
    }
}

@Composable
private fun ScannerCard(state: UiState, vm: MainViewModel, connected: Boolean) {
    var start by remember { mutableStateOf("433.00") }
    var end by remember { mutableStateOf("435.00") }
    var step by remember { mutableStateOf("0.10") }

    SectionCard(title = "Frequency Scanner", icon = Icons.Filled.GraphicEq) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Start", start, { start = it }, Modifier.weight(1f), "MHz")
            NumberField("End", end, { end = it }, Modifier.weight(1f), "MHz")
            NumberField("Step", step, { step = it }, Modifier.weight(1f), "MHz")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    vm.startScan(
                        start.toDoubleOrNull() ?: 433.0,
                        end.toDoubleOrNull() ?: 435.0,
                        step.toDoubleOrNull() ?: 0.1,
                    )
                },
                enabled = connected && !state.toolBusy,
                modifier = Modifier.weight(1f),
            ) { Text("Scan") }
            OutlinedButton(
                onClick = vm::stopScan,
                enabled = state.toolBusy,
                modifier = Modifier.weight(1f),
            ) { Text("Stop") }
        }
        if (state.toolStatus.isNotEmpty()) {
            Text(state.toolStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.scanResults.takeLast(40).forEach { ScanBar(it) }
    }
}

@Composable
private fun ScanBar(point: ScanPoint, minDbm: Int = -120, maxDbm: Int = 0) {
    val fraction = ((point.rssiDbm - minDbm).toFloat() / (maxDbm - minDbm)).coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "%.2f".format(point.freqMhz),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(5.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(5.dp)),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${point.rssiDbm}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp))
    }
}

@Composable
private fun BruteForceCard(state: UiState, vm: MainViewModel, connected: Boolean) {
    var bits by remember { mutableStateOf("12") }
    var shortUs by remember { mutableStateOf("350") }
    var longUs by remember { mutableStateOf("1050") }
    var count by remember { mutableStateOf("256") }
    var repeat by remember { mutableStateOf("3") }
    var gap by remember { mutableStateOf("15") }

    SectionCard(title = "Brute-force (OOK)", icon = Icons.Filled.Bolt) {
        Text(
            "Transmits sequential PWM codes. Only use on devices you own and on " +
                "frequencies you are licensed to operate.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Bits", bits, { bits = it }, Modifier.weight(1f), decimal = false)
            NumberField("Codes", count, { count = it }, Modifier.weight(1f), decimal = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Short", shortUs, { shortUs = it }, Modifier.weight(1f), "µs", decimal = false)
            NumberField("Long", longUs, { longUs = it }, Modifier.weight(1f), "µs", decimal = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Repeat", repeat, { repeat = it }, Modifier.weight(1f), decimal = false)
            NumberField("Gap", gap, { gap = it }, Modifier.weight(1f), "ms", decimal = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    vm.startBruteForce(
                        bits = bits.toIntOrNull() ?: 12,
                        shortUs = shortUs.toIntOrNull() ?: 350,
                        longUs = longUs.toIntOrNull() ?: 1050,
                        count = count.toIntOrNull() ?: 256,
                        repeatEach = repeat.toIntOrNull() ?: 3,
                        gapMs = gap.toIntOrNull() ?: 15,
                    )
                },
                enabled = connected && !state.toolBusy,
                modifier = Modifier.weight(1f),
            ) { Text("Start") }
            OutlinedButton(
                onClick = vm::stopBruteForce,
                enabled = state.toolBusy,
                modifier = Modifier.weight(1f),
            ) { Text("Stop") }
        }
        if (state.toolStatus.isNotEmpty()) {
            Text(state.toolStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
