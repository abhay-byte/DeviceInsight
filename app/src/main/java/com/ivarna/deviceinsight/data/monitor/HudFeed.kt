package com.ivarna.deviceinsight.data.monitor

data class CoreStat(
    val id: Int,
    val loadPct: Float,
    val freqMhz: Int
)

data class HudSlow(
    val cpuPct: Float = 0f,
    val cores: List<CoreStat> = emptyList(),
    val clusterSizes: List<Int> = emptyList(),
    val governor: String? = null,
    val tempC: Float = 0f,
    val memUsedGb: Float = 0f,
    val memTotalGb: Float = 0f,
    val swapUsedGb: Float = 0f,
    val swapTotalMb: Long = 0L,
    val zramGb: Float = 0f,
    val netDown: Long = 0L,
    val netUp: Long = 0L,
    val batteryPct: Float = 0f,
    val watts: Float = 0f,
    val voltage: Float = 0f,
    val currentMa: Int = 0,
    val remainingMin: Int = 0,
    val charging: Boolean = false,
    val stoUsedGb: Float = 0f,
    val stoTotalGb: Float = 0f,
    val gpuPct: Float? = null,
    val gpuMHz: Long? = null,
    val gpuName: String = "",
    val gpuVulkan: String = "",
    val gpuGles: String = "",
    val gpuRootLocked: Boolean = false,
    val gpuFitted: Boolean = false,
    val timestamp: Long = 0L
)

data class HudFast(
    val fps: Int = 0,
    val source: String = "—"
)

fun HudFast.isNoSignal(): Boolean = source == "—"
