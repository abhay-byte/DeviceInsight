package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.datastore.preferences.core.preferencesOf
import com.ivarna.deviceinsight.data.fps.model.FpsMode
import com.ivarna.deviceinsight.ui.caliper.CaliperKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class HudConfigCodecTest {
    @Test
    fun emptyPreferencesUseOneExactDefaultSet() {
        val result = HudConfigCodec.fromPreferences(preferencesOf())
        assertEquals(HudDefaults.medium, result.panel.medium)
        assertEquals(HudDefaults.scale, result.panel.scale)
        assertEquals(HudDefaults.opacity, result.panel.opacity)
        assertEquals(HudDefaults.backgroundBlurEnabled, result.panel.backgroundBlurEnabled)
        assertEquals(HudDefaults.locked, result.panel.locked)
        assertEquals(HudDefaults.modules, result.panel.modules)
        assertEquals(HudDefaults.showCoreBank, result.panel.showCoreBank)
        assertEquals(HudDefaults.x, result.x)
        assertEquals(HudDefaults.y, result.y)
        assertEquals(FpsMode.AUTO, result.fpsMode)
    }

    @Test
    fun everyMediumAndScaleRoundTrip() {
        HudMedium.entries.forEach { medium ->
            HudScale.entries.forEach { scale ->
                val result = HudConfigCodec.fromPreferences(
                    preferencesOf(CaliperKeys.hudMedium to medium.name, CaliperKeys.hudScale to scale.name)
                )
                assertEquals(medium, result.panel.medium)
                assertEquals(scale, result.panel.scale)
            }
        }
    }

    @Test
    fun malformedEnumsAndFpsUseSafeDefaults() {
        val result = HudConfigCodec.fromPreferences(
            preferencesOf(CaliperKeys.hudMedium to "NOT_A_MEDIUM", CaliperKeys.hudScale to "XL", CaliperKeys.fpsMode to "old")
        )
        assertEquals(HudDefaults.medium, result.panel.medium)
        assertEquals(HudDefaults.scale, result.panel.scale)
        assertEquals(FpsMode.AUTO, result.fpsMode)
    }

    @Test
    fun modulesOpacityPositionBlurAndLockAreParsed() {
        val result = HudConfigCodec.fromPreferences(
            preferencesOf(
                CaliperKeys.hudModules to "NETWORK, CPU, invalid",
                CaliperKeys.hudOpacity to 0.61f,
                CaliperKeys.hudBlur to false,
                CaliperKeys.hudLocked to true,
                CaliperKeys.hudShowCoreBank to false,
                CaliperKeys.hudX to -40,
                CaliperKeys.hudY to 9999,
                CaliperKeys.fpsMode to "SHIZUKU"
            )
        )
        assertEquals(setOf(HudModule.NETWORK, HudModule.CPU), result.panel.modules)
        assertEquals(0.61f, result.panel.opacity)
        assertEquals(false, result.panel.backgroundBlurEnabled)
        assertEquals(true, result.panel.locked)
        assertEquals(false, result.panel.showCoreBank)
        assertEquals(-40, result.x)
        assertEquals(9999, result.y)
        assertEquals(FpsMode.SHIZUKU, result.fpsMode)
    }

    @Test
    fun opacityIsBounded() {
        assertEquals(0.4f, HudConfigCodec.fromPreferences(preferencesOf(CaliperKeys.hudOpacity to 0.1f)).panel.opacity)
        assertEquals(0.9f, HudConfigCodec.fromPreferences(preferencesOf(CaliperKeys.hudOpacity to 1.0f)).panel.opacity)
    }
}
