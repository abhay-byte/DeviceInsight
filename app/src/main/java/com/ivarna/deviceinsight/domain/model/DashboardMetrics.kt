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
    val gpuTemp: Float = 0f,   // °C; 0 = unavailable
    val gpuFreqMhz: Int = 0,
    val gpuMaxFreqMhz: Int = 0,
    val gpuMinFreqMhz: Int = 0,
    val gpuCores: Int = 0,
    val gpuVendor: String = "Unknown",
    val batteryLevel: Int,     // 0 - 100
    val batteryStatus: String, // Charging, Discharging, etc.
    val batteryVoltage: Int = 0, // in mV
    val batteryHealth: String = "Good",
    val isCharging: Boolean = false,
    val temperature: Float,    // Battery temperature in Celsius
    val cpuTemperature: Float, // CPU temperature in Celsius
    val powerConsumption: Float, // Power consumption in Watts
    val cpuCoreFrequencies: List<Int> = emptyList(), // CPU core frequencies in MHz
    val cpuCoreMaxFrequencies: List<Int> = emptyList(), // Max frequency per core in MHz
    val cpuClockRange: String = "",
    val cpuArchitecture: String = "",
    val cpuTotalCores: Int = 8,
    val storageUsedPerc: Float,// 0.0 - 1.0
    val storageFreeGb: String,
    val storageTotalGb: String = "",
    val storageUsedGb: String = "",
    val networkSpeed: String,  // Total speed
    val networkDownloadSpeed: String, // Download speed
    val networkUploadSpeed: String,   // Upload speed
    val uptime: String,
    val cpuGovernor: String? = null,
    val maxCpuFrequency: Int = 3000, // Max CPU frequency in MHz
    val screenRefreshRate: Int = 60,
    val cpuHistory: List<CpuDataPoint> = emptyList(),
    val cpuCoreHistory: List<List<CpuCoreDataPoint>> = emptyList(),
    val ramHistory: List<MemoryDataPoint> = emptyList(),
    val powerHistory: List<PowerDataPoint> = emptyList(),
    val fps: Int = 0,
    val fpsHistory: List<FpsDataPoint> = emptyList(),
    val gpuHistory: List<Float> = emptyList(),
    val netHistory: List<Float> = emptyList()
)
