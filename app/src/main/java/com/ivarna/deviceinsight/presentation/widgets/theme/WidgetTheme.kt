package com.ivarna.deviceinsight.presentation.widgets.theme

import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object WidgetTheme {
    val colors = darkColorScheme(
        primary = Color(0xFF00E5FF), // TechNoir Primary
        secondary = Color(0xFFD500F9), // TechNoir Secondary
        tertiary = Color(0xFFFFD600), // TechNoir Amber
        surface = Color(0xFF121212),
        onSurface = Color.White,
        background = Color.Black
    )
    
    // Simple wrapper if we want to support dynamic colors later
    @androidx.compose.runtime.Composable
    fun WidgetTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
        androidx.glance.GlanceTheme(
            colors = androidx.glance.material3.ColorProviders(
                light = colors,
                dark = colors // Force dark for now to match app aesthetic
            ),
            content = content
        )
    }
}
