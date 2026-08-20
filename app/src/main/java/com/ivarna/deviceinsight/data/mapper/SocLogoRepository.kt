package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

enum class SocFamily { SNAPDRAGON, MEDIATEK, TENSOR, EXYNOS, UNKNOWN }

@Singleton
class SocLogoRepository @Inject constructor() {

    fun logoDrawableResFor(marketingName: String): Int {
        return when (familyFor(marketingName)) {
            SocFamily.SNAPDRAGON -> com.ivarna.deviceinsight.R.drawable.ic_soc_snapdragon
            SocFamily.MEDIATEK -> com.ivarna.deviceinsight.R.drawable.ic_soc_mediatek
            SocFamily.TENSOR -> com.ivarna.deviceinsight.R.drawable.ic_soc_tensor
            SocFamily.EXYNOS -> com.ivarna.deviceinsight.R.drawable.ic_soc_exynos
            SocFamily.UNKNOWN -> com.ivarna.deviceinsight.R.drawable.ic_soc_generic
        }
    }

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
        const val SNAPDRAGON = "file:///android_asset/soc_snapdragon.png"
        const val MEDIATEK = "file:///android_asset/soc_mediatek.png"
        const val TENSOR = "file:///android_asset/soc_tensor.png"
        const val EXYNOS = "file:///android_asset/soc_exynos.jpg"
    }
}
