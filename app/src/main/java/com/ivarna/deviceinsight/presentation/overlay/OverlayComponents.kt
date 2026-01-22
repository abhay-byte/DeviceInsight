package com.ivarna.deviceinsight.presentation.overlay

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OverlayMetric(
    val id: String,
    val name: String,
    var enabled: Boolean,
    var order: Int
)

@Composable
fun PremiumMetricCard(
    metric: OverlayMetric,
    isDragging: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        label = "elevation"
    )
    
    // Additional alpha for the content background to make it look "lifted"
    val containerAlpha = if (isDragging) 0.9f else 0.6f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Add spacing between items
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isDragging) 1f else 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = metric.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = metric.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}
