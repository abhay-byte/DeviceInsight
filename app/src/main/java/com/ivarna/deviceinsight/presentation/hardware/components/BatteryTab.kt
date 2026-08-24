package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.components.*
import java.util.Locale

@Composable
fun BatteryTab(info: HardwareInfo) {
    val battery = info.batteryDetailedInfo
    val c = Caliper.colors

    // memoized formatted strings — avoids re-formatting on recomposition
    val voltageText = remember(info.batteryVoltage) {
        String.format(Locale.US, "%.3f V", info.batteryVoltage / 1000f)
    }
    val tempText = remember(info.batteryTemperature) { Fmt.temp(info.batteryTemperature) }
    val levelFraction = remember(info.batteryLevel) { (info.batteryLevel.coerceIn(0, 100) / 100f) }
    val isCritical = info.batteryLevel < 20
    val isHot = info.batteryTemperature >= 45f

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── FUEL — hero gauge (§5.7 LinearGauge, CH-04 POWER) ─────────────
        PanelCard(
            channel = Channels.POWER,
            title = Channels.POWER.label + " · FUEL",
            status = {
                if (info.isCharging) {
                    LedDot(active = true, color = c.channel(Channels.POWER))
                    Spacer(Modifier.width(6.dp))
                    Text("CHARGING", style = Caliper.type.meta, color = c.ink60)
                } else {
                    Text(info.batteryStatus.uppercase(), style = Caliper.type.meta, color = if (isCritical) c.fault else c.ink60)
                }
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OdometerText(
                    text = "${info.batteryLevel}%",
                    style = Caliper.type.readoutL,
                    color = if (isCritical) c.fault else c.ink
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = voltageText,
                        style = Caliper.type.dataS,
                        color = c.ink60
                    )
                    Text(
                        text = tempText,
                        style = Caliper.type.meta,
                        color = if (isHot) c.fault else c.ink60
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearGauge(
                fraction = levelFraction,
                voltage = voltageText,
                charging = info.isCharging,
                critical = isCritical
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0", style = Caliper.type.meta, color = c.ink40)
                Text("health · ${info.batteryHealth.lowercase()}", style = Caliper.type.meta, color = c.ink60)
                Text("100", style = Caliper.type.meta, color = c.ink40)
            }
        }

        // ── Battery Details — spec sheet with dotted leaders (S-10) ──────────
        InfoSection(title = "Battery Details") {
            SpecRow("power source", battery.powerSource)
            SpecRow("technology", info.batteryTechnology)
            SpecRow("capacity", battery.capacity)
            SpecRow("charge counter", battery.chargeCounter)
            SpecRow("charge rate", battery.currentNow)
            SpecRow("cycles", if (battery.chargingCycles >= 0) battery.chargingCycles.toString() else "—")
            if (info.isCharging) {
                SpecRow("remaining charge", battery.remainingChargeTime)
            }
        }

        // ── Live Readings — thermal ramp (§5.7/§4.1 Thermal is a ramp) ───────
        InfoSection(title = "Live Readings") {
            SpecRow("voltage", voltageText)
            SpecRow("temperature", tempText)
            Spacer(Modifier.height(8.dp))
            ThermalGauge(tempC = info.batteryTemperature, modifier = Modifier.fillMaxWidth())
        }
    }
}
