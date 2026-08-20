// P2-5 / P2-6: single Glance family 2×2 / 4×2 / 4×4, medium parameterized from
// DataStore (same "caliper" store as the app) — widget/HUD/app never disagree.
package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.appwidget.SizeMode
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ivarna.deviceinsight.ui.caliper.CarbonColors
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.PaperColors
import com.ivarna.deviceinsight.ui.caliper.BlueprintColors
import com.ivarna.deviceinsight.ui.caliper.Channels
import com.ivarna.deviceinsight.ui.caliper.CaliperColors
import com.ivarna.deviceinsight.ui.caliper.CaliperKeys
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

private const val CW_CPU = "cpu"
private const val CW_MEM = "mem"
private const val CW_PWR = "pwr"
private const val CW_NET = "net"

data class WidgetReading(
    val valueText: String,
    val subline: String,
    val updated: String,
    val channelName: String,
    val channelId: String,
    val barFraction: Float? = null
)

private suspend fun contextMedium(context: Context): Medium =
    context.mediumFlow.first() ?: Medium.PAPER

private fun mediumColors(medium: Medium): CaliperColors = when (medium) {
    Medium.PAPER -> PaperColors
    Medium.CARBON -> CarbonColors
    Medium.BLUEPRINT -> BlueprintColors
}

private fun timestamp(): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

private fun cpuReading(context: Context): WidgetReading {
    val util = com.ivarna.deviceinsight.utils.CpuUtilizationUtils(context)
    return WidgetReading(
        valueText = "${(util.getCpuUtilizationPercentage() * 100).toInt()}%",
        subline = "CH-01 · CPU",
        updated = timestamp(),
        channelName = "CPU",
        channelId = CW_CPU
    )
}

private fun memReading(context: Context): WidgetReading {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val m = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(m)
    val total = m.totalMem; val avail = m.availMem; val used = total - avail
    return WidgetReading(
        valueText = String.format(Locale.US, "%.1f / %.0f GB", used / 1e9, total / 1e9),
        subline = "CH-02 · MEMORY",
        updated = timestamp(),
        channelName = "MEMORY",
        channelId = CW_MEM,
        barFraction = (used.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    )
}

private fun pwrReading(context: Context): WidgetReading {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
    val level = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return WidgetReading(
        valueText = "$level%",
        subline = "CH-04 · POWER",
        updated = timestamp(),
        channelName = "POWER",
        channelId = CW_PWR,
        barFraction = level / 100f
    )
}

private suspend fun readings(context: Context): List<WidgetReading> =
    listOf(cpuReading(context), memReading(context), pwrReading(context))

// ─────────────────────────── 2×2 · SINGLE CHANNEL ───────────────────────────

class SingleChannelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val medium = contextMedium(context)
        val c = mediumColors(medium)
        val r = cpuReading(context)
        provideContent { Channel2x2(r, c, medium) }
    }
}

class SingleChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleChannelWidget()
}

// ─────────────────────────── 4×2 · DUAL ───────────────────────────

class DualChannelWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val medium = contextMedium(context)
        val c = mediumColors(medium)
        val rs = readings(context)
        provideContent { Channel4x2(rs, c, medium) }
    }
}

class DualChannelWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DualChannelWidget()
}

// ─────────────────────────── 4×4 · BENCH ───────────────────────────

class BenchWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val medium = contextMedium(context)
        val c = mediumColors(medium)
        val rs = readings(context)
        provideContent { Channel4x4(rs, c, medium) }
    }
}

class BenchWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BenchWidget()
}

// ─────────────────────────── content ───────────────────────────

@androidx.compose.runtime.Composable
private fun Channel2x2(r: WidgetReading, c: CaliperColors, medium: Medium) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(c.panel))
            .cornerRadius(0.dp)
            .padding(12.dp)
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text("CH-01 · CPU", style = TextStyle(
                color = ColorProvider(c.ink60), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.defaultWeight())
            Text("●", style = TextStyle(color = ColorProvider(c.channel(Channels.CPU)), fontSize = 9.sp))
        }
        Spacer(GlanceModifier.height(6.dp))
        Text(r.valueText, style = TextStyle(
            color = ColorProvider(c.ink), fontSize = 30.sp, fontWeight = FontWeight.Medium))
        Spacer(GlanceModifier.height(4.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            Text("CR", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text("upd ${r.updated}", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
        }
    }
}

@androidx.compose.runtime.Composable
private fun Channel4x2(rs: List<WidgetReading>, c: CaliperColors, medium: Medium) {
    Row(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(c.panel))
            .cornerRadius(0.dp)
            .padding(12.dp)
    ) {
        rs.getOrNull(0)?.let { r ->
            Column(GlanceModifier.defaultWeight()) {
                Text(r.subline, style = TextStyle(color = ColorProvider(c.ink60), fontSize = 10.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(4.dp))
                Text(r.valueText, style = TextStyle(color = ColorProvider(c.ink), fontSize = 22.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(4.dp))
                r.barFraction?.let { f -> Bar(f, c) }
                Spacer(GlanceModifier.height(2.dp))
                Text("upd ${r.updated}", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
            }
        }
        rs.getOrNull(1)?.let { r ->
            Column(GlanceModifier.defaultWeight().padding(start = 8.dp)) {
                Text(r.subline, style = TextStyle(color = ColorProvider(c.ink60), fontSize = 10.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(4.dp))
                Text(r.valueText, style = TextStyle(color = ColorProvider(c.ink), fontSize = 22.sp, fontWeight = FontWeight.Medium))
                Spacer(GlanceModifier.height(4.dp))
                r.barFraction?.let { f -> Bar(f, c) }
                Spacer(GlanceModifier.height(2.dp))
                Text("upd ${r.updated}", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Channel4x4(rs: List<WidgetReading>, c: CaliperColors, medium: Medium) {
    Column(
        GlanceModifier.fillMaxSize()
            .background(ColorProvider(c.panel))
            .cornerRadius(0.dp)
            .padding(12.dp)
    ) {
        rs.getOrNull(0)?.let { r ->
            Text("CH-01 · CPU", style = TextStyle(color = ColorProvider(c.ink60), fontSize = 11.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(2.dp))
            Text(r.valueText, style = TextStyle(color = ColorProvider(c.ink), fontSize = 30.sp, fontWeight = FontWeight.Medium))
            Spacer(GlanceModifier.height(6.dp))
        }
        Row(GlanceModifier.fillMaxWidth()) {
            rs.getOrNull(2)?.let { r ->
                Column(GlanceModifier.defaultWeight()) {
                    Text("CH-04 · PWR", style = TextStyle(color = ColorProvider(c.ink60), fontSize = 10.sp))
                    Text(r.valueText, style = TextStyle(color = ColorProvider(c.ink), fontSize = 20.sp, fontWeight = FontWeight.Medium))
                    r.barFraction?.let { f -> Bar(f, c) }
                }
            }
            rs.getOrNull(1)?.let { r ->
                Column(GlanceModifier.defaultWeight().padding(start = 8.dp)) {
                    Text("CH-02 · MEM", style = TextStyle(color = ColorProvider(c.ink60), fontSize = 10.sp))
                    Text(r.valueText, style = TextStyle(color = ColorProvider(c.ink), fontSize = 20.sp, fontWeight = FontWeight.Medium))
                    r.barFraction?.let { f -> Bar(f, c) }
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Row(GlanceModifier.fillMaxWidth()) {
            Text("── END OF SHEET ──", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text("upd ${rs.firstOrNull()?.updated ?: ""}", style = TextStyle(color = ColorProvider(c.ink40), fontSize = 9.sp))
        }
    }
}

@androidx.compose.runtime.Composable
private fun Bar(fraction: Float, c: CaliperColors) {
    // Pre-rendered hatched bar — deterministic, no canvas in Glance.
    Column {
        val bmp = rememberHatchBitmap(c, fraction)
        androidx.glance.Image(
            provider = ImageProvider(bmp),
            contentDescription = "bar ${(fraction * 100).toInt()} percent",
            modifier = GlanceModifier.fillMaxWidth().height(6.dp)
        )
    }
}

@androidx.compose.runtime.Composable
private fun rememberHatchBitmap(c: CaliperColors, fraction: Float): Bitmap {
    return androidx.compose.runtime.remember(c.medium, fraction) {
        val w = 96; val h = 6
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply { color = c.channel(Channels.CPU).toArgbInt(); style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, w * fraction, h.toFloat(), paint)
        val line = Paint().apply { color = c.ink.toArgbInt(); strokeWidth = 1f }
        var x = 2f
        while (x < w * fraction) {
            canvas.drawLine(x, 0f, x, h.toFloat(), line)
            x += 4f
        }
        bmp
    }
}

private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
    )