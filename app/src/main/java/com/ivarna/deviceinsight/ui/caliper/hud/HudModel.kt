package com.ivarna.deviceinsight.ui.caliper.hud

import com.ivarna.deviceinsight.data.monitor.CoreStat
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow

enum class HudModule { FPS, CPU, MEMORY, POWER, GPU, NETWORK, TRACE }

enum class HudScale { S, M, L }

data class HudConfig(
    val medium: HudMedium = HudMedium.CARBON,
    val scale: HudScale = HudScale.M,
    val opacity: Float = 0.75f, // 0.4–0.9
    val blurBehind: Boolean = true,
    val locked: Boolean = false,
    val modules: Set<HudModule> = setOf(HudModule.FPS, HudModule.CPU, HudModule.MEMORY, HudModule.POWER),
    val showCoreBank: Boolean = true,
    val x: Int = 100,
    val y: Int = 100,
    val fpsMode: String = "AUTO" // AUTO / ROOT / SHIZUKU
) {
    fun modulesCsv(): String = modules.joinToString(",") { it.name }
    companion object {
        fun fromCsv(csv: String): Set<HudModule> = csv.split(",").mapNotNull {
            try { HudModule.valueOf(it.trim()) } catch (_: Exception) { null }
        }.toSet().ifEmpty { setOf(HudModule.FPS, HudModule.CPU, HudModule.MEMORY, HudModule.POWER) }
    }
}

// Re-export HudSlow/HudFast for HUD UI to import from single place
typealias HudSlowModel = HudSlow
typealias HudFastModel = HudFast

object FmtHud {
    fun wattsSigned(v: Float): String = com.ivarna.deviceinsight.ui.caliper.Fmt.wattsSigned(v)
    fun pct(v: Float, dec: Int = 0): String = com.ivarna.deviceinsight.ui.caliper.Fmt.pct(v, dec)
    fun temp(c: Float): String = String.format(java.util.Locale.US, "%.0f°C", c)
    fun ghz(khz: Int): String = when {
        khz >= 1_000_000 -> String.format(java.util.Locale.US, "%.2f GHz", khz / 1_000_000f)
        khz >= 1000 -> String.format(java.util.Locale.US, "%.0f MHz", khz / 1000f)
        else -> "$khz kHz"
    }
    fun mb(mb: Int): String = when {
        mb >= 1024 -> String.format(java.util.Locale.US, "%.1f GB", mb / 1024f)
        else -> "$mb MB"
    }
    fun rate(bytesPerSec: Long): String = com.ivarna.deviceinsight.ui.caliper.Fmt.rate(bytesPerSec)
    /** HH:mm:ss wall clock from a timestamp (— when unset). */
    fun clock(ts: Long): String =
        if (ts <= 0L) "--:--:--"
        else java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date(ts))
    /** Xh Ym remaining when >=60, N min remaining otherwise; null hides. */
    fun remaining(min: Int): String? = when {
        min <= 0 -> null
        min >= 60 -> String.format(java.util.Locale.US, "%dh %dm remaining", min / 60, min % 60)
        else -> "$min min remaining"
    }
}
