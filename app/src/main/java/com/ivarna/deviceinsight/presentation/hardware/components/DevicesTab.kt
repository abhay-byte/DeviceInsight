package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.CameraInfo
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.UsbDeviceInfo

@Composable
fun DevicesTab(info: HardwareInfo) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        DevicesHeader(
            cameraCount = info.cameras.size,
            usbCount = info.usbDevices.size
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (info.cameras.isNotEmpty()) {
            CategoryHeader(name = "Cameras", count = info.cameras.size)
            Spacer(modifier = Modifier.height(10.dp))
            info.cameras.forEachIndexed { index, camera ->
                CameraCard(camera = camera)
                if (index < info.cameras.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (info.usbDevices.isNotEmpty()) {
            CategoryHeader(name = "USB Devices", count = info.usbDevices.size)
            Spacer(modifier = Modifier.height(10.dp))
            info.usbDevices.forEachIndexed { index, usb ->
                UsbCard(usb = usb)
                if (index < info.usbDevices.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (info.cameras.isEmpty() && info.usbDevices.isEmpty()) {
            EmptyDevicesState()
        }

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun DevicesHeader(
    cameraCount: Int,
    usbCount: Int
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.12f),
                        secondary.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.3f),
                        secondary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(0.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.25f),
                                secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connected Devices",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$cameraCount",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "cameras · ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$usbCount",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "USB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraCard(camera: CameraInfo) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val facingColor = when {
        camera.facing.contains("Rear", ignoreCase = true) -> primary
        camera.facing.contains("Front", ignoreCase = true) -> secondary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val facingIcon = when {
        camera.facing.contains("Rear", ignoreCase = true) -> Icons.Filled.CameraAlt
        camera.facing.contains("Front", ignoreCase = true) -> Icons.Filled.Cameraswitch
        else -> Icons.Filled.Camera
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        facingColor.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        facingColor.copy(alpha = 0.25f),
                        secondary.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(facingColor.copy(alpha = 0.14f))
                        .border(1.dp, facingColor.copy(alpha = 0.3f), RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = facingIcon,
                        contentDescription = null,
                        tint = facingColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${camera.facing} Camera",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = camera.id,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .background(facingColor.copy(alpha = 0.14f))
                        .border(0.5.dp, facingColor.copy(alpha = 0.3f), RoundedCornerShape(0.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = camera.resolution.split(" ").firstOrNull() ?: camera.resolution,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = facingColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Key specs row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CameraSpecChip(
                    label = "VIDEO",
                    value = camera.videoResolution.split(" ").firstOrNull() ?: "—",
                    icon = Icons.Filled.Videocam
                )
                CameraSpecChip(
                    label = "FOCAL",
                    value = camera.focalLength,
                    icon = Icons.Filled.ZoomOutMap
                )
                CameraSpecChip(
                    label = "FLASH",
                    value = if (camera.flashSupported) "Yes" else "No",
                    icon = if (camera.flashSupported) Icons.Filled.FlashOn else Icons.Filled.FlashAuto
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            // Feature pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeaturePill(
                    label = "OIS",
                    enabled = camera.videoStabilizationSupported
                )
                FeaturePill(
                    label = "ZOOM",
                    enabled = camera.zoomSupported
                )
                FeaturePill(
                    label = "AE LOCK",
                    enabled = camera.autoExposureLockingSupported
                )
                FeaturePill(
                    label = "AWB LOCK",
                    enabled = camera.autoWhiteBalanceLockingSupported
                )
            }
            if (camera.focusModes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "FOCUS MODES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 9.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    camera.focusModes.take(4).forEach { mode ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(0.dp))
                                .background(facingColor.copy(alpha = 0.08f))
                                .border(0.5.dp, facingColor.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = mode.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = facingColor.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSpecChip(
    label: String,
    value: String,
    icon: ImageVector
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(primary.copy(alpha = 0.08f))
            .border(0.5.dp, primary.copy(alpha = 0.18f), RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        fontSize = 8.sp
                    ),
                    color = primary.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = primary
            )
        }
    }
}

@Composable
private fun FeaturePill(
    label: String,
    enabled: Boolean
) {
    val accent = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(accent.copy(alpha = if (enabled) 0.14f else 0.06f))
            .border(0.5.dp, accent.copy(alpha = if (enabled) 0.3f else 0.15f), RoundedCornerShape(0.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            ),
            color = accent.copy(alpha = if (enabled) 1f else 0.5f)
        )
    }
}

@Composable
private fun UsbCard(usb: UsbDeviceInfo) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tertiary.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tertiary.copy(alpha = 0.25f),
                        primary.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(tertiary.copy(alpha = 0.14f))
                        .border(1.dp, tertiary.copy(alpha = 0.3f), RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Usb,
                        contentDescription = null,
                        tint = tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = usb.productName.ifBlank { "USB Device" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = usb.manufacturerName.ifBlank { "Unknown Manufacturer" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .background(tertiary.copy(alpha = 0.14f))
                        .border(0.5.dp, tertiary.copy(alpha = 0.3f), RoundedCornerShape(0.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = usb.deviceId,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = tertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UsbSpecChip(label = "CLASS", value = usb.deviceClass)
                UsbSpecChip(label = "SPEED", value = usb.speed)
                UsbSpecChip(label = "USB", value = usb.usbVersion)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UsbSpecChip(label = "SERIAL", value = usb.serialNumber, mono = true)
                UsbSpecChip(label = "REVISION", value = usb.revision, mono = true)
            }
        }
    }
}

@Composable
private fun UsbSpecChip(
    label: String,
    value: String,
    mono: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(primary.copy(alpha = 0.08f))
            .border(0.5.dp, primary.copy(alpha = 0.18f), RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = primary.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = primary.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun EmptyDevicesState() {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            .border(1.dp, primary.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Usb,
                contentDescription = null,
                tint = primary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No connected devices",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
