package com.ivarna.deviceinsight.ui.caliper.hud

import com.ivarna.deviceinsight.data.monitor.CoreStat
import com.ivarna.deviceinsight.data.monitor.HudFast
import com.ivarna.deviceinsight.data.monitor.HudSlow
import androidx.datastore.preferences.core.Preferences
import com.ivarna.deviceinsight.data.fps.model.FpsMode
import com.ivarna.deviceinsight.ui.caliper.CaliperKeys

enum class HudModule { FPS, CPU, MEMORY, POWER, GPU, NETWORK, TRACE }

enum class HudScale { S, M, L }

object HudDefaults {
    val medium = HudMedium.CARBON
    val scale = HudScale.M
    const val opacity = 0.75f
    const val backgroundBlurEnabled = true
    const val locked = false
    val modules = setOf(HudModule.FPS, HudModule.CPU, HudModule.MEMORY, HudModule.POWER, HudModule.NETWORK)
    const val showCoreBank = true
    const val x = 100
    const val y = 100
    val fpsMode = FpsMode.AUTO

    fun modulesCsv(): String = modules.joinToString(",") { it.name }
}

data class HudConfig(
    val medium: HudMedium = HudDefaults.medium,
    val scale: HudScale = HudDefaults.scale,
    val opacity: Float = HudDefaults.opacity, // 0.4–0.9
    val backgroundBlurEnabled: Boolean = HudDefaults.backgroundBlurEnabled,
    val locked: Boolean = HudDefaults.locked,
    val modules: Set<HudModule> = HudDefaults.modules,
    val showCoreBank: Boolean = HudDefaults.showCoreBank
) {
    fun modulesCsv(): String = modules.joinToString(",") { it.name }
    companion object {
        fun fromCsv(csv: String): Set<HudModule> = csv.split(",").mapNotNull {
            runCatching { HudModule.valueOf(it.trim().uppercase()) }.getOrNull()
        }.toSet().ifEmpty { HudDefaults.modules }
    }
}

data class HudRuntimeConfig(
    val panel: HudConfig = HudConfig(),
    val x: Int = HudDefaults.x,
    val y: Int = HudDefaults.y,
    val fpsMode: FpsMode = HudDefaults.fpsMode
)

object HudConfigCodec {
    fun fromPreferences(prefs: Preferences): HudRuntimeConfig {
        val opacity = prefs[CaliperKeys.hudOpacity]
            ?.takeIf { it.isFinite() }
            ?.coerceIn(0.4f, 0.9f)
            ?: HudDefaults.opacity
        val panel = HudConfig(
            medium = hudMediumFromString(prefs[CaliperKeys.hudMedium]),
            scale = prefs[CaliperKeys.hudScale].let { value ->
                runCatching { value?.let(HudScale::valueOf) ?: HudDefaults.scale }
                    .getOrDefault(HudDefaults.scale)
            },
            opacity = opacity,
            backgroundBlurEnabled = prefs[CaliperKeys.hudBlur] ?: HudDefaults.backgroundBlurEnabled,
            locked = prefs[CaliperKeys.hudLocked] ?: HudDefaults.locked,
            modules = HudConfig.fromCsv(prefs[CaliperKeys.hudModules] ?: HudDefaults.modulesCsv()),
            showCoreBank = prefs[CaliperKeys.hudShowCoreBank] ?: HudDefaults.showCoreBank
        )
        return HudRuntimeConfig(
            panel = panel,
            x = prefs[CaliperKeys.hudX] ?: HudDefaults.x,
            y = prefs[CaliperKeys.hudY] ?: HudDefaults.y,
            fpsMode = FpsMode.fromPersisted(prefs[CaliperKeys.fpsMode])
        )
    }
}

fun FpsMode.Companion.fromPersisted(value: String?): FpsMode =
    FpsMode.entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HudDefaults.fpsMode

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
