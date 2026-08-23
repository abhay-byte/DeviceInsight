package com.ivarna.deviceinsight.data.monitor

import com.ivarna.deviceinsight.ui.caliper.HatchPattern
import com.ivarna.deviceinsight.ui.caliper.widget.MemSeg
import kotlin.math.max

data class MemComposition(
    val segs: List<MemSeg>,
    val totalBytes: Long = 0L
)

object MemInfoParser {
    /**
     * Parse /proc/meminfo string into MemSeg fractions.
     * MemTotal is denominator for ALL.
     * ZRAM backing is de-duplicated from swap.
     * Returns normalized fractions summing to 1.
     */
    fun parse(meminfo: String, zramBytes: Long?): MemComposition {
        val map = mutableMapOf<String, Long>()
        meminfo.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val parts = trimmed.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val key = parts[0].trimEnd(':')
                val kb = parts[1].toLongOrNull() ?: return@forEach
                map[key] = kb * 1024
            }
        }
        val memTotal = map["MemTotal"] ?: return fallback(map)
        if (memTotal <= 0) return fallback(map)

        val memAvailable = map["MemAvailable"]
        val active = map["Active"] ?: map["Active(anon)"]
        val cached = map["Cached"] ?: 0L
        val swapTotal = map["SwapTotal"] ?: 0L
        val swapFree = map["SwapFree"] ?: 0L

        val activeF: Float
        val fallbackActive: Float? = if (active == null && memAvailable != null) {
            // fallback = (MemTotal - MemAvailable - Cached - zram)/MemTotal
            val z = zramBytes ?: 0L
            ((memTotal - memAvailable - cached - z).toFloat() / memTotal.toFloat()).coerceIn(0f, 1f)
        } else null

        activeF = when {
            active != null -> active.toFloat() / memTotal.toFloat()
            fallbackActive != null -> fallbackActive
            else -> 0f
        }

        val cachedF = cached.toFloat() / memTotal.toFloat()
        val zramF = if (zramBytes != null && zramBytes > 0) zramBytes.toFloat() / memTotal.toFloat() else 0f
        val swapRawF = if (swapTotal > 0) (swapTotal - swapFree).toFloat() / memTotal.toFloat() else 0f
        val swapF = max(0f, swapRawF - zramF)
        var freeF = 1f - (activeF + cachedF + zramF + swapF)
        freeF = freeF.coerceAtLeast(0f)

        var sum = activeF + cachedF + zramF + swapF + freeF
        var aF = activeF
        var cF = cachedF
        var zF = zramF
        var sF = swapF
        var fF = freeF
        if (sum > 1f && sum > 0f) {
            val s = 1f / sum
            aF *= s
            cF *= s
            zF *= s
            sF *= s
            fF *= s
            sum = 1f
        }

        // Ensure sum close to 1 within tolerance
        val segs = mutableListOf<MemSeg>()
        segs.add(MemSeg(fraction = aF.coerceIn(0f, 1f), pattern = HatchPattern.SOLID, channelId = "CH-02"))
        segs.add(MemSeg(fraction = cF.coerceIn(0f, 1f), pattern = HatchPattern.DIAGONAL, channelId = "CH-03"))
        if (zF > 0.001f) {
            segs.add(MemSeg(fraction = zF.coerceIn(0f, 1f), pattern = HatchPattern.CROSS, channelId = "CH-04"))
        }
        // swap as bar segment only when zramF==0 && swapF>0; otherwise swap lives as subline independent
        if (zF == 0f && sF > 0.001f) {
            segs.add(MemSeg(fraction = sF.coerceIn(0f, 1f), pattern = HatchPattern.CROSS, channelId = "CH-04"))
        } else if (zF > 0f && sF > 0.001f) {
            // ZRAM exists but we still include swap if significant? Plan: swap-without-zram bar only
            // So we intentionally omit swap bar when ZRAM present (swap via subline)
        }
        segs.add(MemSeg(fraction = fF.coerceIn(0f, 1f), pattern = HatchPattern.NONE, channelId = ""))

        // Final sanity: normalize if sum deviates >0.02
        val totalFrac = segs.sumOf { it.fraction.toDouble() }.toFloat()
        if (kotlin.math.abs(totalFrac - 1f) > 0.02f && totalFrac > 0) {
            val norm = 1f / totalFrac
            for (i in segs.indices) {
                segs[i] = segs[i].copy(fraction = segs[i].fraction * norm)
            }
        }

        return MemComposition(segs = segs, totalBytes = memTotal)
    }

    private fun fallback(map: Map<String, Long>): MemComposition {
        // On parse failure (missing MemTotal) return today's usedFraction single SOLID segment
        val memTotal = map["MemTotal"] ?: 0L
        val memAvail = map["MemAvailable"] ?: 0L
        val usedFraction = if (memTotal > 0) (memTotal - memAvail).toFloat() / memTotal.toFloat() else 0.5f
        return MemComposition(
            segs = listOf(
                MemSeg(fraction = usedFraction.coerceIn(0f, 1f), pattern = HatchPattern.SOLID, channelId = "CH-02"),
                MemSeg(fraction = (1f - usedFraction).coerceIn(0f, 1f), pattern = HatchPattern.NONE, channelId = "")
            ),
            totalBytes = memTotal
        )
    }

    fun readZramBytes(): Long? {
        return try {
            val f = java.io.File("/sys/block/zram0/mm_stat")
            if (!f.exists() || !f.canRead()) return null
            val parts = f.readText().trim().split("\\s+".toRegex())
            parts.getOrNull(0)?.toLongOrNull()
        } catch (_: Exception) { null }
    }

    fun readMeminfoString(): String? {
        return try {
            java.io.File("/proc/meminfo").readText()
        } catch (_: Exception) { null }
    }
}
