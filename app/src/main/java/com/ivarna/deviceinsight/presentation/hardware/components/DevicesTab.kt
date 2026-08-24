package com.ivarna.deviceinsight.presentation.hardware.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ivarna.deviceinsight.domain.model.CameraInfo
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.UsbDeviceInfo
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.ui.caliper.hatch
import com.ivarna.deviceinsight.ui.caliper.rememberCaliperHaptics

@Composable
fun DevicesTab(info: HardwareInfo, onCameraGrantedReload: () -> Unit = {}) {
    val context = LocalContext.current
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var showRationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        if (granted) { haptics.confirm(); onCameraGrantedReload() } else { showRationale = true; haptics.tick() }
    }
    // re-check on resume (e.g., returning from Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val now = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                val wasGranted = cameraGranted
                cameraGranted = now
                if (now) showRationale = false
                if (now && !wasGranted) onCameraGrantedReload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DevicesHeader(
            cameraCount = if (cameraGranted) info.cameras.size else 0,
            usbCount = info.usbDevices.size
        )

        // CAMERAS — channel-locked hatch when permission not granted (CALIPER §5.14 + §4.5)
        if (!cameraGranted) {
            SectionLabel(name = "CAMERAS", count = 0)
            PanelCard(
                title = "CAMERAS · CHANNEL LOCKED",
                status = { Text("LOCKED", style = Caliper.type.meta, color = c.fault) },
                modifier = Modifier
                    .border(1.dp, c.hairline)
                    .drawBehind { hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, c.ink40.copy(alpha = 0.12f)) }
            ) {
                Text(
                    "Camera roster is locked — grant camera to inventory lenses, resolutions and focal lengths. The shutter never opens; only the hardware list is read.",
                    style = Caliper.type.dataS, color = c.ink60
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier.fillMaxWidth().height(14.dp)
                        .border(1.dp, c.hairline)
                        .drawBehind { hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, c.ink40.copy(alpha = 0.25f)) }
                )
                Spacer(Modifier.height(10.dp))
                if (showRationale) {
                    MarginNote(
                        message = "Camera not granted. Use the key below — this is the only place that opens the system popup. No auto dialog elsewhere.",
                        title = "NOTE 002"
                    )
                    Spacer(Modifier.height(10.dp))
                }
                HardKey("GRANT CAMERA", variant = HardKeyVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { launcher.launch(Manifest.permission.CAMERA) })
                Spacer(Modifier.height(8.dp))
                Text("or grant in Settings → 02 PERMISSIONS · also appears only on explicit tap", style = Caliper.type.meta, color = c.ink40)
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
        } else if (info.cameras.isNotEmpty()) {
            SectionLabel(name = "CAMERAS", count = info.cameras.size)
            info.cameras.forEach { camera ->
                // key by id for stability
                CameraCard(camera = camera)
            }
        } else {
            // permission granted but no cameras found (e.g., emulator)
            SectionLabel(name = "CAMERAS", count = 0)
            Box(
                Modifier.fillMaxWidth()
                    .border(1.dp, c.hairline)
                    .background(c.panel)
                    .padding(12.dp)
            ) {
                Text("NO SIGNAL — no camera modules enumerated", style = Caliper.type.dataS, color = c.ink60)
            }
        }

        if (info.usbDevices.isNotEmpty()) {
            SectionLabel(name = "USB DEVICES", count = info.usbDevices.size)
            info.usbDevices.forEach { usb ->
                UsbCard(usb = usb)
            }
        }

        if (cameraGranted && info.cameras.isEmpty() && info.usbDevices.isEmpty()) {
            EmptyState(
                title = "NO SIGNAL",
                message = "no cameras or USB peripherals detected",
            )
        }
        // when locked we already show the Channel Locked card above; no duplicate EmptyState
    }
}

@Composable
private fun DevicesHeader(
    cameraCount: Int,
    usbCount: Int
) {
    val c = Caliper.colors
    PanelCard(
        title = "MISC · CONNECTED DEVICES",
        status = {
            Text(
                text = "$cameraCount cameras · $usbCount USB",
                style = Caliper.type.meta,
                color = c.ink40
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                OdometerText(
                    text = "$cameraCount",
                    style = Caliper.type.readoutL,
                    color = c.ink
                )
                Text("CAMERAS", style = Caliper.type.meta, color = c.ink60)
            }
            Box(
                modifier = Modifier.width(1.dp).height(40.dp).background(c.hairline)
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                OdometerText(
                    text = "$usbCount",
                    style = Caliper.type.readoutL,
                    color = c.ink
                )
                Text("USB", style = Caliper.type.meta, color = c.ink60)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "FIG. 1 — camera & USB inventory · dotted leaders are spec",
            style = Caliper.type.meta,
            color = c.ink40
        )
    }
}

@Composable
private fun SectionLabel(name: String, count: Int) {
    val c = Caliper.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Caliper.type.meta, color = c.ink)
            Spacer(Modifier.width(8.dp))
            Text("· $count", style = Caliper.type.meta, color = c.ink40)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        DoubleRule()
    }
}

@Composable
private fun CameraCard(camera: CameraInfo) {
    val c = Caliper.colors
    // memoized facing label
    val facingLabel = remember(camera.facing, camera.id) {
        "${camera.facing.uppercase()} · CAM ${camera.id}"
    }
    val resolutionBadge = remember(camera.resolution) {
        camera.resolution.split(" ").firstOrNull() ?: camera.resolution
    }

    PanelCard(
        title = facingLabel,
        status = {
            Text(resolutionBadge, style = Caliper.type.meta, color = c.ink40)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecRow("video", camera.videoResolution)
            SpecRow("focal", camera.focalLength)
            FeatureRow("FLASH", camera.flashSupported)

            // Feature matrix — ledger style, no pill gradients
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                FeatureRow("OIS — video stabilization", camera.videoStabilizationSupported)
                FeatureRow("ZOOM", camera.zoomSupported)
                FeatureRow("AE LOCK", camera.autoExposureLockingSupported)
                FeatureRow("AWB LOCK", camera.autoWhiteBalanceLockingSupported)
            }

            if (camera.focusModes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("FOCUS MODES", style = Caliper.type.meta, color = c.ink60)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    camera.focusModes.take(4).forEach { mode ->
                        Box(
                            modifier = Modifier
                                .background(c.panel)
                                .border(1.dp, c.hairline)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = mode.replace("_", "-").uppercase(),
                                style = Caliper.type.meta.copy(fontSize = 9.sp),
                                color = c.ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbCard(usb: UsbDeviceInfo) {
    val c = Caliper.colors
    val title = remember(usb.productName, usb.deviceId) {
        usb.productName.ifBlank { "USB DEVICE" }.uppercase()
    }
    PanelCard(
        title = title,
        status = {
            Text(usb.deviceId, style = Caliper.type.meta, color = c.ink40)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = usb.manufacturerName.ifBlank { "unknown manufacturer" },
                style = Caliper.type.meta,
                color = c.ink60
            )
            Spacer(Modifier.height(2.dp))
            SpecRow("class", usb.deviceClass.ifBlank { "—" })
            SpecRow("speed", usb.speed.ifBlank { "—" })
            SpecRow("usb", usb.usbVersion.ifBlank { "—" })
            SpecRow("serial", usb.serialNumber.ifBlank { "—" })
            SpecRow("revision", usb.revision.ifBlank { "—" })
        }
    }
}
