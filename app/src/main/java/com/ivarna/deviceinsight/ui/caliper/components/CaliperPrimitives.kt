package com.ivarna.deviceinsight.ui.caliper.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ─────────────────────────── LED ───────────────────────────

@Composable
fun LedDot(
    modifier: Modifier = Modifier,
    active: Boolean = true,
    color: Color = Caliper.colors.accent,
    pulsing: Boolean = true,
    dotSize: Dp = 6.dp
) {
    val reduced = rememberReducedMotion()
    val pulse by rememberInfiniteTransition(label = "led")
        .animateFloat(0.6f, 1f,
            infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse), label = "ledAlpha")
    val alpha = when {
        !active -> 0.25f
        pulsing && !reduced -> pulse
        else -> 1f
    }
    Canvas(modifier.size(dotSize)) {
        drawCircle(color.copy(alpha = alpha), radius = size.minDimension / 2)
    }
}

// ─────────────────────────── channel tick ───────────────────────────

@Composable
fun ChannelTick(channel: Channel, modifier: Modifier = Modifier, tickSize: Dp = 6.dp) {
    Box(modifier.size(tickSize).background(Caliper.colors.channel(channel)))
}

// ─────────────────────────── rules & sheet marks ───────────────────────────

@Composable
fun DoubleRule(modifier: Modifier = Modifier, color: Color = Caliper.colors.hairline) {
    Canvas(modifier.fillMaxWidth().height(4.dp)) {
        drawLine(color, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.dp.toPx())
        drawLine(color, Offset(0f, 3.5f), Offset(size.width, 3.5f), 1.dp.toPx())
    }
}

@Composable
fun EndOfSheet(modifier: Modifier = Modifier) {
    Text("— END OF SHEET —",
        style = Caliper.type.meta, color = Caliper.colors.ink40,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp))
}

/** Dotted-leader spec row — "model ............. Pixel 8 Pro" */
@Composable
fun SpecRow(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    Row(modifier.fillMaxWidth().heightIn(min = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
        Box(Modifier.weight(1f).padding(horizontal = 6.dp).height(1.dp).drawBehind {
            drawLine(c.ink40, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())))
        })
        Text(value, style = Caliper.type.dataS, color = c.ink, maxLines = 1)
    }
}

// ─────────────────────────── StampBadge ───────────────────────────

@Composable
fun StampBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Caliper.colors.fault,
    rotation: Float = -3f,
    animateIn: Boolean = true
) {
    val reduced = rememberReducedMotion()
    val haptics = rememberCaliperHaptics()
    var stamped by remember { mutableStateOf(!animateIn || reduced) }
    LaunchedEffect(Unit) { if (!stamped) { delay(60); stamped = true; haptics.stamp() } }
    val scale by animateFloatAsState(
        if (stamped) 1f else 1.12f,
        tween(180, easing = CaliperMotion.Ease), label = "stamp"
    )
    Text(
        text.uppercase(),
        style = Caliper.type.meta.copy(fontSize = 13.sp, letterSpacing = 0.12.sp * 10 / 11), // ≈0.12em
        color = color.copy(alpha = 0.85f),
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }
            .border(1.5.dp, color.copy(alpha = 0.85f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics { contentDescription = "status: ${text.lowercase()}" }
    )
}

// ─────────────────────────── HardKey ───────────────────────────

enum class HardKeyVariant { PRIMARY, SECONDARY, DESTRUCTIVE, DISABLED }

@Composable
fun HardKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: HardKeyVariant = HardKeyVariant.SECONDARY,
    enabled: Boolean = true
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val disabled = !enabled || variant == HardKeyVariant.DISABLED

    val bg = when (variant) {
        HardKeyVariant.PRIMARY -> c.ink
        HardKeyVariant.SECONDARY -> Color.Transparent
        HardKeyVariant.DESTRUCTIVE -> c.fault
        HardKeyVariant.DISABLED -> Color.Transparent
    }
    val fg = when (variant) {
        HardKeyVariant.PRIMARY -> c.surface
        HardKeyVariant.DESTRUCTIVE -> Color(0xFFF7F3EA)   // cream always reads on fault red
        else -> if (disabled) c.ink40 else c.ink
    }

    Box(
        modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { val s = if (pressed) 0.98f else 1f; scaleX = s; scaleY = s }
            .then(
                if (disabled) Modifier
                else Modifier.clickable(interactionSource = interaction, indication = null) {
                    haptics.confirm(); onClick()
                }
            )
            .then(
                when {
                    variant == HardKeyVariant.DISABLED -> Modifier.dashedBorder(c.ink40, 1.5.dp)
                    variant == HardKeyVariant.PRIMARY || variant == HardKeyVariant.DESTRUCTIVE -> Modifier
                    else -> Modifier.border(1.5.dp, c.ink)
                }
            )
            .then(
                if (variant == HardKeyVariant.DISABLED)
                    Modifier.drawBehind {   // hatch/dots fill for the disabled key
                        hatch(Rect(Offset.Zero, size), HatchPattern.DOTS, c.ink40.copy(alpha = 0.25f))
                    }
                else Modifier.background(bg)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { role = Role.Button; if (disabled) disabled() },
        contentAlignment = Alignment.Center
    ) {
        Text(label.uppercase(), style = Caliper.type.meta.copy(fontSize = 13.sp), color = fg)
    }
}

// ─────────────────────────── DIPSwitch ───────────────────────────

@Composable
fun DipSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    val knobX by animateDpAsState(
        if (checked) 16.dp else 0.dp,
        spring(dampingRatio = 1f, stiffness = 700f), label = "dip"
    )
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp, 32.dp)
                .background(c.surface)
                .toggleable(
                    value = checked, enabled = enabled, role = Role.Switch,
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { haptics.tick(); onCheckedChange(it) }
                .border(1.dp, c.hairline)
                .padding(4.dp)
        ) {
            Box(Modifier.offset(x = knobX).size(24.dp).background(if (checked) c.ink else c.ink40))
        }
        if (label != null) {
            Spacer(Modifier.width(12.dp))
            Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
        }
        Spacer(Modifier.width(8.dp))
        LedDot(active = checked, dotSize = 5.dp, pulsing = false)
    }
}

// ─────────────────────────── FaderKey (hardware slider) ───────────────────────────

@Composable
fun FaderKey(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    ticks: Int = 5,
    label: String = "",
    valueText: (Float) -> String = { String.format(java.util.Locale.US, "%.2f", it) }
) {
    val c = Caliper.colors
    val span = valueRange.endInclusive - valueRange.start
    var f by remember(value) { mutableStateOf(((value - valueRange.start) / span).coerceIn(0f, 1f)) }

    Column(modifier) {
        if (label.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.uppercase(), style = Caliper.type.meta, color = c.ink60)
                Text(valueText(value), style = Caliper.type.meta, color = c.ink)
            }
        }
        Canvas(
            Modifier.fillMaxWidth().height(36.dp)
                .pointerInput(valueRange) {
                    detectTapGestures { pos ->
                        val nf = (pos.x / size.width).coerceIn(0f, 1f)
                        f = nf; onValueChange(valueRange.start + nf * span)
                    }
                }
                .pointerInput(valueRange) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val nf = (f + dragAmount / size.width).coerceIn(0f, 1f)
                        f = nf; onValueChange(valueRange.start + nf * span)
                    }
                }
                .semantics { contentDescription = "$label fader, ${valueText(value)}" }
        ) {
            val mid = size.height / 2
            drawLine(c.hairline, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
            repeat(ticks) { i ->
                val x = size.width * i / (ticks - 1f)
                drawLine(c.ink40, Offset(x, mid - 4.dp.toPx()), Offset(x, mid + 4.dp.toPx()), 1.dp.toPx())
            }
            val ks = 14.dp.toPx()
            val kx = (size.width * f - ks / 2).coerceIn(0f, size.width - ks)
            drawRect(c.ink, topLeft = Offset(kx, mid - ks / 2), size = Size(ks, ks))
        }
    }
}

// ─────────────────────────── SegKey ───────────────────────────

@Composable
fun <T> SegKey(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelFor: (T) -> String = { it.toString() }
) {
    val c = Caliper.colors
    val haptics = rememberCaliperHaptics()
    Row(
        modifier.fillMaxWidth().height(40.dp).border(1.dp, c.ink),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { i, opt ->
            val sel = opt == selected
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (sel) c.ink else Color.Transparent)
                    .then(if (i > 0) Modifier.drawBehind {
                        drawLine(c.ink, Offset.Zero, Offset(0f, size.height), 1.dp.toPx())
                    } else Modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { haptics.tick(); onSelect(opt) },
                contentAlignment = Alignment.Center
            ) {
                Text(labelFor(opt).uppercase(), style = Caliper.type.meta,
                    color = if (sel) c.surface else c.ink)
            }
        }
    }
}

// ─────────────────────────── OdometerText ───────────────────────────

@Composable
fun OdometerText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = Caliper.type.readoutL,
    color: Color = Caliper.colors.ink,
    staggerMs: Int = 24
) {
    val reduced = rememberReducedMotion()
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            OdometerDigit(
                digit = ch,
                delayMs = if (reduced) 0 else (text.length - 1 - i) * staggerMs,
                style = style, color = color
            )
        }
    }
}

@Composable
private fun OdometerDigit(digit: Char, delayMs: Int, style: TextStyle, color: Color) {
    var shown by remember { mutableStateOf(digit) }
    LaunchedEffect(digit) {
        if (delayMs > 0) delay(delayMs.toLong())
        shown = digit
    }
    AnimatedContent(
        targetState = shown,
        transitionSpec = {
            (slideInVertically(tween(180, easing = CaliperMotion.Ease)) { it / 2 } + fadeIn(tween(120))) togetherWith
                (slideOutVertically(tween(180, easing = CaliperMotion.Ease)) { -it / 2 } + fadeOut(tween(120)))
        },
        label = "digit"
    ) { d ->
        Text(d.toString(), style = style, color = color, softWrap = false, maxLines = 1)
    }
}

// ─────────────────────────── MarginNote ───────────────────────────

@Composable
fun MarginNote(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "NOTE",
    error: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: (() -> Unit)? = null
) {
    val c = Caliper.colors
    Row(
        modifier.fillMaxWidth()
            .background(c.panel)
            .border(1.dp, if (error) c.fault else c.hairline)
    ) {
        if (error) Box(Modifier.width(3.dp).fillMaxHeight().background(c.fault))
        Column(Modifier.weight(1f).padding(12.dp)) {
            Text(title.uppercase(), style = Caliper.type.meta,
                color = if (error) c.fault else c.ink40)
            Spacer(Modifier.height(4.dp))
            Text(message, style = Caliper.type.dataS, color = c.ink)
            if (actionLabel != null) {
                Spacer(Modifier.height(8.dp))
                HardKey(actionLabel, variant = HardKeyVariant.SECONDARY, onClick = onAction)
            }
        }
        if (onDismiss != null) {
            Text("✕", style = Caliper.type.meta, color = c.ink40,
                modifier = Modifier.padding(12.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null
                ) { onDismiss() })
        }
    }
}

// ─────────────────────────── baseline input (FIND field) ───────────────────────────

@Composable
fun BaselineField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Caliper.colors
    BasicTextField(
        value = value, onValueChange = onValueChange, singleLine = true,
        textStyle = Caliper.type.dataS.copy(color = c.ink),
        cursorBrush = SolidColor(c.accent),
        modifier = modifier.drawBehind {
            drawLine(c.hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
        }
    )
}