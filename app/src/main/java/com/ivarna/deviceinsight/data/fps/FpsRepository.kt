package com.ivarna.deviceinsight.data.fps

import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.model.FpsSnapshot
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.source.GfxinfoFpsDataSource
import com.ivarna.deviceinsight.data.fps.source.SurfaceFlingerFpsDataSource
import com.ivarna.deviceinsight.data.fps.util.ForegroundAppResolver
import com.ivarna.deviceinsight.data.monitor.HudSettingsCache
import javax.inject.Inject
import javax.inject.Singleton

interface FpsRepository {
    suspend fun getFps(): FpsSnapshot
}

@Singleton
class FpsRepositoryImpl @Inject constructor(
    private val surfaceFlingerSource: SurfaceFlingerFpsDataSource,
    private val gfxinfoSource: GfxinfoFpsDataSource,
    private val foregroundAppResolver: ForegroundAppResolver,
    private val shellGateway: ShellGateway,
    private val hudSettingsCache: HudSettingsCache
) : FpsRepository {

    private var lastGoodSnapshot: FpsSnapshot? = null
    private var lastGoodAtMs: Long = 0L
    private var lastSource: FpsMethod? = null
    private var lastPackage: String? = null
    private val recentDisplayFps = ArrayDeque<Float>(3)
    private var lastGoodPackage: String? = null

    override suspend fun getFps(): FpsSnapshot {
        // Keep gateway mode in sync with current setting (fixes AUTO chain on every sample)
        shellGateway.setModeFromString(hudSettingsCache.fpsMode)

        val foreground = foregroundAppResolver.resolve()
        val pkg = foreground?.packageName
        val isGame = pkg?.let { foregroundAppResolver.isGameLikeSurface(it) } ?: false

        // Package changed -> reset package-specific state (gfxinfo timestamps, SF cache is handled in source)
        if (pkg != lastPackage) {
            // Do not carry lastGood across packages (would show old game's FPS on new app)
            if (lastPackage != null && pkg != null) {
                lastGoodSnapshot = null
                lastGoodAtMs = 0L
                recentDisplayFps.clear()
            }
            lastPackage = pkg
            // lastSource reset will be handled below when method changes
        }

        var rawSnapshot: FpsSnapshot? = null

        if (isGame) {
            // Games: SF only, never gfxinfo (gfxinfo measures UI, not Vulkan/NativeActivity)
            rawSnapshot = surfaceFlingerSource.readFps()?.takeIf { it.currentFps > 0f && it.method != FpsMethod.NONE }
            // No gfxinfo fallback for games — honest "—" if SF unavailable
        } else {
            // UI: SF first, then gfxinfo fallback
            rawSnapshot = surfaceFlingerSource.readFps()?.takeIf { it.currentFps > 0f && it.method != FpsMethod.NONE }
            if (rawSnapshot == null) {
                rawSnapshot = gfxinfoSource.readFps()?.takeIf { it.currentFps > 0f && it.method != FpsMethod.NONE }
            }
        }

        // Hold last good briefly to avoid flashing 0/— on transient failures
        if (rawSnapshot == null && lastGoodSnapshot != null) {
            val age = System.currentTimeMillis() - lastGoodAtMs
            // Only hold if same package and same source tier is still allowed
            val samePackage = lastGoodPackage == pkg
            if (age < LAST_GOOD_HOLD_MS && samePackage) {
                rawSnapshot = lastGoodSnapshot!!.copy(isStale = true)
            } else if (age >= LAST_GOOD_HOLD_MS) {
                lastGoodSnapshot = null
            }
        }

        // Update lastGood when we have fresh valid data
        if (rawSnapshot != null && !rawSnapshot.isStale && rawSnapshot.currentFps > 0f && rawSnapshot.method != FpsMethod.NONE) {
            // Source changed -> clear smoothing window
            if (rawSnapshot.method != lastSource) {
                recentDisplayFps.clear()
                lastSource = rawSnapshot.method
            }
            lastGoodSnapshot = rawSnapshot
            lastGoodAtMs = System.currentTimeMillis()
            lastGoodPackage = pkg
        } else if (rawSnapshot == null) {
            // No data -> clear source tracking after TTL expires
            // Keep lastSource for smoothing until lastGood expires
            if (lastGoodSnapshot == null) {
                lastSource = null
            }
        }

        if (rawSnapshot == null) return FpsSnapshot.ZERO.copy(packageName = pkg)

        // Smooth current FPS (median of 3) — separate from long-window percentiles (not yet buffered)
        // For DeviceInsight HUD, we keep responsiveness via recent median, not EMA
        val smoothed = smoothDisplayFps(rawSnapshot.currentFps)
        return rawSnapshot.copy(currentFps = smoothed)
    }

    private fun smoothDisplayFps(fps: Float): Float {
        if (fps <= 0f) return fps
        while (recentDisplayFps.size >= 3) recentDisplayFps.removeFirst()
        recentDisplayFps.addLast(fps)
        if (recentDisplayFps.size < 2) return fps
        val sorted = recentDisplayFps.sorted()
        return sorted[sorted.size / 2]
    }

    companion object {
        const val LAST_GOOD_HOLD_MS = 3500L
    }
}
