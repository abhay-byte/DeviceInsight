package com.ivarna.deviceinsight.ui.caliper.hud

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.components.OdometerText
import com.ivarna.deviceinsight.ui.caliper.components.StampBadge
import kotlin.math.roundToInt

/** HUD is always dark-scrim — it floats over games, not over your theme. */
object HudInk {
    val text = Color(0xFFF2EEE2)
    val dim = Color(0x99F2EEE2)
    val scrim = Color(0xB3141310)
    val cpu = Color(0xFFFF6B4A); val gpu = Color(0xFFF06BB0)
    val pwr = Color(0xFFFFB84D); val net = Color(0xFF2FD3B0)
    // m1: HUD scrim blur is the single sanctioned blur — 8dp on API 31+ via
    // Modifier.blur (RenderEffect-backed), no blur fallback on older devices.
}

data class HudState(
    val fps: Float?, val fpsSource: String,          // "SF" | "GFX" — honesty about the measurement
    val cpu: Float, val cpuHist: List<Float>,
    val gpu: Float?, val ramBytes: Long, val tempC: Float,
    val netDown: Long, val netUp: Long
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CaliperHud(state: HudState, modifier: Modifier = Modifier) {
    Box(modifier) {
        // §5.11: 70% ink scrim — blur the scrim only (8dp, API 31+), never the labels.
        Box(
            Modifier.matchParentSize().background(HudInk.scrim)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(8.dp)
                    else Modifier
                )
        )
        Column(
            Modifier.padding(10.dp)
                .drawBehind { drawCornerBrackets(HudInk.text, 10.dp.toPx(), 12.dp.toPx(), 1.5.dp.toPx()) }
        ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            OdometerText(
                text = state.fps?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.readoutL, color = HudInk.text, staggerMs = 0
            )
            StampBadge(state.fpsSource, color = HudInk.dim, rotation = 0f, animateIn = false)
        }
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(HudInk.dim.copy(alpha = 0.4f), Offset.Zero, Offset(size.width, 0f), 1f)
        }
        Spacer(Modifier.height(6.dp))
        HudRow(HudInk.cpu, "CPU", "${state.cpu.roundToInt()}%", state.cpuHist)
        state.gpu?.let { HudRow(HudInk.gpu, "GPU", "${it.roundToInt()}%", emptyList()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RAM  ${Fmt.bytes(state.ramBytes)}", style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.meta, color = HudInk.dim)
            Text("TEMP  ${Fmt.temp(state.tempC)}", style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.meta, color = HudInk.dim)
        }
        Text(
            "NET  ↓${Fmt.rate(state.netDown)}  ↑${Fmt.rate(state.netUp)}",
            style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.meta, color = HudInk.dim
        )
        }
    }
}

@Composable
private fun HudRow(tickColor: Color, label: String, value: String, hist: List<Float>) {
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(tickColor))
        Text(" $label", style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.meta, color = HudInk.dim)
        Spacer(Modifier.weight(1f))
        Text(value, style = com.ivarna.deviceinsight.ui.caliper.Caliper.type.dataS, color = HudInk.text)
        if (hist.size > 1) {
            Spacer(Modifier.width(6.dp))
            Canvas(Modifier.size(width = 40.dp, height = 12.dp)) {
                val step = size.width / (hist.size - 1)
                val maxV = (hist.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
                val p = androidx.compose.ui.graphics.Path()
                hist.forEachIndexed { i, v ->
                    val x = i * step; val y = size.height * (1f - v / maxV)
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, tickColor, style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawCornerBrackets(color: Color, inset: Float, len: Float, stroke: Float) {
    val w = size.width; val h = size.height
    drawLine(color, Offset(inset, inset), Offset(inset + len, inset), stroke)
    drawLine(color, Offset(inset, inset), Offset(inset, inset + len), stroke)
    drawLine(color, Offset(w - inset, inset), Offset(w - inset - len, inset), stroke)
    drawLine(color, Offset(w - inset, inset), Offset(w - inset, inset + len), stroke)
    drawLine(color, Offset(inset, h - inset), Offset(inset + len, h - inset), stroke)
    drawLine(color, Offset(inset, h - inset), Offset(inset, h - inset - len), stroke)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset - len, h - inset), stroke)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset, h - inset - len), stroke)
}