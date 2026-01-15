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
import com.ivarna.deviceinsight.utils.DisplayRefreshRateUtils
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

class DashboardRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cpuUtilizationUtils: CpuUtilizationUtils,
    private val displayRefreshRateUtils: DisplayRefreshRateUtils
) : DashboardRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _dashboardMetrics = MutableStateFlow<DashboardMetrics?>(null)

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            while (true) {
                try {
                    _dashboardMetrics.emit(collectMetrics())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var lastRxBytes = TrafficStats.getTotalRxBytes()
    private var lastTxBytes = TrafficStats.getTotalTxBytes()
    private var lastNetworkCheckTime = SystemClock.elapsedRealtime()

    private val cpuHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.CpuDataPoint>().apply {
        val now = System.currentTimeMillis()
        for (i in 0 until 61) add(com.ivarna.deviceinsight.domain.model.CpuDataPoint(i.toLong(), now - (60 - i) * 1000, 0f))
    }
    private val cpuCoreHistoryList = ArrayList<java.util.LinkedList<com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint>>()
    private val ramHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.MemoryDataPoint>().apply {
        val now = System.currentTimeMillis()
        for (i in 0 until 61) add(com.ivarna.deviceinsight.domain.model.MemoryDataPoint(i.toLong(), now - (60 - i) * 1000, 0f))
    }
    private val powerHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.PowerDataPoint>().apply {
        val now = System.currentTimeMillis()
        for (i in 0 until 61) add(com.ivarna.deviceinsight.domain.model.PowerDataPoint(i.toLong(), now - (60 - i) * 1000, 0f))
    }
    private val fpsHistory = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.FpsDataPoint>().apply {
        val now = System.currentTimeMillis()
        for (i in 0 until 61) add(com.ivarna.deviceinsight.domain.model.FpsDataPoint(i.toLong(), now - (60 - i) * 1000, 60))
    }
    private val HISTORY_SIZE = 61
    private var maxCpuFreqCache: Int = 0

    private var historyCounter: Long = 61

    override fun getDashboardMetrics(): kotlinx.coroutines.flow.Flow<DashboardMetrics> = _dashboardMetrics.asStateFlow().filterNotNull()

    override fun refreshMetrics() {
        // Handled by internal loop
    }

    private fun collectMetrics(): DashboardMetrics {
        val cpu = cpuUtilizationUtils.getCpuUtilizationPercentage()
        val ram = getRamUsage()
        val ramUsed = getRamUsedBytes()
        val ramTotal = getRamTotalBytes()
        val swapUsed = getSwapUsedBytes()
        val swapTotal = getSwapTotalBytes()
        val power = getPowerConsumption()
        
        // Calculate network speeds
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = SystemClock.elapsedRealtime()

        val timeDiff = currentTime - lastNetworkCheckTime
        val rxSpeed: String
        val txSpeed: String
        val totalSpeed: String
        
        if (timeDiff > 0) {
            val rxDiff = currentRx - lastRxBytes
            val txDiff = currentTx - lastTxBytes
            val rxBps = (rxDiff * 1000) / timeDiff
            val txBps = (txDiff * 1000) / timeDiff
            val totalBps = ((rxDiff + txDiff) * 1000) / timeDiff
            
            rxSpeed = formatFileSize(rxBps) + "/s"
            txSpeed = formatFileSize(txBps) + "/s"
            totalSpeed = formatFileSize(totalBps) + "/s"
            
            // Update state
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastNetworkCheckTime = currentTime
        } else {
            rxSpeed = "0 B/s"
            txSpeed = "0 B/s"
            totalSpeed = "0 B/s"
        }
         
        val now = System.currentTimeMillis()
        val x = historyCounter++
        
        val fps = displayRefreshRateUtils.getRefreshRate()
         
        addToHistory(cpuHistory, com.ivarna.deviceinsight.domain.model.CpuDataPoint(x, now, cpu * 100))
        addToHistory(ramHistory, com.ivarna.deviceinsight.domain.model.MemoryDataPoint(x, now, ram * 100))
        addToHistory(powerHistory, com.ivarna.deviceinsight.domain.model.PowerDataPoint(x, now, power))
        addToHistory(fpsHistory, com.ivarna.deviceinsight.domain.model.FpsDataPoint(x, now, fps))

        val coreFrequencies = getCpuCoreFrequencies()
        while (cpuCoreHistoryList.size < coreFrequencies.size) {
            val list = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint>()
            val now = System.currentTimeMillis()
            for (i in 0 until 61) {
                list.add(com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint(i.toLong(), now - (60 - i) * 1000, 0f))
            }
            cpuCoreHistoryList.add(list)
        }
        coreFrequencies.forEachIndexed { index, freq ->
            addToHistory(cpuCoreHistoryList[index], com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint(x, now, freq.toFloat()))
        }

        if (maxCpuFreqCache == 0) {
            maxCpuFreqCache = getMaxCpuFrequency()
        }

        return DashboardMetrics(
            cpuUsage = cpu,
            ramUsage = ram,
            ramUsedBytes = ramUsed,
            ramTotalBytes = ramTotal,
            swapUsedBytes = swapUsed,
            swapTotalBytes = swapTotal,
            gpuUsage = 0.25f,
            gpuModel = getGpuModel(),
            batteryLevel = getBatteryLevel(),
            batteryStatus = getBatteryStatus(),
            temperature = getBatteryTemperature(),
            cpuTemperature = getCpuTemperature(),
            powerConsumption = power,
            cpuCoreFrequencies = coreFrequencies,
            storageUsedPerc = getStorageUsedPerc(),
            storageFreeGb = getStorageFreeGb(),
            networkSpeed = totalSpeed,
            networkDownloadSpeed = rxSpeed,
            networkUploadSpeed = txSpeed,
            uptime = getUptime(),
            cpuGovernor = cpuUtilizationUtils.getCurrentCpuGovernor(),
            maxCpuFrequency = maxCpuFreqCache,
            cpuHistory = ArrayList(cpuHistory),
            cpuCoreHistory = cpuCoreHistoryList.map { ArrayList(it) },
            ramHistory = ArrayList(ramHistory),
            powerHistory = ArrayList(powerHistory),
            fps = fps,
            fpsHistory = ArrayList(fpsHistory)
        )
    }

    private fun getMaxCpuFrequency(): Int {
        var maxFreq = 0
        try {
            // Check up to 16 cores
            for (i in 0 until 16) {
                // Try cpuinfo_max_freq first (hardware limit)
                var file = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (!file.exists()) {
                    // Fallback to scaling_max_freq (current policy limit)
                    file = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_max_freq")
                }
                
                if (file.exists() && file.canRead()) {
                    val freq = file.readText().trim().toIntOrNull()
                    if (freq != null) {
                        if (freq > maxFreq) maxFreq = freq
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardRepository", "Error reading max CPU freq: ${e.message}")
        }
        // Return in MHz (files are usually in kHz)
        return if (maxFreq > 0) maxFreq / 1000 else 3000 // Default to 3000 MHz if not found
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
        
        val power = voltageV * currentA
        
        // Use battery status to ensure correct sign for polarity
        // +ve for charging, -ve for discharging
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
            kotlin.math.abs(power)
        } else if (status == BatteryManager.BATTERY_STATUS_DISCHARGING) {
            -kotlin.math.abs(power)
        } else {
            power
        }
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

    // --- Swap ---
    private fun getSwapUsedBytes(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotal: Long = 0
            var swapFree: Long = 0
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("SwapTotal:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapTotal = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                } else if (line?.startsWith("SwapFree:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapFree = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                }
            }
            reader.close()
            swapTotal - swapFree
        } catch (e: Exception) {
            0L
        }
    }

    private fun getSwapTotalBytes(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotal: Long = 0
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("SwapTotal:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapTotal = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                    break
                }
            }
            reader.close()
            swapTotal
        } catch (e: Exception) {
            0L
        }
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

    private fun getCpuTemperature(): Float {
        try {
            // Try to read CPU temperature from thermal zones
            // Common paths for CPU thermal zones on Android
            val thermalPaths = listOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp",
                "/sys/devices/virtual/thermal/thermal_zone0/temp",
                "/sys/devices/virtual/thermal/thermal_zone1/temp"
            )
            
            for (path in thermalPaths) {
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    val temp = file.readText().trim().toIntOrNull()
                    if (temp != null) {
                        // Temperature is typically in millidegrees Celsius
                        // Convert to degrees Celsius
                        return if (temp > 1000) {
                            temp / 1000f
                        } else {
                            temp.toFloat()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardRepository", "Error reading CPU temperature: ${e.message}")
        }
        return 0f // Return 0 if unable to read
    }

    private fun getCpuCoreFrequencies(): List<Int> {
        return cpuUtilizationUtils.getAllCoreFrequencies().values.map { 
            (it.first / 1000).toInt() // Convert kHz to MHz
        }
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

    // --- CPU ---
    // Usage is now handled by CpuUtilizationUtils

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
