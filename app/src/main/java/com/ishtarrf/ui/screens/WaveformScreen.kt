package com.ishtarrf.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ishtarrf.ui.MainViewModel
import com.ishtarrf.ui.UiState
import com.ishtarrf.ui.components.SaveSignalDialog
import com.ishtarrf.ui.components.SectionCard
import com.ishtarrf.ui.components.WaveformView

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaveformScreen(state: UiState, vm: MainViewModel) {
    val signal = state.currentSignal
    var zoom by remember { mutableFloatStateOf(1f) }
    var showSave by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Waveform", icon = Icons.AutoMirrored.Filled.ShowChart) {
            WaveformView(
                pulsesUs = signal?.pulsesUs ?: emptyList(),
                startNegative = state.startNegative,
                zoom = zoom,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zoom", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 0.25f..6f,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text("${"%.1f".format(zoom)}×", style = MaterialTheme.typography.labelMedium)
            }
        }

        SectionCard(title = "Details") {
            if (signal == null) {
                Text(
                    "Capture a RAW signal (Control → Start RX) or load one from the Library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${signal.pulsesUs.size} pulses") })
                    AssistChip(onClick = {}, label = { Text("%.3f MHz".format(signal.freqMhz)) })
                    AssistChip(onClick = {}, label = { Text("${signal.totalDurationUs / 1000} ms") })
                    signal.rssiDbm?.let { AssistChip(onClick = {}, label = { Text("$it dBm") }) }
                    AssistChip(onClick = {}, label = { Text(signal.modulation.label) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.startNegative,
                        onCheckedChange = vm::setStartNegative,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Start with LOW (−)")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showSave = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Save, null, Modifier.padding(end = 6.dp))
                        Text("Save as .sub")
                    }
                    OutlinedButton(
                        onClick = vm::clearSignal,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.padding(end = 6.dp))
                        Text("Clear")
                    }
                }
            }
        }
    }

    if (showSave && signal != null) {
        SaveSignalDialog(
            folders = state.folders,
            defaultFolder = state.selectedFolder,
            onDismiss = { showSave = false },
            onSave = { name, folder ->
                vm.saveCurrentAsSub(name, folder)
                showSave = false
            },
        )
    }
}
