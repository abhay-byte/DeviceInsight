package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext

/**
 * Provides real-time GPU metrics: usage %, current/max frequency, temperature,
 * and a human-readable renderer/model name.
 *
 * Detection strategy per vendor (no public SDK exists — these are vendor-specific sysfs nodes):
 *  - Qualcomm Adreno:  /sys/class/kgsl/kgsl-3d0/gpubusy      ("busy_ns total_ns")
 *                      /sys/class/kgsl/kgsl-3d0/gpuclk      (Hz)
 *                      /sys/class/kgsl/kgsl-3d0/max_gpuclk  (Hz)
 *  - ARM Mali:         /sys/class/devfreq/<maliN>/busy_time
 *                      /sys/class/devfreq/<maliN>/total_time
 *                      /sys/class/devfreq/<maliN>/cur_freq / max_freq
 *  - PowerVR (IMG):   /sys/class/devfreq/<pvr*|img*>/busy_time + total_time
 *  - Samsung Xclipse: /sys/class/devfreq/<exynos-gpu*|xclipse*>/busy_time + total_time
 *
 * Usage % is computed as Δbusy / Δtotal over the sample interval (one second).
 * If busy_time is unavailable, falls back to cur_freq/max_freq as a proxy.
 *
 * GPU temperature is read from /sys/class/thermal matching "gpu" / "gfx" / "mali" / "adreno" / "xclipse".
 */
@Singleton
class GpuUsageProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "GpuUsageProvider"

    enum class Vendor { UNKNOWN, ADRENO, MALI, POWERVR, XCLIPSE }

    data class Probe(
        val vendor: Vendor,
        /** Path whose busy_time + total_time to read. Null = no delta path. */
        val busyTotalPath: String?,
        /** Path whose cur_freq to read (Hz). */
        val curFreqPath: String?,
        /** Path whose max_freq to read (Hz). */
        val maxFreqPath: String?,
        /** Human-readable path label (used for diagnostics). */
        val sourceLabel: String
    )

    data class Metrics(
        val usage: Float,         // 0.0 .. 1.0
        val curFreqMhz: Int,
        val maxFreqMhz: Int,
        val temperatureC: Float,   // °C, NaN/-1 if unavailable
        val vendor: Vendor,
        val renderer: String,      // e.g. "Adreno (TM) 750"
        val sourceLabel: String    // which sysfs path produced the data
    )

    private val probe: Probe by lazy { detectProbe() }
    private val renderer: String by lazy { readRenderer() }

    @Volatile private var prevBusy: Long = -1
    @Volatile private var prevTotal: Long = -1
    @Volatile private var initialized: Boolean = false

    /** Cached Shizuku availability (cheap to check). */
    private val shizukuAvailable: Boolean by lazy {
        try { Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED }
        catch (_: Exception) { false }
    }

    fun getMetrics(): Metrics {
        val p = probe
        val usage = computeUsage(p)
        val (curHz, maxHz) = readFreqs(p)
        val temp = readGpuTemp()
        return Metrics(
            usage = usage,
            curFreqMhz = (curHz / 1_000_000).toInt(),
            maxFreqMhz = (maxHz / 1_000_000).toInt(),
            temperatureC = temp,
            vendor = p.vendor,
            renderer = renderer.ifBlank { p.vendor.name },
            sourceLabel = p.sourceLabel
        )
    }

    // ────────────────────────────── Detection ──────────────────────────────

    private fun detectProbe(): Probe {
        // 1. Adreno via kgsl
        tryKgsl()?.let { return it }

        // 2. devfreq scan — Mali / PowerVR / Xclipse / generic
        devfreqProbe()?.let { return it }

        // 3. Frequency-only fallback
        try {
            val freq = File("/sys/class/kgsl/kgsl-3d0/gpuclk")
            val maxF = File("/sys/class/kgsl/kgsl-3d0/max_gpuclk")
            if (freq.exists()) {
                return Probe(Vendor.UNKNOWN, null, freq.absolutePath,
                    if (maxF.exists()) maxF.absolutePath else null, "freq-only:kgsl")
            }
        } catch (_: Exception) {}

        return Probe(Vendor.UNKNOWN, null, null, null, "none")
    }

    private fun tryKgsl(): Probe? = try {
        val busyFile = File("/sys/class/kgsl/kgsl-3d0/gpubusy")
        val freqFile = File("/sys/class/kgsl/kgsl-3d0/gpuclk")
        val maxFile  = File("/sys/class/kgsl/kgsl-3d0/max_gpuclk")
        val hasBusy = busyFile.exists() || hasElevatedRead(busyFile.absolutePath)
        if (hasBusy) {
            Probe(
                Vendor.ADRENO,
                busyFile.absolutePath,
                freqFile.absolutePath,
                maxFile.absolutePath,
                "kgsl:kgsl-3d0"
            )
        } else null
    } catch (_: Exception) { null }

    private fun devfreqProbe(): Probe? {
        return try {
            val dir = File("/sys/class/devfreq")
            // Without elevation, the listing itself may fail. If so, probe known names.
            val entries = dir.listFiles()?.toList() ?: if (shizukuAvailable || isRootAvailable()) {
                listGpuDevfreqElevated()
            } else emptyList()

            val gpuLike = entries.filter { isGpuNode(it.name) }
            val candidates = (gpuLike + entries).distinctBy { it.absolutePath }

            for (node in candidates) {
                val busyPath = File(node, "busy_time").absolutePath
                val totalPath = File(node, "total_time").absolutePath
                val busyVal = readSysfsFirstAvailable(busyPath)?.trim()
                val totalVal = readSysfsFirstAvailable(totalPath)?.trim()
                if (!busyVal.isNullOrEmpty() && !totalVal.isNullOrEmpty()) {
                    val vendor = classifyByName(node.name)
                    val freq = File(node, "cur_freq").takeIf { it.exists() || hasElevatedRead(File(node, "cur_freq").absolutePath) }
                    val max = File(node, "max_freq").takeIf { it.exists() || hasElevatedRead(File(node, "max_freq").absolutePath) }
                    return Probe(
                        vendor = vendor,
                        busyTotalPath = node.absolutePath,
                        curFreqPath = freq?.absolutePath,
                        maxFreqPath = max?.absolutePath,
                        sourceLabel = "devfreq:${node.name}"
                    )
                }
            }
            null
        } catch (_: Exception) { null }
    }

    private fun listGpuDevfreqElevated(): List<File> {
        val out = execElevated("ls /sys/class/devfreq/ 2>/dev/null")
        if (out.isEmpty()) return emptyList()
        return out.map { File("/sys/class/devfreq/$it") }
    }

    private fun hasElevatedRead(path: String): Boolean =
        shizukuAvailable || isRootAvailable()

    private fun isGpuNode(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("gpu") || n.contains("gfx") ||
            n.contains("mali") || n.contains("adreno") ||
            n.contains("pvr") || n.contains("powervr") ||
            n.contains("xclipse") || n.contains("rogue") ||
            n.contains("IMG")
    }

    private fun classifyByName(name: String): Vendor {
        val n = name.lowercase()
        return when {
            n.contains("mali") -> Vendor.MALI
            n.contains("adreno") || n.contains("kgsl") -> Vendor.ADRENO
            n.contains("xclipse") -> Vendor.XCLIPSE
            n.contains("pvr") || n.contains("powervr") ||
                n.contains("rogue") || n.contains("img") -> Vendor.POWERVR
            else -> Vendor.UNKNOWN
        }
    }

    // ────────────────────────────── Usage calc ─────────────────────────────

    private fun computeUsage(p: Probe): Float {
        val path = p.busyTotalPath ?: return freqBasedUsage(p)
        return try {
            val (busy, total) = readBusyTotal(path)
            if (!initialized || prevTotal < 0 || total <= prevTotal || busy < prevBusy) {
                // First sample or counter wrapped — seed and report 0.
                prevBusy = busy
                prevTotal = total
                initialized = true
                0f
            } else {
                val dBusy = (busy - prevBusy).coerceAtLeast(0L)
                val dTotal = (total - prevTotal).coerceAtLeast(0L)
                prevBusy = busy
                prevTotal = total
                if (dTotal <= 0L) 0f
                else (dBusy.toDouble() / dTotal.toDouble()).toFloat().coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            Log.w(tag, "computeUsage failed (${p.sourceLabel}): ${e.message}")
            freqBasedUsage(p)
        }
    }

    private fun freqBasedUsage(p: Probe): Float {
        val cur = p.curFreqPath?.let { readLongOrZero(it) } ?: return 0f
        val max = p.maxFreqPath?.let { readLongOrZero(it) } ?: return 0f
        if (max <= 0) return 0f
        return (cur.toDouble() / max.toDouble()).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Reads "busy_ns total_ns" from /sys/class/kgsl/kgsl-3d0/gpubusy
     * OR two separate busy_time / total_time files under a devfreq node.
     */
    private fun readBusyTotal(path: String): Pair<Long, Long> {
        val kgsl = File("/sys/class/kgsl/kgsl-3d0/gpubusy")
        return if (kgsl.exists() && kgsl.absolutePath == path) {
            val tokens = readSysfsFirstAvailable(path)
                ?.trim().orEmpty().split("\\s+".toRegex())
            require(tokens.size >= 2)
            tokens[0].toLong() to tokens[1].toLong()
        } else {
            val busy = readSysfsFirstAvailable(File(path, "busy_time").absolutePath)
                ?.trim()?.toLong() ?: 0L
            val total = readSysfsFirstAvailable(File(path, "total_time").absolutePath)
                ?.trim()?.toLong() ?: 0L
            busy to total
        }
    }

    private fun readFreqs(p: Probe): Pair<Long, Long> {
        val cur = p.curFreqPath?.let { readLongOrZero(it) } ?: 0L
        val max = p.maxFreqPath?.let { readLongOrZero(it) } ?: 0L
        return cur to max
    }

    private fun readLongOrZero(path: String): Long =
        readSysfsFirstAvailable(path)?.trim()?.toLongOrNull() ?: 0L

    /**
     * Reads a sysfs file. Tries Shizuku elevation first, then root via `su`,
     * then falls back to a direct read (works on debuggable builds / relaxed SELinux).
     * Returns null if all methods fail.
     */
    private fun readSysfsFirstAvailable(path: String): String? {
        if (shizukuAvailable) {
            val out = execElevated("cat $path")
            if (out.isNotEmpty()) return out.joinToString("\n")
        }
        if (isRootAvailable()) {
            val out = execRoot("cat $path")
            if (out.isNotEmpty()) return out.joinToString("\n")
        }
        return try {
            File(path).takeIf { it.exists() && it.canRead() }?.readText()
        } catch (_: Exception) { null }
    }

    // ────────────────────────────── Temperature ───────────────────────────

    private fun readGpuTemp(): Float {
        return try {
            val dir = File("/sys/class/thermal")
            val zones: List<File> = dir.listFiles { f -> f.name.startsWith("thermal_zone") }
                ?.toList()
                ?: if (shizukuAvailable || isRootAvailable()) listThermalZonesElevated()
                else emptyList()
            if (zones.isEmpty()) return -1f

            var best: Float = -1f
            var bestRank = Int.MIN_VALUE

            for (z in zones) {
                val type = runCatching {
                    readSysfsFirstAvailable(File(z, "type").absolutePath)
                        ?.trim()?.lowercase()
                }.getOrNull() ?: continue
                val raw = runCatching {
                    readSysfsFirstAvailable(File(z, "temp").absolutePath)
                        ?.trim()?.toLong()
                }.getOrNull() ?: continue
                val temp = if (raw > 1000 || raw < -1000) raw / 1000f else raw.toFloat()
                if (temp <= 0f) continue
                val rank = when {
                    type.contains("gpu") || type.contains("gfx") -> 100
                    type.contains("xclipse") -> 80
                    type.contains("mali") -> 70
                    type.contains("adreno") -> 70
                    type.contains("tsens") && type.contains("gpu") -> 90
                    else -> -1
                }
                if (rank > bestRank) {
                    bestRank = rank
                    best = temp
                }
            }
            best
        } catch (e: Exception) {
            Log.w(tag, "readGpuTemp: ${e.message}")
            -1f
        }
    }

    private fun listThermalZonesElevated(): List<File> {
        val out = execElevated("ls /sys/class/thermal/ 2>/dev/null")
        if (out.isEmpty()) return emptyList()
        return out
            .filter { it.startsWith("thermal_zone") }
            .map { File("/sys/class/thermal/$it") }
    }

    // ────────────────────────────── Renderer ──────────────────────────────

    private fun readRenderer(): String {
        // Try the existing GpuProvider renderer via glGetString fallback chain.
        return try {
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            egl.eglInitialize(display, IntArray(2))
            val attrs = intArrayOf(EGL10.EGL_RENDERABLE_TYPE, 0x4, EGL10.EGL_NONE)
            val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
            val n = IntArray(1)
            egl.eglChooseConfig(display, attrs, configs, 1, n)
            val cfg = configs[0] ?: return fallbackRenderer()
            val ctxAttrs = intArrayOf(0x3098, 3, EGL10.EGL_NONE)
            val ctx = egl.eglCreateContext(display, cfg, EGL10.EGL_NO_CONTEXT, ctxAttrs)
            val surf = egl.eglCreatePbufferSurface(display, cfg,
                intArrayOf(EGL10.EGL_WIDTH, 1, EGL10.EGL_HEIGHT, 1, EGL10.EGL_NONE))
            egl.eglMakeCurrent(display, surf, surf, ctx)
            val ren = (ctx.gl as javax.microedition.khronos.opengles.GL10)
                .glGetString(javax.microedition.khronos.opengles.GL10.GL_RENDERER)
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, surf)
            egl.eglDestroyContext(display, ctx)
            egl.eglTerminate(display)
            ren ?: fallbackRenderer()
        } catch (e: Exception) {
            Log.w(tag, "readRenderer: ${e.message}")
            fallbackRenderer()
        }
    }

    private fun fallbackRenderer(): String {
        val renderer = try {
            // Renderer property available on many devices (often empty on modern builds).
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java, String::class.java)
                .invoke(null, "ro.hardware.egl", "") as String
        } catch (_: Exception) { "" }
        return renderer.ifBlank {
            // Fall back to OpenGL ES version + SoC hints
            val esVer = (context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager).deviceConfigurationInfo.glEsVersion
            "OpenGL ES $esVer · ${Build.HARDWARE}".trim()
        }
    }

    // ──────────────────────────── Elevation helpers ──────────────────────

    private fun isRootAvailable(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            proc.inputStream.bufferedReader().use { it.readLine() == "0" }
        } catch (_: Exception) { false }
    }

    private fun execElevated(command: String): List<String> {
        if (!shizukuAvailable) return emptyList()
        return try {
            val m = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val proc = m.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            readProcessLines(proc)
        } catch (e: Exception) {
            Log.w(tag, "Shizuku exec failed: ${e.message}")
            emptyList()
        }
    }

    private fun execRoot(command: String): List<String> {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            readProcessLines(proc)
        } catch (e: Exception) {
            Log.w(tag, "Root exec failed: ${e.message}")
            emptyList()
        }
    }

    private fun readProcessLines(proc: Process): List<String> {
        val out = mutableListOf<String>()
        BufferedReader(InputStreamReader(proc.inputStream)).use { r ->
            var line: String?
            while (r.readLine().also { line = it } != null) {
                out.add(line!!)
            }
        }
        proc.waitFor()
        return out
    }
}