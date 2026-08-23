package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow
import com.ivarna.deviceinsight.data.monitor.isNoSignal

@Composable
private fun Text(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicText(text, style = style, modifier = modifier)
}

@Composable
private fun BandLabel(text: String, tick: Color) {
    val c = LocalHudColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        HudTick(tick)
        Spacer(Modifier.width(5.dp))
        Text(text.uppercase(), hudStyle(LocalHudMetrics.current.metaSp, trackingEm = 0.08f).copy(color = c.ink40))
    }
}

// ─────────────── HM-0 · HEADER — clock, LED, lock affordance ───────────────

@Composable
fun HudHeaderBand(slow: HudSlow, paused: Boolean, fault: Boolean, locked: Boolean, onLock: () -> Unit) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!locked) {
            // crosshair = lock key (tap ⌖ to lock while unlocked)
            Canvas(Modifier.size(12.dp).clickableNoIndication(onLock)) {
                val r = size.minDimension / 2 - 1.5.dp.toPx()
                drawCircle(c.ink, radius = r, style = Stroke(1.5.dp.toPx()))
                drawLine(c.ink, Offset(0f, center.y), Offset(size.width, center.y), 1.5f)
                drawLine(c.ink, Offset(center.x, 0f), Offset(center.x, size.height), 1.5f)
            }
            Spacer(Modifier.width(6.dp))
        }
        Text("DI·HUD", hudStyle(m.microSp, trackingEm = 0.1f).copy(color = c.ink40))
        Spacer(Modifier.weight(1f))
        StrokedText(FmtHud.clock(slow.timestamp), hudStyle(m.valueSp), fill = c.ink)
        Spacer(Modifier.width(8.dp))
        LedPulse(color = c.accent, active = !paused, fault = fault)
    }
}

// ─────────────── HM-1 · FPS — hero band ───────────────

@Composable
fun HudFpsBand(fast: HudFast) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("FPS", hudStyle(m.metaSp, trackingEm = 0.08f).copy(color = c.ink40))
            Spacer(Modifier.width(8.dp))
            if (fast.isNoSignal()) {
                // honest signal only HUD — no display-refresh fallback
                StrokedText("—", hudStyle(m.heroSp, androidx.compose.ui.text.font.FontWeight.Light, 0f), fill = c.ink60)
                Spacer(Modifier.width(6.dp))
                Text("NO SIGNAL", hudStyle(m.microSp, trackingEm = 0.08f).copy(color = c.fault))
            } else {
                StrokedText(
                    fast.fps.toString(),
                    hudStyle(m.heroSp, androidx.compose.ui.text.font.FontWeight.Light, 0f),
                    fill = c.ink
                )
                Spacer(Modifier.width(8.dp))
                HudStamp(fast.source, c.ink40)   // honesty: SF or GFX
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

// ─────────────── HM-2 · CPU ───────────────

@Composable
fun HudCpuBand(slow: HudSlow, showCoreBank: Boolean) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BandLabel("CH-01 · CPU", c.ch01)
            Spacer(Modifier.weight(1f))
            MiniOdometer(FmtHud.pct(slow.cpuPct), hudStyle(m.valueSp), c.ink)
            Spacer(Modifier.width(8.dp))
            val avgFreq = slow.cores.map { it.freqMhz }.average().toInt()
            if (avgFreq > 0) {
                StrokedText(FmtHud.ghz(avgFreq), hudStyle(m.valueSp), fill = c.ink60)
                Spacer(Modifier.width(8.dp))
            }
            StrokedText(FmtHud.temp(slow.tempC), hudStyle(m.valueSp), fill = thermalColor(slow.tempC))
        }
        if (showCoreBank && slow.cores.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            CoreBank(slow.cores, slow.clusterSizes)
        }
        if (m.showGovLine && slow.cores.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            val peak = slow.cores.maxByOrNull { it.freqMhz }
            val govLine = buildString {
                append("gov ${slow.governor ?: "—"}")
                peak?.let { append(" · peak C${it.id} ${it.freqMhz} MHz") }
            }
            Text(govLine, hudStyle(m.microSp).copy(color = c.ink40))
        }
    }
}

// ─────────────── HM-3 · MEMORY ───────────────

@Composable
private fun MemRow(label: String, usedMb: Int, totalMb: Int, tickColor: Color, pattern: MemPattern) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val fitted = totalMb > 0
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HudTick(tickColor)
            Spacer(Modifier.width(5.dp))
            Text(label, hudStyle(m.metaSp, trackingEm = 0.08f).copy(color = c.ink40))
            Spacer(Modifier.weight(1f))
            if (fitted) {
                Text("${FmtHud.mb(usedMb)}   ${FmtHud.pct(100f * usedMb / totalMb)}", hudStyle(m.valueSp).copy(color = c.ink))
            } else {
                Text("— NOT FITTED", hudStyle(m.valueSp).copy(color = c.ink40))
            }
        }
        if (fitted) {
            Spacer(Modifier.height(3.dp))
            MemBar(usedMb.toFloat() / totalMb.toFloat(), tickColor, pattern)
        }
    }
}

// ─────────────── HM-3 · MEMORY band ───────────────

@Composable
fun HudMemoryBand(slow: HudSlow) {
    val m = LocalHudMetrics.current
    Column(Modifier.fillMaxWidth()) {
        MemRow(
            "CH-02 · RAM",
            usedMb = (slow.memUsedGb * 1024f).toInt(),
            totalMb = (slow.memTotalGb * 1024f).toInt(),
            tickColor = LocalHudColors.current.ch02,
            pattern = MemPattern.SOLID
        )
        Spacer(Modifier.height(5.dp))
        // SWP — NOT FITTED when swapTotal==0 (plan acceptance); zram counts into used when present
        val swapUsedMb = ((slow.swapUsedGb + slow.zramGb) * 1024f).toInt()
        val swapTotalMb = if (slow.swapTotalMb > 0) (slow.swapTotalMb / 1024L).toInt()
                          else if (slow.zramGb > 0f) (slow.zramGb * 1024f).toInt() else 0
        MemRow(
            "CH-02 · SWP",
            usedMb = swapUsedMb,
            totalMb = swapTotalMb,
            tickColor = LocalHudColors.current.ch02,
            pattern = MemPattern.CROSS
        )
        if (m.showGovLine && slow.zramGb > 0f) {
            Spacer(Modifier.height(2.dp))
            Text("zram ${String.format(java.util.Locale.US, "%.1f GB", slow.zramGb)}", hudStyle(m.microSp).copy(color = LocalHudColors.current.ink40))
        }
    }
}

// ─────────────── HM-4b · POWER ───────────────

@Composable
fun HudPowerBand(slow: HudSlow) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val charging = slow.charging
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BandLabel("CH-04 · PWR", c.ch04)
            Spacer(Modifier.weight(1f))
            // signed watts, honest zero (≈ prefix per Fmt.wattsSigned)
            StrokedText(FmtHud.wattsSigned(slow.watts), hudStyle(m.valueSp), fill = if (charging) c.ch04 else c.ink)
            Spacer(Modifier.width(10.dp))
            StrokedText("BAT ${FmtHud.pct(slow.batteryPct * 100f)}", hudStyle(m.valueSp), fill = if (slow.batteryPct < 0.2f) c.fault else c.ink)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FuelMicro(slow.batteryPct, critical = slow.batteryPct < 0.2f, charging = charging)
            }
            if (slow.voltage > 0f) {
                Spacer(Modifier.width(8.dp))
                Text("${(slow.voltage * 1000).toInt()} mV", hudStyle(m.microSp).copy(color = c.ink40))
            }
        }
        FmtHud.remaining(slow.remainingMin)?.let { rem ->
            Spacer(Modifier.height(2.dp))
            Text(
                if (charging) "charging" else rem,
                hudStyle(m.microSp).copy(color = c.ink40)
            )
        }
    }
}

// ─────────────── HM-5/6 · GPU / NETWORK (optional modules) ───────────────

@Composable
fun HudGpuBand(slow: HudSlow) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BandLabel("CH-06 · GPU", c.ch06)
        Spacer(Modifier.weight(1f))
        when {
            !slow.gpuFitted -> Text("— NOT FITTED", hudStyle(m.valueSp).copy(color = c.ink40))
            slow.gpuRootLocked -> Text("— CHANNEL LOCKED", hudStyle(m.valueSp).copy(color = c.fault))
            else -> StrokedText(
                "${FmtHud.pct(slow.gpuPct ?: 0f)} · ${slow.gpuMHz ?: 0} MHz",
                hudStyle(m.valueSp), fill = c.ink
            )
        }
    }
}

@Composable
fun HudNetBand(slow: HudSlow) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BandLabel("CH-03 · NET", c.ch03)
        Spacer(Modifier.weight(1f))
        StrokedText("↓ ${FmtHud.rate(slow.netDown)}   ↑ ${FmtHud.rate(slow.netUp)}", hudStyle(m.valueSp), fill = c.ink)
    }
}
