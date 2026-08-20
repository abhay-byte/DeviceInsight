package com.ivarna.deviceinsight.data.provider

import android.util.Log
import com.ivarna.deviceinsight.data.mapper.SocMapper
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import java.io.File
import javax.inject.Inject

@javax.inject.Singleton
class CpuProvider @Inject constructor(
    private val cpuUtilizationUtils: CpuUtilizationUtils,
    private val socMapper: SocMapper
) {
    @Volatile private var cachedSocModel: String? = null
    @Volatile private var cachedCpuArchitecture: String? = null
    @Volatile private var cachedManufacturingProcess: String? = null
    @Volatile private var cachedCpuRevision: String? = null
    @Volatile private var cachedCpuClockRange: String? = null
    @Volatile private var cachedFeatures: Map<String, Boolean>? = null
    @Volatile private var cachedMaxCpuFreq: Int? = null
    @Volatile private var cachedCoreParts: List<String>? = null
    @Volatile private var cachedCoreTypes: List<String>? = null

    private fun getSystemProperty(key: String): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java, String::class.java)
            get.invoke(null, key, "") as String
        } catch (_: Exception) {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("getprop", key))
                p.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
            } catch (_: Exception) { "" }
        }
    }

    fun getSocModel(): String {
        cachedSocModel?.let { return it }
        
        // 1. Try ro.soc.model
        var soc = getSystemProperty("ro.soc.model")
        if (soc.isNotBlank()) {
            val mapped = socMapper.mapHardwareToMarketingName(soc)
            if (mapped != soc.uppercase()) {
                cachedSocModel = mapped
                return mapped
            }
        }

        // 2. Try ro.board.platform
        val platform = getSystemProperty("ro.board.platform")
        if (platform.isNotBlank()) {
            val mapped = socMapper.mapHardwareToMarketingName(platform)
            if (mapped != platform.uppercase()) {
                cachedSocModel = mapped
                return mapped
            }
        }

        // 3. Try /proc/cpuinfo Hardware line
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

        val result = socMapper.mapHardwareToMarketingName(
            if (soc.isNotBlank()) soc else if (platform.isNotBlank()) platform else hardware
        )
        cachedSocModel = result
        return result
    }

    fun getCpuArchitecture(): String {
        cachedCpuArchitecture?.let { return it }
        val result = try {
            val cores = mutableMapOf<String, Int>()
            for (part in readCpuParts()) {
                val name = partToName(part)
                cores[name] = cores.getOrDefault(name, 0) + 1
            }
            if (cores.isNotEmpty()) {
                cores.entries.joinToString(" + ") { "${it.value}x ${it.key}" }
            } else {
                System.getProperty("os.arch") ?: "aarch64"
            }
        } catch (e: Exception) {
            System.getProperty("os.arch") ?: "Unknown"
        }
        cachedCpuArchitecture = result
        return result
    }

    /** Ordered CPU-part IDs from /proc/cpuinfo, one per logical core. */
    fun getCoreParts(): List<String> {
        cachedCoreParts?.let { return it }
        val result = try {
            readCpuParts()
        } catch (e: Exception) {
            emptyList()
        }
        cachedCoreParts = result
        return result
    }

    /** Type name per logical core, aligned with /proc/cpuinfo order. */
    fun getCoreTypes(): List<String> {
        cachedCoreTypes?.let { return it }
        val parts = getCoreParts()
        val result = if (parts.isEmpty()) {
            List(Runtime.getRuntime().availableProcessors()) { "CPU" }
        } else {
            parts.map { partToName(it) }
        }
        cachedCoreTypes = result
        return result
    }

    private fun readCpuParts(): List<String> {
        val cpuInfo = File("/proc/cpuinfo").readLines()
        val parts = mutableListOf<String>()
        for (line in cpuInfo) {
            if (line.trim().startsWith("CPU part")) {
                val part = line.split(":")[1].trim().lowercase()
                parts.add(part)
            }
        }
        return parts
    }

    private fun partToName(part: String): String = when (part) {
        "0xd85" -> "Cortex-X925"
        "0xd87" -> "Cortex-A725"
        "0xd80" -> "Cortex-X4"
        "0xd81" -> "Cortex-A720"
        "0xd82" -> "Cortex-A520"
        "0xd4e" -> "Cortex-X3"
        "0xd4f", "0xd47", "0xd4d" -> "Cortex-A715"
        "0xd44" -> "Cortex-X2"
        "0xd4b" -> "Cortex-A710"
        "0xd46" -> "Cortex-A510"
        "0xd41", "0xd42" -> "Cortex-A78"
        "0xd0d" -> "Cortex-A77"
        "0xd0b" -> "Cortex-A76"
        "0xd0a" -> "Cortex-A75"
        "0xd09" -> "Cortex-A73"
        "0xd08" -> "Cortex-A72"
        "0xd05" -> "Cortex-A55"
        "0xd03" -> "Cortex-A53"
        "0xd84" -> "Oryon"
        else -> "Cortex (Part $part)"
    }

    fun getManufacturingProcess(): String {
        cachedManufacturingProcess?.let { return it }
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
        
        val result = socMapper.getProcessNode(hardware)
        cachedManufacturingProcess = result
        return result
    }

    fun getCpuRevision(): String {
        cachedCpuRevision?.let { return it }
        val result = try {
            val cpuInfo = File("/proc/cpuinfo").readLines()
            var rev = "Unknown"
            for (line in cpuInfo) {
                if (line.contains("CPU revision")) {
                    rev = line.split(":")[1].trim()
                    break
                }
            }
            rev
        } catch (e: Exception) {
            "Unknown"
        }
        cachedCpuRevision = result
        return result
    }

    fun getCpuClockRange(): String {
        cachedCpuClockRange?.let { return it }
        var minFreq = Long.MAX_VALUE
        var maxFreq = 0L
        val result = try {
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
                "${minFreq / 1000} MHz - ${maxFreq / 1000} MHz"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
        if (result != "Unknown") {
            cachedCpuClockRange = result
        }
        return result
    }

    fun getCpuUtilization(): Float {
        return cpuUtilizationUtils.getCpuUtilizationPercentage()
    }

    fun getFeatures(): Map<String, Boolean> {
        cachedFeatures?.let { return it }
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
        cachedFeatures = features
        return features
    }

    fun getMaxCpuFrequency(): Int {
        cachedMaxCpuFreq?.let { return it }
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
        val result = if (maxFreq > 0) maxFreq / 1000 else 3000
        cachedMaxCpuFreq = result
        return result
    }

    fun getCpuCoreFrequencies(): List<Int> {
        return cpuUtilizationUtils.getAllCoreFrequencies().values.map { 
            (it.first / 1000).toInt()
        }
    }

    fun getCpuCoreMaxFrequencies(): List<Int> {
        return cpuUtilizationUtils.getAllCoreFrequencies().values.map { 
            (it.second / 1000).toInt()
        }
    }
}
