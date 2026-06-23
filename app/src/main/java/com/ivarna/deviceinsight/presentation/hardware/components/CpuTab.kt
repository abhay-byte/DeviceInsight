package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ivarna.deviceinsight.data.mapper.SocLogoRepository
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun CpuTab(info: HardwareInfo) {
    val logoRepo = remember { SocLogoRepository() }
    val logoUrl = logoRepo.logoUrlFor(info.socModel)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Column {
        // ── SoC Hero Header ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.10f), secondary.copy(alpha = 0.03f))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(primary.copy(alpha = 0.3f), secondary.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "${info.socModel} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .height(60.dp)
                            .padding(bottom = 8.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text(
                    text = info.socModel,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = primary
                )
                Text(
                    text = info.cpuArchitecture,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "Cores",
                        value = info.cpuCoreCount.toString(),
                        color = primary
                    )
                    SummaryItem(
                        label = "Process",
                        value = info.manufacturingProcess.takeIf { it.isNotBlank() } ?: "—",
                        color = secondary
                    )
                    SummaryItem(
                        label = "Usage",
                        value = "${(info.cpuUtilization * 100).toInt()}%",
                        color = tertiary
                    )
                }
            }
        }

        InfoSection(title = "System on Chip", icon = Icons.Filled.Memory) {
            InfoRow("SoC Model",            info.socModel)
            InfoRow("Architecture",         info.cpuArchitecture)
            InfoRow("Manufacturing Process",info.manufacturingProcess)
            InfoRow("Instruction Set",      info.supportedAbis.firstOrNull() ?: "Unknown")
            InfoRow("CPU Revision",         info.cpuRevision, monospace = true)
        }

        InfoSection(title = "Processor Cores", icon = Icons.Filled.Speed) {
            InfoRow("Core Count",    info.cpuCoreCount.toString(), monospace = true)
            InfoRow("Clock Range",   info.cpuClockRange)
            InfoRow("Utilization",   "${(info.cpuUtilization * 100).toInt()}%", monospace = true, valueColor = primary)

            if (info.coreClocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                info.coreClocks.take(8).forEachIndexed { index, clock ->
                    UsageBar(
                        label = "Core ${index + 1}",
                        value = (clock.toFloat() / (info.coreClocks.maxOrNull()?.toFloat() ?: 1f)).coerceIn(0f, 1f),
                        color = primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        InfoSection(title = "ABI Support") {
            InfoRow("Supported ABIs",  info.supportedAbis.joinToString(", "))
            InfoRow("64-bit ABIs",     info.supported64BitAbis.joinToString(", "))
        }

        InfoSection(title = "Extensions & Security", icon = Icons.Filled.Shield) {
            FeatureRow("AES",         info.hasAes)
            FeatureRow("ASIMD / NEON",info.hasNeon)
            FeatureRow("PMULL",       info.hasPmull)
            FeatureRow("SHA-1",       info.hasSha1)
            FeatureRow("SHA-2",       info.hasSha2)
        }
    }
}

private fun formatClockSpeed(mhz: Int): String {
    return if (mhz >= 1000) "%.2f GHz".format(mhz / 1000f) else "$mhz MHz"
}
