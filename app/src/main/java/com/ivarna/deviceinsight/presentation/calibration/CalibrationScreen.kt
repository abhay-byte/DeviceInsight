package com.ivarna.deviceinsight.presentation.calibration

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.hatch

/**
 * S-00 Calibration onboarding — four numbered sheets (Paper-only) ended by the certificate.
 * All sheets are Paper (light drafting paper) per user request — no Carbon/Blueprint here.
 * Each permission is gated to an explicit HardKey tap — never auto-requested.
 * Media step removed — defaults to PAPER; not asked. Skipped steps surface as MarginNotes.
 */
@Composable
fun CalibrationScreen(
    initialMedium: Medium?,
    onMedium: (Medium) -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val haptics = rememberCaliperHaptics()
    // Force Paper for all calibration — user reported only 05 was Paper, make all Paper.
    // Persist PAPER immediately so even if user kills app mid-calibration, theme stays Paper.
    LaunchedEffect(Unit) {
        onMedium(Medium.PAPER)
    }

    Column(
        Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())
    ) {
        when (step) {
            0 -> CoverSheet(onBegin = { step = 1 })
            1 -> UsageAccessSheet(
                onGranted = { step = 2 },
                onSkip = { step = 2 }
            )
            2 -> CameraCalibSheet(
                onGranted = { step = 3 },
                onSkip = { step = 3 }
            )
            3 -> OverlaySheet(
                onGranted = { step = 4 },
                onSkip = { step = 4 }
            )
            4 -> CalibSheet(
                num = "04 · ROOT PROBE",
                title = "Calibration.",
                body = "Optional. Probe runs and stamps the result. No root is fine — locked channels hatch and stay honest.",
                actionLabel = "INSPECT CERTIFICATE",
                onAction = {
                    onMedium(Medium.PAPER)
                    haptics.confirm()
                    step = 5
                },
                skipLabel = "SKIP",
                onSkip = {
                    onMedium(Medium.PAPER)
                    step = 5
                }
            )
            else -> CertificateSheet(
                deviceName = "DEVICEINSIGHT",
                onFinish = onFinish
            )
        }
        // Progress: 00 is cover, 01-04 are the four permission sheets. Certificate shows all ✓.
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val labels = listOf("00", "01", "02", "03", "04")
            labels.forEachIndexed { i, s ->
                // step 0 is cover (00), so i==0 corresponds to step 0; for certificate (step 5) all are ✓
                val done = when {
                    step >= 5 -> true
                    i < step -> true
                    i == step -> false
                    else -> false
                }
                // cover (00) is not a numbered channel but we still show its dot
                Text(
                    if (done) "$s · ✓" else s,
                    style = Caliper.type.meta,
                    color = if (i <= step) Caliper.colors.ink else Caliper.colors.ink40,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CoverSheet(onBegin: () -> Unit) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Text("DOC № DI-0001 · REV 2.0", style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(20.dp))
        // Logo — CALIPER spec: paper square 2dp radius · ink crosshair centered · 3dp accent center dot
        // Drawn via Canvas (avoids VectorDrawable vs mipmap adaptive-icon crash)
        Box(
            Modifier.size(96.dp)
                .background(c.panel)
                .border(1.dp, c.hairline),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(72.dp)) {
                val stroke = 1.5.dp.toPx()
                val r = size.minDimension / 2 - stroke
                // outer paper square with 2dp radius
                drawRoundRect(color = c.ink, size = size, cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()), style = Stroke(stroke))
                // inner hairline frame 24dp
                val inset = (size.width - 24.dp.toPx()) / 2
                drawRoundRect(color = c.hairline, topLeft = Offset(inset, inset), size = Size(24.dp.toPx(), 24.dp.toPx()), cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx()), style = Stroke(1.dp.toPx()))
                // crosshair
                drawCircle(color = c.ink, radius = r * 0.42f, center = center, style = Stroke(stroke))
                drawLine(color = c.ink, start = Offset(center.x - r - 2.dp.toPx(), center.y), end = Offset(center.x + r + 2.dp.toPx(), center.y), strokeWidth = stroke)
                drawLine(color = c.ink, start = Offset(center.x, center.y - r - 2.dp.toPx()), end = Offset(center.x, center.y + r + 2.dp.toPx()), strokeWidth = stroke)
                drawCircle(color = c.accent, radius = 3.dp.toPx(), center = center)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Device", style = Caliper.type.display1.copy(fontSize = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink)
        Text("Insights.", style = Caliper.type.display1.copy(fontSize = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp)), color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("DEVICE  INSIGHTS", style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(12.dp))
        Text(
            "Field-grade performance instruments — measure everything. Label everything.",
            style = Caliper.type.body, color = c.ink60,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        PanelCard(title = "INSTRUMENT · CALIPER  REV A") {
            Text("One-of-a-kind bench for your phone — not a template, not a dashboard. Each channel is numbered, hatched, and honestly labeled.", style = Caliper.type.dataS, color = c.ink60)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(c.accent))
                Text("CALIPER  PAPER  ·  0dp radius · hairline · graph paper", style = Caliper.type.meta, color = c.ink40)
            }
        }
        Spacer(Modifier.height(20.dp))
        HardKey("BEGIN CALIBRATION", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = onBegin)
        Spacer(Modifier.height(8.dp))
        Text("2 min · every step optional · ≈ marks reduced accuracy", style = Caliper.type.meta, color = c.ink40, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun hasUsageAccess(context: android.content.Context): Boolean {
    return try {
        val appOps = context.getSystemService(android.content.Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }
}

@Composable
private fun UsageAccessSheet(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val c = Caliper.colors
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(hasUsageAccess(context)) }
    val haptics = rememberCaliperHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = hasUsageAccess(context)
                if (now && !isGranted) {
                    isGranted = true
                    haptics.confirm()
                } else if (!now && isGranted) {
                    isGranted = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text("01 · USAGE ACCESS", style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(8.dp))
        Text("Calibration.", style = Caliper.type.display1, color = c.ink)
        Spacer(Modifier.height(16.dp))
        Text(
            "DeviceInsight reads per-app CPU and memory from UsageStats. Without it, figures are estimated (≈) and the ledger shows coarse totals. Grant once — no background polling.",
            style = Caliper.type.body, color = c.ink60
        )
        Spacer(Modifier.height(16.dp))
        UsageDiagram(Modifier.fillMaxWidth().height(110.dp).background(c.panel).border(1.dp, c.hairline).padding(12.dp))
        Spacer(Modifier.height(16.dp))
        PanelCard(
            title = "FIG. 1 — USAGE PATH",
            status = {
                if (isGranted) StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                else Text("LOCKED", style = Caliper.type.meta, color = c.fault)
            }
        ) {
            if (isGranted) {
                Text("usage access granted · per-process CPU / memory live", style = Caliper.type.dataS, color = c.ink60)
            } else {
                Box(Modifier.fillMaxWidth().height(28.dp).border(1.dp, c.hairline).drawBehindWithHatch(c.ink40.copy(alpha = 0.2f)))
                Spacer(Modifier.height(8.dp))
                Text("without access · ledger shows ≈ estimates · hatch until granted", style = Caliper.type.dataS, color = c.ink40)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isGranted) {
            StampBadge("GRANTED · CHANNEL LIVE", color = c.accent, rotation = -1.5f, animateIn = true)
            Spacer(Modifier.height(12.dp))
            HardKey("CONTINUE → CAMERA", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = onGranted)
            Spacer(Modifier.height(8.dp))
            Text("usage roster will populate in PROCESSES · ledger", style = Caliper.type.meta, color = c.ink40)
        } else {
            HardKey("GRANT USAGE ACCESS", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    } catch (_: Exception) {}
                }
            })
            Spacer(Modifier.height(8.dp))
            HardKey("SKIP (≈ ESTIMATES)", variant = HardKeyVariant.SECONDARY, modifier = Modifier.fillMaxWidth(), onClick = onSkip)
            Spacer(Modifier.height(8.dp))
            Text("skip → ledger shows ≈ · grant later in Settings · no auto popup", style = Caliper.type.meta, color = c.ink40)
        }
    }
}

@Composable
private fun OverlaySheet(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val c = Caliper.colors
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val haptics = rememberCaliperHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = Settings.canDrawOverlays(context)
                if (now && !isGranted) {
                    isGranted = true
                    haptics.confirm()
                } else if (!now && isGranted) {
                    isGranted = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text("03 · OVERLAY", style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(8.dp))
        Text("Calibration.", style = Caliper.type.display1, color = c.ink)
        Spacer(Modifier.height(16.dp))
        Text(
            "The HUD module floats over other apps (FPS, CPU, temp). Grant display-over-apps — the system sheet appears only when you tap GRANT. No auto popup.",
            style = Caliper.type.body, color = c.ink60
        )
        Spacer(Modifier.height(16.dp))
        OverlayDiagram(Modifier.fillMaxWidth().height(110.dp).background(c.panel).border(1.dp, c.hairline).padding(12.dp))
        Spacer(Modifier.height(16.dp))
        PanelCard(
            title = "FIG. 2 — HUD OVERLAY",
            status = {
                if (isGranted) StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                else Text("LOCKED", style = Caliper.type.meta, color = c.fault)
            }
        ) {
            if (isGranted) {
                Text("overlay permission granted · HUD can float over games", style = Caliper.type.dataS, color = c.ink60)
            } else {
                Box(Modifier.fillMaxWidth().height(28.dp).border(1.dp, c.hairline).drawBehindWithHatch(c.ink40.copy(alpha = 0.2f)))
                Spacer(Modifier.height(8.dp))
                Text("without overlay · HUD disabled · hatch until granted", style = Caliper.type.dataS, color = c.ink40)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isGranted) {
            StampBadge("GRANTED · CHANNEL LIVE", color = c.accent, rotation = -1.5f, animateIn = true)
            Spacer(Modifier.height(12.dp))
            HardKey("CONTINUE → ROOT", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = onGranted)
            Spacer(Modifier.height(8.dp))
            Text("HUD available in OVERLAY · per-app profiles", style = Caliper.type.meta, color = c.ink40)
        } else {
            HardKey("GRANT OVERLAY", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (_: Exception) {
                    try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) } catch (_: Exception) {}
                }
            })
            Spacer(Modifier.height(8.dp))
            HardKey("SKIP", variant = HardKeyVariant.SECONDARY, modifier = Modifier.fillMaxWidth(), onClick = onSkip)
            Spacer(Modifier.height(8.dp))
            Text("skip → HUD stays disabled · grant later in Settings → no auto popup", style = Caliper.type.meta, color = c.ink40)
        }
    }
}

@Composable
private fun CameraCalibSheet(
    onGranted: () -> Unit,
    onSkip: () -> Unit
) {
    val c = Caliper.colors
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showRationale by remember { mutableStateOf(false) }
    var showPermanentlyDenied by remember { mutableStateOf(false) }
    val haptics = rememberCaliperHaptics()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        isGranted = granted
        if (granted) {
            haptics.confirm()
            onGranted()
        } else {
            val shouldShow = try {
                (context as? androidx.activity.ComponentActivity)?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            } catch (_: Exception) { false }
            showRationale = true
            showPermanentlyDenied = !shouldShow
            haptics.tick()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (now != isGranted) {
                    isGranted = now
                    if (now) {
                        showRationale = false
                        showPermanentlyDenied = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text("02 · CAMERA", style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(8.dp))
        Text("Calibration.", style = Caliper.type.display1, color = c.ink)
        Spacer(Modifier.height(16.dp))
        Text(
            "DeviceInsight inventories your camera modules — lenses, resolutions, focal lengths, available focus modes and stabilization. It reads the hardware roster only; the shutter never opens.",
            style = Caliper.type.body, color = c.ink60
        )
        Spacer(Modifier.height(16.dp))
        CameraDiagram(
            modifier = Modifier.fillMaxWidth().height(120.dp)
                .background(c.panel).border(1.dp, c.hairline)
                .padding(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        PanelCard(
            title = "FIG. 1 — CAMERA ROSTER",
            status = {
                if (isGranted) {
                    StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                } else {
                    Text("LOCKED", style = Caliper.type.meta, color = c.fault)
                }
            }
        ) {
            if (isGranted) {
                Text("optics inventory populated · resolution, focal, AF modes, OIS, zoom", style = Caliper.type.dataS, color = c.ink60)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(8.dp).background(c.channel(Channels.STORAGE)))
                    Text("CAM · hardware roster", style = Caliper.type.meta, color = c.ink60)
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(28.dp)
                        .border(1.dp, c.hairline)
                        .drawBehindWithHatch(c.ink40.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(8.dp))
                Text("roster shows CHANNEL LOCKED · hatch pattern until granted", style = Caliper.type.dataS, color = c.ink40)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (isGranted) {
            StampBadge("GRANTED · CHANNEL LIVE", color = c.accent, rotation = -1.5f, animateIn = true)
            Spacer(Modifier.height(12.dp))
            HardKey("CONTINUE → OVERLAY", variant = HardKeyVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = onGranted)
            Spacer(Modifier.height(8.dp))
            Text("camera roster will populate in DEVICE · HARDWARE", style = Caliper.type.meta, color = c.ink40)
        } else {
            if (showRationale) {
                MarginNote(
                    message = if (showPermanentlyDenied)
                        "Camera was denied. You can enable it later in Settings → Permissions → Camera, or continue with a locked roster."
                    else
                        "Camera access was not granted. The roster stays locked (hatched). You can grant later from Settings — no system popup will reappear on its own.",
                    title = "NOTE 002",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            HardKey("GRANT CAMERA", variant = HardKeyVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = { launcher.launch(Manifest.permission.CAMERA) })
            Spacer(Modifier.height(8.dp))
            HardKey("SKIP — LIMITED ROSTER", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
                onClick = onSkip)
            Spacer(Modifier.height(8.dp))
            Text("skip → DEVICE · HARDWARE shows CHANNEL LOCKED · hatched", style = Caliper.type.meta, color = c.ink40)
            if (showPermanentlyDenied) {
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
    }
}

private fun Modifier.drawBehindWithHatch(color: androidx.compose.ui.graphics.Color): Modifier = this.then(
    Modifier.drawBehind {
        hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, color)
    }
)

@Composable
private fun UsageDiagram(modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val phoneW = size.width * 0.30f
        val phoneH = size.height * 0.90f
        val phoneLeft = size.width * 0.22f
        val phoneTop = (size.height - phoneH) / 2
        drawRoundRect(c.ink, topLeft = Offset(phoneLeft, phoneTop), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(c.hairline, topLeft = Offset(phoneLeft + 4.dp.toPx(), phoneTop + 6.dp.toPx()), size = Size(phoneW - 8.dp.toPx(), phoneH - 12.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = Stroke(1.dp.toPx()))
        // settings gear
        val gearX = phoneLeft + phoneW * 0.5f
        val gearY = phoneTop + phoneH * 0.5f
        drawCircle(c.ink60, radius = 6.dp.toPx(), center = Offset(gearX, gearY), style = Stroke(1.2.dp.toPx()))
        // arrow to ledger
        val arrowStartX = phoneLeft + phoneW + 14.dp.toPx()
        val arrowEndX = arrowStartX + 22.dp.toPx()
        val arrowY = gearY
        drawLine(c.ink, Offset(arrowStartX, arrowY), Offset(arrowEndX, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(arrowEndX - 4.dp.toPx(), arrowY - 4.dp.toPx()), Offset(arrowEndX, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(arrowEndX - 4.dp.toPx(), arrowY + 4.dp.toPx()), Offset(arrowEndX, arrowY), 1.dp.toPx())
        // ledger preview
        val previewLeft = arrowEndX + 8.dp.toPx()
        val previewW = size.width - previewLeft - 8.dp.toPx()
        if (previewW > 0) {
            drawRect(c.panel, topLeft = Offset(previewLeft, phoneTop + 12.dp.toPx()), size = Size(previewW, phoneH - 24.dp.toPx()))
            drawRect(c.hairline, topLeft = Offset(previewLeft, phoneTop + 12.dp.toPx()), size = Size(previewW, phoneH - 24.dp.toPx()), style = Stroke(1.dp.toPx()))
            // hatch for estimated
            val hatchH = 18.dp.toPx()
            hatch(Rect(previewLeft + 4.dp.toPx(), phoneTop + 20.dp.toPx(), previewLeft + previewW - 4.dp.toPx(), phoneTop + 20.dp.toPx() + hatchH), HatchPattern.DIAGONAL, c.ink40.copy(alpha = 0.25f))
            drawRect(c.hairline, topLeft = Offset(previewLeft + 4.dp.toPx(), phoneTop + 20.dp.toPx()), size = Size(previewW - 8.dp.toPx(), hatchH), style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
private fun OverlayDiagram(modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val phoneW = size.width * 0.30f
        val phoneH = size.height * 0.90f
        val phoneLeft = size.width * 0.22f
        val phoneTop = (size.height - phoneH) / 2
        drawRoundRect(c.ink, topLeft = Offset(phoneLeft, phoneTop), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()), style = Stroke(stroke))
        // HUD brackets
        val hudLeft = phoneLeft + phoneW + 12.dp.toPx()
        val hudTop = phoneTop + 18.dp.toPx()
        val hudW = size.width - hudLeft - 12.dp.toPx()
        val hudH = 42.dp.toPx()
        // brackets
        val br = 6.dp.toPx()
        drawLine(c.ink, Offset(hudLeft, hudTop), Offset(hudLeft + br, hudTop), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft, hudTop), Offset(hudLeft, hudTop + br), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft + hudW, hudTop), Offset(hudLeft + hudW - br, hudTop), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft + hudW, hudTop), Offset(hudLeft + hudW, hudTop + br), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft, hudTop + hudH), Offset(hudLeft + br, hudTop + hudH), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft, hudTop + hudH - br), Offset(hudLeft, hudTop + hudH), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft + hudW, hudTop + hudH), Offset(hudLeft + hudW - br, hudTop + hudH), 1.5.dp.toPx())
        drawLine(c.ink, Offset(hudLeft + hudW, hudTop + hudH - br), Offset(hudLeft + hudW, hudTop + hudH), 1.5.dp.toPx())
        drawRect(c.panel, topLeft = Offset(hudLeft + 2.dp.toPx(), hudTop + 2.dp.toPx()), size = Size(hudW - 4.dp.toPx(), hudH - 4.dp.toPx()))
        drawRect(c.hairline, topLeft = Offset(hudLeft + 2.dp.toPx(), hudTop + 2.dp.toPx()), size = Size(hudW - 4.dp.toPx(), hudH - 4.dp.toPx()), style = Stroke(1.dp.toPx()))
        // arrow from phone to HUD
        val arrowY = phoneTop + phoneH * 0.5f
        drawLine(c.ink, Offset(phoneLeft + phoneW + 4.dp.toPx(), arrowY), Offset(hudLeft, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(hudLeft - 4.dp.toPx(), arrowY - 4.dp.toPx()), Offset(hudLeft, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(hudLeft - 4.dp.toPx(), arrowY + 4.dp.toPx()), Offset(hudLeft, arrowY), 1.dp.toPx())
    }
}

@Composable
private fun CameraDiagram(modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val phoneW = size.width * 0.32f
        val phoneH = size.height * 0.92f
        val phoneLeft = (size.width - phoneW) / 2
        val phoneTop = (size.height - phoneH) / 2
        drawRoundRect(color = c.ink, topLeft = Offset(phoneLeft, phoneTop), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(color = c.hairline, topLeft = Offset(phoneLeft + 4.dp.toPx(), phoneTop + 6.dp.toPx()), size = Size(phoneW - 8.dp.toPx(), phoneH - 12.dp.toPx()), cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()), style = Stroke(1.dp.toPx()))
        val islandW = phoneW * 0.62f
        val islandH = 18.dp.toPx()
        val islandLeft = phoneLeft + 8.dp.toPx()
        val islandTop = phoneTop + 10.dp.toPx()
        drawRoundRect(color = c.ink60, topLeft = Offset(islandLeft, islandTop), size = Size(islandW, islandH), cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()), style = Stroke(1.dp.toPx()))
        val lensR = 5.dp.toPx()
        val lensY = islandTop + islandH / 2
        val lensSpacing = 16.dp.toPx()
        val firstX = islandLeft + 10.dp.toPx()
        for (i in 0 until 3) {
            val cx = firstX + i * lensSpacing
            drawCircle(color = c.ink, radius = lensR, center = Offset(cx, lensY), style = Stroke(1.2.dp.toPx()))
            drawCircle(color = c.accent, radius = 1.5.dp.toPx(), center = Offset(cx, lensY))
        }
        val arrowStartX = islandLeft + islandW + 12.dp.toPx()
        val arrowMidX = arrowStartX + 22.dp.toPx()
        val arrowY = lensY
        drawLine(c.ink, Offset(arrowStartX, arrowY), Offset(arrowMidX, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(arrowMidX - 4.dp.toPx(), arrowY - 4.dp.toPx()), Offset(arrowMidX, arrowY), 1.dp.toPx())
        drawLine(c.ink, Offset(arrowMidX - 4.dp.toPx(), arrowY + 4.dp.toPx()), Offset(arrowMidX, arrowY), 1.dp.toPx())
        val previewLeft = arrowMidX + 8.dp.toPx()
        val previewTop = arrowY - 14.dp.toPx()
        val previewW = size.width - previewLeft - 8.dp.toPx()
        val previewH = 28.dp.toPx()
        if (previewW > 0) {
            drawRect(c.panel, topLeft = Offset(previewLeft, previewTop), size = Size(previewW.coerceAtLeast(0f), previewH))
            drawRect(c.hairline, topLeft = Offset(previewLeft, previewTop), size = Size(previewW.coerceAtLeast(0f), previewH), style = Stroke(1.dp.toPx()))
            val segW = (previewW - 4.dp.toPx()) / 2
            drawRect(c.channel(Channels.STORAGE), topLeft = Offset(previewLeft + 2.dp.toPx(), previewTop + 2.dp.toPx()), size = Size(segW - 2.dp.toPx(), previewH - 4.dp.toPx()))
            val hatchRect = Rect(previewLeft + 2.dp.toPx() + segW, previewTop + 2.dp.toPx(), previewLeft + previewW - 2.dp.toPx(), previewTop + previewH - 2.dp.toPx())
            hatch(hatchRect, HatchPattern.DOTS, c.ink40.copy(alpha = 0.35f))
            drawLine(c.hairline, Offset(previewLeft + segW + 1.dp.toPx(), previewTop), Offset(previewLeft + segW + 1.dp.toPx(), previewTop + previewH), 1.dp.toPx())
        }
    }
}

@Composable
private fun CalibSheet(
    num: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    skipLabel: String? = null,
    onSkip: () -> Unit = {},
    onMedium: (() -> Unit)? = null
) {
    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text(num, style = Caliper.type.meta, color = Caliper.colors.ink40)
        Spacer(Modifier.height(8.dp))
        Text(title, style = Caliper.type.display1, color = Caliper.colors.ink)
        Spacer(Modifier.height(16.dp))
        Text(body, style = Caliper.type.body, color = Caliper.colors.ink60)
        Spacer(Modifier.height(24.dp))
        onMedium?.invoke()
        HardKey(actionLabel, variant = HardKeyVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(), onClick = onAction)
        if (skipLabel != null) {
            Spacer(Modifier.height(8.dp))
            HardKey(skipLabel, variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(), onClick = onSkip)
        }
    }
}

@Composable
private fun CertificateSheet(deviceName: String, onFinish: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp)) {
        Text("DOC № DI-0001 · REV 2.0", style = Caliper.type.meta, color = Caliper.colors.ink40)
        Spacer(Modifier.height(8.dp))
        Text("Calibration.", style = Caliper.type.display1, color = Caliper.colors.ink)
        Spacer(Modifier.height(12.dp))
        Text(deviceName, style = Caliper.type.dataM, color = Caliper.colors.ink)
        Text("date · ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}",
            style = Caliper.type.dataS, color = Caliper.colors.ink60)
        Spacer(Modifier.height(12.dp))
        StampBadge("CALIBRATED · DI-0001")
        Spacer(Modifier.height(20.dp))
        HardKey("INSPECT THE INSTRUMENT", variant = HardKeyVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth(), onClick = onFinish)
    }
}
