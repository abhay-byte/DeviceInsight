package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import com.ivarna.deviceinsight.ui.caliper.Channel
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BenchConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
            return
        }

        val kind = resolveKind()

        // official contract: the activity ALWAYS answers with the id attached — even on cancel/back
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        lifecycleScope.launch {
            // load system medium AND the widget's saved prefs before first frame — no PAPER-first lie
            val systemMedium = runCatching { mediumFlow.first() }.getOrNull() ?: Medium.PAPER
            val glanceId = runCatching { GlanceAppWidgetManager(this@BenchConfigActivity).getGlanceIdBy(appWidgetId) }.getOrNull()
            val saved = glanceId?.let { runCatching { BenchState.config(this@BenchConfigActivity, it) }.getOrNull() }
            setContent {
                CaliperTheme(medium = systemMedium) {
                    BenchConfigScreen(
                        kind = kind,
                        systemMedium = systemMedium,
                        initial = saved ?: BenchConfig(),
                        onSave = { cfg -> saveConfig(cfg) },
                        onSkip = { saveConfig(BenchConfig()) },
                        onCancel = { setResult(RESULT_CANCELED); finish() }
                    )
                }
            }
        }
    }

    private fun resolveKind(): WidgetKind {
        return try {
            val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
            val cls = info?.provider?.className ?: return WidgetKind.SCOPE
            when {
                cls.contains("SingleChannel") -> WidgetKind.SCOPE
                cls.contains("DualChannel") -> WidgetKind.STACK
                cls.contains("Fuel") -> WidgetKind.FUEL
                cls.contains("Raster") -> WidgetKind.RASTER
                cls.contains("Bench") -> WidgetKind.BENCH
                else -> WidgetKind.SCOPE
            }
        } catch (_: Exception) { WidgetKind.SCOPE }
    }

    private fun saveConfig(cfg: BenchConfig) {
        lifecycleScope.launch {
            try {
                val mgr = GlanceAppWidgetManager(this@BenchConfigActivity)
                val glanceId = mgr.getGlanceIdBy(appWidgetId)
                BenchState.save(this@BenchConfigActivity, glanceId, cfg)
                val widget: androidx.glance.appwidget.GlanceAppWidget = when (resolveKind()) {
                    WidgetKind.SCOPE -> ScopeWidget()
                    WidgetKind.STACK -> StackWidget()
                    WidgetKind.FUEL -> FuelWidget()
                    WidgetKind.RASTER -> RasterWidget()
                    WidgetKind.BENCH -> BenchWidgetAll()
                }
                widget.update(this@BenchConfigActivity, glanceId)
            } catch (_: Exception) { }
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}

/** WD §3 media picker: three explicit media + FOLLOW SYSTEM as a fourth option. */
private enum class MediaPick { PAPER, CARBON, BLUEPRINT, FOLLOW }

@Composable
private fun BenchConfigScreen(
    kind: WidgetKind,
    systemMedium: Medium,
    initial: BenchConfig,
    onSave: (BenchConfig) -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    var pick by remember {
        mutableStateOf(if (initial.followSystem) MediaPick.FOLLOW else MediaPick.valueOf(initial.medium.name))
    }
    var cadence by remember { mutableStateOf(initial.cadence) }
    var traceWindow by remember { mutableStateOf(initial.traceWindowS) }
    var wattHero by remember { mutableStateOf(initial.wattHero) }
    var compact by remember { mutableStateOf(initial.compactChannels) }

    val followSystem = pick == MediaPick.FOLLOW
    val medium = if (pick == MediaPick.FOLLOW) systemMedium else Medium.valueOf(pick.name)

    val cfg = BenchConfig(medium, followSystem, cadence, traceWindow, wattHero, compact)
    // fresh process / stale bus → demo data, never an empty BUDGET panel; live only if <5 s old
    val now = System.currentTimeMillis()
    val live = GlobalSnapshot.current()
    val snap = if (live != null && now - live.timestamp in 0 until 5_000L) live else benchDemoSnapshot(kind)

    CaliperTheme(medium = medium) {
        Column(
            modifier = Modifier.fillMaxSize().background(Caliper.colors.surface).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            ScreenHeader("№ 05.2 — CALIBRATE", "Calibrate.", "${kind.name} instrument · configure the bench")
            Spacer(Modifier.height(12.dp))

            PreviewPanel(kind, cfg, snap)
            Text(
                "home screen · glance",
                style = Caliper.type.meta,
                color = Caliper.colors.ink40,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))

            Text("MEDIA", style = Caliper.type.meta, color = Caliper.colors.ink60)
            Spacer(Modifier.height(6.dp))
            SegKey(
                options = MediaPick.entries.toList(),
                selected = pick,
                onSelect = { pick = it },
                labelFor = { it.name }
            )
            Spacer(Modifier.height(12.dp))

            Text("CADENCE", style = Caliper.type.meta, color = Caliper.colors.ink60)
            Spacer(Modifier.height(6.dp))
            SegKey(options = Cadence.entries.toList(), selected = cadence, onSelect = { cadence = it }, labelFor = { it.name })
            Spacer(Modifier.height(12.dp))

            when (kind) {
                WidgetKind.FUEL -> {
                    DipSwitch(checked = wattHero, onCheckedChange = { wattHero = it }, label = "wattage hero (off = percent)")
                    Spacer(Modifier.height(12.dp))
                }
                WidgetKind.SCOPE -> {
                    Text("TRACE WINDOW", style = Caliper.type.meta, color = Caliper.colors.ink60)
                    Spacer(Modifier.height(6.dp))
                    SegKey(options = listOf(60, 300), selected = traceWindow, onSelect = { traceWindow = it }, labelFor = { "${it}s" })
                    Spacer(Modifier.height(12.dp))
                }
                WidgetKind.BENCH -> {
                    Text("COMPACT CHANNELS", style = Caliper.type.meta, color = Caliper.colors.ink60)
                    Spacer(Modifier.height(6.dp))
                    val options = listOf(
                        listOf("CH-01", "CH-02", "CH-04", "CH-03"),
                        listOf("CH-01", "CH-02", "CH-03", "CH-06"),
                        listOf("CH-04", "CH-05", "CH-02", "CH-01")
                    )
                    SegKey(options = options, selected = compact, onSelect = { compact = it }, labelFor = { it.joinToString(" · ") })
                    Spacer(Modifier.height(12.dp))
                }
                else -> {}
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                HardKey("SKIP — USE DEFAULTS", variant = HardKeyVariant.SECONDARY, modifier = Modifier.weight(1f), onClick = onSkip)
                HardKey("SAVE", variant = HardKeyVariant.PRIMARY, modifier = Modifier.weight(1f), onClick = { onSave(cfg) })
            }
            Spacer(Modifier.height(8.dp))
            HardKey("CANCEL", variant = HardKeyVariant.DISABLED, modifier = Modifier.fillMaxWidth(), onClick = onCancel)
            EndOfSheet()
        }
    }
}

// ─────────────── Preview: Compose facsimile of the Glance T2 band tree ───────────────
// Glance has no Compose ScopeTrace and cannot be embedded here — honest anatomy match,
// not pixel-identical: header · hero · sublines · canvas band · footer upd.

@Composable
private fun PreviewPanel(kind: WidgetKind, cfg: BenchConfig, snap: BenchSnapshot) {
    PanelCard(title = "PREVIEW", status = { Text(kind.name, style = Caliper.type.meta, color = Caliper.colors.ink40) }) {
        val stale = snap.timestamp == 0L
        when (kind) {
            WidgetKind.SCOPE -> {
                BandHeader("CH-01", "CPU", if (stale) "SIGNAL LOST" else "LIVE", Channels.CPU)
                HeroLine(Fmt.pct(snap.cpuPct, 1), stale)
                if (snap.freqGHz > 0) MetaSub(Fmt.hz((snap.freqGHz * 1e6).toLong()))
                if (snap.tempC > 0) MetaSub(Fmt.temp(snap.tempC))
                SparkCanvas(snap.cpuHist, Caliper.colors.channel(Channels.CPU), height = 72.dp)
                if (!snap.governor.isNullOrBlank()) MetaSub(snap.governor!!)
                BandFooter(updString(snap.timestamp), "${cfg.traceWindowS}s window", stale)
            }
            WidgetKind.STACK -> {
                BandHeader("CH-02", "MEMORY", if (stale) "SIGNAL LOST" else "LIVE", Channels.MEMORY)
                HeroLine(
                    if (snap.memTotalGb > 0) String.format(java.util.Locale.US, "%.1f / %.0f GB", snap.memUsedGb, snap.memTotalGb) else "—",
                    stale
                )
                HatchCanvas(snap.memComposition, height = 14.dp)
                if (snap.memComposition.isNotEmpty()) MetaSub(compositionLabel(snap))
                SparkCanvas(snap.memHist, Caliper.colors.channel(Channels.MEMORY), height = 28.dp)
                BandFooter(updString(snap.timestamp), "${cfg.traceWindowS}s window", stale)
            }
            WidgetKind.FUEL -> {
                BandHeader("CH-04", "POWER", if (snap.charging) "CHARGING" else if (stale) "SIGNAL LOST" else "LIVE", Channels.POWER)
                HeroLine(if (cfg.wattHero) Fmt.wattsSigned(snap.watts) else Fmt.pct(snap.batteryPct * 100, 0), stale)
                if (!cfg.wattHero) MetaSub(Fmt.wattsSigned(snap.watts))
                FuelCanvas(snap.batteryPct, Caliper.colors.channel(Channels.POWER), snap.charging, height = 14.dp)
                SparkCanvas(snap.wattHist, Caliper.colors.channel(Channels.POWER), height = 32.dp)
                BandFooter(updString(snap.timestamp), "${cfg.traceWindowS}s window", stale)
            }
            WidgetKind.RASTER -> {
                BandHeader("CH-06", "GPU", if (stale) "SIGNAL LOST" else "LIVE", Channels.GPU)
                when {
                    !snap.gpuFitted -> {
                        Text("NOT FITTED", style = Caliper.type.dataM, color = Caliper.colors.ink40)
                        if (snap.gpuName.isNotBlank()) MetaSub(snap.gpuName)
                    }
                    snap.gpuRootLocked -> Text("CHANNEL LOCKED", style = Caliper.type.dataM, color = Caliper.colors.fault)
                    else -> {
                        HeroLine("${snap.gpuPct?.toInt() ?: 0}% · ${snap.gpuMHz ?: 0} MHz", stale)
                        SparkCanvas(snap.gpuHist, Caliper.colors.channel(Channels.GPU), height = 28.dp)
                        val datasheet = listOf(snap.gpuName, snap.gpuVulkan).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                        if (datasheet.isNotEmpty()) MetaSub(datasheet)
                    }
                }
                BandFooter(updString(snap.timestamp), "${cfg.traceWindowS}s window", stale)
            }
            WidgetKind.BENCH -> {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("DEVICEINSIGHT · BENCH", style = Caliper.type.meta, color = Caliper.colors.ink60)
                    Spacer(Modifier.width(6.dp))
                    Spacer(Modifier.weight(1f))
                    Text(if (stale) "SIGNAL LOST" else "LIVE", style = Caliper.type.meta, color = if (stale) Caliper.colors.fault else Caliper.colors.ink40)
                    Box(Modifier.padding(start = 6.dp).size(6.dp).background(if (stale) Caliper.colors.ink40 else Caliper.colors.accent))
                }
                Spacer(Modifier.height(8.dp))
                cfg.compactChannels.take(4).forEach { chId ->
                    val (label, value) = benchTileData(chId, snap)
                    SpecRow("$chId · $label", value)
                }
                BandFooter(updString(snap.timestamp), "${cfg.compactChannels.size} channels", stale)
            }
        }
    }
}

@Composable
private fun BandHeader(chId: String, name: String, status: String, ch: Channel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 12.dp).background(Caliper.colors.channel(ch)))
        Spacer(Modifier.width(6.dp))
        Text("$chId · $name", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.width(6.dp))
        Text(status, style = Caliper.type.meta, color = Caliper.colors.ink40)
    }
}

@Composable
private fun HeroLine(text: String, stale: Boolean) {
    Text(text, style = Caliper.type.readoutL, color = if (stale) Caliper.colors.ink40 else Caliper.colors.ink)
}

@Composable
private fun MetaSub(text: String) {
    Text(text, style = Caliper.type.meta, color = Caliper.colors.ink60)
}

@Composable
private fun BandFooter(upd: String, window: String, stale: Boolean) {
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth()) {
        Text(if (stale) "upd SIGNAL LOST" else "upd $upd", style = Caliper.type.meta, color = if (stale) Caliper.colors.fault else Caliper.colors.ink40)
        Spacer(Modifier.weight(1f))
        Text(window, style = Caliper.type.meta, color = Caliper.colors.ink40)
    }
}

/** polyline spark from real history — never ScopeTrace, never a fake curve */
@Composable
private fun SparkCanvas(values: List<Float>, color: Color, height: androidx.compose.ui.unit.Dp) {
    if (values.isEmpty()) return
    val hairline = Caliper.colors.hairline
    Canvas(Modifier.fillMaxWidth().height(height)) {
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val max = values.maxOrNull()?.takeIf { it > 0.001f } ?: 1f
        // graph-paper grid
        for (i in 1..3) {
            val x = size.width * i / 4f
            drawLine(hairline, Offset(x, 0f), Offset(x, size.height), 1f)
        }
        drawLine(hairline, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), 1f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val y = size.height * (1f - (v / max).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
            if (i == 0) path.moveTo(0f, y) else path.lineTo(i * step, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    }
}

/** cadastral composition bar — SOLID/DIAGONAL/CROSS/VERTICAL patterns from memComposition */
@Composable
private fun HatchCanvas(segs: List<MemSeg>, height: androidx.compose.ui.unit.Dp) {
    val c = Caliper.colors
    val list = segs.ifEmpty { listOf(MemSeg(1f, HatchPattern.NONE, "")) }
    Canvas(Modifier.fillMaxWidth().height(height).border(1.dp, c.hairline)) {
        var x = 0f
        list.forEach { seg ->
            val w = size.width * seg.fraction.coerceIn(0f, 1f)
            val col = when (seg.channelId) {
                "CH-01" -> c.channel(Channels.CPU)
                "CH-02" -> c.channel(Channels.MEMORY)
                "CH-03" -> c.channel(Channels.NETWORK)
                "CH-04" -> c.channel(Channels.POWER)
                "CH-05" -> c.channel(Channels.STORAGE)
                "CH-06" -> c.channel(Channels.GPU)
                else -> c.ink40
            }
            val rectW = w.coerceAtLeast(0f)
            drawRect(col.copy(alpha = if (seg.pattern == HatchPattern.NONE) 0f else 0.85f),
                topLeft = Offset(x, 0f), size = androidx.compose.ui.geometry.Size(rectW, size.height))
            when (seg.pattern) {
                HatchPattern.DIAGONAL, HatchPattern.CROSS -> {
                    val gap = 5.dp.toPx()
                    var lx = x
                    while (lx < x + rectW) {
                        drawLine(col, Offset(lx, size.height), Offset(lx + size.height, 0f), 1.2f)
                        if (seg.pattern == HatchPattern.CROSS) drawLine(col, Offset(lx, 0f), Offset(lx + size.height, size.height), 1.2f)
                        lx += gap
                    }
                }
                HatchPattern.VERTICAL -> {
                    val gap = 5.dp.toPx()
                    var lx = x
                    while (lx < x + rectW) {
                        drawLine(col, Offset(lx, 0f), Offset(lx, size.height), 1.2f)
                        lx += gap
                    }
                }
                else -> {}
            }
            x += rectW
        }
    }
}

/** fuel gauge strip: filled fraction + needle tick (+ charge accent) */
@Composable
private fun FuelCanvas(pct: Float, color: Color, charging: Boolean, height: androidx.compose.ui.unit.Dp) {
    val c = Caliper.colors
    Canvas(Modifier.fillMaxWidth().height(height).border(1.dp, c.hairline)) {
        val f = pct.coerceIn(0f, 1f)
        drawRect(color.copy(alpha = 0.85f), size = androidx.compose.ui.geometry.Size(size.width * f, size.height))
        val nx = size.width * f
        drawLine(c.ink, Offset(nx, 0f), Offset(nx, size.height), 2.dp.toPx())
        if (charging) drawLine(c.accent, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 1.dp.toPx())
    }
}

private fun compositionLabel(snap: BenchSnapshot): String {
    var crossUsed = false
    return snap.memComposition.filter { it.fraction >= 0.02f }.joinToString(" · ") { seg ->
        when (seg.pattern) {
            HatchPattern.SOLID -> "active"
            HatchPattern.DIAGONAL -> "cached"
            HatchPattern.CROSS -> { val l = if (!crossUsed && snap.zramGb > 0f) "zram" else "swap"; crossUsed = true; l }
            else -> "free"
        }
    }
}

private fun benchTileData(chId: String, snap: BenchSnapshot): Pair<String, String> = when (chId) {
    "CH-01" -> "CPU" to Fmt.pct(snap.cpuPct, 1)
    "CH-02" -> "MEMORY" to if (snap.memTotalGb > 0) String.format(java.util.Locale.US, "%.1f GB", snap.memUsedGb) else "—"
    "CH-03" -> "NETWORK" to if (snap.netDown > 0 || snap.netUp > 0) "↓ ${Fmt.rate(snap.netDown)} ↑ ${Fmt.rate(snap.netUp)}" else "—"
    "CH-04" -> "POWER" to if (snap.batteryPresent) Fmt.wattsSigned(snap.watts) else "NOT FITTED"
    "CH-05" -> "STORAGE" to if (snap.stoTotalGb > 0) String.format(java.util.Locale.US, "%.1f / %.0f GB", snap.stoUsedGb, snap.stoTotalGb) else "—"
    "CH-06" -> "GPU" to if (snap.gpuFitted) "${snap.gpuPct?.toInt() ?: 0}% · ${snap.gpuMHz ?: 0} MHz" else "NOT FITTED"
    else -> chId to "—"
}

private fun benchDemoSnapshot(kind: WidgetKind): BenchSnapshot {
    return BenchSnapshot(
        timestamp = System.currentTimeMillis(),
        cpuPct = 38.4f,
        freqGHz = 2.4f,
        tempC = 46f,
        memUsedGb = 6.8f,
        memTotalGb = 12f,
        batteryPct = 0.72f,
        watts = -1.2f,
        charging = false,
        gpuFitted = false,
        gpuName = "Preview GPU",
        cpuHist = listOf(10f, 20f, 35f, 38f, 30f, 25f),
        memHist = listOf(50f, 55f, 52f, 48f),
        wattHist = listOf(-1.0f, -1.2f, -0.8f, -1.1f),
        batteryPresent = true
    )
}
