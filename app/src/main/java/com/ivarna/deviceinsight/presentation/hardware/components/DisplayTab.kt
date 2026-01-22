package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun DisplayTab(info: HardwareInfo) {
    Column {
        InfoSection(title = "Screen") {
            InfoRow("Resolution", info.resolution)
            InfoRow("Density", "${info.density} (${info.densityDpi} dpi)")
            InfoRow("Refresh Rate", "${info.refreshRate} Hz")
        }
    }
}
