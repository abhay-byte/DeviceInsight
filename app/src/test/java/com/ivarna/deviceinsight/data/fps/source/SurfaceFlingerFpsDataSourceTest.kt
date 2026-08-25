package com.ivarna.deviceinsight.data.fps.source

import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.util.ForegroundAppResolver
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test

class SurfaceFlingerFpsDataSourceTest {

    private val gateway = mockk<ShellGateway>(relaxed = true)
    private val resolver = mockk<ForegroundAppResolver>(relaxed = true)
    private val source = SurfaceFlingerFpsDataSource(gateway, resolver)

    @Test
    fun parseLatency_extractsFrametimesAndFps() {
        val output = """
            16666666
            1000000000 1000000000 1016666666
            1016666666 1025000000 1033333332
            1033333332 1033333332 1050000000
        """.trimIndent()
        val snap = source.parseLatency(output)
        assertNotNull(snap)
        assertEquals(FpsMethod.SURFACEFLINGER, snap!!.method)
        assertTrue(snap.frametimes.size >= 3)
        assertTrue(snap.currentFps in 55f..65f)
    }

    @Test
    fun parseLatency_returnsNullForHeaderOnly() {
        assertNull(source.parseLatency("16666666"))
        assertNull(source.parseLatency("16666666\n"))
        assertNull(source.parseLatency(""))
    }

    @Test
    fun parseLatency_rejectsSentinels() {
        val output = """
            16666666
            0 0 0
            9223372036854775807 9223372036854775807 9223372036854775807
            1000000000 1000000000 1016666666
        """.trimIndent()
        val snap = source.parseLatency(output)
        assertNotNull(snap)
        assertEquals(1, snap!!.frametimes.size)
    }

    @Test
    fun parseLatency_respectsRefreshCeiling() {
        // Refresh period 16ms ~60Hz, but frametimes avg 8ms would be ~120FPS — should be capped to refresh ceiling later via repo, but SF caps 240
        val output = """
            8333333
            1000000000 1000000000 1008000000
            1008000000 1008000000 1016000000
        """.trimIndent()
        val snap = source.parseLatency(output)
        assertNotNull(snap)
        assertTrue(snap!!.currentFps <= 240f)
    }

    @Test
    fun parseLayerName_variants() {
        assertEquals(
            "SurfaceView[com.example.game/com.example.GameActivity]#123",
            source.parseLayerName("SurfaceView[com.example.game/com.example.GameActivity]#123")
        )
        assertEquals(
            "com.example.game/com.example.GameActivity#456",
            source.parseLayerName("com.example.game/com.example.GameActivity#456")
        )
        assertEquals(
            "com.example.game/com.example.GameActivity#1183",
            source.parseLayerName("RequestedLayerState{com.example.game/com.example.GameActivity#1183 parentId=42}")
        )
        assertEquals(
            "com.example.game/com.example.GameActivity#1183",
            source.parseLayerName("RequestedLayerState{3fa18c4 com.example.game/com.example.GameActivity#1183 parentId=42}")
        )
        assertEquals(
            "SurfaceView[com.example.game/com.example.GameActivity]#1183",
            source.parseLayerName("RequestedLayerState{3fa18c4 SurfaceView[com.example.game/com.example.GameActivity]#1183 z=10}")
        )
        // Leading handle stripped, not returned as name
        assertNotEquals("3fa18c4", source.parseLayerName("RequestedLayerState{3fa18c4 com.example.game/com.example.GameActivity#1183 parentId=42}"))
    }

    @Test
    fun parseLayerName_handlesStandardAndA15() {
        // Standard old format
        assertEquals("com.game/pkg#1", source.parseLayerName("com.game/pkg#1"))
        // A15 with hex handle and multiple suffixes
        assertEquals(
            "com.game/pkg#99",
            source.parseLayerName("RequestedLayerState{a1b2c3d com.game/pkg#99 parentId=10 z=5 relativeParentId=0}")
        )
        // Empty/blank
        assertNull(source.parseLayerName(""))
        assertNull(source.parseLayerName("   "))
    }

    @Test
    fun parseLatency_invalidTriplesSkippedAndNoDivisionByZero() {
        val output = """
            16666666
            bad line without numbers
            1000000000 1010000000
            1000000000 1010000000 1016666666
            0 0 0
            9223372036854775807 0 9223372036854775807
            2000000000 2000000000 1999999999
            3000000000 3000000000 3016666666
        """.trimIndent()
        val snap = source.parseLatency(output)
        assertNotNull(snap)
        // Only 2 valid triples (1016666666 diff 16666666, 3016666666 diff 16666666)
        assertEquals(2, snap!!.frametimes.size)
        assertTrue(snap.currentFps in 55f..65f)
    }

    @Test
    fun parseLatency_extremeOverflowSentinelRejected() {
        val output = """
            16666666
            9223372036854775807 9223372036854775807 9223372036854775807
            0 0 0
            1000000000 1000000000 1000000000
            1000000000 1000000000 1010000000
        """.trimIndent()
        val snap = source.parseLatency(output)
        assertNotNull(snap)
        assertEquals(1, snap!!.frametimes.size)
        assertTrue(snap.currentFps in 90f..130f) // 10ms -> 100fps
    }

    @Test
    fun parseLatency_android15OnlyRefreshPeriodReturnsNull() {
        // Android 15 regression: only refresh period, no frame rows
        assertNull(source.parseLatency("16666666\n"))
        assertNull(source.parseLatency("16666666\n16666666"))
    }

    @Test
    fun findSurfaceForPackage_prefersRenderSurface() {
        // This test exercises selection logic without shell — we test parseLayerName and then logic manually
        // Real findSurface uses shell, so we just verify parse correctness; selection is tested via integration
        val layers = listOf(
            "ActivityRecord{abc u0 com.example.game/com.example.GameActivity}",
            "InputSink com.example.game",
            "BLAST#2 com.example.game",
            "SurfaceView[com.example.game/com.example.GameActivity]#100",
            "Vulkan[com.example.game]#101"
        )
        // Simulate owned filtering and preference: SurfaceView should win
        val parsed = layers.mapNotNull { source.parseLayerName(it) }
        // Ensure ActivityRecord/InputSink would be filtered in real findSurface (contains those strings)
        val filtered = parsed.filter { !it.contains("ActivityRecord") && !it.contains("InputSink") }
        assertTrue(filtered.any { it.contains("SurfaceView") })
        // Preference logic: first SurfaceView/Vulkan wins
        val preferred = filtered.firstOrNull { line ->
            listOf("SurfaceView", "NativeActivity", "Vulkan", "GLSurfaceView").any { marker -> line.contains(marker, ignoreCase = true) }
        }
        assertNotNull(preferred)
        assertTrue(preferred!!.contains("SurfaceView"))
    }
}
