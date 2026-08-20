package com.ivarna.deviceinsight.ui.caliper

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** §4.9 — the grammar of numbers. */
object Fmt {
    fun bytes(v: Long): String {
        if (v < 1024) return "$v B"
        val kb = v / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.2f KB", kb)
        val mb = kb / 1024.0
        return if (mb < 1024) String.format(Locale.US, "%.2f MB", mb)
        else String.format(Locale.US, "%.2f GB", mb / 1024.0)
    }
    fun hz(khz: Long): String = when {
        khz >= 1_000_000 -> String.format(Locale.US, "%.2f GHz", khz / 1e6)
        khz >= 1_000     -> String.format(Locale.US, "%.0f MHz", khz / 1e3)
        else -> "$khz kHz"
    }
    fun pct(v: Float, decimals: Int = 0) = String.format(Locale.US, "%.${decimals}f%%", v)
    fun temp(v: Float) = String.format(Locale.US, "%.1f°C", v)
    fun rate(bytesPerSec: Long) = bytes(bytesPerSec) + "/s"
    fun watts(v: Float) = String.format(Locale.US, "≈ %.2f W", v)
    fun duration(ms: Long): String {
        val h = ms / 3_600_000; val m = (ms % 3_600_000) / 60_000
        return String.format(Locale.US, "%dh %02dm", h, m)
    }
    fun index(n: Int) = String.format(Locale.US, "%04d", n)   // ledger row numbers
}

/** §4.8 — haptic vocabulary. */
class CaliperHaptics(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    private fun wave(timings: LongArray, amplitudes: IntArray) {
        try {
            vibrator?.takeIf { it.hasVibrator() }
                ?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (_: SecurityException) {
            // Permission denied by OEM/app-ops — haptics are non-critical; degrade silently.
        }
    }
    fun tick()    = wave(longArrayOf(0, 8), intArrayOf(0, 120))
    fun confirm() = wave(longArrayOf(0, 15, 20, 15), intArrayOf(0, 120, 120, 120))
    fun arm()     = wave(longArrayOf(0, 15, 15, 15, 15, 15), intArrayOf(0, 40, 40, 80, 80, 120))
    fun fault()   = wave(longArrayOf(0, 40), intArrayOf(0, 160))
    fun stamp()   = wave(longArrayOf(0, 12), intArrayOf(0, 140))
}

@Composable
fun rememberCaliperHaptics(): CaliperHaptics {
    val ctx = LocalContext.current
    return remember { CaliperHaptics(ctx) }
}