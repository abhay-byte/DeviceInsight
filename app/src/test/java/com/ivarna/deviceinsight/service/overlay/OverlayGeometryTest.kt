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

    @Test
    fun growingWindowNearBottomMovesUpJustEnough() {
        val usable = OverlayBounds(0, 0, 1000, 2000)

        val old = OverlayGeometry.clampPosition(100, 1700, usable, 300, 200)
        assertEquals(1700, old.y)

        val grown = OverlayGeometry.clampPosition(old.x, old.y, usable, 300, 500)
        assertEquals(1500, grown.y)
    }

    @Test
    fun shrinkingWindowDoesNotSnapToBottom() {
        val usable = OverlayBounds(0, 0, 1000, 2000)

        val position = OverlayGeometry.clampPosition(100, 1200, usable, 300, 600)
        val shrunk = OverlayGeometry.clampPosition(position.x, position.y, usable, 300, 150)

        assertEquals(1200, shrunk.y)
    }

    @Test
    fun safeFrameRespectsSystemInsetsAndEdgeMargin() {
        val safe = OverlayGeometry.safeFrame(
            display = OverlayBounds(0, 0, 1000, 2000),
            insets = OverlayInsets(left = 10, top = 30, right = 20, bottom = 120),
            edgeMarginPx = 16
        )

        assertEquals(26, safe.left)
        assertEquals(46, safe.top)
        assertEquals(964, safe.right)
        assertEquals(1864, safe.bottom)

        val point = OverlayGeometry.clampPosition(0, 1900, safe, 300, 500)
        assertEquals(26, point.x)
        assertEquals(1364, point.y)
    }

    @Test
    fun oversizedWindowPinsToSafeTopLeftWithoutInvalidRange() {
        val safe = OverlayBounds(20, 40, 100, 100)
        val point = OverlayGeometry.clampPosition(999, 999, safe, 500, 500)

        assertEquals(20, point.x)
        assertEquals(40, point.y)
    }
}
