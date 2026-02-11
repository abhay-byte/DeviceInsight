package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.utils.FormattingUtils

@Composable
fun CpuTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "SoC (System on Chip)") {
            InfoRow("SoC Model", info.socModel)
            InfoRow("Core Architecture", info.cpuArchitecture)
            InfoRow("Manufacturing Process", info.manufacturingProcess)
            InfoRow("Instruction Set", info.supportedAbis.firstOrNull() ?: "Unknown")
            InfoRow("CPU Revision", info.cpuRevision)
        }

        InfoSection(title = "Processor") {
            InfoRow("CPU Cores", info.cpuCoreCount.toString())
            InfoRow("CPU Clock Range", info.cpuClockRange)
            
            // Core Clocks
            info.coreClocks.forEachIndexed { index, clock ->
                if (index < 8) { // Request specified 1-8
                    InfoRow("Core ${index + 1} Clock", formatClockSpeed(clock))
                }
            }
            
            InfoRow("CPU Utilization", String.format("%.1f%%", info.cpuUtilization * 100))
        }

        InfoSection(title = "ABI Support") {
            InfoRow("Supported ABIs", info.supportedAbis.joinToString(", "))
            InfoRow("64-bit ABIs", info.supported64BitAbis.joinToString(", "))
        }

        InfoSection(title = "Extensions & Security") {
            InfoRow("AES", if (info.hasAes) "Supported" else "Not Supported")
            InfoRow("ASIMD / NEON", if (info.hasNeon) "Supported" else "Not Supported")
            InfoRow("PMULL", if (info.hasPmull) "Supported" else "Not Supported")
            InfoRow("SHA1", if (info.hasSha1) "Supported" else "Not Supported")
            InfoRow("SHA2", if (info.hasSha2) "Supported" else "Not Supported")
        }
    }
}

private fun formatClockSpeed(mhz: Int): String {
    return if (mhz >= 1000) {
        String.format("%.2f GHz", mhz / 1000f)
    } else {
        "$mhz MHz"
    }
}
