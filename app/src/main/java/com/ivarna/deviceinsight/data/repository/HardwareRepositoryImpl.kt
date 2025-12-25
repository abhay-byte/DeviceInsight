package com.ivarna.deviceinsight.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.repository.HardwareRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HardwareRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HardwareRepository {

    override fun getHardwareInfo(): HardwareInfo {
        return HardwareInfo(
            deviceName = Build.DEVICE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
            kernelVersion = getKernelVersion(),
            buildId = Build.ID,
            upTime = getUpTime(),
            resolution = getScreenResolution(),
            density = getDensityString(),
            densityDpi = context.resources.displayMetrics.densityDpi,
            refreshRate = getRefreshRate(),
            batteryTechnology = getBatteryTechnology(),
            batteryHealth = getBatteryHealth(),
            batteryCapacity = getBatteryCapacity(),
            sensorCount = getSensorCount(),
            availableSensors = getSensorList()
        )
    }

    private fun getKernelVersion(): String {
        return try {
            System.getProperty("os.version") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getUpTime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val uptimeSeconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMillis)
        val days = TimeUnit.SECONDS.toDays(uptimeSeconds)
        val hours = TimeUnit.SECONDS.toHours(uptimeSeconds) - TimeUnit.DAYS.toHours(days)
        val minutes = TimeUnit.SECONDS.toMinutes(uptimeSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.SECONDS.toHours(uptimeSeconds))
        return "${days}d ${hours}h ${minutes}m"
    }

    private fun getScreenResolution(): String {
        val metrics = context.resources.displayMetrics
        return "${metrics.widthPixels} x ${metrics.heightPixels}"
    }

    private fun getDensityString(): String {
        return when (context.resources.displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> "LDPI"
            DisplayMetrics.DENSITY_MEDIUM -> "MDPI"
            DisplayMetrics.DENSITY_HIGH -> "HDPI"
            DisplayMetrics.DENSITY_XHIGH -> "XHDPI"
            DisplayMetrics.DENSITY_XXHIGH -> "XXHDPI"
            DisplayMetrics.DENSITY_XXXHIGH -> "XXXHDPI"
            else -> "Unknown"
        }
    }

    private fun getRefreshRate(): Float {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return windowManager.defaultDisplay.refreshRate
    }

    private fun getBatteryTechnology(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
    }

    private fun getBatteryHealth(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        return when(health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    private fun getBatteryCapacity(): String {
        // Trying to read from typical system files for capacity
        // This is not guaranteed to work on all devices but is a common method
        val capacity = getProfileCapacity()
        return if (capacity > 0) "${capacity.toInt()} mAh" else "Unknown"
    }
    
    private fun getProfileCapacity(): Double {
        val mPowerProfile = try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            powerProfileClass.getConstructor(Context::class.java).newInstance(context)
        } catch (e: Exception) {
            null
        }
        
        return try {
            val getAveragePowerMethod = mPowerProfile?.javaClass?.getMethod("getAveragePower", String::class.java)
            getAveragePowerMethod?.invoke(mPowerProfile, "battery.capacity") as? Double ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun getSensorCount(): Int {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sensorManager.getSensorList(Sensor.TYPE_ALL).size
    }

    private fun getSensorList(): List<String> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        return sensorManager.getSensorList(Sensor.TYPE_ALL).map { it.name }
    }
}
