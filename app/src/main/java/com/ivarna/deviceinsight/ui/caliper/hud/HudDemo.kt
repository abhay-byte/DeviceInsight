package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import com.ivarna.deviceinsight.data.monitor.CoreStat
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Config-sheet demo feed (DI-HD-001 HudDemo). Animates a plausible probe so the sheet
 * preview reads live without the overlay service running. No GlobalScope — the states
 * are remembered inside composition and stop when the sheet leaves.
 */
@Composable
fun rememberHudDemo(
    animate: Boolean = true
): Pair<State<HudSlow>, State<HudFast>> {
    val slow = remember { mutableStateOf(demoSlow()) }
    val fast = remember { mutableStateOf(HudFast(59, "SF")) }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var t = 0f
        while (true) {
            t += 0.35f
            fast.value = HudFast(fps = (57 + sin(t) * 6).toInt().coerceIn(1, 120), source = "SF")
            slow.value = demoSlow().copy(
                cpuPct = 38f + sin(t * 0.7f) * 14f,
                batteryPct = 0.72f,
                watts = -1.2f + sin(t * 0.5f) * 0.4f,
                tempC = 46f + sin(t * 0.3f) * 3f
            )
            delay(1000)   // demo ~1 Hz — the sheet must not look like the 10 Hz probe
        }
    }
    return slow to fast
}

private fun demoSlow(): HudSlow = HudSlow(
    cpuPct = 38.4f,
    cores = (0 until 8).map { i ->
        CoreStat(id = i, loadPct = 20f + i * 8f % 70f, freqMhz = 1800 + i * 137)
    },
    clusterSizes = listOf(4, 2, 2),
    governor = "waltz",
    tempC = 46f,
    memUsedGb = 6.8f,
    memTotalGb = 12f,
    swapUsedGb = 0.4f,
    swapTotalMb = 2048L,
    zramGb = 1.1f,
    netDown = 18_100_000L,
    netUp = 2_400_000L,
    batteryPct = 0.72f,
    watts = -1.2f,
    voltage = 3.87f,
    currentMa = -310,
    charging = false,
    gpuFitted = true,
    gpuPct = 41f,
    gpuMHz = 720L,
    gpuName = "adreno 740",
    timestamp = System.currentTimeMillis()
)
