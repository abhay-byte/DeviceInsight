package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.PlexMonoFamily

// CALIPER flat hardware chrome — replaces glass InfoSection/InfoRow/StatBadge.

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    monospace: Boolean = false
) {
    val c = Caliper.colors
    val resolvedColor = valueColor ?: c.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(3.dp)
                .background(c.ink40)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = Caliper.type.dataS.copy(fontSize = 12.sp),
            color = c.ink60,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = if (monospace)
                Caliper.type.dataS.copy(fontFamily = PlexMonoFamily, fontSize = 12.sp)
            else
                Caliper.type.dataS.copy(fontSize = 12.sp),
            color = resolvedColor,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(0.55f)
                .padding(start = 8.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable () -> Unit
) {
    val c = Caliper.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = Caliper.type.meta,
                color = c.ink,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(c.panel)
                .border(1.dp, c.hairline)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            content()
        }
    }
}

@Composable
fun StatBadge(label: String, value: String, color: Color? = null, modifier: Modifier = Modifier) {
    val c = Caliper.colors
    // §4.2: accent is interactive-only, never data — defaults render in ink.
    val resolvedColor = color ?: c.ink
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(c.panel)
            .border(1.dp, c.hairline)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = Caliper.type.readoutL.copy(fontSize = 20.sp),
            color = resolvedColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = Caliper.type.meta,
            color = c.ink60
        )
    }
}

@Composable
fun UsageBar(
    label: String,
    value: Float, // 0.0 - 1.0
    color: Color? = null
) {
    val c = Caliper.colors
    // §4.2: accent is interactive-only — bars carry channel color when passed, ink by default.
    val resolvedColor = color ?: c.ink
    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedWidth by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(800),
        label = "usageBar"
    )
    LaunchedEffect(value) { animTarget = value }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = Caliper.type.meta,
                color = c.ink60
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = Caliper.type.meta.copy(fontFamily = PlexMonoFamily),
                color = resolvedColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(c.hairline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWidth)
                    .height(6.dp)
                    .background(resolvedColor)
            )
        }
    }
}