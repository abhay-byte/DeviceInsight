package com.ivarna.deviceinsight.presentation.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.hud.CaliperHud
import com.ivarna.deviceinsight.ui.caliper.hud.HudState

/** № 03 — OVERLAY / HUD config (S-11). CALIPER config sheet. */
@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // HUD live preview fed by current config (values are illustrative on config sheet).
    val preview = HudState(
        fps = 119.8f, fpsSource = "SF",
        cpu = 42f, cpuHist = listOf(0.3f, 0.4f, 0.42f, 0.38f, 0.5f, 0.46f),
        gpu = 71f, ramBytes = 6_800_000_000L, tempC = 58f,
        netDown = 18_100_000L, netUp = 2_400_000L
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            sheetLabel = "№ 03 — OVERLAY",
            title = "Overlay.",
            sub = if (state.isServiceRunning) "● live · running" else "performance overlay · config",
            warn = state.isServiceRunning
        )

        // HUD preview — corner brackets + scrim (the real service draws the window)
        CaliperHud(preview, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))

        // Status + permissions
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

        // Style & Layout
        PanelCard(title = "STYLE & LAYOUT") {
            DipSwitch(
                checked = state.isHorizontal,
                onCheckedChange = viewModel::setHorizontal,
                label = "horizontal layout"
            )
            Spacer(Modifier.height(12.dp))
            FaderKey(
                value = state.scaleFactor,
                onValueChange = viewModel::setScaleFactor,
                valueRange = 0.5f..2.0f,
                ticks = 7,
                label = "scale",
                valueText = { String.format(java.util.Locale.US, "%.1fx", it) }
            )
        }
        Spacer(Modifier.height(12.dp))

        // FPS mode
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

        // Metrics toggles
        PanelCard(title = "METRICS") {
            state.metrics.sortedBy { it.order }.forEach { metric ->
                DipSwitch(
                    checked = metric.enabled,
                    onCheckedChange = { enabled -> viewModel.toggleMetric(metric.id, enabled) },
                    label = metric.name
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // Actions
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.isServiceRunning) {
                HardKey("STOP", variant = HardKeyVariant.DESTRUCTIVE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        context.stopService(Intent(context, com.ivarna.deviceinsight.service.OverlayService::class.java))
                        viewModel.setServiceRunning(false)
                    })
            } else {
                HardKey("START", variant = HardKeyVariant.PRIMARY,
                    modifier = Modifier.weight(1f),
                    enabled = state.permissions.hasOverlay,
                    onClick = {
                        val intent = viewModel.buildServiceIntent()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        viewModel.setServiceRunning(true)
                    })
            }
        }
        EndOfSheet()
    }
}