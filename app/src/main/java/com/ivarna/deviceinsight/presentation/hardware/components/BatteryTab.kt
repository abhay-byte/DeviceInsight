package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun BatteryTab(info: HardwareInfo) {
    val battery = info.batteryDetailedInfo
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Battery") {
            InfoRow("Power Source", battery.powerSource)
            InfoRow("Level", "${info.batteryLevel} %")
            InfoRow("Status", info.batteryStatus)
            InfoRow("Health", info.batteryHealth)
            InfoRow("Technology", info.batteryTechnology)
            InfoRow("Temperature", "${info.batteryTemperature}°C")
            InfoRow("Voltage", "${info.batteryVoltage / 1000f} V")
            InfoRow("Charge Counter", battery.chargeCounter)
            InfoRow("Charge Rate", battery.currentNow)
            InfoRow("Charging Cycles", battery.chargingCycles.toString())
            if (info.isCharging) {
                InfoRow("Remaining Charge Time", battery.remainingChargeTime)
            }
            InfoRow("Capacity", battery.capacity)
        }
    }
}
