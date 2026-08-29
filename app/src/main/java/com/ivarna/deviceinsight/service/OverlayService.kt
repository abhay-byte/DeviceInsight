package com.ivarna.deviceinsight.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ivarna.deviceinsight.MainActivity
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.fps.FpsMonitor
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.MonitorBus
import com.ivarna.deviceinsight.service.overlay.DialogOverlayWindowHost
import com.ivarna.deviceinsight.service.overlay.OverlayWindowHost
import com.ivarna.deviceinsight.service.overlay.RawOverlayWindowHost
import com.ivarna.deviceinsight.ui.caliper.hud.HudConfig
import com.ivarna.deviceinsight.ui.caliper.hud.HudRuntimeConfig
import com.ivarna.deviceinsight.ui.caliper.hudRuntimeConfigFlow
import com.ivarna.deviceinsight.ui.caliper.LauncherAlias
import com.ivarna.deviceinsight.ui.caliper.setHudLocked
import com.ivarna.deviceinsight.ui.caliper.setHudPosition
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {

    companion object {
        val isRunning = OverlayRuntimeState.isRunning
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val BLUR_RADIUS_DP = 10
        private const val TAG = "DeviceInsightOverlay"
    }

    @Inject lateinit var monitorBus: MonitorBus
    @Inject lateinit var fpsMonitor: FpsMonitor

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: android.view.WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var startupJob: Job? = null
    private var positionPersistJob: Job? = null
    private var host: OverlayWindowHost? = null
    private var observersStarted = false
    private var isDestroyed = false
    private var runtimeConfig = HudRuntimeConfig()
    private val slowState = mutableStateOf(com.ivarna.deviceinsight.data.monitor.HudSlow())
    private val fastState = mutableStateOf(HudFast())
    private val configState = mutableStateOf(HudConfig())
    private val blurAvailableState = mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SERVICE_CREATE api=${Build.VERSION.SDK_INT} manufacturer=${Build.MANUFACTURER} model=${Build.MODEL}")
        try {
            savedStateRegistryController.performAttach()
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
            windowManager = getSystemService(android.view.WindowManager::class.java)
            startForegroundNotification()
            Log.d(TAG, "FGS_STARTED")
        } catch (t: Throwable) {
            Log.e(TAG, "FGS_START_FAILED (${t::class.java.simpleName}: ${t.message})", t)
            isRunning.set(false)
            stopSelf()
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "PERMISSION_DENIED")
            isRunning.set(false)
            stopSelf()
            return
        }
        startStartupIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "SERVICE_START_COMMAND startId=$startId attached=${host?.isAttached == true}")
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "PERMISSION_DENIED_ON_START")
            isRunning.set(false)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        startStartupIfNeeded()
        return START_STICKY
    }

    private fun startStartupIfNeeded() {
        if (host?.isAttached == true || startupJob?.isActive == true) {
            Log.d(TAG, "START_IGNORED state=${if (host?.isAttached == true) "RUNNING" else "STARTING"}")
            return
        }
        startupJob = scope.launch {
            Log.d(TAG, "CONFIG_LOAD_BEGIN")
            val initial = withContext(Dispatchers.IO) {
                try {
                    (applicationContext as? com.ivarna.deviceinsight.SystemStatsApplication)?.awaitHudMigration()
                    applicationContext.hudRuntimeConfigFlow.first()
                        .also { Log.d(TAG, "CONFIG_LOAD_OK config=$it") }
                } catch (t: Throwable) {
                    Log.e(TAG, "CONFIG_LOAD_FAILED (${t::class.java.simpleName}: ${t.message})", t)
                    HudRuntimeConfig()
                }
            }
            if (!isActive || isDestroyed) return@launch
            runtimeConfig = initial
            configState.value = initial.panel
            attachOverlay(initial)
        }
    }

    private fun attachOverlay(initial: HudRuntimeConfig) {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "PERMISSION_DENIED_BEFORE_ATTACH")
            isRunning.set(false)
            stopSelf()
            return
        }

        fun newComposeView() = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent { HudContent() }
        }
        var composeView = newComposeView()
        val preferred: OverlayWindowHost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DialogOverlayWindowHost(this, windowManager, ::onBlurAvailabilityChanged, ::onPositionAdjusted)
        } else {
            RawOverlayWindowHost(this, windowManager, ::onBlurAvailabilityChanged, ::onPositionAdjusted)
        }
        var selected = preferred
        Log.d(TAG, "HOST_ATTACH_BEGIN preferred=${preferred::class.java.simpleName} initial=$initial")
        try {
            preferred.attach(composeView, initial)
        } catch (t: Throwable) {
            Log.e(TAG, "HOST_ATTACH_FAIL host=${preferred::class.java.simpleName} (${t::class.java.simpleName}: ${t.message})", t)
            try { preferred.detach() } catch (cleanup: Throwable) {
                Log.e(TAG, "HOST_ATTACH_CLEANUP_FAIL", cleanup)
            }
            if (preferred is DialogOverlayWindowHost) {
                Log.w(TAG, "HOST_FALLBACK_BEGIN host=RawOverlayWindowHost")
                selected = RawOverlayWindowHost(this, windowManager, ::onBlurAvailabilityChanged, ::onPositionAdjusted)
                // A Dialog may have attached the original ComposeView before failing during
                // initial layout. Always give the raw host a fresh unattached view.
                composeView = newComposeView()
                try {
                    selected.attach(composeView, initial)
                } catch (fallback: Throwable) {
                    Log.e(TAG, "HOST_FALLBACK_FAIL (${fallback::class.java.simpleName}: ${fallback.message})", fallback)
                    isRunning.set(false)
                    try { selected.detach() } catch (cleanup: Throwable) { Log.e(TAG, "HOST_FALLBACK_CLEANUP_FAIL", cleanup) }
                    stopSelf()
                    return
                }
            } else {
                isRunning.set(false)
                stopSelf()
                return
            }
        }

        host = selected
        selected.updateLocked(initial.panel.locked)
        selected.updateBackgroundBlur(
            initial.panel.backgroundBlurEnabled,
            (resources.displayMetrics.density * BLUR_RADIUS_DP).toInt().coerceAtLeast(1)
        )
        blurAvailableState.value = selected.blurAvailable
        if (selected.position.x != initial.x || selected.position.y != initial.y) {
            persistPosition(selected.position.x, selected.position.y)
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        isRunning.set(true)
        Log.d(TAG, "HOST_ATTACH_OK host=${selected::class.java.simpleName} position=${selected.position.x},${selected.position.y}")
        observeConfigAndFeeds()
        startFastTicker()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "CONFIGURATION_CHANGED orientation=${newConfig.orientation} density=${newConfig.densityDpi}")
        host?.let {
            it.requestContentLayout()
            it.updateLocked(configState.value.locked)
            it.updateBackgroundBlur(
                configState.value.backgroundBlurEnabled,
                (resources.displayMetrics.density * BLUR_RADIUS_DP).toInt().coerceAtLeast(1)
            )
        }
    }

    private fun onBlurAvailabilityChanged(available: Boolean) {
        blurAvailableState.value = available
        Log.d(TAG, "BLUR_CAPABILITY available=$available")
        host?.updateBackgroundBlur(
            configState.value.backgroundBlurEnabled,
            (resources.displayMetrics.density * BLUR_RADIUS_DP).toInt().coerceAtLeast(1)
        )
    }

    private fun observeConfigAndFeeds() {
        if (observersStarted) return
        observersStarted = true
        scope.launch {
            applicationContext.hudRuntimeConfigFlow
                .distinctUntilChanged()
                .collectLatest { next ->
                    val previous = runtimeConfig
                    runtimeConfig = next
                    configState.value = next.panel
                    val currentHost = host ?: return@collectLatest
                    if (previous.panel.locked != next.panel.locked) currentHost.updateLocked(next.panel.locked)
                    if (previous.panel.backgroundBlurEnabled != next.panel.backgroundBlurEnabled) {
                        currentHost.updateBackgroundBlur(
                            next.panel.backgroundBlurEnabled,
                            (resources.displayMetrics.density * BLUR_RADIUS_DP).toInt().coerceAtLeast(1)
                        )
                    }
                    if (previous.x != next.x || previous.y != next.y) {
                        currentHost.updatePosition(next.x, next.y)
                        if (currentHost.position.x != next.x || currentHost.position.y != next.y) {
                            persistPosition(currentHost.position.x, currentHost.position.y)
                        }
                    }
                    Log.d(TAG, "CONFIG_UPDATE medium=${next.panel.medium} scale=${next.panel.scale} opacity=${next.panel.opacity} blur=${next.panel.backgroundBlurEnabled} locked=${next.panel.locked} position=${next.x},${next.y}")
                }
        }
        scope.launch { monitorBus.slow.collect { slowState.value = it } }
        scope.launch { monitorBus.fast.collect { fastState.value = it } }
    }

    private fun startFastTicker() {
        scope.launch(Dispatchers.IO) {
            while (isActive && isRunning.get()) {
                val startedAt = System.currentTimeMillis()
                val sample = try {
                    fpsMonitor.getCurrentFpsWithSource()
                } catch (t: Throwable) {
                    Log.e(TAG, "FPS_SAMPLE_FAILED", t)
                    null
                }
                monitorBus.pushFast(HudFast(sample?.fps ?: 0, sample?.source ?: "—"))
                delay((1000L - (System.currentTimeMillis() - startedAt)).coerceAtLeast(200L))
            }
        }
    }

    private fun onPositionAdjusted(position: com.ivarna.deviceinsight.service.overlay.OverlayPosition) {
        val current = runtimeConfig
        if (current.x == position.x && current.y == position.y) return
        Log.d(TAG, "POSITION_ADJUSTED actual=${position.x},${position.y} previous=${current.x},${current.y}")
        persistPosition(position.x, position.y)
    }

    private fun onDrag(dxPx: Int, dyPx: Int) {
        val currentHost = host ?: return
        val before = currentHost.position
        currentHost.updatePosition(before.x + dxPx, before.y + dyPx)
        val after = currentHost.position
        positionPersistJob?.cancel()
        positionPersistJob = scope.launch(Dispatchers.IO) {
            delay(500)
            try { setHudPosition(after.x, after.y) }
            catch (t: Throwable) { Log.e(TAG, "POSITION_PERSIST_FAILED", t) }
        }
    }

    private fun persistPosition(x: Int, y: Int) {
        positionPersistJob?.cancel()
        positionPersistJob = scope.launch(Dispatchers.IO) {
            try { setHudPosition(x, y) }
            catch (t: Throwable) { Log.e(TAG, "POSITION_CLAMP_PERSIST_FAILED", t) }
        }
    }

    private fun onLock() {
        scope.launch(Dispatchers.IO) {
            try { setHudLocked(true) }
            catch (t: Throwable) { Log.e(TAG, "LOCK_PERSIST_FAILED", t) }
        }
    }

    private fun onOpenConfig() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("di_route", "hud-config")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    @Composable
    private fun HudContent() {
        val cfg = configState.value
        val blurSupported = blurAvailableState.value
        val effectiveOpacity = if (cfg.backgroundBlurEnabled && !blurSupported) {
            (cfg.opacity + 0.10f).coerceAtMost(0.97f)
        } else cfg.opacity
        com.ivarna.deviceinsight.ui.caliper.hud.HudPanel(
            config = cfg,
            slow = slowState,
            fast = fastState,
            effectiveOpacity = effectiveOpacity,
            interactive = true,
            onDrag = ::onDrag,
            onLock = ::onLock,
            onOpenConfig = ::onOpenConfig
        )
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "DeviceInsight HUD", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DeviceInsight HUD")
            .setContentText("Scope Probe running · tap STOP in app")
            .setSmallIcon(R.drawable.ic_tile_caliper)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (isDestroyed) return
        isDestroyed = true
        Log.d(TAG, "SERVICE_DESTROY")
        isRunning.set(false)
        startupJob?.cancel()
        positionPersistJob?.cancel()
        scope.cancel()
        try { host?.detach() } catch (t: Throwable) { Log.e(TAG, "HOST_DETACH_FAILED", t) }
        host = null
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
        super.onDestroy()
        runCatching { LauncherAlias.retryPending(this) }
            .onFailure { Log.e(TAG, "LAUNCHER_ALIAS_RETRY_FAILED", it) }
    }
}
