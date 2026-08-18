package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.MountPoint
import com.ivarna.deviceinsight.utils.FormattingUtils

@Composable
fun StorageTab(info: HardwareInfo) {
    val dir = info.directoryInfo
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val internalUsed = (info.totalStorage - info.availableStorage).coerceAtLeast(0L)
    val internalTotal = info.totalStorage
    val internalFree = info.availableStorage
    val internalFraction = if (internalTotal > 0) internalUsed.toFloat() / internalTotal else 0f

    val externalUsed = (info.totalExternalStorage - info.availableExternalStorage).coerceAtLeast(0L)
    val externalTotal = info.totalExternalStorage
    val externalFree = info.availableExternalStorage
    val externalFraction = if (externalTotal > 0) externalUsed.toFloat() / externalTotal else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        StorageHeader(
            internalUsed = internalUsed,
            internalTotal = internalTotal,
            internalFree = internalFree
        )

        Spacer(modifier = Modifier.height(18.dp))

        // ── Internal + External storage usage cards ──
        StorageUsageCard(
            title = "Internal Storage",
            subtitle = "Primary user data partition",
            icon = Icons.Filled.SdStorage,
            accent = primary,
            usedBytes = internalUsed,
            totalBytes = internalTotal,
            freeBytes = internalFree,
            fraction = internalFraction
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (externalTotal > 0) {
            StorageUsageCard(
                title = "External Storage",
                subtitle = "SD card or secondary partition",
                icon = Icons.Filled.Usb,
                accent = tertiary,
                usedBytes = externalUsed,
                totalBytes = externalTotal,
                freeBytes = externalFree,
                fraction = externalFraction
            )
            Spacer(modifier = Modifier.height(20.dp))
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ── Directory paths ──
        SectionHeader(name = "Directory Paths", count = 4)
        Spacer(modifier = Modifier.height(10.dp))
        DirectoryPathCard(label = "Data Directory", path = dir.data)
        Spacer(modifier = Modifier.height(8.dp))
        DirectoryPathCard(label = "Root Directory", path = dir.root)
        Spacer(modifier = Modifier.height(8.dp))
        DirectoryPathCard(label = "Java Home", path = dir.javaHome)
        Spacer(modifier = Modifier.height(8.dp))
        DirectoryPathCard(label = "Download / Cache", path = dir.downloadCache)
        Spacer(modifier = Modifier.height(20.dp))

        // ── Mount points ──
        if (dir.mountPoints.isNotEmpty()) {
            SectionHeader(name = "Mount Points", count = dir.mountPoints.size)
            Spacer(modifier = Modifier.height(10.dp))
            dir.mountPoints.forEachIndexed { index, mount ->
                MountPointCard(mount = mount)
                if (index < dir.mountPoints.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun StorageHeader(
    internalUsed: Long,
    internalTotal: Long,
    internalFree: Long
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val usedFraction = if (internalTotal > 0) internalUsed.toFloat() / internalTotal else 0f
    val percent = (usedFraction * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    primary.copy(alpha = 0.25f),
                                    secondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Storage Overview",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (internalTotal > 0) {
                            "${FormattingUtils.formatFileSize(internalUsed)} of ${FormattingUtils.formatFileSize(internalTotal)}"
                        } else {
                            "No storage detected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Surface(
                    color = when {
                        percent >= 90 -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                        percent >= 70 -> primary.copy(alpha = 0.18f)
                        else -> primary.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = when {
                            percent >= 90 -> MaterialTheme.colorScheme.error
                            else -> primary
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (internalTotal > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                StorageProgressBar(
                    fraction = usedFraction,
                    color = when {
                        percent >= 90 -> MaterialTheme.colorScheme.error
                        percent >= 70 -> primary
                        else -> primary
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Free: ${FormattingUtils.formatFileSize(internalFree)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = primary
                    )
                    Text(
                        text = "Used: ${FormattingUtils.formatFileSize(internalUsed)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageUsageCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    usedBytes: Long,
    totalBytes: Long,
    freeBytes: Long,
    fraction: Float
) {
    val percent = (fraction * 100).toInt()
    val progressColor = when {
        percent >= 90 -> MaterialTheme.colorScheme.error
        percent >= 70 -> accent
        else -> accent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.25f),
                        accent.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (totalBytes > 0) {
                StorageProgressBar(
                    fraction = fraction,
                    color = progressColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StorageMetaChip(
                        label = "Used",
                        value = FormattingUtils.formatFileSize(usedBytes),
                        color = progressColor
                    )
                    StorageMetaChip(
                        label = "Free",
                        value = FormattingUtils.formatFileSize(freeBytes),
                        color = accent
                    )
                    StorageMetaChip(
                        label = "Total",
                        value = FormattingUtils.formatFileSize(totalBytes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Not available on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StorageProgressBar(fraction: Float, color: Color) {
    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedWidth by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(800),
        label = "storageBar"
    )
    LaunchedEffect(fraction) { animTarget = fraction.coerceIn(0f, 1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedWidth)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(color, color.copy(alpha = 0.6f))
                    )
                )
        )
    }
}

@Composable
private fun StorageMetaChip(
    label: String,
    value: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(0.5.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    fontSize = 8.sp
                ),
                color = color.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = color
            )
        }
    }
}

@Composable
private fun SectionHeader(name: String, count: Int) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(primary)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            ),
            color = primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "·  $count",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun DirectoryPathCard(label: String, path: String) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.04f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontSize = 9.sp
                    ),
                    color = secondary
                )
                Text(
                    text = path,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MountPointCard(mount: MountPoint) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val accent = if (mount.isReadOnly) MaterialTheme.colorScheme.tertiary else primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.25f),
                        secondary.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storage,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mount.path,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = mount.device,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
                if (mount.isReadOnly) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "RO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MountMetaChip(label = "FS", value = mount.fileSystem)
                MountMetaChip(
                    label = "Access",
                    value = if (mount.isReadOnly) "Read-Only" else "Read-Write"
                )
            }
        }
    }
}

@Composable
private fun MountMetaChip(label: String, value: String) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(primary.copy(alpha = 0.08f))
            .border(0.5.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
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
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp
                ),
                color = primary.copy(alpha = 0.9f)
            )
        }
    }
}
