package com.ivarna.deviceinsight.presentation.hardware

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.hardware.components.*

private data class HardwareTab(
    val label: String,
    val icon: ImageVector
)

private val hardwareTabs = listOf(
    HardwareTab("System",     Icons.Filled.PhoneAndroid),
    HardwareTab("CPU",        Icons.Filled.Memory),
    HardwareTab("Display",    Icons.Filled.Monitor),
    HardwareTab("GPU",        Icons.Filled.DeveloperBoard),
    HardwareTab("Network",    Icons.Filled.Wifi),
    HardwareTab("Battery",    Icons.Filled.Battery5Bar),
    HardwareTab("Android",    Icons.Filled.Android),
    HardwareTab("Hardware",   Icons.Filled.DataObject),
    HardwareTab("Thermal",    Icons.Filled.DeviceThermostat),
    HardwareTab("Dirs",       Icons.Filled.Folder),
    HardwareTab("Sensors",    Icons.Filled.Sensors),
)

@Composable
fun HardwareScreen(
    viewModel: HardwareViewModel = hiltViewModel()
) {
    val hardwareInfo by viewModel.hardwareInfo.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTabIndex) {
        listState.animateScrollToItem(
            index = maxOf(0, selectedTabIndex - 1)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Premium pill-style horizontal tab bar ──────────────────────────
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(hardwareTabs) { index, tab ->
                PillTab(
                    label = tab.label,
                    icon = tab.icon,
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index }
                )
            }
        }

        // ── Tab content ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (hardwareInfo == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading hardware info…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    hardwareInfo?.let { info ->
                        when (selectedTabIndex) {
                            0  -> SystemTab(info)
                            1  -> CpuTab(info)
                            2  -> DisplayTab(info)
                            3  -> GpuTab(info)
                            4  -> NetworkTab(info)
                            5  -> BatteryTab(info)
                            6  -> AndroidTab(info)
                            7  -> DevicesTab(info)
                            8  -> ThermalTab(info)
                            9  -> DirectoriesTab(info)
                            10 -> SensorsTab(info)
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun PillTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    val bgAlpha by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = 0.15f)
                      else MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
        animationSpec = tween(250),
        label = "pillBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) primary
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        animationSpec = tween(250),
        label = "pillContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = 0.4f)
                      else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
        animationSpec = tween(250),
        label = "pillBorder"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 0.dp else 0.dp,
        animationSpec = tween(250),
        label = "pillElevation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (selected)
                    Brush.linearGradient(
                        listOf(primary.copy(alpha = 0.18f), secondary.copy(alpha = 0.08f))
                    )
                else
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
                        )
                    )
            )
            .border(
                width = if (selected) 1.dp else 0.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(17.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = if (selected) 0.5.sp else 0.sp,
                fontSize = 11.sp
            ),
            color = contentColor
        )
    }
}
