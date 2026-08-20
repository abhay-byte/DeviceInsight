package com.ivarna.deviceinsight.data.mapper

import javax.inject.Inject
import javax.inject.Singleton

enum class GpuVendor { MALI, ADRENO, XCLIPSE, POWERV, UNKNOWN }

@Singleton
class GpuLogoRepository @Inject constructor() {

    fun urlFor(renderer: String, vendor: String): String? {
        return when (vendorFor(renderer, vendor)) {
            GpuVendor.MALI -> MALI
            GpuVendor.ADRENO -> ADRENO
            GpuVendor.XCLIPSE -> XCLIPSE
            GpuVendor.POWERV -> POWERV
            GpuVendor.UNKNOWN -> null
        }
    }

    fun vendorFor(renderer: String, vendor: String): GpuVendor {
        val r = renderer.lowercase()
        val v = vendor.lowercase()
        return when {
            r.contains("mali") || r.contains("immortalis") -> GpuVendor.MALI
            r.contains("adreno") -> GpuVendor.ADRENO
            r.contains("xclipse") || v.contains("amd") -> GpuVendor.XCLIPSE
            r.contains("powervr") || v.contains("imagination") -> GpuVendor.POWERV
            else -> GpuVendor.UNKNOWN
        }
    }

    companion object {
        const val MALI = "file:///android_asset/gpu_arm_mali.jpg"
        const val ADRENO = "file:///android_asset/gpu_adreno.jpeg"
        const val XCLIPSE = "file:///android_asset/gpu_xclipse.webp"
        const val POWERV = "file:///android_asset/gpu_powervr.jpg"
    }
}