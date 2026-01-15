package com.ivarna.deviceinsight.presentation.widgets

import android.content.Context
import android.app.ActivityManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ivarna.deviceinsight.R
import androidx.glance.text.FontWeight
import androidx.glance.ImageProvider
import com.ivarna.deviceinsight.presentation.widgets.theme.WidgetTheme
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils

class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cpuUsage = CpuUtilizationUtils(context).getCpuUtilizationPercentage() * 100
        val batteryLevel = getBatteryLevel(context)
        val memoryInfo = getMemoryInfo(context)

        provideContent {
            WidgetTheme.WidgetTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.rounded_widget_background))
                        .padding(16.dp),
                    verticalAlignment = Alignment.Vertical.Top
                ) {
                    Text(
                        text = "System Monitor",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // CPU Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Horizontal.Start
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "CPU",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFAAAAAA)),
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = "${cpuUsage.toInt()}%",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF00E5FF)),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "Battery",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFAAAAAA)),
                                    fontSize = 10.sp
                                )
                            )
                            Text(
                                text = "$batteryLevel%",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFD500F9)),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // Memory Row
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Memory",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFAAAAAA)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "${memoryInfo.first} / ${memoryInfo.second} MB",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFD600)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    // Swap Row
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Text(
                            text = "Swap",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFAAAAAA)),
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = "${getSwapUsedBytes() / (1024 * 1024)} / ${getSwapTotalBytes() / (1024 * 1024)} MB",
                            style = TextStyle(
                                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFFD600)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getMemoryInfo(context: Context): Pair<Int, Int> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
         
        val totalMB = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availMB = (memInfo.availMem / (1024 * 1024)).toInt()
        val usedMB = totalMB - availMB
         
        return Pair(usedMB, totalMB)
    }

    private fun getSwapUsedBytes(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotal: Long = 0
            var swapFree: Long = 0
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("SwapTotal:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapTotal = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                } else if (line?.startsWith("SwapFree:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapFree = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                }
            }
            reader.close()
            swapTotal - swapFree
        } catch (e: Exception) {
            0L
        }
    }

    private fun getSwapTotalBytes(): Long {
        return try {
            val reader = RandomAccessFile("/proc/meminfo", "r")
            var line: String?
            var swapTotal: Long = 0
            while (reader.readLine().also { line = it } != null) {
                if (line?.startsWith("SwapTotal:") == true) {
                    val parts = line?.split("\\s+".toRegex())
                    if (parts?.size!! > 1) {
                        swapTotal = parts[1].toLong() * 1024 // Convert from KB to bytes
                    }
                    break
                }
            }
            reader.close()
            swapTotal
        } catch (e: Exception) {
            0L
        }
    }
}
