package com.ivarna.deviceinsight.presentation.hardware

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.presentation.components.GlassCard

@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    val tabs = listOf("System", "Build", "Display", "Battery", "Sensors")
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 110.dp)
            ) {
                hardwareInfo?.let { info ->
            // Curved Tab Bar
            val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val path = Path().apply {
                        val width = size.width
                        val height = size.height
                        val cornerRadius = 24.dp.toPx()
                        
                        // Top curve
                        moveTo(0f, cornerRadius)
                        quadraticBezierTo(0f, 0f, cornerRadius, 0f)
                        lineTo(width - cornerRadius, 0f)
                        quadraticBezierTo(width, 0f, width, cornerRadius)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = surfaceColor
                    )
                }
                
                // Tab Row
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = title) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content
            when (selectedTabIndex) {
                0 -> SystemTab(info)
                1 -> BuildTab(info)
                2 -> DisplayTab(info)
                3 -> BatteryTab(info)
                4 -> SensorsTab(info)
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}
}
}

@Composable
fun SystemTab(info: HardwareInfo) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Android System",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InfoRow("Version", info.androidVersion)
            InfoRow("API Level", info.apiLevel.toString())
            InfoRow("Security Patch", info.securityPatch)
            InfoRow("Uptime", info.upTime)
            InfoRow("Kernel Version", info.kernelVersion)
            InfoRow("Build ID", info.buildId)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Device Details",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            InfoRow("Manufacturer", info.manufacturer)
            InfoRow("Model", info.model)
            InfoRow("Device Name", info.deviceName)
            InfoRow("Brand", info.brand)
            InfoRow("Board", info.board)
            InfoRow("Hardware", info.hardware)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Additional Info",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            InfoRow("Resolution", info.resolution)
            InfoRow("Density", "${info.density} (${info.densityDpi} dpi)")
            InfoRow("Refresh Rate", "${info.refreshRate} Hz")
        }
    }
}

@Composable
fun BuildTab(info: HardwareInfo) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Build Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InfoRow("Board", info.board)
            InfoRow("Hardware", info.hardware)
            InfoRow("Kernel Version", info.kernelVersion)
            InfoRow("Build ID", info.buildId)
        }
    }
}

@Composable
fun DisplayTab(info: HardwareInfo) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Display",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InfoRow("Resolution", info.resolution)
            InfoRow("Density", "${info.density} (${info.densityDpi} dpi)")
            InfoRow("Refresh Rate", "${info.refreshRate} Hz")
        }
    }
}

@Composable
fun BatteryTab(info: HardwareInfo) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Battery",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InfoRow("Technology", info.batteryTechnology)
            InfoRow("Health", info.batteryHealth)
            InfoRow("Capacity", info.batteryCapacity)
        }
    }
}

@Composable
fun SensorsTab(info: HardwareInfo) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sensors",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            InfoRow("Total Count", info.sensorCount.toString())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Available Sensors",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            if (info.availableSensors.isNotEmpty()) {
                 Text(
                     text = info.availableSensors.take(10).joinToString("\n"),
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
                 if(info.sensorCount > 10) {
                     Text("and ${info.sensorCount - 10} more...", style = MaterialTheme.typography.bodySmall)
                 }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
