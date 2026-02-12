package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun ThermalTab(info: HardwareInfo) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Thermal Sensors") {
            if (info.thermalSensors.isEmpty()) {
                InfoRow("Status", "No sensors detected or permission denied")
            } else {
                info.thermalSensors.forEach { sensor ->
                    InfoRow(sensor.name, "${sensor.temperature}°C")
                }
            }
        }
    }
}
