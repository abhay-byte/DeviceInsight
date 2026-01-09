package com.ivarna.deviceinsight.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.ivarna.deviceinsight.presentation.components.CircularGauge
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.components.CpuUtilizationGraph
import com.ivarna.deviceinsight.presentation.components.PowerConsumptionGraph
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(300.dp)
                .padding(top = 50.dp, end = 50.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = uiState?.let { "Uptime: ${it.uptime}" } ?: "Loading...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Primary Gauges Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            CircularGauge(
                                value = uiState?.cpuUsage ?: 0f,
                                label = "CPU",
                                color = MaterialTheme.colorScheme.primary,
                                size = 120.dp
                            )
                        }
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            CircularGauge(
                                value = uiState?.ramUsage ?: 0f,
                                label = "RAM",
                                color = MaterialTheme.colorScheme.tertiary,
                                size = 120.dp
                            )
                        }
                    }
                }
            }
            
            // CPU Graph Section (New)
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                         CpuUtilizationGraph(
                            dataPoints = uiState?.cpuHistory ?: emptyList(),
                            modifier = Modifier.height(180.dp)
                        )
                    }
                }
            }

            // GPU and Temp Row
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        CircularGauge(
                            value = uiState?.gpuUsage ?: 0f,
                            label = "GPU",
                            color = MaterialTheme.colorScheme.secondary,
                            size = 100.dp
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                uiState?.gpuModel ?: "Adreno GPU",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                uiState?.let { "${it.temperature}°C" } ?: "--°C",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Quick Metrics Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickMetricItem(
                        icon = Icons.Filled.BatteryFull,
                        label = "Battery",
                        value = uiState?.let { "${it.batteryLevel}%" } ?: "--%",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricItem(
                        icon = Icons.Filled.Storage,
                        label = "Storage",
                        value = uiState?.storageFreeGb ?: "-- GB",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    QuickMetricItem(
                        icon = Icons.Filled.Speed,
                        label = "Network",
                        value = uiState?.networkSpeed ?: "0 KB/s",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Swap Metrics
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Swap",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Used:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState?.let { "${it.swapUsedBytes / (1024 * 1024)} MB" } ?: "0 MB",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = uiState?.let { "${it.swapTotalBytes / (1024 * 1024)} MB" } ?: "0 MB",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Power Graph
            item {
                val powerMultiplier by viewModel.powerMultiplier.collectAsStateWithLifecycle()
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Power Consumption",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Multiplier Chips
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.01f, 0.1f, 1f, 10f, 100f).forEach { mult ->
                                val label = when(mult) {
                                    0.01f -> "0.01x"
                                    0.1f -> "0.1x"
                                    1f -> "1x"
                                    10f -> "10x"
                                    100f -> "100x"
                                    else -> "${mult}x"
                                }
                                val selected = powerMultiplier == mult
                                androidx.compose.material3.FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setPowerMultiplier(mult) },
                                    label = { Text(label) },
                                    colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        PowerConsumptionGraph(
                            dataPoints = uiState?.powerHistory ?: emptyList(),
                            multiplier = powerMultiplier,
                            modifier = Modifier.height(180.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMetricItem(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            horizontalAlignment = Alignment.Start, 
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
