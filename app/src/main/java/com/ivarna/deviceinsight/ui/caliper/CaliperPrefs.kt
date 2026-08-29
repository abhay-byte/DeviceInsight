// P2-3: single shared DataStore accessor + exact keys — all consumers
// (SettingsViewModel, MainActivity, MediaTileService, widgets) read this file.
package com.ivarna.deviceinsight.ui.caliper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.ivarna.deviceinsight.ui.caliper.hud.HudConfigCodec
import com.ivarna.deviceinsight.ui.caliper.hud.HudDefaults
import com.ivarna.deviceinsight.ui.caliper.hud.HudRuntimeConfig
import com.ivarna.deviceinsight.data.fps.model.FpsMode

// Single accessor — never create a second delegate on this file (crash otherwise).
val Context.caliperDataStore by preferencesDataStore(name = "caliper")

object CaliperKeys {
    val medium = stringPreferencesKey("medium")
    val showGrid = booleanPreferencesKey("showGrid")
    val hatchingEnabled = booleanPreferencesKey("hatchingEnabled")
    val caliperMigrated = booleanPreferencesKey("caliperMigrated")
    // HUD — single source of truth (caliper only)
    val hudMedium = stringPreferencesKey("hudMedium")
    val hudScale = stringPreferencesKey("hudScale")
    val hudOpacity = floatPreferencesKey("hudOpacity")
    val hudBlur = booleanPreferencesKey("hudBlur")
    val hudLocked = booleanPreferencesKey("hudLocked")
    val hudModules = stringPreferencesKey("hudModules")
    val hudShowCoreBank = booleanPreferencesKey("hudShowCoreBank")
    val hudX = intPreferencesKey("hudX")
    val hudY = intPreferencesKey("hudY")
    val fpsMode = stringPreferencesKey("fpsMode")
    val hudMigrated = booleanPreferencesKey("hudMigrated")
}

/** Persisted medium; null means "follow system dark" (Paper on light, Carbon on dark). */
val Context.mediumFlow: Flow<Medium?>
    get() = caliperDataStore.data.map { prefs ->
        prefs[CaliperKeys.medium]?.let { runCatching { Medium.valueOf(it) }.getOrNull() }
    }

suspend fun Context.setMedium(medium: Medium) {
    caliperDataStore.edit { it[CaliperKeys.medium] = medium.name }
}

val Context.showGridFlow: Flow<Boolean>
    get() = caliperDataStore.data.map { it[CaliperKeys.showGrid] ?: true }

suspend fun Context.setShowGrid(show: Boolean) {
    caliperDataStore.edit { it[CaliperKeys.showGrid] = show }
}

val Context.hatchingEnabledFlow: Flow<Boolean>
    get() = caliperDataStore.data.map { it[CaliperKeys.hatchingEnabled] ?: true }

suspend fun Context.setHatchingEnabled(enabled: Boolean) {
    caliperDataStore.edit { it[CaliperKeys.hatchingEnabled] = enabled }
}

val Context.caliperMigratedFlow: Flow<Boolean>
    get() = caliperDataStore.data.map { it[CaliperKeys.caliperMigrated] ?: false }

suspend fun Context.markCaliperMigrated() {
    caliperDataStore.edit { it[CaliperKeys.caliperMigrated] = true }
}

// ── HUD flows (caliper single source) ──

val Context.hudRuntimeConfigFlow: Flow<HudRuntimeConfig>
    get() = caliperDataStore.data.map(HudConfigCodec::fromPreferences)

val Context.hudMediumFlow: Flow<String>
    get() = hudRuntimeConfigFlow.map { it.panel.medium.name }

val Context.hudScaleFlow: Flow<String>
    get() = hudRuntimeConfigFlow.map { it.panel.scale.name }

val Context.hudOpacityFlow: Flow<Float>
    get() = hudRuntimeConfigFlow.map { it.panel.opacity }

val Context.hudBlurFlow: Flow<Boolean>
    get() = hudRuntimeConfigFlow.map { it.panel.backgroundBlurEnabled }

val Context.hudLockedFlow: Flow<Boolean>
    get() = hudRuntimeConfigFlow.map { it.panel.locked }

val Context.hudModulesFlow: Flow<String>
    get() = hudRuntimeConfigFlow.map { it.panel.modulesCsv() }

val Context.hudShowCoreBankFlow: Flow<Boolean>
    get() = hudRuntimeConfigFlow.map { it.panel.showCoreBank }

val Context.hudXFlow: Flow<Int>
    get() = hudRuntimeConfigFlow.map { it.x }

val Context.hudYFlow: Flow<Int>
    get() = hudRuntimeConfigFlow.map { it.y }

val Context.hudFpsModeFlow: Flow<String>
    get() = hudRuntimeConfigFlow.map { it.fpsMode.name }

val Context.hudMigratedFlow: Flow<Boolean>
    get() = caliperDataStore.data.map { it[CaliperKeys.hudMigrated] ?: false }

suspend fun Context.setHudMedium(v: String) { caliperDataStore.edit { it[CaliperKeys.hudMedium] = v } }
suspend fun Context.setHudScale(v: String) { caliperDataStore.edit { it[CaliperKeys.hudScale] = v } }
suspend fun Context.setHudOpacity(v: Float) { caliperDataStore.edit { it[CaliperKeys.hudOpacity] = v.coerceIn(0.4f, 0.9f) } }
suspend fun Context.setHudBlur(v: Boolean) { caliperDataStore.edit { it[CaliperKeys.hudBlur] = v } }
suspend fun Context.setHudLocked(v: Boolean) { caliperDataStore.edit { it[CaliperKeys.hudLocked] = v } }
suspend fun Context.setHudModules(v: String) { caliperDataStore.edit { it[CaliperKeys.hudModules] = v } }
suspend fun Context.setHudShowCoreBank(v: Boolean) { caliperDataStore.edit { it[CaliperKeys.hudShowCoreBank] = v } }
suspend fun Context.setHudX(v: Int) { caliperDataStore.edit { it[CaliperKeys.hudX] = v } }
suspend fun Context.setHudY(v: Int) { caliperDataStore.edit { it[CaliperKeys.hudY] = v } }
suspend fun Context.setHudPosition(x: Int, y: Int) {
    caliperDataStore.edit {
        it[CaliperKeys.hudX] = x
        it[CaliperKeys.hudY] = y
    }
}
suspend fun Context.setFpsMode(v: String) { caliperDataStore.edit { it[CaliperKeys.fpsMode] = v } }
suspend fun Context.setFpsMode(v: FpsMode) { caliperDataStore.edit { it[CaliperKeys.fpsMode] = v.name } }
suspend fun Context.setHudMigrated(v: Boolean) { caliperDataStore.edit { it[CaliperKeys.hudMigrated] = v } }
