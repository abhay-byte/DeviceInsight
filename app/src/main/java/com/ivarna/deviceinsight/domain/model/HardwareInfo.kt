package com.ivarna.deviceinsight.domain.model

data class HardwareInfo(
    val deviceName: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val board: String,
    val hardware: String,
    
    // OS
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val kernelVersion: String,
    val buildId: String,
    val upTime: String,
    
    // Display
    val resolution: String,
    val density: String,
    val densityDpi: Int,
    val refreshRate: Float,
    
    // Battery
    val batteryTechnology: String,
    val batteryHealth: String, // Good, Dead etc
    val batteryCapacity: String, // Capacity if available
    
    // Sensors
    val sensorCount: Int,
    val availableSensors: List<String>
)
