package com.ivarna.deviceinsight.domain.repository

import com.ivarna.deviceinsight.ui.caliper.Medium
import kotlinx.coroutines.flow.Flow

/** Settings repository — CALIPER medium pinned to the single "caliper" DataStore. */
interface SettingsRepository {
    fun getMedium(): Flow<Medium?>
    suspend fun setMedium(medium: Medium)
}