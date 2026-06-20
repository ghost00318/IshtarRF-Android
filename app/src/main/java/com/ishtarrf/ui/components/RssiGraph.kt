package com.ishtarrf.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A rolling RSSI line chart. RSSI is roughly [-120, 0] dBm; values are clamped
 * to that window and drawn left→right oldest→newest.
 */
@Composable
fun RssiGraph(
    values: List<Int>,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    minDbm: Int = -120,
    maxDbm: Int = 0,
) {
    val lineColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val w = size.width
        val h = size.height
        val range = (maxDbm - minDbm).toFloat().coerceAtLeast(1f)

        // horizontal grid lines at 25% steps
        for (i in 0..4) {
            val y = h * i / 4f
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        if (values.size < 2) return@Canvas

        val stepX = w / (values.size - 1).toFloat()
        val path = Path()
        values.forEachIndexed { i, raw ->
            val v = raw.coerceIn(minDbm, maxDbm)
            val x = i * stepX
            val y = h - ((v - minDbm) / range) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

        // newest point marker
        val last = values.last().coerceIn(minDbm, maxDbm)
        val ly = h - ((last - minDbm) / range) * h
        drawCircle(Color(0xFFFF9F3E), radius = 4f, center = Offset(w, ly))
    }
}
