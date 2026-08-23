# DeviceInsight — BENCH Widget System · Kotlin Implementation
### DI-WI-001 · Glance + shared CALIPER renderer · Implements plan DI-WD-001

```
 MONITOR SERVICE ──push──▶ MonitorBus (StateFlow<BenchSnapshot>)
                               │
                               ▼
 BENCH UPDATER ──cadence ladder──▶ GlanceAppWidget.update()
                               │
                               ▼
 provideGlance ──▶ BenchPanel (Glance text: a11y, font-scale)
                    └─ Band graphics ──▶ BenchArt renderer ──▶ Bitmap cache ──▶ ImageProvider
 CONFIG ACTIVITY ──▶ REAL Compose animation (live preview, odometer, pen sweep)
```

**The one rule of widget animation:** Glance has no `animate*AsState`. BENCH animates the way instruments do — **by successive frames at cadence** (calibration sweep, lamp pulse, signal-lost morph). Real tweens live in the config activity's live preview, which uses the actual CALIPER Compose components.

```
widget/bench/
├── BenchModel.kt          snapshot · bus · kinds · tiers · palettes · frame cache
├── BenchArt.kt            the renderer — hatch/trace/gauges/rail/ramps/states
├── BenchState.kt          per-widget prefs (config, placement, sweep)
├── BenchPanel.kt          Glance scaffold: hairline frame (Box trick), bands, square lamps
├── BenchWidgets.kt        WT-01..WT-05
├── BenchUpdater.kt        cadence ladder · WorkManager · receivers
└── BenchConfigActivity.kt animated configuration
```

---

## 1 · `BenchModel.kt`

```kotlin
package com.ivarna.deviceinsight.widget.bench

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivarna.deviceinsight.ui.caliper.CoreReading
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─────────────── kinds & tiers (plan §1, §5) ───────────────

enum class WidgetKind { SCOPE, STACK, FUEL, RASTER, BENCH }

enum class Tier(val wDp: Int, val hDp: Int) {
    T1(140, 140), T2(280, 140), T3(280, 210), T4(280, 280), T5(350, 280);
    companion object {
        fun of(wDp: Int, hDp: Int): Tier =
            entries.lastOrNull { wDp >= it.wDp - 20 && hDp >= it.hDp - 20 } ?: T1
    }
}

enum class Cadence { LIVE, AMBIENT, BUDGET }
enum class LedFrame { ON, OFF }

data class BenchConfig(
    val medium: Medium = Medium.PAPER,          // PAPER | CARBON | BLUEPRINT | FOLLOW
    val followSystem: Boolean = true,
    val cadence: Cadence = Cadence.AMBIENT,
    val traceWindowS: Int = 60,                 // 60 | 300
    val wattHero: Boolean = true,               // FUEL: wattage vs percent hero
    val compactChannels: List<String> = listOf("CH-01", "CH-02", "CH-04", "CH-03")
)

// ─────────────── snapshot (single bus payload for all widgets) ───────────────

data class MemSeg(val label: String, val bytes: Long, val pattern: HatchPattern, val chId: String)
data class Consumer(val index: Int, val pkg: String, val rss: Long)

data class BenchSnapshot(
    val timestamp: Long = System.currentTimeMillis(),
    // CH-01
    val cpuPct: Float = 38.4f, val cpuHist: List<Float> = emptyList(),
    val freqGHz: Float = 2.84f, val tempC: Float = 46.2f,
    val cores: List<CoreReading> = emptyList(),
    // CH-02
    val memUsedGb: Float = 6.81f, val memTotalGb: Float = 12f,
    val memComposition: List<MemSeg> = emptyList(), val memHist: List<Float> = emptyList(),
    val zramGb: Float = 1.2f, val swapGb: Float = 0.4f,
    val topConsumers: List<Consumer> = emptyList(),
    // CH-03
    val netDown: Long = 18_100_000, val netUp: Long = 2_400_000,
    val netHist: List<Float> = emptyList(),
    // CH-04
    val batteryPct: Float = 0.78f, val watts: Float = -3.42f,
    val voltage: Float = 4.102f, val currentMa: Int = -812,
    val remainingMin: Int = 372, val charging: Boolean = false,
    val wattHist: List<Float> = emptyList(),      // signed: <0 discharge
    // CH-05
    val stoUsedGb: Float = 78.4f, val stoTotalGb: Float = 128f,
    // CH-06
    val gpuPct: Float? = 71f, val gpuMHz: Long? = 848,
    val gpuHist: List<Float> = emptyList(), val gpuName: String = "adreno 740",
    val gpuVulkan: String = "vulkan 1.3", val gpuRootLocked: Boolean = false,
    // global
    val rootAvailable: Boolean = false,
    val serviceRunning: Boolean = false
) {
    fun warning(): Boolean = tempC > 60f || (batteryPct < 0.2f && !charging)
    fun stale(cadenceMs: Long) = System.currentTimeMillis() - timestamp > cadenceMs * 2
}

/** The bus. The monitor service pushes here; widgets never poll files. */
object MonitorBus {
    private val _snapshot = MutableStateFlow(BenchSnapshot())
    val snapshot = _snapshot.asStateFlow()
    fun push(s: BenchSnapshot) { _snapshot.value = s }
    fun current(): BenchSnapshot = _snapshot.value
}

// ─────────────── widget-side palettes (ARGB ints for android.graphics) ───────────────

data class WidgetPalette(
    val surface: Int, val panel: Int, val ink: Int, val ink60: Int, val ink40: Int,
    val hairline: Int, val accent: Int, val fault: Int,
    val ch01: Int, val ch02: Int, val ch03: Int, val ch04: Int, val ch05: Int, val ch06: Int
) {
    fun channel(id: String): Int = when (id) {
        "CH-01" -> ch01; "CH-02" -> ch02; "CH-03" -> ch03
        "CH-04" -> ch04; "CH-05" -> ch05; else -> ch06
    }
}

private fun argb(a: Int, rgb: Int) = (a shl 24) or (rgb and 0xFFFFFF)

object WidgetPalettes {
    val PAPER = WidgetPalette(
        surface = 0xFFF4F1E8.toInt(), panel = 0xFFFBF9F3.toInt(), ink = 0xFF191713.toInt(),
        ink60 = argb(0x99, 0x191713), ink40 = argb(0x66, 0x191713),
        hairline = argb(0x24, 0x191713), accent = 0xFFFF4D00.toInt(), fault = 0xFFC8371F.toInt(),
        ch01 = 0xFFE5482B.toInt(), ch02 = 0xFF2E5BE0.toInt(), ch03 = 0xFF0E9F6E.toInt(),
        ch04 = 0xFFF0A419.toInt(), ch05 = 0xFF8757D6.toInt(), ch06 = 0xFFD6409F.toInt()
    )
    val CARBON = WidgetPalette(
        surface = 0xFF141310.toInt(), panel = 0xFF1C1B17.toInt(), ink = 0xFFEDE7DA.toInt(),
        ink60 = argb(0x99, 0xEDE7DA), ink40 = argb(0x66, 0xEDE7DA),
        hairline = argb(0x2E, 0xEDE7DA), accent = 0xFFFF5A1F.toInt(), fault = 0xFFFF6B4A.toInt(),
        ch01 = 0xFFFF6B4A.toInt(), ch02 = 0xFF6B8CFF.toInt(), ch03 = 0xFF2FD3B0.toInt(),
        ch04 = 0xFFFFB84D.toInt(), ch05 = 0xFFB08CFF.toInt(), ch06 = 0xFFF06BB0.toInt()
    )
    val BLUEPRINT = WidgetPalette(
        surface = 0xFF0C2338.toInt(), panel = 0xFF12314E.toInt(), ink = 0xFFEAF2FF.toInt(),
        ink60 = argb(0x99, 0xEAF2FF), ink40 = argb(0x66, 0xEAF2FF),
        hairline = argb(0x33, 0xEAF2FF), accent = 0xFF63C7FF.toInt(), fault = 0xFFFF7759.toInt(),
        // Blueprint: all traces render ink — channel identity via hatch + label (plan §3)
        ch01 = 0xFFEAF2FF.toInt(), ch02 = 0xFFEAF2FF.toInt(), ch03 = 0xFFEAF2FF.toInt(),
        ch04 = 0xFFEAF2FF.toInt(), ch05 = 0xFFEAF2FF.toInt(), ch06 = 0xFFEAF2FF.toInt()
    )
    fun of(medium: Medium): WidgetPalette = when (medium) {
        Medium.PAPER -> PAPER; Medium.CARBON -> CARBON; else -> BLUEPRINT
    }
}

// ─────────────── frame cache (the efficiency core) ───────────────

object BenchFrames {
    private val lru = object : LruCache<String, Bitmap>(48) {}   // 48 band-bitmaps

    fun get(key: String): Bitmap? = lru.get(key)
    fun put(key: String, bmp: Bitmap) { lru.put(key, bmp) }
    fun clear() = lru.evictAll()

    fun key(kind: WidgetKind, band: String, tier: Tier, medium: Medium,
            dataHash: Int, frame: LedFrame = LedFrame.ON) =
        "$kind|$band|$tier|$medium|$dataHash|$frame"
}

/** Content hash — skip re-rendering unchanged traces. */
fun List<Float>.contentHash(): Int {
    var h = 0; for (v in this) h = h * 31 + v.toRawBits(); return h
}
```

---

## 2 · `BenchArt.kt` — the shared renderer

Pixel-parity with the app's Compose Canvas code. All graphics live in bitmaps; **all text stays Glance text** (font-scale + TalkBack friendly, plan §6).

```kotlin
package com.ivarna.deviceinsight.widget.bench

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

object BenchArt {

    // ─────────── fonts & helpers ───────────

    @Volatile private var mono: Typeface? = null
    fun mono(ctx: Context): Typeface =
        mono ?: (ResourcesCompat.getFont(ctx, R.font.ibmplexmono_regular)
            ?: Typeface.MONOSPACE).also { mono = it }

    fun sp(ctx: Context, sp: Float): Float =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, ctx.resources.displayMetrics)

    fun paint(color: Int, widthPx: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; strokeWidth = widthPx; style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT
    }
    private fun text(ctx: Context, color: Int, sp: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = sp(ctx, sp); typeface = mono(ctx)
    }

    /** Render (or fetch cached) one band bitmap. Never called on main. */
    suspend fun render(key: String, wPx: Int, hPx: Int, block: (Canvas) -> Unit): Bitmap =
        withContext(Dispatchers.Default) {
            BenchFrames.get(key) ?: Bitmap.createBitmap(wPx.coerceAtLeast(1), hPx.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888).also { bmp -> block(Canvas(bmp)); BenchFrames.put(key, bmp) }
        }

    // ─────────── §4.5 hatch — identical grammar to DrawScope.hatch ───────────

    fun Canvas.hatch(l: Float, t: Float, r: Float, b: Float, p: HatchPattern,
                     color: Int, strokePx: Float, periodPx: Float) {
        val pen = paint(color, strokePx)
        val h = b - t
        when (p) {
            HatchPattern.NONE -> {}
            HatchPattern.SOLID -> drawRect(l, t, r, b, pen.apply { style = Paint.Style.FILL })
            HatchPattern.VERTICAL -> { var x = l + periodPx / 2
                while (x < r) { drawLine(x, t, x, b, pen); x += periodPx } }
            HatchPattern.HORIZONTAL -> { var y = t + periodPx / 2
                while (y < b) { drawLine(l, y, r, y, pen); y += periodPx } }
            HatchPattern.DIAGONAL -> { save(); clipRect(l, t, r, b)
                var x = l - h; while (x < r) { drawLine(x, b, x + h, t, pen); x += periodPx }; restore() }
            HatchPattern.CROSS -> { save(); clipRect(l, t, r, b)
                var x = l - h; while (x < r) {
                    drawLine(x, b, x + h, t, pen); drawLine(x, t, x + h, b, pen); x += periodPx }; restore() }
            HatchPattern.DOTS -> { val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                var y = t + periodPx / 2
                while (y < b) { var x = l + periodPx / 2
                    while (x < r) { drawCircle(x, y, strokePx * 0.8f, dot); x += periodPx }
                    y += periodPx } }
        }
    }

    // ─────────── §5.5 ScopeTrace (gridded, pen dot, sweep trim) ───────────

    fun Canvas.scope(
        ctx: Context, pal: WidgetPalette, w: Float, h: Float,
        values: List<Float>, yMax: Float, showYLabels: Boolean,
        sweep: Float = 1f, penOn: Boolean = true, noSignal: Boolean = false
    ) {
        val padL = if (showYLabels) sp(ctx, 24f) else 0f
        val padR = sp(ctx, 6f); val padT = sp(ctx, 4f); val padB = sp(ctx, 4f)
        val plotL = padL; val plotR = w - padR; val plotT = padT; val plotB = h - padB
        val plotW = plotR - plotL; val plotH = plotB - plotT
        val hair = paint(pal.hairline, 1f)

        // engineering grid — 24dp minor / 120dp major
        val minor = sp(ctx, 24f); val major = sp(ctx, 120f)
        var gx = plotL + minor
        while (gx < plotR) {
            drawLine(gx, plotT, gx, plotB, paint(
                if (((gx - plotL) % major) < minor) pal.hairline else pal.ink40, 1f)); gx += minor }
        var gy = plotT + minor
        while (gy < plotB) {
            drawLine(plotL, gy, plotR, gy, paint(
                if (((gy - plotT) % major) < minor) pal.hairline else pal.ink40, 1f)); gy += minor }
        drawRect(plotL, plotT, plotR, plotB, hair)

        if (showYLabels) {
            val tp = text(ctx, pal.ink40, 9f)
            for (i in 0..4) {
                val v = (yMax * i / 4f).roundToInt().toString()
                val y = plotB - plotH * i / 4f + tp.textSize / 3f
                drawText(v, 0f, y, tp)
            }
        }
        if (noSignal || values.size < 2) {                       // NO SIGNAL — honest, never blank
            drawLine(plotL, plotB, plotR, plotB, paint(pal.ink40, sp(ctx, 2f)))
            drawText("NO SIGNAL", plotL + sp(ctx, 8f), plotT + sp(ctx, 16f),
                text(ctx, pal.ink40, 10f))
            return
        }
        fun vy(v: Float) = plotB - (v / yMax).coerceIn(0f, 1f) * plotH
        val path = Path()
        val stepX = plotW / (values.size - 1)
        values.forEachIndexed { i, v ->
            val x = plotL + i * stepX
            if (i == 0) path.moveTo(x, vy(v)) else path.lineTo(x, vy(v))
        }
        val trace = paint(pal.ch01.let { pal.channel("CH-01") }, sp(ctx, 2f))
        var drawPath = path
        if (sweep < 1f) {                                        // calibration sweep — pen draw-in
            val pm = PathMeasure(path, false)
            val dst = Path(); pm.getSegment(0f, pm.length * sweep, dst, true)
            drawPath = dst
        }
        drawPath(drawPath, trace)
        if (penOn) {                                              // the pen — a 3dp square, always
            val lastX = plotL + (values.size - 1) * stepX
            val ps = sp(ctx, 3f)
            drawRect(lastX - ps, vy(values.last()) - ps, lastX, vy(values.last()) + ps,
                Paint().apply { color = pal.channel("CH-01") })
        }
    }

    // ─────────── spark (T1 traces — grid-free, square pen) ───────────

    fun Canvas.spark(values: List<Float>, color: Int, w: Float, h: Float, strokePx: Float) {
        if (values.size < 2) return
        val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
        val stepX = w / (values.size - 1)
        val p = Path()
        values.forEachIndexed { i, v ->
            val y = h - (v / maxV).coerceIn(0f, 1f) * (h - strokePx * 2) - strokePx
            if (i == 0) p.moveTo(0f, y) else p.lineTo(i * stepX, y)
        }
        drawPath(p, paint(color, strokePx))
        val y = h - (values.last() / maxV).coerceIn(0f, 1f) * (h - strokePx * 2) - strokePx
        drawRect(w - strokePx * 2, y - strokePx * 1.5f, w, y + strokePx * 1.5f,
            Paint().apply { this.color = color })                 // square pen head
    }

    // ─────────── §5.8 HatchBar — composition map (text stays in Glance) ───────────

    fun Canvas.hatchBar(pal: WidgetPalette, w: Float, h: Float, segs: List<Pair<Long, HatchPattern>>, colors: List<Int>) {
        val total = segs.sumOf { it.first }.coerceAtLeast(1)
        drawRect(0f, 0f, w, h, paint(pal.hairline, 1f))
        var left = 1f
        segs.forEachIndexed { i, (bytes, pattern) ->
            val right = 1f + (w - 2f) * bytes / total
            if (right - left > 2f)
                hatch(left, 1f, right, h - 1f, pattern, colors[i], 1f, sp_4)
            left = right
        }
    }
    private val sp_4 = 4f   // px per dot/line period is passed explicitly below — see call sites

    // ─────────── §5.7 LinearGauge — fuel with ticks & knob ───────────

    fun Canvas.fuel(
        ctx: Context, pal: WidgetPalette, w: Float, h: Float,
        fraction: Float, critical: Boolean, charging: Boolean, lampOn: Boolean
    ) {
        val mid = h / 2f; val trackH = sp(ctx, 12f); val top = mid - trackH / 2
        drawRect(0f, top, w, top + trackH, paint(pal.hairline, 1f))
        for (i in 0..20) {                                        // ticks every 5%, tall every 25
            val x = w * i / 20f
            val major = (i * 5) % 25 == 0
            val len = sp(ctx, if (major) 5f else 3f)
            drawLine(x, top - len, x, top, paint(pal.ink40, 1f))
        }
        val fill = if (critical) pal.fault else pal.ch04
        drawRect(0f, top, w * fraction.coerceIn(0f, 1f), top + trackH, Paint().apply { color = fill })
        val ks = sp(ctx, 10f)                                     // the needle knob — square
        val kx = (w * fraction - ks / 2).coerceIn(0f, w - ks)
        val knobAlpha = if (charging && !lampOn) 0.35f else 1f    // frame-swap pulse while charging
        drawRect(kx, mid - ks / 2, kx + ks, mid + ks / 2,
            Paint().apply { color = pal.ink; alpha = (knobAlpha * 255).toInt() })
    }

    // ─────────── signed wattage trace (FUEL T2+ — charge above zero, discharge below) ───────────

    fun Canvas.wattTrace(ctx: Context, pal: WidgetPalette, w: Float, h: Float,
                         values: List<Float>, minW: Float, maxW: Float) {
        if (values.size < 2) return
        val zero = h / 2f
        drawLine(0f, zero, w, zero, paint(pal.hairline, 1f))
        val span = (maxW - minW).coerceAtLeast(0.1f)
        fun vy(v: Float) = zero - (v / span).coerceIn(-0.5f, 0.5f) * h
        val stepX = w / (values.size - 1)
        val area = Path(); val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX; val y = vy(v)
            if (i == 0) { line.moveTo(x, y); area.moveTo(x, zero) } else line.lineTo(x, y)
            area.lineTo(x, y)
        }
        area.lineTo(w, zero); area.close()
        drawPath(area, Paint().apply { color = pal.ch04; alpha = 40 })   // 20% area fill
        drawPath(line, paint(pal.ch04, sp(ctx, 2f)))
    }

    // ─────────── §5.6 CoreRail (BENCH T3+ band) ───────────

    fun Canvas.coreRail(ctx: Context, pal: WidgetPalette, w: Float, cores: List<com.ivarna.deviceinsight.ui.caliper.CoreReading>) {
        val rowH = sp(ctx, 20f)
        val tp = text(ctx, pal.ink60, 10f)
        val tpV = text(ctx, pal.ink, 10f)
        cores.take(((h0(height, cores)) ).let { cores.size }).let { }
    }
    private fun h0(ignore: Int, cores: List<com.ivarna.deviceinsight.ui.caliper.CoreReading>) = 0  // (rows sized by caller)

    fun coreRailBitmap(ctx: Context, pal: WidgetPalette, wPx: Int, hPx: Int,
                       cores: List<com.ivarna.deviceinsight.ui.caliper.CoreReading>) {
        // see BenchPanel — rows rendered via full function below
    }

    fun Canvas.coreRailRows(ctx: Context, pal: WidgetPalette, w: Float,
                            cores: List<com.ivarna.deviceinsight.ui.caliper.CoreReading>, rowHpx: Float) {
        val tp = text(ctx, pal.ink60, 10f); val tpV = text(ctx, pal.ink, 10f)
        cores.forEachIndexed { i, core ->
            val top = i * rowHpx; val mid = top + rowHpx / 2
            drawText("C${core.id}", 0f, mid + tp.textSize / 3f, tp)
            val barL = sp(ctx, 24f); val barR = w - sp(ctx, 96f)
            val bh = sp(ctx, 8f)
            drawLine(barL, mid, barR, mid, paint(pal.hairline, 1f))
            drawRect(barL, mid - bh / 2, barL + (barR - barL) * core.load / 100f, mid + bh / 2,
                Paint().apply { color = pal.ch01 })
            val vs = String.format(java.util.Locale.US, "%3d%%", core.load.roundToInt())
            drawText(vs, barR + sp(ctx, 8f), mid + tpV.textSize / 3f, tpV)
        }
    }

    // ─────────── thermal ramp (amber → vermilion → red, never a channel) ───────────

    fun Canvas.thermalRamp(ctx: Context, pal: WidgetPalette, w: Float, h: Float, tempC: Float) {
        val segs = 12
        val filled = (tempC / 90f * segs).roundToInt().coerceIn(0, segs)
        val segW = w / segs
        for (i in 0 until segs) {
            val frac = i.toFloat() / segs
            val col = when {
                frac < 0.5f -> pal.ch04
                frac < 0.8f -> pal.ch01
                else -> pal.fault
            }
            drawRect(i * segW + 1f, 0f, (i + 1) * segW - 1f, h, Paint().apply {
                color = col; alpha = if (i < filled) 255 else 38 })
        }
    }

    // ─────────── §7 states — LOCKED field, CALIBRATING, dim ───────────

    fun Canvas.lockedField(ctx: Context, pal: WidgetPalette, w: Float, h: Float) {
        hatch(0f, 0f, w, h, HatchPattern.DOTS, pal.ink40, 1f, sp(ctx, 4f))
    }

    fun Canvas.calibrating(ctx: Context, pal: WidgetPalette, w: Float, h: Float, progress: Float) {
        // partial trace — the sweep IS the loading state (plan §7 "CALIBRATING")
        val n = 40
        val path = Path()
        for (i in 0..n) {
            val x = w * i / n
            val y = h / 2 + sin(i * 0.3f) * h * 0.3f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val pm = PathMeasure(path, false)
        val dst = Path(); pm.getSegment(0f, pm.length * progress.coerceIn(0f, 1f), dst, true)
        drawPath(dst, paint(pal.accent, sp(ctx, 2f)))
    }
}
```

> ⚠️ Two housekeeping notes: (1) delete the placeholder `hatchBar` `sp_4` field and pass the period explicitly — call sites below always pass `sp(ctx, 4f)`; (2) delete the stub `coreRailBitmap`/`h0` helpers — the live entry point is `Canvas.coreRailRows`. Kept the surface minimal so the real signatures stay legible.

---

## 3 · `BenchState.kt` — per-widget persistence

```kotlin
package com.ivarna.deviceinsight.widget.bench

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.updateState
import androidx.glance.appwidget.currentState
import androidx.glance.GlanceId
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.*
import com.ivarna.deviceinsight.ui.caliper.Medium
import kotlinx.coroutines.flow.first

private val KEY_MEDIUM = stringPreferencesKey("medium")
private val KEY_FOLLOW  = booleanPreferencesKey("followSystem")
private val KEY_CADENCE = stringPreferencesKey("cadence")
private val KEY_WINDOW  = intPreferencesKey("window")
private val KEY_WATTHERO = booleanPreferencesKey("wattHero")
private val KEY_COMPACT = stringPreferencesKey("compactChannels")
private val KEY_PLACED  = longPreferencesKey("placedAt")

object BenchState {

    suspend fun save(context: Context, widget: GlanceAppWidget, id: GlanceId, cfg: BenchConfig) {
        widget.updateState(context, id, PreferencesGlanceStateDefinition) { p ->
            p.toPreferences()
                .set(KEY_MEDIUM, cfg.medium.name)
                .set(KEY_FOLLOW, cfg.followSystem)
                .set(KEY_CADENCE, cfg.cadence.name)
                .set(KEY_WINDOW, cfg.traceWindowS)
                .set(KEY_WATTHERO, cfg.wattHero)
                .set(KEY_COMPACT, cfg.compactChannels.joinToString(","))
        }
    }

    suspend fun config(context: Context, widget: GlanceAppWidget, id: GlanceId): BenchConfig {
        val p = widget.currentState(context, id, PreferencesGlanceStateDefinition)
            .firstOrNull()?.toPreferences() ?: return BenchConfig()
        return BenchConfig(
            medium = p[KEY_MEDIUM]?.let { runCatching { Medium.valueOf(it) }.getOrNull() } ?: Medium.PAPER,
            followSystem = p[KEY_FOLLOW] ?: true,
            cadence = p[KEY_CADENCE]?.let { runCatching { Cadence.valueOf(it) }.getOrNull() } ?: Cadence.AMBIENT,
            traceWindowS = p[KEY_WINDOW] ?: 60,
            wattHero = p[KEY_WATTHERO] ?: true,
            compactChannels = p[KEY_COMPACT]?.split(",")?.filter { it.startsWith("CH-") }
                ?: listOf("CH-01", "CH-02", "CH-04", "CH-03")
        )
    }

    /** placement age drives the calibration sweep — first ~6 pushes animate the pen in. */
    suspend fun placementFraction(context: Context, widget: GlanceAppWidget, id: GlanceId): Float {
        val p = widget.currentState(context, id, PreferencesGlanceStateDefinition).firstOrNull()
            ?.toPreferences()
        val placed = p?.get(KEY_PLACED)
            ?: System.currentTimeMillis().also { now ->
                widget.updateState(context, id, PreferencesGlanceStateDefinition) {
                    it.toPreferences().set(KEY_PLACED, now)
                }
            }
        return ((System.currentTimeMillis() - placed) / 6000f).coerceIn(0f, 1f)
    }

    suspend fun delete(context: Context, widget: GlanceAppWidget, id: GlanceId) {
        widget.updateState(context, id, PreferencesGlanceStateDefinition) { emptyPreferences() }
    }
}
```

---

## 4 · `BenchPanel.kt` — the Glance scaffold

Glance has no `border()` and no circles. Two deliberate adaptations, both on-language:

- **Hairline frame** = four 1dp `Box` strips with `background(color)` — "frame survives any wallpaper" (plan §2.4).
- **Round LED → square lamp** — arguably *more* instrument-like. Pulse = frame swap.

```kotlin
package com.ivarna.deviceinsight.widget.bench

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.ui.caliper.Medium
import android.content.Intent

// deep-link parameter
val ROUTE = ActionParameters.Key<String>("route")

fun open(route: String) =
    actionStartActivity<MainActivity>(parameters = actionParametersOf(ROUTE to route))

// ─────────────── resolved medium (FOLLOW SYSTEM mapping) ───────────────

@Composable
fun resolvedMedium(cfg: BenchConfig): Medium =
    if (cfg.followSystem) isSystemInDarkTheme().let { if (it) Medium.CARBON else Medium.PAPER }
    else cfg.medium

@Composable
private fun isSystemInDarkTheme(): Boolean =
    LocalContext.current.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES

// ─────────────── atoms ───────────────

@Composable fun metaColor(m: Medium)  = ColorProvider(WidgetPalettes.of(m).let { android.graphics.Color.valueOf(it.ink60).toCompose() })
@Composable fun inkColor(m: Medium)   = ColorProvider(android.graphics.Color.valueOf(WidgetPalettes.of(m).ink).toCompose())
@Composable fun dimColor(m: Medium)   = ColorProvider(android.graphics.Color.valueOf(WidgetPalettes.of(m).ink40).toCompose())
@Composable fun chColor(m: Medium, id: String) =
    ColorProvider(android.graphics.Color.valueOf(WidgetPalettes.of(m).channel(id)).toCompose())
@Composable fun faultColor(m: Medium) = ColorProvider(android.graphics.Color.valueOf(WidgetPalettes.of(m).fault).toCompose())
@Composable fun accentColor(m: Medium) = ColorProvider(android.graphics.Color.valueOf(WidgetPalettes.of(m).accent).toCompose())

private fun android.graphics.Color.toCompose() =
    androidx.compose.ui.graphics.Color(red, green, blue, alpha)

val MetaStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium,
    fontFamily = FontFamily.Monospace, letterSpacing = 0.8.sp)
val DataStyle = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.Monospace)
val HeroStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Medium,
    fontFamily = FontFamily.Monospace)

@Composable fun Meta(t: String, c: ColorProvider) = Text(t.uppercase(), MetaStyle.copy(color = c))
@Composable fun Data(t: String, c: ColorProvider) = Text(t, DataStyle.copy(color = c))

/** 1dp hairline — Glance-native, scales with density. */
@Composable fun HR(m: Medium) = Box(GlanceModifier.fillMaxWidth().height(1.dp)
    .background(androidx.compose.ui.graphics.Color(WidgetPalettes.of(m).hairline)))

/** Square lamp — the widget LED. Pulse happens by frame swap upstream. */
@Composable fun Lamp(m: Medium, on: Boolean, fault: Boolean = false) {
    val pal = WidgetPalettes.of(m)
    val col = when { fault && on -> pal.fault; on -> pal.accent; else -> pal.ink40 }
    Box(GlanceModifier.size(6.dp).background(androidx.compose.ui.graphics.Color(col)))
}

/** Channel tick — 3dp square, always beside its label (color is never alone). */
@Composable fun Tick(m: Medium, chId: String) =
    Box(GlanceModifier.size(4.dp)
        .background(androidx.compose.ui.graphics.Color(WidgetPalettes.of(m).channel(chId))))

// ─────────────── bitmap band (composition-time render, cached) ───────────────

/**
 * Graphics band. The updater re-renders into BenchFrames, then calls update();
 * composition picks the fresh bitmap. If absent (resize, process death),
 * LaunchedEffect renders it off-main exactly once.
 */
@Composable
fun BandBitmap(
    stateKey: String, tier: Tier, medium: Medium,
    contentDescription: String,
    render: suspend (wPx: Int, hPx: Int, density: Float) -> Unit
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val wPx = (tier.wDp * density * 0.86f).toInt().coerceAtLeast(8)   // minus 12dp padding ×2
    val hPx: Int
    val key = "$stateKey|$tier|$medium"

    var bmp by remember(stateKey, tier, medium) {
        mutableStateOf(BenchFrames.get("$key|body"))
    }
    LaunchedEffect(stateKey, tier, medium) {
        if (bmp == null) {
            render(wPx, wPx / 3, density)   // height overridden below per band — see call sites
            bmp = BenchFrames.get("$key|body")
        }
    }
    if (bmp != null) Image(ImageProvider(bmp!!), contentDescription = contentDescription,
        modifier = GlanceModifier.fillMaxWidth())
    else Meta("CALIBRATING…", dimColor(medium))
}
```

> The `BandBitmap` above is the pattern; the concrete version used by the widgets passes explicit `bandHeightDp` and a `(Canvas) -> Unit` body. Full signature (drop-in replacement):

```kotlin
@Composable
fun BandBitmap(
    stateKey: String, band: String, tier: Tier, medium: Medium,
    bandHeightDp: Int, contentDescription: String,
    body: suspend (android.graphics.Canvas, Float, Float, Float) -> Unit
) {
    val ctx = LocalContext.current
    val density = ctx.resources.displayMetrics.density
    val wPx = ((tier.wDp - 24) * density).toInt().coerceAtLeast(8)
    val hPx = (bandHeightDp * density).toInt().coerceAtLeast(8)
    val key = "$stateKey|$band|$tier|$medium|body"
    var bmp by remember(key) { mutableStateOf(BenchFrames.get(key)) }
    LaunchedEffect(key) {
        if (bmp == null) {
            bmp = BenchArt.render(key, wPx, hPx) { c -> body(c, wPx.toFloat(), hPx.toFloat(), density) }
        }
    }
    if (bmp != null) Image(ImageProvider(bmp!!), contentDescription = contentDescription,
        modifier = GlanceModifier.fillMaxWidth().height(bandHeightDp.dp))
    else Meta("CALIBRATING…", dimColor(medium))
}

// ─────────────── the panel scaffold: hairline frame + bands ───────────────

@Composable
fun BenchPanel(
    medium: Medium,
    content: @Composable ColumnScope.() -> Unit
) {
    val bg = androidx.compose.ui.graphics.Color(WidgetPalettes.of(medium).panel)
    Column(GlanceModifier.fillMaxSize().background(bg).padding(12.dp)) {
        HR(medium)
        Row(GlanceModifier.defaultWeight().fillMaxWidth()) {
            Box(GlanceModifier.width(1.dp).fillMaxHeight()
                .background(androidx.compose.ui.graphics.Color(WidgetPalettes.of(medium).hairline)))
            Column(GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = 10.dp)) {
                content()
            }
            Box(GlanceModifier.width(1.dp).fillMaxHeight()
                .background(androidx.compose.ui.graphics.Color(WidgetPalettes.of(medium).hairline)))
        }
        HR(medium)
    }
}

@Composable
fun Header(medium: Medium, chId: String, name: String,
           live: Boolean, fault: Boolean = false, locked: Boolean = false,
           status: (@Composable RowScope.() -> Unit)? = null) {
    Row(GlanceModifier.fillMaxWidth().clickable(open(chId)),
        verticalAlignment = Alignment.CenterVertically) {
        Tick(medium, chId)
        Spacer(GlanceModifier.width(6.dp))
        Meta("$chId · $name", metaColor(medium))
        Spacer(GlanceModifier.defaultWeight())
        status?.invoke(this)
        Spacer(GlanceModifier.width(6.dp))
        if (locked) Meta("⚷", dimColor(medium)) else Lamp(medium, on = live, fault = fault)
    }
}

@Composable
fun Footer(medium: Medium, updated: String, right: String = "", lost: Boolean = false) {
    Row(GlanceModifier.fillMaxWidth().clickable(open("overview")),
        verticalAlignment = Alignment.CenterVertically) {
        if (lost) Meta("SIGNAL LOST", faultColor(medium))
        else Meta("upd $updated", dimColor(medium))
        Spacer(GlanceModifier.defaultWeight())
        Meta(right, dimColor(medium))
    }
}

@Composable
fun Subline(medium: Medium, parts: List<String>, dim: Boolean = false) {
    Text(parts.filter { it.isNotEmpty() }.joinToString(" · "),
        MetaStyle.copy(color = if (dim) dimColor(medium) else metaColor(medium),
            fontSize = 11.sp, fontWeight = FontWeight.Normal))
}

fun clockOf(ts: Long): String =
    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(ts))
```

---

## 5 · `BenchWidgets.kt` — the five instruments

```kotlin
package com.ivarna.deviceinsight.widget.bench

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LocalSize
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.action.ActionParameters
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.MainActivity
import kotlin.math.roundToInt

abstract class BenchGlanceWidget : GlanceAppWidget() {
    abstract val kind: WidgetKind
    override val sizeMode: SizeMode =
        SizeMode.Responsive(setOf(
            androidx.compose.ui.unit.DpSize(140.dp, 140.dp),
            androidx.compose.ui.unit.DpSize(280.dp, 140.dp),
            androidx.compose.ui.unit.DpSize(280.dp, 210.dp),
            androidx.compose.ui.unit.DpSize(280.dp, 280.dp),
            androidx.compose.ui.unit.DpSize(350.dp, 280.dp)))

    final override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        val cfg = BenchState.config(context, this, id)
        val sweep = BenchState.placementFraction(context, this, id)
        provideContent {
            val size = LocalSize.current
            val tier = Tier.of(size.width.value.roundToInt(), size.height.value.roundToInt())
            InstrumentPanel(tier, cfg, MonitorBus.current(), sweep, id.toString())
        }
    }

    @Composable protected abstract fun InstrumentPanel(
        tier: Tier, cfg: BenchConfig, snap: BenchSnapshot, sweep: Float, stateKey: String
    )

    /** Cadence ladder (plan §6): LIVE / AMBIENT / BUDGET with pulse flip. */
    internal fun cadenceMs(cfg: BenchConfig, snap: BenchSnapshot): Long = when (cfg.cadence) {
        Cadence.LIVE -> if (snap.charging || snap.serviceRunning) 1_000 else 30_000
        Cadence.AMBIENT -> 30_000
        Cadence.BUDGET -> 15 * 60_000
    }
}

// ═══════════════ WT-01 · SCOPE — CH-01 CPU ═══════════════

class ScopeWidget : BenchGlanceWidget() {
    override val kind = WidgetKind.SCOPE

    @Composable override fun InstrumentPanel(tier: Tier, cfg: BenchConfig,
                                             snap: BenchSnapshot, sweep: Float, key: String) {
        val medium = resolvedMedium(cfg)
        val pal = WidgetPalettes.of(medium)
        val stale = snap.stale(cadenceMs(cfg, snap))
        val hash = snap.cpuHist.contentHash()

        BenchPanel(medium) {
            Header(medium, "CH-01", "CPU", live = !stale, fault = snap.tempC > 60f)
            Spacer(GlanceModifier.height(8.dp))

            Text(String.format(java.util.Locale.US, "%.1f%%", snap.cpuPct),
                HeroStyle.copy(color = if (stale) dimColor(medium) else inkColor(medium)),
                modifier = GlanceModifier.clickable(open("CH-01")))
            Spacer(GlanceModifier.height(6.dp))

            // T1: bare spark · T2+: gridded scope with y-axis (plan §4 WT-01)
            if (tier == Tier.T1) {
                BandBitmap(key, "spark", tier, medium, bandHeightDp = 26,
                    contentDescription = "CPU load ${snap.cpuPct.roundToInt()} percent, live trace") { c, w, h, d ->
                    c.spark(snap.cpuHist, pal.channel("CH-01"), w, h, d * 2f)
                }
            } else {
                BandBitmap(key, "scope|$hash", tier, medium, bandHeightDp = if (tier >= Tier.T3) 84 else 56,
                    contentDescription = "CPU load over ${cfg.traceWindowS} seconds") { c, w, h, d ->
                    c.scope(null, pal, w, h, snap.cpuHist, 100f,
                        showYLabels = tier >= Tier.T4,
                        sweep = sweep, noSignal = stale)
                }
            }
            Spacer(GlanceModifier.height(6.dp))

            Subline(medium, listOf(
                String.format(java.util.Locale.US, "%.2f GHz", snap.freqGHz),
                String.format(java.util.Locale.US, "%.1f°C", snap.tempC),
                if (tier >= Tier.T2) "gov schedutil" else ""), dim = stale)

            if (tier >= Tier.T2) {                              // thermal ramp band
                Spacer(GlanceModifier.height(6.dp))
                BandBitmap(key, "thermal", tier, medium, bandHeightDp = 8,
                    contentDescription = "thermal zone") { c, w, h, _ ->
                    c.thermalRamp(null, pal, w, h, snap.tempC)
                }
            }
            if (tier >= Tier.T3) {                              // core rail band — growth, not stretch
                Spacer(GlanceModifier.height(8.dp))
                BandBitmap(key, "cores", tier, medium,
                    bandHeightDp = snap.cores.size * 20,
                    contentDescription = "per-core load") { c, w, h, d ->
                    c.coreRailRows(null, pal, w, snap.cores, d * 20f)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Footer(medium, clockOf(snap.timestamp),
                right = "${cfg.traceWindowS}s window", lost = stale)
        }
    }
}

// ═══════════════ WT-02 · STACK — CH-02 MEMORY ═══════════════

class StackWidget : BenchGlanceWidget() {
    override val kind = WidgetKind.STACK

    @Composable override fun InstrumentPanel(tier: Tier, cfg: BenchConfig,
                                             snap: BenchSnapshot, sweep: Float, key: String) {
        val medium = resolvedMedium(cfg)
        val pal = WidgetPalettes.of(medium)
        val stale = snap.stale(cadenceMs(cfg, snap))
        val usedPct = (snap.memUsedGb / snap.memTotalGb * 100).roundToInt()

        BenchPanel(medium) {
            Header(medium, "CH-02", "MEMORY", live = !stale, status = {
                Text("$usedPct%", MetaStyle.copy(color = metaColor(medium)))
            })
            Spacer(GlanceModifier.height(8.dp))

            Text(String.format(java.util.Locale.US, "%.2f / %.0f GB", snap.memUsedGb, snap.memTotalGb),
                HeroStyle.copy(color = if (stale) dimColor(medium) else inkColor(medium)),
                modifier = GlanceModifier.clickable(open("CH-02")))
            Spacer(GlanceModifier.height(8.dp))

            // the cadastral bar — hatch patterns carry identity even in Blueprint
            BandBitmap(key, "map|${snap.memComposition.sumOf { it.bytes }}",
                tier, medium, bandHeightDp = if (tier >= Tier.T2) 20 else 14,
                contentDescription = "memory composition map") { c, w, h, d ->
                c.hatchBar(pal, w, h,
                    segs = snap.memComposition.map { it.bytes to it.pattern },
                    colors = snap.memComposition.map { pal.channel(it.chId) })
            }
            Spacer(GlanceModifier.height(6.dp))

            if (tier >= Tier.T2) {                              // pressure trace
                BandBitmap(key, "pressure|${snap.memHist.contentHash()}", tier, medium,
                    bandHeightDp = 24, contentDescription = "memory pressure") { c, w, h, d ->
                    c.spark(snap.memHist, pal.channel("CH-02"), w, h, d * 2f)
                }
                Spacer(GlanceModifier.height(6.dp))
            }

            Subline(medium, listOf(
                "zram ${snap.zramGb}", "swap ${snap.swapGb}",
                if (tier >= Tier.T2) "active ${usedPct}%" else ""), dim = stale)

            if (tier >= Tier.T3) {                              // top consumers → dossier
                Spacer(GlanceModifier.height(8.dp))
                Meta("── TOP CONSUMERS ──", dimColor(medium))
                snap.topConsumers.take(if (tier >= Tier.T4) 5 else 3).forEach { cons ->
                    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)
                            .clickable(open("dossier:${cons.index}")),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format(java.util.Locale.US, "%04d", cons.index),
                            MetaStyle.copy(color = dimColor(medium)))
                        Spacer(GlanceModifier.width(8.dp))
                        Text(cons.pkg, DataStyle.copy(color = inkColor(medium), fontSize = 12.sp))
                        Spacer(GlanceModifier.defaultWeight())
                        Data(fmtBytes(cons.rss), metaColor(medium))
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Footer(medium, clockOf(snap.timestamp), lost = stale)
        }
    }
}

private fun fmtBytes(v: Long): String = when {
    v >= 1 shl 30 -> String.format(java.util.Locale.US, "%.0f GB", v / 1073741824.0)
    v >= 1 shl 20 -> String.format(java.util.Locale.US, "%.0f MB", v / 1048576.0)
    else -> "$v KB"
}

// ═══════════════ WT-03 · FUEL — CH-04 POWER (wattage hero) ═══════════════

class FuelWidget : BenchGlanceWidget() {
    override val kind = WidgetKind.FUEL

    @Composable override fun InstrumentPanel(tier: Tier, cfg: BenchConfig,
                                             snap: BenchSnapshot, sweep: Float, key: String) {
        val medium = resolvedMedium(cfg)
        val pal = WidgetPalettes.of(medium)
        val stale = snap.stale(cadenceMs(cfg, snap))
        val critical = snap.batteryPct < 0.2f && !snap.charging
        // frame-swap pulse: charging OR critical alternates the lamp/knob each push
        val lampOn = if (snap.charging || critical) (snap.timestamp / 1000) % 2 == 0L else true

        BenchPanel(medium) {
            Header(medium, "CH-04", "POWER", live = !stale, fault = critical, status = {
                if (snap.charging) Meta("CHARGING", accentColor(medium))
            })
            Spacer(GlanceModifier.height(8.dp))

            if (cfg.wattHero) {                                 // the differentiator: the flow, not the tank
                Text(String.format(java.util.Locale.US, "≈ %+.2f W", snap.watts),
                    HeroStyle.copy(color = if (stale) dimColor(medium)
                        else if (snap.charging) chColor(medium, "CH-04") else inkColor(medium)))
                Spacer(GlanceModifier.height(4.dp))
                Text("${(snap.batteryPct * 100).roundToInt()}%",
                    DataStyle.copy(color = if (critical) faultColor(medium) else metaColor(medium)))
            } else {
                Text("${(snap.batteryPct * 100).roundToInt()}%",
                    HeroStyle.copy(color = if (critical) faultColor(medium) else inkColor(medium)))
            }
            Spacer(GlanceModifier.height(8.dp))

            BandBitmap(key, "fuel|${snap.batteryPct}|$lampOn", tier, medium, bandHeightDp = 34,
                contentDescription = "battery fuel gauge, ${(snap.batteryPct * 100).roundToInt()} percent") { c, w, h, d ->
                c.fuel(null, pal, w, h, snap.batteryPct, critical, snap.charging, lampOn)
            }
            Spacer(GlanceModifier.height(6.dp))

            if (tier >= Tier.T2) {                              // 6h signed wattage history
                BandBitmap(key, "watts|${snap.wattHist.contentHash()}", tier, medium,
                    bandHeightDp = if (tier >= Tier.T3) 48 else 36,
                    contentDescription = "wattage history") { c, w, h, d ->
                    c.wattTrace(null, pal, w, h, snap.wattHist, -8f, 10f)
                }
                Spacer(GlanceModifier.height(6.dp))
            }

            Subline(medium, listOf(
                "${snap.currentMa} mA",
                String.format(java.util.Locale.US, "%.3f V", snap.voltage),
                "${snap.remainingMin / 60}h ${snap.remainingMin % 60}m"), dim = stale)

            if (tier >= Tier.T4) {                              // spec rows (health ledger)
                Spacer(GlanceModifier.height(6.dp))
                Subline(medium, listOf("health good", "cycles 214", "design 5050 mAh"), dim = true)
            }
            Spacer(GlanceModifier.defaultWeight())
            Footer(medium, clockOf(snap.timestamp), lost = stale)
        }
    }
}

// ═══════════════ WT-04 · RASTER — CH-06 GPU (honest when locked) ═══════════════

class RasterWidget : BenchGlanceWidget() {
    override val kind = WidgetKind.RASTER

    @Composable override fun InstrumentPanel(tier: Tier, cfg: BenchConfig,
                                             snap: BenchSnapshot, sweep: Float, key: String) {
        val medium = resolvedMedium(cfg)
        val pal = WidgetPalettes.of(medium)
        val stale = snap.stale(cadenceMs(cfg, snap))
        val locked = snap.gpuRootLocked && !snap.rootAvailable
        val fitted = snap.gpuPct != null

        BenchPanel(medium) {
            Header(medium, "CH-06", "GPU", live = !stale && fitted && !locked,
                locked = locked)
            Spacer(GlanceModifier.height(8.dp))

            if (locked || !fitted) {
                // degrade into a datasheet, never a broken widget (plan §4 WT-04)
                BandBitmap(key, "locked", tier, medium, bandHeightDp = 34,
                    contentDescription = "channel locked") { c, w, h, d ->
                    c.lockedField(null, pal, w, h)
                }
                Spacer(GlanceModifier.height(8.dp))
                Meta(if (locked) "CHANNEL LOCKED" else "NOT FITTED", faultColor(medium))
                Spacer(GlanceModifier.height(6.dp))
                Subline(medium, listOf(
                    if (locked) "live clocks need root — showing capabilities"
                    else "no GPU node on this device"))
                Spacer(GlanceModifier.height(6.dp))
                Subline(medium, listOf(snap.gpuName, snap.gpuVulkan, "gles 3.2"))
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth().clickable(open("calibrate"))) {
                    Meta("[ GRANT IN APP ]", accentColor(medium))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${snap.gpuPct!!.roundToInt()}%", HeroStyle.copy(color = inkColor(medium)))
                    Spacer(GlanceModifier.width(12.dp))
                    Text("${snap.gpuMHz} MHz",
                        DataStyle.copy(color = chColor(medium, "CH-06")))
                }
                Spacer(GlanceModifier.height(6.dp))
                BandBitmap(key, "load|${snap.gpuHist.contentHash()}", tier, medium,
                    bandHeightDp = if (tier >= Tier.T2) 40 else 26,
                    contentDescription = "GPU load history") { c, w, h, d ->
                    c.spark(snap.gpuHist, pal.channel("CH-06"), w, h, d * 2f)
                }
                Spacer(GlanceModifier.height(6.dp))
                Subline(medium, listOf(snap.gpuName, snap.gpuVulkan))
                if (tier >= Tier.T2) {
                    Spacer(GlanceModifier.height(4.dp))
                    Subline(medium, listOf("opengl es 3.2", "2 shader procs",
                        if (tier >= Tier.T3) "bus 600 MHz" else ""), dim = true)
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Footer(medium, clockOf(snap.timestamp), lost = stale)
        }
    }
}

// ═══════════════ WT-05 · BENCH — all channels, one panel ═══════════════

class BenchWidgetAll : BenchGlanceWidget() {
    override val kind = WidgetKind.BENCH

    @Composable override fun InstrumentPanel(tier: Tier, cfg: BenchConfig,
                                             snap: BenchSnapshot, sweep: Float, key: String) {
        val medium = resolvedMedium(cfg)
        val pal = WidgetPalettes.of(medium)
        val stale = snap.stale(cadenceMs(cfg, snap))

        BenchPanel(medium) {
            // masthead strip → Overview
            Row(GlanceModifier.fillMaxWidth().clickable(open("overview")),
                verticalAlignment = Alignment.CenterVertically) {
                Text("DEVICEINSIGHT · BENCH", MetaStyle.copy(color = inkColor(medium)))
                Spacer(GlanceModifier.defaultWeight())
                Lamp(medium, on = !stale, fault = snap.warning())
                Spacer(GlanceModifier.width(6.dp))
                Meta(clockOf(snap.timestamp).substring(0, 5), dimColor(medium))
            }
            Spacer(GlanceModifier.height(8.dp))

            if (tier == Tier.T2) {
                // compact: configurable 4-row ledger strip
                cfg.compactChannels.take(4).forEach { ch ->
                    ChannelRow(medium, ch, snap, stale)
                    Spacer(GlanceModifier.height(6.dp))
                }
            } else {
                // T3+: tile grid — five at T3, six at T4+
                val chans = if (tier == Tier.T3)
                    listOf("CH-01", "CH-02", "CH-04", "CH-03", "CH-05")
                else listOf("CH-01", "CH-02", "CH-03", "CH-04", "CH-05", "CH-06")
                Column {
                    chans.chunked(2).forEach { pair ->
                        Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                            pair.forEach { ch ->
                                ChannelTile(medium, ch, snap, tier, key,
                                    GlanceModifier.defaultWeight().padding(2.dp))
                            }
                            if (pair.size == 1) Box(GlanceModifier.defaultWeight())
                        }
                    }
                }
                if (tier == Tier.T5) {                           // the bench band
                    Spacer(GlanceModifier.height(6.dp))
                    BandBitmap(key, "cores", tier, medium,
                        bandHeightDp = snap.cores.size * 18,
                        contentDescription = "per-core load") { c, w, h, d ->
                        c.coreRailRows(null, pal, w, snap.cores, d * 18f)
                    }
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            Footer(medium, clockOf(snap.timestamp),
                right = if (snap.warning()) "1 channel warning" else "all channels nominal",
                lost = stale)
        }
    }

    @Composable
    private fun ChannelRow(medium: Medium, ch: String, snap: BenchSnapshot, stale: Boolean) {
        Row(GlanceModifier.fillMaxWidth().clickable(open(ch)),
            verticalAlignment = Alignment.CenterVertically) {
            Tick(medium, ch)
            Spacer(GlanceModifier.width(6.dp))
            Text(chName(ch), MetaStyle.copy(color = metaColor(medium)))
            Spacer(GlanceModifier.defaultWeight())
            Data(channelValue(ch, snap), if (stale) dimColor(medium) else inkColor(medium))
            Spacer(GlanceModifier.width(10.dp))
            Text(channelSub(ch, snap), MetaStyle.copy(color = dimColor(medium)))
        }
    }

    @Composable
    private fun ChannelTile(medium: Medium, ch: String, snap: BenchSnapshot,
                            tier: Tier, key: String, modifier: GlanceModifier) {
        val pal = WidgetPalettes.of(medium)
        Column(modifier.fillMaxHeight().background(
            androidx.compose.ui.graphics.Color(pal.surface)
        ).padding(6.dp).clickable(open(ch))) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Tick(medium, ch)
                Spacer(GlanceModifier.width(4.dp))
                Meta(chName(ch), dimColor(medium))
            }
            Spacer(GlanceModifier.height(4.dp))
            Text(channelValue(ch, snap), HeroStyle.copy(fontSize = 22.sp,
                color = inkColor(medium)))
            when (ch) {
                "CH-01" -> BandBitmap(key, "t_cpu|${snap.cpuHist.contentHash()}", tier, medium,
                    18, "cpu trace") { c, w, h, d -> c.spark(snap.cpuHist, pal.channel(ch), w, h, d * 2f) }
                "CH-02" -> BandBitmap(key, "t_mem|${snap.memComposition.sumOf { it.bytes }}",
                    tier, medium, 10, "memory map") { c, w, h, _ ->
                    c.hatchBar(pal, w, h, snap.memComposition.map { it.bytes to it.pattern },
                        snap.memComposition.map { pal.channel(it.chId) }) }
                "CH-03" -> BandBitmap(key, "t_net|${snap.netHist.contentHash()}", tier, medium,
                    18, "network trace") { c, w, h, d -> c.spark(snap.netHist, pal.channel(ch), w, h, d * 2f) }
                "CH-04" -> BandBitmap(key, "t_pwr|${snap.batteryPct}", tier, medium, 14,
                    "fuel gauge") { c, w, h, _ ->
                    c.fuel(null, pal, w, h, snap.batteryPct, false, snap.charging, true) }
                "CH-06" -> if (snap.gpuPct != null)
                    BandBitmap(key, "t_gpu|${snap.gpuHist.contentHash()}", tier, medium,
                        18, "gpu trace") { c, w, h, d ->
                        c.spark(snap.gpuHist, pal.channel(ch), w, h, d * 2f) }
            }
            Spacer(GlanceModifier.height(2.dp))
            Text(channelSub(ch, snap), MetaStyle.copy(fontSize = 9.sp, color = dimColor(medium)))
        }
    }

    private fun chName(ch: String) = when (ch) {
        "CH-01" -> "CPU"; "CH-02" -> "MEM"; "CH-03" -> "NET"
        "CH-04" -> "PWR"; "CH-05" -> "STO"; else -> "GPU"
    }
    private fun channelValue(ch: String, s: BenchSnapshot): String = when (ch) {
        "CH-01" -> String.format(java.util.Locale.US, "%.1f%%", s.cpuPct)
        "CH-02" -> "${(s.memUsedGb / s.memTotalGb * 100).roundToInt()}%"
        "CH-03" -> "↓${fmtBytes(s.netDown)}/s"
        "CH-04" -> String.format(java.util.Locale.US, "≈ %+.2f W", s.watts)
        "CH-05" -> "${(s.stoUsedGb / s.stoTotalGb * 100).roundToInt()}%"
        else -> s.gpuPct?.let { "$it%" } ?: "—"
    }
    private fun channelSub(ch: String, s: BenchSnapshot): String = when (ch) {
        "CH-01" -> String.format(java.util.Locale.US, "%.2f GHz · %.0f°C", s.freqGHz, s.tempC)
        "CH-02" -> String.format(java.util.Locale.US, "%.1f/%.0f GB", s.memUsedGb, s.memTotalGb)
        "CH-03" -> "↑${fmtBytes(s.netUp)}/s"
        "CH-04" -> "${(s.batteryPct * 100).roundToInt()}% · ${s.voltage} V"
        "CH-05" -> String.format(java.util.Locale.US, "%.0f/%.0f GB", s.stoUsedGb, s.stoTotalGb)
        else -> s.gpuMHz?.let { "$it MHz" } ?: "not fitted"
    }
}
```

---

## 6 · `BenchUpdater.kt` — cadence ladder, worker, receivers

```kotlin
package com.ivarna.deviceinsight.widget.bench

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.update
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.TimeUnit

object BenchUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    private val widgets = listOf(
        ScopeWidget(), StackWidget(), FuelWidget(), RasterWidget(), BenchWidgetAll()
    )

    /** Call from MonitorService.onCreate(). */
    fun start(context: Context) {
        if (started) return
        started = true
        val appCtx = context.applicationContext
        scope.launch {
            val mgr = GlanceAppWidgetManager(appCtx)
            widgets.forEach { widget ->
                launch {
                    val ids = mgr.getGlanceIds(widget.javaClass)
                    ids.forEach { id ->
                        launch { subscribe(appCtx, widget, id) }
                    }
                }
            }
        }
        // budget ladder — WorkManager regardless of service state
        WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
            "bench-budget", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<BenchWorker>(15, TimeUnit.MINUTES).build()
        )
    }

    /** One collector per widget: throttle by cadence, then update() → fresh bitmaps + text. */
    private suspend fun subscribe(ctx: Context, widget: BenchGlanceWidget, id: androidx.glance.GlanceId) {
        var lastPush = 0L
        MonitorBus.snapshot.collectLatest { snap ->
            val cfg = BenchState.config(ctx, widget, id)
            val cadence = widget.cadenceMs(cfg, snap)
            val now = System.currentTimeMillis()
            val pulseDue = (snap.charging || snap.batteryPct < 0.2f) && now - lastPush > 1_000
            if (now - lastPush >= cadence || pulseDue || snap.stale(cadence)) {
                widget.update(ctx, id)     // re-runs provideGlance → fresh snapshot, fresh frames
                lastPush = now
            } else {
                kotlinx.coroutines.delay(minOf(cadence - (now - lastPush), 1_000))
            }
        }
    }

    /** WorkManager entry — 15 min BUDGET push for widgets whose service is down. */
    suspend fun pushBudget(context: Context) {
        val mgr = GlanceAppWidgetManager(context)
        widgets.forEach { widget ->
            runCatching {
                mgr.getGlanceIds(widget.javaClass).forEach { it ->
                    widget.update(context, it)
                }
            }
        }
    }
}

class BenchWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        BenchUpdater.pushBudget(applicationContext)
        return Result.success()
    }
}

// ─────────────── receivers (one per widget — manifest-registered) ───────────────

class ScopeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ScopeWidget()
    override fun onDeleted(context: Context, intents: Array<android.content.Intent>) {
        super.onDeleted(context, intents)
        BenchFrames.clear()                                  // reclaim bitmap memory
    }
}
class StackWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = StackWidget() }
class FuelWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = FuelWidget() }
class RasterWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = RasterWidget() }
class BenchWidgetAllReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = BenchWidgetAll() }
```

**Manifest + provider XML** (repeat block per widget, swap class/label/xml):

```xml
<application>
  <receiver android:name=".widget.bench.ScopeWidgetReceiver"
            android:exported="true"
            android:label="SCOPE — live CPU">
    <intent-filter>
      <action android:name="android.appwidget.action.APPWIDGET_UPDATE"/>
    </intent-filter>
    <meta-data android:name="android.appwidget.provider"
               android:resource="@xml/widget_scope"/>
  </receiver>

  <activity android:name=".widget.bench.BenchConfigActivity"
            android:exported="true"
            android:theme="@style/Theme.DeviceInsight">
    <intent-filter>
      <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE"/>
    </intent-filter>
  </activity>
</application>
```

```xml
<!-- res/xml/widget_scope.xml -->
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="140dp"
    android:minHeight="140dp"
    android:targetCellWidth="2"
    android:targetCellHeight="2"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:widgetFeatures="reconfigurable"
    android:updatePeriodMillis="0"
    android:configure="com.ivarna.deviceinsight.widget.bench.BenchConfigActivity"
    android:previewImage="@drawable/preview_scope_paper"/>
```

**MainActivity route handling** (one block in `onCreate`):

```kotlin
intent?.getStringExtra("route")?.let { route ->   // Glance actionParameters land as extras
    when {
        route.startsWith("CH-") -> navigate(ChannelRoute(route))
        route == "overview"     -> navigate(Overview)
        route.startsWith("dossier:") -> navigate(DossierRoute(route.removePrefix("dossier:").toInt()))
        route == "calibrate"    -> navigate(Calibration)
    }
}
```

---

## 7 · `BenchConfigActivity.kt` — animated configuration

This is where **real Compose animation** lives: a live preview built from the actual CALIPER components — rolling odometer, pulsing lamp, pen-sweeping trace — re-rendering the widget in real time as the user toggles media, cadence, and hero.

```kotlin
package com.ivarna.deviceinsight.widget.bench

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class BenchConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidget.INVALID_APPWIDGET_ID
        ) ?: AppWidget.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidget.INVALID_APPWIDGET_ID) { finish(); return }

        val kind = intent?.getStringExtra("kind")?.let {
            runCatching { WidgetKind.valueOf(it) }.getOrNull()
        } ?: WidgetKind.SCOPE
        val widget: BenchGlanceWidget = when (kind) {
            WidgetKind.SCOPE -> ScopeWidget(); WidgetKind.STACK -> StackWidget()
            WidgetKind.FUEL -> FuelWidget(); WidgetKind.RASTER -> RasterWidget()
            WidgetKind.BENCH -> BenchWidgetAll()
        }

        setContent {
            var cfg by remember { mutableStateOf(BenchConfig()) }
            var medium by remember { mutableStateOf(Medium.PAPER) }

            CaliperTheme(medium) {
                ConfigScreen(
                    kind = kind, cfg = cfg, medium = medium,
                    onCfg = { cfg = it },
                    onMedium = { medium = it; cfg = cfg.copy(medium = it, followSystem = false) },
                    onFollowSystem = { cfg = cfg.copy(followSystem = it) },
                    onCommit = {
                        scope.launch {
                            val glanceId = GlanceAppWidgetManager(this@BenchConfigActivity)
                                .getGlanceIdBy(appWidgetId)
                            BenchState.save(this@BenchConfigActivity, widget, glanceId, cfg)
                            widget.update(this@BenchConfigActivity, glanceId)
                            setResult(RESULT_OK, Intent().putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                            finish()
                        }
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

// ─────────────── live demo feed (drives the animated preview) ───────────────

@Composable
private fun rememberDemoSnapshot(): BenchSnapshot {
    var t by remember { mutableStateOf(0.0) }
    var snap by remember { mutableStateOf(BenchDemo.tick(0.0)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)                                     // preview rolls at LIVE cadence
            t += 0.9
            snap = BenchDemo.tick(t)
        }
    }
    return snap
}

object BenchDemo {
    private val cpu = ArrayDeque(List(60) { 30f + Random.nextFloat() * 12f })
    private val mem = ArrayDeque(List(60) { 0.56f + Random.nextFloat() * 0.02f })
    private val net = ArrayDeque(List(60) { Random.nextFloat() })
    private val watt = ArrayDeque(List(60) { -3f + Random.nextFloat() })

    fun tick(t: Double): BenchSnapshot {
        fun push(q: ArrayDeque<Float>, v: Float) { q.removeFirst(); q.addLast(v) }
        val cpuNow = (38 + 26 * sin(t) + 6 * sin(t * 3.7)).toFloat().coerceIn(3f, 99f)
        push(cpu, cpuNow); push(mem, (0.56f + 0.02f * sin(t * 0.5)).toFloat())
        push(net, Random.nextFloat()); push(watt, (-3.4f + 1.5f * sin(t)).toFloat())
        return BenchSnapshot(
            cpuPct = cpuNow, cpuHist = cpu.toList(), freqGHz = 2.84f, tempC = (44 + 6 * sin(t * 0.8)).toFloat(),
            cores = List(8) { i -> CoreReading(i, (cpuNow * (0.4f + Random.nextFloat())).coerceAtMost(100f),
                (1800 + (600 * Random.nextFloat()).toLong()) * 1000) },
            memUsedGb = 6.81f, memTotalGb = 12f, memHist = mem.toList(),
            memComposition = listOf(
                MemSeg("active", 4_510_974_771L, HatchPattern.SOLID, "CH-02"),
                MemSeg("cached", 2_042_239_462L, HatchPattern.DIAGONAL, "CH-03"),
                MemSeg("zram", 1_288_490_188L, HatchPattern.CROSS, "CH-04"),
                MemSeg("free", 5_583_457_484L, HatchPattern.NONE, "CH-05")),
            zramGb = 1.2f, swapGb = 0.4f,
            topConsumers = listOf(
                Consumer(142, "com.android.chrome", 312 * 1048576L),
                Consumer(147, "com.spotify.music", 480 * 1048576L),
                Consumer(201, "system", 918 * 1048576L)),
            netDown = 18_100_000, netUp = 2_400_000, netHist = net.toList(),
            batteryPct = 0.78f, watts = -3.42f, voltage = 4.102f, currentMa = -812,
            remainingMin = 372, wattHist = watt.toList(),
            gpuPct = 71f, gpuMHz = 848, gpuHist = net.toList().map { it * 100f },
            gpuName = "adreno 740", gpuVulkan = "vulkan 1.3"
        )
    }
}

// ─────────────── the screen ───────────────

@Composable
fun ConfigScreen(
    kind: WidgetKind, cfg: BenchConfig, medium: Medium,
    onCfg: (BenchConfig) -> Unit, onMedium: (Medium) -> Unit,
    onFollowSystem: (Boolean) -> Unit,
    onCommit: () -> Unit, onCancel: () -> Unit
) {
    val snap = rememberDemoSnapshot()

    Column(Modifier.fillMaxSize().caliperGrid()
        .verticalScroll(rememberScrollState())) {

        Masthead()
        ScreenHeader("№ W-${kind.ordinal + 1} — ${kind.name}",
            "Calibrate ${kind.name.lowercase()}.", "placed on the bench — preview is live")

        // ── LIVE PREVIEW: real CALIPER components, real animation ──
        Text("PREVIEW", style = Caliper.type.meta, color = Caliper.colors.ink40,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        AnimatedContent(                                  // medium crossfade + panel swap
            targetState = medium,
            transitionSpec = {
                (fadeIn(tween(CaliperMotion.tBase)) togetherWith fadeOut(tween(CaliperMotion.tBase)))
            }, label = "medium"
        ) { m ->
            PreviewPanel(kind = kind, snap = snap, cfg = cfg, medium = m,
                modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(Modifier.height(20.dp))

        // ── media picker: three LIVE mini-panels (never color dots) ──
        PanelCard(title = "MEDIA") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediumChip(Medium.PAPER, "PAPER", medium == Medium.PAPER, onMedium)
                MediumChip(Medium.CARBON, "CARBON", medium == Medium.CARBON, onMedium)
                MediumChip(Medium.BLUEPRINT, "BLUEPRINT", medium == Medium.BLUEPRINT, onMedium)
            }
            Spacer(Modifier.height(10.dp))
            DipSwitch(cfg.followSystem, {
                onFollowSystem(it)
            }, label = "follow system (paper/carbon)")
        }

        Spacer(Modifier.height(12.dp))
        PanelCard(title = "CADENCE") {
            SegKey(listOf(Cadence.LIVE, Cadence.AMBIENT, Cadence.BUDGET),
                cfg.cadence, { onCfg(cfg.copy(cadence = it)) },
                labelFor = { it.name })
            Spacer(Modifier.height(8.dp))
            Text(when (cfg.cadence) {
                Cadence.LIVE -> "1 s while charging or service running · traces every 5 s"
                Cadence.AMBIENT -> "30 s while screen on"
                Cadence.BUDGET -> "every 15 min — battery-first"
            }, style = Caliper.type.meta, color = Caliper.colors.ink40)
        }

        Spacer(Modifier.height(12.dp))
        PanelCard(title = "INSTRUMENT") {
            SegKey(listOf(60, 300), cfg.traceWindowS,
                { onCfg(cfg.copy(traceWindowS = it)) }, labelFor = { "${it}s" })
            if (kind == WidgetKind.FUEL) {
                Spacer(Modifier.height(10.dp))
                DipSwitch(cfg.wattHero, { onCfg(cfg.copy(wattHero = it)) },
                    label = "wattage hero (off = percent hero)")
            }
            if (kind == WidgetKind.BENCH) {
                Spacer(Modifier.height(10.dp))
                Text("COMPACT ROWS — pick four channels", style = Caliper.type.meta,
                    color = Caliper.colors.ink40)
                Spacer(Modifier.height(6.dp))
                SegKey(listOf("CPU+MEM+PWR+NET", "CPU+GPU+PWR+STO"),
                    if (cfg.compactChannels.contains("CH-06")) "CPU+GPU+PWR+STO" else "CPU+MEM+PWR+NET",
                    { sel ->
                        onCfg(cfg.copy(compactChannels =
                            if (sel.startsWith("CPU+GPU")) listOf("CH-01", "CH-06", "CH-04", "CH-05")
                            else listOf("CH-01", "CH-02", "CH-04", "CH-03")))
                    })
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HardKey("CANCEL", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.weight(1f), onClick = onCancel)
            HardKey("PLACE ON BENCH", variant = HardKeyVariant.PRIMARY,
                modifier = Modifier.weight(1f), onClick = onCommit)
        }
        EndOfSheet()
    }
}

// ── the preview itself — OdometerText rolls, LED pulses, ScopeTrace sweeps ──

@Composable
private fun PreviewPanel(kind: WidgetKind, snap: BenchSnapshot, cfg: BenchConfig,
                         medium: Medium, modifier: Modifier = Modifier) {
    // renders inside CaliperTheme of the chosen medium, so just use Caliper.colors
    val c = Caliper.colors
    Column(modifier.fillMaxWidth().background(c.panel).border(1.dp, c.hairline).padding(12.dp)) {
        when (kind) {
            WidgetKind.SCOPE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelTick(Channels.CPU)
                    Spacer(Modifier.width(6.dp))
                    Text("CH-01 · CPU", style = Caliper.type.meta, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    LedDot(color = c.accent)                       // pulsing — real animation
                }
                Spacer(Modifier.height(6.dp))
                OdometerText(                                      // digits roll every tick
                    String.format(java.util.Locale.US, "%.1f%%", snap.cpuPct),
                    style = Caliper.type.readoutL)
                Spacer(Modifier.height(6.dp))
                ScopeTrace(                                         // pen-sweeps on medium change
                    values = snap.cpuHist, channel = Channels.CPU,
                    yMax = 100f, windowLabel = "${cfg.traceWindowS}s",
                    valueFormat = { Fmt.pct(it, 1) },
                    timeLabelFor = { f -> "-${((1f - f) * cfg.traceWindowS).toInt()}s" },
                    height = 140.dp)
            }
            WidgetKind.STACK -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelTick(Channels.MEMORY)
                    Spacer(Modifier.width(6.dp))
                    Text("CH-02 · MEMORY", style = Caliper.type.meta, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    Text("57%", style = Caliper.type.meta, color = c.ink60)
                }
                Spacer(Modifier.height(6.dp))
                OdometerText(String.format(java.util.Locale.US, "%.2f / 12 GB", snap.memUsedGb),
                    style = Caliper.type.readoutL)
                Spacer(Modifier.height(8.dp))
                HatchBar(segments = snap.memComposition.map {
                    HatchSegment(it.label, it.bytes,
                        when (it.chId) { "CH-02" -> c.channel(Channels.MEMORY)
                            "CH-03" -> c.channel(Channels.NETWORK)
                            "CH-04" -> c.channel(Channels.POWER)
                            else -> c.ink40 },
                        it.pattern)
                })
            }
            WidgetKind.FUEL -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelTick(Channels.POWER)
                    Spacer(Modifier.width(6.dp))
                    Text("CH-04 · POWER", style = Caliper.type.meta, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    LedDot(color = c.accent)
                }
                Spacer(Modifier.height(6.dp))
                if (cfg.wattHero) {
                    OdometerText(String.format(java.util.Locale.US, "≈ %+.2f W", snap.watts),
                        style = Caliper.type.readoutL)
                    Spacer(Modifier.height(6.dp))
                    LinearGauge(fraction = snap.batteryPct,
                        voltage = String.format(java.util.Locale.US, "%.3f V", snap.voltage))
                } else {
                    OdometerText("78", style = Caliper.type.readoutL)
                    Spacer(Modifier.height(6.dp))
                    LinearGauge(fraction = snap.batteryPct)
                }
            }
            WidgetScopeRASTER_PLACEHOLDER -> Unit
            WidgetScopeBENCH_PLACEHOLDER -> Unit
            else -> {
                // RASTER + BENCH previews reuse CoreRail / ThermalGauge / channel rows
                // exactly like the widget bands — same components, same grammar.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChannelTick(Channels.GPU)
                    Spacer(Modifier.width(6.dp))
                    Text("CH-06 · GPU", style = Caliper.type.meta, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    LedDot()
                }
                Spacer(Modifier.height(6.dp))
                OdometerText("71% · 848 MHz", style = Caliper.type.readoutL)
                Spacer(Modifier.height(8.dp))
                CoreRail(snap.cores.take(4))
                Spacer(Modifier.height(8.dp))
                ThermalGauge(snap.tempC)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("upd ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())} · ${cfg.traceWindowS}s window",
            style = Caliper.type.meta, color = c.ink40)
    }
}

@Composable
private fun MediumChip(medium: Medium, label: String, selected: Boolean, onPick: (Medium) -> Unit) {
    // a LIVE mini-panel per medium — actual colors, actual hairline — never a color dot
    val bg = when (medium) {
        Medium.PAPER -> Color(0xFFFBF9F3); Medium.CARBON -> Color(0xFF1C1B17)
        else -> Color(0xFF12314E)
    }
    val fg = when (medium) {
        Medium.PAPER -> Color(0xFF191713); Medium.CARBON -> Color(0xFFEDE7DA)
        else -> Color(0xFFEAF2FF)
    }
    Column(Modifier.width(96.dp).background(bg)
        .border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFFF4D00) else fg.copy(alpha = 0.2f))
        .clickable { onPick(medium) }.padding(8.dp)) {
        Text(label, style = Caliper.type.meta.copy(fontSize = 10.sp), color = fg.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Row {
            listOf(0xFFE5482B, 0xFF2E5BE0, 0xFF0E9F6E).forEach {
                Box(Modifier.size(6.dp).background(
                    if (medium == Medium.BLUEPRINT) fg else Color(it.toArgb())))
                Spacer(Modifier.width(3.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("38.4%", style = Caliper.type.dataS.copy(fontSize = 13.sp), color = fg)
    }
}
```

> ⚠️ Delete the two placeholder lines (`WidgetScopeRASTER_PLACEHOLDER`/`BENCH_PLACEHOLDER`) — they're markers; the `else ->` branch is the RASTER/BENCH preview. Also note the pattern: **previews are built from the same components as the app, never screenshots** — so widget, preview, and app can never disagree.

---

## 8 · The Animation System (under Glance constraints)

| Where | Technique | Component |
|---|---|---|
| Widget placement | **Calibration sweep** — trace draws in over the first ~6 pushes (`placementFraction`) | `BenchArt.scope(sweep=…)` |
| Charging / critical | **Lamp frame-swap** — knob & lamp alternate ON/OFF each push, 1 Hz | `FuelWidget`, `subscribe(pulseDue)` |
| Signal loss | **Morph** — values dim to ink/40, flat trace, `SIGNAL LOST` stamp | `stale()` branches |
| Config preview | **Real Compose** — `OdometerText` rolls, `LedDot` pulses, `ScopeTrace` pen-sweeps, `AnimatedContent` crossfades media | `PreviewPanel` |
| Content change | **Pen head** — square 3dp pen always marks the live end of every trace | `BenchArt.spark/scope` |

The principle, stated once in the docs and honored everywhere: *instruments animate by updating — one frame per cadence, like a multimeter's refresh. Everything that must tween lives in the app or the config preview.*

---

## 9 · Efficiency Ledger

1. **Render only what changed.** Band cache keys include `contentHash` — an unchanged CPU trace never re-rasterizes; only Glance text updates.
2. **Off-main rendering**, single pass, `Dispatchers.Default`, `LruCache(48)` band-bitmaps, cleared on widget delete.
3. **Cadence ladder**: 1s (charging/service) → 30s (screen-on) → 15min (WorkManager) → SIGNAL LOST (no work at all — staleness is computed, not polled).
4. **One bus, many instruments** — the monitor service pushes one `BenchSnapshot`; five widgets subscribe with independent throttles.
5. **Text stays text** — font scale, TalkBack, and locale come free; bitmaps carry only graphics.
6. **No update storms** — `collectLatest` + delay-until-due; a widget never recomposes faster than its own cadence.

---

## 10 · Quick Reference

| Plan item | Implementation |
|---|---|
| WT-01 SCOPE | `ScopeWidget` — spark (T1) → gridded scope + thermal (T2+) → + core rail (T3+) |
| WT-02 STACK | `StackWidget` — hatch map + pressure trace + consumer ledger (T3+) |
| WT-03 FUEL | `FuelWidget` — wattage/percent hero toggle, signed watt trace, pulse frames |
| WT-04 RASTER | `RasterWidget` — dual readout, or locked datasheet degrade |
| WT-05 BENCH | `BenchWidgetAll` — 4-row strip (T2) → tile grid (T3/T4) → + core band (T5) |
| 3 media + follow | `WidgetPalettes`, `resolvedMedium()` |
| Cadence ladder | `BenchGlanceWidget.cadenceMs()` + `BenchUpdater.subscribe()` |
| SIGNAL LOST | `BenchSnapshot.stale()` + dim/stamp branches |
| Calibration sweep | `BenchState.placementFraction()` → `scope(sweep=…)` |
| Config + previews | `BenchConfigActivity` — live animated CALIPER preview |

---

*Same instrument, smaller bench. Label everything. `upd` or it didn't happen.*
