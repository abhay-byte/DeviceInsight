package com.ivarna.deviceinsight.data.provider

import android.util.Log
import com.ivarna.deviceinsight.domain.model.ThermalSensor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalProvider @Inject constructor() {
    
    fun getThermalSensors(): List<ThermalSensor> {
        val sensors = mutableListOf<ThermalSensor>()
        try {
            val thermalDir = File("/sys/class/thermal/")
            if (thermalDir.exists() && thermalDir.isDirectory) {
                thermalDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("thermal_zone")) {
                        try {
                            val typeFile = File(file, "type")
                            val tempFile = File(file, "temp")
                            
                            if (typeFile.exists() && tempFile.exists()) {
                                val type = typeFile.readText().trim()
                                val tempRaw = tempFile.readText().trim().toLongOrNull() ?: 0L
                                
                                // Temperature is usually in milli-Celsius
                                val temp = if (tempRaw > 1000 || tempRaw < -1000) {
                                    tempRaw / 1000f
                                } else {
                                    tempRaw.toFloat()
                                }
                                
                                if (temp != 0f) {
                                    sensors.add(ThermalSensor(type, temp))
                                }
                            }
                        } catch (e: Exception) {
                            // Skip zones we can't read
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ThermalProvider", "Error reading thermal sensors: ${e.message}")
        }
        
        // Sort by name for consistency
        return sensors.sortedBy { it.name }
    }

    fun getCpuTemperature(): Float {
        val sensors = getThermalSensors()
        // Try to find a sensor that likely represents the package or a core
        return sensors.firstOrNull { 
            val name = it.name.lowercase()
            name.contains("cpu") || name.contains("soc") || name.contains("package")
        }?.temperature ?: 0f
    }
}
