package com.ivarna.deviceinsight.data.provider

import android.util.Log
import com.ivarna.deviceinsight.data.mapper.SocMapper
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import java.io.File
import javax.inject.Inject

class CpuProvider @Inject constructor(
    private val cpuUtilizationUtils: CpuUtilizationUtils,
    private val socMapper: SocMapper
) {
    fun getSocModel(): String {
        val hardware = try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            var hw = ""
            for (line in cpuInfo) {
                if (line.startsWith("Hardware")) hw = line.split(":")[1].trim()
            }
            if (hw.isEmpty()) android.os.Build.HARDWARE else hw
        } catch (e: Exception) {
            android.os.Build.HARDWARE
        }

        return socMapper.mapHardwareToMarketingName(hardware)
    }

    fun getCpuArchitecture(): String {
        // ...Existing implementation for core detail parsing...
        return try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            val cores = mutableMapOf<String, Int>()
            var currentPart = ""
            
            for (line in cpuInfo) {
                if (line.startsWith("CPU part")) {
                    val part = line.split(":")[1].trim().lowercase()
                    val name = when (part) {
                        "0xd4e" -> "Cortex-X3"
                        "0xd4f" -> "Cortex-A715"
                        "0xd41" -> "Cortex-A78"
                        "0xd03" -> "Cortex-A53"
                        "0xd08" -> "Cortex-A72"
                        "0xd42" -> "Cortex-A78C"
                        "0xd44" -> "Cortex-X2"
                        "0xd46" -> "Cortex-A510"
                        "0xd4b" -> "Cortex-A710"
                        "0xd47" -> "Cortex-A715"
                        "0xd4d" -> "Cortex-A715"
                        else -> "Cortex (Part $part)"
                    }
                    cores[name] = cores.getOrDefault(name, 0) + 1
                }
            }
            
            if (cores.isNotEmpty()) {
                cores.entries.joinToString(" + ") { "${it.value}x ${it.key}" }
            } else {
                System.getProperty("os.arch") ?: "aarch64"
            }
        } catch (e: Exception) {
            System.getProperty("os.arch") ?: "Unknown"
        }
    }

    fun getManufacturingProcess(): String {
        val hardware = try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            var hw = ""
            for (line in cpuInfo) {
                if (line.startsWith("Hardware")) hw = line.split(":")[1].trim()
            }
            if (hw.isEmpty()) android.os.Build.HARDWARE else hw
        } catch (e: Exception) {
            android.os.Build.HARDWARE
        }
        
        return socMapper.getProcessNode(hardware)
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
