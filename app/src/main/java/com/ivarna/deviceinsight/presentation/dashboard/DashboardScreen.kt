package com.ivarna.deviceinsight.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.ui.caliper.*
import com.ivarna.deviceinsight.ui.caliper.components.*

/** № 01 — OVERVIEW (S-01 system ledger). No embedded Masthead — global skeleton owns it. */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val metrics by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceCard by viewModel.deviceCard.collectAsStateWithLifecycle()
    val c = Caliper.colors

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            sheetLabel = "№ 01 — OVERVIEW · REV 2.0",
            title = "Overview.",
            sub = "all channels nominal",
        )
        Spacer(Modifier.height(4.dp))

        if (metrics == null) {
            CalibratingIndicator(percent = null)
        } else {
            val m = metrics!!
            // CH-01 CPU
            ReadoutTile(
                channel = Channels.CPU,
                value = Fmt.pct(m.cpuUsage * 100, 1),
                subline = "${Fmt.temp(m.cpuTemperature)} · ${m.cpuTotalCores}C/${m.cpuTotalCores}T" +
                    (m.cpuGovernor?.let { " · gov $it" } ?: ""),
                spark = m.cpuHistory.map { it.utilization },
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))

            // CH-02 MEMORY
            val memUsed = Fmt.bytes(m.ramUsedBytes)
            val memTotal = Fmt.bytes(m.ramTotalBytes)
            ReadoutTile(
                channel = Channels.MEMORY,
                value = memUsed,
                unit = "/ $memTotal",
                subline = "swap ${Fmt.bytes(m.swapUsedBytes)}",
                barFraction = m.ramUsage,
                spark = m.ramHistory.map { it.utilization },
                statusText = Fmt.pct(m.ramUsage * 100),
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))

            // CH-03 NETWORK
            ReadoutTile(
                channel = Channels.NETWORK,
                value = m.networkDownloadSpeed.takeIf { it.isNotBlank() } ?: "—",
                unit = "/s ↓",
                subline = "↑ ${m.networkUploadSpeed.ifBlank { "—" }}",
                statusText = "LIVE",
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))

            // CH-04 POWER
            ReadoutTile(
                channel = Channels.POWER,
                value = "${m.batteryLevel}",
                unit = "%",
                subline = "${Fmt.watts(m.powerConsumption)} · ${Fmt.temp(m.temperature)} · ${m.batteryStatus}",
                barFraction = m.batteryLevel / 100f,
                statusText = m.batteryStatus.uppercase(),
                onClick = {}
            )
            Spacer(Modifier.height(12.dp))

            // CH-06 GPU (when present)
            if (m.gpuModel.isNotBlank()) {
                PanelCard(channel = Channels.GPU, title = "GPU LOAD") {
                    ScopeTrace(
                        values = m.cpuHistory.map { it.utilization },   // placeholder trace; real GPU hist not exposed
                        channel = Channels.GPU,
                        windowLabel = "60 s",
                        valueFormat = { Fmt.pct(it, 1) },
                        timeLabelFor = { frac -> "-${((1f - frac) * 60).toInt()}s" },
                        height = 120.dp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            // Device plate (dotted leaders)
            PanelCard(title = "DEVICE") {
                SpecRow("model", deviceCard.deviceName)
                SpecRow("soc", deviceCard.cpuModel)
                SpecRow("gpu", deviceCard.gpuModel)
                SpecRow("android", deviceCard.androidVersion)
            }
        }
        EndOfSheet()
    }
}