package com.ivarna.deviceinsight.ui.caliper

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import android.provider.Settings
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
        // §4.2 Blueprint: traces = line color; channel identity is hatch + label.
        Medium.BLUEPRINT -> ink
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
    display1 = TextStyle(fontFamily = InstrumentSerifFamily, fontStyle = FontStyle.Italic, fontSize = 40.sp, lineHeight = 44.sp),
    display2 = TextStyle(fontFamily = InstrumentSerifFamily, fontStyle = FontStyle.Italic, fontSize = 28.sp, lineHeight = 32.sp),
    readoutXl = TextStyle(fontFamily = PlexMonoFamily, fontWeight = FontWeight.Light,  fontSize = 54.sp, lineHeight = 58.sp, fontFeatureSettings = TNUM),
    readoutL  = TextStyle(fontFamily = PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 34.sp, lineHeight = 38.sp, fontFeatureSettings = TNUM),
    dataM     = TextStyle(fontFamily = PlexMonoFamily, fontSize = 22.sp, lineHeight = 26.sp, fontFeatureSettings = TNUM),
    dataS     = TextStyle(fontFamily = PlexMonoFamily, fontSize = 16.sp, lineHeight = 20.sp, fontFeatureSettings = TNUM),
    body      = TextStyle(fontFamily = PlexMonoFamily, fontSize = 14.sp, lineHeight = 20.sp),
    label     = TextStyle(fontFamily = PlexMonoFamily, fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = TNUM),
    // ALL-CAPS micro labels — callers uppercase the string
    meta      = TextStyle(fontFamily = PlexMonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em, fontFeatureSettings = TNUM)
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
    // m4: M3 colors mapped onto Caliper ink-family tokens, never accent.
    // Legacy tab chrome (hardware/overlay) reads colorScheme.primary|secondary|
    // tertiary|error; accent stays interactive-only via Caliper.colors.accent.
    // This keeps the enforced 88/10/2 ratio — data and chrome render in ink.
    val m3 = if (medium == Medium.PAPER) lightColorScheme(
        background = colors.surface,
        surface = colors.panel,
        surfaceVariant = colors.panel,
        primary = colors.ink,
        onPrimary = colors.surface,
        secondary = colors.ink60,
        tertiary = colors.ink40,
        error = colors.fault,
        onBackground = colors.ink,
        onSurface = colors.ink,
        onSurfaceVariant = colors.ink60,
        outline = colors.ink40,
        outlineVariant = colors.hairline
    ) else darkColorScheme(
        background = colors.surface,
        surface = colors.panel,
        surfaceVariant = colors.panel,
        primary = colors.ink,
        onPrimary = colors.surface,
        secondary = colors.ink60,
        tertiary = colors.ink40,
        error = colors.fault,
        onBackground = colors.ink,
        onSurface = colors.ink,
        onSurfaceVariant = colors.ink60,
        outline = colors.ink40,
        outlineVariant = colors.hairline
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