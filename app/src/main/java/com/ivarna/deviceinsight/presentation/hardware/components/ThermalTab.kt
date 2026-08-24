package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.ThermalSensor
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlin.math.roundToInt

@Composable
fun ThermalTab(info: HardwareInfo) {
    val sensors = info.thermalSensors
    val maxTemp = remember(sensors) { sensors.maxOfOrNull { it.temperature } ?: 0f }
    val avgTemp = remember(sensors) {
        if (sensors.isNotEmpty()) sensors.map { it.temperature }.average().toFloat() else 0f
    }
    val minTemp = remember(sensors) { sensors.minOfOrNull { it.temperature } ?: 0f }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ThermalHeader(
            sensorCount = sensors.size,
            maxTemp = maxTemp,
            avgTemp = avgTemp,
            minTemp = minTemp
        )

        if (sensors.isEmpty()) {
            EmptyState(
                title = "NO SIGNAL",
                message = "no thermal sensors detected — requires root on some devices"
            )
        } else {
            QuickThermalStats(
                maxTemp = maxTemp,
                avgTemp = avgTemp,
                minTemp = minTemp
            )

            // Group sensors by category — memoized to avoid re-group on recomposition
            val grouped = remember(sensors) { sensors.groupBy { categorizeSensor(it.name) } }
            val categoryOrder = remember { listOf("CPU", "GPU", "Battery", "Skin", "System", "Other") }

            categoryOrder.forEach { category ->
                val list = grouped[category] ?: return@forEach
                if (list.isNotEmpty()) {
                    SectionHeader(name = category, count = list.size)
                    list.forEach { sensor ->
                        ThermalSensorCard(sensor = sensor, maxTemp = maxTemp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThermalHeader(
    sensorCount: Int,
    maxTemp: Float,
    avgTemp: Float,
    minTemp: Float
) {
    val c = Caliper.colors
    val status = remember(maxTemp, sensorCount) {
        when {
            sensorCount == 0 -> "NO SIGNAL"
            maxTemp >= 70f -> "CRITICAL"
            maxTemp >= 55f -> "WARM"
            maxTemp >= 40f -> "NORMAL"
            else -> "COOL"
        }
    }
    val statusColor = remember(maxTemp, sensorCount) {
        when {
            sensorCount == 0 -> c.ink40
            maxTemp >= 70f -> c.fault
            else -> c.ink
        }
    }

    PanelCard(
        title = "THERMAL · SENSORS",
        status = {
            if (sensorCount > 0) {
                LedDot(active = maxTemp >= 55f, color = if (maxTemp >= 70f) c.fault else c.channel(Channels.POWER))
                Spacer(Modifier.width(6.dp))
                Text(status, style = Caliper.type.meta, color = statusColor)
            } else {
                Text(status, style = Caliper.type.meta, color = c.ink40)
            }
        }
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "$sensorCount sensors",
                    style = Caliper.type.meta,
                    color = c.ink60
                )
                if (sensorCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OdometerText(
                            text = "${maxTemp.roundToInt()}°C",
                            style = Caliper.type.readoutL,
                            color = if (maxTemp >= 70f) c.fault else c.ink
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("PEAK", style = Caliper.type.meta, color = c.ink40)
                    }
                }
            }
            if (sensorCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("avg ${avgTemp.roundToInt()}°C", style = Caliper.type.meta, color = c.ink60)
                    Text("min ${minTemp.roundToInt()}°C", style = Caliper.type.meta, color = c.ink40)
                }
            }
        }
        if (sensorCount > 0) {
            Spacer(Modifier.height(12.dp))
            ThermalGauge(tempC = maxTemp, modifier = Modifier.fillMaxWidth())
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBadge(
            label = "MAX",
            value = "${maxTemp.roundToInt()}°C",
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "AVG",
            value = "${avgTemp.roundToInt()}°C",
            modifier = Modifier.weight(1f)
        )
        StatBadge(
            label = "MIN",
            value = "${minTemp.roundToInt()}°C",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ThermalSensorCard(
    sensor: ThermalSensor,
    maxTemp: Float
) {
    val c = Caliper.colors
    val fraction = remember(sensor.temperature, maxTemp) {
        if (maxTemp > 0f) (sensor.temperature / maxTemp).coerceIn(0f, 1f) else 0f
    }
    val category = remember(sensor.name) { categorizeSensor(sensor.name) }

    // CALIPER: thermal is a ramp amber→vermilion→deep red, not a channel
    val barColor = when {
        sensor.temperature >= 75f -> c.fault
        sensor.temperature >= 55f -> c.channel(Channels.CPU)
        sensor.temperature >= 45f -> c.channel(Channels.POWER)
        else -> c.ink60
    }

    PanelCard(
        title = sensorName(sensor.name).uppercase(),
        status = {
            Text("${sensor.temperature.roundToInt()}°C", style = Caliper.type.meta, color = if (sensor.temperature >= 70f) c.fault else c.ink)
        }
    ) {
        Text(category.uppercase(), style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(8.dp))
        TempProgressBar(fraction = fraction, color = barColor)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(sensor.name, style = Caliper.type.meta, color = c.ink60, maxLines = 1, modifier = Modifier.weight(1f))
            Text(Fmt.temp(sensor.temperature), style = Caliper.type.meta, color = c.ink40)
        }
    }
}

@Composable
private fun TempProgressBar(fraction: Float, color: androidx.compose.ui.graphics.Color) {
    val c = Caliper.colors
    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedWidth by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(600),
        label = "tempBar"
    )
    LaunchedEffect(fraction) { animTarget = fraction.coerceIn(0f, 1f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(c.hairline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedWidth)
                .height(6.dp)
                .background(color)
        )
    }
}

@Composable
private fun SectionHeader(name: String, count: Int) {
    val c = Caliper.colors
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
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

// ─────────────────────────────────────────────────────────────────────────────
// Helpers — keep pure, no composition
// ─────────────────────────────────────────────────────────────────────────────
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
        else -> rawName
    }
}
