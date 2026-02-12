package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo

@Composable
fun DirectoriesTab(info: HardwareInfo) {
    val dir = info.directoryInfo
    
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        InfoSection(title = "Directories") {
            InfoRow("Data", dir.data)
            InfoRow("Root", dir.root)
            InfoRow("Java Home", dir.javaHome)
            InfoRow("Download/Cache Content", dir.downloadCache)
        }

        InfoSection(title = "External Storage") {
            InfoRow("Primary External Storage", dir.externalStorage.primary)
            InfoRow("External Files", dir.externalStorage.externalFiles)
            InfoRow("Alarms", dir.externalStorage.alarms)
            InfoRow("DCIM", dir.externalStorage.dcim)
            InfoRow("Documents", dir.externalStorage.documents)
            InfoRow("Downloads", dir.externalStorage.downloads)
            InfoRow("Movies", dir.externalStorage.movies)
            InfoRow("Music", dir.externalStorage.music)
            InfoRow("Notifications", dir.externalStorage.notifications)
            InfoRow("Pictures", dir.externalStorage.pictures)
            InfoRow("Podcasts", dir.externalStorage.podcasts)
            InfoRow("Ringtones", dir.externalStorage.ringtones)
        }

        InfoSection(title = "Mount Points") {
            dir.mountPoints.forEach { mount ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    InfoRow(mount.path, "Device: ${mount.device}")
                    InfoRow("File System", mount.fileSystem)
                    InfoRow("Access", if (mount.isReadOnly) "Read-Only" else "Read-Write")
                }
            }
        }
    }
}
