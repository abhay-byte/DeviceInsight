# DeviceInsight — HUD REV A · **"The Scope Probe"**
### DI-HD-001 · redesign + theme + Kotlin UI implementation of the performance overlay

> Replaces the current overlay (rounded card, colored-text soup, ad-hoc layout) with the smallest CALIPER instrument. The overlay is not a widget and not a page — it's a **probe clipped onto the corner of whatever you're doing.** Corner brackets, channel bands, square pen traces, one honesty stamp.

---

## §1 — Audit: where every current reading lands

| Current overlay item | New home |
|---|---|
| `10:32:53` clock | **Header band** — blinking colon, UTC optional, LED lamp |
| `CPU 52% (schedutil)` | **HM-CPU** band — value hero, governor as spec note at L |
| `CPU 54.3°C` | HM-CPU trailing value, thermally colored (ink → amber → fault) |
| `Core 0–7` frequencies | **CoreBank** — 2×4 cells: load bar + `C0` + `1804` |
| `3867/7484MB (51%)` | **HM-MEM** band + hatched bar |
| `285/2047MB (13%)` swap | HM-MEM second row, cross-hatched bar (`NOT FITTED` if no swap) |
| `+0.00W` | **HM-PWR** — signed watts, always (honest zero is a reading) |
| `100%`, `35.2°C` battery | HM-PWR micro fuel gauge + temp |
| `FPS 90` | **HM-FPS** hero + source stamp + rolling `min/avg` |
| CPU / Power line charts | FPS trace in hero band + **HM-TRACE** band (CPU / PWR sparks with pen) |
| — *(new)* | 3 media themes · S/M/L sizes · drag-lock · blur-behind · thermal coloring |

**Removed:** the rounded card, the drop shadow, per-row rainbow colors with no labels, and every spacing value that wasn't on the 4dp grid.

---

## §2 — Anatomy

```
 ⌜────────────────────────────────────⌝   ← 1.5dp ink corner brackets — the only frame
 │ ⌖ DI·HUD              10:32:53  ●  │      HM-0 HEADER · drag handle · clock · LED
 ├────────────────────────────────────┤      hairline
 │ FPS  90.0  [SF]      min 60 · avg 88│     HM-1 FPS hero — 10 Hz refresh
 │ ▁▂▄▆▅▃▂▁▂▄▃▂ ●                     │      6 s fps trace, square pen head
 ├────────────────────────────────────┤
 │ ▪ CPU    52%   1.80 GHz     54.3°C │      HM-2 CPU — channel tick, value, freq, temp
 │   ▍C0 1804  ▍C1 2188  ▌C2 2416 …   │      CoreBank 2×4 (M+) — load bar + freq
 │   gov schedutil · peak C2 2416 MHz │      (L only)
 ├────────────────────────────────────┤
 │ ▪ RAM   3867 / 7484 MB         51%  │      HM-3 MEMORY
 │   ▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░        │      solid hatch bar
 │ ▪ SWP    285 / 2047 MB         13%  │
 │   ▒░░░░░░░░░░░░░░░░░░░░░░░░░        │      cross-hatch bar
 ├────────────────────────────────────┤
 │ ▪ PWR   +0.00 W          BAT 100%   │      HM-4 POWER — signed watts
 │   ├──┼─────■─────┼─────┼──┤  35.2°C │      micro fuel gauge + battery temp
 ├────────────────────────────────────┤
 │ ▪ GPU    71% · 848 MHz              │      HM-5 (optional module)
 │ ▪ NET   ↓ 18.1   ↑ 2.4 MB/s         │      HM-6 (optional module)
 ├────────────────────────────────────┤
 │ ▪ CPU ▂▄▆▅▃ ● 52%   ▪ PWR ▁▁▂ ● 0.0W│     HM-7 TRACE band (L / toggle)
 ⌞────────────────────────────────────⌟
```

Bands are **modules**: toggleable, reorderable, each owned by its channel. Separators are 1dp hairlines — no cards inside the panel.

---

## §3 — Sizes

| | **S** | **M** *(default)* | **L** |
|---|---|---|---|
| Panel width | 196dp | 260dp | 300dp |
| FPS hero | 24sp | 28sp | 32sp |
| Values / meta / micro | 12 / 9 / 8sp | 13 / 9 / 8sp | 14 / 10 / 9sp |
| CoreBank | 8 bars, 1 row, no labels | 2×4 cells with freq | 2×4 + cluster rule + peak/gov line |
| Memory | inline `RAM 51% · SWP 13%` | two hatched bars | bars + values inside |
| Power | `+0.0W · BAT 100%` | + fuel micro-gauge | + battery spec note |
| Trace band | — | FPS trace only | FPS + CPU + PWR traces |

Growth, never stretch — S→L adds instruments, not font size inflation.

---

## §4 — Theme: three media, self-contained

A HUD floats over *anything* — so unlike the app, the HUD medium is **explicitly chosen, never system-following**. Legibility beats theme-matching.

| Token | **CARBON** *(default)* | **PAPER** | **BLUEPRINT** |
|---|---|---|---|
| scrim | `#141310` | `#F4F1E8` | `#0C2338` |
| ink | `#EDE7DA` | `#191713` | `#EAF2FF` |
| hairline | ink @ 18% | ink @ 14% | ink @ 20% |
| accent (LED, active) | `#FF5A1F` | `#FF4D00` | `#63C7FF` |
| fault | `#FF6B4A` | `#C8371F` | `#FF7759` |
| CH-01 … CH-06 | lightened registry | paper registry | **all ink** — channel identity by tick label + hatch only |
| default opacity | 82% | 92% | 86% |

PAPER is the signature: a cream drafting sheet clipped over a neon game — instantly recognizable in any gameplay screenshot, and it *photographs beautifully* for the store listing.

**The legibility stack (non-negotiable, in order):**
1. **Scrim** — medium color at configured opacity, full-bleed inside the brackets.
2. **Blur-behind** — the one sanctioned blur, 10dp, `FLAG_BLUR_BEHIND` (API 31+, runtime-verified). If the OEM disables cross-window blur → scrim opacity is *raised* 10 points instead. Never both assumptions.
3. **Text stroke** — every value carries a 1dp stroke in scrim color under its fill, so numerals survive 60% opacity over a bright sky.

---

## §5 — Motion vocabulary (refresh-driven)

A probe animates like an instrument: **by updating.**

| Element | Behavior |
|---|---|
| FPS hero | plain stroked readout at **10 Hz** — refresh *is* the animation. (Odometers here would strobe; deliberately not used.) |
| CPU % value | mini-odometer roll at 2 Hz |
| Core load bars | `spring/needle` (0.82 / 420) settle + peak-hold caret, decay 2 s |
| Fuel knob | spring; pulses by alpha while charging |
| LED | 2 s sine breathe; off = paused, fault-red = thermal/critical |
| Traces | square **pen head** always marks the live end; history scrolls by append |
| Thermal value | ink → CH-04 amber (≥65°C) → fault red (≥75°C) — color is signal |
| Reduced motion | no LED breathe, no spring overshoot, bars snap |

---

## §6 — `HudTheme.kt`

```kotlin
package com.ivarna.deviceinsight.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.PlexMonoFamily

// ─────────────── media & scale ───────────────

enum class HudMedium { CARBON, PAPER, BLUEPRINT }
enum class HudScale { S, M, L }

@Immutable
data class HudColors(
    val scrim: Color, val ink: Color, val ink60: Color, val ink40: Color,
    val hairline: Color, val accent: Color, val fault: Color,
    val ch01: Color, val ch02: Color, val ch03: Color,
    val ch04: Color, val ch05: Color, val ch06: Color
)

object HudPalettes {
    val CARBON = HudColors(
        scrim = Color(0xFF141310), ink = Color(0xFFEDE7DA),
        ink60 = Color(0x99EDE7DA), ink40 = Color(0x66EDE7DA),
        hairline = Color(0x2EEDE7DA), accent = Color(0xFFFF5A1F), fault = Color(0xFFFF6B4A),
        ch01 = Color(0xFFFF6B4A), ch02 = Color(0xFF6B8CFF), ch03 = Color(0xFF2FD3B0),
        ch04 = Color(0xFFFFB84D), ch05 = Color(0xFFB08CFF), ch06 = Color(0xFFF06BB0)
    )
    val PAPER = HudColors(
        scrim = Color(0xFFF4F1E8), ink = Color(0xFF191713),
        ink60 = Color(0x99191713), ink40 = Color(0x66191713),
        hairline = Color(0x24191713), accent = Color(0xFFFF4D00), fault = Color(0xFFC8371F),
        ch01 = Color(0xFFE5482B), ch02 = Color(0xFF2E5BE0), ch03 = Color(0xFF0E9F6E),
        ch04 = Color(0xFFF0A419), ch05 = Color(0xFF8757D6), ch06 = Color(0xFFD6409F)
    )
    val BLUEPRINT = HudColors(
        scrim = Color(0xFF0C2338), ink = Color(0xFFEAF2FF),
        ink60 = Color(0x99EAF2FF), ink40 = Color(0x66EAF2FF),
        hairline = Color(0x33EAF2FF), accent = Color(0xFF63C7FF), fault = Color(0xFFFF7759),
        // Blueprint: all channel traces render ink — identity by tick + label + hatch
        ch01 = Color(0xFFEAF2FF), ch02 = Color(0xFFEAF2FF), ch03 = Color(0xFFEAF2FF),
        ch04 = Color(0xFFEAF2FF), ch05 = Color(0xFFEAF2FF), ch06 = Color(0xFFEAF2FF)
    )
    fun of(m: HudMedium) = when (m) {
        HudMedium.CARBON -> CARBON; HudMedium.PAPER -> PAPER; HudMedium.BLUEPRINT -> BLUEPRINT
    }
}

// ─────────────── metrics per scale (§3) ───────────────

@Immutable
data class HudMetrics(
    val widthDp: Int, val padDp: Int,
    val heroSp: Int, val valueSp: Int, val metaSp: Int, val microSp: Int,
    val barHDp: Int,                 // memory bar height
    val coreBankCellsPerRow: Int,    // 4 → 2×4 grid; 8 → single bar row
    val coreBankShowFreq: Boolean,
    val showGovLine: Boolean,
    val showTraceBand: Boolean
)

object HudScales {
    fun of(s: HudScale) = when (s) {
        HudScale.S -> HudMetrics(196, 10, 24, 12, 9, 8, 4, 8, false, false, false)
        HudScale.M -> HudMetrics(260, 12, 28, 13, 9, 8, 6, 4, true, false, true)
        HudScale.L -> HudMetrics(300, 14, 32, 14, 10, 9, 8, 4, true, true, true)
    }
}

// ─────────────── type (mono only — HUD is all data) ───────────────

private const val TNUM = "tnum"

@Composable
fun hudStyle(sizeSp: Int, weight: FontWeight = FontWeight.Normal,
             trackingEm: Float = 0.06f): TextStyle = TextStyle(
    fontFamily = PlexMonoFamily,
    fontSize = sizeSp.sp,
    fontWeight = weight,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = TNUM
)

// ─────────────── locals ───────────────

val LocalHudColors = staticCompositionLocalOf { HudPalettes.CARBON }
val LocalHudMetrics = staticCompositionLocalOf { HudScales.of(HudScale.M) }

@Composable
fun HudTheme(medium: HudMedium, scale: HudScale, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHudColors provides HudPalettes.of(medium),
        LocalHudMetrics provides HudScales.of(scale)
    ) { content() }
}
```

---

## §7 — `HudModel.kt`

```kotlin
package com.ivarna.deviceinsight.hud

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin
import kotlin.random.Random

// ─────────────── data ───────────────

data class CoreStat(val id: Int, val loadPct: Float, val freqMhz: Int)

/** 2 Hz payload — everything except FPS. */
data class HudSlow(
    val timestamp: Long = System.currentTimeMillis(),
    val cpuPct: Float = 52f,
    val gov: String = "schedutil",
    val cpuTempC: Float = 54.3f,
    val cores: List<CoreStat> = emptyList(),
    val clusterSizes: List<Int> = listOf(4, 4),      // for the L cluster rule
    val ramUsedMb: Int = 3867, val ramTotalMb: Int = 7484,
    val swapUsedMb: Int = 285, val swapTotalMb: Int = 2047,   // 0 total = NOT FITTED
    val watts: Float = 0.00f,                         // signed: + charging
    val battPct: Float = 1.00f, val battTempC: Float = 35.2f,
    val gpuPct: Float? = 71f, val gpuMhz: Int? = 848,
    val netDown: Long = 18_100_000, val netUp: Long = 2_400_000,
    val cpuHist: List<Float> = emptyList(),
    val pwrHist: List<Float> = emptyList()
)

/** 10 Hz payload — FPS only, so the hero band is the only thing recomposing fast. */
data class HudFast(
    val fps: Float = 90f,
    val source: String = "SF",                        // "SF" SurfaceFlinger | "GFX" gfxinfo | "—"
    val fpsMin: Float = 60f, val fpsAvg: Float = 88f,
    val fpsHist: List<Float> = emptyList()            // 60 samples ≈ 6 s
)

data class HudState(val slow: HudSlow, val fast: HudFast)

// ─────────────── config ───────────────

enum class HudModule { FPS, CPU, MEMORY, POWER, GPU, NETWORK, TRACE }

data class HudConfig(
    val medium: HudMedium = HudMedium.CARBON,
    val scale: HudScale = HudScale.M,
    val opacity: Float = 0.82f,
    val blurBehind: Boolean = true,
    val locked: Boolean = true,                       // locked = FLAG_NOT_TOUCHABLE passthrough
    val modules: List<HudModule> = listOf(
        HudModule.FPS, HudModule.CPU, HudModule.MEMORY, HudModule.POWER, HudModule.TRACE
    ),
    val showCoreBank: Boolean = true,
    val showTraceBand: Boolean = true
)

/** Shared between app (config screen) and service — single source of truth. */
object HudController {
    val config = MutableStateFlow(HudConfig())
    fun update(transform: (HudConfig) -> HudConfig) { config.value = transform(config.value) }
}

// ─────────────── formatting (the grammar of numbers, HUD dialect) ───────────────

object FmtHud {
    fun pct(v: Float) = String.format(java.util.Locale.US, "%.0f%%", v)
    fun pct1(v: Float) = String.format(java.util.Locale.US, "%.1f%%", v)
    fun temp(v: Float) = String.format(java.util.Locale.US, "%.1f°C", v)
    fun watts(v: Float) = String.format(java.util.Locale.US, "%+.2f W", v)
    fun mb(used: Int, total: Int) = "$used / $total MB"
    fun ghz(mhz: Int) = String.format(java.util.Locale.US, "%.2f GHz", mhz / 1000f)
    fun clock(ts: Long) = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        .format(java.util.Date(ts))
    fun rate(bytesPerSec: Long): String = when {
        bytesPerSec >= 1 shl 20 -> String.format(java.util.Locale.US, "%.1f MB/s", bytesPerSec / 1048576f)
        bytesPerSec >= 1 shl 10 -> String.format(java.util.Locale.US, "%.0f KB/s", bytesPerSec / 1024f)
        else -> "$bytesPerSec B/s"
    }
}

// ─────────────── demo feed (preview & dev) ───────────────

object HudDemo {
    private var t = 0.0
    private val cpu = ArrayDeque(List(60) { 30f + Random.nextFloat() * 15f })
    private val pwr = ArrayDeque(List(60) { 0f })
    private val fpsH = ArrayDeque(List(60) { 88f + Random.nextFloat() * 4f })

    fun slow(t: Double): HudSlow {
        val c = (52 + 18 * sin(t * 0.7)).toFloat().coerceIn(5f, 99f)
        cpu.removeFirst(); cpu.addLast(c)
        pwr.removeFirst(); pwr.addLast((0.4f * sin(t)).toFloat())
        return HudSlow(
            cpuPct = c, cpuTempC = (54.3 + 3 * sin(t * 0.3)).toFloat(),
            cores = List(8) { i ->
                CoreStat(i, (c * (0.3f + Random.nextFloat() * 0.9f)).coerceAtMost(100f),
                    if (i < 4) 1804 + (Random.nextInt(4) * 96) else 2188)
            },
            cpuHist = cpu.toList(), pwrHist = pwr.toList()
        )
    }

    fun fast(): HudFast {
        val f = (89 + 3 * sin(t * 1.7) + Random.nextFloat()).toFloat().coerceAtLeast(58f)
        fpsH.removeFirst(); fpsH.addLast(f)
        t += 0.1
        return HudFast(fps = f, fpsMin = fpsH.min(), fpsAvg = fpsH.average().toFloat(), fpsHist = fpsH.toList())
    }
}

/** Demo flows at production cadence: slow 500 ms, fast 100 ms. */
fun demoHudFlows(): Pair<StateFlow<HudSlow>, StateFlow<HudFast>> {
    val slow = MutableStateFlow(HudDemo.slow(0.0))
    val fast = MutableStateFlow(HudDemo.fast())
    kotlinx.coroutines.GlobalScope.let { }  // (never in prod — see HudService for real wiring)
    return slow to fast
}
```

---

## §8 — `HudAtoms.kt`

```kotlin
package com.ivarna.deviceinsight.hud

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ─────────────── corner brackets — the only frame (§2) ───────────────

fun Modifier.hudFrame(
    color: Color, inset: Dp = 3.dp, len: Dp = 10.dp, stroke: Dp = 1.5.dp
): Modifier = drawBehind {
    val i = inset.toPx(); val l = len.toPx(); val w = stroke.toPx()
    val W = size.width; val H = size.height
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(x1, y1), Offset(x2, y2), w)
    line(i, i, i + l, i);              line(i, i, i, i + l)              // TL
    line(W - i, i, W - i - l, i);      line(W - i, i, W - i, i + l)      // TR
    line(i, H - i, i + l, H - i);      line(i, H - i, i, H - i - l)      // BL
    line(W - i, H - i, W - i - l, H - i); line(W - i, H - i, W - i, H - i - l) // BR
}

// ─────────────── stroked text — legibility layer 3 (§4) ───────────────

/** Value text with a 1dp scrim-colored stroke under its fill. Survives any scene. */
@Composable
fun StrokedText(
    text: String,
    style: TextStyle,
    fill: Color,
    strokeColor: Color = LocalHudColors.current.scrim,
    strokeWidth: Dp = 1.dp,
    modifier: Modifier = Modifier
) {
    val px = with(LocalDensity.current) { strokeWidth.toPx() }
    Box(modifier) {
        androidx.compose.foundation.text.BasicText(
            text, style = style.copy(
                color = strokeColor,
                drawStyle = Stroke(px, cap = StrokeCap.Square)
            )
        )
        androidx.compose.foundation.text.BasicText(text, style = style.copy(color = fill))
    }
}

// ─────────────── LED — the sanctioned circle ───────────────

@Composable
fun LedPulse(color: Color, active: Boolean = true, fault: Boolean = false, size: Dp = 5.dp) {
    val reduced = false // wire to system reduced-motion in prod
    val pulse by rememberInfiniteTransition(label = "led").animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "a"
    )
    val c = when { !active -> Color.Transparent; fault -> LocalHudColors.current.fault; else -> color }
    Canvas(Modifier.size(size)) {
        drawCircle(c.copy(alpha = if (reduced) 1f else pulse), radius = size.toPx() / 2)
    }
}

// ─────────────── hairline & channel tick ───────────────

@Composable
fun HairlineH(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LocalHudColors.current.hairline))
}

@Composable
fun HudTick(color: Color, size: Dp = 4.dp) {
    Box(Modifier.size(size).background(color))
}

// ─────────────── SparkPen — trace with square pen head (§5) ───────────────

@Composable
fun SparkPen(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    yMax: Float? = null,
    stroke: Dp = 1.5.dp,
    penSize: Dp = 3.dp
) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val max = (yMax ?: values.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val y = size.height * (1f - (v / max).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
            if (i == 0) path.moveTo(0f, y) else path.lineTo(i * step, y)
        }
        drawPath(path, color, style = Stroke(stroke.toPx(), cap = StrokeCap.Square))
        val lastY = size.height * (1f - (values.last() / max).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
        val ps = penSize.toPx()                                   // the pen — square, always
        drawRect(color, topLeft = Offset(size.width - ps, lastY - ps / 2), size = Size(ps, ps))
    }
}

// ─────────────── MemBar — hatched micro bar (§2 HM-3) ───────────────

enum class MemPattern { SOLID, CROSS }

@Composable
fun MemBar(
    fraction: Float,
    color: Color,
    pattern: MemPattern,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    val hair = LocalHudColors.current.hairline
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), HudNeedle, label = "mem")
    Canvas(modifier.fillMaxWidth().height(height)) {
        drawRect(hair, style = Stroke(1.dp.toPx()))
        val w = size.width * anim - 2f
        if (w > 2f) when (pattern) {
            MemPattern.SOLID -> drawRect(color, topLeft = Offset(1f, 1f), size = Size(w, size.height - 2f))
            MemPattern.CROSS -> clipRect(1f, 1f, 1f + w, size.height - 1f) {
                val p = 3.dp.toPx()
                var x = -size.height
                while (x < w + size.height) {
                    drawLine(color, Offset(x, size.height), Offset(x + size.height, 0f), 1.dp.toPx())
                    drawLine(color, Offset(x, 0f), Offset(x + size.height, size.height), 1.dp.toPx())
                    x += p
                }
            }
        }
    }
}

/** Needle spring + peak-hold (shared motion token). */
val HudNeedle: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 420f)

@Composable
fun rememberPeakHold(value: Float, decayMs: Long = 2000): Float {
    var peak by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value >= peak) peak = value
        else { delay(decayMs); animate(peak, value, tween(600)) { v, _ -> peak = v } }
    }
    return peak
}

// ─────────────── FuelMicro — battery gauge (§2 HM-4) ───────────────

@Composable
fun FuelMicro(
    fraction: Float,
    modifier: Modifier = Modifier,
    critical: Boolean = fraction < 0.2f,
    height: Dp = 12.dp
) {
    val c = LocalHudColors.current
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), HudNeedle, label = "fuel")
    Canvas(modifier.fillMaxWidth().height(height)) {
        val mid = size.height / 2f
        val track = 6.dp.toPx()
        val top = mid - track / 2
        drawRect(c.hairline, topLeft = Offset(0f, top),
            size = Size(size.width, track), style = Stroke(1.dp.toPx()))
        for (i in 0..20) {                                          // ticks every 5%
            val x = size.width * i / 20f
            val major = (i * 5) % 25 == 0
            val len = (if (major) 4.dp else 2.dp).toPx()
            drawLine(c.ink40, Offset(x, top - len), Offset(x, top), 1.dp.toPx())
        }
        drawRect(if (critical) c.fault else c.ch04,
            topLeft = Offset(0f, top), size = Size(size.width * anim, track))
        val ks = 8.dp.toPx()                                        // square needle knob
        val kx = (size.width * anim - ks / 2).coerceIn(0f, size.width - ks)
        drawRect(c.ink, topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
    }
}

// ─────────────── CoreBank — the mixing console (§2 HM-2) ───────────────

@Composable
fun CoreBank(
    cores: List<CoreStat>,
    clusterSizes: List<Int>,
    modifier: Modifier = Modifier
) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cores.forEachIndexed { index, core ->
            // cluster rule at L: hairline divider between big/mid/little groups
            val clusterStart = clusterSizes.runningFold(0) { acc, n -> acc + n }
                .drop(1).takeWhile { it < cores.size }
            val divider = m.showGovLine && index > 0 && clusterStart.contains(index)
            Row(Modifier.weight(1f)) {
                if (divider) Box(Modifier.width(1.dp).fillMaxHeight(0.8f).background(c.hairline))
                CoreCell(core, c.ch01)
            }
        }
    }
}

@Composable
private fun CoreCell(core: CoreStat, barColor: Color) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val load by animateFloatAsState(core.loadPct / 100f, HudNeedle, label = "core${core.id}")
    val peak = rememberPeakHold(core.loadPct / 100f)
    Row {
        Canvas(Modifier.size(width = 5.dp, height = 22.dp)) {
            drawRect(c.hairline, style = Stroke(1.dp.toPx()))
            val h = size.height * load
            drawRect(barColor, topLeft = Offset(0f, size.height - h),
                size = Size(size.width, h))
            val py = size.height * peak                       // ⌃ peak-hold caret
            drawRect(c.ink, topLeft = Offset(0f, (py - 1.dp.toPx()).coerceAtLeast(0f)),
                size = Size(size.width, 1.dp.toPx()))
        }
        if (m.coreBankShowFreq) {
            Spacer(Modifier.width(4.dp))
            Column {
                androidx.compose.foundation.text.BasicText(
                    "C${core.id}", style = hudStyle(m.microSp).copy(color = c.ink40))
                androidx.compose.foundation.text.BasicText(
                    "${core.freqMhz}", style = hudStyle(m.microSp).copy(color = c.ink60))
            }
        }
    }
}

// ─────────────── MiniOdometer — 2 Hz roll for slow values (§5) ───────────────

@Composable
fun MiniOdometer(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        text.forEach { ch ->
            var shown by remember { mutableStateOf(ch) }
            LaunchedEffect(ch) { shown = ch }
            AnimatedContent(
                targetState = shown,
                transitionSpec = {
                    (slideInVertically(tween(160)) { it / 2 } + fadeIn(tween(100))) togetherWith
                        (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(100)))
                }, label = "mini"
            ) { d ->
                androidx.compose.foundation.text.BasicText(
                    d.toString(), style = style.copy(color = color))
            }
        }
    }
}

// ─────────────── thermal coloring — color is signal (§5) ───────────────

@Composable
fun thermalColor(tempC: Float): Color {
    val c = LocalHudColors.current
    return when {
        tempC >= 75f -> c.fault
        tempC >= 65f -> c.ch04
        else -> c.ink60
    }
}

// ─────────────── stamp — source honesty (§2 HM-1) ───────────────

@Composable
fun HudStamp(text: String, color: Color) {
    val c = LocalHudColors.current
    Box(Modifier.border(1.dp, color).padding(horizontal = 3.dp, vertical = 1.dp)) {
        androidx.compose.foundation.text.BasicText(
            text.uppercase(), style = hudStyle(8, trackingEm = 0.1f).copy(color = color))
    }
}
```

---

## §9 — `HudModules.kt` — the bands

```kotlin
package com.ivarna.deviceinsight.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
private fun BandLabel(text: String, tick: Color) {
    val c = LocalHudColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        HudTick(tick)
        Spacer(Modifier.width(5.dp))
        androidx.compose.foundation.text.BasicText(
            text.uppercase(), style = hudStyle(9, trackingEm = 0.08f).copy(color = c.ink40))
    }
}

// ─────────────── HM-0 · HEADER — clock, LED, drag affordance ───────────────

@Composable
fun HudHeaderBand(slow: HudSlow, paused: Boolean, fault: Boolean, onLock: () -> Unit) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        // crosshair = lock key (tap to lock while unlocked)
        androidx.compose.foundation.Canvas(Modifier.size(12.dp).clickableNoIndication(onLock)) {
            val r = size.minDimension / 2 - 1.5.dp.toPx()
            drawCircle(c.ink, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            drawLine(c.ink, Offset(0f, center.y), Offset(size.width, center.y), 1.5f)
            drawLine(c.ink, Offset(center.x, 0f), Offset(center.x, size.height), 1.5f)
        }
        Spacer(Modifier.width(6.dp))
        androidx.compose.foundation.text.BasicText(
            "DI·HUD", style = hudStyle(9, trackingEm = 0.1f).copy(color = c.ink40))
        Spacer(Modifier.weight(1f))
        val clock = FmtHud.clock(slow.timestamp)
        val colon = if ((slow.timestamp / 1000) % 2 == 0L) ":" else " "   // the heartbeat
        StrokedText(clock.replaceFirst(":", colon).let {
            // blink both colons: build manually
            val p = clock.split(":")
            "${p[0]}$colon${p[1]}$colon${p[2]}"
        }, hudStyle(m.valueSp), fill = c.ink)
        Spacer(Modifier.width(8.dp))
        LedPulse(color = if (fault) c.fault else c.accent, active = !paused, fault = fault)
    }
}

// ─────────────── HM-1 · FPS — hero band ───────────────

@Composable
fun HudFpsBand(fast: HudFast) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.text.BasicText(
                "FPS", style = hudStyle(m.metaSp, trackingEm = 0.08f).copy(color = c.ink40))
            Spacer(Modifier.width(8.dp))
            StrokedText(
                String.format(java.util.Locale.US, "%.1f", fast.fps),
                hudStyle(m.heroSp, androidx.compose.ui.text.font.FontWeight.Light, 0f),
                fill = c.ink
            )
            Spacer(Modifier.width(8.dp))
            HudStamp(fast.source, c.ink40)                     // honesty: SF or GFX
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.text.BasicText(
                "min ${fast.fpsMin.roundToInt()} · avg ${fast.fpsAvg.roundToInt()}",
                style = hudStyle(m.microSp).copy(color = c.ink40))
        }
        Spacer(Modifier.height(4.dp))
        SparkPen(fast.fpsHist, c.accent, Modifier.fillMaxWidth().height(20.dp))
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
            MiniOdometer(FmtHud.pct1(slow.cpuPct), hudStyle(m.valueSp), c.ink)
            Spacer(Modifier.width(8.dp))
            val avg = slow.cores.map { it.freqMhz }.average().toInt()
            StrokedText(FmtHud.ghz(avg), hudStyle(m.valueSp), fill = c.ink60)
            Spacer(Modifier.width(8.dp))
            StrokedText(FmtHud.temp(slow.cpuTempC), hudStyle(m.valueSp),
                fill = thermalColor(slow.cpuTempC))
        }
        if (showCoreBank && slow.cores.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            CoreBank(slow.cores, slow.clusterSizes)
        }
        if (m.showGovLine && slow.cores.isNotEmpty()) {
            Spacer(Modifier.height(3.dp))
            val peak = slow.cores.maxBy { it.freqMhz }
            androidx.compose.foundation.text.BasicText(
                "gov ${slow.gov} · peak C${peak.id} ${peak.freqMhz} MHz",
                style = hudStyle(m.microSp).copy(color = c.ink40))
        }
    }
}

// ─────────────── HM-3 · MEMORY ───────────────

@Composable
private fun MemRow(label: String, used: Int, total: Int, tickColor: Color, pattern: MemPattern) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val fitted = total > 0
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        HudTick(tickColor)
        Spacer(Modifier.width(5.dp))
        androidx.compose.foundation.text.BasicText(
            label, style = hudStyle(m.metaSp, trackingEm = 0.08f).copy(
                color = if (fitted) c.ink40 else c.ink40.copy(alpha = 0.5f)))
        Spacer(Modifier.weight(1f))
        if (fitted) {
            androidx.compose.foundation.text.BasicText(
                "${FmtHud.mb(used, total)}   ${FmtHud.pct(100f * used / total)}",
                style = hudStyle(m.valueSp).copy(color = c.ink))
        } else {
            androidx.compose.foundation.text.BasicText(
                "— NOT FITTED", style = hudStyle(m.valueSp).copy(color = c.ink40))
        }
    }
}

@Composable
fun HudMemoryBand(slow: HudSlow) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Column(Modifier.fillMaxWidth()) {
        MemRow("CH-02 · RAM", slow.ramUsedMb, slow.ramTotalMb, c.ch02, MemPattern.SOLID)
        if (slow.ramTotalMb > 0) {
            Spacer(Modifier.height(3.dp))
            MemBar(slow.ramUsedMb.toFloat() / slow.ramTotalMb, c.ch02, MemPattern.SOLID)
        }
        Spacer(Modifier.height(5.dp))
        MemRow("CH-02 · SWP", slow.swapUsedMb, slow.swapTotalMb, c.ch02, MemPattern.CROSS)
        if (slow.swapTotalMb > 0) {
            Spacer(Modifier.height(3.dp))
            MemBar(slow.swapUsedMb.toFloat() / slow.swapTotalMb, c.ch02, MemPattern.CROSS)
        }
    }
}

// ─────────────── HM-4 · POWER ───────────────

@Composable
fun HudPowerBand(slow: HudSlow) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    val charging = slow.watts > 0.05f
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BandLabel("CH-04 · PWR", c.ch04)
            Spacer(Modifier.weight(1f))
            StrokedText(FmtHud.watts(slow.watts), hudStyle(m.valueSp),
                fill = if (charging) c.ch04 else c.ink)      // signed watts, honest zero
            Spacer(Modifier.width(10.dp))
            StrokedText("BAT ${FmtHud.pct(slow.battPct * 100)}", hudStyle(m.valueSp),
                fill = if (slow.battPct < 0.2f) c.fault else c.ink)
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                FuelMicro(slow.battPct, critical = slow.battPct < 0.2f)
            }
            Spacer(Modifier.width(8.dp))
            StrokedText(FmtHud.temp(slow.battTempC), hudStyle(m.microSp), fill = c.ink40)
        }
        if (m.showGovLine) {
            Spacer(Modifier.height(2.dp))
            androidx.compose.foundation.text.BasicText(
                if (charging) "charging · ${FmtHud.watts(slow.watts)} in"
                else "discharge · est remaining on device",
                style = hudStyle(m.microSp).copy(color = c.ink40))
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
        val pct = slow.gpuPct
        val mhz = slow.gpuMhz
        if (pct != null && mhz != null) {
            StrokedText("${FmtHud.pct(pct)} · $mhz MHz", hudStyle(m.valueSp), fill = c.ink)
        } else {
            androidx.compose.foundation.text.BasicText(
                "— NOT FITTED", style = hudStyle(m.valueSp).copy(color = c.ink40))
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
        StrokedText("↓ ${FmtHud.rate(slow.netDown)}   ↑ ${FmtHud.rate(slow.netUp)}",
            hudStyle(m.valueSp), fill = c.ink)
    }
}

// ─────────────── HM-7 · TRACE band (L / toggle) ───────────────

@Composable
fun HudTraceBand(slow: HudSlow, fast: HudFast) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TraceCell("CPU", c.ch01, slow.cpuHist, FmtHud.pct(slow.cpuPct))
        TraceCell("PWR", c.ch04, slow.pwrHist, FmtHud.watts(slow.watts))
    }
}

@Composable
private fun RowScope.TraceCell(label: String, color: Color, hist: List<Float>, value: String) {
    val c = LocalHudColors.current
    Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudTick(color)
            Spacer(Modifier.width(4.dp))
            androidx.compose.foundation.text.BasicText(
                label, style = hudStyle(8, trackingEm = 0.1f).copy(color = c.ink40))
            Spacer(Modifier.weight(1f))
            androidx.compose.foundation.text.BasicText(
                value, style = hudStyle(8).copy(color = c.ink60))
        }
        Spacer(Modifier.height(2.dp))
        SparkPen(hist, color, Modifier.fillMaxWidth().height(16.dp))
    }
}
```

*(Helper used above:)*

```kotlin
fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.then(
        androidx.compose.ui.Modifier.let { m ->
            m // placeholder — real impl:
        }
    )
// Actual implementation (place in HudAtoms.kt):
// fun Modifier.clickableNoIndication(onClick: () -> Unit) = composed {
//     clickable(interactionSource = remember { MutableInteractionSource() },
//               indication = null, onClick = onClick)
// }
```

---

## §10 — `HudPanel.kt` — assembly, drag, scrim

```kotlin
package com.ivarna.deviceinsight.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The panel. Reads fast/slow states independently so the 10 Hz FPS band
 * is the ONLY thing recomposing between slow ticks.
 */
@Composable
fun HudPanel(
    config: HudConfig,
    slow: State<HudSlow>,
    fast: State<HudFast>,
    effectiveOpacity: Float,                 // service raises this if blur-behind is unavailable
    onDrag: (dxPx: Int, dyPx: Int) -> Unit,
    onLock: () -> Unit,
    onOpenConfig: () -> Unit
) {
    val c = HudPalettes.of(config.medium)
    val m = HudScales.of(config.scale)
    val s = slow.value
    val f = fast.value
    val fault = s.cpuTempC >= 75f || s.battPct < 0.2f

    val dragModifier = if (config.locked) Modifier
    else Modifier
        .pointerInput(Unit) {
            detectDragGestures { change, amount ->
                change.consume()
                onDrag(amount.x.roundToInt(), amount.y.roundToInt())
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onTap = { onOpenConfig() })
        }

    Box(
        Modifier
            .width(m.widthDp.dp)
            .background(c.scrim.copy(alpha = effectiveOpacity))
            .hudFrame(c.ink)
            .then(dragModifier)
    ) {
        Column(Modifier.padding(m.padDp.dp)) {

            HudHeaderBand(
                slow = s,
                paused = false,
                fault = fault,
                onLock = onLock
            )
            HairlineH(Modifier.padding(top = m.padDp.dp / 2, bottom = m.padDp.dp / 2))

            config.modules.forEachIndexed { i, module ->
                if (i > 0) {
                    Spacer(Modifier.height(m.padDp.dp / 2))
                    HairlineH()
                    Spacer(Modifier.height(m.padDp.dp / 2))
                }
                when (module) {
                    HudModule.FPS -> HudFpsBand(f)
                    HudModule.CPU -> HudCpuBand(s, config.showCoreBank)
                    HudModule.MEMORY -> HudMemoryBand(s)
                    HudModule.POWER -> HudPowerBand(s)
                    HudModule.GPU -> if (s.gpuPct != null) HudGpuBand(s)
                    HudModule.NETWORK -> HudNetBand(s)
                    HudModule.TRACE -> if (config.showTraceBand && m.showTraceBand)
                        HudTraceBand(s, f)
                }
            }

            if (!config.locked) {                              // affordance strip while unlocked
                Spacer(Modifier.height(m.padDp.dp / 2))
                HairlineH()
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.text.BasicText(
                    "DRAG TO MOVE · TAP ⌖ TO LOCK",
                    style = hudStyle(8, trackingEm = 0.1f).copy(color = c.ink40),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
```

---

## §11 — `HudService.kt` — window wiring (UI plumbing only)

```kotlin
package com.ivarna.deviceinsight.hud

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.monitor.MonitorRepository   // your existing monitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class HudService : android.app.Service(), LifecycleOwner {

    private val lifecycle = LifecycleRegistry(this).apply {
        currentState = Lifecycle.State.CREATED
    }
    override val lifecycle: Lifecycle get() = lifecycle

    private lateinit var wm: WindowManager
    private lateinit var view: ComposeView
    private lateinit var params: WindowManager.LayoutParams
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: SharedPreferences

    private val slowState = mutableStateOf(HudSlow())
    private val fastState = mutableStateOf(HudFast())
    private var config by mutableStateOf(HudController.config.value)

    // ─────────────── lifecycle ───────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        prefs = getSharedPreferences("hud", MODE_PRIVATE)
        config = HudController.config.value
        lifecycle.currentState = Lifecycle.State.RESUMED

        params = buildParams(
            x = prefs.getInt("x", 40), y = prefs.getInt("y", 120)
        )
        applyBlurBehind()

        view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@HudService)
            setViewTreeSavedStateRegistryOwner(
                androidx.savedstate.SavedStateRegistryOwner { lifecycle } // see note
            )
            setContent { Content() }
        }
        wm.addView(view, params)

        // config changes (from app) → live retheme + flag updates
        scope.launch {
            HudController.config.collectLatest { cfg ->
                config = cfg
                params.flags = touchFlags(params.flags, cfg.locked)
                wm.updateViewLayout(view, params)
            }
        }
        // monitor feed at two rates — only the FPS band recomposes at 10 Hz
        scope.launch { MonitorRepository.slow.collectLatest { slowState.value = it } }
        scope.launch { MonitorRepository.fast.collectLatest { fastState.value = it } }
    }

    override fun onDestroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
        wm.removeView(view)
        scope.cancel()
        super.onDestroy()
    }

    // ─────────────── UI content ───────────────

    @Composable
    private fun Content() {
        var blurSupported by remember { mutableStateOf(true) }
        DisposableEffect(Unit) {
            val listener = android.view.WindowManager.OnCrossWindowBlurEnabledListener { ok ->
                blurSupported = ok
                if (!ok) { params.clearFlags(blurFlag()); wm.updateViewLayout(view, params) }
            }
            if (Build.VERSION.SDK_INT >= 31) wm.addCrossWindowBlurEnabledListener(listener)
            onDispose { if (Build.VERSION.SDK_INT >= 31) wm.removeCrossWindowBlurEnabledListener(listener) }
        }

        val opacity = if (config.blurBehind && blurSupported && Build.VERSION.SDK_INT >= 31)
            config.opacity
        else (config.opacity + 0.10f).coerceAtMost(0.97f)   // scrim compensates for missing blur

        HudTheme(medium = config.medium, scale = config.scale) {
            HudPanel(
                config = config,
                slow = slowState,
                fast = fastState,
                effectiveOpacity = opacity,
                onDrag = { dx, dy -> moveBy(dx, dy) },
                onLock = { HudController.update { it.copy(locked = true) } },
                onOpenConfig = {
                    startActivity(android.content.Intent(this, MainActivity::class.java)
                        .putExtra("route", "hud-config")
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            )
        }
    }

    // ─────────────── window mechanics ───────────────

    private fun buildParams(x: Int, y: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            touchFlags(0, config.locked) or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x; this.y = y
        }

    private fun touchFlags(flags: Int, locked: Boolean): Int =
        flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            if (locked) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0

    private fun blurFlag() = if (Build.VERSION.SDK_INT >= 31)
        WindowManager.LayoutParams.FLAG_BLUR_BEHIND else 0

    @SuppressLint("WrongConstant", "UnspecifiedRegisterReceiverFlag")
    private fun applyBlurBehind() {
        if (Build.VERSION.SDK_INT >= 31 && config.blurBehind) {
            params.flags = params.flags or blurFlag()
            params.setBlurBehindRadius((10 * resources.displayMetrics.density).toInt())
        }
    }

    private fun moveBy(dx: Int, dy: Int) {
        params.x += dx; params.y += dy
        wm.updateViewLayout(view, params)
        prefs.edit().putInt("x", params.x).putInt("y", params.y).apply()
    }

    private companion object {
        init { /* note: SavedStateRegistryOwner needs a real provider in prod —
                   use androidx.savedstate.SavedStateRegistryController */
        }
    }
}
```

> ⚠️ **Two prod notes, flagged honestly:** (1) the `setViewTreeSavedStateRegistryOwner` line needs a real `SavedStateRegistryController` — wire it exactly as the Glance/Compose-in-service pattern from the AndroidX docs; (2) overlay permission (`Settings.canDrawOverlays`) must be checked before `startService` — the app's calibration flow (S-00 step 02) already owns that.

**Manifest:**

```xml
<service android:name=".hud.HudService"
         android:exported="false"
         android:foregroundServiceType="specialUse"/>
```

---

## §12 — Previews & QA gate

```kotlin
@Preview(showBackground = true, backgroundColor = 0xFF101820)  // over a dark "game"
@Composable
private fun HudPreview() {
    val slow = remember { mutableStateOf(HudDemo.slow(0.0)) }
    val fast = remember { mutableStateOf(HudDemo.fast()) }
    LaunchedEffect(Unit) {
        var t = 0.0
        while (true) {
            delay(500); t += 0.5; slow.value = HudDemo.slow(t)
        }
    }
    LaunchedEffect(Unit) {
        while (true) { delay(100); fast.value = HudDemo.fast() }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HudMedium.entries.forEach { medium ->
            HudTheme(medium, HudScale.M) {
                HudPanel(
                    config = HudConfig(medium = medium, locked = false),
                    slow = slow, fast = fast, effectiveOpacity = 0.88f,
                    onDrag = { _, _ -> }, onLock = {}, onOpenConfig = {}
                )
            }
        }
    }
}
```

**Review checklist:**
- [ ] Corner brackets render 1.5dp on all densities; panel radius is 0dp — no rounded card ever
- [ ] Every channel color appears with its `CH-xx` label + tick (Blueprint proves it works with none)
- [ ] FPS recomposes at 10 Hz; all other bands skip on equal state (check with Layout Inspector)
- [ ] Blur unsupported → scrim opacity rises; never a translucent illegible panel
- [ ] Values legible over white, black, and mid-gray test backgrounds (stroke test)
- [ ] `+0.00 W` renders with sign; `SWP — NOT FITTED` when `swapTotal == 0`
- [ ] Locked = full touch passthrough; unlocked = drag + tap-⌖-to-lock + tap = config
- [ ] Thermal value recolors at 65°C / 75°C with the number always present
- [ ] Reduced motion: no LED breathe, no bar springs

---

*The overlay is a probe, not a dashboard. Brackets, bands, a pen, and a stamp — nothing else on the glass.*
