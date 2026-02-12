package com.ivarna.deviceinsight.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.presentation.components.GlassCard
import com.ivarna.deviceinsight.presentation.theme.*

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
                                .height(64.dp)
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
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = formatThemeName(theme),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (theme == currentTheme) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Color Palette Preview
                            ThemePalettePreview(theme = theme)
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

@Composable
fun ThemePalettePreview(theme: AppTheme) {
    val colors = getThemeColors(theme)
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            )
        }
    }
}

fun getThemeColors(theme: AppTheme): List<Color> {
    return when(theme) {
        AppTheme.TechNoir -> listOf(TechNoirPrimary, TechNoirSecondary, TechNoirTertiary)
        AppTheme.Cyberpunk -> listOf(CyberpunkPrimary, CyberpunkSecondary, CyberpunkTertiary)
        AppTheme.DeepOcean -> listOf(OceanPrimary, OceanSecondary, OceanTertiary)
        AppTheme.Matrix -> listOf(MatrixPrimary, MatrixSecondary, MatrixTertiary)
        AppTheme.Dracula -> listOf(DraculaPrimary, DraculaSecondary, DraculaTertiary)
        AppTheme.SunsetMirage -> listOf(SunsetPrimary, SunsetSecondary, SunsetTertiary)
        AppTheme.ForestSpirit -> listOf(ForestPrimary, ForestSecondary, ForestTertiary)
        AppTheme.NeonNights -> listOf(NeonNightsPrimary, NeonNightsSecondary, NeonNightsTertiary)
        AppTheme.NordicIce -> listOf(NordicPrimary, NordicSecondary, NordicTertiary)
        AppTheme.GoldenLuxe -> listOf(LuxePrimary, LuxeSecondary, LuxeTertiary)
    }
}

fun formatThemeName(theme: AppTheme): String {
    return when(theme) {
        AppTheme.TechNoir -> "Tech Noir"
        AppTheme.Cyberpunk -> "Cyberpunk Edge"
        AppTheme.DeepOcean -> "Deep Ocean"
        AppTheme.Matrix -> "Matrix"
        AppTheme.Dracula -> "Dracula"
        AppTheme.SunsetMirage -> "Sunset Mirage"
        AppTheme.ForestSpirit -> "Forest Spirit"
        AppTheme.NeonNights -> "Neon Nights"
        AppTheme.NordicIce -> "Nordic Ice"
        AppTheme.GoldenLuxe -> "Golden Luxe"
    }
}
