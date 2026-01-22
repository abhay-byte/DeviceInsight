package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun NetworkTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Connectivity") {
            InfoRow("Network Type", info.networkType)
            InfoRow("Operator", info.networkOperator)
            InfoRow("IP Address", info.ipAddress)
        }
    }
}
