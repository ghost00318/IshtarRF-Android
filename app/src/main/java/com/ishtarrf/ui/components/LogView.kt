package com.ishtarrf.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ishtarrf.domain.LogEntry
import com.ishtarrf.domain.LogLevel
import com.ishtarrf.ui.theme.MonoStyle

@Composable
fun LogList(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier,
    newestFirst: Boolean = true,
) {
    if (entries.isEmpty()) {
        Box(modifier) {
            Text(
                "Log is empty",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
        return
    }
    val ordered = if (newestFirst) entries.asReversed() else entries
    LazyColumn(modifier) {
        items(ordered, key = { it.seq }) { entry ->
            Text(
                entry.message,
                style = MonoStyle,
                color = colorFor(entry.level),
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun colorFor(level: LogLevel): Color = when (level) {
    LogLevel.ERROR -> MaterialTheme.colorScheme.error
    LogLevel.OK -> MaterialTheme.colorScheme.secondary
    LogLevel.RX -> MaterialTheme.colorScheme.primary
    LogLevel.RSSI -> MaterialTheme.colorScheme.tertiary
    LogLevel.TX -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.DEVICE -> MaterialTheme.colorScheme.onSurfaceVariant
    LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
}
