package com.ivarna.deviceinsight.domain.model

data class CpuDataPoint(
    val xValue: Long,
    val timestamp: Long,
    val utilization: Float
)
data class MemoryDataPoint(
    val xValue: Long,
    val timestamp: Long,
    val utilization: Float
)

data class PowerDataPoint(
    val xValue: Long,
    val timestamp: Long,
    val powerWatts: Float
)

data class FpsDataPoint(
    val xValue: Long,
    val timestamp: Long,
    val fps: Int
)

data class CpuCoreDataPoint(
    val xValue: Long,
    val timestamp: Long,
    val frequencyMHz: Float
)
