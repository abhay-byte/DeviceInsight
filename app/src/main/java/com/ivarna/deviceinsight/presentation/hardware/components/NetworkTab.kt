package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun NetworkTab(info: HardwareInfo) {
    val net = info.networkDetailedInfo

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Telephony", icon = Icons.Filled.CellTower) {
            InfoRow("Phone Type",        net.phoneType)
            InfoRow("Operator Name",     net.networkOperatorName)
            InfoRow("Operator Code",     net.networkOperatorCode,    monospace = true)
            InfoRow("Operator Country",  net.networkCountryIso)
            InfoRow("SIM Provider",      net.simProviderName)
            InfoRow("SIM Provider Code", net.simProviderCode,        monospace = true)
            InfoRow("SIM Country",       net.simCountryIso)
            InfoRow("SIM State",         net.simState)
            InfoRow("Network Type",      info.networkType)
            InfoRow("Data State",        net.dataState)
            InfoRow("Data Activity",     net.dataActivity)
            InfoRow("Roaming",           if (net.isRoaming) "Yes" else "No")
            InfoRow("ICC Card",          if (net.hasIccCard) "Present" else "Absent")
        }

        InfoSection(title = "Wi-Fi", icon = Icons.Filled.Wifi) {
            InfoRow("State",         net.wifiState)
            InfoRow("SSID",         net.ssid)
            InfoRow("BSSID",        net.bssid,           monospace = true)
            InfoRow("Hidden SSID",  if (net.isHiddenSsid) "Yes" else "No")
            InfoRow("IPv4 Address", net.ipv4Address,     monospace = true)
            InfoRow("Signal",       net.signalStrength,  monospace = true)
            InfoRow("Link Speed",   net.linkSpeed,       monospace = true)
            InfoRow("Frequency",    net.frequency,       monospace = true)
            InfoRow("Gateway",      net.gateway,         monospace = true)
            InfoRow("DNS",          net.dns1,            monospace = true)
            InfoRow("DHCP Lease",   net.leaseDuration)

            Spacer(modifier = Modifier.height(8.dp))

            FeatureRow("5 GHz Band",   net.is5GHzSupported)
            FeatureRow("Wi-Fi Aware",  net.isWifiAwareSupported)
            FeatureRow("Wi-Fi Direct", net.isWifiDirectSupported)
        }
    }
}
