package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow
import com.ivarna.deviceinsight.data.monitor.isNoSignal

/**
 * The panel (DI-HD-001 §10). Reads fast/slow states independently so the 10 Hz FPS band
 * is the ONLY thing recomposing between slow ticks. Width is wrap-to-scale
 * (HudScales 196/260/300) — never fillMaxWidth.
 */
@Composable
fun HudPanel(
    config: HudConfig,
    slow: State<HudSlow>,
    fast: State<HudFast>,
    effectiveOpacity: Float,                 // service raises this if blur-behind is unavailable
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onDrag: (dxPx: Int, dyPx: Int) -> Unit = { _, _ -> },
    onLock: () -> Unit = {},
    onOpenConfig: () -> Unit = {}
) {
    val c = HudPalettes.of(config.medium)
    val m = HudScales.of(config.scale)
    val s = slow.value
    val f = fast.value
    val fault = s.tempC >= 75f || (s.batteryPct in 0f..0.2f && !s.charging)

    val dragModifier = if (config.locked || !interactive) Modifier
    else Modifier
        .pointerInput(Unit) {
            detectDragGestures { change, amount ->
                change.consume()
                onDrag(amount.x.toInt(), amount.y.toInt())
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(onTap = { onOpenConfig() })
        }

    Box(
        modifier.width(m.widthDp.dp).wrapContentHeight()
            .background(c.scrim.copy(alpha = effectiveOpacity))
            // scrim radius 0; brackets are the only frame; content padding keeps numerals off ⌜⌝
            .hudFrame(c.ink, inset = 0.dp, len = 12.dp)
            .then(dragModifier)
    ) {
        Column(Modifier.padding(m.padDp.dp)) {

            HudHeaderBand(
                slow = s,
                paused = f.isNoSignal(),
                fault = fault,
                locked = config.locked,
                onLock = onLock
            )
            HairlineH(Modifier.padding(top = m.padDp.dp / 2, bottom = m.padDp.dp / 2))

            config.modules.sorted().forEachIndexed { i, module ->
                if (i > 0) {
                    Spacer(Modifier.height(m.padDp.dp / 2))
                    HairlineH()
                    Spacer(Modifier.height(m.padDp.dp / 2))
                }
                when (module) {
                    HudModule.FPS -> HudFpsBand(f)
                    HudModule.CPU -> HudCpuBand(s, config.showCoreBank)
                    HudModule.MEMORY -> HudMemoryBand(s)
                    HudModule.POWER -> HudPowerBand(s)
                    HudModule.GPU -> HudGpuBand(s)
                    HudModule.NETWORK -> HudNetBand(s)
                    // TRACE has no history feeds on HudSlow/HudFast yet — honest omission, no fake curves
                    HudModule.TRACE -> {}
                }
            }

            if (!config.locked && interactive) {   // affordance strip while unlocked
                Spacer(Modifier.height(m.padDp.dp / 2))
                HairlineH()
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.text.BasicText(
                    "DRAG TO MOVE · TAP ⌖ TO LOCK · TAP PANEL FOR CONFIG",
                    style = hudStyle(8, trackingEm = 0.1f).copy(color = c.ink40),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
