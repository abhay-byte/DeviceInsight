package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun DisplayTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Display", icon = Icons.Filled.Monitor) {
            InfoRow("Screen Resolution", info.resolution,         monospace = true)
            InfoRow("Technology",        info.displayTechnology)
            InfoRow("Screen Size",       info.physicalSize)
            InfoRow("Screen Diagonal",   info.diagonalSize)
            InfoRow("Pixel Density",     info.density,            monospace = true)
            InfoRow("xdpi / ydpi",       "${info.xdpi.toInt()} / ${info.ydpi.toInt()} dpi", monospace = true)
        }

        InfoSection(title = "Graphics Processor", icon = Icons.Filled.DeveloperBoard) {
            InfoRow("GPU Vendor",   info.gpuVendor)
            InfoRow("GPU Renderer", info.gpuRenderer)
            if (info.gpuCores > 0) {
                InfoRow("GPU Cores", info.gpuCores.toString(), monospace = true)
            }
        }

        InfoSection(title = "Settings", icon = Icons.Filled.Tune) {
            InfoRow("Refresh Rate",       "${info.refreshRate.toInt()} Hz", monospace = true)
            InfoRow("Default Orientation", info.defaultOrientation)
        }
    }
}
