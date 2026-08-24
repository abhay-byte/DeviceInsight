package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.CameraInfo
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.UsbDeviceInfo
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.components.*

@Composable
fun DevicesTab(info: HardwareInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DevicesHeader(
            cameraCount = info.cameras.size,
            usbCount = info.usbDevices.size
        )

        if (info.cameras.isNotEmpty()) {
            SectionLabel(name = "CAMERAS", count = info.cameras.size)
            info.cameras.forEach { camera ->
                // key by id for stability
                CameraCard(camera = camera)
            }
        }

        if (info.usbDevices.isNotEmpty()) {
            SectionLabel(name = "USB DEVICES", count = info.usbDevices.size)
            info.usbDevices.forEach { usb ->
                UsbCard(usb = usb)
            }
        }

        if (info.cameras.isEmpty() && info.usbDevices.isEmpty()) {
            EmptyState(
                title = "NO SIGNAL",
                message = "no cameras or USB peripherals detected",
            )
        }
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
