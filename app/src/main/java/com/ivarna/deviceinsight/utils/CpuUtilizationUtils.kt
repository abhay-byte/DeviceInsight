package com.ivarna.deviceinsight.utils

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.pow

class CpuUtilizationUtils(context: Context) {
    private val cpuDir = File("/sys/devices/system/cpu/")
    private val totalCores = Runtime.getRuntime().availableProcessors()
    
    // Exponent calibrated so:
    // - 2000/3200 (62.5% ratio) -> ~30% utilization
    // - 2800/3200 (87.5% ratio) -> ~80% utilization
    private val SCALING_EXPONENT = 2.5f
    
    // Cache for CPU frequencies with 200ms refresh rate
    private val frequencyCache = mutableMapOf<Int, Pair<Long, Long>>() // coreId to (currentFreq, maxFreq)
    private var lastCacheUpdate = 0L
    private val CACHE_DURATION_MS = 200L // 200ms cache duration

    /**
     * Calculate CPU utilization percentage using exponential scaling.
     * This better reflects real-world CPU workload intensity based on power consumption physics:
     * P proportional to V^2 * f where voltage scales non-linearly with frequency.
     */
    fun getCpuUtilizationPercentage(useExponentialScaling: Boolean = true): Float {
        return try {
            if (!cpuDir.exists()) {
                return 0f
            }

            var totalWeightedUtilization = 0f
            var totalWeight = 0L
            var validCores = 0

            val allFrequencies = getAllCoreFrequencies()

            for (i in 0 until totalCores) {
                val freqPair = allFrequencies[i] ?: continue
                val currentFreq = freqPair.first
                val maxFreq = freqPair.second

                if (currentFreq > 0 && maxFreq > 0) {
                    // Calculate utilization based on scaling method
                    val utilization = if (useExponentialScaling) {
                        calculateExponentialUtilization(currentFreq, maxFreq)
                    } else {
                        calculateLinearUtilization(currentFreq, maxFreq)
                    }
                    
                    // Weight by max frequency (performance cores contribute more)
                    val weight = maxFreq
                    totalWeightedUtilization += utilization * weight
                    totalWeight += weight
                    validCores++
                }
            }

            if (validCores > 0 && totalWeight > 0) {
                val weightedUtilization = totalWeightedUtilization / totalWeight
                return (weightedUtilization / 100f).coerceIn(0f, 1f) // Return as 0.0-1.0
            }
            
            0f
        } catch (e: Exception) {
            Log.e("CpuUtilizationUtils", "Error getting CPU utilization", e)
            0f
        }
    }

    private fun calculateExponentialUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        val ratio = (currentSpeed.toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
        return ratio.pow(SCALING_EXPONENT) * 100f
    }

    private fun calculateLinearUtilization(currentSpeed: Long, maxSpeed: Long): Float {
        if (maxSpeed == 0L) return 0f
        return (currentSpeed.toFloat() / maxSpeed.toFloat() * 100).coerceIn(0f, 100f)
    }

    fun getAllCoreFrequencies(): Map<Int, Pair<Long, Long>> {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCacheUpdate < CACHE_DURATION_MS && frequencyCache.isNotEmpty()) {
            return frequencyCache.toMap()
        }
        
        val frequencies = mutableMapOf<Int, Pair<Long, Long>>()
        for (i in 0 until totalCores) {
            val currentFreq = getCurrentCpuFreq(i)
            val maxFreq = getMaxCpuFreq(i)
            frequencies[i] = Pair(currentFreq, maxFreq)
        }
        
        frequencyCache.clear()
        frequencyCache.putAll(frequencies)
        lastCacheUpdate = currentTime
        
        return frequencies
    }

    private fun getCurrentCpuFreq(coreIndex: Int): Long {
        return try {
            val scalingCurFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq")
            if (scalingCurFreqFile.exists()) {
                scalingCurFreqFile.readText().trim().toLongOrNull() ?: 0L
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun getMaxCpuFreq(coreIndex: Int): Long {
        return try {
            val cpuinfoMaxFreqFile = File("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq")
            if (cpuinfoMaxFreqFile.exists()) {
                cpuinfoMaxFreqFile.readText().trim().toLongOrNull() ?: 0L
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }
}
