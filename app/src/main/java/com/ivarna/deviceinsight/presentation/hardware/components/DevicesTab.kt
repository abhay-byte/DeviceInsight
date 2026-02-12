package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun DevicesTab(info: HardwareInfo) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        // Camera Section
        info.cameras.forEach { camera ->
            InfoSection(title = camera.facing) {
                InfoRow("Resolution", camera.resolution)
                InfoRow("Video Resolution", camera.videoResolution)
                InfoRow("Focal Length", camera.focalLength)
                InfoRow("Focus Modes", camera.focusModes.joinToString(", "))
                InfoRow("Video Snapshot", if (camera.videoSnapshotSupported) "Supported" else "Not Supported")
                InfoRow("Video Stabilization", if (camera.videoStabilizationSupported) "Supported" else "Not Supported")
                InfoRow("Zoom", if (camera.zoomSupported) "Supported" else "Not Supported")
                InfoRow("Smooth Zoom", if (camera.smoothZoomSupported) "Supported" else "Not Supported")
                InfoRow("Auto Exposure Locking", if (camera.autoExposureLockingSupported) "Supported" else "Not Supported")
                InfoRow("Auto White Balance Locking", if (camera.autoWhiteBalanceLockingSupported) "Supported" else "Not Supported")
                InfoRow("Flash", if (camera.flashSupported) "Supported" else "Not Supported")
            }
        }

        // USB Section
        info.usbDevices.forEach { usb ->
            InfoSection(title = "USB Device - ${usb.productName}") {
                InfoRow("Manufacturer", usb.manufacturerName)
                InfoRow("Product", usb.productName)
                InfoRow("Serial", usb.serialNumber)
                InfoRow("Device ID", usb.deviceId)
                InfoRow("Device Class", usb.deviceClass)
                InfoRow("Device Protocol", usb.deviceProtocol)
                InfoRow("Revision", usb.revision)
                InfoRow("Supported USB Version", usb.usbVersion)
                InfoRow("Current Speed", usb.speed)
            }
        }
    }
}
