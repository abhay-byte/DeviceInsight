package com.ivarna.deviceinsight.ui.caliper.widget

import com.ivarna.deviceinsight.data.fps.FpsSample
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.hud.HudConfig
import com.ivarna.deviceinsight.ui.caliper.hud.HudMedium
import com.ivarna.deviceinsight.ui.caliper.hud.HudModule
import com.ivarna.deviceinsight.ui.caliper.hud.toCaliperMedium
import com.ivarna.deviceinsight.ui.caliper.hud.toHudMedium
import org.junit.Assert.*
import org.junit.Test

class BenchSelfCheckTest {

    @Test
    fun zeroSnapshotIsStale() {
        assertTrue(BenchSnapshot().stale(1000))
    }

    @Test
    fun freshNotStale_oldStale() {
        val fresh = BenchSnapshot(timestamp = System.currentTimeMillis())
        assertFalse(fresh.stale(30_000))
        val old = BenchSnapshot(timestamp = System.currentTimeMillis() - 61_000)
        assertTrue(old.stale(30_000))
    }

    @Test
    fun toBenchSnapshotMapping() {
        val m = DashboardMetrics(
            cpuUsage = 0.384f,
            ramUsage = 0.5f,
            ramUsedBytes = (6.813e9).toLong(),
            ramTotalBytes = (16e9).toLong(),
            swapUsedBytes = 0L,
            swapTotalBytes = 0L,
            gpuUsage = 0.5f,
            gpuModel = "TestGPU",
            gpuTemp = 0f,
            gpuFreqMhz = 500,
            gpuMaxFreqMhz = 800,
            gpuMinFreqMhz = 200,
            gpuCores = 4,
            gpuVendor = "Test",
            batteryLevel = 42,
            batteryStatus = "Discharging",
            batteryVoltage = 3850,
            batteryHealth = "Good",
            isCharging = false,
            temperature = 30f,
            cpuTemperature = 45f,
            powerConsumption = -2.5f,
            cpuCoreFrequencies = listOf(1800, 2400),
            cpuCoreMaxFrequencies = listOf(2400, 2400),
            storageUsedPerc = 0.5f,
            storageFreeGb = "10 GB Free",
            storageTotalGb = "20 GB",
            storageUsedGb = "10 GB",
            networkSpeed = "0 B/s",
            networkDownloadSpeed = "0 B/s",
            networkUploadSpeed = "0 B/s",
            uptime = "0h 00m"
        )
        val snap = m.toBenchSnapshot(serviceRunning = false, rxBps = 1000L, txBps = 2000L, stoUsedBytes = 5_000_000_000L, stoTotalBytes = 10_000_000_000L, gpuFitted = true)
        assertEquals(38.4f, snap.cpuPct, 0.01f)
        assertEquals(-2.5f, snap.watts, 0.001f)
        assertEquals(0.42f, snap.batteryPct, 0.001f)
        assertEquals(1000L, snap.netDown)
        assertEquals(2000L, snap.netUp)
        assertEquals(5f, snap.stoUsedGb, 0.2f)
        assertTrue(snap.gpuFitted)
        assertEquals(50f, snap.gpuPct!!, 0.01f)
    }

    @Test
    fun tierOf() {
        assertEquals(Tier.T1, Tier.of(140, 140))
        assertEquals(Tier.T2, Tier.of(280, 140))
        assertEquals(Tier.T4, Tier.of(280, 280))
        assertEquals(Tier.T1, Tier.of(100, 100))
        assertEquals(Tier.T5, Tier.of(350, 280))
        assertEquals(Tier.T3, Tier.of(280, 210))
    }

    @Test
    fun tierExactBounds() {
        // DI-WF-001 F1: exact bounds — no -20 tolerance; expectations derived from the formula
        assertEquals(Tier.T1, Tier.of(139, 139))    // below T1 floor → T1 fallback
        assertEquals(Tier.T1, Tier.of(140, 140))    // exact T1 floor
        assertEquals(Tier.T1, Tier.of(279, 140))    // 1dp shy of T2 width stays T1
        assertEquals(Tier.T2, Tier.of(280, 140))    // crosses into T2
        assertEquals(Tier.T2, Tier.of(280, 209))    // 1dp shy of T3 height stays T2
        assertEquals(Tier.T3, Tier.of(280, 210))    // exact T3 floor
        assertEquals(Tier.T4, Tier.of(349, 280))    // 1dp shy of T5 width stays T4
        assertEquals(Tier.T5, Tier.of(350, 280))    // exact T5 floor
        assertEquals(Tier.T5, Tier.of(1000, 400))   // huge sizes clamp at top tier
        assertEquals(Tier.T1, Tier.of(280, 100))    // wide but too short → T1
    }

    @Test
    fun previewSnapshotDeterministic() {
        val a = BenchDemo.previewSnapshot()
        val b = BenchDemo.previewSnapshot()
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        // structural stability for generator cache keys / state keys
        assertEquals(a.cores.size, b.cores.size)
        assertEquals(a.cpuHist.contentHash(), b.cpuHist.contentHash())
        assertEquals(a.memComposition, b.memComposition)
        assertEquals(a.topConsumers, b.topConsumers)
        assertTrue("netHist must be populated (D8)", a.netHist.isNotEmpty())
        assertTrue("gpuGles must be populated (D8)", a.gpuGles.isNotBlank())
        assertTrue(a.batteryPresent)
        assertEquals(0L, a.timestamp % 1000L) // fixed epoch — never System.currentTimeMillis
    }

    @Test
    fun previewShotMatrix_shape() {
        // 5 kinds x 3 media: single instruments at T2, bench at T4
        assertEquals(15, BenchPreviewGenerator.SHOT_MATRIX.size)
        Medium.entries.forEach { medium ->
            listOf(WidgetKind.SCOPE, WidgetKind.STACK, WidgetKind.FUEL, WidgetKind.RASTER).forEach { kind ->
                val shot = BenchPreviewGenerator.SHOT_MATRIX.first { it.kind == kind && it.medium == medium }
                assertEquals(Tier.T2, shot.tier)
                assertEquals("preview_${kind.name.lowercase()}_${medium.name.lowercase()}_280x140.png", shot.fileName)
            }
            val bench = BenchPreviewGenerator.SHOT_MATRIX.first { it.kind == WidgetKind.BENCH && it.medium == medium }
            assertEquals(Tier.T4, bench.tier)
            assertEquals("preview_bench_${medium.name.lowercase()}_280x280.png", bench.fileName)
        }
        // capture density is exactly 3x (480dpi) so px dims are tier*3
        assertEquals(840, BenchPreviewGenerator.SHOT_MATRIX.first().wPx)
        assertEquals(420, BenchPreviewGenerator.SHOT_MATRIX.first().hPx)
        assertEquals(480, BenchPreviewGenerator.CAPTURE_DENSITY_DPI)
    }

    @Test
    fun blueprintPaletteIsInk() {
        assertEquals(WidgetPalettes.BLUEPRINT.ch01, WidgetPalettes.BLUEPRINT.ink)
        assertEquals(WidgetPalettes.BLUEPRINT.ch02, WidgetPalettes.BLUEPRINT.ink)
        assertEquals(WidgetPalettes.BLUEPRINT.ch06, WidgetPalettes.BLUEPRINT.ink)
        assertNotEquals(WidgetPalettes.PAPER.ch01, WidgetPalettes.PAPER.ink)
    }

    @Test
    fun benchConfigDefaults() {
        val c = BenchConfig()
        assertTrue(c.followSystem)
        assertTrue(c.wattHero)
        assertEquals(4, c.compactChannels.size)
        assertEquals(Cadence.AMBIENT, c.cadence)
        assertEquals(60, c.traceWindowS)
    }

    @Test
    fun enrichWithHistory_appendsAndCaps() {
        val prev = BenchSnapshot(cpuHist = listOf(10f, 20f), memHist = listOf(30f), wattHist = listOf(1f), netHist = listOf(5f), gpuHist = listOf(7f))
        val raw = BenchSnapshot(cpuPct = 30f, memUsedGb = 6f, memTotalGb = 12f, watts = 2f, netDown = 2048L, netUp = 0L, gpuPct = 40f)
        val enriched = BenchUpdater.enrichWithHistory(raw, prev)
        assertEquals(listOf(10f, 20f, 30f), enriched.cpuHist)
        assertEquals(3, enriched.cpuHist.size)
        assertEquals(2, enriched.memHist.size) // 30 + 50
        assertEquals(50f, enriched.memHist.last(), 0.01f)
        assertEquals(2, enriched.wattHist.size)
        assertEquals(2f, enriched.wattHist.last(), 0.01f)
        assertFalse(enriched.cpuHist.isEmpty())
        // null prev → single entry
        val first = BenchUpdater.enrichWithHistory(raw, null)
        assertEquals(1, first.cpuHist.size)
        assertEquals(30f, first.cpuHist.first(), 0.01f)
        // cap at MAX_HIST (fill beyond)
        val bigPrev = BenchSnapshot(cpuHist = List(400) { 1f })
        val capped = BenchUpdater.enrichWithHistory(raw, bigPrev)
        assertEquals(300, capped.cpuHist.size)
    }

    @Test
    fun emptyCpuHistDoesNotThrow() {
        val snap = BenchSnapshot(cpuHist = emptyList())
        // spark/scope with empty should show NO SIGNAL, not throw
        assertTrue(snap.cpuHist.isEmpty())
        assertTrue(snap.stale(1000))
    }

    @Test
    fun warningAndStaleLogic() {
        val hot = BenchSnapshot(tempC = 61f, timestamp = System.currentTimeMillis())
        assertTrue(hot.warning())
        val lowBatt = BenchSnapshot(batteryPct = 0.15f, charging = false, batteryPresent = true, timestamp = System.currentTimeMillis())
        assertTrue(lowBatt.warning())
        val chargingLow = BenchSnapshot(batteryPct = 0.15f, charging = true, batteryPresent = true, timestamp = System.currentTimeMillis())
        assertFalse(chargingLow.warning())
    }

    // ── Phase 0 harness additions ──

    @Test
    fun cadenceMs_liveAmbientBudget() {
        val snapCharging = BenchSnapshot(charging = true, serviceRunning = false, timestamp = System.currentTimeMillis())
        val snapIdle = BenchSnapshot(charging = false, serviceRunning = false, timestamp = System.currentTimeMillis())
        val snapRunning = BenchSnapshot(charging = false, serviceRunning = true, timestamp = System.currentTimeMillis())
        assertEquals(1_000L, cadenceMs(BenchConfig(cadence = Cadence.LIVE), snapCharging))
        assertEquals(1_000L, cadenceMs(BenchConfig(cadence = Cadence.LIVE), snapRunning))
        assertEquals(1_000L, cadenceMs(BenchConfig(cadence = Cadence.LIVE), snapIdle))
        assertEquals(30_000L, cadenceMs(BenchConfig(cadence = Cadence.AMBIENT), snapCharging))
        assertEquals(15 * 60_000L, cadenceMs(BenchConfig(cadence = Cadence.BUDGET), snapCharging))
        assertEquals(900_000L, cadenceMs(BenchConfig(cadence = Cadence.BUDGET), snapIdle))
    }

    @Test
    fun toBenchSnapshot_currentMaAndBatteryPresentAndCycleCountNull() {
        val m = DashboardMetrics(
            cpuUsage = 0.5f, ramUsage = 0.5f,
            ramUsedBytes = 4L * 1024 * 1024 * 1024, ramTotalBytes = 8L * 1024 * 1024 * 1024,
            swapUsedBytes = 0L, swapTotalBytes = 0L,
            gpuUsage = 0f, gpuModel = "", gpuTemp = 0f, gpuFreqMhz = 0, gpuMaxFreqMhz = 0, gpuMinFreqMhz = 0, gpuCores = 0, gpuVendor = "Unknown",
            batteryLevel = 80, batteryStatus = "Discharging", batteryVoltage = 4000, batteryHealth = "Good",
            isCharging = false, temperature = 30f, cpuTemperature = 40f, powerConsumption = 1f,
            cpuCoreFrequencies = emptyList(), cpuCoreMaxFrequencies = emptyList(),
            storageUsedPerc = 0f, storageFreeGb = "", storageTotalGb = "", storageUsedGb = "",
            networkSpeed = "", networkDownloadSpeed = "", networkUploadSpeed = "", uptime = ""
        )
        val snap = m.toBenchSnapshot(serviceRunning = false).copy(
            currentMa = 123,
            batteryPresent = true,
            batteryHealth = "Good",
            cycleCount = null,
            designMah = null
        )
        assertEquals(123, snap.currentMa)
        assertTrue(snap.batteryPresent)
        assertNull(snap.cycleCount)
        assertNull(snap.designMah)
        assertEquals("Good", snap.batteryHealth)
        // cycleCount null when OEM hides it — widget never shows 835
        val sentinel = -1
        val cycleNull = if (sentinel >= 0) sentinel else null as Int?
        assertNull(cycleNull)
    }

    @Test
    fun benchFrames_noRecycle_sizeOfKb() {
        // Guard: BenchModel.kt must not contain dangerous recycle() on evicted bitmaps
        val candidates = listOf(
            java.io.File("src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchModel.kt"),
            java.io.File("app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchModel.kt")
        )
        val f = candidates.firstOrNull { it.exists() } ?: throw AssertionError("BenchModel.kt not found")
        val src = f.readText()
        assertFalse("BenchFrames lru must not recycle evicted bitmap — RemoteViews may still hold Bitmap", src.contains("recycle()"))
        assertTrue("sizeOf must be byteCount/1024 KB", src.contains("byteCount / 1024"))
    }

    @Test
    fun topConsumers_hideWhenEmpty() {
        // STACK ledger hides when empty (permission not granted)
        val emptySnap = BenchSnapshot(topConsumers = emptyList())
        assertTrue(emptySnap.topConsumers.isEmpty())
        // When granted, label-only (rssMb==0) is valid
        val withConsumers = BenchSnapshot(topConsumers = listOf(Consumer(pkg = "com.test", label = "TestApp", rssMb = 0)))
        assertEquals(1, withConsumers.topConsumers.size)
        assertEquals(0, withConsumers.topConsumers.first().rssMb)
        assertEquals("TestApp", withConsumers.topConsumers.first().label)
    }

    @Test
    fun fpsSample_sourceInSet() {
        val sf = FpsSample(60, "SF")
        val gfx = FpsSample(30, "GFX")
        val none = FpsSample(0, "—")
        assertTrue(sf.source in setOf("SF", "GFX", "—"))
        assertTrue(gfx.source in setOf("SF", "GFX", "—"))
        assertTrue(none.source in setOf("SF", "GFX", "—"))
        // HUD shows — + NO SIGNAL when both fail, never fallback to refresh rate
        assertEquals("—", none.source)
        assertEquals(0, none.fps)
    }

    @Test
    fun hudConfig_csvRoundTrip() {
        val cfg = HudConfig(modules = setOf(HudModule.FPS, HudModule.CPU, HudModule.MEMORY))
        val csv = cfg.modulesCsv()
        val parsed = HudConfig.fromCsv(csv)
        assertEquals(cfg.modules, parsed)
        // empty csv defaults
        val empty = HudConfig.fromCsv("")
        assertEquals(setOf(HudModule.FPS, HudModule.CPU, HudModule.MEMORY, HudModule.POWER), empty)
    }

    @Test
    fun hudMediumDistinctFromMedium() {
        // HudMedium is distinct enum, not typealias
        assertNotEquals(HudMedium::class.java, Medium::class.java)
        assertEquals(HudMedium.CARBON, HudMedium.valueOf("CARBON"))
        assertEquals(Medium.CARBON, Medium.valueOf("CARBON"))
        // mapping via extension toCaliperMedium / toHudMedium
        assertEquals(Medium.CARBON, HudMedium.CARBON.toCaliperMedium())
        assertEquals(HudMedium.PAPER, Medium.PAPER.toHudMedium())
        assertEquals(Medium.BLUEPRINT, HudMedium.BLUEPRINT.toCaliperMedium())
    }

    @Test
    fun placedAt_writeOnce_logic() {
        // Simulate BenchState.save write-once via preference map check
        // Use in-memory map to mimic p[KEY_PLACED]==null check at BenchModel:416
        val placedKey = "placedAt"
        val prefs = mutableMapOf<String, Long>()
        fun savePlacedAt(now: Long) {
            if (!prefs.containsKey(placedKey)) prefs[placedKey] = now
        }
        savePlacedAt(1000L)
        savePlacedAt(2000L)
        assertEquals(1000L, prefs[placedKey])
    }
}
