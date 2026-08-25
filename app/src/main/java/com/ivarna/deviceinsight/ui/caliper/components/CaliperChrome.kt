package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sin

// ─────────────────────────── Masthead (§5.1) ───────────────────────────

// S1: gear HardKey (48dp, contentDescription "Settings") opens Settings.
@Composable
fun Masthead(
    modifier: Modifier = Modifier,
    degraded: Boolean = false,
    rootVerified: Boolean = false,
    onSettingsClick: (() -> Unit)? = null
) {
    val c = Caliper.colors
    // B1: masthead bg is panel not surface, ensure ink text visible on Paper light
    Column(modifier.fillMaxWidth().background(c.panel).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            // §5.1: masthead is exactly 52dp tall (below the status-bar inset).
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CrosshairMark(Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("DEVICEINSIGHT", style = Caliper.type.meta.copy(fontSize = 13.sp), color = c.ink)
            Spacer(Modifier.weight(1f))
            if (rootVerified) { StampBadge("ROOT VERIFIED", color = c.accent); Spacer(Modifier.width(8.dp)) }
            if (degraded) StampBadge("DEGRADED") else LedDot()
            Spacer(Modifier.width(8.dp))
            UtcClock()
            if (onSettingsClick != null) {
                Spacer(Modifier.width(8.dp))
                GearKey(onClick = onSettingsClick, color = c.ink)
            }
        }
        DoubleRule()
    }
}

// 48dp target gear — CALIPER-styled icon key with ink border.
@Composable
private fun GearKey(onClick: () -> Unit, color: androidx.compose.ui.graphics.Color) {
    val c = Caliper.colors
    Box(
        Modifier.size(48.dp)
            .border(1.dp, c.hairline)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = "Settings"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(20.dp)) {
            val r = size.minDimension / 2 - 2.dp.toPx()
            drawCircle(color, radius = r * 0.45f)
            drawCircle(color, radius = r, style = Stroke(1.5.dp.toPx()))
            for (i in 0 until 8) {
                val a = i * Math.PI / 4
                val x1 = (size.width / 2 + kotlin.math.cos(a) * (r * 0.75f)).toFloat()
                val y1 = (size.height / 2 + kotlin.math.sin(a) * (r * 0.75f)).toFloat()
                val x2 = (size.width / 2 + kotlin.math.cos(a) * r).toFloat()
                val y2 = (size.height / 2 + kotlin.math.sin(a) * r).toFloat()
                drawLine(color, Offset(x1, y1), Offset(x2, y2), 1.5.dp.toPx())
            }
        }
    }
}

@Composable
private fun CrosshairMark(modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        val r = size.minDimension / 2 - stroke
        drawCircle(c.ink, radius = r, style = Stroke(stroke))
        drawLine(c.ink, Offset(center.x - r - 2f, center.y), Offset(center.x + r + 2f, center.y), stroke)
        drawLine(c.ink, Offset(center.x, center.y - r - 2f), Offset(center.x, center.y + r + 2f), stroke)
        drawCircle(c.accent, radius = 2.dp.toPx(), center = center)
    }
}

@Composable
private fun UtcClock() {
    val c = Caliper.colors
    var now by remember { mutableStateOf(ZonedDateTime.now(ZoneId.systemDefault())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000 - (System.currentTimeMillis() % 1000))
            now = ZonedDateTime.now(ZoneId.systemDefault())
        }
    }
    val colon = if (now.second % 2 == 0) ":" else " "   // the heartbeat
    Text(
        String.format(java.util.Locale.US, "%02d%s%02d%s%02d",
            now.hour, colon, now.minute, colon, now.second),
        style = Caliper.type.dataS, color = c.ink
    )
}

// ─────────────────────────── ModeRail (§5.2) ───────────────────────────

data class RailKey(val number: Int, val label: String, val warning: Boolean = false)

@Composable
fun ModeRail(
    keys: List<RailKey>,
    selected: Int,
    onSelect: (RailKey) -> Unit,
    modifier: Modifier = Modifier,
    // §5.2/§6: ≥600dp widths switch to a left instrument rail (ModeRail vertical).
    vertical: Boolean = false
) {
    val c = Caliper.colors
    if (vertical) {
        Column(
            modifier.fillMaxHeight().width(232.dp).background(c.surface)
                .drawBehind { drawLine(c.hairline, Offset(size.width - 0.5f, 0f), Offset(size.width - 0.5f, size.height), 1.dp.toPx()) }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            keys.forEach { key -> VerticalRailKey(key, key.number == selected, onSelect) }
        }
    } else {
        Column(
            modifier.fillMaxWidth().background(c.surface)
                .drawBehind { drawLine(c.hairline, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            val haptics = rememberCaliperHaptics()
            Row(Modifier.fillMaxWidth().height(64.dp)) {
                keys.forEach { key ->
                    val sel = key.number == selected
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                haptics.tick(); onSelect(key)
                            }
                            .semantics {
                                role = Role.Tab
                                contentDescription = "[${key.number}] ${key.label}"
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NumberKeyBox(key.number, sel)
                            if (key.warning) { Spacer(Modifier.width(4.dp)); LedDot(color = c.fault, dotSize = 4.dp) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(key.label.uppercase(), style = Caliper.type.meta,
                            color = if (sel) c.ink else c.ink60)
                        Spacer(Modifier.height(3.dp))
                        Canvas(Modifier.size(width = 10.dp, height = 4.dp)) {
                            if (sel) {
                                val tri = Path().apply {
                                    moveTo(size.width / 2, size.height); lineTo(0f, 0f); lineTo(size.width, 0f); close()
                                }
                                drawPath(tri, c.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}

// M1: active key = ink-filled square NUMBER + caret + accent underline. Number
// renders inside the square (ink on surface idle, surface on ink when active).
@Composable
private fun NumberKeyBox(number: Int, selected: Boolean) {
    val c = Caliper.colors
    Box(
        Modifier.size(18.dp)
            .then(if (selected) Modifier.background(c.ink) else Modifier)
            .border(1.dp, if (selected) c.ink else c.ink40),
        contentAlignment = Alignment.Center
    ) {
        Text(
            number.toString(),
            style = Caliper.type.label.copy(fontSize = 11.sp, lineHeight = 13.sp),
            color = if (selected) c.surface else c.ink
        )
    }
}

@Composable
private fun VerticalRailKey(key: RailKey, selected: Boolean, onSelect: (RailKey) -> Unit) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    Column(
        Modifier.fillMaxWidth()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                haptics.tick(); onSelect(key)
            }
            .semantics {
                role = Role.Tab
                contentDescription = "[${key.number}] ${key.label}"
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberKeyBox(key.number, selected)
            Spacer(Modifier.width(10.dp))
            Text(key.label.uppercase(), style = Caliper.type.meta.copy(fontSize = 12.sp),
                color = if (selected) c.ink else c.ink60)
            if (key.warning) { Spacer(Modifier.width(6.dp)); LedDot(color = c.fault, dotSize = 4.dp) }
        }
        Canvas(Modifier.fillMaxWidth().height(4.dp).padding(top = 2.dp)) {
            if (selected) {
                val cx = size.width / 2
                val tri = Path().apply {
                    moveTo(cx, size.height); lineTo(cx - 4.dp.toPx(), 0f); lineTo(cx + 4.dp.toPx(), 0f); close()
                }
                drawPath(tri, c.accent)
                drawLine(c.accent, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx())
            }
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, warn: Boolean = false) {
    val c = Caliper.colors
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = Caliper.type.display1, color = c.ink)
        Text(subtitle, style = Caliper.type.meta, color = if (warn) c.fault else c.ink40)
        Spacer(Modifier.height(10.dp))
        DoubleRule()
    }
}

// ─────────────────────────── states (§5.15) ───────────────────────────

@Composable
fun CalibratingIndicator(percent: Float? = null) {
    val c = Caliper.colors
    val reduced = rememberReducedMotion()
    val rotation by rememberInfiniteTransition(label = "reticle").animateFloat(
        0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Canvas(Modifier.size(28.dp)) {
            rotate(if (reduced) 0f else rotation) {
                drawCircle(c.ink, radius = size.minDimension / 2 - 2.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                drawLine(c.ink, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx())
                drawLine(c.ink, Offset(center.x, 0f), Offset(center.x, size.height), 1.5.dp.toPx())
            }
            drawCircle(c.accent, radius = 2.dp.toPx(), center = center)
        }
        Spacer(Modifier.height(10.dp))
        Text("CALIBRATING${percent?.let { " · ${it.roundToInt()}%" } ?: ""}",
            style = Caliper.type.meta, color = c.ink60)
    }
}

/**
 * LOAD-THEN-SHOW gate (§5.15): holds the calibrating reticle until [ready] AND
 * the destination's enter transition has cleared ([settleMs]), then sweeps the
 * sheet in with the standard fade+rise. Guarantees heavy first composition
 * never lands mid-navigation — pages load, then show.
 */
@Composable
fun LoadThenShow(
    ready: Boolean,
    modifier: Modifier = Modifier,
    settleMs: Int = 240,
    content: @Composable () -> Unit
) {
    val reveal = remember { MutableTransitionState(false) }
    LaunchedEffect(ready) {
        if (ready && !reveal.targetState) {
            delay(settleMs.toLong())
            reveal.targetState = true
        }
    }
    if (!reveal.targetState) {
        Box(modifier, contentAlignment = Alignment.TopCenter) {
            CalibratingIndicator(percent = null)
        }
    } else {
        AnimatedVisibility(
            visibleState = reveal,
            enter = fadeIn(tween(260, easing = CaliperMotion.Ease)) +
                slideInVertically(tween(260, easing = CaliperMotion.Ease)) { it / 16 },
            exit = fadeOut(tween(120)),
            label = "load-then-show"
        ) {
            Box(modifier) { content() }
        }
    }
}

@Composable
fun EmptyState(title: String, message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    val c = Caliper.colors
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {   // mini test-pattern
            listOf(Channels.CPU, Channels.MEMORY, Channels.NETWORK, Channels.POWER, Channels.STORAGE)
                .forEach { ch ->
                    Box(Modifier.size(8.dp).background(c.channel(ch)))
                    Spacer(Modifier.width(2.dp))
                }
        }
        Spacer(Modifier.height(12.dp))
        Text(title.uppercase(), style = Caliper.type.meta, color = c.ink)
        Spacer(Modifier.height(4.dp))
        Text(message, style = Caliper.type.dataS, color = c.ink60)
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            HardKey(actionLabel, variant = HardKeyVariant.PRIMARY, onClick = onAction)
        }
    }
}

@Composable
fun FaultState(code: String, cause: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        StampBadge("FAULT $code")
        Spacer(Modifier.height(10.dp))
        Text(cause, style = Caliper.type.dataS, color = Caliper.colors.ink60)
        Spacer(Modifier.height(16.dp))
        HardKey("RETRY", variant = HardKeyVariant.SECONDARY, onClick = onRetry)
    }
}

// ─────────────────────────── CalibrationSweep (first-launch signature) ───────────────────────────

@Composable
fun CalibrationSweep(
    visible: Boolean,
    onFinished: () -> Unit,
) {
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
        val c = Caliper.colors
        val reduced = rememberReducedMotion()
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            if (reduced) progress.snapTo(1f)
            else progress.animateTo(1f, tween(1200, easing = LinearEasing))
            delay(900)
            onFinished()
        }
        Box(Modifier.fillMaxSize().background(c.surface), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(32.dp).caliperGrid()) {
                val w = size.width; val x = w * progress.value
                drawLine(c.accent, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                val path = Path()
                var started = false
                for (i in 0..200) {
                    val px = w * i / 200f
                    if (px <= x) {
                        val py = size.height / 2 + sin(i * 0.15f) * size.height * 0.18f
                        if (!started) { path.moveTo(px, py); started = true } else path.lineTo(px, py)
                    }
                }
                drawPath(path, c.channel(Channels.CPU),
                    style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Square))
            }
            if (progress.value >= 1f) StampBadge("CALIBRATED")
        }
    }
}