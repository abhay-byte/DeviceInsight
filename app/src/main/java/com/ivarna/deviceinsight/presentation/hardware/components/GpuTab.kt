package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun GpuTab(info: HardwareInfo) {
    val gpu = info.gpuDetailedInfo
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val cleanGlesVersion = gpu.openGlVersion.let {
        if (it.contains("OpenGL ES "))
            it.substringAfter("OpenGL ES ").split(" ").firstOrNull() ?: "3.2"
        else "3.2"
    }

    Column(modifier = Modifier.padding(bottom = 16.dp)) {

        // ── GPU Hero Header ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.12f), secondary.copy(alpha = 0.04f))
                    )
                )
                .border(
                    1.dp,
                    Brush.linearGradient(
                        listOf(primary.copy(alpha = 0.3f), secondary.copy(alpha = 0.1f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.DeveloperBoard,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = gpu.vulkanDeviceName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    ),
                    color = primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = gpu.openGlVendor,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryItem(
                        label = "Cores",
                        value = if (info.gpuCores > 0) info.gpuCores.toString() else "—",
                        color = primary
                    )
                    SummaryItem(label = "Vulkan API",    value = "1.3",             color = secondary)
                    SummaryItem(label = "OpenGL ES",     value = cleanGlesVersion,   color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        InfoSection(title = "OpenGL ES Details", icon = Icons.Filled.DeveloperBoard) {
            InfoRow("Renderer",       gpu.openGlRenderer)
            InfoRow("Vendor",         gpu.openGlVendor)
            InfoRow("Version",        gpu.openGlVersion)
            InfoRow("Shader Version", "OpenGL ES GLSL ES 3.20")

            if (gpu.openGlExtensions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                DetailedListHeader("Extensions", gpu.openGlExtensions.size)
                ExtensionList(gpu.openGlExtensions)
            }
        }

        InfoSection(title = "Vulkan Device") {
            InfoRow("Device Name",   gpu.vulkanDeviceName)
            InfoRow("Device Type",   gpu.vulkanDeviceType)
            InfoRow("Vendor ID",     gpu.vulkanVendorId,     monospace = true)
            InfoRow("Device ID",     gpu.vulkanDeviceId,     monospace = true)
            InfoRow("VRAM Size",     gpu.vulkanMemorySize)
            InfoRow("API Version",   gpu.vulkanApiVersion)
            InfoRow("Driver Version",gpu.vulkanDriverVersion, monospace = true)
            InfoRow("Device UUID",   gpu.vulkanDeviceUuid,   monospace = true)
        }

        InfoSection(title = "Vulkan Features") {
            gpu.vulkanFeatures.forEach { (feature, supported) ->
                FeatureRow(feature, supported)
            }
        }

        if (gpu.vulkanLimits.isNotEmpty()) {
            InfoSection(title = "Vulkan Limits") {
                gpu.vulkanLimits.forEach { (limit, value) ->
                    InfoRow(limit, value, monospace = true)
                }
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
fun SummaryItem(label: String, value: String, color: Color? = null) {
    val resolvedColor = color ?: MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(resolvedColor.copy(alpha = 0.08f))
            .border(1.dp, resolvedColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = resolvedColor
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.8.sp,
                fontSize = 9.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun FeatureRow(label: String, supported: Boolean) {
    val supportedColor = Color(0xFF4ADE80)   // Neon green
    val unsupportedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    val resolvedColor = if (supported) supportedColor else unsupportedColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (supported)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = resolvedColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = if (supported) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    tint = resolvedColor,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = if (supported) "Yes" else "No",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = resolvedColor
                )
            }
        }
    }
    HorizontalDivider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
    )
}

@Composable
fun DetailedListHeader(title: String, count: Int) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            ),
            color = primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            color = primary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                ),
                color = primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun ExtensionList(extensions: List<String>) {
    Column(modifier = Modifier.padding(top = 2.dp)) {
        extensions.forEach { ext ->
            Text(
                text = ext,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.5.dp)
            )
        }
    }
}
