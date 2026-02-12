package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun DisplayTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Display") {
            InfoRow("Screen Resolution", info.resolution)
            InfoRow("Technology", info.displayTechnology)
            InfoRow("Screen Size", info.physicalSize)
            InfoRow("Screen Diagonal", info.diagonalSize)
            InfoRow("Pixel Density", info.density)
            InfoRow("xdpi / ydpi", "${info.xdpi.toInt()} / ${info.ydpi.toInt()} dpi")
        }

        InfoSection(title = "Graphics Processor") {
            InfoRow("GPU Vendor", info.gpuVendor)
            InfoRow("GPU Renderer", info.gpuRenderer)
            if (info.gpuCores > 0) {
                InfoRow("GPU Cores", info.gpuCores.toString())
            }
        }

        InfoSection(title = "Settings") {
            InfoRow("Refresh Rate", "${info.refreshRate.toInt()} Hz")
            InfoRow("Default Orientation", info.defaultOrientation)
        }
    }
}
