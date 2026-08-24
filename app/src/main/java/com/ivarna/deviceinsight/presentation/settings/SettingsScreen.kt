package com.ivarna.deviceinsight.presentation.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.PaperColors
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.hatch
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentMedium: Medium?,
    onMediumSelected: (Medium) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val c = Caliper.colors
    val resolved = currentMedium ?: Medium.PAPER
    val haptics = rememberCaliperHaptics()
    var showColophon by remember { mutableStateOf(false) }
    var showWidgets by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val showGrid by context.showGridFlow.collectAsStateWithLifecycle(initialValue = true)
    // CALIPER §5.14 — camera permission is gated; check once at top so state survives sheet switches
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showPermRationale by remember { mutableStateOf(false) }
    var permPermanentlyDenied by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) {
            haptics.confirm()
            showPermRationale = false
            permPermanentlyDenied = false
        } else {
            val shouldShow = try {
                (context as? androidx.activity.ComponentActivity)?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            } catch (_: Exception) { false }
            showPermRationale = true
            permPermanentlyDenied = !shouldShow
            haptics.tick()
        }
    }
    // re-check when returning from system Settings (Settings card + DevicesTab both need this)
    DisposableEffect(lifecycleOwner) {
        val obs2 = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) {
                val now = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (now != cameraGranted) {
                    cameraGranted = now
                    if (now) { showPermRationale = false; permPermanentlyDenied = false }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs2)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs2) }
    }
    var widgetCount by remember { mutableIntStateOf(0) }
    suspend fun refreshCount() {
        try {
            val mgr = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            val kinds = listOf(
                com.ivarna.deviceinsight.ui.caliper.widget.ScopeWidget::class.java,
                com.ivarna.deviceinsight.ui.caliper.widget.StackWidget::class.java,
                com.ivarna.deviceinsight.ui.caliper.widget.FuelWidget::class.java,
                com.ivarna.deviceinsight.ui.caliper.widget.RasterWidget::class.java,
                com.ivarna.deviceinsight.ui.caliper.widget.BenchWidgetAll::class.java
            )
            var total = 0
            for (k in kinds) try { total += mgr.getGlanceIds(k).size } catch (_: Exception) {}
            widgetCount = total
        } catch (_: Exception) {}
    }
    LaunchedEffect(showWidgets) {
        if (!showWidgets) refreshCount()
    }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev -> if (ev == Lifecycle.Event.ON_RESUME) scope.launch { refreshCount() } }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    if (showWidgets) BackHandler { showWidgets = false }
    BackHandler(enabled = showColophon) { showColophon = false }

    Column(Modifier.fillMaxSize().caliperGrid()) {
        if (showWidgets) {
            WidgetsSheet(onBack = { showWidgets = false })  // sheet is the only scroller on this branch
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (!showColophon) {
            if (onBack != null) {
                HardKey("← BACK", variant = HardKeyVariant.SECONDARY,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onClick = onBack)
            }
            ScreenHeader("№ 05 — SETTINGS", "Settings.", "control panel · caliper standard")

            // 01 PRESENTATION
            Text("01 PRESENTATION", style = Caliper.type.meta, color = c.ink60,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(Modifier.height(4.dp))
            Text("media", style = Caliper.type.label, color = c.ink40,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(6.dp))
            SegKey(
                options = listOf(Medium.PAPER, Medium.CARBON, Medium.BLUEPRINT),
                selected = resolved,
                onSelect = { onMediumSelected(it); haptics.confirm() },
                modifier = Modifier.padding(horizontal = 16.dp),
                labelFor = { it.name }
            )
            Spacer(Modifier.height(8.dp))
            MediumSwatches(resolved)
            MarginNote(
                message = "the launcher icon follows media — some launchers cache icons, so the swap may need a launcher restart to show",
                title = "NOTE",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(12.dp))

            // presentation DIPs drive global grid/hatching via DataStore
            DipSwitch(
                checked = showGrid,
                onCheckedChange = { scope.launch { context.setShowGrid(it) } },
                modifier = Modifier.padding(horizontal = 16.dp),
                label = "graph-paper grid"
            )
            Spacer(Modifier.height(10.dp))

            if (!cameraGranted) {
                Text("02 PERMISSIONS", style = Caliper.type.meta, color = c.ink60,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                Spacer(Modifier.height(6.dp))
                PanelCard(
                    title = "CAMERA · ROSTER",
                    status = { Text("LOCKED", style = Caliper.type.meta, color = c.fault) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        "camera roster reads hardware only — no shutter, no preview. Without access the optics inventory stays hatched as CHANNEL LOCKED.",
                        style = Caliper.type.dataS, color = c.ink60
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().height(12.dp)
                            .border(1.dp, c.hairline)
                            .drawBehind { hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, c.ink40.copy(alpha = 0.25f)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    if (showPermRationale) {
                        MarginNote(
                            message = if (permPermanentlyDenied)
                                "Camera was denied. Open system settings to enable, or continue with a locked roster — no popup will reappear on its own."
                            else
                                "Camera not granted. Roster stays locked. This card is the only place that opens the system popup — no auto dialog elsewhere.",
                            title = "NOTE 002",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    HardKey("GRANT CAMERA", variant = HardKeyVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) })
                    Spacer(Modifier.height(8.dp))
                    Text("only this key opens the system popup · skip → DEVICE · HARDWARE stays hatched", style = Caliper.type.meta, color = c.ink40)
                    if (permPermanentlyDenied) {
                        Spacer(Modifier.height(8.dp))
                        HardKey("OPEN SYSTEM SETTINGS", variant = HardKeyVariant.SECONDARY,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    try { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
                                }
                            })
                    }
                }
                Spacer(Modifier.height(12.dp))
                DoubleRule(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
            }

            // 03 WIDGETS
            Text("03 WIDGETS", style = Caliper.type.meta, color = c.ink60,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(Modifier.height(6.dp))
            PanelCard(
                title = "INSTRUMENTS",
                status = { Text(if (widgetCount == 0) "NOT PLACED" else "PLACED ×$widgetCount", style = Caliper.type.meta, color = c.ink40) },
                onClick = { showWidgets = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text("home-screen panels · tap to place or inspect", style = Caliper.type.dataS, color = c.ink60)
            }
            Spacer(Modifier.height(20.dp))
            DoubleRule(Modifier.padding(horizontal = 16.dp))

            // 05 SYSTEM
            Text("05 SYSTEM", style = Caliper.type.meta, color = c.ink60,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Spacer(Modifier.height(10.dp))
            HardKey("06 ABOUT → COLOPHON", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { showColophon = true })
        } else {
            // № 06 — COLOPHON
            ScreenHeader("№ 06 — COLOPHON", "Colophon.", "the making of the instrument")
            Text("Set in Instrument Serif & IBM Plex Mono.", style = Caliper.type.dataS, color = c.ink,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))
            Text("Drawn on a 4pt grid. No gradients were used in", style = Caliper.type.dataS, color = c.ink,
                modifier = Modifier.padding(horizontal = 16.dp))
            Text("the making of this instrument.", style = Caliper.type.dataS, color = c.ink,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            Text("REVISIONS", style = Caliper.type.meta, color = c.ink60,
                modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(6.dp))
            SpecRow("1.0.3 · 4", "CALIPER calibration · camera & thermal integrity", Modifier.padding(horizontal = 16.dp))
            SpecRow("REV A", "CALIPER design language adopted", Modifier.padding(horizontal = 16.dp))
            SpecRow("v1", "Elegant Glassmorphism (retired)", Modifier.padding(horizontal = 16.dp))
            SpecRow("v0", "first internal build", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(16.dp))
            SpecRow("VERSION", "${com.ivarna.deviceinsight.BuildConfig.VERSION_NAME} · Build ${com.ivarna.deviceinsight.BuildConfig.VERSION_CODE}", Modifier.padding(horizontal = 16.dp))
            SpecRow("LICENSE", "GPL-3.0", Modifier.padding(horizontal = 16.dp))
            SpecRow("BUILT BY", "Ivarna", Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(20.dp))
            HardKey("← BACK TO SETTINGS", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { showColophon = false })
        }
        EndOfSheet()
            }
        }
    }
}

/** Three paper-sample swatches — real rendered mini-panels, not color dots. */
@Composable
private fun MediumSwatches(selected: Medium) {
    val c = Caliper.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            Triple(Medium.PAPER, PaperColors.surface, PaperColors.ink),
            Triple(Medium.CARBON, Color(0xFF141310), Color(0xFFEDE7DA)),
            Triple(Medium.BLUEPRINT, Color(0xFF0C2338), Color(0xFFEAF2FF))
        ).forEach { (medium, swatch, inkColor) ->
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().height(48.dp)
                        .background(swatch)
                        .border(1.dp, if (medium == selected) c.accent else c.hairline)
                ) {
                    Text("A1", style = Caliper.type.meta, color = inkColor,
                        modifier = Modifier.padding(8.dp))
                }
                Text(medium.name.uppercase(), style = Caliper.type.meta, color = c.ink60,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}