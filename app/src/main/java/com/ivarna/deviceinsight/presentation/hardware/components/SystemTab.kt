package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.utils.FormattingUtils
import com.ivarna.deviceinsight.utils.FormattingUtils.capitalizeWords

@Composable
fun SystemTab(info: HardwareInfo) {
    val context = LocalContext.current
    
    Column {
        InfoSection(title = "System") {
            InfoRow("Device Model", "${info.brand.capitalizeWords()} ${info.model}")
            InfoRow("Device Type", info.deviceType)
            InfoRow("Manufacturer", info.manufacturer)
            InfoRow("Model", info.model)
            InfoRow("Brand", info.brand)
            InfoRow("Board", info.board)
            InfoRow("Device", info.deviceName)
            InfoRow("Hardware", info.hardware)
            InfoRow("Product", info.product)
            InfoRow("Serial", info.serial)
        }

        InfoSection(title = "Memory") {
            InfoRow("Installed RAM", FormattingUtils.formatInstalledRam(info.totalRam))
            InfoRow("Total Memory", FormattingUtils.formatMemorySize(info.totalRam))
            InfoRow("Available Memory", FormattingUtils.formatMemorySize(info.availableRam))
        }

        InfoSection(title = "Internal Storage") {
            InfoRow("Total Space", FormattingUtils.formatFileSize(info.totalStorage))
            InfoRow("Free Space", FormattingUtils.formatFileSize(info.availableStorage))
        }

        if (info.totalExternalStorage > 0) {
            InfoSection(title = "External Storage") {
                InfoRow("Total Space", FormattingUtils.formatFileSize(info.totalExternalStorage))
                InfoRow("Free Space", FormattingUtils.formatFileSize(info.availableExternalStorage))
            }
        }

        InfoSection(title = "Android OS") {
            InfoRow("Version", info.androidVersion)
            InfoRow("API Level", info.apiLevel.toString())
            InfoRow("Security Patch", info.securityPatch)
            InfoRow("Kernel Version", info.kernelVersion)
            InfoRow("Rooted", if (info.isRooted) "Yes" else "No")
            InfoRow("Uptime", info.upTime)
        }
    }
}
