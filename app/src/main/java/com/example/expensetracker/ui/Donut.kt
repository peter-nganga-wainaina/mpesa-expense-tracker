package com.example.expensetracker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One coloured share of a [Donut]. Shares are relative: they needn't add up to anything. */
data class DonutSlice(val share: Long, val color: Color)

/**
 * A ring split into [slices], clockwise from the top, with [center] drawn inside it.
 * Drawn by hand so its colours and proportions match the app's look exactly.
 */
@Composable
fun Donut(
    slices: List<DonutSlice>,
    trackColor: Color,
    description: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
    thickness: Dp = 22.dp,
    center: @Composable () -> Unit,
) {
    Box(modifier.size(diameter).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val strokePx = thickness.toPx()
            val inset = strokePx / 2
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(strokePx))

            val total = slices.sumOf { it.share }.toFloat()
            if (total <= 0f) return@Canvas
            // A small gap between slices keeps neighbouring colours distinct; skip it for a single slice.
            val gap = if (slices.size > 1) 2.5f else 0f
            var start = -90f
            slices.forEach { slice ->
                val sweep = slice.share / total * 360f
                if (sweep > gap) {
                    drawArc(
                        slice.color, start + gap / 2, sweep - gap, useCenter = false,
                        topLeft = topLeft, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Butt),
                    )
                }
                start += sweep
            }
        }
        center()
    }
}
