package com.ivarna.deviceinsight.data.provider

import android.util.Log
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import java.io.File
import java.io.RandomAccessFile
import javax.inject.Inject

class CpuProvider @Inject constructor(
    private val cpuUtilizationUtils: CpuUtilizationUtils
) {
    fun getSocModel(): String {
        return try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            var hardware = ""
            var model = ""
            for (line in cpuInfo) {
                if (line.startsWith("Hardware")) hardware = line.split(":")[1].trim()
                if (line.startsWith("Model")) model = line.split(":")[1].trim()
            }
            if (hardware.isNotEmpty()) hardware else if (model.isNotEmpty()) model else android.os.Build.HARDWARE
        } catch (e: Exception) {
            android.os.Build.HARDWARE
        }
    }

    fun getCpuArchitecture(): String {
        return try {
            System.getProperty("os.arch") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getManufacturingProcess(): String {
        // This is extremely hardware specific and often not exposed in system files.
        // Returning a placeholder or trying to map common SoCs could work, 
        // but for now, we'll try to look at common SoC names.
        val soc = getSocModel().lowercase()
        return when {
            soc.contains("sm8650") -> "4 nm" // Snapdragon 8 Gen 3
            soc.contains("sm8550") -> "4 nm" // Snapdragon 8 Gen 2
            soc.contains("sm8450") -> "4 nm" // Snapdragon 8 Gen 1
            soc.contains("sm8350") -> "5 nm" // Snapdragon 888
            soc.contains("sm8250") -> "7 nm" // Snapdragon 865
            soc.contains("msm8998") -> "10 nm" // Snapdragon 835
            soc.contains("msm8996") -> "14 nm" // Snapdragon 820
            soc.contains("msm8974") -> "28 nm" // Snapdragon 800
            else -> "Unknown"
        }
    }

    fun getCpuRevision(): String {
        return try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            for (line in cpuInfo) {
                if (line.contains("CPU revision")) return line.split(":")[1].trim()
            }
            "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getCpuClockRange(): String {
        var minFreq = Long.MAX_VALUE
        var maxFreq = 0L
        try {
            for (i in 0 until Runtime.getRuntime().availableProcessors()) {
                val minFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                
                if (minFile.exists()) {
                    val min = minFile.readText().trim().toLongOrNull() ?: continue
                    if (min < minFreq) minFreq = min
                }
                if (maxFile.exists()) {
                    val max = maxFile.readText().trim().toLongOrNull() ?: continue
                    if (max > maxFreq) maxFreq = max
                }
            }
            if (maxFreq > 0) {
                return "${minFreq / 1000} MHz - ${maxFreq / 1000} MHz"
            }
        } catch (e: Exception) { }
        return "Unknown"
    }

    fun getCpuUtilization(): Float {
        return cpuUtilizationUtils.getCpuUtilizationPercentage()
    }

    fun getFeatures(): Map<String, Boolean> {
        val features = mutableMapOf(
            "aes" to false,
            "neon" to false,
            "pmull" to false,
            "sha1" to false,
            "sha2" to false
        )
        try {
            val cpuInfo = File("/proc/cpuinfo").readText().lowercase()
            features["aes"] = cpuInfo.contains("aes")
            features["neon"] = cpuInfo.contains("neon") || cpuInfo.contains("asimd")
            features["pmull"] = cpuInfo.contains("pmull")
            features["sha1"] = cpuInfo.contains("sha1")
            features["sha2"] = cpuInfo.contains("sha2")
        } catch (e: Exception) { }
        return features
    }

    fun getMaxCpuFrequency(): Int {
        var maxFreq = 0
        try {
            for (i in 0 until 16) {
                var file = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (!file.exists()) {
                    file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
                }
                
                if (file.exists() && file.canRead()) {
                    val freq = file.readText().trim().toIntOrNull()
                    if (freq != null && freq > maxFreq) maxFreq = freq
                }
            }
        } catch (e: Exception) {
            Log.e("CpuProvider", "Error reading max CPU freq: ${e.message}")
        }
        return if (maxFreq > 0) maxFreq / 1000 else 3000
    }

    fun getCpuCoreFrequencies(): List<Int> {
        return cpuUtilizationUtils.getAllCoreFrequencies().values.map { 
            (it.first / 1000).toInt()
        }
    }
}
