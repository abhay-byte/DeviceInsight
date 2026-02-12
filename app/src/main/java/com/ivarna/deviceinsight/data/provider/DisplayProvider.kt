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

class DisplayProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gpuMapper: GpuMapper
) {
    fun getScreenResolution(): String {
        val metrics = context.resources.displayMetrics
        return "${metrics.widthPixels} \u00D7 ${metrics.heightPixels}"
    }

    fun getDisplayTechnology(): String {
        // Most high-end Xiaomi/Poco devices use AMOLED
        val hardware = Build.HARDWARE.lowercase()
        return if (hardware.contains("mt6897") || hardware.contains("sm8")) "AMOLED" else "LCD"
    }

    fun getPhysicalSize(): String {
        val metrics = context.resources.displayMetrics
        val xdpi = if (metrics.xdpi > 1) metrics.xdpi else metrics.densityDpi.toFloat()
        val ydpi = if (metrics.ydpi > 1) metrics.ydpi else metrics.densityDpi.toFloat()
        val widthMm = (metrics.widthPixels / xdpi * 25.4).toInt()
        val heightMm = (metrics.heightPixels / ydpi * 25.4).toInt()
        return "$widthMm mm \u00D7 $heightMm mm"
    }

    fun getDiagonalSize(): String {
        val metrics = context.resources.displayMetrics
        val xdpi = if (metrics.xdpi > 1) metrics.xdpi else metrics.densityDpi.toFloat()
        val ydpi = if (metrics.ydpi > 1) metrics.ydpi else metrics.densityDpi.toFloat()
        val x = Math.pow(metrics.widthPixels.toDouble() / xdpi, 2.0)
        val y = Math.pow(metrics.heightPixels.toDouble() / ydpi, 2.0)
        val screenInches = sqrt(x + y)
        return String.format("%.2f inches", screenInches)
    }

    fun getDensityString(): String {
        val dpi = context.resources.displayMetrics.densityDpi
        val bucket = when {
            dpi >= 640 -> "xxxhdpi"
            dpi >= 480 -> "xxhdpi"
            dpi >= 320 -> "xhdpi"
            dpi >= 240 -> "hdpi"
            dpi >= 160 -> "mdpi"
            else -> "ldpi"
        }
        return "$dpi dpi ($bucket)"
    }

    fun getXDPI(): Float {
        val xdpi = context.resources.displayMetrics.xdpi
        return if (xdpi > 1) xdpi else context.resources.displayMetrics.densityDpi.toFloat()
    }

    fun getYDPI(): Float {
        val ydpi = context.resources.displayMetrics.ydpi
        return if (ydpi > 1) ydpi else context.resources.displayMetrics.densityDpi.toFloat()
    }

    fun getGpuInfo(): GpuMapper.GpuInfo {
        return gpuMapper.mapHardwareToGpuInfo(Build.HARDWARE)
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
