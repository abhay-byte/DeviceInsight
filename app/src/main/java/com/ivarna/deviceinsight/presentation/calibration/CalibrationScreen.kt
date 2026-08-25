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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*

/**
 * Two-page onboarding: Welcome → Setup.
 * Replaces the previous multi-step calibration wizard.
 */
@Composable
fun CalibrationScreen(
    initialMedium: Medium?,
    onMedium: (Medium) -> Unit,
    onFinish: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        onMedium(Medium.PAPER)
    }
    Column(
        Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())
    ) {
        when (page) {
            0 -> WelcomePage(onContinue = { page = 1 })
            else -> SetupPage(onFinish = onFinish)
        }
    }
}

@Composable
private fun WelcomePage(onContinue: () -> Unit) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("DeviceInsight", style = Caliper.type.display1.copy(fontSize = 40.sp), color = c.ink)
        Spacer(Modifier.height(12.dp))
        Text(
            "Understand what your device is doing in real time.",
            style = Caliper.type.body, color = c.ink60,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Monitor CPU, memory, GPU, battery, storage, network activity and running processes. You can also use the optional performance overlay while using other apps or games.",
            style = Caliper.type.dataS, color = c.ink60,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        PanelCard(title = "FEATURES") {
            FeatureRow("System performance", "CPU, memory, battery, storage, network with live graphs")
            Spacer(Modifier.height(8.dp))
            FeatureRow("Device hardware", "System, CPU, display, GPU, sensors, and more")
            Spacer(Modifier.height(8.dp))
            FeatureRow("Processes and app activity", "Per-app usage and running process details")
            Spacer(Modifier.height(8.dp))
            FeatureRow("In-game / app overlay", "Optional floating performance panel for games and apps")
        }
        Spacer(Modifier.height(24.dp))
        HardKey("CONTINUE", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = onContinue)
    }
}

@Composable
private fun FeatureRow(title: String, subtitle: String) {
    val c = Caliper.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(c.accent))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = Caliper.type.dataS, color = c.ink)
            Text(subtitle, style = Caliper.type.meta, color = c.ink60)
        }
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
private fun SetupPage(onFinish: () -> Unit) {
    val c = Caliper.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = rememberCaliperHaptics()

    var hasUsage by remember { mutableStateOf(hasUsageAccess(context)) }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showCameraRationale by remember { mutableStateOf(false) }
    var cameraPermanentlyDenied by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) {
            haptics.confirm()
            showCameraRationale = false
            cameraPermanentlyDenied = false
        } else {
            val shouldShow = try {
                (context as? androidx.activity.ComponentActivity)?.let {
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                } ?: false
            } catch (_: Exception) { false }
            showCameraRationale = true
            cameraPermanentlyDenied = !shouldShow
            haptics.tick()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val nowUsage = hasUsageAccess(context)
                if (nowUsage != hasUsage) {
                    hasUsage = nowUsage
                    if (nowUsage) haptics.confirm()
                }
                val nowOverlay = Settings.canDrawOverlays(context)
                if (nowOverlay != hasOverlay) {
                    hasOverlay = nowOverlay
                    if (nowOverlay) haptics.confirm()
                }
                val nowCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (nowCamera != cameraGranted) {
                    cameraGranted = nowCamera
                    if (nowCamera) {
                        showCameraRationale = false
                        cameraPermanentlyDenied = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Set up DeviceInsight", style = Caliper.type.display1, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text("Grant access for the features you want. You can skip and enable later in Settings.", style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(16.dp))

        // Usage access
        PanelCard(
            title = "USAGE ACCESS",
            status = {
                if (hasUsage) StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                else Text("NOT GRANTED", style = Caliper.type.meta, color = c.ink40)
            }
        ) {
            Text("Helps DeviceInsight show per-app activity and more accurate process information.", style = Caliper.type.dataS, color = c.ink60)
            Spacer(Modifier.height(12.dp))
            if (hasUsage) {
                Text("Usage access granted · per-app CPU / memory live", style = Caliper.type.meta, color = c.ink60)
            } else {
                HardKey("ALLOW USAGE ACCESS", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
                    }
                })
            }
        }
        Spacer(Modifier.height(12.dp))

        // Camera
        PanelCard(
            title = "CAMERA",
            status = {
                if (cameraGranted) StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                else Text("NOT GRANTED", style = Caliper.type.meta, color = c.ink40)
            }
        ) {
            Text("Used only to list your camera hardware — lenses, resolutions and capabilities. The camera itself is never opened to take pictures or record.", style = Caliper.type.dataS, color = c.ink60)
            Spacer(Modifier.height(12.dp))
            if (cameraGranted) {
                Text("Camera roster will be available in Device details", style = Caliper.type.meta, color = c.ink60)
            } else {
                if (showCameraRationale) {
                    MarginNote(
                        message = if (cameraPermanentlyDenied)
                            "Camera was denied. You can enable it later in Settings → Permissions → Camera."
                        else
                            "Camera access was not granted. The camera roster will stay unavailable. You can grant later from Settings.",
                        title = "NOTE",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                }
                HardKey("ALLOW CAMERA ACCESS", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = {
                    cameraLauncher.launch(Manifest.permission.CAMERA)
                })
                if (cameraPermanentlyDenied) {
                    Spacer(Modifier.height(8.dp))
                    HardKey("OPEN APP SETTINGS", variant = HardKeyVariant.SECONDARY, modifier = Modifier.fillMaxWidth(), onClick = {
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
        Spacer(Modifier.height(12.dp))

        // Overlay
        PanelCard(
            title = "DISPLAY OVER OTHER APPS",
            status = {
                if (hasOverlay) StampBadge("GRANTED", color = c.accent, rotation = -2f, animateIn = false)
                else Text("NOT GRANTED", style = Caliper.type.meta, color = c.ink40)
            }
        ) {
            Text("Required only for the floating performance overlay.", style = Caliper.type.dataS, color = c.ink60)
            Spacer(Modifier.height(12.dp))
            if (hasOverlay) {
                Text("Overlay permission granted · floating panel can appear", style = Caliper.type.meta, color = c.ink60)
            } else {
                HardKey("ALLOW OVERLAY", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } catch (_: Exception) {
                        try { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) } catch (_: Exception) {}
                    }
                })
            }
        }
        Spacer(Modifier.height(16.dp))

        // Optional capabilities
        Text("OPTIONAL CAPABILITIES", style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(8.dp))
        PanelCard(title = "ROOT / SHIZUKU") {
            Text("Optional advanced features if your device supports them. DeviceInsight works fully without them. You can configure these later from Settings.", style = Caliper.type.dataS, color = c.ink60)
        }
        Spacer(Modifier.height(16.dp))

        HardKey("FINISH SETUP", variant = HardKeyVariant.PRIMARY, modifier = Modifier.fillMaxWidth(), onClick = onFinish)
        Spacer(Modifier.height(8.dp))
        HardKey("SKIP OPTIONAL PERMISSIONS", variant = HardKeyVariant.SECONDARY, modifier = Modifier.fillMaxWidth(), onClick = onFinish)
        Spacer(Modifier.height(8.dp))
        Text("Every permission opens only after you tap its button — nothing is requested automatically.", style = Caliper.type.meta, color = c.ink40)
    }
}
