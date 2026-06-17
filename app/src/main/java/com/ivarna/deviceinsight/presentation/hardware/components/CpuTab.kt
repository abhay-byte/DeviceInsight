package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ivarna.deviceinsight.data.mapper.SocLogoRepository
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun CpuTab(info: HardwareInfo) {
    val logoRepo = remember { SocLogoRepository() }
    val logoUrl = logoRepo.logoUrlFor(info.socModel)

    Column {
        InfoSection(title = "SoC System on Chip") {
            SocLogoHeader(logoUrl = logoUrl, socModel = info.socModel)
            InfoRow("SoC Model", info.socModel)
            InfoRow("Core Architecture", info.cpuArchitecture)
            InfoRow("Manufacturing Process", info.manufacturingProcess)
            InfoRow("Instruction Set", info.supportedAbis.firstOrNull() ?: "Unknown")
            InfoRow("CPU Revision", info.cpuRevision)
        }

        InfoSection(title = "Processor") {
            InfoRow("CPU Cores", info.cpuCoreCount.toString())
            InfoRow("CPU Clock Range", info.cpuClockRange)

            info.coreClocks.forEachIndexed { index, clock ->
                if (index < 8) {
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

@Composable
private fun SocLogoHeader(logoUrl: String?, socModel: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        if (logoUrl != null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "$socModel logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .height(72.dp)
                    .padding(8.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Memory,
                contentDescription = "SoC",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
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
