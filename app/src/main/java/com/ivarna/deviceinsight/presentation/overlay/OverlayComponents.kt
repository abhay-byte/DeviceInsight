package com.ivarna.deviceinsight.presentation.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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


// ─────────────────────────────────────────────────────────────────────────────
// Premium metric row card with drag handle + custom switch
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OverlayMetricCard(
    metric: OverlayMetricItem,
    isDragging: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        animationSpec = tween(200),
        label = "metricElevation"
    )
    val accent = metricColor(metric.category)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(0.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isDragging) 0.14f else 0.06f),
                            MaterialTheme.colorScheme.surface.copy(alpha = if (isDragging) 0.5f else 0.25f)
                        )
                    )
                )
                .border(
                    width = if (isDragging) 1.5.dp else 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = if (isDragging) 0.5f else 0.25f),
                            secondary.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Drag handle
                Icon(
                    imageVector = Icons.Filled.DragIndicator,
                    contentDescription = "Reorder",
                    tint = primary.copy(alpha = if (isDragging) 1f else 0.45f),
                    modifier = Modifier.size(20.dp)
                )

                // Category icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(accent.copy(alpha = 0.14f))
                        .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = metricIcon(metric.icon),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metric.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = metric.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 9.sp
                        ),
                        color = accent.copy(alpha = 0.8f)
                    )
                }

                PremiumSwitch(
                    checked = metric.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
private fun PremiumSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor by animateColorAsState(
        targetValue = if (checked) primary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "switchTrack"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) primary
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        label = "switchThumb"
    )

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(trackColor)
            .border(0.5.dp, primary.copy(alpha = 0.2f), RoundedCornerShape(0.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumbColor)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(12.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FPS mode pill selector
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FpsModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    active: Boolean,
    description: String,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> primary.copy(alpha = 0.5f)
            else -> primary.copy(alpha = 0.15f)
        },
        animationSpec = tween(200),
        label = "fpsPillBorder"
    )

    val bgBrush = if (selected) {
        Brush.linearGradient(
            colors = listOf(
                primary.copy(alpha = 0.18f),
                secondary.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
            )
        )
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(bgBrush)
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(0.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    fontSize = 11.sp
                ),
                color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = if (active) "Active" else "Inactive",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.5.sp
            ),
            color = if (active) primary.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Interactive Permission Action Card with instructions & explicit CTA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PermissionActionCard(
    title: String,
    description: String,
    instructions: List<String>,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean,
    actionText: String,
    onActionClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val tertiary = MaterialTheme.colorScheme.tertiary

    val accentColor = when {
        isGranted -> primary
        isRequired -> error
        else -> tertiary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isGranted) 0.04f else 0.08f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isGranted) 0.3f else 0.5f),
                        secondary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(0.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(0.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(0.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isGranted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = if (isGranted) "ALLOWED" else if (isRequired) "REQUIRED" else "RECOMMENDED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                fontSize = 9.sp
                            ),
                            color = accentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )

            if (!isGranted && instructions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(0.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(0.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "HOW TO ENABLE:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    instructions.forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isGranted) {
                // Prominent CTA Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(0.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.25f),
                                    secondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
                        .clickable { onActionClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Permission granted and active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = primary
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission status chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PermissionChip(
    label: String,
    granted: Boolean,
    onClick: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val accent = if (granted) primary else error

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(0.dp))
            .background(accent.copy(alpha = if (granted) 0.12f else 0.18f))
            .border(
                width = if (granted) 0.8.dp else 1.2.dp,
                color = accent.copy(alpha = if (granted) 0.4f else 0.7f),
                shape = RoundedCornerShape(0.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (granted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (granted) label else "$label (Allow ↗)",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    fontSize = 11.sp
                ),
                color = if (granted) accent else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
fun metricColor(category: String): Color = when (category) {
    "System" -> Color(0xFF4FC3F7)
    "Performance" -> Color(0xFF00E5FF)
    "Memory" -> Color(0xFFBA68C8)
    "Power" -> Color(0xFFFFC400)
    "Thermal" -> Color(0xFFFF8A65)
    "Display" -> Color(0xFF81C784)
    "Network" -> Color(0xFF64B5F6)
    else -> Color(0xFF9E9E9E)
}

fun metricIcon(iconName: String): ImageVector = when (iconName) {
    "schedule" -> Icons.Filled.AccessTime
    "memory" -> Icons.Filled.Memory
    "show_chart" -> Icons.Filled.ShowChart
    "thermostat" -> Icons.Filled.Thermostat
    "speed" -> Icons.Filled.Speed
    "storage" -> Icons.Filled.Storage
    "swap_horiz" -> Icons.Filled.SwapHoriz
    "bolt" -> Icons.Filled.Bolt
    "trending_up" -> Icons.Filled.TrendingUp
    "battery_full" -> Icons.Filled.BatteryFull
    "device_thermostat" -> Icons.Filled.DeviceThermostat
    "videogame_asset" -> Icons.Filled.VideogameAsset
    "analytics" -> Icons.Filled.Analytics
    "network_check" -> Icons.Filled.NetworkCheck
    "apps" -> Icons.Filled.Apps
    else -> Icons.Filled.Bolt
}
