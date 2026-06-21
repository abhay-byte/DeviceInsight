package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

enum class SocFamily { SNAPDRAGON, MEDIATEK, TENSOR, EXYNOS, UNKNOWN }

@Singleton
class SocLogoRepository @Inject constructor() {

    fun logoUrlFor(marketingName: String): String? {
        return when (familyFor(marketingName)) {
            SocFamily.SNAPDRAGON -> SNAPDRAGON
            SocFamily.MEDIATEK -> MEDIATEK
            SocFamily.TENSOR -> TENSOR
            SocFamily.EXYNOS -> EXYNOS
            SocFamily.UNKNOWN -> null
        }
    }

    fun familyFor(marketingName: String): SocFamily {
        val n = marketingName.lowercase()
        return when {
            n.contains("snapdragon") -> SocFamily.SNAPDRAGON
            n.contains("dimensity") || n.contains("mediatek") -> SocFamily.MEDIATEK
            n.contains("tensor") -> SocFamily.TENSOR
            n.contains("exynos") -> SocFamily.EXYNOS
            n.contains("qcom") || n.contains("qualcomm") -> SocFamily.SNAPDRAGON
            n == "mt" -> SocFamily.MEDIATEK
            n.contains("samsung") || n.contains("universal") -> SocFamily.EXYNOS
            else -> SocFamily.UNKNOWN
        }
    }

    companion object {
        const val SNAPDRAGON =
            "https://www.freelogovectors.net/wp-content/uploads/2023/09/snapdragon-logo-07-freelogovectors.net_.png"
        const val MEDIATEK =
            "https://www.mediatek.com/hubfs/Mediatek_Corporate_Assets_June2025/Brand_Logos_Chips_june2025/Png%20Images/MediaTek%20Logo_Primary%20Logo_Orange.png"
        const val TENSOR =
            "https://developers.google.com/static/edge/tensor-sdk/images/tensor-logo.png"
        const val EXYNOS =
            "https://image.semiconductor.samsung.com/image/samsung/p6/semiconductor/sustainability_p5/exynos-processor-the-brains-of-a-smartphone-with-eyes-on-a-better-life/article03_sec_full1.png?\$ORIGIN_PNG\$"
    }
}
