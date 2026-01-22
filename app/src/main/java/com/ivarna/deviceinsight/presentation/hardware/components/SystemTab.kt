package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun SystemTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Device") {
            InfoRow("Manufacturer", info.manufacturer)
            InfoRow("Model", info.model)
            InfoRow("Device Name", info.deviceName)
            InfoRow("Brand", info.brand)
            InfoRow("Board", info.board)
            InfoRow("Hardware", info.hardware)
            InfoRow("Build ID", info.buildId)
        }

        InfoSection(title = "Android OS") {
            InfoRow("Version", info.androidVersion)
            InfoRow("API Level", info.apiLevel.toString())
            InfoRow("Security Patch", info.securityPatch)
            InfoRow("Kernel Version", info.kernelVersion)
            InfoRow("Rooted", if(info.isRooted) "Yes" else "No")
            InfoRow("Uptime", info.upTime)
        }
    }
}
