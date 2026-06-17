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
            "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5c/Snapdragon_Logo.svg/500px-Snapdragon_Logo.svg.png"
        const val MEDIATEK =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/1a/MediaTek_Logo_wiki.svg/500px-MediaTek_Logo_wiki.svg.png"
        const val TENSOR =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7b/Representaci%C3%B3n_vectorial_referencial_del_chip_google_tensor.svg/500px-Representaci%C3%B3n_vectorial_referencial_del_chip_google_tensor.svg.png"
        const val EXYNOS =
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6f/Exynos_Logo.svg/500px-Exynos_Logo.svg.png"
    }
}
