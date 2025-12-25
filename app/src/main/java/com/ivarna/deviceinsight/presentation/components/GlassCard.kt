package com.ivarna.deviceinsight.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable { onClick() }
    } else {
        modifier
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(1.dp, borderColor),
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    )
}

@Composable
fun AnimatedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
    delayMillis: Int = 0,
    animationDuration: Int = 500,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = animationDuration),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    GlassCard(
        modifier = modifier
            .scale(scale)
            .then(Modifier.run { if (isVisible) this else Modifier }), // Simplistic alpha application
        shape = shape,
        containerColor = containerColor.copy(alpha = containerColor.alpha * alpha), // Fade in container
        borderColor = borderColor.copy(alpha = borderColor.alpha * alpha),
        onClick = onClick,
        content = content
    )
}
