package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BenchConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    // W10: medium resolved off the main thread before setContent — no runBlocking
    private val initialMedium = androidx.compose.runtime.mutableStateOf(Medium.PAPER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val kind = resolveKind()

        setResult(RESULT_CANCELED)

        lifecycleScope.launch {
            initialMedium.value = try {
                this@BenchConfigActivity.mediumFlow.first() ?: Medium.PAPER
            } catch (_: Exception) { Medium.PAPER }
        }

        setContent {
            CaliperTheme(medium = initialMedium.value) {
                BenchConfigScreen(
                    kind = kind,
                    onSave = { cfg -> saveConfig(cfg) },
                    onSkip = { saveConfig(BenchConfig()) },
                    onCancel = { setResult(RESULT_CANCELED); finish() }
                )
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

@Composable
private fun BenchConfigScreen(
    kind: WidgetKind,
    onSave: (BenchConfig) -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    var medium by remember { mutableStateOf(Medium.PAPER) }
    var followSystem by remember { mutableStateOf(true) }
    var cadence by remember { mutableStateOf(Cadence.AMBIENT) }
    var traceWindow by remember { mutableStateOf(60) }
    var wattHero by remember { mutableStateOf(true) }
    var compact by remember { mutableStateOf(listOf("CH-01", "CH-02", "CH-04", "CH-03")) }

    val cfg = BenchConfig(medium, followSystem, cadence, traceWindow, wattHero, compact)
    val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: benchDemoSnapshot(kind)

    Column(
        modifier = Modifier.fillMaxSize().background(Caliper.colors.surface).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        ScreenHeader("№ 05.2 — CALIBRATE", "Calibrate.", "${kind.name} instrument · configure the bench")
        Spacer(Modifier.height(12.dp))

        // Preview
        PreviewPanel(kind, cfg, snap)
        Spacer(Modifier.height(16.dp))

        // Media — mini panels swatches pattern
        Text("MEDIA", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(6.dp))
        SegKey(options = listOf(Medium.PAPER, Medium.CARBON, Medium.BLUEPRINT), selected = medium, onSelect = { medium = it }, labelFor = { it.name })
        Spacer(Modifier.height(8.dp))
        DipSwitch(checked = followSystem, onCheckedChange = { followSystem = it }, label = "follow system")
        Spacer(Modifier.height(12.dp))

        // Cadence
        Text("CADENCE", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(6.dp))
        SegKey(options = Cadence.entries.toList(), selected = cadence, onSelect = { cadence = it }, labelFor = { it.name })
        Spacer(Modifier.height(12.dp))

        // Kind-specific
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

@Composable
private fun PreviewPanel(kind: WidgetKind, cfg: BenchConfig, snap: BenchSnapshot) {
    PanelCard(title = "PREVIEW", status = { Text(kind.name, style = Caliper.type.meta, color = Caliper.colors.ink40) }) {
        // W10: real CALIPER Compose instruments (WI §8) — never Glance composables in the Activity
        when (kind) {
            WidgetKind.SCOPE -> {
                OdometerText(Fmt.pct(snap.cpuPct, 1), style = Caliper.type.readoutL, color = Caliper.colors.ink)
                Text("CH-01 · CPU", style = Caliper.type.meta, color = Caliper.colors.ink60)
                Spacer(Modifier.height(8.dp))
                ScopeTrace(
                    values = snap.cpuHist,
                    channel = Channels.CPU,
                    windowLabel = "${cfg.traceWindowS}s",
                    height = 96.dp
                )
            }
            WidgetKind.STACK -> {
                OdometerText(
                    if (snap.memTotalGb > 0) String.format(java.util.Locale.US, "%.1f / %.0f GB", snap.memUsedGb, snap.memTotalGb) else "—",
                    style = Caliper.type.readoutL, color = Caliper.colors.ink
                )
                Text("CH-02 · MEMORY", style = Caliper.type.meta, color = Caliper.colors.ink60)
                Spacer(Modifier.height(8.dp))
                val denom = (snap.memTotalGb * 1e9f).toLong().coerceAtLeast(1L)
                HatchBar(segments = memPreviewSegments(snap, denom))
            }
            WidgetKind.FUEL -> {
                OdometerText(
                    if (cfg.wattHero) Fmt.wattsSigned(snap.watts) else Fmt.pct(snap.batteryPct * 100, 0),
                    style = Caliper.type.readoutL, color = Caliper.colors.ink
                )
                Text("CH-04 · POWER", style = Caliper.type.meta, color = Caliper.colors.ink60)
                Spacer(Modifier.height(8.dp))
                LinearGauge(
                    fraction = snap.batteryPct.coerceIn(0f, 1f),
                    voltage = if (snap.voltage > 0) "${(snap.voltage * 1000).toInt()} mV" else null,
                    charging = snap.charging
                )
            }
            WidgetKind.RASTER -> {
                if (!snap.gpuFitted) {
                    Text("NOT FITTED", style = Caliper.type.dataM, color = Caliper.colors.ink40)
                    if (snap.gpuName.isNotBlank()) Text(snap.gpuName, style = Caliper.type.meta, color = Caliper.colors.ink60)
                } else if (snap.gpuRootLocked) {
                    Text("CHANNEL LOCKED", style = Caliper.type.dataM, color = Caliper.colors.fault)
                } else {
                    OdometerText("${snap.gpuPct?.toInt() ?: 0}% · ${snap.gpuMHz ?: 0} MHz", style = Caliper.type.readoutL, color = Caliper.colors.ink)
                    Text("CH-06 · GPU", style = Caliper.type.meta, color = Caliper.colors.ink60)
                    Spacer(Modifier.height(8.dp))
                    LinearGauge(fraction = (snap.gpuPct ?: 0f).coerceIn(0f, 1f))
                }
            }
            WidgetKind.BENCH -> {
                cfg.compactChannels.take(4).forEach { chId ->
                    SpecRow(chId, when (chId) {
                        "CH-01" -> Fmt.pct(snap.cpuPct, 1)
                        "CH-02" -> if (snap.memTotalGb > 0) String.format(java.util.Locale.US, "%.1f GB", snap.memUsedGb) else "—"
                        "CH-04" -> Fmt.wattsSigned(snap.watts)
                        else -> "—"
                    })
                }
            }
        }
    }
}

// STACK preview: memComposition fractions → HatchBar segments (bytes scaled to total for HatchBar's math)
@Composable
private fun memPreviewSegments(snap: BenchSnapshot, totalBytes: Long): List<HatchSegment> {
    val c = Caliper.colors
    fun colorFor(id: String) = when (id) {
        "CH-01" -> c.channel(Channels.CPU)
        "CH-02" -> c.channel(Channels.MEMORY)
        "CH-03" -> c.channel(Channels.NETWORK)
        "CH-04" -> c.channel(Channels.POWER)
        "CH-05" -> c.channel(Channels.STORAGE)
        "CH-06" -> c.channel(Channels.GPU)
        else -> c.ink40
    }
    return snap.memComposition.map { seg ->
        val label = when (seg.pattern) {
            HatchPattern.SOLID -> "active"
            HatchPattern.DIAGONAL -> "cached"
            HatchPattern.CROSS -> "zram/swap"
            else -> "free"
        }
        HatchSegment(
            label = label,
            bytes = (seg.fraction * totalBytes).toLong(),
            color = colorFor(seg.channelId),
            pattern = seg.pattern
        )
    }.ifEmpty { listOf(HatchSegment("free", totalBytes, c.ink40, HatchPattern.NONE)) }
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
