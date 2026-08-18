package com.ivarna.deviceinsight.presentation.overlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.presentation.components.ReorderableList
import androidx.compose.ui.res.stringResource

@Composable
fun OverlayScreen(
    viewModel: OverlayViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val onRequestOverlay: () -> Unit = {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
    val onRequestUsage: () -> Unit = {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
    val onRequestShizuku: () -> Unit = {
        viewModel.requestShizukuPermission()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OverlayHeader(
                isRunning = state.isServiceRunning,
                permissions = state.permissions,
                onRequestOverlay = onRequestOverlay,
                onRequestUsage = onRequestUsage,
                onRequestShizuku = onRequestShizuku
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dedicated setup instructions and prominent action cards when permissions are missing
            if (!state.permissions.hasOverlay || !state.permissions.hasUsageStats ||
                (state.permissions.hasShizukuInstalled && !state.permissions.hasShizukuPermission)
            ) {
                PermissionsSetupSection(
                    permissions = state.permissions,
                    onRequestOverlay = onRequestOverlay,
                    onRequestUsage = onRequestUsage,
                    onRequestShizuku = onRequestShizuku
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            StyleSection(
                scaleFactor = state.scaleFactor,
                isHorizontal = state.isHorizontal,
                onScaleChange = viewModel::setScaleFactor,
                onScaleCommit = viewModel::commitScaleFactor,
                onHorizontalChange = viewModel::setHorizontal
            )

            Spacer(modifier = Modifier.height(16.dp))

            FpsModeSection(
                fpsMode = state.fpsMode,
                isShizukuReady = state.permissions.hasShizukuInstalled && state.permissions.hasShizukuPermission,
                isRootReady = state.permissions.hasRoot,
                onSelect = viewModel::setFpsMode,
                onRequestShizuku = onRequestShizuku
            )

            Spacer(modifier = Modifier.height(18.dp))

            MetricsSection(
                metrics = state.metrics,
                onToggle = { id, enabled -> viewModel.toggleMetric(id, enabled) },
                onReorder = { from, to ->
                    viewModel.reorderMetrics(from, to)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            ActionButtons(
                isRunning = state.isServiceRunning,
                canStart = state.permissions.hasOverlay,
                onStart = {
                    val intent = viewModel.buildServiceIntent()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    viewModel.setServiceRunning(true)
                },
                onStop = {
                    context.stopService(Intent(context, com.ivarna.deviceinsight.service.OverlayService::class.java))
                    viewModel.setServiceRunning(false)
                },
                onRequestOverlay = onRequestOverlay
            )

            Spacer(modifier = Modifier.height(160.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun OverlayHeader(
    isRunning: Boolean,
    permissions: OverlayPermissions,
    onRequestOverlay: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val error = MaterialTheme.colorScheme.error

    val statusColor = when {
        isRunning -> primary
        !permissions.hasOverlay -> error
        else -> tertiary
    }
    val statusText = when {
        isRunning -> "Active"
        !permissions.hasOverlay -> "Permission Required"
        else -> "Ready"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.14f),
                        secondary.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.35f),
                        secondary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    statusColor.copy(alpha = 0.25f),
                                    secondary.copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Performance Overlay",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = statusText.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = statusColor
                        )
                    }
                }
                if (isRunning) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            if (!permissions.hasOverlay || !permissions.hasUsageStats ||
                (permissions.hasShizukuInstalled && !permissions.hasShizukuPermission)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "REQUIRED PERMISSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PermissionChip(
                        label = "Overlay",
                        granted = permissions.hasOverlay,
                        onClick = if (!permissions.hasOverlay) onRequestOverlay else null
                    )
                    PermissionChip(
                        label = "Usage",
                        granted = permissions.hasUsageStats,
                        onClick = if (!permissions.hasUsageStats) onRequestUsage else null
                    )
                    if (permissions.hasShizukuInstalled) {
                        PermissionChip(
                            label = "Shizuku",
                            granted = permissions.hasShizukuPermission,
                            onClick = if (!permissions.hasShizukuPermission) onRequestShizuku else null
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Style section: layout + scale
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StyleSection(
    scaleFactor: Float,
    isHorizontal: Boolean,
    onScaleChange: (Float) -> Unit,
    onScaleCommit: () -> Unit,
    onHorizontalChange: (Boolean) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    SectionCard(title = "Style & Layout", icon = Icons.Filled.ViewAgenda) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Layout toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(primary.copy(alpha = 0.14f))
                            .border(1.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Horizontal Layout",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Compact side-by-side view",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = isHorizontal,
                    onCheckedChange = onHorizontalChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primary,
                        checkedTrackColor = primary.copy(alpha = 0.4f),
                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Scale slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(secondary.copy(alpha = 0.14f))
                            .border(1.dp, secondary.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = secondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Overlay Scale",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(primary.copy(alpha = 0.14f))
                        .border(0.5.dp, primary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(scaleFactor)}x",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = scaleFactor,
                onValueChange = onScaleChange,
                onValueChangeFinished = onScaleCommit,
                valueRange = 0.5f..2.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    thumbColor = primary,
                    activeTrackColor = primary,
                    inactiveTrackColor = primary.copy(alpha = 0.15f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0.5x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "1.0x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "1.5x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "2.0x",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FPS Mode section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FpsModeSection(
    fpsMode: FpsMode,
    isShizukuReady: Boolean,
    isRootReady: Boolean,
    onSelect: (FpsMode) -> Unit,
    onRequestShizuku: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    val autoStatus = when {
        isShizukuReady -> "Shizuku"
        isRootReady -> "Root"
        else -> "Display"
    }

    SectionCard(title = "FPS Monitor Mode", icon = Icons.Filled.Bolt) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(primary.copy(alpha = 0.08f))
                    .border(0.5.dp, primary.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Active source: $autoStatus",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FpsModePill(
                    label = "AUTO",
                    icon = Icons.Filled.AutoAwesome,
                    selected = fpsMode == FpsMode.AUTO,
                    active = true,
                    description = autoStatus,
                    onClick = { onSelect(FpsMode.AUTO) }
                )
                FpsModePill(
                    label = "ROOT",
                    icon = Icons.Filled.AdminPanelSettings,
                    selected = fpsMode == FpsMode.ROOT,
                    active = isRootReady,
                    description = if (isRootReady) "Available" else "Not found",
                    onClick = { onSelect(FpsMode.ROOT) }
                )
                FpsModePill(
                    label = "SHIZUKU",
                    icon = Icons.Filled.Shield,
                    selected = fpsMode == FpsMode.SHIZUKU,
                    active = isShizukuReady,
                    description = if (isShizukuReady) "Authorized" else "Tap to grant",
                    onClick = {
                        onSelect(FpsMode.SHIZUKU)
                        if (!isShizukuReady) onRequestShizuku()
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Metrics section
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MetricsSection(
    metrics: List<OverlayMetricItem>,
    onToggle: (String, Boolean) -> Unit,
    onReorder: (Int, Int) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val enabledCount = metrics.count { it.enabled }
    val totalCount = metrics.size

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primary)
            )
            Text(
                text = "CUSTOMIZE METRICS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(primary.copy(alpha = 0.12f))
                    .border(0.5.dp, primary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$enabledCount / $totalCount",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = primary
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            val sortedMetrics = metrics.sortedBy { it.order }
            ReorderableList(
                items = sortedMetrics,
                onReorder = onReorder,
                modifier = Modifier.fillMaxWidth(),
                key = { it.id }
            ) { metric, isDragging ->
                OverlayMetricCard(
                    metric = metric,
                    isDragging = isDragging,
                    onToggle = { enabled -> onToggle(metric.id, enabled) }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permissions Setup Section with guided instructions & action cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PermissionsSetupSection(
    permissions: OverlayPermissions,
    onRequestOverlay: () -> Unit,
    onRequestUsage: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    SectionCard(title = "Required Permissions & Setup", icon = Icons.Filled.Shield) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!permissions.hasOverlay) {
                PermissionActionCard(
                    title = "Display Over Other Apps",
                    description = "Required to draw the floating live performance monitor HUD over your applications and games.",
                    instructions = listOf(
                        "1. Tap 'Grant Overlay Permission' below",
                        "2. Select 'DeviceInsights' from the system list",
                        "3. Turn ON 'Allow display over other apps'"
                    ),
                    icon = Icons.Filled.Layers,
                    isGranted = false,
                    isRequired = true,
                    actionText = "Grant Overlay Permission",
                    onActionClick = onRequestOverlay
                )
            }

            if (!permissions.hasUsageStats) {
                PermissionActionCard(
                    title = "Usage Access",
                    description = "Enables tracking the active foreground app name and per-app CPU/RAM resource stats.",
                    instructions = listOf(
                        "1. Tap 'Grant Usage Access' below",
                        "2. Select 'DeviceInsights'",
                        "3. Turn ON 'Permit usage access'"
                    ),
                    icon = Icons.Filled.Analytics,
                    isGranted = false,
                    isRequired = false,
                    actionText = "Grant Usage Access",
                    onActionClick = onRequestUsage
                )
            }

            if (permissions.hasShizukuInstalled && !permissions.hasShizukuPermission) {
                PermissionActionCard(
                    title = "Shizuku Authorization",
                    description = "Enables accurate real-time FPS monitoring on non-rooted devices.",
                    instructions = listOf(
                        "1. Tap 'Authorize Shizuku' below",
                        "2. Tap 'Allow' in the Shizuku authorization dialog"
                    ),
                    icon = Icons.Filled.Shield,
                    isGranted = false,
                    isRequired = false,
                    actionText = "Authorize Shizuku",
                    onActionClick = onRequestShizuku
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action buttons
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ActionButtons(
    isRunning: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestOverlay: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isRunning) {
            // Stop button (prominent when running)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP OVERLAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else if (canStart) {
            // Start button (prominent when ready)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.25f),
                                secondary.copy(alpha = 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.6f),
                                secondary.copy(alpha = 0.25f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START OVERLAY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Prominent Grant Permission Action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.3f),
                                secondary.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.7f),
                                secondary.copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onRequestOverlay() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRANT OVERLAY PERMISSION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable section card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primary)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                ),
                color = primary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.18f),
                            secondary.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            content()
        }
    }
}

