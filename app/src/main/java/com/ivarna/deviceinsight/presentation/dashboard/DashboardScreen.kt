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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.components.CircularGauge
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.components.CpuUtilizationGraph
import com.ivarna.deviceinsight.presentation.components.CpuMultiCoreFrequencyGraph
import com.ivarna.deviceinsight.presentation.components.PowerConsumptionGraph
import com.ivarna.deviceinsight.presentation.components.RamUsageGraph

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "SYSTEM ONLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = uiState?.let { "UPTIME: ${it.uptime.uppercase()}" } ?: "LOADING...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            // Performance Overview (Gauges)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularGauge(
                                value = uiState?.cpuUsage ?: 0f,
                                label = "CPU",
                                color = MaterialTheme.colorScheme.primary,
                                size = 130.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState?.let { String.format("%.0f°C", it.cpuTemperature) } ?: "--°C",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                                uiState?.cpuGovernor?.let { governor ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = governor.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularGauge(
                                value = uiState?.ramUsage ?: 0f,
                                label = "RAM",
                                color = MaterialTheme.colorScheme.tertiary,
                                size = 130.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val usedGb = (uiState?.ramUsedBytes ?: 0L) / (1024f * 1024f * 1024f)
                            val totalGb = (uiState?.ramTotalBytes ?: 0L) / (1024f * 1024f * 1024f)
                            Text(
                                text = String.format("%.1f / %.1f GB", usedGb, totalGb),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }

            // CPU Multi-Core Frequency Graph
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CpuMultiCoreFrequencyGraph(
                            coreHistory = uiState?.cpuCoreHistory ?: emptyList(),
                            maxFreq = uiState?.maxCpuFrequency ?: 3000,
                            modifier = Modifier.height(220.dp)
                        )
                    }
                }
            }

            // Network Activity
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Icon(
                                Icons.Filled.Router, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "NETWORK TRAFFIC", 
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            NetworkMetric(
                                icon = Icons.Filled.ArrowDownward,
                                label = "DL",
                                speed = uiState?.networkDownloadSpeed ?: "0 B/S",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            NetworkMetric(
                                icon = Icons.Filled.ArrowUpward,
                                label = "UL",
                                speed = uiState?.networkUploadSpeed ?: "0 B/S",
                                color = Color(0xFF00E676), // Neon Green
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Detailed Storage & Memory
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailedResourceCard(
                        title = "Storage",
                        icon = Icons.Filled.Storage,
                        usedLabel = "Used",
                        usedValue = "${((uiState?.storageUsedPerc ?: 0f) * 100).toInt()}%",
                        subtext = "${uiState?.storageFreeGb ?: "0"} GB free",
                        progress = uiState?.storageUsedPerc ?: 0f,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    DetailedResourceCard(
                        title = "Swap",
                        icon = Icons.Filled.Memory,
                        usedLabel = "Used",
                        usedValue = uiState?.let { "${it.swapUsedBytes / (1024 * 1024)}MB" } ?: "0MB",
                        subtext = uiState?.let { "Total ${it.swapTotalBytes / (1024 * 1024)}MB" } ?: "0MB",
                        progress = uiState?.let { if(it.swapTotalBytes > 0) it.swapUsedBytes.toFloat() / it.swapTotalBytes else 0f } ?: 0f,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Power & Thermal Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DeviceThermostat, 
                                    null, 
                                    tint = MaterialTheme.colorScheme.error, 
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "THERMAL HUB", 
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                ThermalItem("CORE", "${uiState?.cpuTemperature ?: "--"}°C")
                                ThermalItem("BOARD", "${uiState?.temperature ?: "--"}°C")
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "POWER DRAW", 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            val powerConsumption = uiState?.powerConsumption
                            val powerText = if (powerConsumption != null) {
                                if (powerConsumption > 0) String.format("+%.2f", powerConsumption)
                                else String.format("%.2f", powerConsumption)
                            } else "--"
                            
                            Text(
                                text = "${powerText}W",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Black
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // CPU Usage History
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

            // RAM Usage History
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RamUsageGraph(
                            dataPoints = uiState?.ramHistory ?: emptyList(),
                            modifier = Modifier.height(180.dp)
                        )
                    }
                }
            }
            
            // Power History with Control
            item {
                val powerMultiplier by viewModel.powerMultiplier.collectAsStateWithLifecycle()
                
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "POWER ANALYSIS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Multiplier Chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listOf(0.01f, 0.1f, 1f, 10f, 100f)) { mult ->
                                val label = when(mult) {
                                    0.01f -> "0.01X"
                                    0.1f -> "0.1X"
                                    1f -> "1.0X"
                                    10f -> "10X"
                                    100f -> "100X"
                                    else -> "${mult}X"
                                }
                                FilterChip(
                                    selected = powerMultiplier == mult,
                                    onClick = { viewModel.setPowerMultiplier(mult) },
                                    label = { 
                                        Text(
                                            label, 
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                        ) 
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                        containerColor = Color.Transparent,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = powerMultiplier == mult,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                        selectedBorderColor = MaterialTheme.colorScheme.primary
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
fun NetworkMetric(
    icon: ImageVector,
    label: String,
    speed: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            speed.uppercase(), 
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Composable
fun DetailedResourceCard(
    title: String,
    icon: ImageVector,
    usedLabel: String,
    usedValue: String,
    subtext: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        borderColor = color.copy(alpha = 0.1f),
        containerColor = color.copy(alpha = 0.03f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title.uppercase(), 
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    usedLabel, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    usedValue, 
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                subtext, 
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun ThermalItem(label: String, value: String) {
    Column {
        Text(
            label, 
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            value, 
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold
            )
        )
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
