package com.ivarna.deviceinsight.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.telephony.TelephonyManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.repository.HardwareRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class HardwareRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HardwareRepository {

    override fun getHardwareInfo(): HardwareInfo {
        val memInfo = getMemoryInfo()
        val storageInfo = getStorageInfo()
        val batteryInfo = getBatteryDetails()
        
        return HardwareInfo(
            deviceName = Build.DEVICE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            
            totalRam = memInfo.first,
            availableRam = memInfo.second,
            totalStorage = storageInfo.first,
            availableStorage = storageInfo.second,
            
            networkOperator = getNetworkOperatorName(),
            networkType = getNetworkType(),
            ipAddress = getIpAddress(),
            
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
            kernelVersion = getKernelVersion(),
            buildId = Build.ID,
            isRooted = checkRootMethod1() || checkRootMethod2() || checkRootMethod3(),
            upTime = getUpTime(),
            
            resolution = getScreenResolution(),
            density = getDensityString(),
            densityDpi = context.resources.displayMetrics.densityDpi,
            refreshRate = getRefreshRate(),
            
            batteryTechnology = batteryInfo.technology,
            batteryHealth = batteryInfo.health,
            batteryLevel = batteryInfo.level,
            batteryStatus = batteryInfo.status,
            batteryVoltage = batteryInfo.voltage,
            batteryTemperature = batteryInfo.temperature,
            isCharging = batteryInfo.isCharging,
            batteryCapacity = getBatteryCapacity(),
            
            sensorCount = getSensorCount(),
            availableSensors = getSensorList(),
            fingerprintSensorPresent = hasFingerprintSensor()
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

    private fun getBatteryDetails(): LocalBatteryInfo {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val statusString = when(status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: 0
        val healthString = when(health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        val technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        return LocalBatteryInfo(technology, healthString, batteryPct, statusString, voltage, temperature, isCharging)
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

    private fun getMemoryInfo(): Pair<Long, Long> {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return Pair(memInfo.totalMem, memInfo.availMem)
    }

    private fun getStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        return Pair(totalBlocks * blockSize, availableBlocks * blockSize)
    }
    
    private fun getNetworkOperatorName(): String {
        val manager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return manager.networkOperatorName ?: "Unknown"
    }
    
    private fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "No Network"
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "Unknown"
        
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other"
        }
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
        } catch (ex: Exception) { }
        return "Unknown"
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        return paths.any { File(it).exists() }
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = BufferedReader(java.io.InputStreamReader(process.inputStream))
            `in`.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
    
    private fun hasFingerprintSensor(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
    }
    
    private data class LocalBatteryInfo(
        val technology: String,
        val health: String,
        val level: Int,
        val status: String,
        val voltage: Int,
        val temperature: Float,
        val isCharging: Boolean
    )
}
