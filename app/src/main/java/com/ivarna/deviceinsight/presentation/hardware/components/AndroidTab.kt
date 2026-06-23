package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun AndroidTab(info: HardwareInfo) {
    val android = info.androidDetailedInfo

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Android Details", icon = Icons.Filled.Android) {
            InfoRow("Android Version", android.androidVersion)
            InfoRow("API Level",       android.apiLevel.toString(), monospace = true)
            InfoRow("Security Patch",  android.securityPatch)
            InfoRow("Rooted Device",   if (android.isRooted) "Yes" else "No")
            InfoRow("Android ID",      android.androidId,   monospace = true)
            InfoRow("Baseband",        android.baseband)
            InfoRow("Build ID",        android.buildId,     monospace = true)
            InfoRow("Codename",        android.codename)
            InfoRow("Fingerprint",     android.fingerprint, monospace = true)
            InfoRow("ID",              android.id,          monospace = true)
            InfoRow("Incremental",     android.incremental, monospace = true)
        }

        InfoSection(title = "Java Runtime", icon = Icons.Filled.Code) {
            InfoRow("Java Runtime",  android.javaRuntimeVersion)
            InfoRow("Java VM",       android.javaVmVersion)
            InfoRow("Java VM Heap",  android.javaVmHeapSize, monospace = true)
        }

        InfoSection(title = "Kernel & System", icon = Icons.Filled.Terminal) {
            InfoRow("Kernel Arch",           android.kernelArchitecture)
            InfoRow("Kernel Version",        android.kernelVersion,  monospace = true)
            InfoRow("Tags",                  android.tags)
            InfoRow("Type",                  android.type)
            InfoRow("Google Play Services",  android.gmsVersion)
            InfoRow("Huawei Services",       android.hmsVersion)
        }

        InfoSection(title = "Libraries", icon = Icons.Filled.Book) {
            InfoRow("OpenSSL Version",     android.openSslVersion,     monospace = true)
            InfoRow("ZLib Version",        android.zLibVersion,        monospace = true)
            InfoRow("ICU CLDR Version",    android.icuCldrVersion,     monospace = true)
            InfoRow("ICU Library Version", android.icuLibraryVersion,  monospace = true)
            InfoRow("ICU Unicode Version", android.icuUnicodeVersion,  monospace = true)
        }

        InfoSection(title = "Localization", icon = Icons.Filled.Language) {
            InfoRow("Language",  android.androidLanguage)
            InfoRow("Time Zone", android.configuredTimeZone)
            InfoRow("Uptime",    android.upTime, monospace = true)
        }
    }
}
