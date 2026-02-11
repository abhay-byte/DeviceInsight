package com.ivarna.deviceinsight.data.provider

import android.util.Log
import java.io.File
import javax.inject.Inject

class ThermalProvider @Inject constructor() {
    fun getCpuTemperature(): Float {
        try {
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone1/temp",
                "/sys/class/thermal/thermal_zone7/temp" // Common on some devices
            )
            
            for (path in thermalPaths) {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val temp = file.readText().trim().toIntOrNull()
                    if (temp != null) {
                        return if (temp > 1000) temp / 1000f else temp.toFloat()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ThermalProvider", "Error reading CPU temperature: ${e.message}")
        }
        return 0f
    }
}
