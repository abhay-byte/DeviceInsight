package com.ivarna.deviceinsight.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint

@Composable
fun CircularGauge(
    value: Float, // 0.0 to 1.0
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    strokeWidth: Dp = 8.dp
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1500),
        label = "gauge"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 240f
            val startAngle = 150f
            val canvasStrokeWidth = strokeWidth.toPx()
            
            // Add internal padding to the drawing logic to prevent glow cutoff
            val paddingPx = 16.dp.toPx() 
            val gaugeSize = size.toPx() - (paddingPx * 2)
            val centerOffset = paddingPx + (canvasStrokeWidth / 2f)
            
            // Draw background track with subtle glow
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerOffset, centerOffset),
                size = Size(gaugeSize - canvasStrokeWidth, gaugeSize - canvasStrokeWidth),
                style = Stroke(width = canvasStrokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Gradient for the progress arc
            val brush = Brush.sweepGradient(
                colors = listOf(
                    color.copy(alpha = 0.6f),
                    color,
                    color
                ),
                center = Offset(size.toPx() / 2, size.toPx() / 2)
            )

            // Draw Glow (using native canvas for blur)
            drawIntoCanvas { canvas ->
                val glowPaint = NativePaint().apply {
                    isAntiAlias = true
                    this.color = color.toArgb()
                    style = NativePaint.Style.STROKE
                    this.strokeWidth = canvasStrokeWidth * 1.5f
                    strokeCap = NativePaint.Cap.ROUND
                    maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
                }
                
                canvas.nativeCanvas.drawArc(
                    centerOffset,
                    centerOffset,
                    size.toPx() - centerOffset,
                    size.toPx() - centerOffset,
                    startAngle,
                    sweepAngle * animatedValue,
                    false,
                    glowPaint
                )
            }

            // Draw progress arc
            drawArc(
                brush = brush,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedValue,
                useCenter = false,
                topLeft = Offset(centerOffset, centerOffset),
                size = Size(gaugeSize - canvasStrokeWidth, gaugeSize - canvasStrokeWidth),
                style = Stroke(width = canvasStrokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = "${(animatedValue * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}
