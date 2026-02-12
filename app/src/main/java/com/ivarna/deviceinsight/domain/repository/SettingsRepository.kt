package com.ivarna.deviceinsight.domain.repository

import com.ivarna.deviceinsight.presentation.theme.AppTheme
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getTheme(): Flow<AppTheme>
    suspend fun setTheme(theme: AppTheme)
}
