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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.ThermalSensor
import kotlin.math.roundToInt

@Composable
fun ThermalTab(info: HardwareInfo) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val sensors = info.thermalSensors
    val maxTemp = sensors.maxOfOrNull { it.temperature } ?: 0f
    val avgTemp = if (sensors.isNotEmpty()) sensors.map { it.temperature }.average().toFloat() else 0f
    val minTemp = sensors.minOfOrNull { it.temperature } ?: 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp)
    ) {
        ThermalHeader(
            sensorCount = sensors.size,
            maxTemp = maxTemp,
            avgTemp = avgTemp,
            minTemp = minTemp
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (sensors.isEmpty()) {
            EmptyThermalState()
        } else {
            // Quick stats row
            QuickThermalStats(
                maxTemp = maxTemp,
                avgTemp = avgTemp,
                minTemp = minTemp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Group sensors by category
            val grouped = sensors.groupBy { categorizeSensor(it.name) }
            val categoryOrder = listOf("CPU", "GPU", "Battery", "Skin", "System", "Other")

            categoryOrder.forEach { category ->
                val list = grouped[category] ?: return@forEach
                if (list.isNotEmpty()) {
                    CategoryHeader(name = category, count = list.size)
                    Spacer(modifier = Modifier.height(10.dp))
                    list.forEachIndexed { index, sensor ->
                        ThermalSensorCard(sensor = sensor, maxTemp = maxTemp)
                        if (index < list.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun ThermalHeader(
    sensorCount: Int,
    maxTemp: Float,
    avgTemp: Float,
    minTemp: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error
    val tempColor = temperatureColor(maxTemp)
    val status = when {
        sensorCount == 0 -> "No Data"
        maxTemp >= 70f -> "Critical"
        maxTemp >= 55f -> "Warm"
        maxTemp >= 40f -> "Normal"
        else -> "Cool"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tempColor.copy(alpha = 0.14f),
                        secondary.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tempColor.copy(alpha = 0.35f),
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
                                tempColor.copy(alpha = 0.25f),
                                secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(1.dp, tempColor.copy(alpha = 0.4f), RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        maxTemp >= 70f -> Icons.Filled.LocalFireDepartment
                        maxTemp >= 40f -> Icons.Filled.Whatshot
                        else -> Icons.Filled.AcUnit
                    },
                    contentDescription = null,
                    tint = tempColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Thermal Status",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tempColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = tempColor
                    )
                }
            }
            if (sensorCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${maxTemp.roundToInt()}°",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = tempColor
                    )
                    Text(
                        text = "PEAK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickThermalStats(
    maxTemp: Float,
    avgTemp: Float,
    minTemp: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatBadge(
            label = "MAX",
            value = "${maxTemp.roundToInt()}°C",
            color = temperatureColor(maxTemp),
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "AVG",
            value = "${avgTemp.roundToInt()}°C",
            color = temperatureColor(avgTemp),
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "MIN",
            value = "${minTemp.roundToInt()}°C",
            color = temperatureColor(minTemp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThermalSensorCard(
    sensor: ThermalSensor,
    maxTemp: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val tempColor = temperatureColor(sensor.temperature)
    val fraction = if (maxTemp > 0f) (sensor.temperature / maxTemp).coerceIn(0f, 1f) else 0f
    val category = categorizeSensor(sensor.name)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tempColor.copy(alpha = 0.06f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tempColor.copy(alpha = 0.25f),
                        primary.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(0.dp)
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
                        .clip(RoundedCornerShape(0.dp))
                        .background(tempColor.copy(alpha = 0.14f))
                        .border(1.dp, tempColor.copy(alpha = 0.3f), RoundedCornerShape(0.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Thermostat,
                        contentDescription = null,
                        tint = tempColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sensorName(sensor.name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 9.sp
                        ),
                        color = tempColor.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${sensor.temperature.roundToInt()}°C",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = tempColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            TempProgressBar(
                fraction = fraction,
                color = tempColor
            )
        }
    }
}

@Composable
private fun TempProgressBar(fraction: Float, color: Color) {
    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedWidth by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(800),
        label = "tempBar"
    )
    LaunchedEffect(fraction) { animTarget = fraction.coerceIn(0f, 1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(0.dp))
            .background(color.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedWidth)
                .height(6.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(color, color.copy(alpha = 0.6f))
                    )
                )
        )
    }
}

@Composable
private fun EmptyThermalState() {
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
                imageVector = Icons.Filled.DeviceThermostat,
                contentDescription = null,
                tint = primary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No thermal sensors detected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This may require root access",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun temperatureColor(temp: Float): Color = when {
    temp >= 75f -> Color(0xFFFF5252)
    temp >= 60f -> Color(0xFFFF8A65)
    temp >= 45f -> Color(0xFFFFC400)
    temp >= 30f -> Color(0xFF81C784)
    else -> Color(0xFF64B5F6)
}

private fun categorizeSensor(name: String): String {
    val lower = name.lowercase()
    return when {
        lower.contains("cpu") || lower.contains("tsens") && lower.contains("cpu") -> "CPU"
        lower.contains("gpu") -> "GPU"
        lower.contains("battery") || lower.contains("batt") || lower.contains("chg") -> "Battery"
        lower.contains("skin") || lower.contains("back") || lower.contains("front") -> "Skin"
        lower.contains("cpu") -> "CPU"
        lower.contains("tsens") -> "System"
        else -> "Other"
    }
}

private fun sensorName(rawName: String): String {
    val lower = rawName.lowercase()
    return when {
        lower.contains("cpu") && lower.contains("big") -> "CPU Big Core"
        lower.contains("cpu") && lower.contains("little") -> "CPU Little Core"
        lower.contains("cpu") && lower.contains("mid") -> "CPU Mid Core"
        lower.contains("cpu") -> "CPU"
        lower.contains("gpu") -> "GPU"
        lower.contains("battery") -> "Battery"
        lower.contains("skin") -> "Skin Temperature"
        lower.contains("tsens") -> "Thermal Sensor"
        else -> rawName.replaceFirstChar { it.uppercase() }
    }
}
