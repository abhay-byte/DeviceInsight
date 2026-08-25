package com.ivarna.deviceinsight.presentation.dashboard.channels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.components.*

@Composable
fun GpuChannel(
    onBack: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("GPU", "gpu load and clocks", onBack, ready = m != null) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        when {
            !snap.gpuFitted && snap.gpuName.isBlank() && metrics.gpuModel.isBlank() -> {
                Text("NOT FITTED", style = Caliper.type.dataM, color = c.ink40)
                return@ChannelScaffold
            }
            snap.gpuRootLocked -> {
                Text("CHANNEL LOCKED", style = Caliper.type.dataM, color = c.fault)
                if (snap.gpuName.isNotBlank()) Text(snap.gpuName, style = Caliper.type.meta, color = c.ink60)
                return@ChannelScaffold
            }
        }

        val pct = (snap.gpuPct?.times(100f)) ?: metrics.gpuUsage * 100f
        val mhz = snap.gpuMHz?.toInt() ?: metrics.gpuFreqMhz
        OdometerText("${pct.toInt()}% · $mhz MHz", style = Caliper.type.readoutL, color = c.ink)
        val datasheet = listOf(
            snap.gpuName.ifBlank { metrics.gpuModel },
            snap.gpuVulkan,
            snap.gpuGles
        ).filter { it.isNotBlank() }.distinct().joinToString(" · ")
        if (datasheet.isNotBlank()) Text(datasheet, style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.GPU, title = "LOAD · 60 s") {
            val hist = when {
                snap.gpuHist.any { it > 0f } -> snap.gpuHist
                metrics.gpuHistory.isNotEmpty() -> metrics.gpuHistory
                else -> emptyList()
            }
            if (hist.size > 1) {
                ScopeTrace(
                    values = hist,
                    channel = Channels.GPU,
                    windowLabel = "60 s",
                    valueFormat = { Fmt.pct(it, 1) },
                    timeLabelFor = { frac -> "-${((1f - frac) * 60).toInt()}s" },
                    height = 120.dp
                )
            } else {
                Text("waiting for raster samples", style = Caliper.type.meta, color = c.ink40)
            }
        }
        if (metrics.gpuTemp > 0f) {
            Spacer(Modifier.height(6.dp))
            SpecRow("temperature", Fmt.temp(metrics.gpuTemp))
        }
    }
}
