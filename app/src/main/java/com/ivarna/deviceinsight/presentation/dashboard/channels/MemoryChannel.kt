package com.ivarna.deviceinsight.presentation.dashboard.channels

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.ui.caliper.widget.MemSeg
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.components.*

/** CH-02 — Memory. (CALIPER S-03). Composition from the bench bus, not the Hardware tab. */
@Composable
fun MemoryChannel(
    onBack: () -> Unit,
    onTasks: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("№ 01.2 — MEMORY · S-03", "Memory.", "CH-02 · pressure and composition", onBack, ready = m != null) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        OdometerText(
            Fmt.bytes(metrics.ramUsedBytes),
            style = Caliper.type.readoutL, color = c.ink
        )
        Text("/ ${Fmt.bytes(metrics.ramTotalBytes)} · swap ${Fmt.bytes(metrics.swapUsedBytes)}",
            style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.MEMORY, title = "COMPOSITION") {
            val totalBytes = metrics.ramTotalBytes.coerceAtLeast(1L)
            val segs: List<MemSeg> = snap.memComposition.ifEmpty {
                listOf(MemSeg(metrics.ramUsage.coerceIn(0f, 1f), HatchPattern.SOLID, "CH-02"))
            }
            HatchBar(segments = memSegments(segs, totalBytes))
            Spacer(Modifier.height(10.dp))
            SpecRow("zram", if (snap.zramGb > 0f) String.format(java.util.Locale.US, "%.1f GB", snap.zramGb) else "—")
            SpecRow("swap total", Fmt.bytes(metrics.swapTotalBytes))
        }
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.MEMORY, title = "PRESSURE · 60 s") {
            Sparkline(metrics.ramHistory.map { it.utilization }, Channels.MEMORY,
                Modifier.fillMaxWidth().height(48.dp))
        }
        Spacer(Modifier.height(12.dp))

        if (snap.topConsumers.isNotEmpty()) {
            HardKey("TOP CONSUMERS → PROCESSES", variant = HardKeyVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth(), onClick = onTasks)
        }
    }
}

@Composable
private fun memSegments(
    segs: List<MemSeg>,
    totalBytes: Long
): List<HatchSegment> {
    val c = Caliper.colors
    fun colorFor(id: String) = when (id) {
        "CH-01" -> c.channel(Channels.CPU)
        "CH-02" -> c.channel(Channels.MEMORY)
        "CH-03" -> c.channel(Channels.NETWORK)
        "CH-04" -> c.channel(Channels.POWER)
        "CH-05" -> c.channel(Channels.STORAGE)
        "CH-06" -> c.channel(Channels.GPU)
        else -> c.ink40
    }
    return segs.map { seg ->
        HatchSegment(
            label = when (seg.pattern) {
                HatchPattern.SOLID -> "active"
                HatchPattern.DIAGONAL -> "cached"
                HatchPattern.CROSS -> "zram/swap"
                else -> "free"
            },
            bytes = (seg.fraction * totalBytes).toLong(),
            color = colorFor(seg.channelId),
            pattern = seg.pattern
        )
    }.ifEmpty { listOf(HatchSegment("free", totalBytes, c.ink40, HatchPattern.NONE)) }
}
