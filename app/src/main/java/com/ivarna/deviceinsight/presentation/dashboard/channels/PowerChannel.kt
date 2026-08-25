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

@Composable
fun PowerChannel(
    onBack: () -> Unit,
    vm: ChannelViewModel = hiltViewModel()
) {
    val m by vm.metrics.collectAsStateWithLifecycle()
    val snap by vm.bus.snapshot.collectAsStateWithLifecycle()
    val c = Caliper.colors

    ChannelScaffold("Power", "battery and draw", onBack, ready = m != null) {
        val metrics = m
        if (metrics == null) {
            CalibratingIndicator(percent = null)
            return@ChannelScaffold
        }
        if (!snap.batteryPresent && metrics.batteryLevel <= 0) {
            Text("NOT FITTED", style = Caliper.type.dataM, color = c.ink40)
            return@ChannelScaffold
        }
        val watts = if (snap.watts != 0f) snap.watts else metrics.powerConsumption
        OdometerText(
            com.ivarna.deviceinsight.ui.caliper.Fmt.wattsSigned(watts),
            style = Caliper.type.readoutL, color = c.ink
        )
        Text(if (snap.charging || metrics.isCharging) "charging" else "discharging",
            style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(12.dp))

        PanelCard(channel = Channels.POWER, title = "FUEL") {
            LinearGauge(
                fraction = (metrics.batteryLevel / 100f).coerceIn(0f, 1f),
                voltage = if (snap.voltage > 0) "${(snap.voltage * 1000).toInt()} mV" else null,
                charging = snap.charging || metrics.isCharging
            )
            val remaining = remainingLabel(snap.remainingMin)
            if (remaining != null) {
                Spacer(Modifier.height(6.dp))
                SpecRow("remaining", remaining)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (snap.wattHist.any { it != 0f }) {
            PanelCard(channel = Channels.POWER, title = "DRAW · 60 s") {
                Sparkline(snap.wattHist, Channels.POWER, Modifier.fillMaxWidth().height(48.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        // datasheet rows only when the platform actually reports them (never "—"-fake)
        snap.batteryHealth?.takeIf { it.isNotBlank() }?.let {
            SpecRow("health", it)
        }
        snap.cycleCount?.takeIf { it > 0 }?.let {
            SpecRow("cycles", "$it")
        }
    }
}

internal fun remainingLabel(min: Int): String? = when {
    min <= 0 -> null
    min >= 60 -> String.format(java.util.Locale.US, "%dh %dm", min / 60, min % 60)
    else -> "$min min"
}
