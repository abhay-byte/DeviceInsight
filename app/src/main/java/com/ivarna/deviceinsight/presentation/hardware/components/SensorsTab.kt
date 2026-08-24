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
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.SensorDetail
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.components.*
import kotlin.math.abs

@Composable
fun SensorsTab(info: HardwareInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SensorHeader(
            sensorCount = info.sensorCount,
            hasFingerprint = info.fingerprintSensorPresent
        )

        CategorySummary(sensors = info.sensorDetails)

        SensorList(sensors = info.sensorDetails)
    }
}

@Composable
private fun SensorHeader(
    sensorCount: Int,
    hasFingerprint: Boolean
) {
    val c = Caliper.colors
    PanelCard(
        title = "SENSORS · SUITE",
        status = {
            if (hasFingerprint) StampBadge("FP", color = c.ink, rotation = -2f, animateIn = false)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                OdometerText(
                    text = "$sensorCount",
                    style = Caliper.type.readoutL,
                    color = c.ink
                )
                Text(
                    text = if (sensorCount == 1) "sensor detected" else "sensors detected",
                    style = Caliper.type.meta,
                    color = c.ink60
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "FIG. 1 — sensor inventory",
                    style = Caliper.type.meta,
                    color = c.ink40
                )
            }
        }
    }
}

@Composable
private fun CategorySummary(
    sensors: List<SensorDetail>,
) {
    val c = Caliper.colors
    val categories = remember { listOf("Motion", "Position", "Environment", "Biometric") }
    val counts = remember(sensors) {
        categories.associateWith { cat -> sensors.count { it.category == cat } }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            Column(
                modifier = Modifier.weight(1f)
                    .background(c.panel)
                    .border(1.dp, c.hairline)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = (counts[cat] ?: 0).toString(),
                    style = Caliper.type.dataM.copy(fontSize = 18.sp),
                    color = c.ink
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cat.uppercase(),
                    style = Caliper.type.meta.copy(fontSize = 8.sp),
                    color = c.ink60
                )
            }
        }
    }
}

@Composable
private fun SensorList(sensors: List<SensorDetail>) {
    if (sensors.isEmpty()) {
        EmptyState(
            title = "NO SIGNAL",
            message = "no sensors detected on this device"
        )
        return
    }

    val grouped = remember(sensors) { sensors.groupBy { it.category } }
    val categoryOrder = remember { listOf("Motion", "Position", "Environment", "Biometric", "Other") }

    categoryOrder.forEach { category ->
        val list = grouped[category] ?: return@forEach
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryHeader(name = category, count = list.size)
            list.forEach { sensor ->
                SensorCard(sensor = sensor)
            }
        }
    }
}

@Composable
fun CategoryHeader(name: String, count: Int) {
    val c = Caliper.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name.uppercase(), style = Caliper.type.meta, color = c.ink)
            Spacer(Modifier.width(8.dp))
            Text("·  $count", style = Caliper.type.meta, color = c.ink40)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        DoubleRule()
    }
}

@Composable
private fun SensorCard(sensor: SensorDetail) {
    val c = Caliper.colors
    PanelCard(
        title = sensor.typeName.uppercase(),
        status = {
            if (sensor.isWakeUpSensor) {
                Text("WAKE", style = Caliper.type.meta.copy(fontSize = 9.sp), color = c.ink)
            }
        }
    ) {
        Text(
            text = sensor.vendor.ifBlank { "unknown vendor" },
            style = Caliper.type.meta,
            color = c.ink60
        )
        Spacer(Modifier.height(8.dp))
        SpecRow("version", "v${sensor.version}")
        if (sensor.maximumRange > 0f) {
            SpecRow("range", formatNumber(sensor.maximumRange))
        }
        if (sensor.resolution > 0f) {
            SpecRow("resolution", formatNumber(sensor.resolution))
        }
        if (sensor.power > 0f) {
            SpecRow("power", "${formatNumber(sensor.power)} mA")
        }
        // minDelay as meta note
        if (sensor.minDelay > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "min delay · ${sensor.minDelay} µs",
                style = Caliper.type.meta,
                color = c.ink40
            )
        }
    }
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
