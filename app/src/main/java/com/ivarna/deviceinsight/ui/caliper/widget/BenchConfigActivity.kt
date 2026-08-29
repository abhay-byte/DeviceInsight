package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
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
                WidgetTargetRegistry.invalidate()
                val sample = WidgetSnapshotCoordinator.resolveInitial(this@BenchConfigActivity).snapshot
                BenchUpdater.publishAndForceUpdate(
                    this@BenchConfigActivity,
                    sample,
                    WidgetSnapshotSource.ON_DEMAND
                )
            } catch (t: Throwable) {
                Log.e("DeviceInsightWidget", "CONFIG_SAVE_FAIL appWidgetId=$appWidgetId", t)
                setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                finish()
                return@launch
            }
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
    val published by WidgetSnapshotCoordinator.latest.collectAsState(initial = null)
    var snap by remember { mutableStateOf(BenchDemo.previewSnapshot()) }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        snap = WidgetSnapshotCoordinator.resolveInitial(context).snapshot
    }
    LaunchedEffect(published) { published?.snapshot?.let { snap = it } }

    // page chrome follows the APP theme — the widget's media pick only affects the preview render
    CaliperTheme(medium = systemMedium) {
        Column(
            modifier = Modifier.fillMaxSize().background(Caliper.colors.surface).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            ScreenHeader("Calibrate", "${kind.name} instrument")
            Spacer(Modifier.height(12.dp))

            PreviewPanel(kind, cfg, snap, appWidgetId, published?.source ?: WidgetSnapshotSource.ON_DEMAND)
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

@Composable
private fun PreviewPanel(
    kind: WidgetKind,
    cfg: BenchConfig,
    snap: BenchSnapshot,
    appWidgetId: Int,
    snapshotSource: WidgetSnapshotSource
) {
    val context = LocalContext.current
    var exactSize by remember(appWidgetId) {
        mutableStateOf(WidgetSizeResolver.fromOptions(
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            } else null
        ))
    }
    LaunchedEffect(appWidgetId) {
        exactSize = WidgetSizeResolver.resolve(context, appWidgetId)
    }
    val tier = Tier.of(exactSize.width.value.toInt(), exactSize.height.value.toInt())
    PanelCard(title = "PREVIEW", status = {
            Text("${kind.name} · T${tier.ordinal + 1} · ${exactSize.width.value.toInt()}×${exactSize.height.value.toInt()}dp", style = Caliper.type.meta, color = Caliper.colors.ink40)
    }) {
        LiveWidgetPreview(
            kind = kind,
            config = cfg,
            snapshot = snap,
            exactSize = exactSize,
            appWidgetId = appWidgetId,
            snapshotSource = snapshotSource,
            modifier = Modifier.fillMaxWidth().aspectRatio(exactSize.width / exactSize.height)
        )
    }
}
