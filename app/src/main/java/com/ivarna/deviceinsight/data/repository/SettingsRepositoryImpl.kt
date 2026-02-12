package com.ivarna.deviceinsight.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.ivarna.deviceinsight.domain.repository.SettingsRepository
import com.ivarna.deviceinsight.presentation.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    override fun getTheme(): Flow<AppTheme> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "app_theme") {
                val themeName = sharedPreferences.getString(key, AppTheme.TechNoir.name)
                val theme = try {
                    AppTheme.valueOf(themeName ?: AppTheme.TechNoir.name)
                } catch (e: Exception) {
                    AppTheme.TechNoir
                }
                trySend(theme)
            }
        }
        
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        // Initial value
        val initialThemeName = prefs.getString("app_theme", AppTheme.TechNoir.name)
        val initialTheme = try {
            AppTheme.valueOf(initialThemeName ?: AppTheme.TechNoir.name)
        } catch (e: Exception) {
            AppTheme.TechNoir
        }
        trySend(initialTheme)
        
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).commit()
    }
}
