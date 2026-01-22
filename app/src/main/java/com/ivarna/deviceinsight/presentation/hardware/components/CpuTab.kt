package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import android.text.format.Formatter

@Composable
fun CpuTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Processor") {
            InfoRow("Cores", info.cpuCoreCount.toString())
            InfoRow("Supported ABIs", info.supportedAbis.joinToString(", "))
        }

        InfoSection(title = "Memory") {
            InfoRow("Total RAM", formatSize(info.totalRam))
            InfoRow("Available RAM", formatSize(info.availableRam))
        }

        InfoSection(title = "Storage") {
            InfoRow("Total Storage", formatSize(info.totalStorage))
            InfoRow("Available Storage", formatSize(info.availableStorage))
        }
    }
}

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
