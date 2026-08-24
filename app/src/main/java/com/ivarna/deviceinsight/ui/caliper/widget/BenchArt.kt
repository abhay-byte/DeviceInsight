package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ivarna.deviceinsight.ui.caliper.HatchPattern

private fun sp(context: Context, v: Float): Float = v * context.resources.displayMetrics.scaledDensity
private fun dp(context: Context, v: Float): Float = v * context.resources.displayMetrics.density

private fun colorInt(c: Color): Int = c.toArgb()

fun renderSync(key: String, wPx: Int, hPx: Int, block: (Canvas) -> Unit): Bitmap {
    BenchFrames.get(key)?.let { return it }
    val bmp = Bitmap.createBitmap(wPx.coerceAtLeast(1), hPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    block(canvas)
    BenchFrames.put(key, bmp)
    return bmp
}

// Hatch on Android Canvas — pixel parity with CaliperDraw.hatch
fun Canvas.hatch(
    rect: RectF,
    pattern: HatchPattern,
    color: Int,
    strokePx: Float = 1f,
    periodPx: Float = 4f
) {
    when (pattern) {
        HatchPattern.NONE -> {}
        HatchPattern.SOLID -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.FILL }
            drawRect(rect, p)
        }
        HatchPattern.VERTICAL -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = strokePx }
            var x = rect.left + periodPx / 2
            while (x < rect.right) {
                drawLine(x, rect.top, x, rect.bottom, p)
                x += periodPx
            }
        }
        HatchPattern.HORIZONTAL -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = strokePx }
            var y = rect.top + periodPx / 2
            while (y < rect.bottom) {
                drawLine(rect.left, y, rect.right, y, p)
                y += periodPx
            }
        }
        HatchPattern.DIAGONAL -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = strokePx }
            save()
            clipRect(rect)
            var x = rect.left - rect.height()
            while (x < rect.right) {
                drawLine(x, rect.bottom, x + rect.height(), rect.top, p)
                x += periodPx
            }
            restore()
        }
        HatchPattern.CROSS -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = strokePx }
            save()
            clipRect(rect)
            var x = rect.left - rect.height()
            while (x < rect.right) {
                drawLine(x, rect.bottom, x + rect.height(), rect.top, p)
                drawLine(x, rect.top, x + rect.height(), rect.bottom, p)
                x += periodPx
            }
            restore()
        }
        HatchPattern.DOTS -> {
            val p = Paint().apply { this.color = color; style = Paint.Style.FILL }
            var y = rect.top + periodPx / 2
            while (y < rect.bottom) {
                var x = rect.left + periodPx / 2
                while (x < rect.right) {
                    drawCircle(x, y, strokePx * 0.8f, p)
                    x += periodPx
                }
                y += periodPx
            }
        }
    }
}

fun Canvas.hatchBar(
    context: Context,
    pal: WidgetPalette,
    w: Float,
    h: Float,
    segs: List<MemSeg>,
    colors: List<Color>
) {
    val periodPx = sp(context, 4f)
    var x = 0f
    segs.forEachIndexed { i, seg ->
        val segW = w * seg.fraction.coerceIn(0f, 1f)
        if (segW <= 0f) return@forEachIndexed
        val rect = RectF(x, 0f, (x + segW).coerceAtMost(w), h)
        val col = colors.getOrElse(i) { pal.ink }
        hatch(rect, seg.pattern, col.toArgb(), strokePx = dp(context, 1f), periodPx = periodPx)
        x += segW
    }
    // hairline border
    val border = Paint().apply { color = pal.hairline.toArgb(); style = Paint.Style.STROKE; strokeWidth = dp(context, 1f) }
    drawRect(RectF(0f, 0f, w, h), border)
}

fun Canvas.spark(
    values: List<Float>,
    pal: WidgetPalette,
    channelColor: Color,
    w: Float,
    h: Float,
    density: Float,
    strokePx: Float = 2f * density
) {
    if (values.size < 2) {
        // NO SIGNAL flat line
        val midY = h / 2
        val p = Paint().apply { color = pal.ink40.toArgb(); style = Paint.Style.STROKE; strokeWidth = strokePx; strokeCap = Paint.Cap.SQUARE }
        drawLine(0f, midY, w, midY, p)
        return
    }
    val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
    val minV = (values.minOrNull() ?: 0f)
    val range = (maxV - minV).coerceAtLeast(0.0001f)
    val path = Path()
    values.forEachIndexed { i, v ->
        val x = i * w / (values.size - 1)
        val norm = ((v - minV) / range).coerceIn(0f, 1f)
        val y = h * (1f - norm)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val paint = Paint().apply { color = channelColor.toArgb(); style = Paint.Style.STROKE; strokeWidth = strokePx; isAntiAlias = true; strokeCap = Paint.Cap.SQUARE; strokeJoin = Paint.Join.MITER }
    drawPath(path, paint)
    // square pen head
    val lastV = values.last()
    val lastNorm = ((lastV - minV) / range).coerceIn(0f, 1f)
    val lastY = h * (1f - lastNorm)
    val headPaint = Paint().apply { color = channelColor.toArgb(); style = Paint.Style.FILL }
    val hs = strokePx * 0.9f
    drawRect(RectF(w - hs, lastY - hs / 2, w, lastY + hs / 2), headPaint)
}

fun Canvas.scope(
    context: Context,
    values: List<Float>,
    pal: WidgetPalette,
    channelColor: Color,
    w: Float,
    h: Float,
    density: Float,
    sweep: Float = 1f,
    showYLabels: Boolean = false,
    showAllYLabels: Boolean = false
) {
    // grid 24dp minor, 120dp major
    val minor = 24f * density
    val major = 120f * density
    val gridMinorPaint = Paint().apply { color = pal.gridMinor.toArgb(); strokeWidth = 1f }
    val gridMajorPaint = Paint().apply { color = pal.gridMajor.toArgb(); strokeWidth = 1f }
    var x = 0f
    while (x <= w) { drawLine(x, 0f, x, h, gridMinorPaint); x += minor }
    var y = 0f
    while (y <= h) { drawLine(0f, y, w, y, gridMinorPaint); y += minor }
    x = 0f
    while (x <= w) { drawLine(x, 0f, x, h, gridMajorPaint); x += major }
    y = 0f
    while (y <= h) { drawLine(0f, y, w, y, gridMajorPaint); y += major }

    // W2: ascii y-axis labels, 10sp ink40 on the right edge (T2–T3: 0/50/100 · T4+: all five)
    if (showYLabels) {
        val lp = Paint().apply { color = pal.ink40.toArgb(); textSize = sp(context, 10f); isAntiAlias = true; textAlign = Paint.Align.RIGHT }
        val labels = if (showAllYLabels) listOf(100, 75, 50, 25, 0) else listOf(100, 50, 0)
        val fm = lp.fontMetrics
        val textH = fm.descent - fm.ascent
        labels.forEach { v ->
            val cy = h * (1f - v / 100f)
            // clamp BASELINE so ascender/descender stay inside the bitmap (top "100" was clipped)
            val ty = (cy - textH / 2f - fm.ascent).coerceIn(-fm.ascent, (h - fm.descent).coerceAtLeast(-fm.ascent))
            drawText("$v", w - 4f * density, ty, lp)
        }
    }

    if (values.size < 2) {
        val midY = h / 2
        val p = Paint().apply { color = pal.ink40.toArgb(); style = Paint.Style.STROKE; strokeWidth = 1f * density }
        drawLine(0f, midY, w, midY, p)
        val tp = Paint().apply { color = pal.ink60.toArgb(); textSize = 10f * density; isAntiAlias = true }
        // panel knockout so the label never collides with the grid/flat line
        val label = "NO SIGNAL"
        val fm2 = tp.fontMetrics
        val textH2 = fm2.descent - fm2.ascent
        val tw = tp.measureText(label)
        val tx = (w - tw) / 2f
        val tyy = midY - textH2 / 2f - fm2.ascent
        val pad = 5f * density
        drawRect(RectF(tx - pad, midY - textH2 / 2f - 2f * density, tx + tw + pad, midY + textH2 / 2f + 2f * density),
            Paint().apply { color = pal.panel.toArgb() })
        drawText(label, tx, tyy, tp)
        return
    }

    val maxV = 100f
    val path = Path()
    values.forEachIndexed { i, v ->
        val px = i * w / (values.size - 1)
        val py = h * (1f - (v / maxV).coerceIn(0f, 1f))
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    val trimmed = if (sweep < 1f) {
        val pm = PathMeasure(path, false)
        val dst = Path()
        pm.getSegment(0f, pm.length * sweep.coerceIn(0f, 1f), dst, true)
        dst
    } else path

    val paint = Paint().apply { color = channelColor.toArgb(); style = Paint.Style.STROKE; strokeWidth = 2f * density; isAntiAlias = true; strokeCap = Paint.Cap.SQUARE }
    drawPath(trimmed, paint)
    // 3dp square pen head
    val lastV = values.last()
    val lastY = h * (1f - (lastV / maxV).coerceIn(0f, 1f))
    val head = Paint().apply { color = channelColor.toArgb(); style = Paint.Style.FILL }
    val hs = 3f * density
    drawRect(RectF(w - hs, lastY - hs / 2, w, lastY + hs / 2), head)
}

fun Canvas.fuelGauge(
    context: Context,
    pct: Float,
    pal: WidgetPalette,
    w: Float,
    h: Float,
    density: Float,
    charging: Boolean
) {
    val trackH = 12f * density
    val y = (h - trackH) / 2
    // track
    val trackPaint = Paint().apply { color = pal.hairline.toArgb(); style = Paint.Style.FILL }
    drawRect(RectF(0f, y, w, y + trackH), trackPaint)
    // fill
    val isCritical = pct < 0.2f
    val fillColor = if (isCritical) pal.fault else pal.ch06.let { pal.ch04 }
    val fillW = w * pct.coerceIn(0f, 1f)
    val fillPaint = Paint().apply { color = fillColor.toArgb(); style = Paint.Style.FILL }
    drawRect(RectF(0f, y, fillW, y + trackH), fillPaint)
    // ticks every 5%
    val tickPaint = Paint().apply { color = pal.ink40.toArgb(); strokeWidth = 1f }
    for (i in 0..20) {
        val tx = i * w / 20f
        drawLine(tx, y, tx, y + trackH, tickPaint)
    }
    // square knob
    val knobSize = 10f * density
    val knobX = (fillW - knobSize / 2).coerceIn(0f, w - knobSize)
    val knobPaint = Paint().apply {
        color = if (charging) pal.accent.toArgb() else fillColor.toArgb()
        style = Paint.Style.FILL
        alpha = if (charging) 220 else 255
    }
    drawRect(RectF(knobX, y - 2f * density, knobX + knobSize, y + trackH + 2f * density), knobPaint)
}

fun Canvas.wattTrace(
    values: List<Float>,
    pal: WidgetPalette,
    w: Float,
    h: Float,
    density: Float
) {
    if (values.size < 2) {
        val midY = h / 2
        val p = Paint().apply { color = pal.hairline.toArgb(); strokeWidth = 1f }
        drawLine(0f, midY, w, midY, p)
        return
    }
    val maxAbs = values.maxOfOrNull { kotlin.math.abs(it) }?.coerceAtLeast(1f) ?: 1f
    val midY = h / 2
    // zero hairline
    val zeroPaint = Paint().apply { color = pal.hairline.toArgb(); strokeWidth = 1f }
    drawLine(0f, midY, w, midY, zeroPaint)

    val path = Path()
    val fillPath = Path()
    values.forEachIndexed { i, v ->
        val x = i * w / (values.size - 1)
        val y = midY - (v / maxAbs) * (h / 2 * 0.85f)
        if (i == 0) { path.moveTo(x, y); fillPath.moveTo(x, midY); fillPath.lineTo(x, y) }
        else { path.lineTo(x, y); fillPath.lineTo(x, y) }
    }
    fillPath.lineTo(w, midY)
    fillPath.close()
    val fillPaint = Paint().apply { color = pal.ch04.toArgb(); style = Paint.Style.FILL; alpha = 40; isAntiAlias = true }
    drawPath(fillPath, fillPaint)
    val strokePaint = Paint().apply { color = pal.ch04.toArgb(); style = Paint.Style.STROKE; strokeWidth = 2f * density; isAntiAlias = true; strokeCap = Paint.Cap.SQUARE }
    drawPath(path, strokePaint)
}

fun Canvas.coreRailRows(
    context: Context,
    pal: WidgetPalette,
    w: Float,
    cores: List<com.ivarna.deviceinsight.ui.caliper.components.CoreReading>,
    rowHpx: Float
) {
    val periodPx = sp(context, 4f)
    cores.forEachIndexed { i, core ->
        val y = i * rowHpx
        if (y + rowHpx > 0) {
            val fraction = (core.load / 100f).coerceIn(0f, 1f)
            val barW = w * 0.6f
            val barX = 32f * context.resources.displayMetrics.density
            val barY = y + rowHpx * 0.25f
            val barH = rowHpx * 0.5f
            // track
            val trackPaint = Paint().apply { color = pal.hairline.toArgb(); style = Paint.Style.FILL }
            drawRect(RectF(barX, barY, barX + barW, barY + barH), trackPaint)
            val fillRect = RectF(barX, barY, barX + barW * fraction, barY + barH)
            hatch(fillRect, HatchPattern.SOLID, pal.ch01.toArgb(), strokePx = 1f, periodPx = periodPx)
        }
    }
}

fun Canvas.thermalRamp(
    pal: WidgetPalette,
    w: Float,
    h: Float,
    tempC: Float
) {
    val segs = 12
    val segW = w / segs
    val amber = Color(0xFFF0A419)
    val vermilion = Color(0xFFE5482B)
    val fault = pal.fault
    for (i in 0 until segs) {
        val t = i / (segs - 1).toFloat()
        val col = when {
            t < 0.5f -> androidx.compose.ui.graphics.lerp(amber, vermilion, t * 2)
            else -> androidx.compose.ui.graphics.lerp(vermilion, fault, (t - 0.5f) * 2)
        }
        val rect = RectF(i * segW, 0f, (i + 1) * segW, h)
        val p = Paint().apply { color = col.toArgb(); style = Paint.Style.FILL }
        drawRect(rect, p)
    }
    // indicator
    val norm = ((tempC - 20f) / 80f).coerceIn(0f, 1f)
    val ix = norm * w
    val indPaint = Paint().apply { color = pal.ink.toArgb(); style = Paint.Style.FILL }
    drawRect(RectF(ix - 2f, 0f, ix + 2f, h), indPaint)
}

fun Canvas.lockedField(
    context: Context,
    pal: WidgetPalette,
    w: Float,
    h: Float
) {
    val rect = RectF(0f, 0f, w, h)
    val periodPx = sp(context, 4f)
    hatch(rect, HatchPattern.DOTS, Color(pal.ink40.toArgb()).copy(alpha = 0.4f).toArgb(), strokePx = 1f, periodPx = periodPx)
    val tp = Paint().apply { color = pal.ink60.toArgb(); textSize = 10f * context.resources.displayMetrics.density; isAntiAlias = true }
    drawText("LOCKED", w / 2 - 20f * context.resources.displayMetrics.density, h / 2, tp)
}

fun Canvas.calibrating(
    context: Context,
    pal: WidgetPalette,
    w: Float,
    h: Float,
    density: Float,
    sweep: Float = 0.7f
) {
    val midY = h / 2
    val amp = h * 0.35f
    val path = Path()
    val steps = 100
    for (i in 0..steps) {
        val x = i * w / steps
        val y = midY + kotlin.math.sin(i * 2 * Math.PI / 20).toFloat() * amp
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    val trimmed = if (sweep < 1f) {
        val pm = PathMeasure(path, false)
        val dst = Path()
        pm.getSegment(0f, pm.length * sweep.coerceIn(0f, 1f), dst, true)
        dst
    } else path
    val paint = Paint().apply { color = pal.ink40.toArgb(); style = Paint.Style.STROKE; strokeWidth = 2f * density; isAntiAlias = true; strokeCap = Paint.Cap.SQUARE }
    drawPath(trimmed, paint)
}
