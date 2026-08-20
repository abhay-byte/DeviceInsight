// P2-3: single shared DataStore accessor + exact keys — all consumers
// (SettingsViewModel, MainActivity, MediaTileService, widgets) read this file.
package com.ivarna.deviceinsight.ui.caliper

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single accessor — never create a second delegate on this file (crash otherwise).
val Context.caliperDataStore by preferencesDataStore(name = "caliper")

object CaliperKeys {
    val medium = stringPreferencesKey("medium")
    val showGrid = booleanPreferencesKey("showGrid")
    val hatchingEnabled = booleanPreferencesKey("hatchingEnabled")
    val caliperMigrated = booleanPreferencesKey("caliperMigrated")
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