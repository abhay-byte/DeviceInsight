package com.ivarna.deviceinsight.service.overlay

import org.junit.Assert.assertEquals
import org.junit.Test

class OverlayGeometryTest {
    private val frame = OverlayBounds(10, 20, 210, 220)

    @Test
    fun clampsNegativeAndOversizedCoordinates() {
        assertEquals(10, OverlayGeometry.clampPosition(-10, -20, frame, 50, 60).x)
        assertEquals(20, OverlayGeometry.clampPosition(-10, -20, frame, 50, 60).y)
        assertEquals(160, OverlayGeometry.clampPosition(999, 999, frame, 50, 60).x)
        assertEquals(160, OverlayGeometry.clampPosition(999, 999, frame, 50, 60).y)
    }

    @Test
    fun usesUsableEdgeWhenWindowIsLargerThanFrame() {
        val point = OverlayGeometry.clampPosition(100, 100, frame, 500, 500)
        assertEquals(10, point.x)
        assertEquals(20, point.y)
    }

    @Test
    fun exactBottomRightFitsMeasuredWindow() {
        val point = OverlayGeometry.clampPosition(160, 160, frame, 50, 60)
        assertEquals(160, point.x)
        assertEquals(160, point.y)
    }
}
