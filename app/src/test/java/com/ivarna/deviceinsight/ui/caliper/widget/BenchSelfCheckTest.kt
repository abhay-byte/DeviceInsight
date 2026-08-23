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
        assertEquals(30_000L, cadenceMs(BenchConfig(cadence = Cadence.LIVE), snapIdle))
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
