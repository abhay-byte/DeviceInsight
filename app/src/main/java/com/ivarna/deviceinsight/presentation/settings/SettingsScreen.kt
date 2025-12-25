package com.ivarna.deviceinsight.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.theme.AppTheme

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Appearance Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    AppTheme.values().forEach { theme ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .selectable(
                                    selected = (theme == currentTheme),
                                    onClick = { onThemeSelected(theme) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (theme == currentTheme),
                                onClick = null
                            )
                            Text(
                                text = formatThemeName(theme),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // About Section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Text(
                        text = "DeviceInsight",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                     Text(
                        text = "A premium system monitoring tool built with Jetpack Compose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatThemeName(theme: AppTheme): String {
    return when(theme) {
        AppTheme.TechNoir -> "Tech Noir"
        AppTheme.Cyberpunk -> "Cyberpunk Edge"
        AppTheme.DeepOcean -> "Deep Ocean"
        AppTheme.Matrix -> "Matrix"
        AppTheme.Dracula -> "Dracula"
    }
}
