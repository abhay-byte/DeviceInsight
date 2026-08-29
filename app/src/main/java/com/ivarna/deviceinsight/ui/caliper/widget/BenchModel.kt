package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.ui.caliper.BlueprintColors
import com.ivarna.deviceinsight.ui.caliper.CarbonColors
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.PaperColors
import com.ivarna.deviceinsight.ui.caliper.CaliperColors
import com.ivarna.deviceinsight.ui.caliper.components.CoreReading
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.data.provider.*
import kotlinx.coroutines.flow.first

enum class WidgetKind { SCOPE, STACK, FUEL, RASTER, BENCH }

enum class Tier(val wDp: Int, val hDp: Int) {
    T1(140, 140), T2(280, 140), T3(280, 210), T4(280, 280), T5(350, 280);
    companion object {
        fun of(wDp: Int, hDp: Int): Tier =
            entries.lastOrNull { wDp >= it.wDp && hDp >= it.hDp } ?: T1
    }
}

enum class Cadence { LIVE, AMBIENT, BUDGET }

data class BenchConfig(
    val medium: Medium = Medium.PAPER,
    val followSystem: Boolean = true,
    val cadence: Cadence = Cadence.AMBIENT,
    val traceWindowS: Int = 60,
    val wattHero: Boolean = true,
    val compactChannels: List<String> = listOf("CH-01", "CH-02", "CH-04", "CH-03")
)

data class MemSeg(
    val fraction: Float,
    val pattern: HatchPattern,
    val channelId: String
)

data class Consumer(
    val pkg: String = "",
    val label: String = "",
    val rssMb: Int = 0
)

data class BenchSnapshot(
    val timestamp: Long = 0L,
    val cpuPct: Float = 0f,
    val cpuHist: List<Float> = emptyList(),
    val freqGHz: Float = 0f,
    val tempC: Float = 0f,
    val cores: List<CoreReading> = emptyList(),
    val governor: String? = null,
    val memUsedGb: Float = 0f,
    val memTotalGb: Float = 0f,
    val memComposition: List<MemSeg> = emptyList(),
    val memHist: List<Float> = emptyList(),
    val zramGb: Float = 0f,
    val swapGb: Float = 0f,
    val topConsumers: List<Consumer> = emptyList(),
    val netDown: Long = 0L,
    val netUp: Long = 0L,
    val netHist: List<Float> = emptyList(),
    val batteryPct: Float = 0f,
    val watts: Float = 0f,
    val voltage: Float = 0f,
    val currentMa: Int = 0,
    val remainingMin: Int = 0,
    val charging: Boolean = false,
    val wattHist: List<Float> = emptyList(),
    val stoUsedGb: Float = 0f,
    val stoTotalGb: Float = 0f,
    val gpuPct: Float? = null,
    val gpuMHz: Long? = null,
    val gpuHist: List<Float> = emptyList(),
    val gpuName: String = "",
    val gpuVulkan: String = "",
    val gpuGles: String = "",
    val gpuRootLocked: Boolean = false,
    val gpuFitted: Boolean = false,
    val rootAvailable: Boolean = false,
    val serviceRunning: Boolean = false,
    val batteryPresent: Boolean = true,
    val batteryHealth: String? = null,
    val cycleCount: Int? = null,
    val designMah: Int? = null
) {
    fun warning(): Boolean = tempC > 60f || (batteryPct in 0f..0.2f && !charging && batteryPresent)
    fun stale(cadenceMs: Long): Boolean =
        timestamp == 0L || System.currentTimeMillis() - timestamp > cadenceMs * 2
}

data class WidgetPalette(
    val medium: Medium,
    val panel: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val ink: androidx.compose.ui.graphics.Color,
    val ink60: androidx.compose.ui.graphics.Color,
    val ink40: androidx.compose.ui.graphics.Color,
    val hairline: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val fault: androidx.compose.ui.graphics.Color,
    val gridMinor: androidx.compose.ui.graphics.Color,
    val gridMajor: androidx.compose.ui.graphics.Color,
    val ch01: androidx.compose.ui.graphics.Color,
    val ch02: androidx.compose.ui.graphics.Color,
    val ch03: androidx.compose.ui.graphics.Color,
    val ch04: androidx.compose.ui.graphics.Color,
    val ch05: androidx.compose.ui.graphics.Color,
    val ch06: androidx.compose.ui.graphics.Color
) {
    fun channelFor(id: String): androidx.compose.ui.graphics.Color = when (id) {
        "CH-01" -> ch01
        "CH-02" -> ch02
        "CH-03" -> ch03
        "CH-04" -> ch04
        "CH-05" -> ch05
        "CH-06" -> ch06
        else -> ink
    }
}

object WidgetPalettes {
    private fun fromCaliper(c: CaliperColors, isBlueprint: Boolean): WidgetPalette {
        return WidgetPalette(
            medium = c.medium,
            panel = c.panel,
            surface = c.surface,
            ink = c.ink,
            ink60 = c.ink60,
            ink40 = c.ink40,
            hairline = c.hairline,
            accent = c.accent,
            fault = c.fault,
            gridMinor = c.gridMinor,
            gridMajor = c.gridMajor,
            ch01 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.CPU),
            ch02 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.MEMORY),
            ch03 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.NETWORK),
            ch04 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.POWER),
            ch05 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.STORAGE),
            ch06 = if (isBlueprint) c.ink else c.channel(com.ivarna.deviceinsight.ui.caliper.Channels.GPU)
        )
    }
    val PAPER: WidgetPalette = fromCaliper(PaperColors, false)
    val CARBON: WidgetPalette = fromCaliper(CarbonColors, false)
    val BLUEPRINT: WidgetPalette = fromCaliper(BlueprintColors, true)
    fun of(medium: Medium): WidgetPalette = when (medium) {
        Medium.PAPER -> PAPER
        Medium.CARBON -> CARBON
        Medium.BLUEPRINT -> BLUEPRINT
    }
}

fun DashboardMetrics.toBenchSnapshot(
    serviceRunning: Boolean,
    consumers: List<Consumer> = emptyList(),
    rxBps: Long = 0L,
    txBps: Long = 0L,
    stoUsedBytes: Long = 0L,
    stoTotalBytes: Long = 0L,
    gpuFitted: Boolean = false,
    gpuRootLocked: Boolean = false
): BenchSnapshot {
    val cpuPct = cpuUsage * 100f
    val cpuHist = cpuHistory.map { it.utilization }
    val freqGHz = (cpuCoreFrequencies.maxOrNull()?.toFloat()?.div(1000f)) ?: 0f
    val tempC = cpuTemperature
    val maxFreq = cpuCoreMaxFrequencies.maxOrNull() ?: maxCpuFrequency
    val cores = cpuCoreFrequencies.mapIndexed { i, mhz ->
        val max = cpuCoreMaxFrequencies.getOrNull(i) ?: maxFreq
        val load = if (max > 0) (mhz.toFloat() / max.toFloat() * 100f).coerceIn(0f, 100f) else 0f
        CoreReading(i, load = load, freqKhz = mhz.toLong() * 1000L)
    }
    // ponytail: real per-core load when CpuProvider exposes it — currently freq/max approximation

    val memUsedGb = ramUsedBytes / 1e9f
    val memTotalGb = ramTotalBytes / 1e9f
    val swapGb = swapUsedBytes / 1e9f
    // Corrected fractions: MemTotal denominator for all; de-duplicate ZRAM (not in Dashboard path)
    // Keep fallback to simple used/swap/free when Active/Cached not available; normalize if sum>1
    val usedFraction = if (ramTotalBytes > 0) ramUsedBytes.toFloat() / ramTotalBytes.toFloat() else 0f
    val swapRawF = if (ramTotalBytes > 0 && swapTotalBytes > 0) swapUsedBytes.toFloat() / ramTotalBytes.toFloat() else 0f
    // ZRAM de-duplication: no ZRAM in this path (0), so swapF = swapRawF
    val swapF = swapRawF.coerceIn(0f, 1f)
    var freeF = (1f - usedFraction - swapF).coerceAtLeast(0f)
    var uF = usedFraction.coerceIn(0f, 1f)
    var sF = swapF
    var fF = freeF
    val sum = uF + sF + fF
    if (sum > 1f) {
        val scale = 1f / sum
        uF *= scale; sF *= scale; fF *= scale
    }
    val composition = mutableListOf<MemSeg>()
    composition.add(MemSeg(fraction = uF, pattern = HatchPattern.SOLID, channelId = "CH-02"))
    if (sF > 0.001f) {
        composition.add(MemSeg(fraction = sF, pattern = HatchPattern.CROSS, channelId = "CH-04"))
    }
    // free segment always
    composition.add(MemSeg(fraction = fF, pattern = HatchPattern.NONE, channelId = ""))
    // Note: Active/Cached/ZRAM parsed via MemInfoParser in foreground BUDGET path; this fallback keeps single used segment

    val batteryPct = batteryLevel / 100f
    val watts = powerConsumption
    val voltage = batteryVoltage / 1000f

    val stoUsedGb = if (stoUsedBytes > 0) stoUsedBytes / 1e9f else 0f
    val stoTotalGb = if (stoTotalBytes > 0) stoTotalBytes / 1e9f else 0f

    val gpuPctVal: Float? = if (gpuFitted) gpuUsage * 100f else null
    val gpuMHzVal: Long? = if (gpuFitted) gpuFreqMhz.toLong() else null

    return BenchSnapshot(
        timestamp = System.currentTimeMillis(),
        cpuPct = cpuPct,
        cpuHist = cpuHist,
        freqGHz = freqGHz,
        tempC = tempC,
        cores = cores,
        governor = cpuGovernor,
        memUsedGb = memUsedGb,
        memTotalGb = memTotalGb,
        memComposition = composition,
        memHist = ramHistory.map { it.utilization },
        zramGb = 0f,
        swapGb = swapGb,
        topConsumers = consumers,
        netDown = rxBps,
        netUp = txBps,
        netHist = emptyList(),
        batteryPct = batteryPct,
        watts = watts,
        voltage = voltage,
        currentMa = 0,
        charging = isCharging,
        wattHist = powerHistory.map { it.powerWatts },
        stoUsedGb = stoUsedGb,
        stoTotalGb = stoTotalGb,
        gpuPct = gpuPctVal,
        gpuMHz = gpuMHzVal,
        gpuHist = emptyList(),
        gpuName = gpuModel,
        gpuFitted = gpuFitted,
        gpuRootLocked = gpuRootLocked,
        serviceRunning = serviceRunning,
        batteryPresent = true
    )
}

object BenchSampler {
    fun sample(context: Context): BenchSnapshot {
        return try {
            val cpuUtil = com.ivarna.deviceinsight.utils.CpuUtilizationUtils(context)
            val cpuPct = try { cpuUtil.getCpuUtilizationPercentage() * 100f } catch (_: Exception) { 0f }

            // CpuProvider needs CpuUtilizationUtils + SocMapper — use file-based fallback for sampler
            var freqs: List<Int> = emptyList()
            var maxFreqs: List<Int> = emptyList()
            var governor: String? = null
            try {
                governor = cpuUtil.getCurrentCpuGovernor()
                // read frequencies directly from sysfs without Hilt SocMapper
                // scaling_*_freq is KHz — convert to MHz (provider/DashboardMetrics unit) or
                // downstream MHz/1000=GHz math prints "1785.60 GHz"
                val cores = Runtime.getRuntime().availableProcessors()
                freqs = (0 until cores).mapNotNull { i ->
                    try { java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toIntOrNull()?.div(1000) } catch (_: Exception) { null }
                }
                maxFreqs = (0 until cores).mapNotNull { i ->
                    try { java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toIntOrNull()?.div(1000) } catch (_: Exception) { null }
                }
            } catch (_: Exception) { }
            val maxAll = maxFreqs.maxOrNull() ?: 0
            val coreReadings = freqs.mapIndexed { i, mhz ->
                val max = maxFreqs.getOrNull(i) ?: maxAll
                val load = if (max > 0) (mhz.toFloat() / max.toFloat() * 100f).coerceIn(0f, 100f) else 0f
                CoreReading(i, load = load, freqKhz = mhz.toLong() * 1000L)
            }
            val freqGHz = (freqs.maxOrNull()?.toFloat()?.div(1000f)) ?: 0f
            val tempC = try { ThermalProvider().getCpuTemperature() } catch (_: Exception) { 0f }

            val mem = try { MemoryProvider(context).getMemoryInfo() } catch (_: Exception) { Pair(0L, 0L) }
            val total = mem.first; val avail = mem.second; val used = if (total > avail) total - avail else 0L
            val memUsedGb = used / 1e9f
            val memTotalGb = total / 1e9f
            val swapGb = 0f

            var batteryPct = 0f; var charging = false; var voltage = 0f; var watts = 0f; var batteryPresent = true
            var currentMa = 0
            try {
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (level >= 0) batteryPct = level / 100f
                val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val status = intent?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                batteryPresent = intent?.getBooleanExtra(android.os.BatteryManager.EXTRA_PRESENT, true) ?: true
                val voltMv = intent?.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
                voltage = voltMv / 1000f
                watts = try { PowerProvider(context).getPowerConsumption() } catch (_: Exception) { 0f }
                currentMa = try {
                    val cur = bm.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    (cur / 1000).toInt()
                } catch (_: Exception) { 0 }
            } catch (_: Exception) { }

            var gpuPct: Float? = null; var gpuMHz: Long? = null; var gpuName = ""; var gpuFitted = false; var gpuRootLocked = false
            var gpuVulkan = ""; var gpuGles = ""
            try {
                val gm = GpuUsageProvider(context).getMetrics()
                gpuFitted = gm.sourceLabel != "none"
                gpuRootLocked = !gpuFitted && gm.vendor != GpuUsageProvider.Vendor.UNKNOWN
                if (gpuFitted) {
                    gpuPct = gm.usage * 100f
                    gpuMHz = gm.curFreqMhz.toLong()
                    gpuName = gm.renderer
                } else {
                    gpuName = gm.renderer
                }
            } catch (_: Exception) { }
            // GpuProvider needs GpuMapper; skip detailed info in sampler — use usage provider name
            gpuVulkan = gpuName
            gpuGles = gpuName

            var stoUsedGb = 0f; var stoTotalGb = 0f
            try {
                val sto = StorageProvider(context).getInternalStorageInfo()
                val totalB = sto.first; val freeB = sto.second
                stoTotalGb = totalB / 1e9f
                stoUsedGb = (totalB - freeB) / 1e9f
            } catch (_: Exception) { }

            var rxBps = 0L; var txBps = 0L
            try {
                val t = NetworkTrafficProvider().getTrafficSpeed()
                rxBps = t.rxBps; txBps = t.txBps
            } catch (_: Exception) { }

            BenchSnapshot(
                timestamp = System.currentTimeMillis(),
                cpuPct = cpuPct,
                freqGHz = freqGHz,
                tempC = tempC,
                cores = coreReadings,
                governor = governor,
                memUsedGb = memUsedGb,
                memTotalGb = memTotalGb,
                memComposition = listOf(MemSeg(fraction = if (memTotalGb > 0) memUsedGb / memTotalGb else 0f, pattern = HatchPattern.SOLID, channelId = "CH-02")),
                batteryPct = batteryPct,
                watts = watts,
                voltage = voltage,
                currentMa = currentMa,
                charging = charging,
                stoUsedGb = stoUsedGb,
                stoTotalGb = stoTotalGb,
                gpuPct = gpuPct,
                gpuMHz = gpuMHz,
                gpuName = gpuName,
                gpuVulkan = gpuVulkan,
                gpuGles = gpuGles,
                gpuFitted = gpuFitted,
                gpuRootLocked = gpuRootLocked,
                batteryPresent = batteryPresent,
                zramGb = 0f,
                swapGb = swapGb,
                netDown = rxBps,
                netUp = txBps
            )
        } catch (_: Exception) {
            BenchSnapshot(timestamp = System.currentTimeMillis())
        }
    }
}

object BenchFrames {
    private val lru = object : LruCache<String, Bitmap>(24) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        // Dangerous recycle removed — let GC reclaim; RemoteViews may still hold Bitmap
    }
    fun get(key: String): Bitmap? = synchronized(lru) { lru.get(key) }
    fun put(key: String, bmp: Bitmap) { synchronized(lru) { lru.put(key, bmp) } }
    fun remove(key: String) { synchronized(lru) { lru.remove(key) } }
    fun clear() { synchronized(lru) { lru.evictAll() } }
}

fun List<Float>.contentHash(): Int = fold(0) { acc, v -> 31 * acc + v.hashCode() }

object BenchState {
    private val KEY_MEDIUM = stringPreferencesKey("medium")
    private val KEY_FOLLOW = booleanPreferencesKey("follow")
    private val KEY_CADENCE = stringPreferencesKey("cadence")
    private val KEY_WINDOW = intPreferencesKey("window")
    private val KEY_WATTHERO = booleanPreferencesKey("wattHero")
    private val KEY_COMPACT = stringPreferencesKey("compact")
    private val KEY_PLACED = longPreferencesKey("placedAt")

    suspend fun save(context: Context, id: GlanceId, cfg: BenchConfig) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { p ->
            val mutable = p.toMutablePreferences()
            // placedAt write-once: only set if absent (fix reconfigure restart sweep)
            if (p[KEY_PLACED] == null) {
                mutable[KEY_PLACED] = System.currentTimeMillis()
            }
            mutable[KEY_MEDIUM] = cfg.medium.name
            mutable[KEY_FOLLOW] = cfg.followSystem
            mutable[KEY_CADENCE] = cfg.cadence.name
            mutable[KEY_WINDOW] = cfg.traceWindowS
            mutable[KEY_WATTHERO] = cfg.wattHero
            mutable[KEY_COMPACT] = cfg.compactChannels.joinToString(",")
            mutable
        }
    }

    suspend fun config(context: Context, id: GlanceId): BenchConfig {
        val p = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        return BenchConfig(
            medium = p[KEY_MEDIUM]?.let { runCatching { Medium.valueOf(it) }.getOrNull() } ?: Medium.PAPER,
            followSystem = p[KEY_FOLLOW] ?: true,
            cadence = p[KEY_CADENCE]?.let { runCatching { Cadence.valueOf(it) }.getOrNull() } ?: Cadence.AMBIENT,
            traceWindowS = p[KEY_WINDOW] ?: 60,
            wattHero = p[KEY_WATTHERO] ?: true,
            compactChannels = p[KEY_COMPACT]?.split(",")?.filter { it.startsWith("CH-") }?.takeIf { it.isNotEmpty() }
                ?: listOf("CH-01", "CH-02", "CH-04", "CH-03")
        )
    }

    suspend fun placedAt(context: Context, id: GlanceId): Long {
        val p = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        return p[KEY_PLACED] ?: 0L
    }
}

// Requested cadence remains a model-level compatibility helper. The update engine uses
// effectiveCadenceMs/effectiveCadence so LIVE is only claimed while a real app monitor or
// foreground producer is publishing samples.
fun cadenceMs(cfg: BenchConfig, snap: BenchSnapshot): Long = when (cfg.cadence) {
    Cadence.LIVE -> 1_000L
    Cadence.AMBIENT -> 30_000L
    Cadence.BUDGET -> 15 * 60_000L
}

fun effectiveCadenceMs(cfg: BenchConfig, source: WidgetSnapshotSource): Long = when (cfg.cadence) {
    Cadence.LIVE -> if (source == WidgetSnapshotSource.APP_MONITOR || source == WidgetSnapshotSource.FOREGROUND_SERVICE) {
        1_000L
    } else {
        15 * 60_000L
    }
    Cadence.AMBIENT -> 30_000L
    Cadence.BUDGET -> 15 * 60_000L
}

suspend fun resolvedMedium(context: Context, cfg: BenchConfig): Medium {
    if (!cfg.followSystem) return cfg.medium
    val flowMedium = try { context.mediumFlow.first() } catch (_: Exception) { null }
    if (flowMedium != null) return flowMedium
    val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    return if (night) Medium.CARBON else Medium.PAPER
}
