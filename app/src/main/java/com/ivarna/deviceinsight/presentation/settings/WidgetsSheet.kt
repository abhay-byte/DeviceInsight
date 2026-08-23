package com.ivarna.deviceinsight.presentation.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.widget.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WidgetsSheet(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var count by remember { mutableIntStateOf(0) }
    var instruments by remember { mutableStateOf<List<InstrumentInfo>>(emptyList()) }
    var pinSupported by remember { mutableStateOf(true) }

    // Initial load + ON_RESUME refresh (fixes MAJOR 4)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { refreshInstruments(ctx) { c, list -> count = c; instruments = list } }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        pinSupported = AppWidgetManager.getInstance(ctx).isRequestPinAppWidgetSupported
        refreshInstruments(ctx) { c, list -> count = c; instruments = list }
    }

    // Per-kind counts for ADD section
    val perKindCounts = remember(instruments) {
        WidgetKind.entries.associateWith { k -> instruments.count { it.kind == k } }
    }

    // F1 (plan §2.0): only scroller on the INSTRUMENTS branch — parent Column in
    // SettingsScreen is bounded (no verticalScroll), so this is legal. ScreenHeader
    // already pads 16dp; no extra sheet padding to avoid a double inset.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("№ 05.1 — INSTRUMENTS", "Widgets.", "place on the bench · inspect the line")
        Spacer(Modifier.height(12.dp))

        // 01 ADD
        Text("01 ADD", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(8.dp))
        WidgetKind.entries.forEach { kind ->
            val placedN = perKindCounts[kind] ?: 0
            val statusText = if (placedN == 0) "NOT PLACED" else "PLACED ×$placedN"
            PanelCard(title = kind.name, status = { Text(statusText, style = Caliper.type.meta, color = Caliper.colors.ink40) }) {
                Text(when (kind) {
                    WidgetKind.SCOPE -> "SCOPE — live CPU load"
                    WidgetKind.STACK -> "STACK — memory composition"
                    WidgetKind.FUEL -> "FUEL — wattage, fuel gauge"
                    WidgetKind.RASTER -> "RASTER — GPU load and clocks"
                    WidgetKind.BENCH -> "BENCH — all channels"
                }, style = Caliper.type.dataS, color = Caliper.colors.ink)
                Spacer(Modifier.height(8.dp))
                HardKey("ADD TO HOME SCREEN", variant = HardKeyVariant.SECONDARY, onClick = {
                    requestPin(ctx, kind, scope) { refreshInstruments(ctx) { c, list -> count = c; instruments = list } }
                })
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!pinSupported) {
            MarginNote(message = "this launcher does not accept pin requests — use the manual path below", title = "NOTE")
            Spacer(Modifier.height(8.dp))
        }
        // Manual path always
        Text("MANUAL PATH", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(6.dp))
        SpecRow("01", "long-press home", Modifier.padding(vertical = 2.dp))
        SpecRow("02", "Widgets", Modifier.padding(vertical = 2.dp))
        SpecRow("03", "DeviceInsight", Modifier.padding(vertical = 2.dp))
        SpecRow("04", "pick SCOPE · STACK · FUEL · RASTER · BENCH", Modifier.padding(vertical = 2.dp))
        MarginNote(message = "existing Dual placements now show STACK (memory), not CPU+MEM.", title = "NOTE")
        Spacer(Modifier.height(16.dp))
        DoubleRule()
        Spacer(Modifier.height(16.dp))

        // 02 ACTIVE
        Text("02 ACTIVE", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(8.dp))
        if (instruments.isEmpty()) {
            PanelCard(title = "NO SIGNAL") {
                Text("no instruments on the bench", style = Caliper.type.dataS, color = Caliper.colors.ink60)
                Spacer(Modifier.height(8.dp))
                HardKey("ADD TO HOME SCREEN", variant = HardKeyVariant.PRIMARY, onClick = { requestPin(ctx, WidgetKind.SCOPE, scope) { scope.launch { delay(1200); refreshInstruments(ctx) { c, list -> count = c; instruments = list } } } })
            }
        } else {
            instruments.forEach { info ->
                PanelCard(title = info.kind.name, status = { Text("placed", style = Caliper.type.meta, color = Caliper.colors.ink40) }) {
                    Text("${info.kind.name} · ${info.medium.name} · ${info.cadence.name}", style = Caliper.type.dataS, color = Caliper.colors.ink)
                    Text("upd ${info.upd}", style = Caliper.type.meta, color = Caliper.colors.ink40)
                    Spacer(Modifier.height(8.dp))
                    HardKey("CALIBRATE", variant = HardKeyVariant.SECONDARY, onClick = {
                        val intent = Intent(ctx, BenchConfigActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, info.appWidgetId)
                        }
                        ctx.startActivity(intent)
                    })
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text("remove from home screen — the launcher owns the panel.", style = Caliper.type.meta, color = Caliper.colors.ink40)
        }

        Spacer(Modifier.height(16.dp))
        HardKey("← BACK TO SETTINGS", variant = HardKeyVariant.SECONDARY, onClick = onBack)
        EndOfSheet()
    }
}

private data class InstrumentInfo(
    val kind: WidgetKind,
    val medium: com.ivarna.deviceinsight.ui.caliper.Medium,
    val cadence: Cadence,
    val upd: String,
    val appWidgetId: Int
)

private suspend fun refreshInstruments(ctx: Context, onResult: (Int, List<InstrumentInfo>) -> Unit) {
    try {
        val mgr = GlanceAppWidgetManager(ctx)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last
        val upd = if (snap != null) {
            val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            fmt.format(java.util.Date(snap.timestamp))
        } else "—"
        val pairs: List<Pair<Class<out androidx.glance.appwidget.GlanceAppWidget>, WidgetKind>> = listOf(
            ScopeWidget::class.java to WidgetKind.SCOPE,
            StackWidget::class.java to WidgetKind.STACK,
            FuelWidget::class.java to WidgetKind.FUEL,
            RasterWidget::class.java to WidgetKind.RASTER,
            BenchWidgetAll::class.java to WidgetKind.BENCH
        )
        val list = mutableListOf<InstrumentInfo>()
        for ((cls, kind) in pairs) {
            val ids = try { mgr.getGlanceIds(cls) } catch (_: Exception) { emptyList() }
            for (id in ids) {
                val cfg = try { BenchState.config(ctx, id) } catch (_: Exception) { BenchConfig() }
                val awId = try { mgr.getAppWidgetId(id) } catch (_: Exception) { 0 }
                list.add(InstrumentInfo(kind, cfg.medium, cfg.cadence, upd, awId))
            }
        }
        onResult(list.size, list)
    } catch (_: Exception) { onResult(0, emptyList()) }
}

private fun requestPin(ctx: Context, kind: WidgetKind, scope: kotlinx.coroutines.CoroutineScope, onRefresh: suspend () -> Unit) {
    try {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (mgr.isRequestPinAppWidgetSupported) {
            val receiver = when (kind) {
                WidgetKind.SCOPE -> SingleChannelWidgetReceiver::class.java
                WidgetKind.STACK -> DualChannelWidgetReceiver::class.java
                WidgetKind.FUEL -> FuelWidgetReceiver::class.java
                WidgetKind.RASTER -> RasterWidgetReceiver::class.java
                WidgetKind.BENCH -> BenchWidgetReceiver::class.java
            }
            val cn = ComponentName(ctx, receiver)
            mgr.requestPinAppWidget(cn, null, null)
        }
    } catch (_: Exception) { }
    // Primary refresh: delay + ON_RESUME observer will also catch
    scope.launch {
        delay(1200)
        onRefresh()
    }
}

// Backward compat overload for call sites without scope (not used)
private fun requestPin(ctx: Context, kind: WidgetKind) {
    // degrade: no scope, just try pin
    try {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (!mgr.isRequestPinAppWidgetSupported) return
        val receiver = when (kind) {
            WidgetKind.SCOPE -> SingleChannelWidgetReceiver::class.java
            WidgetKind.STACK -> DualChannelWidgetReceiver::class.java
            WidgetKind.FUEL -> FuelWidgetReceiver::class.java
            WidgetKind.RASTER -> RasterWidgetReceiver::class.java
            WidgetKind.BENCH -> BenchWidgetReceiver::class.java
        }
        val cn = ComponentName(ctx, receiver)
        mgr.requestPinAppWidget(cn, null, null)
    } catch (_: Exception) { }
}
