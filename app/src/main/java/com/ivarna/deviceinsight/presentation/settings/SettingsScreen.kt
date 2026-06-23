package com.ivarna.deviceinsight.presentation.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivarna.deviceinsight.presentation.theme.*

@Composable
fun SettingsScreen(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Appearance Section
        item {
            SettingsSectionHeader(title = "Appearance", icon = Icons.Filled.ColorLens)
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppTheme.values().forEach { theme ->
                        ThemeItem(
                            theme = theme,
                            isSelected = theme == currentTheme,
                            onClick = { onThemeSelected(theme) }
                        )
                    }
                }
            }
        }

        // About Section
        item {
            SettingsSectionHeader(title = "About", icon = Icons.Filled.Info)
            Spacer(modifier = Modifier.height(12.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "DeviceInsight",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "A premium system monitoring tool built with Jetpack Compose.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(primary, secondary))
                )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp
            ),
            color = primary
        )
    }
}

@Composable
fun ThemeItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) primary.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(300),
        label = "themeBgColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) primary.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(300),
        label = "themeBorderColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = formatThemeName(theme),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
            )
        }

        ThemePalettePreview(theme = theme)
    }
}

@Composable
fun ThemePalettePreview(theme: AppTheme) {
    val colors = getThemeColors(theme)
    Row(
        horizontalArrangement = Arrangement.spacedBy(-6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.reversed().forEach { color ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
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
