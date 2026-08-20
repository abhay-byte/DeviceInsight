package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpuMapper @Inject constructor() {
    
    fun mapHardwareToGpuInfo(hardware: String): GpuInfo {
        val hw = hardware.lowercase().trim()
        
        return when {
            hw.contains("mt6897") -> GpuInfo("ARM", "Mali-G615 MC6", 6, 400, 1400)
            hw.contains("sm8750") || hw.contains("sun") -> GpuInfo("Qualcomm", "Adreno 830", 6, 300, 1100)
            hw.contains("sm8650") || hw.contains("pineapple") -> GpuInfo("Qualcomm", "Adreno 750", 6, 305, 903)
            hw.contains("sm8550") || hw.contains("kalama") -> GpuInfo("Qualcomm", "Adreno 740", 4, 220, 680)
            hw.contains("sm8450") || hw.contains("taro") -> GpuInfo("Qualcomm", "Adreno 730", 4, 220, 818)
            hw.contains("sm8475") || hw.contains("cape") -> GpuInfo("Qualcomm", "Adreno 730", 4, 220, 900)
            hw.contains("sm8350") || hw.contains("lahaina") -> GpuInfo("Qualcomm", "Adreno 660", 3, 250, 840)
            hw.contains("sm8250") || hw.contains("kona") -> GpuInfo("Qualcomm", "Adreno 650", 3, 250, 670)
            hw.contains("sm7475") || hw.contains("marble") -> GpuInfo("Qualcomm", "Adreno 725", 4, 220, 580)
            hw.contains("mt6991") -> GpuInfo("ARM", "Immortalis-G925 MC12", 12, 300, 1612)
            hw.contains("mt6989") -> GpuInfo("ARM", "Immortalis-G720 MC12", 12, 300, 1300)
            hw.contains("mt6985") -> GpuInfo("ARM", "Immortalis-G715 MC11", 11, 300, 981)
            hw.contains("mt6983") || hw.contains("dimensity 9000") -> GpuInfo("ARM", "Mali-G710 MC10", 10, 350, 848)
            hw.contains("mt6895") || hw.contains("dimensity 8100") -> GpuInfo("ARM", "Mali-G610 MC6", 6, 350, 850)
            hw.contains("mt6877") || hw.contains("dimensity 900") -> GpuInfo("ARM", "Mali-G68 MC4", 4, 300, 900)
            hw.contains("tensor g4") || hw.contains("zuma") -> GpuInfo("ARM", "Mali-G715 MP7", 7, 300, 890)
            hw.contains("tensor g3") || hw.contains("zuma") -> GpuInfo("ARM", "Mali-G715", 7, 300, 890)
            hw.contains("tensor g2") || hw.contains("cheetah") -> GpuInfo("ARM", "Mali-G710 MP7", 7, 300, 848)
            hw.contains("exynos 2400") || hw.contains("s5e9945") -> GpuInfo("Samsung / AMD", "Xclipse 940", 6, 300, 1095)
            hw.contains("exynos 2200") || hw.contains("s5e9925") -> GpuInfo("Samsung / AMD", "Xclipse 920", 3, 300, 1300)
            else -> GpuInfo("Integrated", "Mobile GPU", 0, 300, 850)
        }
    }

    data class GpuInfo(
        val vendor: String,
        val renderer: String,
        val cores: Int,
        val baseFreqMhz: Int = 300,
        val maxFreqMhz: Int = 900
    )
}
