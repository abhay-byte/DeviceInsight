package com.ivarna.deviceinsight.ui.caliper.hud

import org.junit.Assert.assertEquals
import org.junit.Test

class HudThemeSourceTest {
    @Test
    fun everyHudMediumHasOnePaletteAndMetricSource() {
        HudMedium.entries.forEach { medium ->
            val palette = HudPalettes.of(medium)
            val caliper = medium.caliperColors()
            assertEquals(caliper.ink, palette.ink)
            assertEquals(caliper.ink60, palette.ink60)
            assertEquals(caliper.ink40, palette.ink40)
            assertEquals(caliper.hairline, palette.hairline)
            assertEquals(caliper.accent, palette.accent)
            assertEquals(caliper.fault, palette.fault)
        }
        assertEquals(156, HudScales.of(HudScale.S).widthDp)
        assertEquals(252, HudScales.of(HudScale.M).widthDp)
        assertEquals(340, HudScales.of(HudScale.L).widthDp)
    }
}
