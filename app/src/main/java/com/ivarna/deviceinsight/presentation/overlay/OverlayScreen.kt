package com.ivarna.deviceinsight.presentation.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import rikka.shizuku.Shizuku
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.components.ReorderableList
import com.ivarna.deviceinsight.service.OverlayService

data class OverlayMetric(
    val id: String,
    val name: String,
    var enabled: Boolean,
    var order: Int
)

@Composable
fun OverlayScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE) }
    
    var hasPermission by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }
    var hasShizukuPermission by remember { mutableStateOf(false) }
    var isShizukuAvailable by remember { mutableStateOf(false) }
    var isRootAvailable by remember { mutableStateOf(false) }
    var fpsMode by remember { mutableStateOf(prefs.getString("fps_mode", "AUTO") ?: "AUTO") }
    var scaleFactor by remember { mutableStateOf(prefs.getFloat("scaleFactor", 1.0f)) }
    var isHorizontal by remember { mutableStateOf(prefs.getBoolean("isHorizontal", false)) }
    
    // Load metric order from preferences
    val defaultOrder = listOf("time", "cpu", "power", "battery", "ram", "swap", "cpuTemp", "batteryTemp", "cpuGraph", "powerGraph", "fps", "fpsGraph", "cpuFreq", "network", "currentApp")
    val savedOrderStr = prefs.getString("metricOrder", null)
    val savedOrder = savedOrderStr?.split(",") ?: defaultOrder
    
    // Ensure "network" and other new metrics are added if missing from saved prefs
    val finalOrder = savedOrder.toMutableList()
    defaultOrder.forEach { 
        if (!finalOrder.contains(it)) finalOrder.add(it) 
    }
    
    var metrics by remember {
        mutableStateOf(
            finalOrder.mapIndexed { index, id ->
                when (id) {
                    "time" -> OverlayMetric("time", "System Time", prefs.getBoolean("showTime", true), index)
                    "cpu" -> OverlayMetric("cpu", "CPU Usage", prefs.getBoolean("showCpu", true), index)
                    "power" -> OverlayMetric("power", "Power Consumption", prefs.getBoolean("showPower", true), index)
                    "battery" -> OverlayMetric("battery", "Battery Level", prefs.getBoolean("showBattery", true), index)
                    "ram" -> OverlayMetric("ram", "RAM Usage", prefs.getBoolean("showRam", true), index)
                    "swap" -> OverlayMetric("swap", "Swap Usage", prefs.getBoolean("showSwap", true), index)
                    "cpuTemp" -> OverlayMetric("cpuTemp", "CPU Temperature", prefs.getBoolean("showCpuTemp", true), index)
                    "batteryTemp" -> OverlayMetric("batteryTemp", "Battery Temperature", prefs.getBoolean("showBatteryTemp", true), index)
                    "cpuGraph" -> OverlayMetric("cpuGraph", "CPU Usage Graph", prefs.getBoolean("showCpuGraph", true), index)
                    "powerGraph" -> OverlayMetric("powerGraph", "Power Usage Graph", prefs.getBoolean("showPowerGraph", true), index)
                    "fps" -> OverlayMetric("fps", "FPS Monitor", prefs.getBoolean("showFps", true), index)
                    "fpsGraph" -> OverlayMetric("fpsGraph", "FPS History Graph", prefs.getBoolean("showFpsGraph", true), index)
                    "cpuFreq" -> OverlayMetric("cpuFreq", "CPU Core Frequencies", prefs.getBoolean("showCpuFreq", true), index)
                    "network" -> OverlayMetric("network", "Network Speed", prefs.getBoolean("showNetwork", true), index)
                    "currentApp" -> OverlayMetric("currentApp", "Current App", prefs.getBoolean("showCurrentApp", true), index)
                    else -> OverlayMetric(id, id, true, index)
                }
            }
        )
    }
    
    var draggedItem by remember { mutableStateOf<OverlayMetric?>(null) }
    var draggedOverItem by remember { mutableStateOf<OverlayMetric?>(null) }
    
    // Save preferences whenever they change
    fun savePreferences() {
        prefs.edit().apply {
            metrics.forEach { metric ->
                putBoolean("show${metric.id.capitalize()}", metric.enabled)
            }
            putFloat("scaleFactor", scaleFactor)
            putBoolean("isHorizontal", isHorizontal)
            putString("fps_mode", fpsMode)
            putString("metricOrder", metrics.sortedBy { it.order }.joinToString(",") { it.id })
            apply()
        }
    }
    
    fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    
    fun checkPermission() {
        hasPermission = Settings.canDrawOverlays(context)
        hasUsageStats = hasUsageStatsPermission(context)
        try {
            isShizukuAvailable = Shizuku.pingBinder()
            if (isShizukuAvailable) {
                 hasShizukuPermission = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            isShizukuAvailable = false
        }
        
        // Check Root Access
        Thread {
            isRootAvailable = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
                process.waitFor() == 0
            } catch (e: Exception) { false }
        }.start()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkPermission()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (!hasPermission) {
            GlassCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Overlay Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To monitor performance while using other apps, please grant 'Display over other apps' permission.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
        } else {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FPS Monitor Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val statusText = if (fpsMode == "SHIZUKU") {
                         if (isShizukuAvailable && hasShizukuPermission) "Active: Shizuku" else "Inactive (Shizuku not ready)"
                    } else if (fpsMode == "ROOT") {
                         if (isRootAvailable) "Active: Root" else "Inactive (Root not found)"
                    } else {
                         if (isShizukuAvailable && hasShizukuPermission) "Active: Shizuku (Auto)"
                         else if (isRootAvailable) "Active: Root (Auto)"
                         else "Fallback: Display Refresh Rate"
                    }
                    
                    Text(text = statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(), 
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                         listOf("AUTO", "ROOT", "SHIZUKU").forEach { mode ->
                             FilterChip(
                                 selected = fpsMode == mode,
                                 onClick = { 
                                     fpsMode = mode
                                     savePreferences()
                                     // Optional: Request permissions immediately if forced
                                     if (mode == "SHIZUKU" && isShizukuAvailable && !hasShizukuPermission) {
                                         try { Shizuku.requestPermission(0) } catch(e: Exception){}
                                     }
                                 },
                                 label = { Text(mode) }
                             )
                         }
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Performance Overlay",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Horizontal Layout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Display metrics side-by-side",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isHorizontal,
                            onCheckedChange = {
                                isHorizontal = it
                                savePreferences()
                            }
                        )
                    }

                    Text(
                        text = "The overlay displays real-time CPU and system stats floating over other applications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "The overlay will display CPU %, battery, RAM, and swap information.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                   
                    // Parameter toggles with drag and drop - Premium glassmorphism design
                    Text(
                        text = "Customize Overlay Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Long press and drag to reorder • Toggle to show/hide",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                   
                    // Reorderable list with haptic feedback
                    val haptic = LocalHapticFeedback.current
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Reorderable list of metrics
                        val currentMetrics = metrics.sortedBy { it.order }
                        
                        ReorderableList(
                            items = currentMetrics,
                            onReorder = { fromIdx, toIdx ->
                                val newList = currentMetrics.toMutableList()
                                val item = newList.removeAt(fromIdx)
                                newList.add(toIdx, item)
                                metrics = newList.mapIndexed { i, m -> m.copy(order = i) }
                                savePreferences()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            key = { it.id }
                        ) { metric, isDragging ->
                            PremiumMetricCard(
                                metric = metric,
                                isDragging = isDragging,
                                onToggle = { enabled ->
                                    if (metric.id == "currentApp" && enabled && !hasUsageStatsPermission(context)) {
                                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                        context.startActivity(intent)
                                        android.widget.Toast.makeText(context, "Please grant Usage Access", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                    if (metric.id == "fps" && enabled) {
                                         // Check Shizuku status for user info
                                         try {
                                             if (rikka.shizuku.Shizuku.pingBinder()) {
                                                 if (rikka.shizuku.Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                                     rikka.shizuku.Shizuku.requestPermission(0)
                                                 } else {
                                                     android.widget.Toast.makeText(context, "Using Shizuku for accurate FPS", android.widget.Toast.LENGTH_SHORT).show()
                                                 }
                                             } else {
                                                 android.widget.Toast.makeText(context, "Shizuku not running. Showing screen refresh rate.", android.widget.Toast.LENGTH_SHORT).show()
                                             }
                                         } catch (e: Exception) {
                                             // Shizuku not installed or other error
                                             android.widget.Toast.makeText(context, "Install Shizuku for accurate FPS (currently showing refresh rate)", android.widget.Toast.LENGTH_SHORT).show()
                                         }
                                    }
                                    metrics = metrics.map {
                                        if (it.id == metric.id) it.copy(enabled = enabled) else it
                                    }
                                    savePreferences()
                                }
                            )
                        }
                    }
                    
                    if (!hasUsageStats) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Usage Access for App Detection")
                        }
                    }

                    if (isShizukuAvailable && !hasShizukuPermission) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    Shizuku.requestPermission(0)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Shizuku Permission for Accurate FPS")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                   
                    // Scale factor control
                    Text(
                        text = "Overlay Scale: ${String.format("%.1f", scaleFactor)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = scaleFactor,
                        onValueChange = { scaleFactor = it },
                        onValueChangeFinished = { savePreferences() },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                   
                    Spacer(modifier = Modifier.height(24.dp))
                   
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                savePreferences()
                                val intent = Intent(context, OverlayService::class.java).apply {
                                    // Specifically pass FPS explicitly because the loop might miss it if id != showX pattern
                                    putExtra("showFps", metrics.find { it.id == "fps" }?.enabled ?: true)
                                    putExtra("showFpsGraph", metrics.find { it.id == "fpsGraph" }?.enabled ?: true)
                                    
                                    metrics.forEach { metric ->
                                        // Handle special case where id matches param name
                                        val paramName = "show" + metric.id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                        putExtra(paramName, metric.enabled)
                                    }
                                    putExtra("scaleFactor", scaleFactor)
                                    putExtra("isHorizontal", isHorizontal)
                                    putExtra("metricOrder", metrics.sortedBy { it.order }.joinToString(",") { it.id })
                                }
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            }
                        ) {
                            Text("Start Overlay")
                        }
                         
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(context, OverlayService::class.java)
                                context.stopService(intent)
                            }
                        ) {
                            Text("Stop Overlay")
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(110.dp))
    }
}
@Composable
fun PremiumMetricCard(
    metric: OverlayMetric,
    isDragging: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 2.dp,
        label = "elevation"
    )
    
    // Additional alpha for the content background to make it look "lifted"
    val containerAlpha = if (isDragging) 0.9f else 0.6f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Add spacing between items
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isDragging) 1f else 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = metric.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = metric.enabled,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}