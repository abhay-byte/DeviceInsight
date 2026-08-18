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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
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
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.SensorDetail
import kotlin.math.abs

@Composable
fun SensorsTab(info: HardwareInfo) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        SensorHeader(
            sensorCount = info.sensorCount,
            hasFingerprint = info.fingerprintSensorPresent
        )

        Spacer(modifier = Modifier.height(20.dp))

        CategorySummary(
            sensors = info.sensorDetails,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary
        )

        Spacer(modifier = Modifier.height(22.dp))

        SensorList(sensors = info.sensorDetails)

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun SensorHeader(
    sensorCount: Int,
    hasFingerprint: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

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
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    imageVector = Icons.Filled.Sensors,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sensor Suite",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$sensorCount",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sensorCount == 1) "sensor detected" else "sensors detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (hasFingerprint) {
                Surface(
                    color = primary.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            ),
                            color = primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySummary(
    sensors: List<SensorDetail>,
    primary: Color,
    secondary: Color,
    tertiary: Color
) {
    val categories = listOf("Motion", "Position", "Environment", "Biometric")
    val counts = categories.associateWith { cat ->
        sensors.count { it.category == cat }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { cat ->
            val accent = when (cat) {
                "Motion" -> primary
                "Position" -> secondary
                "Environment" -> tertiary
                else -> MaterialTheme.colorScheme.secondary
            }
            CategoryBadge(
                label = cat,
                count = counts[cat] ?: 0,
                color = accent,
                icon = iconForCategory(cat),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CategoryBadge(
    label: String,
    count: Int,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            ),
            color = color
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                fontSize = 9.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SensorList(sensors: List<SensorDetail>) {
    if (sensors.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No sensors detected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val grouped = sensors.groupBy { it.category }
    val categoryOrder = listOf("Motion", "Position", "Environment", "Biometric", "Other")

    categoryOrder.forEach { category ->
        val list = grouped[category] ?: return@forEach
        Column(modifier = Modifier.fillMaxWidth()) {
            CategoryHeader(name = category, count = list.size)
            Spacer(modifier = Modifier.height(10.dp))
            list.forEachIndexed { index, sensor ->
                SensorCard(sensor = sensor)
                if (index < list.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CategoryHeader(name: String, count: Int) {
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
private fun SensorCard(sensor: SensorDetail) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val accent = colorForCategory(sensor.category)

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
                        imageVector = iconForSensorType(sensor.type),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensor.typeName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = sensor.vendor.ifBlank { "Unknown Vendor" },
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
                if (sensor.isWakeUpSensor) {
                    Surface(
                        color = primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "WAKE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            ),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SensorMetaRow(
                items = buildList {
                    add("v${sensor.version}" to false)
                    if (sensor.maximumRange > 0f) {
                        add("Range: ${formatNumber(sensor.maximumRange)}" to true)
                    }
                    if (sensor.resolution > 0f) {
                        add("Res: ${formatNumber(sensor.resolution)}" to true)
                    }
                    if (sensor.power > 0f) {
                        add("${formatNumber(sensor.power)} mA" to true)
                    }
                }
            )
        }
    }
}

@Composable
private fun SensorMetaRow(items: List<Pair<String, Boolean>>) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (text, mono) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .border(0.5.dp, primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = text,
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
}

private fun colorForCategory(category: String): Color = when (category) {
    "Motion" -> Color(0xFF4FC3F7)
    "Position" -> Color(0xFFBA68C8)
    "Environment" -> Color(0xFF81C784)
    "Biometric" -> Color(0xFFFF8A65)
    else -> Color(0xFF9E9E9E)
}

private fun iconForCategory(category: String): ImageVector = when (category) {
    "Motion" -> Icons.Filled.DirectionsWalk
    "Position" -> Icons.Filled.SocialDistance
    "Environment" -> Icons.Filled.Thermostat
    "Biometric" -> Icons.Filled.Favorite
    else -> Icons.Filled.Hub
}

private fun iconForSensorType(type: Int): ImageVector = when (type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER,
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
    android.hardware.Sensor.TYPE_GRAVITY -> Icons.Filled.Vibration

    android.hardware.Sensor.TYPE_GYROSCOPE,
    android.hardware.Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> Icons.Filled.RotateRight

    android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> Icons.Filled.Compress

    android.hardware.Sensor.TYPE_PROXIMITY -> Icons.Filled.SocialDistance

    android.hardware.Sensor.TYPE_LIGHT -> Icons.Filled.LightMode

    android.hardware.Sensor.TYPE_PRESSURE -> Icons.Filled.Air

    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE -> Icons.Filled.Thermostat

    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> Icons.Filled.WaterDrop

    android.hardware.Sensor.TYPE_HEART_BEAT,
    android.hardware.Sensor.TYPE_HEART_RATE -> Icons.Filled.Favorite

    android.hardware.Sensor.TYPE_STEP_COUNTER,
    android.hardware.Sensor.TYPE_STEP_DETECTOR,
    android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION,
    android.hardware.Sensor.TYPE_STATIONARY_DETECT,
    android.hardware.Sensor.TYPE_MOTION_DETECT -> Icons.Filled.DirectionsWalk

    else -> Icons.Filled.Memory
}

private fun formatNumber(value: Float): String {
    if (value == 0f) return "0"
    val absVal = abs(value)
    return when {
        absVal >= 100f -> "%.0f".format(value)
        absVal >= 10f -> "%.1f".format(value)
        absVal >= 1f -> "%.2f".format(value)
        else -> "%.3f".format(value)
    }
}
