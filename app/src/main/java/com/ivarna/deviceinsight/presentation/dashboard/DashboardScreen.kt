package com.ivarna.deviceinsight.presentation.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.data.mapper.SocLogoRepository
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.presentation.components.CircularGauge
import com.ivarna.deviceinsight.presentation.components.GlassCard
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
        // Top-right radial glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(420.dp)
                .offset(x = 110.dp, y = (-90).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Mid-left subtle accent glow
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(360.dp)
                .offset(x = (-140).dp, y = 100.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── 1. Device Info Hero ─────────────────────────────────────
            item {
                CompactDeviceCard(
                    info = deviceCard,
                    metrics = data
                )
            }

            // ─── 2. CPU & RAM Hero (clean circular gauges) ───────────────
            item {
                CpuRamHeroSection(metrics = data)
            }

            // ─── 3. System Snapshot Grid (GPU · Battery · Storage · Network)
            item {
                QuickMetricGrid(metrics = data)
            }

            // ─── 4. Power Draw & FPS Live Strip ──────────────────────────
            item {
                PowerFpsStrip(metrics = data)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Device Info Hero Card
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactDeviceCard(
    info: DeviceCardInfo,
    metrics: DashboardMetrics?
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val socLogoUrl = remember(info.cpuModel) {
        SocLogoRepository().logoUrlFor(info.cpuModel)
    }

    // Infinite breathing glow for live beacon
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beaconAlpha"
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        borderColor = primary.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // ── Header: SoC Logo + Device Name + Live Beacon ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SoC Logo Container
                val socFamily = remember(info.cpuModel) {
                    SocLogoRepository().familyFor(info.cpuModel)
                }
                val (socColor, socBadgeText) = when (socFamily) {
                    com.ivarna.deviceinsight.data.mapper.SocFamily.SNAPDRAGON -> Color(0xFFFF334B) to "SD"
                    com.ivarna.deviceinsight.data.mapper.SocFamily.MEDIATEK -> Color(0xFFFF9800) to "MTK"
                    com.ivarna.deviceinsight.data.mapper.SocFamily.TENSOR -> Color(0xFF00E5FF) to "TSR"
                    com.ivarna.deviceinsight.data.mapper.SocFamily.EXYNOS -> Color(0xFF2979FF) to "EXY"
                    com.ivarna.deviceinsight.data.mapper.SocFamily.UNKNOWN -> primary to "SOC"
                }

                val logoDrawableRes = remember(info.cpuModel) {
                    SocLogoRepository().logoDrawableResFor(info.cpuModel)
                }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    socColor.copy(alpha = 0.16f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            socColor.copy(alpha = 0.40f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = logoDrawableRes),
                        contentDescription = "${info.cpuModel} logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }

                // Device Identity
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = info.cpuModel.takeIf { it.isNotBlank() } ?: "SYSTEM ON CHIP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.2).sp
                        ),
                        color = primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = info.deviceName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Live Beacon & Uptime
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            Color(0xFF00E676).copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676).copy(alpha = beaconAlpha))
                            )
                            Text(
                                text = stringResource(R.string.dashboard_status_online).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = Color(0xFF00E676)
                            )
                        }
                    }

                    Text(
                        text = metrics?.uptime?.uppercase() ?: stringResource(R.string.common_dash),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Clean OS / Architecture Badges ──
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactChip(
                    icon = Icons.Filled.Android,
                    text = info.androidVersion.ifBlank { "Android" },
                    tint = primary
                )
                CompactChip(
                    icon = Icons.Filled.Memory,
                    text = "${metrics?.cpuTotalCores ?: info.totalCores} Cores · " +
                        (metrics?.cpuArchitecture?.takeIf { it.isNotBlank() } ?: info.cpuArchitecture.ifBlank { "ARM64" }),
                    tint = tertiary
                )
            }
        }
    }
}

@Composable
private fun CompactChip(
    icon: ImageVector,
    text: String,
    tint: Color
) {
    Surface(
        color = tint.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, tint.copy(alpha = 0.22f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. CPU & RAM Hero (clean circular gauges + summary badges)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CpuRamHeroSection(metrics: DashboardMetrics?) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val cpuUsage = metrics?.cpuUsage ?: 0f
    val ramUsedGb = (metrics?.ramUsedBytes ?: 0L) / (1024f * 1024f * 1024f)
    val ramTotalGb = (metrics?.ramTotalBytes ?: 0L) / (1024f * 1024f * 1024f)
    val ramRatio = if (ramTotalGb > 0) (ramUsedGb / ramTotalGb).coerceIn(0f, 1f) else 0f

    val avgFreq = metrics?.cpuCoreFrequencies.orEmpty().filter { it > 0 }.let { freqs ->
        if (freqs.isNotEmpty()) freqs.average().toInt() else 0
    }
    val clockText = if (avgFreq >= 1000) "%.2f GHz".format(avgFreq / 1000f)
                    else if (avgFreq > 0) "$avgFreq MHz"
                    else stringResource(R.string.common_dash)

    val swapUsedMb = (metrics?.swapUsedBytes ?: 0L) / (1024 * 1024)
    val swapTotalMb = (metrics?.swapTotalBytes ?: 0L) / (1024 * 1024)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        borderColor = primary.copy(alpha = 0.20f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SectionDivider(text = stringResource(R.string.dashboard_cpu_ram))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // ── CPU ──
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularGauge(
                        value = cpuUsage,
                        label = stringResource(R.string.cpu_label),
                        size = 108.dp,
                        color = primary
                    )

                    val cpuTemp = metrics?.cpuTemperature ?: 0f
                    val tempColor = when {
                        cpuTemp >= 70f -> MaterialTheme.colorScheme.error
                        cpuTemp >= 50f -> tertiary
                        cpuTemp > 0f -> Color(0xFF00E676)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBadge(
                            text = if (cpuTemp > 0f) stringResource(R.string.format_temp_celsius, cpuTemp)
                                   else stringResource(R.string.common_celsius_unknown),
                            color = tempColor
                        )
                        MiniBadge(text = clockText, color = primary)
                    }

                    metrics?.cpuGovernor?.takeIf { it.isNotBlank() }?.let { gov ->
                        Text(
                            text = gov.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ── RAM ──
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularGauge(
                        value = ramRatio,
                        label = stringResource(R.string.ram_label),
                        size = 108.dp,
                        color = tertiary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBadge(
                            text = if (ramTotalGb > 0) "${"%.1f".format(ramUsedGb)}/${"%.1f".format(ramTotalGb)} GB"
                                   else stringResource(R.string.common_dash),
                            color = tertiary
                        )
                        MiniBadge(
                            text = if (swapTotalMb > 0) "$swapUsedMb/$swapTotalMb MB" else stringResource(R.string.common_off),
                            color = secondary
                        )
                    }

                    Text(
                        text = if (ramTotalGb > 0) stringResource(R.string.format_gb_available, "%.1f".format((ramTotalGb - ramUsedGb).coerceAtLeast(0f)))
                               else stringResource(R.string.common_dash),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, color.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp
            ),
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. System Snapshot Grid (GPU · Battery · Storage · Network)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickMetricGrid(metrics: DashboardMetrics?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionDivider(text = stringResource(R.string.dashboard_system_resources))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GPU Card — value: load %, subtext: current clock, progress: load
            QuickMetricCard(
                icon = Icons.Filled.Speed,
                label = stringResource(R.string.gpu_label),
                value = stringResource(
                    R.string.format_percent_value,
                    ((metrics?.gpuUsage ?: 0f) * 100).toInt()
                ),
                subtext = if ((metrics?.gpuFreqMhz ?: 0) > 0) {
                    stringResource(R.string.format_mhz, metrics?.gpuFreqMhz ?: 0)
                } else {
                    stringResource(R.string.common_dash)
                },
                progress = metrics?.gpuUsage ?: 0f,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )

            // Battery Card
            QuickMetricCard(
                icon = Icons.Filled.Bolt,
                label = stringResource(R.string.battery_label),
                value = stringResource(
                    R.string.format_percent_value,
                    metrics?.batteryLevel ?: 0
                ),
                subtext = if (metrics?.isCharging == true) {
                    stringResource(R.string.status_charging)
                } else {
                    stringResource(R.string.format_mv, metrics?.batteryVoltage ?: 0)
                },
                progress = (metrics?.batteryLevel ?: 0) / 100f,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Storage Card
            QuickMetricCard(
                icon = Icons.Filled.SdStorage,
                label = stringResource(R.string.storage_label),
                value = stringResource(
                    R.string.format_percent_value,
                    ((metrics?.storageUsedPerc ?: 0f) * 100).toInt()
                ),
                subtext = metrics?.storageFreeGb ?: "0 GB Free",
                progress = metrics?.storageUsedPerc ?: 0f,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Network Card
            QuickMetricCard(
                icon = Icons.Filled.NetworkCheck,
                label = stringResource(R.string.network_label),
                value = (metrics?.networkDownloadSpeed ?: "0")
                    .takeIf { it.isNotBlank() } ?: "0 B/s",
                subtext = stringResource(
                    R.string.format_up_speed,
                    (metrics?.networkUploadSpeed ?: "0").takeIf { it.isNotBlank() } ?: "0 B/s"
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Power Draw & FPS Telemetry Strip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PowerFpsStrip(metrics: DashboardMetrics?) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        borderColor = primary.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Power draw
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
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
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
                            fontWeight = FontWeight.Black
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

            // FPS & Refresh rate
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${metrics?.screenRefreshRate ?: 60}Hz DISPLAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.format_fps, metrics?.fps ?: 0) + " FPS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        ),
                        color = tertiary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = tertiary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideogameAsset,
                        contentDescription = null,
                        tint = tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}