package com.ivarna.deviceinsight

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ivarna.deviceinsight.presentation.caliperRailOrder
import com.ivarna.deviceinsight.ui.caliper.BlueprintColors
import com.ivarna.deviceinsight.ui.caliper.CarbonColors
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.legacyThemeToMedium
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** §7 self-checks — the grammar of numbers + medium mapping. */
class CaliperSelfCheckTest {

    @Test
    fun fmtBytes() {
        assertEquals("1.50 KB", Fmt.bytes(1536))
        assertEquals("2.00 MB", Fmt.bytes(2 * 1024 * 1024))
        assertEquals("6.35 GB", Fmt.bytes(6_813_000_000))
    }

    @Test
    fun fmtTempsAndPct() {
        assertEquals("46.2°C", Fmt.temp(46.2f))
        assertEquals("38%", Fmt.pct(38f))
        assertEquals("38.4%", Fmt.pct(38.4f, 1))
    }

    @Test
    fun fmtRatesAndDuration() {
        assertEquals("17.26 MB/s", Fmt.rate(18_100_000L))
        assertEquals("6h 12m", Fmt.duration((6 * 3600 + 12 * 60) * 1000L))
        assertEquals("0142", Fmt.index(142))
    }

    @Test
    fun carbonChannelColor() {
        // CH-01 CPU on Carbon = #FF6B4A
        assertEquals(0xFF6B4A, CarbonColors.channel(Channels.CPU).toArgb() and 0xFFFFFF)
    }

    @Test
    fun legacyThemesMapToCarbon() {
        assertEquals(Medium.CARBON, legacyThemeToMedium("TechNoir"))
        assertEquals(Medium.CARBON, legacyThemeToMedium("GoldenLuxe"))
        assertEquals(Medium.PAPER, legacyThemeToMedium("NotARealTheme"))
    }

    // m2 (nav): Tasks/Application Active must be the LAST nav key.
    // Visual order and TalkBack order both derive from caliperRailOrder.
    @Test
    fun caliperNavOrderPinned() {
        assertEquals(
            "Tasks (Application Active / PROCESSES) must be position 4 of 4",
            listOf(1 to "OVERVIEW", 2 to "DEVICE", 3 to "OVERLAY", 4 to "PROCESSES"),
            caliperRailOrder,
        )
    }

    // m2 (finding): Blueprint traces render in the line color, identity via hatch+label.
    @Test
    fun blueprintChannelUsesLineColor() {
        assertEquals(0xEAF2FF, BlueprintColors.channel(Channels.CPU).toArgb() and 0xFFFFFF)
        assertEquals(BlueprintColors.ink, BlueprintColors.channel(Channels.MEMORY))
        assertEquals(0xFF6B4A, CarbonColors.channel(Channels.CPU).toArgb() and 0xFFFFFF) // unchanged
    }

    // P2-3 / m5: exactly one DataStore delegate on the "caliper" file — a second
    // delegate would throw "multiple DataStores active for same file" at runtime.
    // Source-scan guard keeps the singleton accessor architectural invariant.
    @Test
    fun dataStoreSingleAccessorAndExactKeys() {
        val candidates = listOf(
            File("src/main/java/com/ivarna/deviceinsight/ui/caliper/CaliperPrefs.kt"),
            File("app/src/main/java/com/ivarna/deviceinsight/ui/caliper/CaliperPrefs.kt")
        )
        val prefs = candidates.firstOrNull { it.exists() }
            ?: throw AssertionError("CaliperPrefs.kt not found for source-scan guard")
        val src = prefs.readText()

        val delegates = Regex("preferencesDataStore\\s*\\(name\\s*=\\s*\"([^\"]+)\"")
            .findAll(src).map { it.groupValues[1] }.toList()
        assertEquals(listOf("caliper"), delegates)

        val keys = Regex("PreferencesKey\\(\"([^\"]+)\"\\)")
            .findAll(src).map { it.groupValues[1] }.toList()
        assertEquals(listOf("medium", "showGrid", "hatchingEnabled", "caliperMigrated", "hudMedium", "hudScale", "hudOpacity", "hudBlur", "hudLocked", "hudModules", "hudShowCoreBank", "hudX", "hudY", "fpsMode", "hudMigrated"), keys)
    }
}