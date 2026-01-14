package com.ivarna.deviceinsight.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Theme Enum
enum class AppTheme {
    TechNoir,
    Cyberpunk,
    DeepOcean,
    Matrix,
    Dracula
}

// Tech Noir Scheme
private val TechNoirScheme = darkColorScheme(
    primary = TechNoirPrimary,
    secondary = TechNoirSecondary,
    tertiary = TechNoirTertiary,
    background = TechNoirBackground,
    surface = TechNoirSurface,
    error = TechNoirError
)

// Cyberpunk Scheme
private val CyberpunkScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    secondary = CyberpunkSecondary,
    tertiary = CyberpunkTertiary,
    background = CyberpunkBackground,
    surface = CyberpunkSurface,
    error = CyberpunkTertiary
)

// Ocean Scheme
private val OceanScheme = darkColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    error = TechNoirError
)

// Matrix Scheme
private val MatrixScheme = darkColorScheme(
    primary = MatrixPrimary,
    secondary = MatrixSecondary,
    tertiary = MatrixTertiary,
    background = MatrixBackground,
    surface = MatrixSurface,
    error = TechNoirError
)

// Dracula Scheme
private val DraculaScheme = darkColorScheme(
    primary = DraculaPrimary,
    secondary = DraculaSecondary,
    tertiary = DraculaTertiary,
    background = DraculaBackground,
    surface = DraculaSurface,
    error = TechNoirError
)

@Composable
fun SystemStatsTheme(
    theme: AppTheme = AppTheme.TechNoir, // Default to TechNoir
    content: @Composable () -> Unit
) {
    val colorScheme = when (theme) {
        AppTheme.TechNoir -> TechNoirScheme
        AppTheme.Cyberpunk -> CyberpunkScheme
        AppTheme.DeepOcean -> OceanScheme
        AppTheme.Matrix -> MatrixScheme
        AppTheme.Dracula -> DraculaScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
