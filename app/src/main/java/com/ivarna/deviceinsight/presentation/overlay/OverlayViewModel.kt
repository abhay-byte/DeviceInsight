package com.ivarna.deviceinsight.presentation.overlay

import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import javax.inject.Inject

enum class FpsMode { AUTO, ROOT, SHIZUKU }

data class OverlayMetricItem(
    val id: String,
    val name: String,
    val category: String,
    val icon: String,
    val enabled: Boolean,
    val order: Int
)

data class OverlayPermissions(
    val hasOverlay: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasShizukuInstalled: Boolean = false,
    val hasShizukuPermission: Boolean = false,
    val hasRoot: Boolean = false
)

data class OverlayUiState(
    val permissions: OverlayPermissions = OverlayPermissions(),
    val metrics: List<OverlayMetricItem> = emptyList(),
    val fpsMode: FpsMode = FpsMode.AUTO,
    val scaleFactor: Float = 1.0f,
    val isHorizontal: Boolean = false,
    val isServiceRunning: Boolean = false
)

@HiltViewModel
class OverlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityProvider: com.ivarna.deviceinsight.data.provider.SecurityProvider
) : ViewModel() {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
        checkPermissions()
    }

    fun refreshPermissions() = checkPermissions()

    private fun loadInitialState() {
        val defaultOrder = METRIC_DEFINITIONS
        val savedOrderStr = prefs.getString("metricOrder", null)
        val savedOrder = savedOrderStr?.split(",") ?: defaultOrder.map { it.id }
        val finalOrder = savedOrder.toMutableList().apply {
            defaultOrder.forEach { def -> if (!contains(def.id)) add(def.id) }
        }

        val metrics = finalOrder.mapIndexed { index, id ->
            val def = defaultOrder.firstOrNull { it.id == id } ?: defaultOrder.first()
            val prefKey = "show" + id.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            OverlayMetricItem(
                id = def.id,
                name = def.name,
                category = def.category,
                icon = def.icon,
                enabled = prefs.getBoolean(prefKey, true),
                order = index
            )
        }

        _uiState.value = _uiState.value.copy(
            metrics = metrics,
            fpsMode = runCatching { FpsMode.valueOf(prefs.getString("fps_mode", "AUTO") ?: "AUTO") }.getOrDefault(FpsMode.AUTO),
            scaleFactor = prefs.getFloat("scaleFactor", 1.0f),
            isHorizontal = prefs.getBoolean("isHorizontal", false)
        )
    }

    private fun checkPermissions() {
        viewModelScope.launch(Dispatchers.IO) {
            val hasOverlay = Settings.canDrawOverlays(context)
            val hasUsage = hasUsageStatsPermission(context)
            var shizukuInstalled = false
            var shizukuGranted = false
            try {
                shizukuInstalled = Shizuku.pingBinder()
                if (shizukuInstalled) {
                    shizukuGranted = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            } catch (_: Exception) {
                shizukuInstalled = false
            }
            val root = securityProvider.isRooted()
            _uiState.value = _uiState.value.copy(
                permissions = OverlayPermissions(
                    hasOverlay = hasOverlay,
                    hasUsageStats = hasUsage,
                    hasShizukuInstalled = shizukuInstalled,
                    hasShizukuPermission = shizukuGranted,
                    hasRoot = root
                )
            )
        }
    }

    fun toggleMetric(metricId: String, enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            metrics = _uiState.value.metrics.map {
                if (it.id == metricId) it.copy(enabled = enabled) else it
            }
        )
        savePreferences()
    }

    fun reorderMetrics(from: Int, to: Int) {
        val current = _uiState.value.metrics.sortedBy { it.order }.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _uiState.value = _uiState.value.copy(
            metrics = current.mapIndexed { i, m -> m.copy(order = i) }
        )
        savePreferences()
    }

    fun setFpsMode(mode: FpsMode) {
        _uiState.value = _uiState.value.copy(fpsMode = mode)
        savePreferences()
    }

    fun setScaleFactor(scale: Float) {
        _uiState.value = _uiState.value.copy(scaleFactor = scale)
    }

    fun commitScaleFactor() {
        savePreferences()
    }

    fun setHorizontal(horizontal: Boolean) {
        _uiState.value = _uiState.value.copy(isHorizontal = horizontal)
        savePreferences()
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = running)
    }

    private fun savePreferences() {
        val state = _uiState.value
        prefs.edit().apply {
            state.metrics.forEach { m ->
                val prefKey = "show" + m.id.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                putBoolean(prefKey, m.enabled)
            }
            putFloat("scaleFactor", state.scaleFactor)
            putBoolean("isHorizontal", state.isHorizontal)
            putString("fps_mode", state.fpsMode.name)
            putString("metricOrder", state.metrics.sortedBy { it.order }.joinToString(",") { it.id })
            apply()
        }
    }

    fun buildServiceIntent(): android.content.Intent {
        val state = _uiState.value
        savePreferences()
        return android.content.Intent(context, com.ivarna.deviceinsight.service.OverlayService::class.java).apply {
            state.metrics.forEach { m ->
                val prefKey = "show" + m.id.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
                putExtra(prefKey, m.enabled)
            }
            putExtra("scaleFactor", state.scaleFactor)
            putExtra("isHorizontal", state.isHorizontal)
            putExtra("metricOrder", state.metrics.sortedBy { it.order }.joinToString(",") { it.id })
        }
    }

    fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(0)
            }
        } catch (_: Exception) {}
    }

    companion object {
        data class MetricDef(val id: String, val name: String, val category: String, val icon: String)

        val METRIC_DEFINITIONS = listOf(
            MetricDef("time", "System Time", "System", "schedule"),
            MetricDef("cpu", "CPU Usage", "Performance", "memory"),
            MetricDef("cpuGraph", "CPU Graph", "Performance", "show_chart"),
            MetricDef("cpuTemp", "CPU Temperature", "Thermal", "thermostat"),
            MetricDef("cpuFreq", "CPU Frequency", "Performance", "speed"),
            MetricDef("ram", "RAM Usage", "Memory", "storage"),
            MetricDef("swap", "Swap Usage", "Memory", "swap_horiz"),
            MetricDef("power", "Power Draw", "Power", "bolt"),
            MetricDef("powerGraph", "Power Graph", "Power", "trending_up"),
            MetricDef("battery", "Battery Level", "Power", "battery_full"),
            MetricDef("batteryTemp", "Battery Temperature", "Thermal", "device_thermostat"),
            MetricDef("fps", "FPS Monitor", "Display", "videogame_asset"),
            MetricDef("fpsGraph", "FPS History", "Display", "analytics"),
            MetricDef("network", "Network Speed", "Network", "network_check"),
            MetricDef("currentApp", "Current App", "System", "apps")
        )
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
