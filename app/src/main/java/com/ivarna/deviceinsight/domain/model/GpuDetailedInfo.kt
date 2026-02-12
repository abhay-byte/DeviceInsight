package com.ivarna.deviceinsight.domain.model

data class GpuDetailedInfo(
    val openGlRenderer: String,
    val openGlVendor: String,
    val openGlVersion: String,
    val gpuVersion: String,
    val openGlExtensions: List<String>,
    
    val vulkanDeviceName: String,
    val vulkanDeviceType: String,
    val vulkanDeviceUuid: String,
    val vulkanDeviceId: String,
    val vulkanVendorId: String,
    val vulkanMemorySize: String,
    val vulkanApiVersion: String,
    val vulkanDriverVersion: String,
    val vulkanExtensions: List<String>,
    val vulkanLimits: Map<String, String>,
    val vulkanFeatures: Map<String, Boolean>
)
