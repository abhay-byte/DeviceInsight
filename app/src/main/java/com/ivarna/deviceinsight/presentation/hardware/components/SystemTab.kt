package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.utils.FormattingUtils
import com.ivarna.deviceinsight.utils.FormattingUtils.capitalizeWords

@Composable
fun SystemTab(info: HardwareInfo) {
    val context = LocalContext.current
    
    Column {
        InfoSection(title = "Device") {
            InfoRow("Device Model", info.deviceModelName)
            InfoRow("Device Type", info.deviceType)
            InfoRow("Manufacturer", info.manufacturer)
            InfoRow("Model", info.model)
            InfoRow("Brand", info.brand)
            InfoRow("Board", info.board)
            InfoRow("Device", info.deviceName)
            InfoRow("Hardware", info.hardware)
            InfoRow("Platform", info.platform)
            InfoRow("Product", info.product)
            InfoRow("Serial", info.serial)
        }

        InfoSection(title = "Memory") {
            InfoRow("Installed RAM", info.installedRam)
            InfoRow("Total Memory", FormattingUtils.formatMemorySize(info.totalRam))
            InfoRow("Available Memory", FormattingUtils.formatMemorySize(info.availableRam))
        }

        InfoSection(title = "Internal Storage") {
            InfoRow("Total Space", FormattingUtils.formatFileSize(info.totalStorage))
            InfoRow("Free Space", FormattingUtils.formatFileSize(info.availableStorage))
        }

        InfoSection(title = "Connectivity") {
            InfoRow("Bluetooth Version", info.bluetoothVersion)
        }

        InfoSection(title = "Device Features") {
            info.deviceFeatures.forEach { feature ->
                InfoRow(feature.replace("android.hardware.", "").replace("android.software.", ""), "Yes")
            }
        }

        val android = info.androidDetailedInfo

        InfoSection(title = "Android OS") {
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
            InfoRow("Java Runtime", android.javaRuntimeVersion)
            InfoRow("Java VM", android.javaVmVersion)
            InfoRow("Java VM Heap", android.javaVmHeapSize)
            InfoRow("Kernel Arch", android.kernelArchitecture)
            InfoRow("Kernel Version", android.kernelVersion)
            InfoRow("Tags", android.tags)
            InfoRow("Type", android.type)
            InfoRow("Google Play Services", android.gmsVersion)
            InfoRow("Huawei Services", android.hmsVersion)
            InfoRow("OpenSSL Version", android.openSslVersion)
            InfoRow("ZLib Version", android.zLibVersion)
            InfoRow("ICU CLDR Version", android.icuCldrVersion)
            InfoRow("ICU Library Version", android.icuLibraryVersion)
            InfoRow("ICU Unicode Version", android.icuUnicodeVersion)
            InfoRow("Android Language", android.androidLanguage)
            InfoRow("Time Zone", android.configuredTimeZone)
            InfoRow("UpTime", android.upTime)
        }
    }
}
