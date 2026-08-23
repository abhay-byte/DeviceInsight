package com.ivarna.deviceinsight.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
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
import com.ivarna.deviceinsight.ui.caliper.hud.HudConfig
import com.ivarna.deviceinsight.ui.caliper.hud.HudPanel
import com.ivarna.deviceinsight.ui.caliper.hud.HudScale
import com.ivarna.deviceinsight.ui.caliper.hud.HudTheme
import com.ivarna.deviceinsight.ui.caliper.hud.hudMediumFromString
import com.ivarna.deviceinsight.ui.caliper.hudMediumFlow
import com.ivarna.deviceinsight.ui.caliper.hudOpacityFlow
import com.ivarna.deviceinsight.ui.caliper.hudBlurFlow
import com.ivarna.deviceinsight.ui.caliper.hudLockedFlow
import com.ivarna.deviceinsight.ui.caliper.hudModulesFlow
import com.ivarna.deviceinsight.ui.caliper.hudScaleFlow
import com.ivarna.deviceinsight.ui.caliper.hudShowCoreBankFlow
import com.ivarna.deviceinsight.ui.caliper.hudXFlow
import com.ivarna.deviceinsight.ui.caliper.hudYFlow
import com.ivarna.deviceinsight.ui.caliper.setHudLocked
import com.ivarna.deviceinsight.ui.caliper.setHudX
import com.ivarna.deviceinsight.ui.caliper.setHudY
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Scope Probe host (DI-HD-001). Keeps the FQCN + foreground contract of the old overlay
 * service; internals are a WRAP_CONTENT ComposeView hosting [HudPanel]. Config lives only
 * in the `caliper` DataStore — no intent extras.
 */
@AndroidEntryPoint
class OverlayService : Service(), LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {

    companion object {
        val isRunning = java.util.concurrent.atomic.AtomicBoolean(false)
        private const val CHANNEL_ID = "overlay_channel"
        private const val NOTIFICATION_ID = 1
        private const val BLUR_RADIUS_DP = 10
    }

    @Inject lateinit var monitorBus: MonitorBus
    @Inject lateinit var fpsMonitor: FpsMonitor

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var view: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionPersistJob: Job? = null
    private var dragX: Int = 100
    private var dragY: Int = 100

    // Panel state holders — slow at 2 Hz (repository), fast at ~10 Hz (own ticker)
    private val slowState = mutableStateOf(com.ivarna.deviceinsight.data.monitor.HudSlow())
    private val fastState = mutableStateOf(HudFast())
    private val configState = mutableStateOf(HudConfig())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning.set(true)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        windowManager = getSystemService(WindowManager::class.java)

        // FGS contract first — never stopSelf without it on API 26+
        startForegroundNotification()

        // F2 defense-in-depth: UI gates START, service refuses to draw without special-app-access.
        // startForeground has already run, so stopSelf here is legal.
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        params = buildParams()
        applyBlurBehind()

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent { HudContent() }
        }
        view = composeView
        try {
            windowManager.addView(composeView, params)
        } catch (_: Exception) {
            // OEM revoked the permission mid-flight (BadTokenException et al.)
            view = null
            stopSelf()
            return
        }

        observeConfigAndFeeds()
        startFastTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Config is DataStore-owned; extras are ignored by design (process-death safe).
        if (!Settings.canDrawOverlays(this)) stopSelf()
        return START_STICKY
    }

    // ─────────────── window plumbing ───────────────

    @SuppressLint("RtlHardcoded")
    private fun buildParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dragX
            y = dragY
        }
    }

    private fun touchFlags(base: Int, locked: Boolean): Int =
        if (locked) base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE   // full passthrough
        else base and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()   // drag + tap

    private fun applyBlurBehind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && configState.value.blurBehind) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.blurBehindRadius = (BLUR_RADIUS_DP * resources.displayMetrics.density).toInt()
        } else {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
        }
    }

    private fun relayout() {
        val v = view ?: return
        try {
            windowManager.updateViewLayout(v, params)
        } catch (_: Exception) {
            stopSelf()
        }
    }

    // ─────────────── feeds ───────────────

    private fun hudConfigFlow(): Flow<HudConfig> {
        val core = combine(
            hudMediumFlow.map(::hudMediumFromString),
            hudScaleFlow.map { s -> runCatching { HudScale.valueOf(s) }.getOrDefault(HudScale.M) },
            hudOpacityFlow,
            hudBlurFlow,
            hudLockedFlow
        ) { medium, scale, opacity, blur, locked ->
            HudConfig(medium = medium, scale = scale, opacity = opacity, blurBehind = blur, locked = locked)
        }
        return combine(
            core,
            hudModulesFlow.map { HudConfig.fromCsv(it) },
            hudShowCoreBankFlow
        ) { cfg, modules, coreBank ->
            cfg.copy(modules = modules, showCoreBank = coreBank)
        }
    }

    private fun observeConfigAndFeeds() {
        scope.launch {
            // initial position from store (never runBlocking on main)
            dragX = try { withContext(Dispatchers.IO) { hudXFlow.first() } } catch (_: Exception) { 100 }
            dragY = try { withContext(Dispatchers.IO) { hudYFlow.first() } } catch (_: Exception) { 100 }
            params.x = dragX; params.y = dragY
            relayout()
        }
        scope.launch {
            hudConfigFlow().collectLatest { cfg ->
                val lockChanged = configState.value.locked != cfg.locked
                val blurChanged = configState.value.blurBehind != cfg.blurBehind
                configState.value = cfg
                if (lockChanged || blurChanged) {
                    applyBlurBehind()
                    params.flags = touchFlags(params.flags, cfg.locked)
                    relayout()
                }
            }
        }
        scope.launch { monitorBus.slow.collect { slowState.value = it } }
        scope.launch { monitorBus.fast.collect { fastState.value = it } }
    }

    /** 10 Hz dumpsys probe on IO — adaptive backoff after 5× "—", honest source stamp. */
    private fun startFastTicker() {
        scope.launch(Dispatchers.IO) {
            var consecutiveDash = 0
            while (isActive && isRunning.get()) {
                val sample = try { fpsMonitor.getCurrentFpsWithSource() } catch (_: Exception) { null }
                if (sample == null || sample.source == "—") consecutiveDash++ else consecutiveDash = 0
                monitorBus.pushFast(HudFast(sample?.fps ?: 0, sample?.source ?: "—"))
                delay(if (consecutiveDash >= 5) 1000L else 100L)
            }
        }
    }

    // ─────────────── panel callbacks ───────────────

    private fun onDrag(dxPx: Int, dyPx: Int) {
        dragX += dxPx
        dragY += dyPx
        params.x = dragX
        params.y = dragY
        relayout()
        positionPersistJob?.cancel()
        positionPersistJob = scope.launch(Dispatchers.IO) {
            delay(500)
            runCatching { setHudX(dragX); setHudY(dragY) }
        }
    }

    private fun onLock() {
        scope.launch(Dispatchers.IO) { runCatching { setHudLocked(true) } }
    }

    private fun onOpenConfig() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("di_route", "hud-config")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    // ─────────────── composition ───────────────

    @androidx.compose.runtime.Composable
    private fun HudContent() {
        var blurSupported by remember { mutableStateOf(true) }
        DisposableEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val listener = java.util.function.Consumer<Boolean> { enabled ->
                    blurSupported = enabled
                    applyBlurBehind()
                    relayout()
                }
                windowManager.addCrossWindowBlurEnabledListener(listener)
                onDispose {
                    windowManager.removeCrossWindowBlurEnabledListener(listener)
                }
            } else {
                onDispose { }
            }
        }

        val cfg = configState.value
        val effectiveOpacity =
            if (cfg.blurBehind && blurSupported && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) cfg.opacity
            else (cfg.opacity + 0.10f).coerceAtMost(0.97f)   // scrim compensates for missing blur

        HudTheme(medium = cfg.medium, scale = cfg.scale) {
            HudPanel(
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
    }

    // ─────────────── notification ───────────────

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DeviceInsight HUD",
                NotificationManager.IMPORTANCE_LOW
            )
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
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        isRunning.set(false)
        scope.cancel()
        view?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        view = null
        super.onDestroy()
    }
}
