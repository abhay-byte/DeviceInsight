package com.ivarna.deviceinsight.presentation.widgets

import android.content.Context
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
import java.io.RandomAccessFile

class CpuWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cpuUsage = getCpuUsage()

        provideContent {
            WidgetTheme.WidgetTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(ImageProvider(R.drawable.rounded_widget_background))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "CPU",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "${cpuUsage.toInt()}%",
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF00E5FF)),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    private fun getCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" +".toRegex())
            val idle = toks[4].toLong()
            val cpu = toks.slice(1..7).sumOf { it.toLong() }
            val total = idle + cpu
            
            // Simple approximation - in production you'd track delta
            ((cpu.toFloat() / total.toFloat()) * 100).coerceIn(0f, 100f)
        } catch (e: Exception) {
            0f
        }
    }
}
