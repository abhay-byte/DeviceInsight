package com.ivarna.deviceinsight.presentation.theme

import androidx.compose.runtime.Composable
import com.ivarna.deviceinsight.ui.caliper.CaliperTheme
import com.ivarna.deviceinsight.ui.caliper.Medium

/**
 * CALIPER theme bridge. Exactly three media (Paper / Carbon / Blueprint).
 * The legacy 10-theme enum is gone; callers pass a [Medium].
 */
@Composable
fun SystemStatsTheme(
    medium: Medium? = null,
    content: @Composable () -> Unit
) {
    CaliperTheme(
        medium = medium ?: if (androidx.compose.foundation.isSystemInDarkTheme()) Medium.CARBON else Medium.PAPER,
        content = content
    )
}