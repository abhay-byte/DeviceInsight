package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpuMapper @Inject constructor() {
    
    fun mapHardwareToGpuInfo(hardware: String): GpuInfo {
        val hw = hardware.lowercase().trim()
        
        return when {
            hw.contains("mt6897") -> GpuInfo("ARM", "Mali-G615 MC6", 6)
            hw.contains("sm8650") -> GpuInfo("Qualcomm", "Adreno 750", 0)
            hw.contains("sm8550") -> GpuInfo("Qualcomm", "Adreno 740", 0)
            hw.contains("sm8450") -> GpuInfo("Qualcomm", "Adreno 730", 0)
            hw.contains("mt6989") -> GpuInfo("ARM", "Immortalis-G720 MC12", 12)
            hw.contains("mt6985") -> GpuInfo("ARM", "Immortalis-G715 MC11", 11)
            else -> GpuInfo("Unknown", "Unknown", 0)
        }
    }

    data class GpuInfo(
        val vendor: String,
        val renderer: String,
        val cores: Int
    )
}
