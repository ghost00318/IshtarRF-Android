package com.ishtarrf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a RAW pulse train as an OOK square wave. Pulses alternate ON/OFF by
 * index; [startNegative] flips which level index 0 represents.
 *
 * The drawing surface is wider than the viewport (so it scrolls horizontally)
 * and its width scales with [zoom].
 */
@Composable
fun WaveformView(
    pulsesUs: List<Int>,
    startNegative: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    zoom: Float = 1f,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val scroll = rememberScrollState()
    val total = pulsesUs.sumOf { it.toLong() }.coerceAtLeast(1L)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .horizontalScroll(scroll),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (pulsesUs.isEmpty()) {
            Text(
                "No signal captured",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Box
        }

        // ~0.06 px per microsecond at zoom 1, clamped to a sane canvas width.
        val pxPerUs = 0.06f * zoom
        val canvasWidth = (total * pxPerUs).toFloat().coerceIn(300f, 60_000f)

        Canvas(
            modifier = Modifier
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { canvasWidth.toDp() })
                .height(height),
        ) {
            val w = size.width
            val h = size.height
            val high = h * 0.22f
            val low = h * 0.78f
            val scaleX = w / total.toFloat()

            // baseline grid
            drawLine(gridColor, Offset(0f, high), Offset(w, high), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, low), Offset(w, low), strokeWidth = 1f)

            var x = 0f
            // index 0 is HIGH unless the signal starts negative
            var levelHigh = !startNegative
            var prevY = if (levelHigh) high else low

            pulsesUs.forEach { dur ->
                val segW = dur * scaleX
                val y = if (levelHigh) high else low
                // vertical transition
                if (y != prevY) {
                    drawLine(lineColor, Offset(x, prevY), Offset(x, y), strokeWidth = 3f)
                }
                // horizontal run
                drawLine(lineColor, Offset(x, y), Offset(x + segW, y), strokeWidth = 3f)
                x += segW
                prevY = y
                levelHigh = !levelHigh
            }
        }
    }
}
