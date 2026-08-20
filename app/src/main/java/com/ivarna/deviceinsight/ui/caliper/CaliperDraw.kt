package com.ivarna.deviceinsight.ui.caliper

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** §4.5 — hatch is a first-class redundancy channel (CVD-safe, prints beautifully). */
fun DrawScope.hatch(
    rect: Rect,
    pattern: HatchPattern,
    color: Color,
    strokeWidth: Dp = 1.dp,
    period: Dp = 4.dp
) {
    val sw = strokeWidth.toPx(); val p = period.toPx()
    when (pattern) {
        HatchPattern.NONE -> {}
        HatchPattern.SOLID ->
            drawRect(color, topLeft = rect.topLeft, size = rect.size)
        HatchPattern.VERTICAL -> {
            var x = rect.left + p / 2
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.top), Offset(x, rect.bottom), sw)
                x += p
            }
        }
        HatchPattern.HORIZONTAL -> {
            var y = rect.top + p / 2
            while (y < rect.bottom) {
                drawLine(color, Offset(rect.left, y), Offset(rect.right, y), sw)
                y += p
            }
        }
        HatchPattern.DIAGONAL -> clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            var x = rect.left - rect.height
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.bottom), Offset(x + rect.height, rect.top), sw)
                x += p
            }
        }
        HatchPattern.CROSS -> clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            var x = rect.left - rect.height
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.bottom), Offset(x + rect.height, rect.top), sw)
                drawLine(color, Offset(x, rect.top), Offset(x + rect.height, rect.bottom), sw)
                x += p
            }
        }
        HatchPattern.DOTS -> {
            var y = rect.top + p / 2
            while (y < rect.bottom) {
                var x = rect.left + p / 2
                while (x < rect.right) {
                    drawCircle(color, radius = sw * 0.8f, center = Offset(x, y))
                    x += p
                }
                y += p
            }
        }
    }
}

/** Dashed border — used for DISABLED HardKeys, locked root panels, perforations. */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    dash: Dp = 4.dp,
    gap: Dp = 4.dp
): Modifier = drawBehind {
    val w = width.toPx(); val half = w / 2
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
    val W = size.width; val H = size.height
    drawLine(color, Offset(half, half), Offset(W - half, half), w, pathEffect = effect)
    drawLine(color, Offset(W - half, half), Offset(W - half, H - half), w, pathEffect = effect)
    drawLine(color, Offset(W - half, H - half), Offset(half, H - half), w, pathEffect = effect)
    drawLine(color, Offset(half, H - half), Offset(half, half), w, pathEffect = effect)
}

/** §4.4 — graph-paper background: 24dp minor @3%, 120dp major @5%. */
@androidx.compose.runtime.Composable
fun Modifier.caliperGrid(showMajor: Boolean = true): Modifier {
    val c = Caliper.colors
    return this.drawBehind {
        val minor = 24.dp.toPx(); val major = 120.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(c.gridMinor, Offset(x, 0f), Offset(x, size.height), 1f); x += minor
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(c.gridMinor, Offset(0f, y), Offset(size.width, y), 1f); y += minor
        }
        if (showMajor) {
            x = 0f
            while (x <= size.width) { drawLine(c.gridMajor, Offset(x, 0f), Offset(x, size.height), 1f); x += major }
            y = 0f
            while (y <= size.height) { drawLine(c.gridMajor, Offset(0f, y), Offset(size.width, y), 1f); y += major }
        }
    }
}

/** Leader-note box used inside ScopeTrace. */
fun DrawScope.noteBox(
    topLeft: Offset, size: Size, fill: Color, border: Color
) {
    drawRect(fill, topLeft = topLeft, size = size)
    drawRect(border, topLeft = topLeft, size = size, style = Stroke(1f))
}