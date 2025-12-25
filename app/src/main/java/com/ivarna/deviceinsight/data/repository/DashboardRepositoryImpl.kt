package com.ivarna.deviceinsight.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

class DashboardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DashboardRepository {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastNetworkCheckTime = SystemClock.elapsedRealtime()

    private val cpuHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.CpuDataPoint>()
    private val ramHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.MemoryDataPoint>()
    private val powerHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.PowerDataPoint>()
    private val HISTORY_SIZE = 60

    private var historyCounter: Long = 0

    override fun getDashboardMetrics(): Flow<DashboardMetrics> = flow {
        while (true) {
            emit(collectMetrics())
            delay(1000) // Update every second
        }
    }

    override fun refreshMetrics() {
        // No-op for flow-based approach
    }

    private fun collectMetrics(): DashboardMetrics {
        val cpu = getCpuUsage()
        val ram = getRamUsage()
        val ramUsed = getRamUsedBytes()
        val ramTotal = getRamTotalBytes()
        val power = getPowerConsumption()
        
        val now = System.currentTimeMillis()
        val x = historyCounter++
        
        addToHistory(cpuHistory, com.ivarna.deviceinsight.domain.model.CpuDataPoint(x, now, cpu * 100))
        addToHistory(ramHistory, com.ivarna.deviceinsight.domain.model.MemoryDataPoint(x, now, ram * 100))
        addToHistory(powerHistory, com.ivarna.deviceinsight.domain.model.PowerDataPoint(x, now, power))

        return DashboardMetrics(
            cpuUsage = cpu,
            ramUsage = ram,
            ramUsedBytes = ramUsed,
            ramTotalBytes = ramTotal,
            gpuUsage = 0.25f, 
            gpuModel = getGpuModel(),
            batteryLevel = getBatteryLevel(),
            batteryStatus = getBatteryStatus(),
            temperature = getBatteryTemperature(),
            storageUsedPerc = getStorageUsedPerc(),
            storageFreeGb = getStorageFreeGb(),
            networkSpeed = getNetworkSpeed(),
            uptime = getUptime(),
            cpuHistory = ArrayList(cpuHistory),
            ramHistory = ArrayList(ramHistory),
            powerHistory = ArrayList(powerHistory)
        )
    }

    private fun <T> addToHistory(list: java.util.LinkedList<T>, item: T) {
        list.add(item)
        if (list.size > HISTORY_SIZE) {
            list.removeFirst()
        }
    }

    private fun getPowerConsumption(): Float {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        // Current in microAmperes
        val currentNow = manager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        // Voltage via Intent (millivolts)
        val intent = getBatteryIntent()
        val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        
        // Power (W) = (Voltage_V) * (Current_A)
        // Voltage_V = voltageMv / 1000
        // Current_A = currentNow / 1,000,000
        
        // Note: currentNow can be negative (discharging) or positive (charging) on some devices,
        // or always positive with a status check. 
        // We will take absolute value for magnitude, or keep sign for graph (negative=discharge usually).
        // Actually, let's keep it simple: Watts magnitude.
        
        if (voltageMv == 0) return 0f
        
        val voltageV = voltageMv / 1000f
        val currentA = currentNow / 1_000_000f
        
        return kotlin.math.abs(voltageV * currentA)
    }

    // --- RAM ---
    private fun getRamInfo(): ActivityManager.MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    private fun getRamUsage(): Float {
        val info = getRamInfo()
        return (info.totalMem - info.availMem).toFloat() / info.totalMem.toFloat()
    }

    private fun getRamUsedBytes(): Long {
        val info = getRamInfo()
        return info.totalMem - info.availMem
    }

    private fun getRamTotalBytes(): Long {
        return getRamInfo().totalMem
    }

    // --- Battery ---
    private fun getBatteryIntent(): Intent? {
        return context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun getBatteryLevel(): Int {
        val intent = getBatteryIntent() ?: return 0
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level != -1 && scale != -1) {
            (level * 100 / scale.toFloat()).toInt()
        } else {
            0
        }
    }

    private fun getBatteryStatus(): String {
        val intent = getBatteryIntent() ?: return "Unknown"
        return when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    private fun getBatteryTemperature(): Float {
        val intent = getBatteryIntent() ?: return 0f
        val tempInt = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        return tempInt / 10f // Battery temp is in tenths of a degree Celsius
    }

    // --- Storage ---
    private fun getStorageUsedPerc(): Float {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val total = totalBlocks * blockSize
        val free = availableBlocks * blockSize
        return (total - free).toFloat() / total.toFloat()
    }

    private fun getStorageFreeGb(): String {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val freeBytes = availableBlocks * blockSize
        val freeGb = freeBytes.toDouble() / (1024 * 1024 * 1024)
        return String.format("%.1f GB Free", freeGb)
    }

    // --- Network ---
    private fun getNetworkSpeed(): String {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = SystemClock.elapsedRealtime()

        val timeDiff = currentTime - lastNetworkCheckTime
        if (timeDiff == 0L) return "0 KB/s"

        val rxDiff = currentRx - lastRxBytes
        val txDiff = currentTx - lastTxBytes
        
        // Update state
        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastNetworkCheckTime = currentTime

        val totalBytes = rxDiff + txDiff
        val speedBps = (totalBytes * 1000) / timeDiff // bytes per second

        return formatFileSize(speedBps) + "/s"
    }
    
    // --- CPU (Placeholder logic for now) ---
    private fun getCpuUsage(): Float {
        // On recent Android versions, /proc/stat is restricted.
        // We will fallback to a simulated fluctuation for the UI demonstration 
        // until we implement a more complex workaround or root method.
        // This simulates a "breathing" CPU load between 10% and 40%
        val time = System.currentTimeMillis()
        val baseLoad = 0.25f 
        val fluctuation = Math.sin(time / 2000.0).toFloat() * 0.15f
        return (baseLoad + fluctuation).coerceIn(0.05f, 1.0f)
    }

    // --- GPU (Placeholder) ---
    private fun getGpuModel(): String {
        return "Adreno GPU" // This actually needs GL context to fetch accurately, will do later
    }

    // --- Uptime ---
    private fun getUptime(): String {
        val uptimeMillis = SystemClock.elapsedRealtime()
        val hours = TimeUnit.MILLISECONDS.toHours(uptimeMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMillis) % 60
        return String.format("%dh %dm", hours, minutes)
    }

    private fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
