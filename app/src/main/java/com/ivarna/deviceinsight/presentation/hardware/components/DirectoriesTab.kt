package com.ivarna.deviceinsight.presentation.hardware.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.model.MountPoint
import com.ivarna.deviceinsight.ui.caliper.Caliper
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.Fmt
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.components.*
import com.ivarna.deviceinsight.utils.FormattingUtils

@Composable
fun StorageTab(info: HardwareInfo) {
    val dir = info.directoryInfo

    val internalUsed = (info.totalStorage - info.availableStorage).coerceAtLeast(0L)
    val internalTotal = info.totalStorage
    val internalFree = info.availableStorage
    val internalFraction = remember(internalUsed, internalTotal) {
        if (internalTotal > 0) internalUsed.toFloat() / internalTotal else 0f
    }

    val externalUsed = (info.totalExternalStorage - info.availableExternalStorage).coerceAtLeast(0L)
    val externalTotal = info.totalExternalStorage
    val externalFree = info.availableExternalStorage
    val externalFraction = remember(externalUsed, externalTotal) {
        if (externalTotal > 0) externalUsed.toFloat() / externalTotal else 0f
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StorageHeader(
            internalUsed = internalUsed,
            internalTotal = internalTotal,
            internalFree = internalFree,
            fraction = internalFraction
        )

        // ── Internal + External storage — HatchBar allocation map (S-06) ─────
        StorageUsagePanel(
            title = "INTERNAL STORAGE",
            subtitle = "primary user data partition",
            usedBytes = internalUsed,
            totalBytes = internalTotal,
            freeBytes = internalFree,
            fraction = internalFraction
        )

        if (externalTotal > 0) {
            StorageUsagePanel(
                title = "EXTERNAL STORAGE",
                subtitle = "SD card or secondary partition",
                usedBytes = externalUsed,
                totalBytes = externalTotal,
                freeBytes = externalFree,
                fraction = externalFraction
            )
        }

        // ── Directory paths — dotted leaders ─────────────────────────────────
        InfoSection(title = "Directory Paths") {
            SpecRow("data", dir.data)
            SpecRow("root", dir.root)
            SpecRow("java home", dir.javaHome)
            SpecRow("download/cache", dir.downloadCache)
        }

        // ── Mount points ─────────────────────────────────────────────────────
        if (dir.mountPoints.isNotEmpty()) {
            SectionHeader(name = "MOUNT POINTS", count = dir.mountPoints.size)
            dir.mountPoints.forEach { mount ->
                MountPointCard(mount = mount)
            }
        } else {
            InfoSection(title = "Mount Points") {
                Text("—", style = Caliper.type.dataS, color = Caliper.colors.ink40)
            }
        }
    }
}

@Composable
private fun StorageHeader(
    internalUsed: Long,
    internalTotal: Long,
    internalFree: Long,
    fraction: Float
) {
    val c = Caliper.colors
    val percent = remember(fraction) { (fraction * 100).toInt() }

    PanelCard(
        title = "CH-05 · STORAGE — OVERVIEW",
        channel = Channels.STORAGE,
        status = {
            Text("$percent%", style = Caliper.type.meta, color = if (percent >= 90) c.fault else c.ink)
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                OdometerText(
                    text = if (internalTotal > 0) FormattingUtils.formatFileSize(internalUsed) else "—",
                    style = Caliper.type.dataM,
                    color = c.ink
                )
                Text(
                    text = if (internalTotal > 0) "of ${FormattingUtils.formatFileSize(internalTotal)} used" else "no storage detected",
                    style = Caliper.type.meta,
                    color = c.ink60
                )
            }
            if (internalTotal > 0) {
                Text(
                    text = "free ${FormattingUtils.formatFileSize(internalFree)}",
                    style = Caliper.type.meta,
                    color = c.ink60
                )
            }
        }
        if (internalTotal > 0) {
            Spacer(Modifier.height(10.dp))
            val segs = remember(internalUsed, internalFree) {
                listOf(
                    HatchSegment("USED", internalUsed, c.channel(Channels.STORAGE), HatchPattern.SOLID),
                    HatchSegment("FREE", internalFree, c.hairline, HatchPattern.DOTS)
                )
            }
            HatchBar(segments = segs, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(
                text = "FIG. 1 — allocation map · solid = used · dots = free",
                style = Caliper.type.meta,
                color = c.ink40
            )
        }
    }
}

@Composable
private fun StorageUsagePanel(
    title: String,
    subtitle: String,
    usedBytes: Long,
    totalBytes: Long,
    freeBytes: Long,
    fraction: Float
) {
    val c = Caliper.colors
    val percent = remember(fraction) { (fraction * 100).toInt() }
    val totalText = remember(totalBytes) { FormattingUtils.formatFileSize(totalBytes) }
    val usedText = remember(usedBytes) { FormattingUtils.formatFileSize(usedBytes) }
    val freeText = remember(freeBytes) { FormattingUtils.formatFileSize(freeBytes) }

    PanelCard(
        title = title,
        channel = Channels.STORAGE,
        status = {
            Text("$percent%", style = Caliper.type.meta, color = if (percent >= 90) c.fault else c.ink)
        }
    ) {
        Text(subtitle, style = Caliper.type.meta, color = c.ink40)
        Spacer(Modifier.height(8.dp))
        if (totalBytes > 0) {
            val segs = remember(usedBytes, freeBytes) {
                listOf(
                    HatchSegment("USED", usedBytes, c.channel(Channels.STORAGE), HatchPattern.SOLID),
                    HatchSegment("FREE", freeBytes, c.hairline, HatchPattern.DOTS)
                )
            }
            HatchBar(segments = segs, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("used $usedText", style = Caliper.type.meta, color = c.ink)
                Text("free $freeText", style = Caliper.type.meta, color = c.ink60)
                Text("total $totalText", style = Caliper.type.meta, color = c.ink40)
            }
        } else {
            Text("—", style = Caliper.type.dataS, color = c.ink40)
        }
    }
}

@Composable
private fun SectionHeader(name: String, count: Int) {
    val c = Caliper.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = Caliper.type.meta, color = c.ink)
            Spacer(Modifier.width(8.dp))
            Text("·  $count", style = Caliper.type.meta, color = c.ink40)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        DoubleRule()
    }
}

@Composable
private fun MountPointCard(mount: MountPoint) {
    val c = Caliper.colors
    PanelCard(
        title = mount.path,
        status = {
            Text(
                text = if (mount.isReadOnly) "RO" else "RW",
                style = Caliper.type.meta,
                color = if (mount.isReadOnly) c.fault else c.ink60
            )
        }
    ) {
        Text(mount.device, style = Caliper.type.meta, color = c.ink60)
        Spacer(Modifier.height(6.dp))
        SpecRow("fs", mount.fileSystem)
        SpecRow("access", if (mount.isReadOnly) "read-only" else "read-write")
    }
}
