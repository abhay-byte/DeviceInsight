package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun NetworkTab(info: HardwareInfo) {
    val net = info.networkDetailedInfo
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Telephony") {
            InfoRow("Phone Type", net.phoneType)
            InfoRow("Operator Name", net.networkOperatorName)
            InfoRow("Operator Code", net.networkOperatorCode)
            InfoRow("Operator Country", net.networkCountryIso)
            InfoRow("SIM Provider", net.simProviderName)
            InfoRow("SIM Provider Code", net.simProviderCode)
            InfoRow("SIM Country", net.simCountryIso)
            InfoRow("SIM State", net.simState)
            InfoRow("Network Type", info.networkType)
            InfoRow("Data State", net.dataState)
            InfoRow("Data Activity", net.dataActivity)
            InfoRow("Roaming", if (net.isRoaming) "Yes" else "No")
            InfoRow("ICC Card", if (net.hasIccCard) "Present" else "Absent")
        }

        InfoSection(title = "Wi-Fi") {
            InfoRow("State", net.wifiState)
            InfoRow("SSID", net.ssid)
            InfoRow("BSSID", net.bssid)
            InfoRow("Hidden SSID", if (net.isHiddenSsid) "Yes" else "No")
            InfoRow("IPv4 Address", net.ipv4Address)
            InfoRow("Signal Strength", net.signalStrength)
            InfoRow("Link Speed", net.linkSpeed)
            InfoRow("Frequency", net.frequency)
            InfoRow("Gateway", net.gateway)
            InfoRow("DNS 1", net.dns1)
            InfoRow("DHCP Lease", net.leaseDuration)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FeatureRow("5 GHz Band", net.is5GHzSupported)
            FeatureRow("Wi-Fi Aware", net.isWifiAwareSupported)
            FeatureRow("Wi-Fi Direct", net.isWifiDirectSupported)
        }
    }
}
