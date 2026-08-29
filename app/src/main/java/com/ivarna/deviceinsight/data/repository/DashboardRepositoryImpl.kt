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
import com.ivarna.deviceinsight.data.monitor.CoreStat
import com.ivarna.deviceinsight.data.monitor.HudSlow
import com.ivarna.deviceinsight.data.monitor.MemInfoParser
import com.ivarna.deviceinsight.data.monitor.MonitorBus
import com.ivarna.deviceinsight.data.monitor.TopConsumersProvider
import com.ivarna.deviceinsight.service.OverlayService
import com.ivarna.deviceinsight.ui.caliper.widget.BenchUpdater
import com.ivarna.deviceinsight.ui.caliper.widget.WidgetSnapshotSource
import com.ivarna.deviceinsight.ui.caliper.widget.toBenchSnapshot
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
    private val cpuProvider: CpuProvider,
    private val gpuUsageProvider: GpuUsageProvider,
    private val gpuMapper: com.ivarna.deviceinsight.data.mapper.GpuMapper,
    private val monitorBus: MonitorBus,
    private val topConsumersProvider: TopConsumersProvider
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
                kotlinx.coroutines.delay(500)
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
    private val gpuHistory = java.util.LinkedList<Float>().apply {
        for (i in 0 until 61) add(0f)
    }
    private val netHistory = java.util.LinkedList<Float>().apply {
        for (i in 0 until 61) add(0f)
    }
    private val netUpHistory = java.util.LinkedList<Float>().apply {
        for (i in 0 until 61) add(0f)
    }
    private val HISTORY_SIZE = 61
    private var maxCpuFreqCache: Int = 0

    private var historyCounter: Long = 61

    override fun getDashboardMetrics(): kotlinx.coroutines.flow.Flow<DashboardMetrics> = _dashboardMetrics.asStateFlow().filterNotNull()

    override fun refreshMetrics() {
        // Handled by internal loop
    }

    private suspend fun collectMetrics(): DashboardMetrics {
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
        
        // Do not fabricate FPS as display refresh — honest no-signal is better than fake 60
        val fps = fpsMonitor.getCurrentFps()
         
        addToHistory(cpuHistory, com.ivarna.deviceinsight.domain.model.CpuDataPoint(x, now, cpu * 100))
        addToHistory(ramHistory, com.ivarna.deviceinsight.domain.model.MemoryDataPoint(x, now, ramUsage * 100))
        addToHistory(powerHistory, com.ivarna.deviceinsight.domain.model.PowerDataPoint(x, now, power))
        addToHistory(fpsHistory, com.ivarna.deviceinsight.domain.model.FpsDataPoint(x, now, fps))

        val coreFrequencies = cpuProvider.getCpuCoreFrequencies()
        val coreMaxFrequencies = cpuProvider.getCpuCoreMaxFrequencies()
        val cpuClockRange = cpuProvider.getCpuClockRange()
        val cpuArch = cpuProvider.getCpuArchitecture()
        val totalCores = if (coreFrequencies.isNotEmpty()) coreFrequencies.size else Runtime.getRuntime().availableProcessors()

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
        val storageUsedPerc = if (storageInfo.first > 0) (storageInfo.first - storageInfo.second).toFloat() / storageInfo.first.toFloat() else 0f
        val storageFreeFormatted = FormattingUtils.formatFileSize(storageInfo.second)
        val storageTotalFormatted = FormattingUtils.formatFileSize(storageInfo.first)
        val storageUsedFormatted = FormattingUtils.formatFileSize(storageInfo.first - storageInfo.second)

        val gpuMetrics = gpuUsageProvider.getMetrics()
        val socTarget = "${android.os.Build.HARDWARE} ${cpuProvider.getSocModel()} ${android.os.Build.BOARD} ${if (android.os.Build.VERSION.SDK_INT >= 31) android.os.Build.SOC_MODEL else ""}"
        val gpuInfo = gpuMapper.mapHardwareToGpuInfo(socTarget)
        val gpuModel = if (gpuMetrics.renderer.isNotBlank() && !gpuMetrics.renderer.contains("Unknown", ignoreCase = true)) {
            gpuMetrics.renderer
        } else {
            gpuInfo.renderer
        }
        var gpuCores = gpuInfo.cores
        if (gpuCores == 0) {
            val mcMatch = Regex("MC(\\d+)|MP(\\d+)", RegexOption.IGNORE_CASE).find(gpuModel)
            if (mcMatch != null) {
                val group1 = mcMatch.groupValues[1]
                val group2 = mcMatch.groupValues[2]
                gpuCores = (if (group1.isNotBlank()) group1 else group2).toIntOrNull() ?: 0
            }
        }
        val gpuMin = gpuInfo.baseFreqMhz
        val gpuMax = if (gpuMetrics.maxFreqMhz > 0) gpuMetrics.maxFreqMhz else gpuInfo.maxFreqMhz
        val gpuCur = if (gpuMetrics.curFreqMhz > 0) {
            gpuMetrics.curFreqMhz
        } else if (gpuMax > 0) {
            (gpuMin + (gpuMax - gpuMin) * (gpuMetrics.usage.coerceIn(0.05f, 1f))).toInt()
        } else 0
        val screenHz = displayRefreshRateUtils.getRefreshRate()

        addToHistory(gpuHistory, gpuMetrics.usage * 100f)
        addToHistory(netHistory, traffic.rxBps.toFloat())
        addToHistory(netUpHistory, traffic.txBps.toFloat())

        val metrics = DashboardMetrics(
            cpuUsage = cpu,
            ramUsage = ramUsage,
            ramUsedBytes = ramUsed,
            ramTotalBytes = ramTotal,
            swapUsedBytes = swapUsed,
            swapTotalBytes = swapTotal,
            gpuUsage = gpuMetrics.usage,
            gpuModel = gpuModel,
            gpuTemp = gpuMetrics.temperatureC,
            gpuFreqMhz = gpuCur,
            gpuMaxFreqMhz = gpuMax,
            gpuMinFreqMhz = gpuMin,
            gpuCores = gpuCores,
            gpuVendor = if (gpuMetrics.vendor.name != "UNKNOWN") gpuMetrics.vendor.name else gpuInfo.vendor,
            batteryLevel = batteryInfo.level,
            batteryStatus = batteryInfo.status,
            batteryVoltage = batteryInfo.voltage,
            batteryHealth = batteryInfo.health,
            isCharging = batteryInfo.isCharging,
            temperature = batteryInfo.temperature,
            cpuTemperature = thermalProvider.getCpuTemperature(),
            powerConsumption = power,
            cpuCoreFrequencies = coreFrequencies,
            cpuCoreMaxFrequencies = coreMaxFrequencies,
            cpuClockRange = cpuClockRange,
            cpuArchitecture = cpuArch,
            cpuTotalCores = totalCores,
            storageUsedPerc = storageUsedPerc,
            storageFreeGb = storageFreeFormatted + " Free",
            storageTotalGb = storageTotalFormatted,
            storageUsedGb = storageUsedFormatted,
            networkSpeed = totalSpeed,
            networkDownloadSpeed = rxSpeed,
            networkUploadSpeed = txSpeed,
            uptime = deviceProvider.getUpTime(),
            cpuGovernor = cpuUtilizationUtils.getCurrentCpuGovernor(),
            maxCpuFrequency = maxCpuFreqCache,
            screenRefreshRate = screenHz,
            cpuHistory = ArrayList(cpuHistory),
            cpuCoreHistory = cpuCoreHistoryList.map { ArrayList(it) },
            ramHistory = ArrayList(ramHistory),
            powerHistory = ArrayList(powerHistory),
            fps = fps,
            fpsHistory = ArrayList(fpsHistory),
            gpuHistory = ArrayList(gpuHistory),
            netHistory = ArrayList(netHistory),
            netUpHistory = ArrayList(netUpHistory)
        )
        // Foreground single writer → MonitorBus (plan 0.1). BUDGET path never writes MonitorBus.
        try {
            val stoTotalBytes = storageInfo.first
            val stoUsedBytes = if (storageInfo.first > storageInfo.second) storageInfo.first - storageInfo.second else 0L
            val gpuFitted = gpuMetrics.sourceLabel != "none"
            val gpuLocked = !gpuFitted && gpuMetrics.vendor != GpuUsageProvider.Vendor.UNKNOWN

            // Enrich battery fields for toBenchSnapshot (0.6)
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryPresent = batteryIntent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true
            val currentMa = try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                (bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000).toInt()
            } catch (_: Exception) { 0 }
            val remainingMin = try {
                if (batteryInfo.isCharging) {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val ms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) bm.computeChargeTimeRemaining() else -1L
                    if (ms > 0) (ms / 60000).toInt() else 0
                } else 0
            } catch (_: Exception) { 0 }

            // Battery T4 details — never fake 835; null when sentinel -1
            val batteryDetailed = try { batteryProvider.getBatteryDetailedInfo() } catch (_: Exception) { null }
            val cycleCount: Int? = batteryDetailed?.chargingCycles?.let { if (it >= 0) it else null }
            val designMah: Int? = batteryDetailed?.capacity?.let { capStr ->
                // capStr like "4500 mAh" or "Unknown"
                try { capStr.split(" ").firstOrNull()?.toIntOrNull() } catch (_: Exception) { null }
            }?.takeIf { it > 0 }
            val batteryHealthStr: String? = batteryInfo.health.takeIf { it.isNotBlank() && it != "Unknown" }

            // Mem composition via parser (0.4) — foreground vs BUDGET both use same math
            val (memComposition, zramGbFromParser) = try {
                val mi = MemInfoParser.readMeminfoString()
                val zr = MemInfoParser.readZramBytes()
                val segs = if (mi != null) MemInfoParser.parse(mi, zr).segs else emptyList()
                val zGb = if (zr != null && zr > 0) zr.toFloat() / 1e9f else 0f
                Pair(segs, zGb)
            } catch (_: Exception) { Pair(emptyList(), 0f) }

            // Top consumers via permission-gated TaskRepository (0.9)
            val consumers = try { topConsumersProvider.loadTopConsumers(5) } catch (_: Exception) { emptyList() }

            val baseSnap = metrics.toBenchSnapshot(
                serviceRunning = OverlayService.isRunning.get(),
                rxBps = traffic.rxBps,
                txBps = traffic.txBps,
                stoUsedBytes = stoUsedBytes,
                stoTotalBytes = stoTotalBytes,
                gpuFitted = gpuFitted,
                gpuRootLocked = gpuLocked
            )
            var snap = baseSnap.copy(
                currentMa = currentMa,
                remainingMin = remainingMin,
                batteryPresent = batteryPresent,
                batteryHealth = batteryHealthStr,
                cycleCount = cycleCount,
                designMah = designMah,
                gpuHist = ArrayList(gpuHistory),
                netHist = ArrayList(netHistory),
                memComposition = if (memComposition.isNotEmpty()) memComposition else baseSnap.memComposition,
                zramGb = if (zramGbFromParser > 0) zramGbFromParser else baseSnap.zramGb,
                topConsumers = consumers
            )
            // Handle watts >20 anomaly (PowerProvider unit confusion): if abs(watts)>20 treat as mA conversion? keep as is
            // Build HudSlow from same source
            val cores = snap.cores.map { CoreStat(it.id, it.load, (it.freqKhz / 1000).toInt()) }
            val clusterSizes = computeClusterSizes(coreMaxFreqs = coreMaxFrequencies)
            val hudSlow = HudSlow(
                cpuPct = snap.cpuPct,
                cores = cores,
                clusterSizes = clusterSizes,
                governor = snap.governor,
                tempC = snap.tempC,
                memUsedGb = snap.memUsedGb,
                memTotalGb = snap.memTotalGb,
                swapUsedGb = snap.swapGb,
                swapTotalMb = swapTotal / (1024 * 1024),
                zramGb = snap.zramGb,
                netDown = snap.netDown,
                netUp = snap.netUp,
                batteryPct = snap.batteryPct,
                watts = snap.watts,
                voltage = snap.voltage,
                currentMa = snap.currentMa,
                remainingMin = snap.remainingMin,
                charging = snap.charging,
                stoUsedGb = snap.stoUsedGb,
                stoTotalGb = snap.stoTotalGb,
                gpuPct = snap.gpuPct,
                gpuMHz = snap.gpuMHz,
                gpuName = snap.gpuName,
                gpuVulkan = snap.gpuVulkan,
                gpuGles = snap.gpuGles,
                gpuRootLocked = snap.gpuRootLocked,
                gpuFitted = snap.gpuFitted,
                timestamp = snap.timestamp
            )
            monitorBus.pushSlow(snap, hudSlow)
            // Publish the exact sample that fed the dashboard. Widgets must not re-read
            // process globals or start a second hardware sampler for the same tick.
            BenchUpdater.publish(context, snap, WidgetSnapshotSource.APP_MONITOR)
        } catch (_: Exception) { }
        return metrics
    }

    private fun computeClusterSizes(coreMaxFreqs: List<Int>): List<Int> {
        if (coreMaxFreqs.isEmpty()) return emptyList()
        val clusters = mutableListOf<Int>()
        var currentFreq = coreMaxFreqs.firstOrNull() ?: return listOf(coreMaxFreqs.size)
        var count = 0
        for (f in coreMaxFreqs) {
            if (f == currentFreq) count++ else {
                clusters.add(count)
                currentFreq = f
                count = 1
            }
        }
        clusters.add(count)
        return if (clusters.isEmpty()) listOf(coreMaxFreqs.size) else clusters
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
