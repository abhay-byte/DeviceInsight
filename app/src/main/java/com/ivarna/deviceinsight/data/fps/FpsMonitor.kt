package com.ivarna.deviceinsight.data.fps

import android.util.Log
import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.privilege.ShizukuAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class FpsSample(val fps: Int, val source: String) // "SF" | "GFX" | "—"

@Singleton
class FpsMonitor @Inject constructor(
    private val fpsRepository: FpsRepository,
    private val shizukuAccess: ShizukuAccess
) {
    companion object {
        private const val TAG = "FpsMonitor"
    }

    /**
     * Compatibility facade over [FpsRepository].
     * Runs repository on IO and maps typed snapshot to legacy [FpsSample].
     * Source: SF if SurfaceFlinger, GFX if gfxinfo, else "—".
     */
    suspend fun getCurrentFpsWithSource(): FpsSample = withContext(Dispatchers.IO) {
        try {
            val snap = fpsRepository.getFps()
            if (snap.currentFps <= 0f || snap.method == FpsMethod.NONE) {
                FpsSample(0, "—")
            } else {
                val src = when (snap.method) {
                    FpsMethod.SURFACEFLINGER -> "SF"
                    FpsMethod.GFXINFO -> "GFX"
                    FpsMethod.DMA_FENCE -> "DMA"
                    FpsMethod.DISPLAY -> "REF"
                    FpsMethod.NONE -> "—"
                }
                Log.d(TAG, "FPS ${snap.currentFps.toInt()} $src pkg=${snap.packageName} access=${snap.access} isStale=${snap.isStale}")
                FpsSample(snap.currentFps.toInt().coerceIn(1, 240), src)
            }
        } catch (e: Exception) {
            Log.w(TAG, "getFps failed", e)
            FpsSample(0, "—")
        }
    }

    // Blocking wrapper for callers that are not yet suspend (DashboardRepository legacy)
    fun getCurrentFps(): Int {
        // Called from DashboardRepository collectMetrics which is suspend, but also used elsewhere sync
        // Use runBlocking with timeout? Keep simple blocking via kotlin coroutines runBlocking
        return try {
            kotlinx.coroutines.runBlocking { getCurrentFpsWithSource().fps }
        } catch (_: Exception) { 0 }
    }

    // Also provide suspend version for direct use
    suspend fun getCurrentFpsSuspend(): Int = getCurrentFpsWithSource().fps

    fun isShizukuPermissionGranted(): Boolean = shizukuAccess.isPermissionGranted()

    fun requestShizukuPermission() {
        shizukuAccess.requestPermission()
    }
}
