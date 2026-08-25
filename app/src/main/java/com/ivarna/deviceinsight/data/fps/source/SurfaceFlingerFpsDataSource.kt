package com.ivarna.deviceinsight.data.fps.source

import com.ivarna.deviceinsight.data.fps.model.FpsMethod
import com.ivarna.deviceinsight.data.fps.model.FpsSnapshot
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import com.ivarna.deviceinsight.data.fps.util.ForegroundAppResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SurfaceFlingerFpsDataSource @Inject constructor(
    private val shellGateway: ShellGateway,
    private val foregroundAppResolver: ForegroundAppResolver
) : FpsDataSource {

    override val priority: Int = 2

    // Very short surface cache — correctness over saving one --list call.
    // Invalidated on package change, latency empty, or surface not found.
    private var cachedPackage: String? = null
    private var cachedSurface: String? = null
    private var cachedAtMs: Long = 0L
    private val cacheTtlMs = 800L

    override suspend fun readFps(): FpsSnapshot? {
        val foreground = foregroundAppResolver.resolve() ?: return null
        val pkg = foreground.packageName
        val surface = findSurfaceForPackage(pkg)
            ?: run {
                // Surface disappeared -> invalidate cache so next tick re-discovers
                invalidateCache()
                return null
            }
        val (result, tier) = shellGateway.executePolicy("dumpsys SurfaceFlinger --latency \"$surface\" 2>/dev/null")
        if (!result.isSuccess) {
            // Latency failed -> invalidate cached surface (ID may have changed after rotation)
            invalidateCacheIf(pkg)
            return null
        }
        if (result.output.isBlank()) {
            invalidateCacheIf(pkg)
            return null
        }
        val snapshot = parseLatency(result.output, tier, pkg, surface)
        if (snapshot == null) {
            // No valid frames (e.g., Android 15 latency with only refreshPeriod)
            // Keep cache but don't treat as persistent failure; next tick will retry same surface
            // If we repeatedly get null, findSurface cache TTL will eventually refresh
        }
        return snapshot
    }

    internal fun findSurfaceForPackage(packageName: String): String? {
        val now = System.currentTimeMillis()
        if (packageName == cachedPackage && cachedSurface != null && now - cachedAtMs < cacheTtlMs) {
            return cachedSurface
        }
        // Fresh --list
        val (listResult, _) = shellGateway.executePolicy("dumpsys SurfaceFlinger --list 2>/dev/null")
        if (!listResult.isSuccess || listResult.output.isBlank()) {
            invalidateCacheIf(packageName)
            return null
        }
        val shortPkg = packageName.substringAfterLast('.')
        val owned = listResult.output.lineSequence()
            .map { it.trim() }
            .mapNotNull { parseLayerName(it) }
            .filter { it.contains(packageName) || (shortPkg.length >= 4 && it.contains(shortPkg)) }
            .filter { !it.contains("ActivityRecord") && !it.contains("InputSink") }
            .toList()
        if (owned.isEmpty()) {
            invalidateCacheIf(packageName)
            return null
        }
        val preferred = owned.firstOrNull { line ->
            listOf("SurfaceView", "NativeActivity", "Vulkan", "GLSurfaceView")
                .any { marker -> line.contains(marker, ignoreCase = true) }
        }
        val chosen = preferred
            ?: owned.firstOrNull { it.contains("#") }
            ?: owned.firstOrNull()

        if (chosen != null) {
            cachedPackage = packageName
            cachedSurface = chosen
            cachedAtMs = now
        } else {
            invalidateCacheIf(packageName)
        }
        return chosen
    }

    private fun invalidateCache() {
        cachedPackage = null
        cachedSurface = null
        cachedAtMs = 0L
    }

    private fun invalidateCacheIf(pkg: String) {
        if (cachedPackage == pkg) invalidateCache()
    }

    internal fun parseLayerName(rawLine: String): String? {
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty()) return null
        val brace = Regex("""RequestedLayerState\{([^}]+)\}""").find(trimmed)
        val body = brace?.groupValues?.get(1) ?: trimmed
        val withoutHandle = body.replace(Regex("""^[0-9a-fA-F]+\s+"""), "")
        val name = withoutHandle
            .replace(Regex("""\s+parentId=.*$"""), "")
            .replace(Regex("""\s+z=.*$"""), "")
            .replace(Regex("""\s+relativeParentId=.*$"""), "")
            .trim()
        return name.takeIf { it.isNotEmpty() }
    }

    internal fun parseLatency(
        output: String,
        accessTier: PrivilegeTier? = null,
        packageName: String? = null,
        surfaceName: String? = null
    ): FpsSnapshot? {
        val lines = output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.size < 2) return null // Android 15 regression: only refreshPeriod
        val refreshPeriodNs = lines[0].toLongOrNull() ?: return null
        if (refreshPeriodNs <= 0L || refreshPeriodNs == Long.MAX_VALUE) return null

        val frametimesMs = mutableListOf<Float>()
        var jankCount = 0
        var prevExpected = 0
        for (line in lines.drop(1)) {
            val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.size < 3) continue
            val frameStartNs = parts[0].toLongOrNull() ?: continue
            val frameCompleteNs = parts[2].toLongOrNull() ?: continue
            if (frameStartNs == 0L || frameCompleteNs == 0L) continue
            if (frameStartNs == Long.MAX_VALUE || frameCompleteNs == Long.MAX_VALUE) continue
            if (frameCompleteNs <= frameStartNs) continue
            val frameTimeNs = frameCompleteNs - frameStartNs
            if (frameTimeNs <= 0) continue
            val frameTimeMs = frameTimeNs / 1_000_000f
            if (frameTimeMs <= 0f || frameTimeMs > 2000f) continue
            frametimesMs.add(frameTimeMs)
            val expected = kotlin.math.ceil(frameTimeNs.toDouble() / refreshPeriodNs).toInt()
            if (prevExpected > 0 && expected > prevExpected) jankCount++
            prevExpected = expected
        }
        if (frametimesMs.isEmpty()) return null
        val avgMs = frametimesMs.average().toFloat()
        if (avgMs <= 0f) return null
        val fps = (1000f / avgMs).coerceIn(1f, 240f)
        return FpsSnapshot(
            currentFps = fps,
            frametimeAvgMs = avgMs,
            frametimes = frametimesMs,
            jankCount = jankCount,
            method = FpsMethod.SURFACEFLINGER,
            access = accessTier,
            packageName = packageName
        )
    }

    // For tests that call parseLatency(output) without provenance
    internal fun parseLatency(output: String): FpsSnapshot? = parseLatency(output, null, null, null)
}
