package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.lifecycle.lifecycleScope
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.delay
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
                        appWidgetId = appWidgetId,
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
    appWidgetId: Int,
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
    // real-time preview: re-sample every second while the page is visible — fresh live data
    // when the monitor bus is warm, deterministic demo otherwise (never an empty BUDGET panel)
    var snap by remember {
        mutableStateOf(BenchDemo.previewSnapshot().copy(timestamp = System.currentTimeMillis()))
    }
    LaunchedEffect(Unit) {
        while (true) {
            val now = System.currentTimeMillis()
            val live = GlobalSnapshot.current()
            snap = if (live != null && now - live.timestamp in 0 until 5_000L) live
                   else BenchDemo.previewSnapshot().copy(timestamp = now)
            delay(1_000)
        }
    }

    // page chrome follows the APP theme — the widget's media pick only affects the preview render
    CaliperTheme(medium = systemMedium) {
        Column(
            modifier = Modifier.fillMaxSize().background(Caliper.colors.surface).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            ScreenHeader("№ 05.2 — CALIBRATE", "Calibrate.", "${kind.name} instrument · configure the bench")
            Spacer(Modifier.height(12.dp))

            PreviewPanel(kind, cfg, snap, appWidgetId)
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


// --------------- Preview: REAL Glance pipeline (DI-WF-001 F2) ---------------
// Renders the exact InstrumentBody the launcher ships - no Compose facsimile, no drift.
// Honors live cfg edits (media pick, cadence, watt hero, window, compact channels).

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
private fun PreviewPanel(kind: WidgetKind, cfg: BenchConfig, snap: BenchSnapshot, appWidgetId: Int) {
    val context = LocalContext.current
    // preview at the widget's ACTUAL placed footprint (launcher options) — same tier the
    // Exact-mode widget renders, so bands (thermal/rail) match what the user placed
    val placed = remember(appWidgetId) {
        val opts = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
            AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId) else null
        val wDp = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)?.takeIf { it > 0 } ?: 280
        val hDp = opts?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)?.takeIf { it > 0 } ?: 140
        Tier.of(wDp, hDp)
    }
    var rv by remember { mutableStateOf<android.widget.RemoteViews?>(null) }
    LaunchedEffect(kind, placed, cfg, snap) {
        val medium = try { resolvedMedium(context, cfg) } catch (_: Exception) { Medium.PAPER }
        rv = try {
            GlanceRemoteViews().compose(context, DpSize(placed.wDp.dp, placed.hDp.dp)) {
                InstrumentBody(kind, placed, medium, cfg, snap, calibrating = false, awId = -1)
            }.remoteViews
        } catch (_: Exception) { null }
    }
    PanelCard(title = "PREVIEW", status = {
        Text("${kind.name} · T${placed.ordinal + 1}", style = Caliper.type.meta, color = Caliper.colors.ink40)
    }) {
        val preview = rv
        if (preview == null) {
            Text("RENDERING...", style = Caliper.type.meta, color = Caliper.colors.ink40)
        } else {
            AndroidView(
                factory = { c -> android.widget.FrameLayout(c) },
                update = { host ->
                    host.removeAllViews()
                    runCatching {
                        val v = preview.apply(host.context, host)
                        host.addView(
                            v,
                            android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(placed.wDp.toFloat() / placed.hDp.toFloat())
            )
        }
    }
}
