package com.ivarna.deviceinsight.ui.caliper.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

val ROUTE = ActionParameters.Key<String>("di_route")
val APPWIDGET_ID = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)

private fun open(route: String) = actionStartActivity<MainActivity>(parameters = actionParametersOf(ROUTE to route))

// root tap → official configure activity for THIS widget id
private fun openConfig(appWidgetId: Int) =
    actionStartActivity<BenchConfigActivity>(parameters = actionParametersOf(APPWIDGET_ID to appWidgetId))

private val T1Size = DpSize(140.dp, 140.dp)
private val T2Size = DpSize(280.dp, 140.dp)
private val T3Size = DpSize(280.dp, 210.dp)
private val T4Size = DpSize(280.dp, 280.dp)
private val T5Size = DpSize(350.dp, 280.dp)

private val AllSizes = setOf(T1Size, T2Size, T3Size, T4Size, T5Size)
private val BenchSizes = setOf(T2Size, T3Size, T4Size, T5Size)
private val Responsive = SizeMode.Responsive(AllSizes)
private val ResponsiveBench = SizeMode.Responsive(BenchSizes)

private fun tierForSize(w: Int, h: Int): Tier = Tier.of(w, h)

internal fun updString(ts: Long): String {
    if (ts == 0L) return "—"
    return SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(ts))
}

// Xh Ym remaining when >=60, N min remaining otherwise; null hides the subline part
internal fun remainingText(min: Int): String? = when {
    min <= 0 -> null
    min >= 60 -> String.format(Locale.US, "%dh %dm remaining", min / 60, min % 60)
    else -> "$min min remaining"
}

// ─────────────── BenchUpdater (full) ───────────────

object BenchUpdater {
    private val widgets: List<GlanceAppWidget> = listOf(ScopeWidget(), StackWidget(), FuelWidget(), RasterWidget(), BenchWidgetAll())
    internal val lastPush = ConcurrentHashMap<String, Long>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var cachedIds: Map<String, List<GlanceId>> = emptyMap()
    @Volatile private var lastIdFetch: Long = 0L

    fun nudge(context: Context) {
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: return
        // Also consider fallback sampling? Global is authoritative when alive
        val now = System.currentTimeMillis()
        val last = lastPush["nudge"] ?: 0L
        if (now - last < 900) return
        lastPush["nudge"] = now
        scope.launch {
            val mgr = GlanceAppWidgetManager(context)
            // ponytail: cache ids — binder at most ~1Hz overall, skip mgr call unless widget due
            val needFetch = now - lastIdFetch > 30_000 || cachedIds.isEmpty()
            val idsMap: Map<String, List<GlanceId>> = if (needFetch) {
                val m = mutableMapOf<String, List<GlanceId>>()
                widgets.forEach { w ->
                    try {
                        val ids = mgr.getGlanceIds(w::class.java)
                        m[w::class.java.name] = ids
                    } catch (e: Exception) {
                        // binder hiccup — keep the previous id list instead of caching an empty (30 s widget freeze)
                        cachedIds[w::class.java.name]?.takeIf { it.isNotEmpty() }?.let { m[w::class.java.name] = it }
                        Log.w("BenchUpdater", "getGlanceIds failed for ${w::class.java.simpleName}", e)
                    }
                }
                cachedIds = m; lastIdFetch = now; m
            } else cachedIds

            widgets.forEach { w ->
                val ids = idsMap[w::class.java.name] ?: emptyList()
                ids.forEach { id ->
                    try {
                        val cfg = BenchState.config(context, id)
                        val due = cadenceMs(cfg, snap)
                        val key = id.toString()
                        val lp = lastPush[key] ?: 0L
                        val pulse = (snap.charging || snap.warning()) && now - lp > 1_000
                        if (now - lp >= due || pulse) {
                            w.update(context, id)
                            lastPush[key] = now
                        }
                    } catch (e: Exception) {
                        Log.w("BenchUpdater", "update failed id=$id", e)
                    }
                }
            }
        }
    }

    fun evict(appWidgetId: Int) {
        lastPush.remove(appWidgetId.toString())
    }
}

// ─────────────── BandBitmap (SYNC) ───────────────

@Composable
fun BandBitmap(
    stateKey: String,
    band: String,
    tier: Tier,
    medium: Medium,
    bandHeightDp: Int,
    contentDescription: String,
    modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
    bitmapWidthDp: Int? = null,
    body: (Canvas, Float, Float, Float) -> Unit
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val wPx = (((bitmapWidthDp ?: (tier.wDp - 24))) * density).toInt().coerceAtLeast(8)
    val hPx = (bandHeightDp * density).toInt().coerceAtLeast(8)
    val key = "$stateKey|$band|$tier|$medium|$bandHeightDp|$wPx"
    val bmp = renderSync(key, wPx, hPx) { c ->
        body(c, wPx.toFloat(), hPx.toFloat(), density)
    }
    val sizeMod = if (bitmapWidthDp != null) modifier.width(bitmapWidthDp.dp) else modifier
    androidx.glance.Image(
        provider = ImageProvider(bmp),
        contentDescription = contentDescription,
        modifier = sizeMod.height(bandHeightDp.dp)
    )
}

// ─────────────── Panel atoms ───────────────

@Composable
private fun BenchPanel(
    pal: WidgetPalette,
    contentDescription: String,
    configTap: androidx.glance.action.Action,
    content: @Composable androidx.glance.layout.ColumnScope.() -> Unit
) {
    // 4-side hairline — outer 12dp inset prevents OEM corner-clip eating frame
    // Row(defaultWeight)+fillMaxHeight uses Glance 1.1.0 RowScope/ColumnScope member defaultWeight
    // spoken summary lives on the root Box; root tap opens Calibrate (child actions win where present)
    Box(
        modifier = GlanceModifier.fillMaxSize().background(ColorProvider(pal.panel))
            .semantics { this.contentDescription = contentDescription }
            .clickable(configTap),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(pal.hairline)), contentAlignment = Alignment.TopStart) {}
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.Top) {
                Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(ColorProvider(pal.hairline)), contentAlignment = Alignment.TopStart) {}
                Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 9.dp), verticalAlignment = Alignment.Top, horizontalAlignment = Alignment.Start) {
                    content()
                }
                Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(ColorProvider(pal.hairline)), contentAlignment = Alignment.TopStart) {}
            }
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(pal.hairline)), contentAlignment = Alignment.TopStart) {}
        }
    }
}

@Composable
private fun Header(
    pal: WidgetPalette,
    chId: String,
    chName: String,
    status: String,
    locked: Boolean = false,
    ledOn: Boolean = true
) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        // Tick 4dp
        Box(modifier = GlanceModifier.width(4.dp).height(12.dp).background(ColorProvider(pal.channelFor(chId))), contentAlignment = Alignment.CenterStart) { }
        Spacer(GlanceModifier.width(6.dp))
        Text("$chId · $chName", style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
        Spacer(GlanceModifier.width(6.dp))
        Text(status, style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        Spacer(GlanceModifier.defaultWeight())
        if (locked) Text("⚷", style = TextStyle(color = ColorProvider(pal.fault), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        else Box(modifier = GlanceModifier.width(6.dp).height(6.dp).background(ColorProvider(if (ledOn) pal.accent else pal.ink40)), contentAlignment = Alignment.Center) { }
    }
}

@Composable
private fun Footer(
    pal: WidgetPalette,
    snap: BenchSnapshot,
    cfg: BenchConfig,
    windowLabel: String
) {
    val stale = snap.stale(cadenceMs(cfg, snap))
    val updText = if (stale) "SIGNAL LOST" else "upd ${updString(snap.timestamp)}"
    val updColor = if (stale) pal.fault else pal.ink40
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(updText, style = TextStyle(color = ColorProvider(updColor), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        Spacer(GlanceModifier.defaultWeight())
        Text(windowLabel, style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
    }
}

@Composable
private fun Hero(
    text: String,
    pal: WidgetPalette,
    stale: Boolean
) {
    Text(text, style = TextStyle(color = ColorProvider(if (stale) pal.ink40 else pal.ink), fontSize = 30.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
}

@Composable
private fun Subline(text: String, pal: WidgetPalette) {
    Text(text, style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
}

@Composable
private fun ChannelRow(
    chId: String,
    label: String,
    value: String,
    pal: WidgetPalette,
    subline: String? = null
) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text("$chId · $label", style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        Text(value, style = TextStyle(color = ColorProvider(pal.ink), fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
        if (subline != null) Text(subline, style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
    }
}

// BENCH masthead — LIVE/SIGNAL LOST + 6dp LED box + HH:mm clock (WD ● 14:32)
@Composable
private fun BenchMasthead(
    pal: WidgetPalette,
    stale: Boolean,
    calibrating: Boolean,
    snap: BenchSnapshot,
    configTap: androidx.glance.action.Action
) {
    Row(modifier = GlanceModifier.fillMaxWidth().clickable(configTap), verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text("DEVICEINSIGHT · BENCH", style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
        Spacer(GlanceModifier.defaultWeight())
        Text(if (stale) "SIGNAL LOST" else "LIVE", style = TextStyle(color = ColorProvider(if (stale) pal.fault else pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
        Spacer(GlanceModifier.width(6.dp))
        Box(
            modifier = GlanceModifier.width(6.dp).height(6.dp)
                .background(ColorProvider(if (!stale && !calibrating) pal.accent else pal.ink40)),
            contentAlignment = Alignment.Center
        ) { }
        Spacer(GlanceModifier.width(4.dp))
        Text(
            if (snap.timestamp == 0L) "—:—" else SimpleDateFormat("HH:mm", Locale.US).format(Date(snap.timestamp)),
            style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        )
    }
}

// one 14dp sub-instrument bitmap per bench tile (CH-01/03/06 sparks · CH-02/05 hatches · CH-04 gauge)
@Composable
private fun TileBitmap(
    chId: String,
    snap: BenchSnapshot,
    pal: WidgetPalette,
    medium: Medium,
    tier: Tier
) {
    val ctx = LocalContext.current
    when (chId) {
        "CH-01" -> BandBitmap("${snap.cpuHist.contentHash()}|${snap.timestamp}", "tileSpark01", tier, medium, 14, "cpu spark") { c, w, h, d ->
            c.spark(snap.cpuHist, pal, pal.ch01, w, h, d)
        }
        "CH-02" -> BandBitmap("${snap.memUsedGb}|${snap.memTotalGb}|${snap.memComposition.hashCode()}|${snap.timestamp}", "tileHatch02", tier, medium, 14, "memory composition") { c, w, h, _ ->
            val segs = if (snap.memComposition.isNotEmpty()) snap.memComposition
            else listOf(MemSeg(fraction = if (snap.memTotalGb > 0) snap.memUsedGb / snap.memTotalGb else 0f, pattern = HatchPattern.SOLID, channelId = "CH-02"))
            c.hatchBar(ctx, pal, w, h, segs, segs.map { pal.channelFor(it.channelId) })
        }
        "CH-03" -> BandBitmap("${snap.netHist.contentHash()}|${snap.netDown}|${snap.netUp}", "tileSpark03", tier, medium, 14, "network spark") { c, w, h, d ->
            c.spark(snap.netHist, pal, pal.ch03, w, h, d)
        }
        "CH-04" -> BandBitmap("${snap.batteryPct}|${snap.charging}|${snap.timestamp}", "tileFuel04", tier, medium, 14, "fuel gauge") { c, w, h, d ->
            c.fuelGauge(ctx, snap.batteryPct, pal, w, h, d, snap.charging)
        }
        "CH-05" -> BandBitmap("${snap.stoUsedGb}|${snap.stoTotalGb}|${snap.timestamp}", "tileHatch05", tier, medium, 14, "storage hatch") { c, w, h, _ ->
            val frac = if (snap.stoTotalGb > 0) snap.stoUsedGb / snap.stoTotalGb else 0f
            val seg = MemSeg(fraction = frac, pattern = HatchPattern.VERTICAL, channelId = "CH-05")
            c.hatchBar(ctx, pal, w, h, listOf(seg), listOf(pal.ch05))
        }
        "CH-06" -> BandBitmap("${snap.gpuHist.contentHash()}|${snap.gpuPct.hashCode()}", "tileSpark06", tier, medium, 14, "gpu spark") { c, w, h, d ->
            c.spark(snap.gpuHist, pal, pal.ch06, w, h, d)
        }
    }
}

// ─────────────── Widgets ───────────────

class ScopeWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = Responsive
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val medium = resolvedMedium(context, cfg)
        val pal = WidgetPalettes.of(medium)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: BenchSampler.sample(context)
        val placedAt = try { BenchState.placedAt(context, id) } catch (_: Exception) { 0L }
        val calibrating = snap.timestamp == 0L || (placedAt != 0L && System.currentTimeMillis() - placedAt < 6000)
        val awId = try { GlanceAppWidgetManager(context).getAppWidgetId(id) } catch (_: Exception) { AppWidgetManager.INVALID_APPWIDGET_ID }
        provideContent {
            val size = LocalSize.current
            val tier = tierForSize(size.width.value.toInt(), size.height.value.toInt())
            val stale = snap.stale(cadenceMs(cfg, snap))
            val calibratingNow = calibrating
            val desc = "Scope. CPU ${snap.cpuPct.toInt()} percent. Updated ${updString(snap.timestamp)}."
            BenchPanel(pal, desc, openConfig(awId)) {
                Header(pal, "CH-01", "CPU", if (calibratingNow) "CALIBRATING…" else if (stale) "SIGNAL LOST" else "LIVE", ledOn = !stale && !calibratingNow)
                Spacer(GlanceModifier.height(6.dp))
                val heroText = Fmt.pct(snap.cpuPct, 1)
                val stateKey = "${snap.cpuHist.contentHash()}|${snap.timestamp}|${snap.tempC}"
                if (tier == Tier.T1) {
                    // T1 stays stacked — hero + 28dp spark
                    Hero(heroText, pal, stale)
                    Spacer(GlanceModifier.height(4.dp))
                    BandBitmap(stateKey, "spark", tier, medium, 28, "cpu spark") { c, w, h, d ->
                        c.spark(snap.cpuHist, pal, pal.ch01, w, h, d)
                    }
                } else {
                    // WD §4 T2: hero + freq + temp LEFT, gridded trace RIGHT; thermal below full width
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Top) {
                        Column(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.Top) {
                            Hero(heroText, pal, stale)
                            Spacer(GlanceModifier.height(4.dp))
                            if (snap.freqGHz > 0) Subline(Fmt.hz((snap.freqGHz * 1e6).toLong()), pal)
                            if (snap.tempC > 0) Subline(Fmt.temp(snap.tempC), pal)
                        }
                        Spacer(GlanceModifier.width(6.dp))
                        BandBitmap(
                            stateKey, "scope", tier, medium,
                            if (tier >= Tier.T3) 56 else 48, "cpu scope",
                            modifier = GlanceModifier.defaultWeight(),
                            bitmapWidthDp = ((tier.wDp - 24) / 2 - 8).coerceAtLeast(80)
                        ) { c, w, h, d ->
                            if (calibrating) c.calibrating(context, pal, w, h, d, 0.7f)
                            else c.scope(context, snap.cpuHist.map { it }, pal, pal.ch01, w, h, d,
                                showYLabels = true, showAllYLabels = tier >= Tier.T4)
                        }
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    BandBitmap("$stateKey|thermal", "thermal", tier, medium, 8, "thermal ramp") { c, w, h, _ ->
                        c.thermalRamp(pal, w, h, snap.tempC)
                    }
                    snap.governor?.let {
                        if (it.isNotBlank()) {
                            Spacer(GlanceModifier.height(8.dp))
                            Subline(it, pal)
                        }
                    }
                }
                if (tier >= Tier.T3 && snap.cores.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    BandBitmap(stateKey, "rail", tier, medium, (snap.cores.size * 12).coerceAtLeast(24), "core rail") { c, w, _, _ ->
                        c.coreRailRows(context, pal, w, snap.cores, 12f * context.resources.displayMetrics.density)
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
            }
        }
    }
}

class StackWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = Responsive
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val medium = resolvedMedium(context, cfg)
        val pal = WidgetPalettes.of(medium)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: BenchSampler.sample(context)
        val placedAt = try { BenchState.placedAt(context, id) } catch (_: Exception) { 0L }
        val calibrating = snap.timestamp == 0L || (placedAt != 0L && System.currentTimeMillis() - placedAt < 6000)
        // STACK consumers — authoritative via TopConsumersProvider (permission-gated)
        val consumers: List<Consumer> = snap.topConsumers

        val awId = try { GlanceAppWidgetManager(context).getAppWidgetId(id) } catch (_: Exception) { AppWidgetManager.INVALID_APPWIDGET_ID }
        provideContent {
            val size = LocalSize.current
            val tier = tierForSize(size.width.value.toInt(), size.height.value.toInt())
            val stale = snap.stale(cadenceMs(cfg, snap))
            val desc = "Stack. Memory ${snap.memUsedGb.toInt()} of ${snap.memTotalGb.toInt()} gigabytes. Updated ${updString(snap.timestamp)}."
            BenchPanel(pal, desc, openConfig(awId)) {
                // trailing header status is the used % when live; CALIBRATING / SIGNAL LOST win otherwise
                Header(pal, "CH-02", "MEMORY",
                    if (calibrating) "CALIBRATING…" else if (stale) "SIGNAL LOST"
                    else Fmt.pct(if (snap.memTotalGb > 0) snap.memUsedGb / snap.memTotalGb * 100f else 0f, 0),
                    ledOn = !stale && !calibrating)
                Spacer(GlanceModifier.height(6.dp))
                val heroText = if (snap.memTotalGb > 0) String.format(Locale.US, "%.1f / %.0f GB", snap.memUsedGb, snap.memTotalGb) else "—"
                Hero(heroText, pal, stale)
                Spacer(GlanceModifier.height(4.dp))
                val barKey = "${snap.memUsedGb}|${snap.memTotalGb}|${snap.memComposition.hashCode()}|${snap.timestamp}"
                BandBitmap(barKey, "hatchBar", tier, medium, 14, "memory composition") { c, w, h, _ ->
                    val segs = if (snap.memComposition.isNotEmpty()) snap.memComposition else listOf(MemSeg(0f, HatchPattern.NONE, ""))
                    val cols = segs.map { pal.channelFor(it.channelId) }
                    c.hatchBar(context, pal, w, h, segs, cols)
                }
                // labeled cadastral — one text subline from memComposition (skip near-zero segments)
                if (snap.memComposition.isNotEmpty()) {
                    Spacer(GlanceModifier.height(2.dp))
                    var crossUsed = false
                    val compSub = snap.memComposition.filter { it.fraction >= 0.02f }.joinToString(" · ") { seg ->
                        when (seg.pattern) {
                            HatchPattern.SOLID -> "active"
                            HatchPattern.DIAGONAL -> "cached"
                            HatchPattern.CROSS -> { val l = if (!crossUsed && snap.zramGb > 0f) "zram" else "swap"; crossUsed = true; l }
                            else -> "free"
                        }
                    }
                    if (compSub.isNotBlank()) Subline(compSub, pal)
                }
                if (tier >= Tier.T2) {
                    Spacer(GlanceModifier.height(8.dp))
                    val histKey = "${snap.memHist.contentHash()}|${snap.timestamp}"
                    BandBitmap(histKey, "memSpark", tier, medium, 28, "memory pressure spark") { c, w, h, d ->
                        if (calibrating) c.calibrating(context, pal, w, h, d, 0.7f)
                        else c.spark(snap.memHist, pal, pal.ch02, w, h, d)
                    }
                }
                if (tier >= Tier.T3 && consumers.isNotEmpty()) {
                    Spacer(GlanceModifier.height(8.dp))
                    val rows = if (tier >= Tier.T4) 5 else 3
                    consumers.take(rows).forEach { con ->
                        Row(modifier = GlanceModifier.fillMaxWidth().clickable(open("processes")), verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(con.label.take(18), style = TextStyle(color = ColorProvider(pal.ink), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                            Spacer(GlanceModifier.defaultWeight())
                            // RSS not obtainable on API 26+ for 3p — label-only when 0, hide 0 MB fake
                            if (con.rssMb > 0) Text("${con.rssMb} MB", style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                        }
                        Spacer(GlanceModifier.height(2.dp))
                    }
                }
                Spacer(GlanceModifier.height(4.dp))
                val sub = buildString {
                    if (snap.zramGb > 0) append("zram ${String.format(Locale.US, "%.1f GB", snap.zramGb)} · ")
                    append("swap ${if (snap.swapGb > 0) String.format(Locale.US, "%.1f GB", snap.swapGb) else "—"}")
                }
                Subline(sub, pal)
                Spacer(GlanceModifier.defaultWeight())
                Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
            }
        }
    }
}

class FuelWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = Responsive
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val medium = resolvedMedium(context, cfg)
        val pal = WidgetPalettes.of(medium)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: BenchSampler.sample(context)
        val placedAt = try { BenchState.placedAt(context, id) } catch (_: Exception) { 0L }
        val calibrating = snap.timestamp == 0L || (placedAt != 0L && System.currentTimeMillis() - placedAt < 6000)
        val awId = try { GlanceAppWidgetManager(context).getAppWidgetId(id) } catch (_: Exception) { AppWidgetManager.INVALID_APPWIDGET_ID }
        provideContent {
            val size = LocalSize.current
            val tier = tierForSize(size.width.value.toInt(), size.height.value.toInt())
            val stale = snap.stale(cadenceMs(cfg, snap))
            if (!snap.batteryPresent) {
                val desc = "Fuel. Not fitted."
                BenchPanel(pal, desc, openConfig(awId)) {
                    Header(pal, "CH-04", "POWER", "NOT FITTED")
                    Spacer(GlanceModifier.height(12.dp))
                    Text("NOT FITTED", style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 22.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                    Spacer(GlanceModifier.defaultWeight())
                    Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
                }
                return@provideContent
            }
            val desc = "Fuel. ${if (cfg.wattHero) "${snap.watts} watts" else "${(snap.batteryPct * 100).toInt()} percent"}. Updated ${updString(snap.timestamp)}."
            BenchPanel(pal, desc, openConfig(awId)) {
                Header(pal, "CH-04", "POWER", if (snap.charging) "CHARGING" else if (calibrating) "CALIBRATING…" else if (stale) "SIGNAL LOST" else "LIVE", ledOn = !stale && !calibrating)
                Spacer(GlanceModifier.height(6.dp))
                // hero + its own secondary line (watt hero → % beneath; % hero → watts beneath)
                if (cfg.wattHero) {
                    Hero(Fmt.wattsSigned(snap.watts), pal, stale)
                    Spacer(GlanceModifier.height(2.dp))
                    Text(Fmt.pct(snap.batteryPct * 100, 0), style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                } else {
                    Hero(Fmt.pct(snap.batteryPct * 100, 0), pal, stale)
                    Spacer(GlanceModifier.height(2.dp))
                    Subline(Fmt.wattsSigned(snap.watts), pal)
                }
                if (snap.charging) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text("CHARGING", style = TextStyle(color = ColorProvider(pal.accent), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                }
                Spacer(GlanceModifier.height(4.dp))
                BandBitmap("${snap.batteryPct}|${snap.charging}|${snap.timestamp}", "fuel", tier, medium, 14, "fuel gauge") { c, w, h, d ->
                    c.fuelGauge(context, snap.batteryPct, pal, w, h, d, snap.charging)
                }
                if (tier >= Tier.T2) {
                    Spacer(GlanceModifier.height(8.dp))
                    val wattKey = "${snap.wattHist.contentHash()}|${snap.timestamp}"
                    BandBitmap(wattKey, "wattTrace", tier, medium, 32, "watt trace") { c, w, h, d ->
                        if (calibrating) c.calibrating(context, pal, w, h, d, 0.7f)
                        else c.wattTrace(snap.wattHist, pal, w, h, d)
                    }
                }
                Spacer(GlanceModifier.height(4.dp))
                val sub = buildString {
                    if (snap.voltage > 0) append("${(snap.voltage * 1000).toInt()} mV")
                    if (snap.currentMa != 0) { if (isNotEmpty()) append(" · "); append("${snap.currentMa} mA") }
                    remainingText(snap.remainingMin)?.let { if (isNotEmpty()) append(" · "); append(it) }
                }
                if (sub.isNotEmpty()) Subline(sub, pal)
                // T4 datasheet rows — only when tier >= T4 and values real (never fake -1/835)
                if (tier >= Tier.T4) {
                    snap.batteryHealth?.let { h ->
                        Spacer(GlanceModifier.height(2.dp))
                        Subline("health $h", pal)
                    }
                    snap.cycleCount?.let { c ->
                        Spacer(GlanceModifier.height(2.dp))
                        Subline("cycles $c", pal)
                    }
                    snap.designMah?.let { d ->
                        Spacer(GlanceModifier.height(2.dp))
                        Subline("design $d mAh", pal)
                    }
                }
                Spacer(GlanceModifier.defaultWeight())
                Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
            }
        }
    }
}

class RasterWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = Responsive
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val medium = resolvedMedium(context, cfg)
        val pal = WidgetPalettes.of(medium)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: BenchSampler.sample(context)
        val awId = try { GlanceAppWidgetManager(context).getAppWidgetId(id) } catch (_: Exception) { AppWidgetManager.INVALID_APPWIDGET_ID }
        provideContent {
            val size = LocalSize.current
            val tier = tierForSize(size.width.value.toInt(), size.height.value.toInt())
            val stale = snap.stale(cadenceMs(cfg, snap))
            if (!snap.gpuFitted) {
                val desc = "Raster. Not fitted."
                BenchPanel(pal, desc, openConfig(awId)) {
                    Header(pal, "CH-06", "GPU", "NOT FITTED")
                    Spacer(GlanceModifier.height(8.dp))
                    Text("NOT FITTED", style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 18.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                    if (snap.gpuName.isNotBlank()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(snap.gpuName, style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    BandBitmap("locked", "locked", tier, medium, 48, "locked field") { c, w, h, _ ->
                        c.lockedField(context, pal, w, h)
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
                }
                return@provideContent
            }
            if (snap.gpuRootLocked) {
                val desc = "Raster. Channel locked."
                BenchPanel(pal, desc, openConfig(awId)) {
                    Header(pal, "CH-06", "GPU", "CHANNEL LOCKED", locked = true)
                    Spacer(GlanceModifier.height(6.dp))
                    Text("CHANNEL LOCKED", style = TextStyle(color = ColorProvider(pal.fault), fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                    if (snap.gpuName.isNotBlank()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(snap.gpuName, style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                    if (snap.gpuVulkan.isNotBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(snap.gpuVulkan, style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                    if (snap.gpuGles.isNotBlank()) {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(snap.gpuGles, style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Box(modifier = GlanceModifier.fillMaxWidth().clickable(open("calibrate")), contentAlignment = Alignment.Center) {
                        Text("[ GRANT IN APP ]", style = TextStyle(color = ColorProvider(pal.accent), fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
                }
                return@provideContent
            }
            val desc = "Raster. GPU ${snap.gpuPct?.toInt() ?: 0} percent at ${snap.gpuMHz ?: 0} megahertz."
            BenchPanel(pal, desc, openConfig(awId)) {
                Header(pal, "CH-06", "GPU", if (stale) "SIGNAL LOST" else "LIVE", ledOn = !stale)
                Spacer(GlanceModifier.height(6.dp))
                val heroText = "${snap.gpuPct?.toInt() ?: 0}% · ${snap.gpuMHz ?: 0} MHz"
                Hero(heroText, pal, stale)
                Spacer(GlanceModifier.height(4.dp))
                val histKey = "${snap.gpuHist.contentHash()}|${snap.timestamp}"
                BandBitmap(histKey, "gpuSpark", tier, medium, 28, "gpu spark") { c, w, h, d ->
                    c.spark(snap.gpuHist, pal, pal.ch06, w, h, d)
                }
                // live datasheet line (WD T1: "adreno 740 · vulkan 1.3") — honest, no fake freq hist
                val datasheet = listOf(snap.gpuName, snap.gpuVulkan).filter { it.isNotBlank() }.distinct().joinToString(" · ")
                if (datasheet.isNotEmpty()) {
                    Spacer(GlanceModifier.height(4.dp))
                    Subline(datasheet, pal)
                }
                Spacer(GlanceModifier.defaultWeight())
                Footer(pal, snap, cfg, "${cfg.traceWindowS}s window")
            }
        }
    }
}

class BenchWidgetAll : GlanceAppWidget() {
    override val sizeMode: SizeMode = ResponsiveBench
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val medium = resolvedMedium(context, cfg)
        val pal = WidgetPalettes.of(medium)
        val snap = GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: BenchSampler.sample(context)
        val placedAt = try { BenchState.placedAt(context, id) } catch (_: Exception) { 0L }
        val calibrating = snap.timestamp == 0L || (placedAt != 0L && System.currentTimeMillis() - placedAt < 6000)
        val awId = try { GlanceAppWidgetManager(context).getAppWidgetId(id) } catch (_: Exception) { AppWidgetManager.INVALID_APPWIDGET_ID }
        provideContent {
            val size = LocalSize.current
            val tier = tierForSize(size.width.value.toInt(), size.height.value.toInt())
            val stale = snap.stale(cadenceMs(cfg, snap))
            val isLedger = tier <= Tier.T2

            if (isLedger) {
                val desc = "Bench. Four channels. Updated ${updString(snap.timestamp)}."
                BenchPanel(pal, desc, openConfig(awId)) {
                    BenchMasthead(pal, stale, calibrating, snap, configTap = openConfig(awId))
                    Spacer(GlanceModifier.height(8.dp))
                    cfg.compactChannels.take(4).forEach { chId ->
                        val (label, value, sub) = channelRowData(chId, snap, cfg)
                        ChannelRow(chId, label, value, pal, sub)
                        Spacer(GlanceModifier.height(2.dp))
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    val footerText = if (snap.warning()) "1 channel warning" else "all channels nominal"
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(footerText, style = TextStyle(color = ColorProvider(if (snap.warning()) pal.fault else pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                        Spacer(GlanceModifier.defaultWeight())
                        Text("upd ${if (stale) "SIGNAL LOST" else updString(snap.timestamp)}", style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                }
            } else {
                // T3+ tiled — W7 (WD §7): 2×3 miniature tiles, label + value + one 14dp bitmap each
                val desc = "Bench. Six channels. Updated ${updString(snap.timestamp)}."
                BenchPanel(pal, desc, openConfig(awId)) {
                    BenchMasthead(pal, stale, calibrating, snap, configTap = openConfig(awId))
                    Spacer(GlanceModifier.height(6.dp))
                    val allChannels = if (tier == Tier.T3) listOf("CH-01", "CH-02", "CH-03", "CH-04", "CH-05")
                                      else listOf("CH-01", "CH-02", "CH-03", "CH-04", "CH-05", "CH-06")
                    allChannels.chunked(2).forEach { row ->
                        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.Top) {
                            row.forEach { chId ->
                                Column(
                                    modifier = GlanceModifier.defaultWeight().clickable(open(chId)).padding(end = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val (label, value, _) = channelRowData(chId, snap, cfg)
                                    Text("$chId · $label", style = TextStyle(color = ColorProvider(pal.ink60), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                                    Text(value, style = TextStyle(color = ColorProvider(pal.ink), fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace))
                                    Spacer(GlanceModifier.height(2.dp))
                                    TileBitmap(chId, snap, pal, medium, tier)
                                }
                            }
                            // odd last row: one tile + weight spacer keeps the 2-column grid
                            if (row.size == 1) Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                        Spacer(GlanceModifier.height(6.dp))
                    }
                    if (tier >= Tier.T5 && snap.cores.isNotEmpty()) {
                        BandBitmap("${snap.cores.hashCode()}|${snap.timestamp}", "benchRail", tier, medium, (snap.cores.size * 8).coerceAtLeast(16), "core rail") { c, w, _, _ ->
                            c.coreRailRows(context, pal, w, snap.cores, 8f * context.resources.displayMetrics.density)
                        }
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    val footerText = if (snap.warning()) "1 channel warning" else "all channels nominal"
                    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Text(footerText, style = TextStyle(color = ColorProvider(if (snap.warning()) pal.fault else pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                        Spacer(GlanceModifier.defaultWeight())
                        Text("upd ${if (stale) "SIGNAL LOST" else updString(snap.timestamp)}", style = TextStyle(color = ColorProvider(pal.ink40), fontSize = 11.sp, fontFamily = FontFamily.Monospace))
                    }
                }
            }
        }
    }
}

private fun channelRowData(chId: String, snap: BenchSnapshot, cfg: BenchConfig): Triple<String, String, String?> {
    return when (chId) {
        "CH-01" -> Triple("CPU", Fmt.pct(snap.cpuPct, 1), if (snap.freqGHz > 0) Fmt.hz((snap.freqGHz * 1e6).toLong()) else null)
        "CH-02" -> Triple("MEMORY", if (snap.memTotalGb > 0) String.format(Locale.US, "%.1f / %.0f GB", snap.memUsedGb, snap.memTotalGb) else "—", null)
        "CH-03" -> Triple("NETWORK", if (snap.netDown > 0 || snap.netUp > 0) "↓ ${Fmt.rate(snap.netDown)} ↑ ${Fmt.rate(snap.netUp)}" else "—", null)
        "CH-04" -> Triple("POWER", if (snap.batteryPresent) Fmt.wattsSigned(snap.watts) else "NOT FITTED", null)
        "CH-05" -> Triple("STORAGE", if (snap.stoTotalGb > 0) String.format(Locale.US, "%.1f / %.0f GB", snap.stoUsedGb, snap.stoTotalGb) else "—", null)
        "CH-06" -> Triple("GPU", if (snap.gpuFitted) "${snap.gpuPct?.toInt() ?: 0}% · ${snap.gpuMHz ?: 0} MHz" else "NOT FITTED", null)
        else -> Triple(chId, "—", null)
    }
}

// TopConsumersProvider is authoritative; ActivityManager fallback removed (empty for 3p on API 26+).
// Ledger hides when permission not granted (snap.topConsumers empty) — see StackWidget.

// ─────────────── Receivers (aliases kept) ───────────────

class SingleChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScopeWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); BenchBudget.enqueue(context) }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { BenchBudget.cancelIfNone(context) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id -> BenchFrames.remove(id.toString()); BenchUpdater.evict(id) }
    }
}

class DualChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StackWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); BenchBudget.enqueue(context) }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { BenchBudget.cancelIfNone(context) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id -> BenchFrames.remove(id.toString()); BenchUpdater.evict(id) }
    }
}

class BenchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BenchWidgetAll()
    override fun onEnabled(context: Context) { super.onEnabled(context); BenchBudget.enqueue(context) }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { BenchBudget.cancelIfNone(context) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id -> BenchFrames.remove(id.toString()); BenchUpdater.evict(id) }
    }
}

class FuelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FuelWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); BenchBudget.enqueue(context) }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { BenchBudget.cancelIfNone(context) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id -> BenchFrames.remove(id.toString()); BenchUpdater.evict(id) }
    }
}

class RasterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RasterWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); BenchBudget.enqueue(context) }
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { BenchBudget.cancelIfNone(context) }
    }
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { id -> BenchFrames.remove(id.toString()); BenchUpdater.evict(id) }
    }
}
