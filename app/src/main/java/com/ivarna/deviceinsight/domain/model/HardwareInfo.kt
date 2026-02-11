package com.ivarna.deviceinsight.domain.model

data class HardwareInfo(
    val deviceName: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val board: String,
    val hardware: String,
    val product: String,
    val serial: String,
    val deviceType: String,
    val supportedAbis: List<String>,
    val cpuCoreCount: Int,
    
    // Memory & Storage
    val totalRam: Long,
    val availableRam: Long,
    val totalStorage: Long,
    val availableStorage: Long,
    val totalExternalStorage: Long,
    val availableExternalStorage: Long,

    // Network
    val networkOperator: String,
    val networkType: String,
    val ipAddress: String,
    
    // OS
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val kernelVersion: String,
    val buildId: String,
    val isRooted: Boolean,
    val upTime: String,
    
    // CPU Details
    val socModel: String,
    val cpuArchitecture: String,
    val manufacturingProcess: String,
    val cpuRevision: String,
    val cpuClockRange: String,
    val cpuUtilization: Float,
    val coreClocks: List<Int>,
    val supported64BitAbis: List<String>,
    val hasAes: Boolean,
    val hasNeon: Boolean,
    val hasPmull: Boolean,
    val hasSha1: Boolean,
    val hasSha2: Boolean,
    
    // Display
    val resolution: String,
    val density: String,
    val densityDpi: Int,
    val refreshRate: Float,
    
    // Battery
    val batteryTechnology: String,
    val batteryHealth: String, // Good, Dead etc
    val batteryLevel: Int,
    val batteryStatus: String,
    val batteryVoltage: Int,
    val batteryTemperature: Float,
    val isCharging: Boolean,
    val batteryCapacity: String, // Capacity if available
    
    // Sensors
    val sensorCount: Int,
    val availableSensors: List<String>,
    val fingerprintSensorPresent: Boolean
)
