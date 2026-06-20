package com.ishtarrf.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ishtarrf.domain.AppTheme
import com.ishtarrf.ui.MainViewModel
import com.ishtarrf.ui.UiState
import com.ishtarrf.ui.components.LogList
import com.ishtarrf.ui.components.NumberField
import com.ishtarrf.ui.components.SectionCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var newFav by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(title = "Theme", icon = Icons.Filled.Palette) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTheme.entries.forEach { theme ->
                    FilterChip(
                        selected = state.theme == theme,
                        onClick = { vm.setTheme(theme) },
                        label = { Text(theme.label) },
                    )
                }
            }
        }

        SectionCard(title = "Favorite frequencies", icon = Icons.Filled.Star) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.favorites.forEach { f ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeFavorite(f) },
                        label = { Text("%.2f".format(f)) },
                        trailingIcon = { Icon(Icons.Filled.Close, "Remove", Modifier.height(16.dp)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    label = "Add frequency",
                    value = newFav,
                    onValueChange = { newFav = it },
                    suffix = "MHz",
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = {
                    newFav.toDoubleOrNull()?.let { vm.addFavorite(it) }
                    newFav = ""
                }) {
                    Icon(Icons.Filled.Add, null, Modifier.padding(end = 4.dp)); Text("Add")
                }
            }
        }

        SectionCard(
            title = "Event Log",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { shareText(context, vm.exportLog()) }) { Text("Share") }
                    TextButton(onClick = vm::clearLog) { Text("Clear") }
                }
            },
        ) {
            LogList(entries = state.log, modifier = Modifier.fillMaxWidth().height(260.dp))
        }

        SectionCard(title = "About") {
            Text("IshtarRF Mobile · v0.1.0", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Sub-GHz RX/TX over ESP32 + CC1101, connected by USB-OTG cable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "You are responsible for complying with local RF regulations — use only " +
                    "permitted frequencies, power levels, and protocols.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Licensed under AGPL-3.0-only. © 2025 Cyber Ducky.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun shareText(context: android.content.Context, text: String) {
    if (text.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share log"))
}
