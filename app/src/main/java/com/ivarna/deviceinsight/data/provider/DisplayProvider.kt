package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.ivarna.deviceinsight.data.mapper.GpuMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.sqrt

@javax.inject.Singleton
class DisplayProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gpuMapper: GpuMapper
) {
    @Volatile private var cachedResolution: String? = null
    @Volatile private var cachedTechnology: String? = null
    @Volatile private var cachedPhysicalSize: String? = null
    @Volatile private var cachedDiagonalSize: String? = null
    @Volatile private var cachedDensityString: String? = null
    @Volatile private var cachedXDpi: Float? = null
    @Volatile private var cachedYDpi: Float? = null
    @Volatile private var cachedGpuInfo: GpuMapper.GpuInfo? = null

    fun getScreenResolution(): String {
        cachedResolution?.let { return it }
        val metrics = context.resources.displayMetrics
        val res = "${metrics.widthPixels} \u00D7 ${metrics.heightPixels}"
        cachedResolution = res
        return res
    }

    fun getDisplayTechnology(): String {
        cachedTechnology?.let { return it }
        val hardware = Build.HARDWARE.lowercase()
        val tech = if (hardware.contains("mt6897") || hardware.contains("sm8")) "AMOLED" else "LCD"
        cachedTechnology = tech
        return tech
    }

    fun getPhysicalSize(): String {
        cachedPhysicalSize?.let { return it }
        val metrics = context.resources.displayMetrics
        val xdpi = if (metrics.xdpi > 1) metrics.xdpi else metrics.densityDpi.toFloat()
        val ydpi = if (metrics.ydpi > 1) metrics.ydpi else metrics.densityDpi.toFloat()
        val widthMm = (metrics.widthPixels / xdpi * 25.4).toInt()
        val heightMm = (metrics.heightPixels / ydpi * 25.4).toInt()
        val size = "$widthMm mm \u00D7 $heightMm mm"
        cachedPhysicalSize = size
        return size
    }

    fun getDiagonalSize(): String {
        cachedDiagonalSize?.let { return it }
        val metrics = context.resources.displayMetrics
        val xdpi = if (metrics.xdpi > 1) metrics.xdpi else metrics.densityDpi.toFloat()
        val ydpi = if (metrics.ydpi > 1) metrics.ydpi else metrics.densityDpi.toFloat()
        val x = Math.pow(metrics.widthPixels.toDouble() / xdpi, 2.0)
        val y = Math.pow(metrics.heightPixels.toDouble() / ydpi, 2.0)
        val screenInches = sqrt(x + y)
        val diag = String.format("%.2f inches", screenInches)
        cachedDiagonalSize = diag
        return diag
    }

    fun getDensityString(): String {
        cachedDensityString?.let { return it }
        val dpi = context.resources.displayMetrics.densityDpi
        val bucket = when {
            dpi >= 640 -> "xxxhdpi"
            dpi >= 480 -> "xxhdpi"
            dpi >= 320 -> "xhdpi"
            dpi >= 240 -> "hdpi"
            dpi >= 160 -> "mdpi"
            else -> "ldpi"
        }
        val str = "$dpi dpi ($bucket)"
        cachedDensityString = str
        return str
    }

    fun getXDPI(): Float {
        cachedXDpi?.let { return it }
        val metrics = context.resources.displayMetrics
        val xdpi = if (metrics.xdpi > 1) metrics.xdpi else metrics.densityDpi.toFloat()
        cachedXDpi = xdpi
        return xdpi
    }

    fun getYDPI(): Float {
        cachedYDpi?.let { return it }
        val metrics = context.resources.displayMetrics
        val ydpi = if (metrics.ydpi > 1) metrics.ydpi else metrics.densityDpi.toFloat()
        cachedYDpi = ydpi
        return ydpi
    }

    fun getGpuInfo(): GpuMapper.GpuInfo {
        cachedGpuInfo?.let { return it }
        val info = gpuMapper.mapHardwareToGpuInfo(Build.HARDWARE)
        cachedGpuInfo = info
        return info
    }

    fun getRefreshRate(): Float {
        return try {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            display?.refreshRate ?: 60f
        } catch (e: Exception) {
            60f
        }
    }

    fun getDefaultOrientation(): String {
        val config = context.resources.configuration
        return if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) "Landscape" else "Portrait"
    }
}
