package com.ivarna.deviceinsight.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.mapper.SocLogoRepository
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.presentation.components.CircularGauge
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.components.GlowStatBlock
import com.ivarna.deviceinsight.presentation.components.QuickMetricCard
import com.ivarna.deviceinsight.presentation.components.SectionDivider

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceCard by viewModel.deviceCard.collectAsStateWithLifecycle()
    val data = uiState

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top-right radial glow (per design spec)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(380.dp)
                .offset(x = 90.dp, y = (-80).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Compact Device Card ────────────────────────────────────
            item {
                DeviceCard(
                    metrics = data,
                    info = deviceCard
                )
            }

            // ─── Hero: CPU + RAM Gauges (50/50) ─────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CpuHeroGauge(
                        metrics = data,
                        modifier = Modifier.weight(1f)
                    )
                    RamHeroGauge(
                        metrics = data,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─── GPU + Thermal Strip ────────────────────────────────────
            item { GpuThermalStrip(metrics = data) }

            // ─── Quick Metric Grid (3 cols) ─────────────────────────────
            item { QuickMetricGrid(metrics = data) }

            // ─── Power + FPS Strip ──────────────────────────────────────
            item { PowerFpsStrip(metrics = data) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compact Device Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DeviceCard(
    metrics: DashboardMetrics?,
    info: DeviceCardInfo
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val socLogoUrl = remember(info.cpuModel) {
        SocLogoRepository().logoUrlFor(info.cpuModel)
    }
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = primary.copy(alpha = 0.04f),
        borderColor = primary.copy(alpha = 0.16f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // ── Top row: SoC logo · device name · uptime ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (socLogoUrl != null) {
                        AsyncImage(
                            model = socLogoUrl,
                            contentDescription = "SoC logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Memory,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        color = primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.dashboard_status_online).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = info.deviceName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dashboard_uptime_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = metrics?.uptime?.uppercase() ?: stringResource(R.string.common_dash),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Detail row: SoC chip · CPU · GPU · RAM · Swap ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailPill(
                    icon = Icons.Filled.DeveloperBoard,
                    label = stringResource(R.string.dashboard_device_chip),
                    value = info.cpuModel.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.common_dash),
                    color = primary,
                    modifier = Modifier.weight(1f),
                    logoUrl = remember(info.cpuModel) {
                        SocLogoRepository().logoUrlFor(info.cpuModel)
                    }
                )
                DetailPill(
                    icon = Icons.Filled.Memory,
                    label = stringResource(R.string.ram_label),
                    value = metrics?.let {
                        "${"%.1f".format(it.ramUsedBytes / (1024f * 1024f * 1024f))}/" +
                            "${"%.1f".format(it.ramTotalBytes / (1024f * 1024f * 1024f))}G"
                    } ?: stringResource(R.string.common_dash),
                    color = tertiary,
                    modifier = Modifier.weight(1f)
                )
                DetailPill(
                    icon = Icons.Filled.SwapHoriz,
                    label = stringResource(R.string.stat_swap),
                    value = metrics?.let {
                        if (it.swapTotalBytes > 0) {
                            "${it.swapUsedBytes / (1024 * 1024)}/${it.swapTotalBytes / (1024 * 1024)}M"
                        } else stringResource(R.string.common_off).uppercase()
                    } ?: stringResource(R.string.common_dash),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailPill(
                    icon = Icons.Filled.Memory,
                    label = stringResource(R.string.gpu_label),
                    value = info.gpuModel.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.common_dash),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun DetailPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    logoUrl: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                ),
                color = color
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (logoUrl != null) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = maxLines,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CPU Hero Gauge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CpuHeroGauge(
    metrics: DashboardMetrics?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = primary.copy(alpha = 0.04f),
        borderColor = primary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.cpu_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = primary
                    )
                }
                metrics?.cpuGovernor?.takeIf { it.isNotBlank() }?.let { governor ->
                    Surface(
                        color = primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = governor.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            ),
                            color = primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CircularGauge(
                value = metrics?.cpuUsage ?: 0f,
                label = stringResource(R.string.dashboard_usage_label).uppercase(),
                color = primary,
                size = 140.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlowStatBlock(
                    label = stringResource(R.string.stat_temp),
                    value = metrics?.cpuTemperature?.let {
                        stringResource(R.string.format_temp_celsius, it)
                    } ?: stringResource(R.string.common_celsius_unknown),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                )
                GlowStatBlock(
                    label = stringResource(R.string.stat_freq),
                    value = metrics?.cpuCoreFrequencies?.maxOrNull()?.let { maxMhz ->
                        stringResource(R.string.format_gb, maxMhz / 1000f)
                    } ?: stringResource(R.string.common_dash),
                    color = primary
                )
                GlowStatBlock(
                    label = stringResource(R.string.stat_cores),
                    value = stringResource(
                        R.string.format_fps,
                        metrics?.cpuCoreFrequencies?.size ?: 0
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RAM Hero Gauge
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RamHeroGauge(
    metrics: DashboardMetrics?,
    modifier: Modifier = Modifier
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = tertiary.copy(alpha = 0.04f),
        borderColor = tertiary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Memory,
                        contentDescription = null,
                        tint = tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.ram_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = tertiary
                    )
                }
                Surface(
                    color = tertiary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_live_badge).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontSize = 9.sp
                        ),
                        color = tertiary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            CircularGauge(
                value = metrics?.ramUsage ?: 0f,
                label = stringResource(R.string.dashboard_usage_label).uppercase(),
                color = tertiary,
                size = 140.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlowStatBlock(
                    label = stringResource(R.string.stat_used),
                    value = metrics?.ramUsedBytes?.let {
                        stringResource(R.string.format_gb, it / (1024f * 1024f * 1024f))
                    } ?: stringResource(R.string.common_dash),
                    color = tertiary
                )
                GlowStatBlock(
                    label = stringResource(R.string.stat_total),
                    value = metrics?.ramTotalBytes?.let {
                        stringResource(R.string.format_gb, it / (1024f * 1024f * 1024f))
                    } ?: stringResource(R.string.common_dash),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlowStatBlock(
                    label = stringResource(R.string.stat_swap),
                    value = metrics?.let {
                        if (it.swapTotalBytes > 0) {
                            stringResource(R.string.format_mb, (it.swapUsedBytes / (1024 * 1024)).toInt())
                        } else {
                            stringResource(R.string.common_off).uppercase()
                        }
                    } ?: stringResource(R.string.common_dash),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GPU + Thermal Strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GpuThermalStrip(metrics: DashboardMetrics?) {
    val secondary = MaterialTheme.colorScheme.secondary
    val error = MaterialTheme.colorScheme.error

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = secondary.copy(alpha = 0.04f),
        borderColor = secondary.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DeveloperBoard,
                        contentDescription = null,
                        tint = secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_gpu_thermal_title).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = secondary
                    )
                }
                Text(
                    text = metrics?.gpuModel?.takeIf { it.isNotBlank() }?.take(24)
                        ?: stringResource(R.string.common_dash_long),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // GPU LOAD
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            R.string.format_percent_value,
                            ((metrics?.gpuUsage ?: 0f) * 100).toInt()
                        ),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = secondary
                        )
                    )
                    Text(
                        text = stringResource(R.string.stat_gpu_load).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                )
                // GPU TEMP
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val gpuT = metrics?.gpuTemp ?: 0f
                    val gpuColor = when {
                        gpuT >= 70f -> error
                        gpuT >= 55f -> MaterialTheme.colorScheme.tertiary
                        gpuT > 0f -> secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        text = if (gpuT > 0f) {
                            stringResource(R.string.format_temp_celsius, gpuT)
                        } else {
                            stringResource(R.string.common_celsius_unknown)
                        },
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = gpuColor
                        )
                    )
                    Text(
                        text = stringResource(R.string.stat_gpu_temp).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                )
                // GPU FREQ
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val freqMhz = metrics?.gpuFreqMhz ?: 0
                    val maxMhz = metrics?.gpuMaxFreqMhz ?: 0
                    Text(
                        text = if (freqMhz > 0) {
                            if (maxMhz > 0) "$freqMhz/$maxMhz" else "$freqMhz"
                        } else {
                            stringResource(R.string.common_dash)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (freqMhz > 0) secondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = stringResource(R.string.stat_gpu_freq).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3-Column Quick Metric Grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickMetricGrid(metrics: DashboardMetrics?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionDivider(text = stringResource(R.string.dashboard_system_resources))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickMetricCard(
                icon = Icons.Filled.Bolt,
                label = stringResource(R.string.battery_label),
                value = stringResource(
                    R.string.format_percent_value,
                    metrics?.batteryLevel ?: 0
                ),
                subtext = metrics?.batteryStatus
                    ?.replace('_', ' ')
                    ?.lowercase()
                    ?.replaceFirstChar { it.uppercase() },
                progress = (metrics?.batteryLevel ?: 0) / 100f,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
            QuickMetricCard(
                icon = Icons.Filled.SdStorage,
                label = stringResource(R.string.storage_label),
                value = stringResource(
                    R.string.format_percent_value,
                    ((metrics?.storageUsedPerc ?: 0f) * 100).toInt()
                ),
                subtext = stringResource(
                    R.string.format_storage_free,
                    metrics?.storageFreeGb ?: "0"
                ),
                progress = metrics?.storageUsedPerc ?: 0f,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            QuickMetricCard(
                icon = Icons.Filled.NetworkCheck,
                label = stringResource(R.string.network_label),
                value = (metrics?.networkDownloadSpeed ?: "0")
                    .takeIf { it.isNotBlank() } ?: "0",
                subtext = stringResource(
                    R.string.format_up_speed,
                    (metrics?.networkUploadSpeed ?: "0").takeIf { it.isNotBlank() } ?: "0"
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Power + FPS Strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PowerFpsStrip(metrics: DashboardMetrics?) {
    val primary = MaterialTheme.colorScheme.primary
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Power,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_power_draw).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    val power = metrics?.powerConsumption
                    Text(
                        text = when {
                            power == null -> stringResource(R.string.format_watts_unknown)
                            power > 0f -> stringResource(R.string.format_watts_pos, power)
                            else -> stringResource(R.string.format_watts, power)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = primary
                    )
                }
            }
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dashboard_fps).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.format_fps, metrics?.fps ?: 0),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideogameAsset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}