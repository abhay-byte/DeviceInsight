package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.utils.FormattingUtils

@Composable
fun SystemTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Device", icon = Icons.Filled.PhoneAndroid) {
            InfoRow("Device Model",  info.deviceModelName)
            InfoRow("Device Type",   info.deviceType)
            InfoRow("Manufacturer",  info.manufacturer)
            InfoRow("Model",         info.model)
            InfoRow("Brand",         info.brand)
            InfoRow("Board",         info.board)
            InfoRow("Device",        info.deviceName)
            InfoRow("Hardware",      info.hardware)
            InfoRow("Platform",      info.platform)
            InfoRow("Product",       info.product)
            InfoRow("Serial",        info.serial, monospace = true)
        }

        InfoSection(title = "Memory", icon = Icons.Filled.Memory) {
            InfoRow("Installed RAM",    info.installedRam)
            InfoRow("Total Memory",     FormattingUtils.formatMemorySize(info.totalRam), monospace = true)
            InfoRow("Available Memory", FormattingUtils.formatMemorySize(info.availableRam), monospace = true)
        }

        InfoSection(title = "Internal Storage", icon = Icons.Filled.SdStorage) {
            InfoRow("Total Space", FormattingUtils.formatFileSize(info.totalStorage), monospace = true)
            InfoRow("Free Space",  FormattingUtils.formatFileSize(info.availableStorage), monospace = true)
        }

        InfoSection(title = "Connectivity", icon = Icons.Filled.Bluetooth) {
            InfoRow("Bluetooth Version", info.bluetoothVersion)
        }

        InfoSection(title = "Device Features", icon = Icons.Filled.DataObject) {
            info.deviceFeatures.forEach { feature ->
                InfoRow(
                    label = feature
                        .replace("android.hardware.", "")
                        .replace("android.software.", ""),
                    value = "Yes"
                )
            }
        }
    }
}
