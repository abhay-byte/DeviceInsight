package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.components.CoreReading
import java.io.File
import java.io.FileOutputStream

// DI-WF-001 §6: captures widget previews from the REAL Glance-to-RemoteViews pipeline at 3x
// density (480dpi) and fontScale 1.0, so the launcher picker preview is pixel-identical to the
// placed widget. Pure function object - safe in release; only PreviewStudioActivity is debug-gated.
object BenchPreviewGenerator {

    const val CAPTURE_DENSITY_DPI = 480 // density 3.0 -> tier dp x 3 px

    data class Shot(val kind: WidgetKind, val tier: Tier, val medium: Medium) {
        val wPx: Int get() = tier.wDp * (CAPTURE_DENSITY_DPI / 160)
        val hPx: Int get() = tier.hDp * (CAPTURE_DENSITY_DPI / 160)
        val fileName: String
            get() = "preview_${kind.name.lowercase()}_${medium.name.lowercase()}_${tier.wDp}x${tier.hDp}.png"
    }

    // QA matrix: SCOPE/STACK/FUEL/RASTER @ T2, BENCH @ T4 x {PAPER, CARBON, BLUEPRINT}.
    // Only paper variants ship as previewImage; the full matrix is kept for QA diffing.
    val SHOT_MATRIX: List<Shot> = buildList {
        for (medium in Medium.entries) {
            listOf(WidgetKind.SCOPE, WidgetKind.STACK, WidgetKind.FUEL, WidgetKind.RASTER).forEach { kind ->
                add(Shot(kind, Tier.T2, medium))
            }
            add(Shot(WidgetKind.BENCH, Tier.T4, medium))
        }
    }

    // Isolated capture context: fixed density + fontScale 1.0 regardless of device settings
    fun captureContext(context: Context): Context =
        context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply {
                densityDpi = CAPTURE_DENSITY_DPI
                fontScale = 1f
            }
        )

    suspend fun generateAll(context: Context, outDir: File): List<File> {
        val ctx = captureContext(context)
        outDir.mkdirs()
        val host = FrameLayout(ctx)
        val written = mutableListOf<File>()
        for (shot in SHOT_MATRIX) {
            try {
                written += generate(shot, ctx, host, outDir)
            } catch (e: Exception) {
                Log.e("BenchPreviewGen", "shot failed: $shot", e)
            }
        }
        File(outDir, "DROP_IN_MANIFEST.txt").writeText(
            buildString {
                appendLine("# BenchPreviewGenerator drop-in - copy PAPER variants to app/src/main/res/drawable-nodpi/")
                written.forEach { appendLine(it.name) }
            }
        )
        return written
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private suspend fun generate(shot: Shot, ctx: Context, host: FrameLayout, dir: File): File {
        // REAL pipeline: Glance composition -> RemoteViews -> inflated View - identical to what
        // GlanceAppWidget ships to the launcher (only size/state are synthetic here)
        val result = GlanceRemoteViews().compose(
            ctx,
            DpSize(shot.tier.wDp.dp, shot.tier.hDp.dp)
        ) {
            InstrumentBody(
                kind = shot.kind,
                tier = Tier.of(shot.tier.wDp, shot.tier.hDp),
                medium = shot.medium,
                cfg = BenchConfig(medium = shot.medium),
                snap = BenchDemo.previewSnapshot().copy(timestamp = System.currentTimeMillis()),
                calibrating = false,
                awId = -1
            )
        }
        host.removeAllViews()
        val view = result.remoteViews.apply(ctx, host)
        host.addView(view, FrameLayout.LayoutParams(shot.wPx, shot.hPx))
        host.measure(
            View.MeasureSpec.makeMeasureSpec(shot.wPx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(shot.hPx, View.MeasureSpec.EXACTLY)
        )
        host.layout(0, 0, shot.wPx, shot.hPx)
        val bmp = Bitmap.createBitmap(shot.wPx, shot.hPx, Bitmap.Config.ARGB_8888)
        host.draw(Canvas(bmp))
        val out = File(dir, shot.fileName)
        FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return out
    }
}

// Deterministic demo snapshot for previews - hash-stable across calls (regression-tested).
// Widget-domain sibling of BenchConfigActivity.benchDemoSnapshot (Compose config preview shape);
// intentionally separate: this one must be identical between generator runs.
object BenchDemo {

    fun previewSnapshot(): BenchSnapshot = BenchSnapshot(
        timestamp = 1_700_000_000_000L,
        cpuPct = 38.4f,
        cpuHist = HIST,
        freqGHz = 2.41f,
        tempC = 46f,
        governor = "schedutil",
        cores = List(8) { c ->
            CoreReading(id = c, load = CORE_LOAD[c].toFloat(), freqKhz = CORE_FREQ_KHZ[c])
        },
        memUsedGb = 6.8f,
        memTotalGb = 12f,
        memComposition = listOf(
            MemSeg(fraction = 0.55f, pattern = HatchPattern.SOLID, channelId = "CH-02"),
            MemSeg(fraction = 0.13f, pattern = HatchPattern.CROSS, channelId = "CH-04"),
            MemSeg(fraction = 0.32f, pattern = HatchPattern.NONE, channelId = "")
        ),
        memHist = HIST.map { (it + 22f).coerceAtMost(98f) },
        zramGb = 0.4f,
        swapGb = 0.8f,
        topConsumers = listOf(
            Consumer(pkg = "com.android.systemui", label = "System UI", rssMb = 412),
            Consumer(pkg = "com.ivarna.deviceinsight", label = "DeviceInsight", rssMb = 96),
            Consumer(pkg = "com.android.launcher", label = "Launcher", rssMb = 88)
        ),
        netDown = 1_240_000L,
        netUp = 96_000L,
        netHist = HIST.map { (it * 0.7f).coerceAtMost(98f) },
        batteryPct = 0.72f,
        watts = -1.24f,
        voltage = 3.98f,
        currentMa = -312,
        remainingMin = 214,
        charging = false,
        wattHist = HIST.map { ((it - 40f) / 10f).coerceIn(-3.2f, 3.2f) },
        stoUsedGb = 84.2f,
        stoTotalGb = 128f,
        gpuPct = 27f,
        gpuMHz = 585L,
        gpuHist = HIST.map { (it * 0.5f).coerceAtMost(90f) },
        gpuName = "Adreno (TM) 618",
        gpuVulkan = "vulkan 1.1.128",
        gpuGles = "OpenGL ES 3.2",
        gpuFitted = true,
        serviceRunning = true,
        batteryPresent = true,
        batteryHealth = "Good",
        cycleCount = 218,
        designMah = 4000
    )

    private val HIST: List<Float> = List(60) { i ->
        (34f + 26f * sinOf(i * 0.7f) + 12f * cosOf(i * 1.9f)).coerceIn(2f, 98f)
    }
    private val CORE_LOAD = intArrayOf(12, 34, 56, 78, 23, 45, 67, 89)
    private val CORE_FREQ_KHZ = longArrayOf(
        1_800_000L, 1_800_000L, 2_200_000L, 2_200_000L,
        2_400_000L, 2_400_000L, 2_400_000L, 1_900_000L
    )

    private fun sinOf(x: Float): Float = kotlin.math.sin(x)
    private fun cosOf(x: Float): Float = kotlin.math.cos(x)
}
