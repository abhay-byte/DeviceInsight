package com.ivarna.deviceinsight.data.repository

import android.content.Context
import com.ivarna.deviceinsight.domain.repository.SettingsRepository
import com.ivarna.deviceinsight.ui.caliper.Medium
import com.ivarna.deviceinsight.ui.caliper.mediumFlow
import com.ivarna.deviceinsight.ui.caliper.setMedium
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    // P2-2: medium moved to the single "caliper" DataStore. The legacy
    // "settings_prefs" SharedPreferences stays for non-theme keys.
    override fun getMedium(): Flow<Medium?> = context.mediumFlow

    override suspend fun setMedium(medium: Medium) = context.setMedium(medium)
}