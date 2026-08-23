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
import com.ivarna.deviceinsight.ui.caliper.components.*

/** CH-05 — Storage. Used/total bar only — no fake per-directory map this pass. */
@Composable
fun StorageChannel(
    onBack: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("№ 01.5 — STORAGE · S-06", "Storage.", "CH-05 · internal volume", onBack) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        val used = metrics.storageUsedGb.ifBlank {
            if (snap.stoTotalGb > 0f) String.format(java.util.Locale.US, "%.0f GB", snap.stoUsedGb) else "—"
        }
        val total = metrics.storageTotalGb.ifBlank {
            if (snap.stoTotalGb > 0f) String.format(java.util.Locale.US, "%.0f GB", snap.stoTotalGb) else "—"
        }
        OdometerText(used, style = Caliper.type.readoutL, color = c.ink)
        Text("/ $total · ${FmtPct.pct(metrics.storageUsedPerc * 100f)}",
            style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.STORAGE, title = "VOLUME") {
            LinearGauge(fraction = metrics.storageUsedPerc.coerceIn(0f, 1f))
            Spacer(Modifier.height(6.dp))
            SpecRow("free", metrics.storageFreeGb.ifBlank { "—" })
        }
    }
}

private object FmtPct {
    fun pct(v: Float, digits: Int = 0): String =
        String.format(java.util.Locale.US, "%.${digits}f%%", v)
}
