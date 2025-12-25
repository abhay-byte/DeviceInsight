package com.ivarna.deviceinsight.presentation.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.ivarna.deviceinsight.presentation.theme.AppTheme
import com.ivarna.deviceinsight.presentation.theme.SystemStatsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var currentTheme by remember { mutableStateOf(AppTheme.TechNoir) }
            
            SystemStatsTheme(theme = currentTheme) {
                SettingsActivityContent(
                    currentTheme = currentTheme,
                    onThemeSelected = { newTheme -> currentTheme = newTheme },
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsActivityContent(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onBackPressed: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            SettingsScreen(
                currentTheme = currentTheme,
                onThemeSelected = onThemeSelected
            )
        }
    }
}
