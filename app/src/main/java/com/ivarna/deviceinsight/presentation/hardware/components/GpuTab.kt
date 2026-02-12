package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun GpuTab(info: HardwareInfo) {
    val gpu = info.gpuDetailedInfo
    
    // Extract clean version numbers for the header
    val cleanGlesVersion = gpu.openGlVersion.let { 
        if (it.contains("OpenGL ES ")) {
            it.substringAfter("OpenGL ES ").split(" ").firstOrNull() ?: "3.2"
        } else "3.2"
    }
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        // GPU Summary Header
        InfoSection(title = "GPU Summary") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = gpu.vulkanDeviceName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = gpu.openGlVendor,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem("Cores", info.gpuCores.toString())
                    SummaryItem("API", "Vulkan 1.3")
                    SummaryItem("GLES", cleanGlesVersion)
                }
            }
        }

        InfoSection(title = "OpenGL ES Details") {
            InfoRow("Renderer", gpu.openGlRenderer)
            InfoRow("Vendor", gpu.openGlVendor)
            InfoRow("Version", gpu.openGlVersion)
            InfoRow("Shader Version", "OpenGL ES GLSL ES 3.20")
            
            Spacer(modifier = Modifier.height(12.dp))
            DetailedListHeader("Extensions", gpu.openGlExtensions.size)
            ExtensionList(gpu.openGlExtensions)
        }

        InfoSection(title = "Vulkan Device") {
            InfoRow("Device Name", gpu.vulkanDeviceName)
            InfoRow("Device Type", gpu.vulkanDeviceType)
            InfoRow("Vendor ID", gpu.vulkanVendorId)
            InfoRow("Device ID", gpu.vulkanDeviceId)
            InfoRow("VRAM Size", gpu.vulkanMemorySize)
            InfoRow("API Version", gpu.vulkanApiVersion)
            InfoRow("Driver Version", gpu.vulkanDriverVersion)
            InfoRow("Device UUID", gpu.vulkanDeviceUuid)
        }

        InfoSection(title = "Vulkan Features") {
            gpu.vulkanFeatures.forEach { (feature, supported) ->
                FeatureRow(feature, supported)
            }
        }

        InfoSection(title = "Vulkan Limits") {
            gpu.vulkanLimits.forEach { (limit, value) ->
                InfoRow(limit, value)
            }
        }

        if (gpu.vulkanExtensions.isNotEmpty()) {
            InfoSection(title = "Vulkan Extensions") {
                DetailedListHeader("Extensions", gpu.vulkanExtensions.size)
                ExtensionList(gpu.vulkanExtensions)
            }
        }
    }
}

@Composable
fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeatureRow(label: String, supported: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (supported) "Supported" else "Not Supported",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (supported) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

@Composable
fun DetailedListHeader(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun ExtensionList(extensions: List<String>) {
    Column(modifier = Modifier.padding(start = 4.dp)) {
        extensions.forEach { ext ->
            Text(
                text = ext,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
            )
        }
    }
}


