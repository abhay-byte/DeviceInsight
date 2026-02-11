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
import com.ivarna.deviceinsight.utils.FormattingUtils
import com.ivarna.deviceinsight.data.fps.FpsMonitor
import com.ivarna.deviceinsight.data.provider.*
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
    private val displayRefreshRateUtils: DisplayRefreshRateUtils,
    private val fpsMonitor: FpsMonitor,
    private val networkTrafficProvider: NetworkTrafficProvider,
    private val batteryProvider: BatteryProvider,
    private val memoryProvider: MemoryProvider,
    private val storageProvider: StorageProvider,
    private val deviceProvider: DeviceProvider,
    private val powerProvider: PowerProvider,
    private val thermalProvider: ThermalProvider,
    private val cpuProvider: CpuProvider
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
        val memInfo = memoryProvider.getMemoryInfo()
        val ramTotal = memInfo.first
        val ramAvailable = memInfo.second
        val ramUsed = ramTotal - ramAvailable
        val ramUsage = ramUsed.toFloat() / ramTotal.toFloat()
        
        val swapInfo = getSwapInfo()
        val swapUsed = swapInfo.first
        val swapTotal = swapInfo.second
        
        val power = powerProvider.getPowerConsumption()
        val traffic = networkTrafficProvider.getTrafficSpeed()
        val rxSpeed = FormattingUtils.formatFileSize(traffic.rxBps) + "/s"
        val txSpeed = FormattingUtils.formatFileSize(traffic.txBps) + "/s"
        val totalSpeed = FormattingUtils.formatFileSize(traffic.totalBps) + "/s"
         
        val now = System.currentTimeMillis()
        val x = historyCounter++
        
        var fps = fpsMonitor.getCurrentFps()
        if (fps <= 0) {
            fps = displayRefreshRateUtils.getRefreshRate()
        }
         
        addToHistory(cpuHistory, com.ivarna.deviceinsight.domain.model.CpuDataPoint(x, now, cpu * 100))
        addToHistory(ramHistory, com.ivarna.deviceinsight.domain.model.MemoryDataPoint(x, now, ramUsage * 100))
        addToHistory(powerHistory, com.ivarna.deviceinsight.domain.model.PowerDataPoint(x, now, power))
        addToHistory(fpsHistory, com.ivarna.deviceinsight.domain.model.FpsDataPoint(x, now, fps))

        val coreFrequencies = cpuProvider.getCpuCoreFrequencies()
        while (cpuCoreHistoryList.size < coreFrequencies.size) {
            val list = java.util.LinkedList<com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint>()
            val innerNow = System.currentTimeMillis()
            for (i in 0 until 61) {
                list.add(com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint(i.toLong(), innerNow - (60 - i) * 1000, 0f))
            }
            cpuCoreHistoryList.add(list)
        }
        coreFrequencies.forEachIndexed { index, freq ->
            addToHistory(cpuCoreHistoryList[index], com.ivarna.deviceinsight.domain.model.CpuCoreDataPoint(x, now, freq.toFloat()))
        }

        if (maxCpuFreqCache == 0) {
            maxCpuFreqCache = cpuProvider.getMaxCpuFrequency()
        }

        val batteryInfo = batteryProvider.getBatteryInfo()
        val storageInfo = storageProvider.getInternalStorageInfo()
        val storageUsedPerc = (storageInfo.first - storageInfo.second).toFloat() / storageInfo.first.toFloat()

        return DashboardMetrics(
            cpuUsage = cpu,
            ramUsage = ramUsage,
            ramUsedBytes = ramUsed,
            ramTotalBytes = ramTotal,
            swapUsedBytes = swapUsed,
            swapTotalBytes = swapTotal,
            gpuUsage = 0.25f,
            gpuModel = "Adreno GPU",
            batteryLevel = batteryInfo.level,
            batteryStatus = batteryInfo.status,
            temperature = batteryInfo.temperature,
            cpuTemperature = thermalProvider.getCpuTemperature(),
            powerConsumption = power,
            cpuCoreFrequencies = coreFrequencies,
            storageUsedPerc = storageUsedPerc,
            storageFreeGb = FormattingUtils.formatFileSize(storageInfo.second) + " Free",
            networkSpeed = totalSpeed,
            networkDownloadSpeed = rxSpeed,
            networkUploadSpeed = txSpeed,
            uptime = deviceProvider.getUpTime(),
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

    private fun getSwapInfo(): Pair<Long, Long> {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotal: Long = 0
            var swapFree: Long = 0
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("SwapTotal:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) swapTotal = parts[1].toLong() * 1024
                } else if (line?.startsWith("SwapFree:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) swapFree = parts[1].toLong() * 1024
                }
            }
            reader.close()
            Pair(swapTotal - swapFree, swapTotal)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    private fun <T> addToHistory(list: java.util.LinkedList<T>, item: T) {
        list.add(item)
        if (list.size > HISTORY_SIZE) {
            list.removeFirst()
        }
    }
}
