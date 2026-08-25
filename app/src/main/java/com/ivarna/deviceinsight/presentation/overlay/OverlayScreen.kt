package com.ivarna.deviceinsight.presentation.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.service.OverlayService
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.hud.HudModule
import com.ivarna.deviceinsight.ui.caliper.hud.HudPanel
import com.ivarna.deviceinsight.ui.caliper.hud.HudScale
import com.ivarna.deviceinsight.ui.caliper.hud.HudTheme
import com.ivarna.deviceinsight.ui.caliper.hud.rememberHudDemo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** № 03 — OVERLAY / Scope Probe config (S-11). CALIPER config sheet. */
@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ON_RESUME refresh + one 400 ms delayed re-check — Android 8 canDrawOverlays is stale
    // right after grant (issuetracker 62047810). Never auto-starts the HUD.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                scope.launch {
                    delay(400)
                    viewModel.refreshPermissions()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "Overlay",
            subtitle = if (state.isServiceRunning) "● live · running" else "floating performance overlay",
            warn = state.isServiceRunning
        )

        // F3: wrap-to-scale preview — stage centers a fixed-width probe, never page-width,
        // never a WindowManager overlay. Gap above PERMISSIONS comes from the vertical padding.
        HudPreviewHost(state, viewModel)
        Spacer(Modifier.height(12.dp))

        // Permissions
        PanelCard(title = "PERMISSIONS") {
            SpecRow("overlay", if (state.permissions.hasOverlay) "GRANTED" else "REQUIRED")
            SpecRow("usage", if (state.permissions.hasUsageStats) "GRANTED" else "REQUIRED")
            if (state.permissions.hasShizukuInstalled) {
                SpecRow("shizuku", if (state.permissions.hasShizukuPermission) "AUTHORIZED" else "TAP TO GRANT")
            }
            Spacer(Modifier.height(8.dp))
            if (!state.permissions.hasOverlay) {
                HardKey("GRANT OVERLAY", modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        )
                    })
                Spacer(Modifier.height(8.dp))
            }
            if (!state.permissions.hasUsageStats) {
                HardKey("GRANT USAGE ACCESS", modifier = Modifier.fillMaxWidth(),
                    onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) })
            }
            if (state.permissions.hasShizukuInstalled && !state.permissions.hasShizukuPermission) {
                Spacer(Modifier.height(8.dp))
                HardKey("AUTHORIZE SHIZUKU", modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.requestShizukuPermission() })
            }
        }
        Spacer(Modifier.height(12.dp))

        // Probe style — explicit medium (never follow-system), S/M/L scale, opacity
        PanelCard(title = "STYLE & LAYOUT") {
            SegKey(
                options = listOf(HudScale.S, HudScale.M, HudScale.L),
                selected = state.config.scale,
                onSelect = { viewModel.setHudScale(it) },
                labelFor = { it.name }
            )
            Spacer(Modifier.height(8.dp))
            SegKey(
                options = listOf(
                    com.ivarna.deviceinsight.ui.caliper.hud.HudMedium.PAPER,
                    com.ivarna.deviceinsight.ui.caliper.hud.HudMedium.CARBON,
                    com.ivarna.deviceinsight.ui.caliper.hud.HudMedium.BLUEPRINT
                ),
                selected = state.config.medium,
                onSelect = { viewModel.setHudMedium(it) },
                labelFor = { it.name }
            )
            Spacer(Modifier.height(8.dp))
            FaderKey(
                value = state.config.opacity,
                onValueChange = { viewModel.setHudOpacity(it) },
                valueRange = 0.40f..0.90f,
                ticks = 6,
                label = "opacity",
                valueText = { "${(it * 100).toInt()}%" }
            )
            Spacer(Modifier.height(8.dp))
            DipSwitch(
                checked = state.config.blurBehind,
                onCheckedChange = { viewModel.setBlurBehind(it) },
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                label = "blur behind" + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "" else " · needs android 12"
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                MarginNote(message = "window blur is unavailable below android 12 — scrim opacity compensates (+10 pt)", title = "NOTE")
            }
            Spacer(Modifier.height(4.dp))
            DipSwitch(
                checked = state.config.locked,
                onCheckedChange = { viewModel.setLocked(it) },
                label = "lock (touch passthrough)"
            )
            HardKey("RESET POSITION", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(), onClick = { viewModel.resetPosition() })
        }
        Spacer(Modifier.height(12.dp))

        // Modules — bands are toggles; TRACE has no history feed yet and renders honestly empty
        PanelCard(title = "MODULES") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                HudModule.entries.forEach { module ->
                    DipSwitch(
                        checked = module in state.config.modules,
                        onCheckedChange = { enabled -> viewModel.toggleModule(module, enabled) },
                        label = module.name
                    )
                }
                DipSwitch(
                    checked = state.config.showCoreBank,
                    onCheckedChange = { viewModel.setShowCoreBank(it) },
                    label = "core bank"
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // FPS mode feeds FpsMonitor via caliper store (HudSettingsCache reads synchronously)
        PanelCard(title = "FPS MONITOR MODE") {
            SegKey(
                options = listOf(FpsMode.AUTO, FpsMode.ROOT, FpsMode.SHIZUKU),
                selected = state.fpsMode,
                onSelect = { mode ->
                    viewModel.setFpsMode(mode)
                    if (mode == FpsMode.SHIZUKU &&
                        state.permissions.hasShizukuInstalled && !state.permissions.hasShizukuPermission
                    ) viewModel.requestShizukuPermission()
                },
                labelFor = { it.name }
            )
        }
        Spacer(Modifier.height(12.dp))

        // F2 action row: STOP always when running; START only when canDrawOverlays;
        // otherwise nothing — GRANT OVERLAY above is the only CTA.
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isServiceRunning -> HardKey("STOP", variant = HardKeyVariant.DESTRUCTIVE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.stopService(Intent(context, OverlayService::class.java))
                        viewModel.setServiceRunning(false)
                    })
                state.permissions.hasOverlay -> HardKey("START", variant = HardKeyVariant.PRIMARY,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // check, then start — never start then hope
                        if (!Settings.canDrawOverlays(context)) return@HardKey
                        val intent = viewModel.buildServiceIntent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        viewModel.setServiceRunning(true)
                    })
                else -> {} // zero START keys while permission denied
            }
        }
        EndOfSheet()
    }
}

/**
 * Preview host — single source of truth for panel width (HudScales).
 * Preview frame owns padding/background; HudPanel owns fixed HUD width.
 * Uses BoxWithConstraints to stay inside page on narrow devices (scaled preview only).
 * clipToBounds at frame boundary ensures brackets/content never escape.
 */
@Composable
private fun HudPreviewHost(
    state: OverlayUiState,
    viewModel: OverlayViewModel
) {
    val previewPadding = 8.dp
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val hudWidth = when (state.config.scale) {
            HudScale.S -> 156.dp
            HudScale.M -> 252.dp
            HudScale.L -> 340.dp
        }
        // Also supports correct HudScales values if they change: read from HudScales directly
        // Fallback uses the explicit mapping above for clarity; actual HudPanel uses same scale.
        val desiredFrameWidth = hudWidth + previewPadding * 2
        val availableWidth = maxWidth
        val frameWidth = if (desiredFrameWidth > availableWidth) availableWidth else desiredFrameWidth
        // If frame must shrink, scale preview representation only (not the service HUD size)
        val scaleFactor = if (desiredFrameWidth > availableWidth) {
            (availableWidth - previewPadding * 2) / hudWidth
        } else 1f

        Box(
            Modifier
                .width(frameWidth)
                .wrapContentHeight()
                .background(Caliper.colors.panel)
                .clipToBounds()
                .padding(previewPadding)
                .clipToBounds()
        ) {
            // Centered, clipped stage
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                // Apply scale only to preview representation when needed
                val previewModifier = if (scaleFactor < 1f) {
                    Modifier.graphicsLayer(scaleX = scaleFactor, scaleY = scaleFactor, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f))
                } else Modifier

                Box(modifier = previewModifier) {
                    if (state.isServiceRunning) {
                        val slow by viewModel.hudSlow.collectAsStateWithLifecycle()
                        val fast by viewModel.hudFast.collectAsStateWithLifecycle()
                        HudTheme(medium = state.config.medium, scale = state.config.scale) {
                            HudPanel(
                                config = state.config.copy(locked = false),
                                slow = androidx.compose.runtime.rememberUpdatedState(slow),
                                fast = androidx.compose.runtime.rememberUpdatedState(fast),
                                effectiveOpacity = state.config.opacity,
                                interactive = false
                            )
                        }
                    } else {
                        val (demoSlow, demoFast) = rememberHudDemo()
                        HudTheme(medium = state.config.medium, scale = state.config.scale) {
                            HudPanel(
                                config = state.config,
                                slow = demoSlow,
                                fast = demoFast,
                                effectiveOpacity = state.config.opacity,
                                interactive = false
                            )
                        }
                    }
                }
            }
        }
    }
}
