package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun BatteryTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Battery Status") {
            InfoRow("Level", "${info.batteryLevel}%")
            InfoRow("Status", info.batteryStatus)
            InfoRow("Health", info.batteryHealth)
            InfoRow("Charging", if(info.isCharging) "Yes" else "No")
        }

        InfoSection(title = "Details") {
            InfoRow("Technology", info.batteryTechnology)
            InfoRow("Voltage", "${info.batteryVoltage} mV")
            InfoRow("Temperature", "${info.batteryTemperature} °C")
            InfoRow("Capacity (Est.)", info.batteryCapacity)
        }
    }
}
