package com.ivarna.deviceinsight.presentation.settings

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ivarna.deviceinsight.R
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
    var selected by remember { mutableStateOf(WidgetKind.SCOPE) }

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

    val perKindCounts = remember(instruments) {
        WidgetKind.entries.associateWith { k -> instruments.count { it.kind == k } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // BACK is the first control — not after the list
        HardKey("← BACK", variant = HardKeyVariant.SECONDARY,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), onClick = onBack)
        ScreenHeader("№ 05.1 — INSTRUMENTS", "Widgets.", "place on the bench · inspect the line")
        Spacer(Modifier.height(12.dp))

        Text("01 ADD", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(8.dp))

        // compact instrument strip — five mini tiles with paper previews
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WidgetKind.entries.forEach { kind ->
                val placedN = perKindCounts[kind] ?: 0
                val sel = kind == selected
                Column(
                    Modifier
                        .weight(1f)
                        .border(1.dp, if (sel) Caliper.colors.ink else Caliper.colors.ink40)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selected = kind }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(previewResFor(kind)),
                        contentDescription = "${kind.name} preview",
                        modifier = Modifier.fillMaxWidth().height(72.dp).alpha(if (sel) 1f else 0.7f),
                        contentScale = ContentScale.FillWidth
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(kind.name, style = Caliper.type.meta, color = if (sel) Caliper.colors.ink else Caliper.colors.ink60)
                    Text(if (placedN == 0) "NOT PLACED" else "×$placedN", style = Caliper.type.meta, color = Caliper.colors.ink40)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        PanelCard(title = selected.name, status = {
            Text(
                (perKindCounts[selected] ?: 0).let { n -> if (n == 0) "NOT PLACED" else "PLACED ×$n" },
                style = Caliper.type.meta, color = Caliper.colors.ink40
            )
        }) {
            Text(personality(selected), style = Caliper.type.dataS, color = Caliper.colors.ink)
            Spacer(Modifier.height(8.dp))
            HardKey("ADD TO HOME SCREEN", variant = HardKeyVariant.SECONDARY, onClick = {
                requestPin(ctx, selected, scope) { refreshInstruments(ctx) { c, list -> count = c; instruments = list } }
            })
        }

        if (!pinSupported) {
            MarginNote(message = "this launcher does not accept pin requests — long-press home → Widgets → DeviceInsight", title = "NOTE")
        } else if (ctx.findActivity() == null) {
            MarginNote(message = "pinning needs a regular activity context — reopen settings from the app", title = "NOTE")
        }
        MarginNote(message = "manual path · 01 long-press home · 02 Widgets · 03 DeviceInsight · 04 pick kind", title = "MANUAL")
        Spacer(Modifier.height(16.dp))
        DoubleRule()
        Spacer(Modifier.height(16.dp))

        // 02 ACTIVE — for the selected kind, short rows
        Text("02 ACTIVE — ${selected.name}", style = Caliper.type.meta, color = Caliper.colors.ink60)
        Spacer(Modifier.height(8.dp))
        val active = instruments.filter { it.kind == selected }
        if (active.isEmpty()) {
            Text("no ${selected.name} on the bench", style = Caliper.type.dataS, color = Caliper.colors.ink60,
                modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            active.forEach { info ->
                PanelCard(title = info.medium.name, status = {
                    Text(info.cadence.name, style = Caliper.type.meta, color = Caliper.colors.ink40)
                }) {
                    Text("upd ${info.upd}", style = Caliper.type.meta, color = Caliper.colors.ink40)
                    Spacer(Modifier.height(6.dp))
                    HardKey("CALIBRATE", variant = HardKeyVariant.SECONDARY, onClick = {
                        val intent = Intent(ctx, BenchConfigActivity::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, info.appWidgetId)
                        }
                        ctx.startActivity(intent)
                    })
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (instruments.isNotEmpty()) {
            Text("remove from home screen — the launcher owns the panel.", style = Caliper.type.meta,
                color = Caliper.colors.ink40, modifier = Modifier.padding(horizontal = 16.dp))
        }
        EndOfSheet()
    }
}

private fun personality(kind: WidgetKind): String = when (kind) {
    WidgetKind.SCOPE -> "SCOPE — live CPU load"
    WidgetKind.STACK -> "STACK — memory composition"
    WidgetKind.FUEL -> "FUEL — wattage, fuel gauge"
    WidgetKind.RASTER -> "RASTER — GPU load and clocks"
    WidgetKind.BENCH -> "BENCH — all channels"
}

// real captured widget renders (night-qualified: carbon PNGs shown in dark mode)
private fun previewResFor(kind: WidgetKind): Int = when (kind) {
    WidgetKind.SCOPE -> R.drawable.preview_scope_280x140
    WidgetKind.STACK -> R.drawable.preview_stack_280x140
    WidgetKind.FUEL -> R.drawable.preview_fuel_280x140
    WidgetKind.RASTER -> R.drawable.preview_raster_280x140
    WidgetKind.BENCH -> R.drawable.preview_bench_280x280
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

internal fun Context.findActivity(): android.app.Activity? {
    var c: Context = this
    while (c is android.content.ContextWrapper) {
        if (c is android.app.Activity) return c
        c = c.baseContext
    }
    return null
}

private fun requestPin(ctx: Context, kind: WidgetKind, scope: kotlinx.coroutines.CoroutineScope, onRefresh: suspend () -> Unit) {
    try {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (!mgr.isRequestPinAppWidgetSupported) return
        val activity = ctx.findActivity() ?: return
        val receiver = when (kind) {
            WidgetKind.SCOPE -> SingleChannelWidgetReceiver::class.java
            WidgetKind.STACK -> DualChannelWidgetReceiver::class.java
            WidgetKind.FUEL -> FuelWidgetReceiver::class.java
            WidgetKind.RASTER -> RasterWidgetReceiver::class.java
            WidgetKind.BENCH -> BenchWidgetReceiver::class.java
        }
        val cn = ComponentName(ctx, receiver)
        // paper preview bitmap, downscaled — binder extras must stay small or OEMs drop the sheet
        val raw = BitmapFactory.decodeResource(ctx.resources, previewResFor(kind)) ?: return
        val preview = if (raw.width <= 256 && raw.height <= 256) raw else {
            val scale = 256f / maxOf(raw.width, raw.height)
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * scale).toInt().coerceAtMost(256),
                (raw.height * scale).toInt().coerceAtMost(256),
                true
            )
        }
        val extras = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
        val success = PendingIntent.getBroadcast(
            activity, kind.ordinal,
            Intent(activity, PinSuccessReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mgr.requestPinAppWidget(cn, extras, success)
    } catch (_: Exception) { }
    // Primary refresh: delay + ON_RESUME observer will also catch
    scope.launch {
        delay(1200)
        onRefresh()
    }
}
