package com.ivarna.deviceinsight.presentation.hardware

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.components.GlassCard

@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        hardwareInfo?.let { info ->
            // Header
            Text(
                text = "${info.manufacturer} ${info.model}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Codename: ${info.deviceName}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // OS Config
                item {
                    InfoCard(title = "Android System") {
                        InfoRow("Version", info.androidVersion)
                        InfoRow("API Level", info.apiLevel.toString())
                        InfoRow("Security", info.securityPatch)
                        InfoRow("Uptime", info.upTime)
                    }
                }
                
                // Processor / Build
                item {
                     InfoCard(title = "Build Info") {
                        InfoRow("Board", info.board)
                        InfoRow("Hardware", info.hardware)
                        InfoRow("Kernel", info.kernelVersion.take(20) + "...")
                        InfoRow("Build ID", info.buildId)
                    }
                }

                // Display
                item {
                    InfoCard(title = "Display") {
                        InfoRow("Resolution", info.resolution)
                        InfoRow("Density", "${info.density} (${info.densityDpi} dpi)")
                        InfoRow("Refresh Rate", "${info.refreshRate} Hz")
                    }
                }

                // Battery
                item {
                    InfoCard(title = "Battery") {
                        InfoRow("Technology", info.batteryTechnology)
                        InfoRow("Health", info.batteryHealth)
                        InfoRow("Capacity", info.batteryCapacity)
                    }
                }

                // Sensors
                item {
                    InfoCard(title = "Sensors") {
                        InfoRow("Total Count", info.sensorCount.toString())
                        if (info.availableSensors.isNotEmpty()) {
                             Spacer(modifier = Modifier.height(8.dp))
                             Text(
                                 text = info.availableSensors.take(5).joinToString("\n"),
                                 style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                             )
                             if(info.sensorCount > 5) {
                                 Text("...", style = MaterialTheme.typography.bodySmall)
                             }
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
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
