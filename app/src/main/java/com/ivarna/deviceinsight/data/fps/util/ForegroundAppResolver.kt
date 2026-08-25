package com.ivarna.deviceinsight.data.fps.util

import android.content.Context
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegePolicy
import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier
import com.ivarna.deviceinsight.data.fps.privilege.ShellGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ForegroundApp(
    val packageName: String,
    val pid: Int,
    val refreshRateHz: Float
)

@Singleton
class ForegroundAppResolver @Inject constructor(
    private val shellGateway: ShellGateway,
    @ApplicationContext private val appContext: Context
) {
    fun resolve(): ForegroundApp? {
        // Prefer fg_app daemon file if root available? DeviceInsight daemon not ported yet, so skip
        readFromDumpsys()?.let { return it }
        return readFromActivityDumpsys()
    }

    fun isGameLikeSurface(packageName: String): Boolean {
        if (KNOWN_GAME_PACKAGES.any { packageName.startsWith(it) || packageName.contains(it) }) return true
        if (hasGameLayer(packageName)) return true
        return hasGameFocusedActivity(packageName)
    }

    private fun hasGameLayer(packageName: String): Boolean {
        val (result, _) = shellGateway.executePolicy("dumpsys SurfaceFlinger --list 2>/dev/null")
        if (!result.isSuccess || result.output.isBlank()) return false
        val shortPkg = packageName.substringAfterLast('.')
        return result.output.lineSequence().any { line ->
            val trimmed = line.trim()
            val ownsLayer = trimmed.contains(packageName) || (shortPkg.length >= 4 && trimmed.contains(shortPkg))
            ownsLayer && GAME_MARKERS.any { marker -> trimmed.contains(marker, ignoreCase = true) }
        }
    }

    private fun hasGameFocusedActivity(packageName: String): Boolean {
        val (result, _) = shellGateway.executePolicy("dumpsys window 2>/dev/null | grep mCurrentFocus")
        if (!result.isSuccess || result.output.isBlank()) return false
        val line = result.output
        if (!line.contains(packageName) && !line.contains(packageName.substringAfterLast('.'))) return false
        return GAME_MARKERS.any { marker -> line.contains(marker, ignoreCase = true) }
    }

    companion object {
        private val GAME_MARKERS = listOf(
            "SurfaceView",
            "NativeActivity",
            "Vulkan",
            "GLSurfaceView",
            "UnityPlayer",
            "Unreal",
            "Cocos2dx"
        )
        private val KNOWN_GAME_PACKAGES = listOf(
            "com.futuremark.",
            "com.antutu.",
            "com.primatelabs.",
            "com.benchmark.",
            "com.miHoYo.",
            "com.tencent.",
            "com.epicgames.",
            "com.unity3d.",
            "com.garena.",
            "com.activision.",
            "com.roblox.",
            "com.mojang."
        )
    }

    private fun readFromDumpsys(): ForegroundApp? {
        val (windowResult, _) = shellGateway.executePolicy(
            "dumpsys window 2>/dev/null | grep -E 'mCurrentFocus|mFocusedApp'"
        )
        if (!windowResult.isSuccess || windowResult.output.isBlank()) return null
        val packageName = windowResult.output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.contains("=null") }
            .mapNotNull { extractPackage(it) }
            .filter { it != appContext.packageName } // never measure self
            .firstOrNull() ?: return null
        val pid = pidOf(packageName)
        val refresh = readActiveRenderFrameRate()
        return ForegroundApp(packageName, pid, refresh)
    }

    private fun readFromActivityDumpsys(): ForegroundApp? {
        val (result, _) = shellGateway.executePolicy(
            "dumpsys activity activities 2>/dev/null | grep -E 'ResumedActivity|mResumedActivity' | head -5"
        )
        if (!result.isSuccess || result.output.isBlank()) return null
        val packageName = result.output.lineSequence()
            .mapNotNull { extractPackage(it) }
            .filter { it != appContext.packageName }
            .firstOrNull() ?: return null
        val pid = pidOf(packageName)
        val refresh = readActiveRenderFrameRate()
        return ForegroundApp(packageName, pid, refresh)
    }

    private fun pidOf(packageName: String): Int {
        val (result, _) = shellGateway.executePolicy("pidof $packageName 2>/dev/null | awk '{print \$1}'")
        return result.output.trim().toIntOrNull() ?: 0
    }

    private fun readActiveRenderFrameRate(): Float {
        // Prefer WindowManager
        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            val display = if (android.os.Build.VERSION.SDK_INT >= 30) {
                appContext.display
            } else {
                @Suppress("DEPRECATION") wm?.defaultDisplay
            }
            val rate = display?.refreshRate
            if (rate != null && rate > 0f) return rate
            val wmRate = wm?.defaultDisplay?.refreshRate
            if (wmRate != null && wmRate > 0f) return wmRate
        } catch (_: Throwable) {}
        val (displayResult, _) = shellGateway.executePolicy("dumpsys display 2>/dev/null")
        if (displayResult.isSuccess && displayResult.output.isNotBlank()) {
            val activeRate = Regex("""mActiveRenderFrameRate=([0-9.]+)""").find(displayResult.output)?.groupValues?.get(1)?.toFloatOrNull()
            if (activeRate != null && activeRate > 0f) return activeRate
            val renderRate = Regex("""renderFrameRate ([0-9.]+)""").find(displayResult.output)?.groupValues?.get(1)?.toFloatOrNull()
            if (renderRate != null && renderRate > 0f) return renderRate
        }
        return 60f
    }

    internal fun extractPackage(line: String): String? {
        fun sanitize(candidate: String?): String? {
            if (candidate == null) return null
            val c = candidate.trim()
            if (c.isEmpty() || c.equals("null", ignoreCase = true)) return null
            return c
        }
        val u0Match = Regex("""u0\s+([^/\s}]+)""").find(line)
        if (u0Match != null) {
            val cand = sanitize(u0Match.groupValues[1])
            if (cand != null) return cand
        }
        val braceMatch = Regex("""\{[^}]*\s+([^/\s}]+)/""").find(line)
        if (braceMatch != null) {
            val cand = sanitize(braceMatch.groupValues[1])
            if (cand != null) return cand
        }
        val slashIdx = line.indexOf('/')
        if (slashIdx > 0) {
            val beforeSlash = line.substring(0, slashIdx)
            val candidate = beforeSlash.substringAfterLast(' ').trim()
            if (candidate.contains('.')) {
                val cand = sanitize(candidate)
                if (cand != null) return cand
            }
        }
        return null
    }
}
