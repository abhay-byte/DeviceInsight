// P2-2 (part 2): legacy theme → nearest Medium mapping used once by the
// calibration migration MarginNote ("Your instrument has been recalibrated").
package com.ivarna.deviceinsight.ui.caliper

/** Maps a legacy AppTheme name to its nearest CALIPER medium.
 *  All legacy themes are dark → Carbon; PAPER is the light fallback. */
fun legacyThemeToMedium(legacyName: String): Medium = when (legacyName) {
    // Every legacy 10-theme entry is a dark scheme; CALIPER maps dark→Carbon.
    "TechNoir", "Cyberpunk", "DeepOcean", "Matrix", "Dracula",
    "SunsetMirage", "ForestSpirit", "NeonNights", "NordicIce", "GoldenLuxe" -> Medium.CARBON
    else -> Medium.PAPER
}