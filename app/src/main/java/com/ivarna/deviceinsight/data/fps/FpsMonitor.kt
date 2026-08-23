package com.ivarna.deviceinsight.data.fps

import android.content.Context
import android.util.Log
import com.ivarna.deviceinsight.data.monitor.HudSettingsCache
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class FpsSample(val fps: Int, val source: String) // "SF" | "GFX" | "—"

@Singleton
class FpsMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hudSettingsCache: HudSettingsCache
) {

    companion object {
        private const val TAG = "FpsMonitor"
        private const val LAYER_CACHE_MS = 30_000L
    }

    enum class AccessType {
        SHIZUKU, ROOT, NONE
    }

    // Layer cache 30s to avoid dumpsys SurfaceFlinger --list every 100ms
    @Volatile private var cachedLayerName: String? = null
    @Volatile private var cachedLayerPkg: String? = null
    @Volatile private var cachedLayerTime: Long = 0L

    private fun getAccessType(): AccessType {
        val mode = hudSettingsCache.fpsMode
        if (mode == "ROOT") {
            return if (isRootAvailable()) AccessType.ROOT else AccessType.NONE
        }
        if (mode == "SHIZUKU") {
            return if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                AccessType.SHIZUKU
            } else {
                AccessType.NONE
            }
        }
        // AUTO
        return if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                AccessType.SHIZUKU
            } else {
                AccessType.NONE
            }
        } else if (isRootAvailable()) {
            AccessType.ROOT
        } else {
            AccessType.NONE
        }
    }

    private fun isRootAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun executeCommand(command: String): List<String> {
        val accessType = getAccessType()
        return try {
            when (accessType) {
                AccessType.SHIZUKU -> executeShizukuCommand(command)
                AccessType.ROOT -> {
                    val output = mutableListOf<String>()
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            output.add(line!!)
                        }
                    }
                    process.waitFor()
                    output
                }
                AccessType.NONE -> {
                    Log.e(TAG, "No root or Shizuku access available.")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing command: $command", e)
            emptyList()
        }
    }

    private fun executeShizukuCommand(command: String): List<String> {
        return try {
            val newProcessMethod = Shizuku::class.java.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = mutableListOf<String>()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.add(line!!)
                }
            }
            process.waitFor()
            output
        } catch (e: Exception) {
            Log.e(TAG, "Shizuku execution failed via reflection", e)
            emptyList()
        }
    }

    /**
     * Gets the current FPS with source stamp.
     * Source: SF if SurfaceFlinger succeeds, else GFX if gfxinfo succeeds, else "—".
     */
    fun getCurrentFpsWithSource(): FpsSample {
        val pkg = getForegroundPackage() ?: return FpsSample(0, "—")
        val sfFps = getSurfaceFlingerFps(pkg)
        if (sfFps > 0) return FpsSample(sfFps, "SF")
        val gfxFps = getGfxInfoFps(pkg)
        if (gfxFps > 0) return FpsSample(gfxFps, "GFX")
        return FpsSample(0, "—")
    }

    fun getCurrentFps(): Int = getCurrentFpsWithSource().fps

    private fun getForegroundPackage(): String? {
        val cmd = "dumpsys window | grep mFocusedApp"
        val output = executeCommand(cmd)
        val focusLine = output.firstOrNull { it.contains("mFocusedApp") } ?: return null
        val regex = Regex("ActivityRecord\\{[^ ]+ [^ ]+ ([^ /]+)/")
        val match = regex.find(focusLine)
        val pkg = match?.groups?.get(1)?.value
        Log.d(TAG, "Found foreground package: $pkg from line: $focusLine")
        return pkg
    }

    private fun getGfxInfoFps(packageName: String): Int {
        val cmd = "dumpsys gfxinfo $packageName framestats"
        val output = executeCommand(cmd)
        if (output.isEmpty()) return 0
        val headerIdx = output.indexOfFirst { it.startsWith("Flags,FrameTimelineVsyncId") }
        if (headerIdx == -1) return 0
        val header = output[headerIdx].split(",")
        val vsyncIdx = header.indexOf("IntendedVsync")
        if (vsyncIdx == -1) return 0
        var frameCount = 0
        val currentTime = System.nanoTime()
        val oneSecondAgo = currentTime - 1_000_000_000L
        for (i in (headerIdx + 1) until output.size) {
            val line = output[i]
            if (line.isBlank() || line.startsWith("---PROFILEDATA---") || line.startsWith("View hierarchy:")) break
            val columns = line.split(",")
            if (columns.size > vsyncIdx) {
                val timestampStr = columns[vsyncIdx]
                try {
                    val timestamp = timestampStr.toLong()
                    if (timestamp > oneSecondAgo) {
                        frameCount++
                    }
                } catch (e: NumberFormatException) {
                    continue
                }
            }
        }
        Log.d(TAG, "GfxInfo FPS for $packageName: $frameCount")
        return frameCount
    }

    private fun getSurfaceFlingerFps(packageName: String): Int {
        val layerName = findSurfaceFlingerLayer(packageName) ?: return 0
        val cmd = "dumpsys SurfaceFlinger --latency '$layerName'"
        val output = executeCommand(cmd)
        val fps = parseFps(output)
        Log.d(TAG, "SurfaceFlinger FPS for $packageName ($layerName): $fps")
        return fps
    }

    private fun findSurfaceFlingerLayer(packageName: String): String? {
        val now = System.currentTimeMillis()
        if (packageName == cachedLayerPkg && cachedLayerName != null && now - cachedLayerTime < LAYER_CACHE_MS) {
            return cachedLayerName
        }
        val cmd = "dumpsys SurfaceFlinger --list"
        val output = executeCommand(cmd)
        val blastLayer = output.firstOrNull { it.contains(packageName) && it.contains("BLAST") }
        val result = when {
            blastLayer != null -> extractLayerName(blastLayer)
            else -> {
                val surfaceViewLayer = output.firstOrNull { it.contains(packageName) && it.contains("SurfaceView") }
                when {
                    surfaceViewLayer != null -> extractLayerName(surfaceViewLayer)
                    else -> {
                        val genericLayer = output.firstOrNull { it.contains(packageName) }
                        if (genericLayer != null) extractLayerName(genericLayer) else null
                    }
                }
            }
        }
        if (result != null) {
            cachedLayerName = result
            cachedLayerPkg = packageName
            cachedLayerTime = now
        }
        return result
    }

    private fun extractLayerName(rawLine: String): String {
        var name = rawLine.trim()
        if (name.startsWith("RequestedLayerState{")) {
            name = name.substringAfter("RequestedLayerState{").substringBeforeLast("}")
            val firstSpace = name.indexOf(' ')
            if (firstSpace != -1) {
                name = name.substring(0, firstSpace)
            }
        }
        return name
    }

    fun isShizukuPermissionGranted(): Boolean {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requestShizukuPermission() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(0)
        }
    }

    private fun parseFps(lines: List<String>): Int {
        if (lines.isEmpty()) return 0
        val now = System.nanoTime()
        val oneSecondAgo = now - 1_000_000_000L
        var frameCount = 0
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val parts = line.split("\\s+".toRegex())
            if (parts.size != 3) continue
            try {
                val actualPresentTime = parts[1].toLong()
                if (actualPresentTime == 0L || actualPresentTime == Long.MAX_VALUE) continue
                if (actualPresentTime >= oneSecondAgo) {
                    frameCount++
                }
            } catch (e: NumberFormatException) {
            }
        }
        return frameCount
    }
}
