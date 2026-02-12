package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun AndroidTab(info: HardwareInfo) {
    val android = info.androidDetailedInfo
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Android Details") {
            InfoRow("Android Version", android.androidVersion)
            InfoRow("API Level", android.apiLevel.toString())
            InfoRow("Security Patch", android.securityPatch)
            InfoRow("Rooted Device", if (android.isRooted) "Yes" else "No")
            InfoRow("Android ID", android.androidId)
            InfoRow("Baseband", android.baseband)
            InfoRow("Build ID", android.buildId)
            InfoRow("Codename", android.codename)
            InfoRow("Fingerprint", android.fingerprint)
            InfoRow("ID", android.id)
            InfoRow("Incremental", android.incremental)
        }

        InfoSection(title = "Java Runtime") {
            InfoRow("Java Runtime", android.javaRuntimeVersion)
            InfoRow("Java VM", android.javaVmVersion)
            InfoRow("Java VM Heap", android.javaVmHeapSize)
        }

        InfoSection(title = "Kernel & System") {
            InfoRow("Kernel Arch", android.kernelArchitecture)
            InfoRow("Kernel Version", android.kernelVersion)
            InfoRow("Tags", android.tags)
            InfoRow("Type", android.type)
            InfoRow("Google Play Services", android.gmsVersion)
            InfoRow("Huawei Services", android.hmsVersion)
        }

        InfoSection(title = "Libraries") {
            InfoRow("OpenSSL Version", android.openSslVersion)
            InfoRow("ZLib Version", android.zLibVersion)
            InfoRow("ICU CLDR Version", android.icuCldrVersion)
            InfoRow("ICU Library Version", android.icuLibraryVersion)
            InfoRow("ICU Unicode Version", android.icuUnicodeVersion)
        }

        InfoSection(title = "Localization") {
            InfoRow("Android Language", android.androidLanguage)
            InfoRow("Time Zone", android.configuredTimeZone)
            InfoRow("UpTime", android.upTime)
        }
    }
}
