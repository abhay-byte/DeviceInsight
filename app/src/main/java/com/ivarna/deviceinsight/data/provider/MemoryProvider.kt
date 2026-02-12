package com.ivarna.deviceinsight.data.provider

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MemoryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getMemoryInfo(): Pair<Long, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return Pair(memInfo.totalMem, memInfo.availMem)
    }

    fun getInstalledRamString(): String {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        
        val totalGb = Math.round(memInfo.totalMem.toDouble() / (1024 * 1024 * 1024)).toInt()
        
        // Hardcoded estimation for high-end SoCs common in this context
        val hardware = Build.HARDWARE.lowercase()
        val ramType = when {
            hardware.contains("mt6897") || hardware.contains("sm8650") -> "LPDDR5X"
            hardware.contains("sm8550") || hardware.contains("sm8450") -> "LPDDR5"
            else -> ""
        }
        
        return if (ramType.isNotEmpty()) "$totalGb GB $ramType" else "$totalGb GB"
    }
}
