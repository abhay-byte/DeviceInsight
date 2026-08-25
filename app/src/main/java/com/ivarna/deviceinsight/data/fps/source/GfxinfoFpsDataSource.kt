package com.ivarna.deviceinsight.data.fps.source

import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.model.FpsSnapshot
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.util.ForegroundAppResolver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-only FPS via gfxinfo framestats. Inaccurate for Vulkan/SurfaceView games.
 */
@Singleton
class GfxinfoFpsDataSource @Inject constructor(
    private val shellGateway: ShellGateway,
    private val foregroundAppResolver: ForegroundAppResolver
) : FpsDataSource {

    override val priority: Int = 3

    private var lastFrameCompletedNs: Long? = null
    private var lastPollTimeMs: Long = 0L
    private var lastPackage: String? = null
    private var profileBootstrapped = false

    private companion object {
        const val MAX_PLAUSIBLE_FRAMETIME_MS = 100f
        const val BOOTSTRAP_MAX_FRAMES = 90
    }

    override suspend fun readFps(): FpsSnapshot? {
        val foreground = foregroundAppResolver.resolve() ?: return null
        if (foreground.packageName != lastPackage) {
            lastFrameCompletedNs = null
            lastPollTimeMs = 0L
            lastPackage = foreground.packageName
            profileBootstrapped = false
        }
        val (result, tier) = shellGateway.executePolicy("dumpsys gfxinfo ${foreground.packageName} framestats 2>/dev/null")
        if (!result.isSuccess || result.output.isBlank()) return null
        return parseGfxinfo(result.output, System.currentTimeMillis(), foreground.refreshRateHz, tier, foreground.packageName)
    }

    internal fun parseGfxinfo(
        output: String,
        nowMs: Long,
        refreshRateHz: Float = 60f,
        accessTier: com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier? = null,
        packageName: String? = null
    ): FpsSnapshot? {
        // Reset package-specific state when package changes (also handled in readFps)
        if (packageName != null && packageName != lastPackage) {
            lastFrameCompletedNs = null
            lastPollTimeMs = 0L
            lastPackage = packageName
            profileBootstrapped = false
        } else if (packageName != null && lastPackage == null) {
            lastPackage = packageName
        }
        var frametimesMs = parseFrameCompletedTimestamps(output)
        if (frametimesMs.isEmpty() && !profileBootstrapped) {
            frametimesMs = parseProfileBootstrap(output)
            profileBootstrapped = true
        }
        if (frametimesMs.isNotEmpty()) {
            val avgMs = frametimesMs.average().toFloat()
            val pollDeltaSec = if (lastPollTimeMs > 0) (nowMs - lastPollTimeMs) / 1000f else 0f
            lastPollTimeMs = nowMs
            val fps = fpsFromFrametimes(frametimesMs, avgMs, pollDeltaSec, refreshRateHz)
            if (fps <= 0f || fps.isNaN() || fps.isInfinite()) return null
            return FpsSnapshot(
                currentFps = fps.coerceIn(1f, 240f),
                frametimeAvgMs = avgMs,
                frametimes = frametimesMs,
                method = FpsMethod.GFXINFO,
                access = accessTier,
                packageName = packageName
            )
        }
        val uiHistogram = parseUiHistogram(output)
        if (uiHistogram.isNotEmpty()) {
            val histogramFps = fpsFromHistogram(uiHistogram)
            if (histogramFps <= 0f) return null
            return FpsSnapshot(
                currentFps = histogramFps,
                frametimeAvgMs = 1000f / histogramFps,
                method = FpsMethod.GFXINFO,
                access = accessTier,
                packageName = packageName
            )
        }
        val histogramFps = parseGpuHistogram(output)
        if (histogramFps <= 0f) return null
        return FpsSnapshot(
            currentFps = histogramFps,
            frametimeAvgMs = 1000f / histogramFps,
            method = FpsMethod.GFXINFO,
            access = accessTier,
            packageName = packageName
        )
    }

    private fun parseFrameCompletedTimestamps(output: String): List<Float> {
        val frametimes = mutableListOf<Float>()
        var inStats = false
        var frameCompletedIdx = -1
        for (line in output.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("---PROFILEDATA---")) { inStats = true; continue }
            if (!inStats) continue
            if (trimmed.startsWith("---")) break
            if (trimmed.startsWith("Flags,")) {
                val headers = trimmed.split(',').map { it.trim() }
                frameCompletedIdx = headers.indexOf("FrameCompleted")
                continue
            }
            if (frameCompletedIdx < 0) continue
            val parts = trimmed.split(',').map { it.trim() }
            if (parts.size <= frameCompletedIdx) continue
            val frameCompleted = parts[frameCompletedIdx].toLongOrNull() ?: continue
            val prev = lastFrameCompletedNs
            if (prev != null && frameCompleted > prev) {
                val deltaMs = (frameCompleted - prev) / 1_000_000f
                if (isPlausible(deltaMs)) frametimes.add(deltaMs)
            }
            lastFrameCompletedNs = frameCompleted
        }
        return frametimes
    }

    private fun parseProfileBootstrap(output: String): List<Float> {
        val frametimes = mutableListOf<Float>()
        var inStats = false
        var frameCompletedIdx = -1
        var prevCompleted: Long? = null
        for (line in output.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("---PROFILEDATA---")) { inStats = true; frameCompletedIdx = -1; continue }
            if (!inStats) continue
            if (trimmed.startsWith("---")) break
            if (trimmed.startsWith("Flags,")) {
                val headers = trimmed.split(',').map { it.trim() }
                frameCompletedIdx = headers.indexOf("FrameCompleted")
                continue
            }
            if (frameCompletedIdx < 0) continue
            val parts = trimmed.split(',').map { it.trim() }
            if (parts.size <= frameCompletedIdx) continue
            val frameCompleted = parts[frameCompletedIdx].toLongOrNull() ?: continue
            val prev = prevCompleted
            if (prev != null && frameCompleted > prev) {
                val deltaMs = (frameCompleted - prev) / 1_000_000f
                if (isPlausible(deltaMs)) frametimes.add(deltaMs)
            }
            prevCompleted = frameCompleted
        }
        return frametimes.takeLast(BOOTSTRAP_MAX_FRAMES)
    }

    private fun isPlausible(deltaMs: Float): Boolean = deltaMs > 0f && deltaMs <= MAX_PLAUSIBLE_FRAMETIME_MS

    private fun fpsFromFrametimes(frametimesMs: List<Float>, avgMs: Float, pollDeltaSec: Float, refreshRateHz: Float): Float {
        val refreshCeiling = refreshRateHz.coerceIn(1f, 240f)
        if (frametimesMs.size >= 2 && avgMs > 0f) {
            val ftFps = (1000f / avgMs).coerceIn(1f, 240f)
            val expectedMs = 1000f / refreshCeiling
            if (avgMs in expectedMs * 0.82f..expectedMs * 1.18f) return refreshCeiling
            return ftFps.coerceAtMost(refreshCeiling)
        }
        if (pollDeltaSec > 0f && frametimesMs.isNotEmpty()) {
            return (frametimesMs.size / pollDeltaSec).coerceIn(1f, refreshCeiling)
        }
        return (1000f / avgMs).coerceIn(1f, refreshCeiling)
    }

    private fun parseUiHistogram(output: String): Map<Int, Int> {
        val histogramLine = output.lineSequence()
            .filter { line -> val t = line.trim(); t.startsWith("HISTOGRAM:") && !t.startsWith("GPU HISTOGRAM:") }
            .lastOrNull() ?: return emptyMap()
        val buckets = linkedMapOf<Int, Int>()
        val entries = histogramLine.substringAfter("HISTOGRAM:").trim().split(Regex("\\s+"))
        for (entry in entries) {
            val match = Regex("""([\d.]+)ms=(\d+)""").find(entry) ?: continue
            val ms = match.groupValues[1].toDoubleOrNull()?.toInt() ?: continue
            val count = match.groupValues[2].toIntOrNull() ?: continue
            if (ms >= 4950 || count <= 0) continue
            buckets[ms] = count
        }
        return buckets
    }

    private fun fpsFromHistogram(buckets: Map<Int, Int>): Float {
        var totalFrames = 0
        var weightedMs = 0.0
        for ((ms, count) in buckets) { totalFrames += count; weightedMs += ms * count }
        if (totalFrames == 0) return 0f
        val avgMs = weightedMs / totalFrames
        return if (avgMs > 0) (1000.0 / avgMs).toFloat() else 0f
    }

    private fun parseGpuHistogram(output: String): Float {
        val histogramLine = output.lineSequence().firstOrNull { it.trim().startsWith("GPU HISTOGRAM:") } ?: return 0f
        var totalFrames = 0
        var weightedMs = 0.0
        val entries = histogramLine.substringAfter("GPU HISTOGRAM:").trim().split(Regex("\\s+"))
        for (entry in entries) {
            val match = Regex("""([\d.]+)ms=(\d+)""").find(entry) ?: continue
            val ms = match.groupValues[1].toDoubleOrNull() ?: continue
            val count = match.groupValues[2].toIntOrNull() ?: continue
            if (ms >= 4950) continue
            totalFrames += count; weightedMs += ms * count
        }
        if (totalFrames == 0) return 0f
        val avgMs = weightedMs / totalFrames
        return if (avgMs > 0) (1000.0 / avgMs).toFloat() else 0f
    }

    // For tests with 3-arg call
    internal fun parseGfxinfo(output: String, nowMs: Long, refreshRateHz: Float): FpsSnapshot? =
        parseGfxinfo(output, nowMs, refreshRateHz, null, null)
}
