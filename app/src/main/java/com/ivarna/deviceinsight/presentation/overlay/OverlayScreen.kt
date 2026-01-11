package com.ivarna.deviceinsight.presentation.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.service.OverlayService

@Composable
fun OverlayScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var hasPermission by remember { mutableStateOf(false) }
    var showCpu by remember { mutableStateOf(true) }
    var showBattery by remember { mutableStateOf(true) }
    var showRam by remember { mutableStateOf(true) }
    var showSwap by remember { mutableStateOf(true) }
    var showCpuTemp by remember { mutableStateOf(true) }
    var showBatteryTemp by remember { mutableStateOf(true) }
    var showCpuGraph by remember { mutableStateOf(true) }
    var showPower by remember { mutableStateOf(true) }
    var showCpuFreq by remember { mutableStateOf(true) }
    var scaleFactor by remember { mutableStateOf(1.0f) }
    
    fun checkPermission() {
        hasPermission = Settings.canDrawOverlays(context)
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
            .padding(16.dp)
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
                   
                    // Parameter toggles
                    Text(
                        text = "Select Parameters to Display:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                   
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CPU Usage")
                            Switch(
                                checked = showCpu,
                                onCheckedChange = { showCpu = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Battery Level")
                            Switch(
                                checked = showBattery,
                                onCheckedChange = { showBattery = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RAM Usage")
                            Switch(
                                checked = showRam,
                                onCheckedChange = { showRam = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Swap Usage")
                            Switch(
                                checked = showSwap,
                                onCheckedChange = { showSwap = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CPU Temperature")
                            Switch(
                                checked = showCpuTemp,
                                onCheckedChange = { showCpuTemp = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Battery Temperature")
                            Switch(
                                checked = showBatteryTemp,
                                onCheckedChange = { showBatteryTemp = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CPU Usage Graph")
                            Switch(
                                checked = showCpuGraph,
                                onCheckedChange = { showCpuGraph = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Power Consumption")
                            Switch(
                                checked = showPower,
                                onCheckedChange = { showPower = it }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("CPU Core Frequencies")
                            Switch(
                                checked = showCpuFreq,
                                onCheckedChange = { showCpuFreq = it }
                            )
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
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )
                   
                    Spacer(modifier = Modifier.height(24.dp))
                   
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(context, OverlayService::class.java).apply {
                                    putExtra("showCpu", showCpu)
                                    putExtra("showBattery", showBattery)
                                    putExtra("showRam", showRam)
                                    putExtra("showSwap", showSwap)
                                    putExtra("showCpuTemp", showCpuTemp)
                                    putExtra("showBatteryTemp", showBatteryTemp)
                                    putExtra("showCpuGraph", showCpuGraph)
                                    putExtra("showPower", showPower)
                                    putExtra("showCpuFreq", showCpuFreq)
                                    putExtra("scaleFactor", scaleFactor)
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
    }
}
