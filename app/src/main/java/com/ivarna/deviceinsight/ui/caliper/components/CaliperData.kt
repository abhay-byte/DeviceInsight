package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────── peak-hold ───────────────────────────

/** Records the max, then decays to the current value after `decayAfter` ms. */
@Composable
fun rememberPeakHold(value: Float, decayAfter: Long = 2000): Float {
    var peak by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value >= peak) {
            peak = value
        } else {
            delay(decayAfter)
            val anim = Animatable(peak)
            anim.animateTo(value, tween(600, easing = CaliperMotion.Ease))
            peak = anim.value
        }
    }
    return peak
}

// ─────────────────────────── PanelCard ───────────────────────────

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    channel: Channel? = null,
    title: String? = null,
    status: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Caliper.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.panel)
            .border(1.dp, c.hairline)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick
                ) else Modifier
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (channel != null) ChannelTick(channel)
            if (title != null) {
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), style = Caliper.type.meta, color = c.ink60)
            }
            Spacer(Modifier.weight(1f))
            status?.invoke(this)
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Text("tap →", style = Caliper.type.meta, color = c.ink40)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(1.dp).background(c.hairline))  // asymmetric rule
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ─────────────────────────── ReadoutTile ───────────────────────────

@Composable
fun ReadoutTile(
    channel: Channel,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    subline: String? = null,
    spark: List<Float>? = null,
    barFraction: Float? = null,
    statusText: String? = null,
    onClick: () -> Unit = {}
) {
    val c = Caliper.colors
    PanelCard(
        modifier = modifier, channel = channel, title = channel.name, onClick = onClick,
        status = {
            if (statusText != null) Text(statusText, style = Caliper.type.meta, color = c.ink40)
            else LedDot()
        }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            OdometerText(value, style = Caliper.type.readoutL)
            if (unit != null) Text(
                " $unit", style = Caliper.type.dataS, color = c.ink60,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (subline != null) {
            Spacer(Modifier.height(4.dp))
            Text(subline, style = Caliper.type.dataS, color = c.ink60)
        }
        if (barFraction != null) {
            Spacer(Modifier.height(10.dp))
            PeakBar(barFraction, channel)
        }
        if (spark != null && spark.size > 1) {
            Spacer(Modifier.height(10.dp))
            Sparkline(spark, channel, Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

@Composable
private fun PeakBar(fraction: Float, channel: Channel) {
    val c = Caliper.colors
    val anim by animateFloatAsState(fraction, CaliperMotion.Needle, label = "bar")
    val peak = rememberPeakHold(fraction)
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val h = size.height
        drawLine(c.hairline, Offset(0f, h / 2), Offset(size.width, h / 2), 1.dp.toPx())
        drawRect(c.channel(channel), size = Size(size.width * anim, h))
        drawLine(c.ink, Offset(size.width * peak, 0f), Offset(size.width * peak, h), 2.dp.toPx())
    }
}

// ─────────────────────────── Sparkline ───────────────────────────

@Composable
fun Sparkline(
    values: List<Float>,
    channel: Channel? = null,
    modifier: Modifier = Modifier,
    color: Color? = null,
    stroke: androidx.compose.ui.unit.Dp = 2.dp
) {
    val traceColor = color ?: channel?.let { Caliper.colors.channel(it) } ?: Caliper.colors.ink
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val n = values.size
        val stepX = size.width / (n - 1)
        val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * (1f - (v / maxV).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, traceColor, style = Stroke(stroke.toPx(), cap = StrokeCap.Square))
        val lastY = size.height * (1f - (values.last() / maxV).coerceIn(0f, 1f))
        drawCircle(traceColor, radius = stroke.toPx() * 0.75f, center = Offset(size.width - stroke.toPx(), lastY))
    }
}

// ─────────────────────────── ScopeTrace (§5.5 — the chart) ───────────────────────────

@Composable
fun ScopeTrace(
    values: List<Float>,
    channel: Channel,
    modifier: Modifier = Modifier,
    windowLabel: String = "60 s",
    yMax: Float = 100f,
    yMin: Float = 0f,
    valueFormat: (Float) -> String = { Fmt.pct(it, 1) },
    yFormat: (Float) -> String = { Fmt.pct(it) },
    secondaryValues: List<Float>? = null,       // e.g. network ↑ as ink@40
    timeLabelFor: (Float) -> String = { "" },   // fraction 0..1 → label
    height: androidx.compose.ui.unit.Dp = 220.dp
) {
    val c = Caliper.colors
    val traceColor = c.channel(channel)
    val reduced = rememberReducedMotion()
    val tm = rememberTextMeasurer()
    val sweep = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (sweep.value < 1f) sweep.animateTo(1f, tween(CaliperMotion.tSweep, easing = CaliperMotion.Ease))
    }
    var crossX by remember { mutableStateOf<Float?>(null) }
    val stats = remember(values) {
        if (values.isEmpty()) Triple(0f, 0f, 0f)
        else Triple(values.min(), values.max(), values.average().toFloat())
    }
    val a11y = if (values.isEmpty()) "${channel.label}: no signal"
    else "${channel.label} over $windowLabel. Min ${valueFormat(stats.first)}, " +
         "max ${valueFormat(stats.second)}, average ${valueFormat(stats.third)}."

    val yStyle = TextStyle(fontFamily = PlexMonoFamily, fontSize = 10.sp, color = c.ink40, fontFeatureSettings = "tnum")

    Column(modifier.semantics { contentDescription = a11y }) {
        Canvas(
            Modifier.fillMaxWidth().height(height)
                .pointerInput(values) { detectTapGestures { crossX = it.x } }
                .pointerInput(values) {
                    detectDragGestures { change, _ -> change.consume(); crossX = change.position.x }
                }
        ) {
            val plotL = 36.dp.toPx(); val plotR = size.width - 8.dp.toPx()
            val plotT = 8.dp.toPx();  val plotB = size.height - 22.dp.toPx()
            val plotW = plotR - plotL; val plotH = plotB - plotT

            // engineering grid
            drawRect(c.hairline, topLeft = Offset(plotL, plotT), size = Size(plotW, plotH), style = Stroke(1.dp.toPx()))
            val minor = 24.dp.toPx(); val major = 120.dp.toPx()
            var gx = plotL + minor
            while (gx < plotR) {
                val isMajor = ((gx - plotL) % major) < minor
                drawLine(if (isMajor) c.gridMajor else c.gridMinor, Offset(gx, plotT), Offset(gx, plotB), 1f)
                gx += minor
            }
            var gy = plotT + minor
            while (gy < plotB) {
                val isMajor = ((gy - plotT) % major) < minor
                drawLine(if (isMajor) c.gridMajor else c.gridMinor, Offset(plotL, gy), Offset(plotR, gy), 1f)
                gy += minor
            }
            // y labels
            for (i in 0 until 5) {
                val frac = i / 4f
                val y = plotB - frac * plotH
                drawText(tm, yFormat(yMin + (yMax - yMin) * frac), topLeft = Offset(2f, y - 6.dp.toPx()), style = yStyle)
            }
            // x labels
            for (i in 0 until 5) {
                val lbl = timeLabelFor(i / 4f)
                if (lbl.isNotEmpty()) {
                    val m = tm.measure(lbl, yStyle)
                    val x = (plotL + (i / 4f) * plotW - m.size.width / 2f).coerceIn(plotL, plotR - m.size.width)
                    drawText(m, topLeft = Offset(x, plotB + 4.dp.toPx()))
                }
            }

            fun vy(v: Float) = plotB - ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f) * plotH

            if (values.size < 2) {   // NO SIGNAL
                drawLine(c.ink40, Offset(plotL, vy(yMin)), Offset(plotR, vy(yMin)), 2.dp.toPx(), cap = StrokeCap.Square)
                drawText(tm, "NO SIGNAL", topLeft = Offset(plotL + 8.dp.toPx(), plotT + 8.dp.toPx()), style = yStyle)
                return@Canvas
            }

            val stepX = plotW / (values.size - 1)
            fun buildPath(list: List<Float>) = Path().apply {
                list.forEachIndexed { i, v ->
                    val x = plotL + i * stepX; val y = vy(v)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            secondaryValues?.takeIf { it.size > 1 }?.let {
                drawPath(buildPath(it), c.ink40, style = Stroke(2.dp.toPx(), cap = StrokeCap.Square))
            }

            // primary trace, trimmed by the pen sweep
            val full = buildPath(values)
            val pm = PathMeasure(); pm.setPath(full, false)
            val shown = Path()
            pm.getSegment(0f, pm.length * sweep.value, shown, true)
            drawPath(shown, traceColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Square))
            pm.getPosition(pm.length * sweep.value)?.let { pos ->
                drawCircle(traceColor, radius = 3.dp.toPx(), center = pos)   // the pen
            }

            // crosshair + leader note
            crossX?.let { cx ->
                if (cx in plotL..plotR) {
                    drawLine(c.ink, Offset(cx, plotT), Offset(cx, plotB), 1.dp.toPx())
                    val idx = (((cx - plotL) / plotW) * (values.size - 1)).roundToInt().coerceIn(0, values.size - 1)
                    drawCircle(c.ink, radius = 4.dp.toPx(), center = Offset(plotL + idx * stepX, vy(values[idx])))
                    val note = "${valueFormat(values[idx])} · min ${valueFormat(stats.first)} · " +
                               "max ${valueFormat(stats.second)} · avg ${valueFormat(stats.third)}"
                    val m = tm.measure(note, yStyle.copy(color = c.ink))
                    val noteW = m.size.width + 12f
                    val nx = (plotR - noteW).coerceAtLeast(plotL)
                    drawRect(c.panel, topLeft = Offset(nx, plotT + 4f), size = Size(noteW, m.size.height + 8f))
                    drawRect(c.hairline, topLeft = Offset(nx, plotT + 4f), size = Size(noteW, m.size.height + 8f), style = Stroke(1f))
                    drawText(m, topLeft = Offset(nx + 6f, plotT + 8f))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$windowLabel window", style = Caliper.type.meta, color = c.ink40)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("pen ", style = Caliper.type.meta, color = c.ink40)
                Canvas(Modifier.size(6.dp)) { drawCircle(traceColor, radius = size.minDimension / 2) }
                Text(" ${values.lastOrNull()?.let(valueFormat) ?: "—"}", style = Caliper.type.meta, color = c.ink)
            }
        }
    }
}

// ─────────────────────────── CoreRail (§5.6) ───────────────────────────

data class CoreReading(val id: Int, val load: Float, val freqKhz: Long)

@Composable
fun CoreRail(cores: List<CoreReading>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cores.forEach { core ->
            Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("C${core.id}", style = Caliper.type.dataS, color = Caliper.colors.ink60,
                    modifier = Modifier.width(28.dp))
                CoreBar(core.load / 100f, Modifier.weight(1f).padding(end = 8.dp))
                Text(String.format(java.util.Locale.US, "%3d%%", core.load.roundToInt()),
                    style = Caliper.type.dataS, color = Caliper.colors.ink, modifier = Modifier.width(48.dp))
                Text(Fmt.hz(core.freqKhz), style = Caliper.type.dataS, color = Caliper.colors.ink60)
            }
        }
    }
}

@Composable
private fun CoreBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    val anim by animateFloatAsState(fraction, CaliperMotion.Needle, label = "core")
    val peak = rememberPeakHold(fraction)
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val mid = size.height / 2
        drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { t ->
            drawLine(c.hairline, Offset(size.width * t, mid - 3.dp.toPx()), Offset(size.width * t, mid + 3.dp.toPx()), 1.dp.toPx())
        }
        val bh = 8.dp.toPx(); val top = mid - bh / 2
        drawRect(c.channel(Channels.CPU), topLeft = Offset(0f, top), size = Size(size.width * anim, bh))
        val px = size.width * peak   // ⌃ peak-hold caret
        val tri = Path().apply {
            moveTo(px, top - 3.dp.toPx()); lineTo(px - 3.dp.toPx(), top); lineTo(px + 3.dp.toPx(), top); close()
        }
        drawPath(tri, c.ink)
    }
}

// ─────────────────────────── LinearGauge (§5.7 — battery fuel) ───────────────────────────

@Composable
fun LinearGauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    voltage: String? = null,
    charging: Boolean = false,
    critical: Boolean = fraction < 0.2f
) {
    val c = Caliper.colors
    val tm = rememberTextMeasurer()
    val reduced = rememberReducedMotion()
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), CaliperMotion.Needle, label = "fuel")
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val labelStyle = TextStyle(fontFamily = PlexMonoFamily, fontSize = 10.sp, color = c.ink40, fontFeatureSettings = "tnum")

    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val mid = size.height / 2
            val trackH = 12.dp.toPx(); val top = mid - trackH / 2
            drawRect(c.hairline, topLeft = Offset(0f, top), size = Size(size.width, trackH), style = Stroke(1.dp.toPx()))
            for (i in 0..20) {   // ticks every 5%; tall at 0/25/50/75/100
                val x = size.width * i / 20f
                val major = (i * 5) % 25 == 0
                val len = (if (major) 5.dp else 3.dp).toPx()
                drawLine(c.ink40, Offset(x, top - len), Offset(x, top), 1.dp.toPx())
            }
            drawRect(if (critical) c.fault else c.channel(Channels.POWER),
                topLeft = Offset(0f, top), size = Size(size.width * anim, trackH))
            val ks = 10.dp.toPx()
            val kx = (size.width * anim - ks / 2).coerceIn(0f, size.width - ks)
            drawRect(c.ink.copy(alpha = if (charging && !reduced) blink else 1f),
                topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
            listOf(0, 25, 50, 75, 100).forEach { pct ->
                val x = size.width * pct / 100f
                val m = tm.measure("$pct", labelStyle)
                drawText(m, topLeft = Offset(x - m.size.width / 2f, mid + trackH / 2 + 2.dp.toPx()))
            }
        }
        if (voltage != null) {
            Spacer(Modifier.width(12.dp))
            Text(voltage, style = Caliper.type.dataS, color = Caliper.colors.ink60)
        }
    }
}

// ─────────────────────────── HatchBar (§5.8 — storage map) ───────────────────────────

data class HatchSegment(val label: String, val bytes: Long, val color: Color, val pattern: HatchPattern)

@Composable
fun HatchBar(
    segments: List<HatchSegment>,
    modifier: Modifier = Modifier,
    onSegmentTap: (HatchSegment) -> Unit = {}
) {
    val c = Caliper.colors
    val total = segments.sumOf { it.bytes }.coerceAtLeast(1)
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(16.dp)
                .pointerInput(segments, total) {
                    detectTapGestures { pos ->
                        var acc = 0f
                        val w = size.width.toFloat()
                        for (seg in segments) {
                            val segW = w * seg.bytes / total
                            if (pos.x < acc + segW) { onSegmentTap(seg); return@detectTapGestures }
                            acc += segW
                        }
                    }
                }
        ) {
            drawRect(c.hairline, style = Stroke(1.dp.toPx()))
            var left = 1f
            segments.forEach { seg ->
                val w = size.width * seg.bytes / total
                hatch(Rect(left, 0f, left + w, size.height), seg.pattern, seg.color)
                left += w
            }
        }
        Spacer(Modifier.height(8.dp))
        Column {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(seg.color).border(1.dp, c.hairline))
                    Text("  ${seg.label}", style = Caliper.type.dataS, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.bytes(seg.bytes), style = Caliper.type.dataS, color = c.ink)
                }
            }
        }
    }
}

// ─────────────────────────── ThermalGauge (ramp — not a channel) ───────────────────────────

@Composable
fun ThermalGauge(tempC: Float, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    val zone = when {
        tempC < 45 -> "neutral"; tempC < 55 -> "warm"; tempC < 65 -> "moderate"
        tempC < 75 -> "severe"; else -> "thermal throttling"
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OdometerText(Fmt.temp(tempC), style = Caliper.type.readoutL)
        Spacer(Modifier.width(16.dp))
        Canvas(Modifier.weight(1f).height(10.dp)) {
            val segs = 12
            val filled = (tempC / 90f * segs).toInt().coerceIn(0, segs)
            val segW = size.width / segs
            for (i in 0 until segs) {
                val frac = i.toFloat() / segs
                val col = when {
                    frac < 0.5f -> c.channel(Channels.POWER)   // amber
                    frac < 0.8f -> c.channel(Channels.CPU)     // vermilion
                    else -> c.fault                             // deep red
                }
                drawRect(col.copy(alpha = if (i < filled) 1f else 0.15f),
                    topLeft = Offset(i * segW + 1f, 0f), size = Size(segW - 2f, size.height))
            }
        }
        Text("  zone: $zone", style = Caliper.type.meta, color = c.ink60)
    }
}