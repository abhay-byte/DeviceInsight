package com.ivarna.deviceinsight.presentation.overlay

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow
import com.ivarna.deviceinsight.data.monitor.MonitorBus
import com.ivarna.deviceinsight.service.OverlayService
import com.ivarna.deviceinsight.ui.caliper.caliperDataStore
import com.ivarna.deviceinsight.ui.caliper.hud.HudConfig
import com.ivarna.deviceinsight.ui.caliper.hud.HudMedium
import com.ivarna.deviceinsight.ui.caliper.hud.HudModule
import com.ivarna.deviceinsight.ui.caliper.hud.HudScale
import com.ivarna.deviceinsight.ui.caliper.hud.hudMediumFromString
import com.ivarna.deviceinsight.ui.caliper.CaliperKeys
import com.ivarna.deviceinsight.ui.caliper.setHudBlur
import com.ivarna.deviceinsight.ui.caliper.setHudLocked
import com.ivarna.deviceinsight.ui.caliper.setHudMedium
import com.ivarna.deviceinsight.ui.caliper.setHudModules
import com.ivarna.deviceinsight.ui.caliper.setHudOpacity
import com.ivarna.deviceinsight.ui.caliper.setHudScale
import com.ivarna.deviceinsight.ui.caliper.setHudShowCoreBank
import com.ivarna.deviceinsight.ui.caliper.setHudX
import com.ivarna.deviceinsight.ui.caliper.setHudY
import com.ivarna.deviceinsight.ui.caliper.setFpsMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

enum class FpsMode { AUTO, ROOT, SHIZUKU }

data class OverlayPermissions(
    val hasOverlay: Boolean = false,
    val hasUsageStats: Boolean = false,
    val hasShizukuInstalled: Boolean = false,
    val hasShizukuPermission: Boolean = false,
    val hasRoot: Boolean = false
)

data class OverlayUiState(
    val permissions: OverlayPermissions = OverlayPermissions(),
    val fpsMode: FpsMode = FpsMode.AUTO,
    val config: HudConfig = HudConfig(),
    val isServiceRunning: Boolean = false
)

/** HUD position reset default (matches CaliperPrefs hudX/hudY fallbacks). */
private const val DEFAULT_X = 100
private const val DEFAULT_Y = 100

@HiltViewModel
class OverlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securityProvider: com.ivarna.deviceinsight.data.provider.SecurityProvider,
    private val monitorBus: MonitorBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverlayUiState())
    val uiState: StateFlow<OverlayUiState> = _uiState.asStateFlow()

    /** Live feeds — collected by the sheet preview only while the probe window runs. */
    val hudSlow: StateFlow<HudSlow> = monitorBus.slow
    val hudFast: StateFlow<HudFast> = monitorBus.fast

    init {
        viewModelScope.launch(Dispatchers.IO) { loadInitialState() }
        checkPermissions()
    }

    fun refreshPermissions() = checkPermissions()

    private suspend fun loadInitialState() {
        val data = try { context.caliperDataStore.data.first() } catch (_: Exception) { null } ?: return
        val modulesCsv = data[CaliperKeys.hudModules] ?: "FPS,CPU,MEMORY,POWER"
        val cfg = HudConfig(
            medium = hudMediumFromString(data[CaliperKeys.hudMedium]),
            scale = runCatching { HudScale.valueOf(data[CaliperKeys.hudScale] ?: "M") }.getOrDefault(HudScale.M),
            opacity = (data[CaliperKeys.hudOpacity] ?: 0.75f).coerceIn(0.4f, 0.9f),
            blurBehind = data[CaliperKeys.hudBlur] ?: true,
            locked = data[CaliperKeys.hudLocked] ?: false,
            modules = HudConfig.fromCsv(modulesCsv),
            showCoreBank = data[CaliperKeys.hudShowCoreBank] ?: true
        )
        _uiState.value = _uiState.value.copy(
            config = cfg,
            fpsMode = runCatching { FpsMode.valueOf(data[CaliperKeys.fpsMode] ?: "AUTO") }.getOrDefault(FpsMode.AUTO)
        )
    }

    // ─────────────── HUD config setters (caliper DataStore single source) ───────────────

    fun setHudMedium(medium: HudMedium) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(medium = medium))
        persist { context.setHudMedium(medium.name) }
    }

    fun setHudScale(scale: HudScale) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(scale = scale))
        persist { context.setHudScale(scale.name) }
    }

    fun setHudOpacity(opacity: Float) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(opacity = opacity.coerceIn(0.4f, 0.9f)))
        persist { context.setHudOpacity(opacity) }
    }

    fun setBlurBehind(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(blurBehind = enabled))
        persist { context.setHudBlur(enabled) }
    }

    fun setLocked(locked: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(locked = locked))
        persist { context.setHudLocked(locked) }
    }

    fun toggleModule(module: HudModule, enabled: Boolean) {
        val current = _uiState.value.config.modules
        val next = if (enabled) current + module else current - module
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(modules = next))
        persist { context.setHudModules(next.joinToString(",") { it.name }) }
    }

    fun setShowCoreBank(show: Boolean) {
        _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(showCoreBank = show))
        persist { context.setHudShowCoreBank(show) }
    }

    fun resetPosition() {
        persist { context.setHudX(DEFAULT_X); context.setHudY(DEFAULT_Y) }
    }

    fun setFpsMode(mode: FpsMode) {
        _uiState.value = _uiState.value.copy(fpsMode = mode)
        persist { context.setFpsMode(mode.name) }
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { block() } }
    }

    // ─────────────── service ───────────────

    /** Empty intent — config comes from the caliper store, not extras (process-death safe). */
    fun buildServiceIntent(): Intent = Intent(context, OverlayService::class.java)

    fun setServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceRunning = running)
    }

    // ─────────────── permissions ───────────────

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

    fun requestShizukuPermission() {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(0)
            }
        } catch (_: Exception) {}
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
