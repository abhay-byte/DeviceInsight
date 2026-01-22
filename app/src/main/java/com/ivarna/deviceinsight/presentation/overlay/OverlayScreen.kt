package com.ivarna.deviceinsight.presentation.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import rikka.shizuku.Shizuku
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.components.ReorderableList
import com.ivarna.deviceinsight.service.OverlayService

@Composable
fun OverlayScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE) }
    
    var hasPermission by remember { mutableStateOf(false) }
    var hasUsageStats by remember { mutableStateOf(false) }
    var hasShizuku by remember { mutableStateOf(false) }
    var isShizukuReady by remember { mutableStateOf(false) }
    var isRootReady by remember { mutableStateOf(false) }
    
    // Preferences
    var fpsMode by remember { mutableStateOf(prefs.getString("fps_mode", "AUTO") ?: "AUTO") }
    var scaleFactor by remember { mutableStateOf(prefs.getFloat("scaleFactor", 1.0f)) }
    var isHorizontal by remember { mutableStateOf(prefs.getBoolean("isHorizontal", false)) }
    
    // Logic to check permissions
    fun checkPermission() {
        hasPermission = Settings.canDrawOverlays(context)
        hasUsageStats = hasUsageStatsPermission(context)
        try {
            isShizukuReady = Shizuku.pingBinder()
            if (isShizukuReady) {
                 hasShizuku = Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            isShizukuReady = false
        }
        
        Thread {
            isRootReady = try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
                process.waitFor() == 0
            } catch (e: Exception) { false }
        }.start()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) checkPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        checkPermission()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Metrics Logic
    val defaultOrder = listOf("time", "cpu", "power", "battery", "ram", "swap", "cpuTemp", "batteryTemp", "cpuGraph", "powerGraph", "fps", "fpsGraph", "cpuFreq", "network", "currentApp")
    val savedOrderStr = prefs.getString("metricOrder", null)
    val savedOrder = savedOrderStr?.split(",") ?: defaultOrder
    val finalOrder = savedOrder.toMutableList().apply { 
        defaultOrder.forEach { if (!contains(it)) add(it) } 
    }
    
    var metrics by remember {
        mutableStateOf(finalOrder.mapIndexed { index, id ->
            val name = when(id) {
                "time" -> "System Time"
                "cpu" -> "CPU Usage"
                "power" -> "Power Consumption"
                "battery" -> "Battery Level"
                "ram" -> "RAM Usage"
                "swap" -> "Swap Usage"
                "cpuTemp" -> "CPU Temperature"
                "batteryTemp" -> "Battery Temperature"
                "cpuGraph" -> "CPU Usage Graph"
                "powerGraph" -> "Power Usage Graph"
                "fps" -> "FPS Monitor"
                "fpsGraph" -> "FPS History Graph"
                "cpuFreq" -> "CPU Core Frequencies"
                "network" -> "Network Speed"
                "currentApp" -> "Current App"
                else -> id
            }
            // Capitalize property name for preference key: showTime, showCpu...
            val prefKey = "show" + id.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            OverlayMetric(id, name, prefs.getBoolean(prefKey, true), index)
        })
    }
    
    fun savePreferences() {
        prefs.edit().apply {
            metrics.forEach { 
                val prefKey = "show" + it.id.replaceFirstChar { c -> c.titlecase() }
                putBoolean(prefKey, it.enabled) 
            }
            putFloat("scaleFactor", scaleFactor)
            putBoolean("isHorizontal", isHorizontal)
            putString("fps_mode", fpsMode)
            putString("metricOrder", metrics.sortedBy { it.order }.joinToString(",") { it.id })
            apply()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasPermission) {
            PermissionWarningCard(context)
        } else {
            // Main Controls
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("General Settings")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Horizontal Layout", style = MaterialTheme.typography.titleMedium)
                            Text("Compact side-by-side view", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isHorizontal, onCheckedChange = { isHorizontal = it; savePreferences() })
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Scale: ${String.format("%.1f", scaleFactor)}x", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = scaleFactor,
                        onValueChange = { scaleFactor = it },
                        onValueChangeFinished = { savePreferences() },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                }
            }
            
            // FPS Mode
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("FPS Monitor Mode")
                    
                    val statusText = when {
                        fpsMode == "SHIZUKU" -> if (isShizukuReady && hasShizuku) "Active: Shizuku" else "Inactive (Shizuku not ready)"
                        fpsMode == "ROOT" -> if (isRootReady) "Active: Root" else "Inactive (Root not found)"
                        else -> if (isShizukuReady && hasShizuku) "Active: Shizuku (Auto)" else if (isRootReady) "Active: Root (Auto)" else "Fallback: Display Refresh Rate"
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(bottom=8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO", "ROOT", "SHIZUKU").forEach { mode ->
                            FilterChip(
                                selected = fpsMode == mode,
                                onClick = { 
                                    fpsMode = mode
                                    savePreferences() 
                                    if (mode == "SHIZUKU" && isShizukuReady && !hasShizuku) {
                                         try { Shizuku.requestPermission(0) } catch(e: Exception){} 
                                    }
                                },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            }

            // Metrics Reordering
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                 Column(modifier = Modifier.padding(16.dp)) {
                     SectionTitle("Customize Metrics")
                     Text("Drag to reorder • Toggle to show/hide", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                     Spacer(modifier = Modifier.height(12.dp))
                     
                     val haptic = LocalHapticFeedback.current
                     val currentMetrics = metrics.sortedBy { it.order }
                     
                     ReorderableList(
                        items = currentMetrics,
                        onReorder = { from, to ->
                            val newList = currentMetrics.toMutableList()
                            val item = newList.removeAt(from)
                            newList.add(to, item)
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
                                // Permission checks
                                if (metric.id == "currentApp" && enabled && !hasUsageStats) {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                }
                                if (metric.id == "fps" && enabled && !hasShizuku && isShizukuReady) {
                                    try { Shizuku.requestPermission(0) } catch(e: Exception){}
                                }
                                
                                metrics = metrics.map { if (it.id == metric.id) it.copy(enabled = enabled) else it }
                                savePreferences()
                            }
                        )
                    }
                 }
            }
            
            // Permissions Help
            if (!hasUsageStats || (isShizukuReady && !hasShizuku)) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SectionTitle("Additional Permissions")
                        if (!hasUsageStats) {
                            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Grant Usage Access (for App Detection)")
                            }
                        }
                        if (isShizukuReady && !hasShizuku) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = { try { Shizuku.requestPermission(0) } catch(e: Exception){} }, modifier = Modifier.fillMaxWidth()) {
                                Text("Grant Shizuku (for Accurate FPS)")
                            }
                        }
                    }
                }
            }
            
            // Start/Stop Service
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        savePreferences()
                        val intent = Intent(context, OverlayService::class.java).apply {
                            metrics.forEach { 
                                val prefKey = "show" + it.id.replaceFirstChar { c -> c.titlecase() }
                                putExtra(prefKey, it.enabled)
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
                    }, 
                    modifier = Modifier.weight(1f)
                ) { Text("Start Overlay") }
                
                OutlinedButton(
                    onClick = { context.stopService(Intent(context, OverlayService::class.java)) },
                    modifier = Modifier.weight(1f)
                ) { Text("Stop") }
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun PermissionWarningCard(context: Context) {
    GlassCard {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Overlay Permission Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("To monitor performance over other apps, please grant permission.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }) { Text("Grant Permission") }
        }
    }
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
    val mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
    return mode == android.app.AppOpsManager.MODE_ALLOWED
}