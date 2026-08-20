package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocMapper @Inject constructor() {
    
    fun mapHardwareToMarketingName(hardware: String): String {
        val hw = hardware.lowercase().trim()
        
        return when {
            // MediaTek Dimensity Series
            hw.contains("mt6991") -> "MediaTek Dimensity 9400"
            hw.contains("mt6989") -> "MediaTek Dimensity 9300"
            hw.contains("mt6985") -> "MediaTek Dimensity 9200"
            hw.contains("mt6983") -> "MediaTek Dimensity 9000"
            hw.contains("mt6897") -> "MediaTek Dimensity 8300 Ultra"
            hw.contains("mt6895") -> "MediaTek Dimensity 8100"
            hw.contains("mt6893") -> "MediaTek Dimensity 1200"
            hw.contains("mt6877") -> "MediaTek Dimensity 900"
            hw.contains("mt6853") -> "MediaTek Dimensity 720"
            hw.contains("mt6833") -> "MediaTek Dimensity 700"
            
            // Qualcomm Snapdragon 8 Series
            hw.contains("sm8750") || hw.contains("sun") -> "Snapdragon 8 Elite"
            hw.contains("sm8650") || hw.contains("pineapple") -> "Snapdragon 8 Gen 3"
            hw.contains("sm8550") || hw.contains("kalama") -> "Snapdragon 8 Gen 2"
            hw.contains("sm8475") || hw.contains("cape") -> "Snapdragon 8+ Gen 1"
            hw.contains("sm8450") || hw.contains("taro") -> "Snapdragon 8 Gen 1"
            hw.contains("sm8350") || hw.contains("lahaina") -> "Snapdragon 888"
            hw.contains("sm8250") || hw.contains("kona") -> "Snapdragon 865"
            hw.contains("sm8150") || hw.contains("msmnile") -> "Snapdragon 855"
            hw.contains("msm8998") -> "Snapdragon 835"
            hw.contains("msm8996") -> "Snapdragon 820"
            
            // Qualcomm Snapdragon 7 & 6 Series
            hw.contains("sm7675") -> "Snapdragon 7+ Gen 3"
            hw.contains("sm7550") || hw.contains("crow") -> "Snapdragon 7 Gen 3"
            hw.contains("sm7475") -> "Snapdragon 7+ Gen 2"
            hw.contains("sm7450") -> "Snapdragon 7 Gen 1"
            hw.contains("sm7325") || hw.contains("yupik") -> "Snapdragon 778G"
            hw.contains("sm6450") -> "Snapdragon 6 Gen 1"
            hw.contains("sm6375") || hw.contains("holi") -> "Snapdragon 695"
            
            // Google Tensor
            hw.contains("zuma") || hw.contains("gs401") -> "Google Tensor G4"
            hw.contains("zuma pro") || hw.contains("gs301") -> "Google Tensor G3"
            hw.contains("cloudripper") || hw.contains("gs201") -> "Google Tensor G2"
            hw.contains("raviole") || hw.contains("gs101") -> "Google Tensor G1"
            
            // Samsung Exynos
            hw.contains("s5e9945") || hw.contains("exynos2400") -> "Exynos 2400"
            hw.contains("s5e9925") || hw.contains("exynos2200") -> "Exynos 2200"
            hw.contains("s5e9840") || hw.contains("exynos2100") -> "Exynos 2100"
            hw.contains("s5e9820") || hw.contains("exynos9820") -> "Exynos 9820"
            hw.contains("s5e9611") || hw.contains("exynos9611") -> "Exynos 9611"
            
            else -> hardware.uppercase()
        }
    }

    fun getProcessNode(hardware: String): String {
        val hw = hardware.lowercase()
        return when {
            // 3nm / 4nm
            hw.contains("sm8650") || hw.contains("mt6989") || hw.contains("s5e9945") -> "3 nm"
            hw.contains("mt6897") || hw.contains("sm8550") || hw.contains("sm8450") || hw.contains("gs301") -> "4 nm"
            
            // 5nm
            hw.contains("sm8350") || hw.contains("gs201") || hw.contains("s5e9925") -> "5 nm"
            
            // 6nm / 7nm
            hw.contains("sm7325") || hw.contains("mt6877") || hw.contains("sm6375") -> "6 nm"
            hw.contains("sm8250") || hw.contains("sm8150") || hw.contains("mt6893") -> "7 nm"
            
            // Older
            hw.contains("msm8998") -> "10 nm"
            hw.contains("msm8996") -> "14 nm"
            else -> "Unknown"
        }
    }
}
