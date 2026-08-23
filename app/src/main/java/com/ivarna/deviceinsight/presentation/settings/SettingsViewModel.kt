package com.ivarna.deviceinsight.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.deviceinsight.domain.repository.SettingsRepository
import com.ivarna.deviceinsight.ui.caliper.LauncherAlias
import com.ivarna.deviceinsight.ui.caliper.Medium
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val medium: StateFlow<Medium?> = settingsRepository.getMedium()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setMedium(medium: Medium) {
        viewModelScope.launch {
            settingsRepository.setMedium(medium)
            // launcher icon follows in-app media — staged, applied on background
            // (disabling the running task's alias makes the system finish it = "crash")
            LauncherAlias.request(appContext, medium)
        }
    }
}