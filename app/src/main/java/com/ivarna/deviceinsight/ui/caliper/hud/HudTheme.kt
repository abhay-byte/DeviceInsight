package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.ivarna.deviceinsight.ui.caliper.BlueprintColors
import com.ivarna.deviceinsight.ui.caliper.CaliperColors
import com.ivarna.deviceinsight.ui.caliper.CarbonColors
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.PaperColors
import com.ivarna.deviceinsight.ui.caliper.PlexMonoFamily

/**
 * Distinct HUD medium — never a typealias over Medium.
 * HUD never follows system; widget Medium followSystem is separate.
 */
enum class HudMedium { PAPER, CARBON, BLUEPRINT }

fun HudMedium.toCaliperMedium(): Medium = when (this) {
    HudMedium.PAPER -> Medium.PAPER
    HudMedium.CARBON -> Medium.CARBON
    HudMedium.BLUEPRINT -> Medium.BLUEPRINT
}

fun Medium.toHudMedium(): HudMedium = when (this) {
    Medium.PAPER -> HudMedium.PAPER
    Medium.CARBON -> HudMedium.CARBON
    Medium.BLUEPRINT -> HudMedium.BLUEPRINT
}

fun HudMedium.caliperColors(): CaliperColors = when (this) {
    HudMedium.PAPER -> PaperColors
    HudMedium.CARBON -> CarbonColors
    HudMedium.BLUEPRINT -> BlueprintColors
}

fun hudMediumFromString(s: String?): HudMedium = try {
    s?.let { HudMedium.valueOf(it) } ?: HudDefaults.medium
} catch (_: Exception) { HudDefaults.medium }

// ─────────────── DI-HD-001 §6 — palettes (hex copied from CaliperColors, no fork) ───────────────

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
        // Blueprint: all channel traces render ink — identity by tick + label + hatch only
        ch01 = Color(0xFFEAF2FF), ch02 = Color(0xFFEAF2FF), ch03 = Color(0xFFEAF2FF),
        ch04 = Color(0xFFEAF2FF), ch05 = Color(0xFFEAF2FF), ch06 = Color(0xFFEAF2FF)
    )

    fun of(m: HudMedium): HudColors = when (m) {
        HudMedium.CARBON -> CARBON
        HudMedium.PAPER -> PAPER
        HudMedium.BLUEPRINT -> BLUEPRINT
    }
}

// ─────────────── metrics per scale (DI-HD-001 §3 — growth, never stretch) ───────────────

data class HudMetrics(
    val widthDp: Int, val padDp: Int,
    val heroSp: Int, val valueSp: Int, val metaSp: Int, val microSp: Int,
    val barHDp: Int,
    val coreBankCellsPerRow: Int,
    val coreBankShowFreq: Boolean,
    val showGovLine: Boolean,
    val showTraceBand: Boolean
)

object HudScales {
    fun of(s: HudScale): HudMetrics = when (s) {
        HudScale.S -> HudMetrics(156, 8, 20, 11, 8, 7, 3, 8, false, false, false)
        HudScale.M -> HudMetrics(252, 12, 26, 12, 9, 8, 5, 4, true, false, true)
        HudScale.L -> HudMetrics(340, 16, 34, 15, 10, 9, 7, 4, true, true, true)
    }
}

// ─────────────── type — mono only, tabular numerals ───────────────

private const val TNUM = "tnum"

fun hudStyle(sizeSp: Int, weight: FontWeight = FontWeight.Normal, trackingEm: Float = 0.06f): TextStyle =
    TextStyle(
        fontFamily = PlexMonoFamily,
        fontSize = sizeSp.sp,
        fontWeight = weight,
        letterSpacing = trackingEm.em,
        fontFeatureSettings = TNUM
    )

// ─────────────── locals + theme ───────────────

val LocalHudColors = staticCompositionLocalOf { HudPalettes.CARBON }
val LocalHudMetrics = staticCompositionLocalOf { HudScales.of(HudScale.M) }

@Composable
fun HudTheme(medium: HudMedium, scale: HudScale, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalHudColors provides HudPalettes.of(medium),
        LocalHudMetrics provides HudScales.of(scale)
    ) { content() }
}
