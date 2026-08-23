package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.mapper.GpuLogoRepository
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.components.PanelCard

@Composable
fun GpuTab(info: HardwareInfo) {
    val gpu = info.gpuDetailedInfo
    val logoRepo = remember { GpuLogoRepository() }
    val logoUrl = remember(gpu.openGlRenderer, gpu.openGlVendor) { logoRepo.urlFor(gpu.openGlRenderer, gpu.openGlVendor) }
    val gpuColor = Caliper.colors.channel(Channels.GPU)

    val cleanGlesVersion = gpu.openGlVersion.let {
        if (it.contains("OpenGL ES "))
            it.substringAfter("OpenGL ES ").split(" ").firstOrNull() ?: "3.2"
        else "3.2"
    }

    Column(modifier = Modifier.padding(bottom = 16.dp)) {

        // ── GPU hero plate (CALIPER flat — real vendor bitmap, never a Material glyph) ──
        PanelCard(title = "GPU", status = {
            Text(gpu.openGlVendor, style = Caliper.type.meta, color = Caliper.colors.ink40)
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "${gpu.vulkanDeviceName} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp),
                        error = painterResource(R.drawable.ic_soc_generic),
                        fallback = painterResource(R.drawable.ic_soc_generic)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.ic_soc_generic),
                        contentDescription = "${gpu.vulkanDeviceName} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(gpu.vulkanDeviceName, style = Caliper.type.dataM, color = Caliper.colors.ink)
                    Text("opengl es $cleanGlesVersion · vulkan 1.3", style = Caliper.type.meta, color = Caliper.colors.ink60)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryItem(label = "Cores", value = if (info.gpuCores > 0) info.gpuCores.toString() else "—")
                SummaryItem(label = "Vulkan API", value = "1.3")
                SummaryItem(label = "OpenGL ES", value = cleanGlesVersion)
            }
        }

        InfoSection(title = "OpenGL ES Details") {
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
            InfoRow("Device Name",    gpu.vulkanDeviceName)
            InfoRow("Device Type",    gpu.vulkanDeviceType)
            InfoRow("Vendor ID",      gpu.vulkanVendorId,      monospace = true)
            InfoRow("Device ID",      gpu.vulkanDeviceId,      monospace = true)
            InfoRow("VRAM Size",      gpu.vulkanMemorySize)
            InfoRow("API Version",    gpu.vulkanApiVersion)
            InfoRow("Driver Version", gpu.vulkanDriverVersion, monospace = true)
            InfoRow("Device UUID",    gpu.vulkanDeviceUuid,    monospace = true)
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
fun SummaryItem(label: String, value: String, color: androidx.compose.ui.graphics.Color? = null) {
    val c = Caliper.colors
    // §4.2: accent is interactive-only — data renders in ink unless a channel color is passed
    val resolvedColor = color ?: c.ink
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(c.panel)
            .border(1.dp, c.hairline)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = Caliper.type.dataM.copy(fontFamily = FontFamily.Monospace),
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
fun FeatureRow(label: String, supported: Boolean) {
    val c = Caliper.colors
    val resolvedColor = if (supported) c.channel(Channels.NETWORK) else c.ink40

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Caliper.type.dataS.copy(fontSize = 12.sp),
            color = if (supported) c.ink else c.ink40,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (supported) "● FITTED" else "○ NOT FITTED",
            style = Caliper.type.meta.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            color = resolvedColor
        )
    }
}

@Composable
fun DetailedListHeader(title: String, count: Int) {
    val c = Caliper.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = Caliper.type.meta,
            color = c.ink
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = count.toString(),
            style = Caliper.type.meta.copy(fontFamily = FontFamily.Monospace),
            color = c.ink60,
            modifier = Modifier
                .background(c.panel)
                .border(1.dp, c.hairline)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ExtensionList(extensions: List<String>) {
    val c = Caliper.colors
    Column(modifier = Modifier.padding(top = 2.dp)) {
        extensions.forEach { ext ->
            Text(
                text = ext,
                style = Caliper.type.dataS.copy(
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = c.ink60,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.5.dp)
            )
        }
    }
}
