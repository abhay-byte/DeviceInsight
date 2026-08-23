package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─────────────── corner brackets — the only frame (DI-HD-001 §2) ───────────────

fun Modifier.hudFrame(
    color: Color, inset: Dp = 3.dp, len: Dp = 12.dp, stroke: Dp = 1.5.dp
): Modifier = drawBehind {
    val i = inset.toPx(); val l = len.toPx(); val w = stroke.toPx()
    val W = size.width; val H = size.height
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(x1, y1), Offset(x2, y2), w)
    line(i, i, i + l, i);                  line(i, i, i, i + l)                    // TL
    line(W - i, i, W - i - l, i);          line(W - i, i, W - i, i + l)            // TR
    line(i, H - i, i + l, H - i);          line(i, H - i, i, H - i - l)            // BL
    line(W - i, H - i, W - i - l, H - i);  line(W - i, H - i, W - i, H - i - l)    // BR
}

// ─────────────── stroked text — legibility layer 3 (§4) ───────────────

/** Value text with a 1dp scrim-colored stroke under its fill. Survives any scene. */
@Composable
fun StrokedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    fill: Color,
    strokeColor: Color = LocalHudColors.current.scrim,
    strokeWidth: Dp = 1.dp,
    modifier: Modifier = Modifier
) {
    val px = with(LocalDensity.current) { strokeWidth.toPx() }
    Box(modifier) {
        androidx.compose.foundation.text.BasicText(
            text, style = style.copy(
                color = strokeColor,
                drawStyle = Stroke(px, cap = StrokeCap.Square)
            )
        )
        androidx.compose.foundation.text.BasicText(text, style = style.copy(color = fill))
    }
}

// ─────────────── LED — the sanctioned pulse ───────────────

@Composable
fun LedPulse(color: Color, active: Boolean = true, fault: Boolean = false, size: Dp = 5.dp) {
    val reduced = rememberReducedMotion()
    val pulse by rememberInfiniteTransition(label = "led").animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val c = when { !active -> Color.Transparent; fault -> LocalHudColors.current.fault; else -> color }
    Canvas(Modifier.size(size)) {
        drawCircle(c.copy(alpha = if (reduced) 1f else pulse), radius = size.toPx() / 2)
    }
}

@Composable
private fun rememberReducedMotion(): Boolean = com.ivarna.deviceinsight.ui.caliper.rememberReducedMotion()

// ─────────────── hairline & channel tick ───────────────

@Composable
fun HairlineH(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalHudColors.current.hairline))
}

@Composable
fun HudTick(color: Color, size: Dp = 4.dp) {
    Box(Modifier.size(size).background(color))
}

// ─────────────── SparkPen — trace with square pen head (§5) ───────────────

@Composable
fun SparkPen(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    yMax: Float? = null,
    stroke: Dp = 1.5.dp,
    penSize: Dp = 3.dp
) {
    val hairline = LocalHudColors.current.hairline
    Canvas(modifier) {
        if (values.size < 2) {
            // honest no-signal: flat hairline
            val mid = size.height / 2f
            drawLine(hairline, Offset(0f, mid), Offset(size.width, mid), 1f)
            return@Canvas
        }
        val max = (yMax ?: values.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val y = size.height * (1f - (v / max).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
            if (i == 0) path.moveTo(0f, y) else path.lineTo(i * step, y)
        }
        drawPath(path, color, style = Stroke(stroke.toPx(), cap = StrokeCap.Square))
        val lastY = size.height * (1f - (values.last() / max).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
        val ps = penSize.toPx()   // the pen — square, always
        drawRect(color, topLeft = Offset(size.width - ps, lastY - ps / 2), size = Size(ps, ps))
    }
}

// ─────────────── MemBar — hatched micro bar (§2 HM-3) ───────────────

enum class MemPattern { SOLID, CROSS }

@Composable
fun MemBar(
    fraction: Float,
    color: Color,
    pattern: MemPattern,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    animate: Boolean = true
) {
    val c = LocalHudColors.current
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), HudNeedle, label = "mem")
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawRect(c.hairline, style = Stroke(1.dp.toPx()))
        val target = if (animate) anim else fraction.coerceIn(0f, 1f)
        val w = size.width * target - 2f
        if (w > 2f) when (pattern) {
            MemPattern.SOLID -> drawRect(color, topLeft = Offset(1f, 1f), size = Size(w, size.height - 2f))
            MemPattern.CROSS -> clipRect(1f, 1f, 1f + w, size.height - 1f) {
                val p = 3.dp.toPx()
                var x = -size.height
                while (x < w + size.height) {
                    drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
                    drawLine(color, Offset(x, 0f), Offset(x + size.height, size.height), 1.dp.toPx())
                    x += p
                }
            }
        }
    }
}

/** Needle spring + peak-hold (shared motion token). */
val HudNeedle: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 420f)

@Composable
fun rememberPeakHold(value: Float, decayMs: Long = 2000): Float {
    var peak by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value >= peak) {
            peak = value
        } else {
            kotlinx.coroutines.delay(decayMs)
            peak = value   // decay to current after hold window
        }
    }
    return peak
}

// ─────────────── FuelMicro — battery gauge (§2 HM-4) ───────────────

@Composable
fun FuelMicro(
    fraction: Float,
    modifier: Modifier = Modifier,
    critical: Boolean = fraction < 0.2f,
    height: Dp = 12.dp,
    charging: Boolean = false
) {
    val c = LocalHudColors.current
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), HudNeedle, label = "fuel")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val mid = size.height / 2f
        val track = 6.dp.toPx()
        val top = mid - track / 2
        drawRect(c.hairline, topLeft = Offset(0f, top), size = Size(size.width, track), style = Stroke(1.dp.toPx()))
        for (i in 0..20) {   // ticks every 5%; major at 0/25/50/75/100
            val x = size.width * i / 20f
            val major = (i * 5) % 25 == 0
            val len = (if (major) 4.dp else 2.dp).toPx()
            drawLine(c.ink40, Offset(x, top - len), Offset(x, top), 1.dp.toPx())
        }
        drawRect(if (critical) c.fault else c.ch04, topLeft = Offset(0f, top), size = Size(size.width * anim, track))
        val ks = 8.dp.toPx()   // square needle knob — accent when charging
        val kx = (size.width * anim - ks / 2).coerceIn(0f, size.width - ks)
        drawRect(if (charging) c.accent else c.ink, topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
    }
}

// ─────────────── CoreBank — the mixing console (§2 HM-2) ───────────────

@Composable
fun CoreBank(
    cores: List<com.ivarna.deviceinsight.data.monitor.CoreStat>,
    clusterSizes: List<Int>,
    modifier: Modifier = Modifier
) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
        cores.forEachIndexed { index, core ->
            // cluster rule at L: hairline divider between big/mid/little groups
            val clusterStarts = clusterSizes.runningFold(0) { acc: Int, n: Int -> acc + n }.drop(1).toSet()
            val divider = m.showGovLine && index > 0 && index in clusterStarts
            Row(Modifier.weight(1f)) {
                if (divider) Box(Modifier.width(1.dp).fillMaxHeight(0.8f).background(c.hairline))
                CoreCell(core, c.ch01)
            }
        }
    }
}

@Composable
private fun CoreCell(core: com.ivarna.deviceinsight.data.monitor.CoreStat, barColor: Color) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val load by animateFloatAsState(core.loadPct / 100f, HudNeedle, label = "core${core.id}")
    val peak = rememberPeakHold(core.loadPct / 100f)
    Row {
        Canvas(Modifier.size(width = 5.dp, height = 22.dp)) {
            drawRect(c.hairline, style = Stroke(1.dp.toPx()))
            val h = size.height * load.coerceIn(0f, 1f)
            drawRect(barColor, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
            val py = size.height * peak.coerceIn(0f, 1f)      // ⌃ peak-hold caret
            drawRect(c.ink, topLeft = Offset(0f, (py - 1.dp.toPx()).coerceAtLeast(0f)), size = Size(size.width, 1.dp.toPx()))
        }
        if (m.coreBankShowFreq) {
            Spacer(Modifier.width(4.dp))
            Column {
                androidx.compose.foundation.text.BasicText("C${core.id}", style = hudStyle(m.microSp).copy(color = c.ink40))
                androidx.compose.foundation.text.BasicText("${core.freqMhz}", style = hudStyle(m.microSp).copy(color = c.ink60))
            }
        }
    }
}

// ─────────────── MiniOdometer — 2 Hz roll for slow values (§5) ───────────────

@Composable
fun MiniOdometer(text: String, style: androidx.compose.ui.text.TextStyle, color: Color, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    Row(modifier) {
        text.forEach { ch ->
            var shown by remember { mutableStateOf(ch) }
            LaunchedEffect(ch) { shown = ch }
            if (reduced) {
                androidx.compose.foundation.text.BasicText(shown.toString(), style = style.copy(color = color))
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = shown,
                    transitionSpec = {
                        (androidx.compose.animation.slideInVertically(tween(160)) { it / 2 } + androidx.compose.animation.fadeIn(tween(100)))
                            .togetherWith(
                                androidx.compose.animation.slideOutVertically(tween(160)) { -it / 2 } + androidx.compose.animation.fadeOut(tween(100))
                            )
                    },
                    label = "mini"
                ) { d ->
                    androidx.compose.foundation.text.BasicText(d.toString(), style = style.copy(color = color))
                }
            }
        }
    }
}

// ─────────────── thermal coloring — color is signal (§5) ───────────────

@Composable
fun thermalColor(tempC: Float): Color {
    val c = LocalHudColors.current
    return when {
        tempC >= 75f -> c.fault
        tempC >= 65f -> c.ch04
        else -> c.ink60
    }
}

// ─────────────── stamp — source honesty (§2 HM-1) ───────────────

@Composable
fun HudStamp(text: String, color: Color) {
    Box(Modifier.border(1.dp, color).padding(horizontal = 3.dp, vertical = 1.dp)) {
        androidx.compose.foundation.text.BasicText(
            text.uppercase(), style = hudStyle(8, trackingEm = 0.1f).copy(color = color))
    }
}

// ─────────────── click without ripple (sketch §3.5 compile fix) ───────────────

fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
