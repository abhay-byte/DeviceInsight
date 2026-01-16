package com.ivarna.deviceinsight.data.fps

import android.content.Context
import android.os.Build
import android.util.Log
import com.ivarna.deviceinsight.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FpsMonitor @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private const val TAG = "FpsMonitor"
    }

    enum class AccessType {
        SHIZUKU, ROOT, NONE
    }

    private fun getAccessType(): AccessType {
        val prefs = context.getSharedPreferences("overlay_prefs", Context.MODE_PRIVATE)
        val mode = prefs.getString("fps_mode", "AUTO")

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
                AccessType.NONE // Shizuku exists but permission not granted
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

    /**
     * Executes a shell command using the best available method (Shizuku or Root).
     */
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
            // Use reflection to access Shizuku.newProcess to avoid hidden API restrictions/compilation issues
            // signature: public static Process newProcess(String[] cmd, String[] env, String dir)
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

    // Shizuku execution removed due to build limitations.
    // The previous implementation using bindUserService requires AIDL which failed to compile with current SDK tools.

    /**
     * Gets the current FPS.
     * Returns 0 if unable to calculate.
     */
    fun getCurrentFps(): Int {
        val packageName = getForegroundPackage()
        
        if (packageName == null) {
            Log.d(TAG, "getCurrentFps: No foreground package found")
            return 0
        }

        // Try gfxinfo first (works for most UI apps and many games)
        val fps = getGfxInfoFps(packageName)
        if (fps > 0) return fps

        // Fallback or legacy (not implemented fully reliable yet due to layer name issues)
        return 0
    }

    private fun getForegroundPackage(): String? {
        // Use 'dumpsys window' to find mFocusedApp
        // Output format: mFocusedApp=ActivityRecord{hash u0 package/activity token}
        val cmd = "dumpsys window | grep mFocusedApp"
        val output = executeCommand(cmd)
        
        val focusLine = output.firstOrNull { it.contains("mFocusedApp") } ?: return null
        
        // Regex to extract package name
        // Matches: ActivityRecord{... u0 com.package/
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

        // Parse framestats CSV
        // Look for header line to identify columns
        val headerIdx = output.indexOfFirst { it.startsWith("Flags,FrameTimelineVsyncId") }
        if (headerIdx == -1) return 0
        
        val header = output[headerIdx].split(",")
        val vsyncIdx = header.indexOf("IntendedVsync")
        
        if (vsyncIdx == -1) return 0
        
        var frameCount = 0
        val currentTime = System.nanoTime()
        val oneSecondAgo = currentTime - 1_000_000_000L
        
        // Data starts after header
        for (i in (headerIdx + 1) until output.size) {
            val line = output[i]
            if (line.isBlank() || line.startsWith("---PROFILEDATA---") || line.startsWith("View hierarchy:")) break
            
            val columns = line.split(",")
            if (columns.size > vsyncIdx) {
                val timestampStr = columns[vsyncIdx]
                try {
                    val timestamp = timestampStr.toLong()
                    // Gfxinfo timestamps are from CLOCK_MONOTONIC, same as System.nanoTime()
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
        
        // Line 1 is refresh period in ns (e.g. 16666666 for 60hz)
        // We don't strictly need it for counting frames in last second, but good to validate
        
        // Data lines: [Desired Present Time] [Actual Present Time] [Frame Ready Time]
        // We count lines where Actual Present Time is within the last 1 second (1,000,000,000 ns)
        
        val now = System.nanoTime()
        val oneSecondAgo = now - 1_000_000_000L
        
        var frameCount = 0
        
        // Skip first line (header)
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val parts = line.split("\\s+".toRegex())
            if (parts.size != 3) continue
            
            try {
                val actualPresentTime = parts[1].toLong()
                
                // Check if frame was presented (non-zero, not MAX_VALUE)
                if (actualPresentTime == 0L || actualPresentTime == Long.MAX_VALUE) continue
                
                // For 'dumpsys SurfaceFlinger --latency', the timestamps are typically monotonic.
                // However, they come from the SurfaceFlinger process. System.nanoTime() in our process 
                // should generally match the clock used by SF (CLOCK_MONOTONIC).
                
                if (actualPresentTime >= oneSecondAgo) {
                    frameCount++
                }
            } catch (e: NumberFormatException) {
                // Ignore malformed lines
            }
        }
        
        return frameCount
    }
}
