package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun SensorsTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Summary") {
            InfoRow("Total Sensors", info.sensorCount.toString())
            InfoRow("Fingerprint Sensor", if (info.fingerprintSensorPresent) "Present" else "Not Detected")
        }

        if (info.availableSensors.isNotEmpty()) {
            InfoSection(title = "Available Sensors") {
                 info.availableSensors.forEach { sensor ->
                     Text(
                         text = sensor,
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(vertical = 2.dp)
                     )
                 }
            }
        }
    }
}
