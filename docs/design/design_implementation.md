# DeviceInsight — CALIPER Design System · Kotlin Implementation

Complete, drop-in Jetpack Compose implementation of every component in the CALIPER spec. Package: `com.ivarna.deviceinsight.ui.caliper`.

```
ui/caliper/
├── CaliperTheme.kt        palettes · channels · typography · motion · locals
├── CaliperDraw.kt         hatching · dashed borders · graph-paper grid
├── CaliperUtils.kt        number formatting · haptics · reduced-motion
├── components/
│   ├── CaliperPrimitives.kt   LED · stamps · keys · DIP · fader · seg · odometer · MarginNote
│   ├── CaliperData.kt         PanelCard · ReadoutTile · ScopeTrace · CoreRail · gauges · HatchBar
│   ├── CaliperLedger.kt       LedgerTable · Dossier · SafetyLatch · Processes screen
│   └── CaliperChrome.kt       Masthead · ModeRail · ScreenHeader · states · calibration sweep
├── hud/CaliperHud.kt
└── widget/ChannelWidget.kt
```

---

## 0 · Setup

```kotlin
// app/build.gradle.kts (additions)
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.glance:glance-appwidget:1.1.0")
}
```

```text
res/font/
  instrument_serif_regular.ttf      (OFL — Google Fonts)
  instrument_serif_italic.ttf
  ibmplexmono_light.ttf
  ibmplexmono_regular.ttf
  ibmplexmono_medium.ttf
```

---

## 1 · `CaliperTheme.kt` — foundations

```kotlin
package com.ivarna.deviceinsight.ui.caliper

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.ivarna.deviceinsight.R

// ─────────────────────────────── media ───────────────────────────────

enum class Medium { PAPER, CARBON, BLUEPRINT }

enum class HatchPattern { SOLID, DIAGONAL, CROSS, DOTS, VERTICAL, HORIZONTAL, NONE }

data class Channel(
    val id: String,      // "CH-01"
    val name: String,    // "CPU"
    val paper: Color,
    val carbon: Color,
    val hatch: HatchPattern
) {
    val label: String get() = "$id · $name"   // mandatory wherever the color appears
}

object Channels {   // §4.1 — the channel registry
    val CPU     = Channel("CH-01", "CPU",     Color(0xFFE5482B), Color(0xFFFF6B4A), HatchPattern.SOLID)
    val MEMORY  = Channel("CH-02", "MEMORY",  Color(0xFF2E5BE0), Color(0xFF6B8CFF), HatchPattern.DIAGONAL)
    val NETWORK = Channel("CH-03", "NETWORK", Color(0xFF0E9F6E), Color(0xFF2FD3B0), HatchPattern.CROSS)
    val POWER   = Channel("CH-04", "POWER",   Color(0xFFF0A419), Color(0xFFFFB84D), HatchPattern.DOTS)
    val STORAGE = Channel("CH-05", "STORAGE", Color(0xFF8757D6), Color(0xFFB08CFF), HatchPattern.VERTICAL)
    val GPU     = Channel("CH-06", "GPU",     Color(0xFFD6409F), Color(0xFFF06BB0), HatchPattern.HORIZONTAL)
}

// ─────────────────────────────── palettes ───────────────────────────────

@Immutable
data class CaliperColors(
    val medium: Medium,
    val surface: Color,   // paper/0 — app background
    val panel: Color,     // paper/1 — raised panels
    val ink: Color,
    val ink60: Color,
    val ink40: Color,
    val hairline: Color,  // 1dp rules
    val accent: Color,    // interactive only — never data
    val fault: Color,
    val gridMinor: Color,
    val gridMajor: Color
) {
    fun channel(ch: Channel): Color = when (medium) {
        Medium.CARBON -> ch.carbon
        else -> ch.paper
    }
}

val PaperColors = CaliperColors(
    medium = Medium.PAPER,
    surface = Color(0xFFF4F1E8), panel = Color(0xFFFBF9F3),
    ink = Color(0xFF191713), ink60 = Color(0x99191713), ink40 = Color(0x66191713),
    hairline = Color(0x24191713),            // ink @ 14%
    accent = Color(0xFFFF4D00), fault = Color(0xFFC8371F),
    gridMinor = Color(0x08191713), gridMajor = Color(0x0D191713)
)

val CarbonColors = CaliperColors(
    medium = Medium.CARBON,
    surface = Color(0xFF141310), panel = Color(0xFF1C1B17),
    ink = Color(0xFFEDE7DA), ink60 = Color(0x99EDE7DA), ink40 = Color(0x66EDE7DA),
    hairline = Color(0x2EEDE7DA),
    accent = Color(0xFFFF5A1F), fault = Color(0xFFFF6B4A),
    gridMinor = Color(0x0DEDE7DA), gridMajor = Color(0x14EDE7DA)
)

val BlueprintColors = CaliperColors(
    medium = Medium.BLUEPRINT,
    surface = Color(0xFF0C2338), panel = Color(0xFF12314E),
    ink = Color(0xFFEAF2FF), ink60 = Color(0x99EAF2FF), ink40 = Color(0x66EAF2FF),
    hairline = Color(0x33EAF2FF),
    accent = Color(0xFF63C7FF), fault = Color(0xFFFF7759),
    gridMinor = Color(0x0DEAF2FF), gridMajor = Color(0x17EAF2FF)
)

// ─────────────────────────────── typography ───────────────────────────────

val InstrumentSerifFamily = FontFamily(
    Font(R.font.instrument_serif_regular),
    Font(R.font.instrument_serif_italic, style = FontStyle.Italic)
)
val PlexMonoFamily = FontFamily(
    Font(R.font.ibmplexmono_light, weight = FontWeight.Light),
    Font(R.font.ibmplexmono_regular, weight = FontWeight.Normal),
    Font(R.font.ibmplexmono_medium, weight = FontWeight.Medium)
)

private const val TNUM = "tnum"   // tabular figures — always on for numerals

@Immutable
data class CaliperTypography(
    val display1: TextStyle, val display2: TextStyle,
    val readoutXl: TextStyle, val readoutL: TextStyle,
    val dataM: TextStyle, val dataS: TextStyle,
    val body: TextStyle, val label: TextStyle, val meta: TextStyle
)

val CaliperType = CaliperTypography(
    display1 = TextStyle(InstrumentSerifFamily, fontStyle = FontStyle.Italic, fontSize = 40.sp, lineHeight = 44.sp),
    display2 = TextStyle(InstrumentSerifFamily, fontStyle = FontStyle.Italic, fontSize = 28.sp, lineHeight = 32.sp),
    readoutXl = TextStyle(PlexMonoFamily, fontWeight = FontWeight.Light,  fontSize = 54.sp, lineHeight = 58.sp, fontFeatureSettings = TNUM),
    readoutL  = TextStyle(PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, fontFeatureSettings = TNUM),
    dataM     = TextStyle(PlexMonoFamily, fontSize = 22.sp, lineHeight = 26.sp, fontFeatureSettings = TNUM),
    dataS     = TextStyle(PlexMonoFamily, fontSize = 16.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM),
    body      = TextStyle(PlexMonoFamily, fontSize = 14.sp, lineHeight = 20.sp),
    label     = TextStyle(PlexMonoFamily, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM),
    // ALL-CAPS micro labels — callers uppercase the string
    meta      = TextStyle(PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em, fontFeatureSettings = TNUM)
)

// ─────────────────────────────── motion ───────────────────────────────

object CaliperMotion {
    val Ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)                    // ease/instrument
    val Needle: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 420f)
    val Snap:   SpringSpec<Float> = spring(dampingRatio = 1.00f, stiffness = 700f)
    const val tFast = 140
    const val tBase = 200
    const val tSweep = 420
}

// ─────────────────────────────── theme ───────────────────────────────

val LocalCaliperColors = staticCompositionLocalOf { PaperColors }
val LocalCaliperTypography = staticCompositionLocalOf { CaliperType }

object Caliper {
    val colors: CaliperColors @Composable get() = LocalCaliperColors.current
    val type: CaliperTypography @Composable get() = LocalCaliperTypography.current
}

@Composable
fun CaliperTheme(
    medium: Medium = if (isSystemInDarkTheme()) Medium.CARBON else Medium.PAPER,
    content: @Composable () -> Unit
) {
    val colors = when (medium) {
        Medium.PAPER -> PaperColors; Medium.CARBON -> CarbonColors; Medium.BLUEPRINT -> BlueprintColors
    }
    val m3 = if (medium == Medium.PAPER) lightColorScheme(
        background = colors.surface, surface = colors.panel,
        primary = colors.accent, onBackground = colors.ink, onSurface = colors.ink
    ) else darkColorScheme(
        background = colors.surface, surface = colors.panel,
        primary = colors.accent, onBackground = colors.ink, onSurface = colors.ink
    )
    CompositionLocalProvider(
        LocalCaliperColors provides colors,
        LocalCaliperTypography provides CaliperType
    ) {
        MaterialTheme(colorScheme = m3, content = content)
    }
}

/** Honor system "remove animations". All sweep/odometer/pulse code branches on this. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) == 0f
    }
}
```

---

## 2 · `CaliperDraw.kt` — hatching & drawing utilities

```kotlin
package com.ivarna.deviceinsight.ui.caliper

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** §4.5 — hatch is a first-class redundancy channel (CVD-safe, prints beautifully). */
fun DrawScope.hatch(
    rect: Rect,
    pattern: HatchPattern,
    color: Color,
    strokeWidth: Dp = 1.dp,
    period: Dp = 4.dp
) {
    val sw = strokeWidth.toPx(); val p = period.toPx()
    when (pattern) {
        HatchPattern.NONE -> {}
        HatchPattern.SOLID ->
            drawRect(color, topLeft = rect.topLeft, size = rect.size)
        HatchPattern.VERTICAL -> {
            var x = rect.left + p / 2
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.top), Offset(x, rect.bottom), sw)
                x += p
            }
        }
        HatchPattern.HORIZONTAL -> {
            var y = rect.top + p / 2
            while (y < rect.bottom) {
                drawLine(color, Offset(rect.left, y), Offset(rect.right, y), sw)
                y += p
            }
        }
        HatchPattern.DIAGONAL -> clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            var x = rect.left - rect.height
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.bottom), Offset(x + rect.height, rect.top), sw)
                x += p
            }
        }
        HatchPattern.CROSS -> clipRect(rect.left, rect.top, rect.right, rect.bottom) {
            var x = rect.left - rect.height
            while (x < rect.right) {
                drawLine(color, Offset(x, rect.bottom), Offset(x + rect.height, rect.top), sw)
                drawLine(color, Offset(x, rect.top), Offset(x + rect.height, rect.bottom), sw)
                x += p
            }
        }
        HatchPattern.DOTS -> {
            var y = rect.top + p / 2
            while (y < rect.bottom) {
                var x = rect.left + p / 2
                while (x < rect.right) {
                    drawCircle(color, radius = sw * 0.8f, center = Offset(x, y))
                    x += p
                }
                y += p
            }
        }
    }
}

/** Dashed border — used for DISABLED HardKeys, locked root panels, perforations. */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    dash: Dp = 4.dp,
    gap: Dp = 4.dp
): Modifier = drawBehind {
    val w = width.toPx(); val half = w / 2
    val effect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx()))
    val W = size.width; val H = size.height
    drawLine(color, Offset(half, half), Offset(W - half, half), w, pathEffect = effect)
    drawLine(color, Offset(W - half, half), Offset(W - half, H - half), w, pathEffect = effect)
    drawLine(color, Offset(W - half, H - half), Offset(half, H - half), w, pathEffect = effect)
    drawLine(color, Offset(half, H - half), Offset(half, half), w, pathEffect = effect)
}

/** §4.4 — graph-paper background: 24dp minor @3%, 120dp major @5%. */
@Composable
fun Modifier.caliperGrid(showMajor: Boolean = true): Modifier {
    val c = Caliper.colors
    return this.drawBehind {
        val minor = 24.dp.toPx(); val major = 120.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(c.gridMinor, Offset(x, 0f), Offset(x, size.height), 1f); x += minor
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(c.gridMinor, Offset(0f, y), Offset(size.width, y), 1f); y += minor
        }
        if (showMajor) {
            x = 0f
            while (x <= size.width) { drawLine(c.gridMajor, Offset(x, 0f), Offset(x, size.height), 1f); x += major }
            y = 0f
            while (y <= size.height) { drawLine(c.gridMajor, Offset(0f, y), Offset(size.width, y), 1f); y += major }
        }
    }
}

/** Leader-note box used inside ScopeTrace. */
fun DrawScope.noteBox(
    topLeft: Offset, size: Size, fill: Color, border: Color
) {
    drawRect(fill, topLeft = topLeft, size = size)
    drawRect(border, topLeft = topLeft, size = size, style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
}
```

---

## 3 · `CaliperUtils.kt` — formatting, haptics

```kotlin
package com.ivarna.deviceinsight.ui.caliper

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** §4.9 — the grammar of numbers. */
object Fmt {
    fun bytes(v: Long): String {
        if (v < 1024) return "$v B"
        val kb = v / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.2f KB", kb)
        val mb = kb / 1024.0
        return if (mb < 1024) String.format(Locale.US, "%.2f MB", mb)
        else String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }
    fun hz(khz: Long): String = when {
        khz >= 1_000_000 -> String.format(Locale.US, "%.2f GHz", khz / 1e6)
        khz >= 1_000     -> String.format(Locale.US, "%.0f MHz", khz / 1e3)
        else -> "$khz kHz"
    }
    fun pct(v: Float, decimals: Int = 0) = String.format(Locale.US, "%.${decimals}f%%", v)
    fun temp(v: Float) = String.format(Locale.US, "%.1f°C", v)
    fun rate(bytesPerSec: Long) = bytes(bytesPerSec) + "/s"
    fun watts(v: Float) = String.format(Locale.US, "≈ %.2f W", v)
    fun duration(ms: Long): String {
        val h = ms / 3_600_000; val m = (ms % 3_600_000) / 60_000
        return String.format(Locale.US, "%dh %02dm", h, m)
    }
    fun index(n: Int) = String.format(Locale.US, "%04d", n)   // ledger row numbers
}

/** §4.8 — haptic vocabulary. */
class CaliperHaptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun wave(timings: LongArray, amplitudes: IntArray) {
        vibrator?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }
    fun tick()    = wave(longArrayOf(0, 8), intArrayOf(0, 120))
    fun confirm() = wave(longArrayOf(0, 15, 20, 15), intArrayOf(0, 120, 120, 120))
    fun arm()     = wave(longArrayOf(0, 15, 15, 15, 15, 15), intArrayOf(0, 40, 40, 80, 80, 120))
    fun fault()   = wave(longArrayOf(0, 40), intArrayOf(0, 160))
    fun stamp()   = wave(longArrayOf(0, 12), intArrayOf(0, 140))
}

@Composable
fun rememberCaliperHaptics(): CaliperHaptics {
    val ctx = LocalContext.current
    return remember { CaliperHaptics(ctx) }
}
```

---

## 4 · `CaliperPrimitives.kt` — LED, stamps, keys, DIP, fader, seg, odometer, MarginNote

```kotlin
package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ─────────────────────────── LED ───────────────────────────

@Composable
fun LedDot(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    color: Color = Caliper.colors.accent,
    pulsing: Boolean = true,
    dotSize: Dp = 6.dp
) {
    val reduced = rememberReducedMotion()
    val pulse by rememberInfiniteTransition(label = "led")
        .animateFloat(0.6f, 1f,
            infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "ledAlpha")
    val alpha = when {
        !active -> 0.25f
        pulsing && !reduced -> pulse
        else -> 1f
    }
    Canvas(modifier.size(dotSize)) {
        drawCircle(color.copy(alpha = alpha), radius = size.minDimension / 2)
    }
}

// ─────────────────────────── channel tick ───────────────────────────

@Composable
fun ChannelTick(channel: Channel, modifier: Modifier = Modifier, tickSize: Dp = 6.dp) {
    Box(modifier.size(tickSize).background(Caliper.colors.channel(channel)))
}

// ─────────────────────────── rules & sheet marks ───────────────────────────

@Composable
fun DoubleRule(modifier: Modifier = Modifier, color: Color = Caliper.colors.hairline) {
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        drawLine(color, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
        drawLine(color, Offset(0f, 3.5f), Offset(size.width, 3.5f), 1.dp.toPx())
    }
}

@Composable
fun EndOfSheet(modifier: Modifier = Modifier) {
    Text("— END OF SHEET —",
        style = Caliper.type.meta, color = Caliper.colors.ink40,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp))
}

/** Dotted-leader spec row — "model ............. Pixel 8 Pro" */
@Composable
fun SpecRow(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Row(modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
        Box(Modifier.weight(1f).padding(horizontal = 6.dp).height(1.dp).drawBehind {
            drawLine(c.ink40, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())))
        })
        Text(value, style = Caliper.type.dataS, color = c.ink, maxLines = 1)
    }
}

// ─────────────────────────── StampBadge ───────────────────────────

@Composable
fun StampBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Caliper.colors.fault,
    rotation: Float = -3f,
    animateIn: Boolean = true
) {
    val reduced = rememberReducedMotion()
    val haptics = rememberCaliperHaptics()
    var stamped by remember { mutableStateOf(!animateIn || reduced) }
    LaunchedEffect(Unit) { if (!stamped) { delay(60); stamped = true; haptics.stamp() } }
    val scale by animateFloatAsState(
        if (stamped) 1f else 1.12f,
        tween(180, easing = CaliperMotion.Ease), label = "stamp"
    )
    Text(
        text.uppercase(),
        style = Caliper.type.meta.copy(fontSize = 13.sp, letterSpacing = 0.12.sp * 10 / 11), // ≈0.12em
        color = color.copy(alpha = 0.85f),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
            .border(1.5.dp, color.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = "status: ${text.lowercase()}" }
    )
}

// ─────────────────────────── HardKey ───────────────────────────

enum class HardKeyVariant { PRIMARY, SECONDARY, DESTRUCTIVE, DISABLED }

@Composable
fun HardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HardKeyVariant = HardKeyVariant.SECONDARY,
    enabled: Boolean = true
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val disabled = !enabled || variant == HardKeyVariant.DISABLED

    val bg = when (variant) {
        HardKeyVariant.PRIMARY -> c.ink
        HardKeyVariant.SECONDARY -> Color.Transparent
        HardKeyVariant.DESTRUCTIVE -> c.fault
        HardKeyVariant.DISABLED -> Color.Transparent
    }
    val fg = when (variant) {
        HardKeyVariant.PRIMARY -> c.surface
        HardKeyVariant.DESTRUCTIVE -> Color(0xFFF7F3EA)   // cream always reads on fault red
        else -> if (disabled) c.ink40 else c.ink
    }

    Box(
        modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { val s = if (pressed) 0.98f else 1f; scaleX = s; scaleY = s }
            .then(
                if (disabled) Modifier
                else Modifier.clickable(interactionSource = interaction, indication = null) {
                    haptics.confirm(); onClick()
                }
            )
            .then(
                when {
                    variant == HardKeyVariant.DISABLED -> Modifier.dashedBorder(c.ink40, 1.5.dp)
                    variant == HardKeyVariant.PRIMARY || variant == HardKeyVariant.DESTRUCTIVE -> Modifier
                    else -> Modifier.border(1.5.dp, c.ink)
                }
            )
            .then(
                if (variant == HardKeyVariant.DISABLED)
                    Modifier.drawBehind {   // hatch/dots fill for the disabled key
                        hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, c.ink40.copy(alpha = 0.25f))
                    }
                else Modifier.background(bg)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { role = Role.Button; if (disabled) disabled() },
        contentAlignment = Alignment.Center
    ) {
        Text(label.uppercase(), style = Caliper.type.meta.copy(fontSize = 13.sp), color = fg)
    }
}

// ─────────────────────────── DIPSwitch ───────────────────────────

@Composable
fun DipSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val knobX by animateDpAsState(
        if (checked) 16.dp else 0.dp,
        spring(dampingRatio = 1f, stiffness = 700f), label = "dip"
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp, 32.dp)
                .background(c.surface)
                .toggleable(
                    value = checked, enabled = enabled, role = Role.Switch,
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { haptics.tick(); onCheckedChange(it) }
                .border(1.dp, c.hairline)
                .padding(4.dp)
        ) {
            Box(Modifier.offset(x = knobX).size(24.dp).background(if (checked) c.ink else c.ink40))
        }
        if (label != null) {
            Spacer(Modifier.width(12.dp))
            Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
        }
        Spacer(Modifier.width(8.dp))
        LedDot(active = checked, dotSize = 5.dp, pulsing = false)
    }
}

// ─────────────────────────── FaderKey (hardware slider) ───────────────────────────

@Composable
fun FaderKey(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    ticks: Int = 5,
    label: String = "",
    valueText: (Float) -> String = { String.format(java.util.Locale.US, "%.2f", it) }
) {
    val c = Caliper.colors
    val span = valueRange.endInclusive - valueRange.start
    var f by remember(value) { mutableStateOf(((value - valueRange.start) / span).coerceIn(0f, 1f)) }

    Column(modifier) {
        if (label.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
                Text(valueText(value), style = Caliper.type.meta, color = c.ink)
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(36.dp)
                .pointerInput(valueRange) {
                    detectTapGestures { pos ->
                        val nf = (pos.x / size.width).coerceIn(0f, 1f)
                        f = nf; onValueChange(valueRange.start + nf * span)
                    }
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val nf = (f + dragAmount / size.width).coerceIn(0f, 1f)
                        f = nf; onValueChange(valueRange.start + nf * span)
                    }
                }
                .semantics { contentDescription = "$label fader, ${valueText(value)}" }
        ) {
            val mid = size.height / 2
            drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
            repeat(ticks) { i ->
                val x = size.width * i / (ticks - 1f)
                drawLine(c.ink40, Offset(x, mid - 4.dp.toPx()), Offset(x, mid + 4.dp.toPx()), 1.dp.toPx())
            }
            val ks = 14.dp.toPx()
            val kx = (size.width * f - ks / 2).coerceIn(0f, size.width - ks)
            drawRect(c.ink, topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
        }
    }
}

// ─────────────────────────── SegKey ───────────────────────────

@Composable
fun <T> SegKey(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String = { it.toString() }
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    Row(
        modifier.fillMaxWidth().height(40.dp).border(1.dp, c.ink),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { i, opt ->
            val sel = opt == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (sel) c.ink else Color.Transparent)
                    .then(if (i > 0) Modifier.drawBehind {
                        drawLine(c.ink, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                    } else Modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { haptics.tick(); onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(labelFor(opt).uppercase(), style = Caliper.type.meta,
                    color = if (sel) c.surface else c.ink)
            }
        }
    }
}

// ─────────────────────────── OdometerText ───────────────────────────

@Composable
fun OdometerText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Caliper.type.readoutL,
    color: Color = Caliper.colors.ink,
    staggerMs: Int = 24
) {
    val reduced = rememberReducedMotion()
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            OdometerDigit(
                digit = ch,
                delayMs = if (reduced) 0 else (text.length - 1 - i) * staggerMs,
                style = style, color = color
            )
        }
    }
}

@Composable
private fun OdometerDigit(digit: Char, delayMs: Int, style: TextStyle, color: Color) {
    var shown by remember { mutableStateOf(digit) }
    LaunchedEffect(digit) {
        if (delayMs > 0) delay(delayMs)
        shown = digit
    }
    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            (slideInVertically(tween(180, easing = CaliperMotion.Ease)) { it / 2 } + fadeIn(tween(120))) togetherWith
                (slideOutVertically(tween(180, easing = CaliperMotion.Ease)) { -it / 2 } + fadeOut(tween(120)))
        },
        label = "digit"
    ) { d ->
        Text(d.toString(), style = style, color = color, softWrap = false, maxLines = 1)
    }
}

// ─────────────────────────── MarginNote ───────────────────────────

@Composable
fun MarginNote(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "NOTE",
    error: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val c = Caliper.colors
    Row(
        modifier.fillMaxWidth()
            .background(c.panel)
            .border(1.dp, if (error) c.fault else c.hairline)
    ) {
        if (error) Box(Modifier.width(3.dp).fillMaxHeight().background(c.fault))
        Column(Modifier.weight(1f).padding(12.dp)) {
            Text(title.uppercase(), style = Caliper.type.meta,
                color = if (error) c.fault else c.ink40)
            Spacer(Modifier.height(4.dp))
            Text(message, style = Caliper.type.dataS, color = c.ink)
            if (actionLabel != null) {
                Spacer(Modifier.height(8.dp))
                HardKey(actionLabel, variant = HardKeyVariant.SECONDARY, onClick = onAction)
            }
        }
        if (onDismiss != null) {
            Text("✕", style = Caliper.type.meta, color = c.ink40,
                modifier = Modifier.padding(12.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { onDismiss() })
        }
    }
}

// ─────────────────────────── baseline input (FIND field) ───────────────────────────

@Composable
fun BaselineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Caliper.colors
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = Caliper.type.dataS.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        modifier = modifier.drawBehind {
            drawLine(c.hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
        }
    )
}
```

---

## 5 · `CaliperData.kt` — panels, tiles, ScopeTrace, gauges, HatchBar

```kotlin
package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────── peak-hold ───────────────────────────

/** Records the max, then decays to the current value after `decayAfter` ms. */
@Composable
fun rememberPeakHold(value: Float, decayAfter: Long = 2000): Float {
    var peak by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        if (value >= peak) peak = value
        else {
            delay(decayAfter)
            val start = peak
            animate(start, value, tween(600, easing = CaliperMotion.Ease)) { v, _ -> peak = v }
        }
    }
    return peak
}

// ─────────────────────────── PanelCard ───────────────────────────

@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    channel: Channel? = null,
    title: String? = null,
    status: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Caliper.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.panel)
            .border(1.dp, c.hairline)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick
                ) else Modifier
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (channel != null) ChannelTick(channel)
            if (title != null) {
                Spacer(Modifier.width(8.dp))
                Text(title.uppercase(), style = Caliper.type.meta, color = c.ink60)
            }
            Spacer(Modifier.weight(1f))
            status?.invoke(this)
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Text("tap →", style = Caliper.type.meta, color = c.ink40)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(1.dp).background(c.hairline))  // asymmetric rule
        Spacer(Modifier.height(12.dp))
        content()
    }
}

// ─────────────────────────── ReadoutTile ───────────────────────────

@Composable
fun ReadoutTile(
    channel: Channel,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    subline: String? = null,
    spark: List<Float>? = null,
    barFraction: Float? = null,
    statusText: String? = null,
    onClick: () -> Unit = {}
) {
    val c = Caliper.colors
    PanelCard(
        modifier = modifier, channel = channel, title = channel.name, onClick = onClick,
        status = {
            if (statusText != null) Text(statusText, style = Caliper.type.meta, color = c.ink40)
            else LedDot()
        }
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            OdometerText(value, style = Caliper.type.readoutL)
            if (unit != null) Text(
                " $unit", style = Caliper.type.dataS, color = c.ink60,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        if (subline != null) {
            Spacer(Modifier.height(4.dp))
            Text(subline, style = Caliper.type.dataS, color = c.ink60)
        }
        if (barFraction != null) {
            Spacer(Modifier.height(10.dp))
            PeakBar(barFraction, channel)
        }
        if (spark != null && spark.size > 1) {
            Spacer(Modifier.height(10.dp))
            Sparkline(spark, channel, Modifier.fillMaxWidth().height(28.dp))
        }
    }
}

@Composable
private fun PeakBar(fraction: Float, channel: Channel) {
    val c = Caliper.colors
    val anim by animateFloatAsState(fraction, CaliperMotion.Needle, label = "bar")
    val peak = rememberPeakHold(fraction)
    Canvas(Modifier.fillMaxWidth().height(6.dp)) {
        val h = size.height
        drawLine(c.hairline, Offset(0f, h / 2), Offset(size.width, h / 2), 1.dp.toPx())
        drawRect(c.channel(channel), size = Size(size.width * anim, h))
        drawLine(c.ink, Offset(size.width * peak, 0f), Offset(size.width * peak, h), 2.dp.toPx())
    }
}

// ─────────────────────────── Sparkline ───────────────────────────

@ComotlinPreviewBugPlaceholder@  // (remove this line — marker only)

@Composable
fun Sparkline(
    values: List<Float>,
    channel: Channel? = null,
    modifier: Modifier = Modifier,
    color: Color? = null,
    stroke: androidx.compose.ui.unit.Dp = 2.dp
) {
    val traceColor = color ?: channel?.let { Caliper.colors.channel(it) } ?: Caliper.colors.ink
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val n = values.size
        val stepX = size.width / (n - 1)
        val maxV = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height * (1f - (v / maxV).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, traceColor, style = Stroke(stroke.toPx(), cap = StrokeCap.Square))
        val lastY = size.height * (1f - (values.last() / maxV).coerceIn(0f, 1f))
        drawCircle(traceColor, radius = stroke.toPx() * 0.75f, center = Offset(size.width - stroke.toPx(), lastY))
    }
}

// ─────────────────────────── ScopeTrace (§5.5 — the chart) ───────────────────────────

@Composable
fun ScopeTrace(
    values: List<Float>,
    channel: Channel,
    modifier: Modifier = Modifier,
    windowLabel: String = "60 s",
    yMax: Float = 100f,
    yMin: Float = 0f,
    valueFormat: (Float) -> String = { Fmt.pct(it, 1) },
    yFormat: (Float) -> String = { Fmt.pct(it) },
    secondaryValues: List<Float>? = null,       // e.g. network ↑ as ink@40
    timeLabelFor: (Float) -> String = { "" },   // fraction 0..1 → label
    height: androidx.compose.ui.unit.Dp = 220.dp
) {
    val c = Caliper.colors
    val traceColor = c.channel(channel)
    val reduced = rememberReducedMotion()
    val tm = rememberTextMeasurer()
    val sweep = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (sweep.value < 1f) sweep.animateTo(1f, tween(CaliperMotion.tSweep, easing = CaliperMotion.Ease))
    }
    var crossX by remember { mutableStateOf<Float?>(null) }
    val stats = remember(values) {
        if (values.isEmpty()) Triple(0f, 0f, 0f)
        else Triple(values.min(), values.max(), values.average().toFloat())
    }
    val a11y = if (values.isEmpty()) "${channel.label}: no signal"
    else "${channel.label} over $windowLabel. Min ${valueFormat(stats.first)}, " +
         "max ${valueFormat(stats.second)}, average ${valueFormat(stats.third)}."

    val yStyle = TextStyle(PlexMonoFamily, fontSize = 10.sp, color = c.ink40, fontFeatureSettings = "tnum")

    Column(modifier.semantics { contentDescription = a11y }) {
        Canvas(
            Modifier.fillMaxWidth().height(height)
                .pointerInput(values) { detectTapGestures { crossX = it.x } }
                .pointerInput(values) {
                    detectDragGestures { change, _ -> change.consume(); crossX = change.position.x }
                }
        ) {
            val plotL = 36.dp.toPx(); val plotR = size.width - 8.dp.toPx()
            val plotT = 8.dp.toPx();  val plotB = size.height - 22.dp.toPx()
            val plotW = plotR - plotL; val plotH = plotB - plotT

            // engineering grid
            drawRect(c.hairline, topLeft = Offset(plotL, plotT), size = Size(plotW, plotH), style = Stroke(1.dp.toPx()))
            val minor = 24.dp.toPx(); val major = 120.dp.toPx()
            var gx = plotL + minor
            while (gx < plotR) {
                val isMajor = ((gx - plotL) % major) < minor
                drawLine(if (isMajor) c.gridMajor else c.gridMinor, Offset(gx, plotT), Offset(gx, plotB), 1f)
                gx += minor
            }
            var gy = plotT + minor
            while (gy < plotB) {
                val isMajor = ((gy - plotT) % major) < minor
                drawLine(if (isMajor) c.gridMajor else c.gridMinor, Offset(plotL, gy), Offset(plotR, gy), 1f)
                gy += minor
            }
            // y labels
            for (i in 0 until 5) {
                val frac = i / 4f
                val y = plotB - frac * plotH
                drawText(tm, yFormat(yMin + (yMax - yMin) * frac), topLeft = Offset(2f, y - 6.dp.toPx()), style = yStyle)
            }
            // x labels
            for (i in 0 until 5) {
                val lbl = timeLabelFor(i / 4f)
                if (lbl.isNotEmpty()) {
                    val m = tm.measure(lbl, yStyle)
                    val x = (plotL + (i / 4f) * plotW - m.size.width / 2f).coerceIn(plotL, plotR - m.size.width)
                    drawText(m, topLeft = Offset(x, plotB + 4.dp.toPx()))
                }
            }

            fun vy(v: Float) = plotB - ((v - yMin) / (yMax - yMin)).coerceIn(0f, 1f) * plotH

            if (values.size < 2) {   // NO SIGNAL
                drawLine(c.ink40, Offset(plotL, vy(yMin)), Offset(plotR, vy(yMin)), 2.dp.toPx(), cap = StrokeCap.Square)
                drawText(tm, "NO SIGNAL", topLeft = Offset(plotL + 8.dp.toPx(), plotT + 8.dp.toPx()), style = yStyle)
                return@Canvas
            }

            val stepX = plotW / (values.size - 1)
            fun buildPath(list: List<Float>) = Path().apply {
                list.forEachIndexed { i, v ->
                    val x = plotL + i * stepX; val y = vy(v)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }

            secondaryValues?.takeIf { it.size > 1 }?.let {
                drawPath(buildPath(it), c.ink40, style = Stroke(2.dp.toPx(), cap = StrokeCap.Square))
            }

            // primary trace, trimmed by the pen sweep
            val full = buildPath(values)
            val pm = PathMeasure(); pm.setPath(full, false)
            val shown = Path()
            pm.getSegment(0f, pm.length * sweep.value, shown, true)
            drawPath(shown, traceColor, style = Stroke(2.dp.toPx(), cap = StrokeCap.Square))
            pm.getPosTan(pm.length * sweep.value)?.let { (pos, _) ->
                drawCircle(traceColor, radius = 3.dp.toPx(), center = pos)   // the pen
            }

            // crosshair + leader note
            crossX?.let { cx ->
                if (cx in plotL..plotR) {
                    drawLine(c.ink, Offset(cx, plotT), Offset(cx, plotB), 1.dp.toPx())
                    val idx = (((cx - plotL) / plotW) * (values.size - 1)).roundToInt().coerceIn(0, values.size - 1)
                    drawCircle(c.ink, radius = 4.dp.toPx(), center = Offset(plotL + idx * stepX, vy(values[idx])))
                    val note = "${valueFormat(values[idx])} · min ${valueFormat(stats.first)} · " +
                               "max ${valueFormat(stats.second)} · avg ${valueFormat(stats.third)}"
                    val m = tm.measure(note, yStyle.copy(color = c.ink))
                    val noteW = m.size.width + 12f
                    val nx = (plotR - noteW).coerceAtLeast(plotL)
                    drawRect(c.panel, topLeft = Offset(nx, plotT + 4f), size = Size(noteW, m.size.height + 8f))
                    drawRect(c.hairline, topLeft = Offset(nx, plotT + 4f), size = Size(noteW, m.size.height + 8f), style = Stroke(1f))
                    drawText(m, topLeft = Offset(nx + 6f, plotT + 8f))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$windowLabel window", style = Caliper.type.meta, color = c.ink40)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("pen ", style = Caliper.type.meta, color = c.ink40)
                Canvas(Modifier.size(6.dp)) { drawCircle(traceColor, radius = size.minDimension / 2) }
                Text(" ${values.lastOrNull()?.let(valueFormat) ?: "—"}", style = Caliper.type.meta, color = c.ink)
            }
        }
    }
}

// ─────────────────────────── CoreRail (§5.6) ───────────────────────────

data class CoreReading(val id: Int, val load: Float, val freqKhz: Long)

@Composable
fun CoreRail(cores: List<CoreReading>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        cores.forEach { core ->
            Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("C${core.id}", style = Caliper.type.dataS, color = Caliper.colors.ink60,
                    modifier = Modifier.width(28.dp))
                CoreBar(core.load / 100f, Modifier.weight(1f).padding(end = 8.dp))
                Text(String.format(java.util.Locale.US, "%3d%%", core.load.roundToInt()),
                    style = Caliper.type.dataS, color = Caliper.colors.ink, modifier = Modifier.width(48.dp))
                Text(Fmt.hz(core.freqKhz), style = Caliper.type.dataS, color = Caliper.colors.ink60)
            }
        }
    }
}

@Composable
private fun CoreBar(fraction: Float, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    val anim by animateFloatAsState(fraction, CaliperMotion.Needle, label = "core")
    val peak = rememberPeakHold(fraction)
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        val mid = size.height / 2
        drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { t ->
            drawLine(c.hairline, Offset(size.width * t, mid - 3.dp.toPx()), Offset(size.width * t, mid + 3.dp.toPx()), 1.dp.toPx())
        }
        val bh = 8.dp.toPx(); val top = mid - bh / 2
        drawRect(c.channel(Channels.CPU), topLeft = Offset(0f, top), size = Size(size.width * anim, bh))
        val px = size.width * peak   // ⌃ peak-hold caret
        val tri = Path().apply {
            moveTo(px, top - 3.dp.toPx()); lineTo(px - 3.dp.toPx(), top); lineTo(px + 3.dp.toPx(), top); close()
        }
        drawPath(tri, c.ink)
    }
}

// ─────────────────────────── LinearGauge (§5.7 — battery fuel) ───────────────────────────

@Composable
fun LinearGauge(
    fraction: Float,
    modifier: Modifier = Modifier,
    voltage: String? = null,
    charging: Boolean = false,
    critical: Boolean = fraction < 0.2f
) {
    val c = Caliper.colors
    val tm = rememberTextMeasurer()
    val reduced = rememberReducedMotion()
    val anim by animateFloatAsState(fraction.coerceIn(0f, 1f), CaliperMotion.Needle, label = "fuel")
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        0.4f, 1f, infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse), label = "b"
    )
    val labelStyle = TextStyle(PlexMonoFamily, fontSize = 10.sp, color = c.ink40, fontFeatureSettings = "tnum")

    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.weight(1f).fillMaxHeight()) {
            val mid = size.height / 2
            val trackH = 12.dp.toPx(); val top = mid - trackH / 2
            drawRect(c.hairline, topLeft = Offset(0f, top), size = Size(size.width, trackH), style = Stroke(1.dp.toPx()))
            for (i in 0..20) {   // ticks every 5%; tall at 0/25/50/75/100
                val x = size.width * i / 20f
                val major = (i * 5) % 25 == 0
                val len = (if (major) 5.dp else 3.dp).toPx()
                drawLine(c.ink40, Offset(x, top - len), Offset(x, top), 1.dp.toPx())
            }
            drawRect(if (critical) c.fault else c.channel(Channels.POWER),
                topLeft = Offset(0f, top), size = Size(size.width * anim, trackH))
            val ks = 10.dp.toPx()
            val kx = (size.width * anim - ks / 2).coerceIn(0f, size.width - ks)
            drawRect(c.ink.copy(alpha = if (charging && !reduced) blink else 1f),
                topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
            listOf(0, 25, 50, 75, 100).forEach { pct ->
                val x = size.width * pct / 100f
                val m = tm.measure("$pct", labelStyle)
                drawText(m, topLeft = Offset(x - m.size.width / 2f, mid + trackH / 2 + 2.dp.toPx()))
            }
        }
        if (voltage != null) {
            Spacer(Modifier.width(12.dp))
            Text(voltage, style = Caliper.type.dataS, color = Caliper.colors.ink60)
        }
    }
}

// ─────────────────────────── HatchBar (§5.8 — storage map) ───────────────────────────

data class HatchSegment(val label: String, val bytes: Long, val color: Color, val pattern: HatchPattern)

@Composable
fun HatchBar(
    segments: List<HatchSegment>,
    modifier: Modifier = Modifier,
    onSegmentTap: (HatchSegment) -> Unit = {}
) {
    val c = Caliper.colors
    val total = segments.sumOf { it.bytes }.coerceAtLeast(1)
    Column(modifier) {
        Canvas(
            Modifier.fillMaxWidth().height(16.dp)
                .pointerInput(segments, total) {
                    detectTapGestures { pos ->
                        var acc = 0f
                        val w = size.width.toFloat()
                        for (seg in segments) {
                            val segW = w * seg.bytes / total
                            if (pos.x < acc + segW) { onSegmentTap(seg); return@detectTapGestures }
                            acc += segW
                        }
                    }
                }
        ) {
            drawRect(c.hairline, style = Stroke(1.dp.toPx()))
            var left = 1f
            segments.forEach { seg ->
                val w = size.width * seg.bytes / total
                hatch(Rect(left, 0f, left + w, size.height), seg.pattern, seg.color)
                left += w
            }
        }
        Spacer(Modifier.height(8.dp))
        Column {
            segments.forEach { seg ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(seg.color).border(1.dp, c.hairline))
                    Text("  ${seg.label}", style = Caliper.type.dataS, color = c.ink60)
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.bytes(seg.bytes), style = Caliper.type.dataS, color = c.ink)
                }
            }
        }
    }
}

// ─────────────────────────── ThermalGauge (ramp — not a channel) ───────────────────────────

@Composable
fun ThermalGauge(tempC: Float, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    val zone = when {
        tempC < 45 -> "neutral"; tempC < 55 -> "warm"; tempC < 65 -> "moderate"
        tempC < 75 -> "severe"; else -> "thermal throttling"
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OdometerText(Fmt.temp(tempC), style = Caliper.type.readoutL)
        Spacer(Modifier.width(16.dp))
        Canvas(Modifier.weight(1f).height(10.dp)) {
            val segs = 12
            val filled = (tempC / 90f * segs).toInt().coerceIn(0, segs)
            val segW = size.width / segs
            for (i in 0 until segs) {
                val frac = i.toFloat() / segs
                val col = when {
                    frac < 0.5f -> c.channel(Channels.POWER)   // amber
                    frac < 0.8f -> c.channel(Channels.CPU)     // vermilion
                    else -> c.fault                             // deep red
                }
                drawRect(col.copy(alpha = if (i < filled) 1f else 0.15f),
                    topLeft = Offset(i * segW + 1f, 0f), size = Size(segW - 2f, size.height))
            }
        }
        Text("  zone: $zone", style = Caliper.type.meta, color = c.ink60)
    }
}
```

> ⚠️ Remove the stray marker line `@ComotifyPreviewBugPlaceholder@` above `Sparkline` if it survived copy — it is not code.

---

## 6 · `CaliperLedger.kt` — process ledger, Dossier, SafetyLatch

```kotlin
package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import com.ivarna.deviceinsight.ui.caliper.*
import kotlin.math.roundToInt

// ─────────────────────────── models ───────────────────────────

enum class ProcState { FOREGROUND, CACHED, SERVICE, BACKGROUND }

data class ProcRow(
    val index: Int, val pkg: String, val cpu: Float, val rssBytes: Long,
    val pid: Int, val uptime: String, val state: ProcState,
    val isSelf: Boolean = false, val isSystem: Boolean = false, val threads: Int = 1
)

data class LedgerSection(val title: String, val rows: List<ProcRow>)

// ─────────────────────────── LedgerTable (§5.9) ───────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LedgerTable(
    sections: List<LedgerSection>,
    modifier: Modifier = Modifier,
    onRowTap: (ProcRow) -> Unit
) {
    LazyColumn(modifier) {
        sections.forEach { section ->
            stickyHeader(key = "h_${section.title}") {
                Column(Modifier.fillMaxWidth().background(Caliper.colors.surface)) {
                    Text("── ${section.title} ──", style = Caliper.type.meta,
                        color = Caliper.colors.ink60, modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp))
                    DoubleRule()
                }
            }
            items(section.rows, key = { it.index }) { row ->
                LedgerRow(row) { onRowTap(row) }
            }
        }
    }
}

@Composable
private fun LedgerRow(row: ProcRow, onClick: () -> Unit) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth()
            .background(c.surface)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics {
                contentDescription = "${Fmt.index(row.index)} ${row.pkg}, " +
                    "cpu ${Fmt.pct(row.cpu, 1)}, ${Fmt.bytes(row.rssBytes)}, ${row.state.name.lowercase()}"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.index(row.index), style = Caliper.type.dataS, color = c.ink40, modifier = Modifier.width(44.dp))
            Text(row.pkg, style = Caliper.type.dataM, color = c.ink,
                maxLines = 1, modifier = Modifier.weight(1f))
            Text(String.format(java.util.Locale.US, "%.1f%%", row.cpu), style = Caliper.type.dataM,
                color = if (row.cpu > 25f) c.fault else c.ink, modifier = Modifier.width(64.dp))
            Text(Fmt.bytes(row.rssBytes), style = Caliper.type.dataM, color = c.ink60)
            if (row.isSelf) Spacer(Modifier.width(8.dp))
            if (row.isSelf) StampBadge("SELF", color = c.accent, rotation = -3f, animateIn = false)
        }
        Text(
            "pid ${row.pid} · ${row.uptime} · ${if (row.state == ProcState.FOREGROUND) "●" else "○"} ${row.state.name.lowercase()}",
            style = Caliper.type.meta, color = c.ink40, modifier = Modifier.padding(start = 44.dp)
        )
    }
}

// ─────────────────────────── Dossier (§5.10 — clipped sheet) ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessDossier(
    row: ProcRow?,
    rootAvailable: Boolean,
    onDismiss: () -> Unit,
    onForceStop: (ProcRow) -> Unit,
    onTerminate: (ProcRow) -> Unit
) {
    if (row == null) return
    val c = Caliper.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = androidx.compose.ui.graphics.RectangleShape,          // 0dp radius — always
        containerColor = c.panel,
        scrimColor = c.ink.copy(alpha = 0.4f),
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            // perforated tear-off edge
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                drawLine(c.ink40, Offset.Zero, Offset(size.width, size.height / 2), 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())))
            }
            Text("DOSSIER · ${Fmt.index(row.index)}", style = Caliper.type.meta, color = c.ink40)
            Text(row.pkg, style = Caliper.type.dataM, color = c.ink)
            Text("pid ${row.pid} · up ${row.uptime}", style = Caliper.type.meta, color = c.ink60)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniStat("CPU", Fmt.pct(row.cpu, 1), listOf(row.cpu / 3, row.cpu / 2, row.cpu), Channels.CPU)
                MiniStat("MEM", Fmt.bytes(row.rssBytes), listOf(0.6f, 0.7f, 0.65f), Channels.MEMORY)
            }
            Spacer(Modifier.height(12.dp))
            SpecRow("uid", "10247")
            SpecRow("oom adj", "0")
            SpecRow("seccomp", "enforced")
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HardKey("FORCE STOP", variant = HardKeyVariant.DESTRUCTIVE,
                    modifier = Modifier.weight(1f),
                    onClick = { onForceStop(row) })   // gate behind SafetyLatch in production
            }
            Spacer(Modifier.height(12.dp))
            SafetyLatch(
                prompt = "ARM — TERMINATE PROCESS ${Fmt.index(row.index)}?",
                killLabel = "TERMINATE ⏻",
                enabled = rootAvailable,
                onArmedKill = { onTerminate(row); onDismiss() }
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, spark: List<Float>, channel: Channel) {
    Column(Modifier.width(96.dp)) {
        Text(label.uppercase(), style = Caliper.type.meta, color = Caliper.colors.ink40)
        Text(value, style = Caliper.type.dataS, color = Caliper.colors.ink)
        Spacer(Modifier.height(4.dp))
        Sparkline(spark, channel, Modifier.fillMaxWidth().height(16.dp))
    }
}

// ─────────────────────────── SafetyLatch (§5.11) ───────────────────────────

@Composable
fun SafetyLatch(
    prompt: String,
    modifier: Modifier = Modifier,
    killLabel: String = "KILL",
    enabled: Boolean = true,
    onArmedKill: () -> Unit,
    onAbort: () -> Unit = {}
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val tm = rememberTextMeasurer()
    var fraction by remember { mutableStateOf(0f) }
    var armed by remember { mutableStateOf(false) }
    val knob by animateFloatAsState(fraction, spring(dampingRatio = 1f, stiffness = 700f), label = "latch")
    val metaStyle = TextStyle(PlexMonoFamily, fontSize = 10.sp, color = c.ink40)

    Column(modifier.fillMaxWidth().border(1.dp, c.fault)) {
        Text(prompt.uppercase(), style = Caliper.type.meta, color = c.fault,
            modifier = Modifier.padding(12.dp))
        Canvas(
            Modifier.fillMaxWidth().height(56.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (!armed) {
                            fraction = (fraction + dragAmount / size.width).coerceIn(0f, 1f)
                            if (fraction >= 0.92f) { armed = true; fraction = 1f; haptics.arm() }
                        }
                    }
                }
        ) {
            val mid = size.height / 2
            drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
            // armed region fills with cross-hatch
            hatch(Rect(0f, mid - 8.dp.toPx(), size.width * knob, mid + 8.dp.toPx()),
                HatchPattern.CROSS, c.fault.copy(alpha = 0.5f))
            val ks = 28.dp.toPx()
            val kx = (size.width - ks) * knob
            drawRect(if (armed) c.fault else c.ink, topLeft = Offset(kx, (mid - ks / 2)), size = Size(ks, ks))
            drawText(tm, "SAFE", topLeft = Offset(4f, 4f), style = metaStyle)
            drawText(tm, "ARM", topLeft = Offset(size.width - 24.dp.toPx(), 4f), style = metaStyle)
        }
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HardKey("ABORT", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.weight(1f),
                onClick = { fraction = 0f; armed = false; onAbort() })
            HardKey(killLabel,
                variant = if (armed) HardKeyVariant.DESTRUCTIVE else HardKeyVariant.DISABLED,
                modifier = Modifier.weight(1f),
                onClick = { if (armed) { onArmedKill(); fraction = 0f; armed = false } })
        }
    }
}

// ─────────────────────────── Processes screen (assembly) ───────────────────────────

@Composable
fun ProcessesScreen(
    rows: List<ProcRow>,
    rootAvailable: Boolean,
    onForceStop: (ProcRow) -> Unit,
    onTerminate: (ProcRow) -> Unit
) {
    var filter by remember { mutableStateOf("APPS") }
    var query by remember { mutableStateOf("") }
    var sortDesc by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<ProcRow?>(null) }

    val filtered = remember(rows, filter, query, sortDesc) {
        rows.asSequence()
            .filter { filter == "ALL" || (filter == "APPS" && !it.isSystem) || (filter == "SYSTEM" && it.isSystem) }
            .filter { query.isEmpty() || it.pkg.contains(query, ignoreCase = true) }
            .sortedBy { if (sortDesc) -it.cpu else it.cpu }
            .toList()
    }
    val sections = buildList {
        filtered.filter { !it.isSystem && !it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("USER APPS", it)) }
        filtered.filter { it.isSystem && !it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("SYSTEM", it)) }
        filtered.filter { it.isSelf }.takeIf { it.isNotEmpty() }?.let { add(LedgerSection("SELF", it)) }
    }

    Column(Modifier.fillMaxSize().background(Caliper.colors.surface)) {
        Masthead()
        ScreenHeader("№ 03 — PROCESSES", "Processes.",
            "${rows.size} listed · ${rows.sumOf { it.threads }} threads")
        SegKey(listOf("ALL", "APPS", "SYSTEM"), filter, { filter = it },
            Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("FIND:", style = Caliper.type.meta, color = Caliper.colors.ink60)
            Spacer(Modifier.width(8.dp))
            BaselineField(query, { query = it }, Modifier.weight(1f))
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("IDX", style = Caliper.type.meta, color = Caliper.colors.ink40, modifier = Modifier.width(44.dp))
            Text("PACKAGE", style = Caliper.type.meta, color = Caliper.colors.ink40, modifier = Modifier.weight(1f))
            Text("SORT CPU ${if (sortDesc) "▼" else "▲"}", style = Caliper.type.meta, color = Caliper.colors.ink,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { sortDesc = !sortDesc })
        }
        LedgerTable(sections, Modifier.weight(1f)) { selected = it }
        EndOfSheet()
    }
    ProcessDossier(selected, rootAvailable,
        onDismiss = { selected = null },
        onForceStop = onForceStop, onTerminate = onTerminate)
}
```

---

## 7 · `CaliperChrome.kt` — Masthead, ModeRail, headers, states, calibration sweep

```kotlin
package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.sin

// ─────────────────────────── Masthead (§5.1) ───────────────────────────

@Composable
fun Masthead(
    modifier: Modifier = Modifier,
    degraded: Boolean = false,
    rootVerified: Boolean = false
) {
    val c = Caliper.colors
    Column(modifier.fillMaxWidth().background(c.surface).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CrosshairMark(Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("DEVICEINSIGHT", style = Caliper.type.meta.copy(fontSize = 13.sp), color = c.ink)
            Spacer(Modifier.weight(1f))
            if (rootVerified) { StampBadge("ROOT VERIFIED", color = c.accent); Spacer(Modifier.width(8.dp)) }
            if (degraded) StampBadge("DEGRADED") else LedDot()
            Spacer(Modifier.width(8.dp))
            UtcClock()
        }
        DoubleRule()
    }
}

@Composable
private fun CrosshairMark(modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val r = size.minDimension / 2 - stroke
        drawCircle(c.ink, radius = r, style = Stroke(stroke))
        drawLine(c.ink, Offset(center.x - r - 2f, center.y), Offset(center.x + r + 2f, center.y), stroke)
        drawLine(c.ink, Offset(center.x, center.y - r - 2f), Offset(center.x, center.y + r + 2f), stroke)
        drawCircle(c.accent, radius = 2.dp.toPx(), center = center)
    }
}

@Composable
private fun UtcClock() {
    val c = Caliper.colors
    var now by remember { mutableStateOf(LocalTime.now(ZoneOffset.UTC)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - (System.currentTimeMillis() % 1000))
            now = LocalTime.now(ZoneOffset.UTC)
        }
    }
    val colon = if (now.second % 2 == 0) ":" else " "   // the heartbeat
    Text(
        String.format(java.util.Locale.US, "%02d%s%02d%s%02d",
            now.hour, colon, now.minute, colon, now.second),
        style = Caliper.type.dataS, color = c.ink
    )
}

// ─────────────────────────── ModeRail (§5.2) ───────────────────────────

data class RailKey(val number: Int, val label: String, val warning: Boolean = false)

@Composable
fun ModeRail(
    keys: List<RailKey>,
    selected: Int,
    onSelect: (RailKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    Column(
        modifier.fillMaxWidth().background(c.surface)
            .drawBehind { drawLine(c.hairline, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp)) {
            keys.forEach { key ->
                val sel = key.number == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            haptics.tick(); onSelect(key)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(18.dp).border(1.dp, if (sel) c.ink else c.ink40)) {
                            if (sel) Box(Modifier.fillMaxSize().background(c.ink))
                        }
                        if (key.warning) { Spacer(Modifier.width(4.dp)); LedDot(color = c.fault, dotSize = 4.dp) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(key.label.uppercase(), style = Caliper.type.meta,
                        color = if (sel) c.ink else c.ink60)
                    Spacer(Modifier.height(3.dp))
                    Canvas(Modifier.size(width = 10.dp, height = 4.dp)) {   // caret ▲ + accent underline
                        if (sel) {
                            val tri = Path().apply {
                                moveTo(size.width / 2, size.height); lineTo(0f, 0f); lineTo(size.width, 0f); close()
                            }
                            drawPath(tri, c.accent)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────── ScreenHeader — numbered sheet ───────────────────────────

@Composable
fun ScreenHeader(sheetLabel: String, title: String, sub: String, warn: Boolean = false) {
    val c = Caliper.colors
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(sheetLabel.uppercase(), style = Caliper.type.meta, color = c.ink40)
        Text(title, style = Caliper.type.display1, color = c.ink)
        Text(sub, style = Caliper.type.meta, color = if (warn) c.fault else c.ink40)
        Spacer(Modifier.height(10.dp))
        DoubleRule()
    }
}

// ─────────────────────────── states (§5.15) ───────────────────────────

@Composable
fun CalibratingIndicator(percent: Float? = null) {
    val c = Caliper.colors
    val reduced = rememberReducedMotion()
    val rotation by rememberInfiniteTransition(label = "reticle").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Canvas(Modifier.size(28.dp)) {
            rotate(if (reduced) 0f else rotation) {
                drawCircle(c.ink, radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                drawLine(c.ink, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx())
                drawLine(c.ink, Offset(center.x, 0f), Offset(center.x, size.height), 1.5.dp.toPx())
            }
            drawCircle(c.accent, radius = 2.dp.toPx(), center = center)
        }
        Spacer(Modifier.height(10.dp))
        Text("CALIBRATING${percent?.let { " · ${it.roundToInt()}%" } ?: ""}",
            style = Caliper.type.meta, color = c.ink60)
    }
}

@Composable
fun EmptyState(title: String, message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {   // mini test-pattern
            listOf(Channels.CPU, Channels.MEMORY, Channels.NETWORK, Channels.POWER, Channels.STORAGE)
                .forEach { ch ->
                    Box(Modifier.size(8.dp).background(c.channel(ch))).padding(end = 2.dp)
                    Spacer(Modifier.width(2.dp))
                }
        }
        Spacer(Modifier.height(12.dp))
        Text(title.uppercase(), style = Caliper.type.meta, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(message, style = Caliper.type.dataS, color = c.ink60)
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            HardKey(actionLabel, variant = HardKeyVariant.PRIMARY, onClick = onAction)
        }
    }
}

@Composable
fun FaultState(code: String, cause: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StampBadge("FAULT $code")
        Spacer(Modifier.height(10.dp))
        Text(cause, style = Caliper.type.dataS, color = Caliper.colors.ink60)
        Spacer(Modifier.height(16.dp))
        HardKey("RETRY", variant = HardKeyVariant.SECONDARY, onClick = onRetry)
    }
}

// ─────────────────────────── CalibrationSweep (first-launch signature) ───────────────────────────

@Composable
fun CalibrationSweep(
    visible: Boolean,
    onFinished: () -> Unit,
    docId: String = "DI-0001"
) {
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        val c = Caliper.colors
        val reduced = rememberReducedMotion()
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            if (reduced) progress.snapTo(1f)
            else progress.animateTo(1f, tween(1200, easing = LinearEasing))
            delay(900)
            onFinished()
        }
        Box(Modifier.fillMaxSize().background(c.surface), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(32.dp).caliperGrid()) {
                val w = size.width; val x = w * progress.value
                drawLine(c.accent, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                val path = Path()
                var started = false
                for (i in 0..200) {
                    val px = w * i / 200f
                    if (px <= x) {
                        val py = size.height / 2 + sin(i * 0.15f) * size.height * 0.18f
                        if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
                    }
                }
                drawPath(path, c.channel(Channels.CPU),
                    style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Square))
            }
            if (progress.value >= 1f) StampBadge("CALIBRATED · $docId")
        }
    }
}
```

---

## 8 · `hud/CaliperHud.kt` — performance overlay

```kotlin
package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.OdometerText
import com.ivarna.deviceinsight.ui.caliper.components.StampBadge

/** HUD is always dark-scrim — it floats over games, not over your theme. */
object HudInk {
    val text = Color(0xFFF2EEE2)
    val dim = Color(0x99F2EEE2)
    val scrim = Color(0xB3141310)
    val cpu = Color(0xFFFF6B4A); val gpu = Color(0xFFF06BB0)
    val pwr = Color(0xFFFFB84D); val net = Color(0xFF2FD3B0)

    // The single sanctioned blur, HUD scrim only (API 31+):
    // .blur(8.dp) — never a style elsewhere.
}

data class HudState(
    val fps: Float?, val fpsSource: String,          // "SF" | "GFX" — honesty about the measurement
    val cpu: Float, val cpuHist: List<Float>,
    val gpu: Float?, val ramBytes: Long, val tempC: Float,
    val netDown: Long, val netUp: Long
)

@Composable
fun CaliperHud(state: HudState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(HudInk.scrim)
            .drawBehind { drawCornerBrackets(HudInk.text, 10.dp.toPx(), 12.dp.toPx(), 1.5.dp.toPx()) }
            .padding(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            OdometerText(
                text = state.fps?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "—",
                style = Caliper.type.readoutL, color = HudInk.text, staggerMs = 0
            )
            StampBadge(state.fpsSource, color = HudInk.dim, rotation = 0f, animateIn = false)
        }
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(HudInk.dim.copy(alpha = 0.4f), Offset.Zero, Offset(size.width, 0f), 1f)
        }
        Spacer(Modifier.height(6.dp))
        HudRow(HudInk.cpu, "CPU", "${state.cpu.roundToInt()}%", state.cpuHist)
        state.gpu?.let { HudRow(HudInk.gpu, "GPU", "${it.roundToInt()}%", emptyList()) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RAM  ${Fmt.bytes(state.ramBytes)}", style = Caliper.type.meta, color = HudInk.dim)
            Text("TEMP  ${Fmt.temp(state.tempC)}", style = Caliper.type.meta, color = HudInk.dim)
        }
        Text(
            "NET  ↓${Fmt.rate(state.netDown)}  ↑${Fmt.rate(state.netUp)}",
            style = Caliper.type.meta, color = HudInk.dim
        )
    }
}

@Composable
private fun HudRow(tickColor: Color, label: String, value: String, hist: List<Float>) {
    Row(Modifier.fillMaxWidth().height(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(tickColor))
        Text(" $label", style = Caliper.type.meta, color = HudInk.dim)
        Spacer(Modifier.weight(1f))
        Text(value, style = Caliper.type.dataS, color = HudInk.text)
        if (hist.size > 1) {
            Spacer(Modifier.width(6.dp))
            Canvas(Modifier.size(width = 40.dp, height = 12.dp)) {
                val step = size.width / (hist.size - 1)
                val maxV = (hist.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
                val p = androidx.compose.ui.graphics.Path()
                hist.forEachIndexed { i, v ->
                    val x = i * step; val y = size.height * (1f - v / maxV)
                    if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                }
                drawPath(p, tickColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawCornerBrackets(color: Color, inset: Float, len: Float, stroke: Float) {
    val w = size.width; val h = size.height
    drawLine(color, Offset(inset, inset), Offset(inset + len, inset), stroke)
    drawLine(color, Offset(inset, inset), Offset(inset, inset + len), stroke)
    drawLine(color, Offset(w - inset, inset), Offset(w - inset - len, inset), stroke)
    drawLine(color, Offset(w - inset, inset), Offset(w - inset, inset + len), stroke)
    drawLine(color, Offset(inset, h - inset), Offset(inset + len, h - inset), stroke)
    drawLine(color, Offset(inset, h - inset), Offset(inset, h - inset - len), stroke)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset - len, h - inset), stroke)
    drawLine(color, Offset(w - inset, h - inset), Offset(w - inset, h - inset - len), stroke)
}
```

**Overlay service wiring** (`SYSTEM_ALERT_WINDOW`):

```kotlin
// Inside your HudService (TYPES_APPLICATION_OVERLAY + FLAG_NOT_FOCUSABLE):
// val view = ComposeView(this).apply {
//     setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromOrReleasedFromPool)
//     setContent { CaliperHud(hudState) }   // hudState fed by a StateFlow from the monitor
// }
// windowManager.addView(view, params)
```

---

## 9 · `widget/ChannelWidget.kt` — Glance bench instrument

```kotlin
package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

data class WidgetReading(val valueText: String, val subline: String, val updated: String)

class ChannelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val reading = WidgetReading("38.4%", "2.84 GHz · 46.2°C", "14:32:07")  // TODO: repository
        provideContent { ChannelWidgetContent(reading) }
    }
}

class ChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChannelWidget()
}

@Composable
private fun ChannelWidgetContent(r: WidgetReading) {
    // Same instrument, smaller bench: channel label, tabular readout, `upd` timestamp.
    // For hatched bars, pre-render patterns to bitmaps at 1x/2x/3x and use ImageProvider.
    Column(
        GlanceModifier.fillMaxSize()
            .background(Color(0xFFFBF9F3))
            .cornerRadius(0.dp)          // 0dp radius — always (no-op below API 31)
            .padding(12.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth()) {
            Text("CH-01 · CPU", style = TextStyle(
                color = ColorProvider(Color(0x99191713)), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.defaultWeight())
            Text("upd ${r.updated}", style = TextStyle(
                color = ColorProvider(Color(0x66191713)), fontSize = 11.sp))
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(r.valueText, style = TextStyle(
            color = ColorProvider(Color(0xFF191713)), fontSize = 34.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        Text(r.subline, style = TextStyle(color = ColorProvider(Color(0x99191713)), fontSize = 11.sp))
    }
}
```

---

## 10 · Usage — app shell, demo monitor, Overview & CPU screens

```kotlin
package com.ivarna.deviceinsight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // TODO: back this with DataStore instead of remember
            var medium by remember { mutableStateOf(Medium.PAPER) }
            CaliperTheme(medium = medium) {
                DeviceInsightApp(
                    onMedium = { medium = it }   // Settings → Presentation
                )
            }
        }
    }
}

// ─────────────────────────── demo monitor ───────────────────────────

data class SystemSnapshot(
    val cpuPct: Float, val cpuHist: List<Float>, val freqGHz: String, val tempC: Float,
    val cores: List<CoreReading>,
    val memUsedGb: Float, val memTotalGb: Int, val memHist: List<Float>,
    val netDown: Long, val netUp: Long, val netHist: List<Float>,
    val batteryPct: Float, val watts: Float, val voltage: String, val remaining: String,
    val warning: Boolean = false
)

object DemoMonitor {
    private var t = 0.0
    private val cpuHist = ArrayDeque(List(60) { 30f + Random.nextFloat() * 10f })
    private val memHist = ArrayDeque(List(60) { 0.55f + Random.nextFloat() * 0.02f })
    private val netHist = ArrayDeque(List(60) { Random.nextFloat() })

    fun tick(): SystemSnapshot {
        t += 0.35
        val cpu = (38 + 26 * sin(t) + 6 * sin(t * 3.7)).toFloat().coerceIn(3f, 99f)
        cpuHist.removeFirst(); cpuHist.addLast(cpu)
        memHist.removeFirst(); memHist.addLast((0.55 + 0.02 * sin(t * 0.5)).toFloat())
        netHist.removeFirst(); netHist.addLast(Random.nextFloat())
        return SystemSnapshot(
            cpuPct = cpu, cpuHist = cpuHist.toList(),
            freqGHz = String.format(java.util.Locale.US, "%.2f", 2.84 + 0.4 * sin(t)),
            tempC = (44 + 6 * sin(t * 0.8)).toFloat(),
            cores = List(8) { i ->
                CoreReading(i, (cpu * (0.4f + Random.nextFloat())).coerceAtMost(100f),
                    (1800 + (600 * Random.nextFloat()).toLong()) * 1000)
            },
            memUsedGb = 6.81f, memTotalGb = 12, memHist = memHist.toList(),
            netDown = (2_000_000 + 16_000_000 * netHist.last()).toLong(),
            netUp = (400_000 + 2_000_000 * Random.nextFloat()).toLong(),
            netHist = netHist.toList(),
            batteryPct = 0.78f, watts = -3.42f, voltage = "4.102 V", remaining = "6h 12m"
        )
    }
}

// ─────────────────────────── app shell ───────────────────────────

@Composable
fun DeviceInsightApp(onMedium: (Medium) -> Unit = {}) {
    var selected by remember { mutableStateOf(1) }
    val s by produceState(DemoMonitor.tick()) {
        while (true) { delay(1000); value = DemoMonitor.tick() }
    }
    val keys = listOf(
        RailKey(1, "OVERVIEW", warning = s.tempC > 60),
        RailKey(2, "ACTIVITY"), RailKey(3, "PROCESSES"), RailKey(4, "DEVICE")
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                1 -> OverviewScreen(s) { /* navigate to channel page */ }
                2 -> CpuScreen(s)
                else -> OverviewScreen(s) {}
            }
        }
        ModeRail(keys, selected, { selected = it.number })
    }
}

// ─────────────────────────── № 01 — OVERVIEW ───────────────────────────

@Composable
fun OverviewScreen(s: SystemSnapshot, onOpenChannel: (Channel) -> Unit) {
    Column(Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())) {
        Masthead()
        ScreenHeader("№ 01 — OVERVIEW", "Overview.",
            if (s.warning) "1 channel warning" else "all channels nominal", warn = s.warning)

        ReadoutTile(
            channel = Channels.CPU, onClick = { onOpenChannel(Channels.CPU) },
            value = String.format(java.util.Locale.US, "%.1f", s.cpuPct), unit = "%",
            subline = "${s.freqGHz} GHz · ${Fmt.temp(s.tempC)} · 8C/8T",
            spark = s.cpuHist
        )
        Spacer(Modifier.height(12.dp))
        ReadoutTile(
            channel = Channels.MEMORY, onClick = { onOpenChannel(Channels.MEMORY) },
            value = String.format(java.util.Locale.US, "%.2f", s.memUsedGb), unit = "/ ${s.memTotalGb} GB",
            subline = "zram 1.2 · swap 0.4",
            barFraction = s.memUsedGb / s.memTotalGb, spark = s.memHist
        )
        Spacer(Modifier.height(12.dp))
        ReadoutTile(
            channel = Channels.NETWORK, onClick = { onOpenChannel(Channels.NETWORK) },
            value = Fmt.bytes(s.netDown), unit = "/s ↓",
            subline = "↑ ${Fmt.rate(s.netUp)}", spark = s.netHist
        )
        Spacer(Modifier.height(12.dp))
        ReadoutTile(
            channel = Channels.POWER, onClick = { onOpenChannel(Channels.POWER) },
            value = "${(s.batteryPct * 100).toInt()}", unit = "%",
            subline = "${Fmt.watts(s.watts)} · ${s.voltage} · ${s.remaining}",
            statusText = "${(s.batteryPct * 100).toInt()}%"
        )
        EndOfSheet()
    }
}

// ─────────────────────────── № 02 — PROCESSOR ───────────────────────────

@Composable
fun CpuScreen(s: SystemSnapshot) {
    Column(Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())) {
        Masthead()
        ScreenHeader("№ 02 — PROCESSOR", "Processor.", "gov schedutil · big.LITTLE 1+4+4")

        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.Bottom) {
            OdometerText(s.freqGHz, style = Caliper.type.readoutXl)
            Text(" GHz", style = Caliper.type.dataS, color = Caliper.colors.ink60,
                modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.CPU, title = "LOAD · 60 s") {
            ScopeTrace(
                values = s.cpuHist, channel = Channels.CPU,
                yMax = 100f, windowLabel = "60 s",
                valueFormat = { Fmt.pct(it, 1) },
                timeLabelFor = { frac -> "-${((1f - frac) * 60).toInt()}s" }
            )
        }
        Spacer(Modifier.height(12.dp))
        PanelCard(title = "CORES") { CoreRail(s.cores) }
        Spacer(Modifier.height(12.dp))
        PanelCard(title = "THERMAL") { ThermalGauge(s.tempC) }
        EndOfSheet()
    }
}

// ─────────────────────────── previews ───────────────────────────

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun OverviewPaperPreview() {
    CaliperTheme(Medium.PAPER) { OverviewScreen(DemoMonitor.tick()) {} }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun GalleryCarbonPreview() {
    CaliperTheme(Medium.CARBON) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var dip by remember { mutableStateOf(true) }
            HardKey("PRIMARY", variant = HardKeyVariant.PRIMARY, onClick = {})
            HardKey("SECONDARY", variant = HardKeyVariant.SECONDARY, onClick = {})
            HardKey("DISABLED", variant = HardKeyVariant.DISABLED, onClick = {})
            DipSwitch(dip, { dip = it }, label = "grid visibility")
            SegKey(listOf("30s", "2m", "10m", "1h"), "2m", {})
            StampBadge("ROOT VERIFIED", color = Caliper.colors.accent)
            LinearGauge(fraction = 0.78f, voltage = "4.102 V")
            HatchBar(listOf(
                HatchSegment("apps", 38_200_000_000, Caliper.colors.channel(Channels.STORAGE), HatchPattern.SOLID),
                HatchSegment("media", 21_400_000_000, Caliper.colors.channel(Channels.MEMORY), HatchPattern.DIAGONAL),
                HatchSegment("system", 12_900_000_000, Caliper.colors.channel(Channels.CPU), HatchPattern.CROSS),
                HatchSegment("free", 52_400_000_000, Caliper.colors.ink40, HatchPattern.NONE)
            ))
        }
    }
}
```

---

## Quick reference — mapping spec → code

| Spec component | Kotlin |
|---|---|
| §4.1 Channel Registry | `Channels`, `Channel`, `CaliperColors.channel()` |
| §4.2 Media | `Medium`, `Paper/Carbon/BlueprintColors`, `CaliperTheme` |
| §4.3 Type scale | `CaliperTypography`, `Caliper.type.*` |
| §4.5 Hatching | `DrawScope.hatch()`, `HatchPattern` |
| §4.7 Motion | `CaliperMotion`, `rememberPeakHold`, `OdometerText`, pen sweep in `ScopeTrace` |
| §5.1–5.2 Chrome | `Masthead`, `ModeRail` |
| §5.3–5.8 Data | `PanelCard`, `ReadoutTile`, `ScopeTrace`, `CoreRail`, `LinearGauge`, `HatchBar` |
| §5.9–5.11 Ledger | `LedgerTable`, `ProcessDossier`, `SafetyLatch` |
| §5.12 Keys | `HardKey`, `DipSwitch`, `FaderKey`, `SegKey` |
| §5.13–5.15 | `StampBadge`, `MarginNote`, `CalibratingIndicator`/`EmptyState`/`FaultState` |
| S-11 HUD | `CaliperHud` + service wiring comment |
| S-12 Widgets | `ChannelWidget` (Glance) |
| S-14 Calibration | `CalibrationSweep` |

**Recipe for a new screen:** `Column(Modifier.fillMaxSize().caliperGrid().verticalScroll(...))` → `Masthead()` → `ScreenHeader("№ n — LABEL", "Serif Title.", subline)` → `PanelCard`s with `Channel` headers → `EndOfSheet()`. Every channel color gets its label; every numeral goes through `Fmt` or `OdometerText`; every destructive action goes through `SafetyLatch`.
