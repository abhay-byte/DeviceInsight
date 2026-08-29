package com.ivarna.deviceinsight.ui.caliper.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow

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
    effectiveOpacity: Float,                 // service raises this if background blur is unavailable
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onDrag: (dxPx: Int, dyPx: Int) -> Unit = { _, _ -> },
    onLock: () -> Unit = {},
    onOpenConfig: () -> Unit = {}
) {
    HudTheme(config.medium, config.scale) {
        HudPanelContent(
            config = config,
            slow = slow,
            fast = fast,
            effectiveOpacity = effectiveOpacity,
            modifier = modifier,
            interactive = interactive,
            onDrag = onDrag,
            onLock = onLock,
            onOpenConfig = onOpenConfig
        )
    }
}

@Composable
private fun HudPanelContent(
    config: HudConfig,
    slow: State<HudSlow>,
    fast: State<HudFast>,
    effectiveOpacity: Float,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onDrag: (dxPx: Int, dyPx: Int) -> Unit = { _, _ -> },
    onLock: () -> Unit = {},
    onOpenConfig: () -> Unit = {}
) {
    val c = LocalHudColors.current
    val m = LocalHudMetrics.current

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
            .clipToBounds()
            .then(dragModifier)
    ) {
        Column(
            Modifier.padding(m.padDp.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            HudHeaderGate(slow, fast, config.locked, onLock)
            HairlineH()

            config.modules.sorted().forEachIndexed { i, module ->
                if (i > 0) HairlineH()
                when (module) {
                    HudModule.FPS -> HudFpsGate(fast)          // ONLY full reader of the 10 Hz feed
                    HudModule.CPU -> HudCpuGate(slow, config.showCoreBank)
                    HudModule.MEMORY -> HudMemoryGate(slow)
                    HudModule.POWER -> HudPowerGate(slow)
                    HudModule.GPU -> HudGpuGate(slow)
                    HudModule.NETWORK -> HudNetGate(slow)
                    // TRACE has no history feeds on HudSlow/HudFast yet — honest omission, no fake curves
                    HudModule.TRACE -> {}
                }
            }

        }
    }
}

// ─────────────── band gates — each reads its feed once, at the leaf ───────────────

@Composable private fun HudFpsGate(fast: State<HudFast>) { HudFpsBand(fast.value) }
@Composable private fun HudCpuGate(slow: State<HudSlow>, showBank: Boolean) { HudCpuBand(slow.value, showBank) }
@Composable private fun HudMemoryGate(slow: State<HudSlow>) { HudMemoryBand(slow.value) }
@Composable private fun HudPowerGate(slow: State<HudSlow>) { HudPowerBand(slow.value) }
@Composable private fun HudGpuGate(slow: State<HudSlow>) { HudGpuBand(slow.value) }
@Composable private fun HudNetGate(slow: State<HudSlow>) { HudNetBand(slow.value) }
