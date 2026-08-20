package com.ivarna.deviceinsight.presentation.settings

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ivarna.deviceinsight.presentation.theme.SystemStatsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Thin wrapper around the №05 SETTINGS Compose route (intent compatibility).
 * The primary entry is the in-app NavHost destination reached from the
 * Masthead gear HardKey.
 */
@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableHighRefreshRate()

        setContent {
            val currentMedium by viewModel.medium.collectAsStateWithLifecycle()

            SystemStatsTheme(medium = currentMedium) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        currentMedium = currentMedium,
                        onMediumSelected = { newMedium -> viewModel.setMedium(newMedium) }
                    )
                }
            }
        }
    }

    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val supportedModes = display?.supportedModes
            val maxMode = supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null && maxMode.refreshRate >= 90f) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            val supportedModes = windowManager.defaultDisplay.supportedModes
            val maxMode = supportedModes?.maxByOrNull { it.refreshRate }
            if (maxMode != null && maxMode.refreshRate >= 90f) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        }
    }
}