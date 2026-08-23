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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.components.*

/** tiny bridge so channels don't import app-internal FormattingUtils under a clashing name */
private object FormattingUtilsBridge {
    fun bytes(v: Float): String =
        com.ivarna.deviceinsight.utils.FormattingUtils.formatFileSize(v.toLong())
}

/** CH-03 — Network. Dual spark when tx history exists; never a fake up-curve from rx. */
@Composable
fun NetworkChannel(
    onBack: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("№ 01.3 — NETWORK · S-04", "Network.", "CH-03 · throughput, both directions", onBack) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        val down = metrics.networkDownloadSpeed.takeIf { it.isNotBlank() } ?: "—"
        val up = metrics.networkUploadSpeed.ifBlank { "—" }
        OdometerText("↓ $down", style = Caliper.type.readoutL, color = c.ink)
        Text("↑ $up /s live", style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.NETWORK, title = "DOWN · RX 60 s") {
            ScopeTrace(
                values = metrics.netHistory,
                channel = Channels.NETWORK,
                windowLabel = "60 s",
                valueFormat = { FormattingUtilsBridge.bytes(it) + "/s" },
                timeLabelFor = { frac -> "-${((1f - frac) * 60).toInt()}s" },
                height = 110.dp
            )
        }

        // reviewer pin: dual spark ONLY when the second (tx) list is real; rx-only otherwise
        if (metrics.netUpHistory.any { it > 0f }) {
            Spacer(Modifier.height(12.dp))
            PanelCard(channel = Channels.NETWORK, title = "UP · TX 60 s") {
                Sparkline(metrics.netUpHistory, Channels.NETWORK,
                    Modifier.fillMaxWidth().height(48.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        PanelCard(channel = Channels.NETWORK, title = "BENCH FEED") {
            SpecRow("rx", if (snap.netDown > 0) FormattingUtilsBridge.bytes(snap.netDown.toFloat()) + "/s" else "—")
            SpecRow("tx", if (snap.netUp > 0) FormattingUtilsBridge.bytes(snap.netUp.toFloat()) + "/s" else "—")
        }
    }
}

