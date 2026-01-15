package com.ivarna.deviceinsight.domain.model

data class DashboardMetrics(
    val cpuUsage: Float,       // 0.0 - 1.0 (0% - 100%)
    val ramUsage: Float,       // 0.0 - 1.0
    val ramUsedBytes: Long,
    val ramTotalBytes: Long,
    val swapUsedBytes: Long,
    val swapTotalBytes: Long,
    val gpuUsage: Float,       // 0.0 - 1.0
    val gpuModel: String,
    val batteryLevel: Int,     // 0 - 100
    val batteryStatus: String, // Charging, Discharging, etc.
    val temperature: Float,    // Battery temperature in Celsius
    val cpuTemperature: Float, // CPU temperature in Celsius
    val powerConsumption: Float, // Power consumption in Watts
    val cpuCoreFrequencies: List<Int>, // CPU core frequencies in MHz
    val storageUsedPerc: Float,// 0.0 - 1.0
    val storageFreeGb: String,
    val networkSpeed: String,  // Total speed
    val networkDownloadSpeed: String, // Download speed
    val networkUploadSpeed: String,   // Upload speed
    val uptime: String,
    val maxCpuFrequency: Int = 3000, // Max CPU frequency in MHz
    val cpuHistory: List<CpuDataPoint> = emptyList(),
    val cpuCoreHistory: List<List<CpuCoreDataPoint>> = emptyList(),
    val ramHistory: List<MemoryDataPoint> = emptyList(),
    val powerHistory: List<PowerDataPoint> = emptyList()
)
