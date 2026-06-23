package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun BatteryTab(info: HardwareInfo) {
    val battery = info.batteryDetailedInfo
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    // Determine color based on level
    val levelColor = when {
        info.batteryLevel > 60 -> MaterialTheme.colorScheme.tertiary
        info.batteryLevel > 25 -> MaterialTheme.colorScheme.primary
        else -> error
    }

    Column(modifier = Modifier.padding(bottom = 16.dp)) {

        // ── Battery Hero Card ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(levelColor.copy(alpha = 0.10f), levelColor.copy(alpha = 0.02f))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(levelColor.copy(alpha = 0.3f), levelColor.copy(alpha = 0.05f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (info.isCharging)
                        Icons.Filled.BatteryChargingFull
                    else
                        Icons.Filled.Battery5Bar,
                    contentDescription = null,
                    tint = levelColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${info.batteryLevel}%",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = levelColor
                )
                Text(
                    text = if (info.isCharging) "Charging" else info.batteryStatus,
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("Voltage",  "${info.batteryVoltage / 1000f} V", color = primary)
                    SummaryItem("Temp",     "${info.batteryTemperature}°C",      color = if (info.batteryTemperature > 40f) error else tertiary)
                    SummaryItem("Health",   info.batteryHealth,                  color = levelColor)
                }
            }
        }

        InfoSection(title = "Battery Details", icon = Icons.Filled.Battery5Bar) {
            InfoRow("Power Source",   battery.powerSource)
            InfoRow("Technology",     info.batteryTechnology)
            InfoRow("Capacity",       battery.capacity)
            InfoRow("Charge Counter", battery.chargeCounter,  monospace = true)
            InfoRow("Charge Rate",    battery.currentNow,     monospace = true)
            InfoRow("Cycles",         battery.chargingCycles.toString(), monospace = true)
            if (info.isCharging) {
                InfoRow("Remaining Charge Time", battery.remainingChargeTime)
            }
        }

        InfoSection(title = "Live Readings", icon = Icons.Filled.ElectricBolt) {
            InfoRow("Voltage",     "${info.batteryVoltage / 1000f} V", monospace = true, valueColor = primary)
            InfoRow("Temperature", "${info.batteryTemperature}°C",     monospace = true,
                valueColor = if (info.batteryTemperature > 40f) error else tertiary)
        }
    }
}
