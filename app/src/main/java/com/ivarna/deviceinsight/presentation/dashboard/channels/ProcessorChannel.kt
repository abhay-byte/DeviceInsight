package com.ivarna.deviceinsight.presentation.dashboard.channels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.components.*

/** CH-01 — Processor. (CALIPER S-02 / CpuScreen sketch) */
@Composable
fun ProcessorChannel(
    onBack: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m: DashboardMetrics? by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("№ 01.1 — PROCESSOR · S-02", "Processor.", "CH-01 · cpu load, clocks, cores", onBack) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        val ghz = if (snap.freqGHz > 0f) snap.freqGHz else metrics.maxCpuFrequency / 1000f
        OdometerText(
            String.format(java.util.Locale.US, "%.2f", ghz),
            style = Caliper.type.readoutL, color = c.ink
        )
        Text("GHz now", style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        ScopeTrace(
            values = metrics.cpuHistory.map { it.utilization },
            channel = Channels.CPU,
            windowLabel = "60 s",
            height = 120.dp
        )
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.CPU, title = "CORES") {
            if (snap.cores.isNotEmpty()) {
                CoreRail(snap.cores)
            } else {
                Text("core rail waiting for bench feed", style = Caliper.type.meta, color = c.ink40)
            }
            Spacer(Modifier.height(8.dp))
            ThermalGauge(metrics.cpuTemperature)
            snap.governor?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                SpecRow("governor", it)
            }
        }
    }
}
