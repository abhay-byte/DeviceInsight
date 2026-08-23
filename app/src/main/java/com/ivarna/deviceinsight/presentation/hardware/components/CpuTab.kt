package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.mapper.SocLogoRepository
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.components.PanelCard

@Composable
fun CpuTab(info: HardwareInfo) {
    val logoRepo = remember { SocLogoRepository() }
    val logoUrl = remember(info.socModel) { logoRepo.logoUrlFor(info.socModel) }
    val cpuColor = Caliper.colors.channel(Channels.CPU)

    Column {
        // ── SoC hero plate (CALIPER flat — no glass, no gradient) ───────────
        PanelCard(title = "SOC", status = {
            Text(info.cpuArchitecture, style = Caliper.type.meta, color = Caliper.colors.ink40)
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "${info.socModel} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp),
                        error = painterResource(R.drawable.ic_soc_generic),
                        fallback = painterResource(R.drawable.ic_soc_generic)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_soc_generic),
                        contentDescription = "${info.socModel} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(info.socModel, style = Caliper.type.dataM, color = Caliper.colors.ink)
                    Text(
                        "process ${info.manufacturingProcess.takeIf { it.isNotBlank() } ?: "—"}",
                        style = Caliper.type.meta, color = Caliper.colors.ink60
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem(label = "Cores", value = info.cpuCoreCount.toString())
                SummaryItem(label = "Usage", value = "${(info.cpuUtilization * 100).toInt()}%")
            }
        }

        InfoSection(title = "System on Chip") {
            InfoRow("SoC Model",             info.socModel)
            InfoRow("Architecture",          info.cpuArchitecture)
            InfoRow("Manufacturing Process", info.manufacturingProcess)
            InfoRow("Instruction Set",       info.supportedAbis.firstOrNull() ?: "Unknown")
            InfoRow("CPU Revision",          info.cpuRevision, monospace = true)
        }

        InfoSection(title = "Processor Cores") {
            InfoRow("Core Count",  info.cpuCoreCount.toString(), monospace = true)
            InfoRow("Clock Range", info.cpuClockRange)
            InfoRow("Utilization", "${(info.cpuUtilization * 100).toInt()}%", monospace = true)

            if (info.coreClocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                info.coreClocks.take(8).forEachIndexed { index, clock ->
                    UsageBar(
                        label = "Core ${index + 1}",
                        value = (clock.toFloat() / (info.coreClocks.maxOrNull()?.toFloat() ?: 1f)).coerceIn(0f, 1f),
                        color = cpuColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        InfoSection(title = "ABI Support") {
            InfoRow("Supported ABIs", info.supportedAbis.joinToString(", "))
            InfoRow("64-bit ABIs",    info.supported64BitAbis.joinToString(", "))
        }

        InfoSection(title = "Extensions & Security") {
            FeatureRow("AES",          info.hasAes)
            FeatureRow("ASIMD / NEON", info.hasNeon)
            FeatureRow("PMULL",        info.hasPmull)
            FeatureRow("SHA-1",        info.hasSha1)
            FeatureRow("SHA-2",        info.hasSha2)
        }
    }
}
