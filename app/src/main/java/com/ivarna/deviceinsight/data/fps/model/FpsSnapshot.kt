package com.ivarna.deviceinsight.data.fps.model

import com.ivarna.deviceinsight.data.fps.privilege.PrivilegeTier

/**
 * Typed FPS sample — distinguishes valid signal from no-signal and carries
 * provenance for honest source labeling (SF/GFX/DMA vs —).
 */
data class FpsSnapshot(
    val currentFps: Float,
    val frametimeAvgMs: Float = 0f,
    val frametimeP1Ms: Float = 0f,
    val frametimeP01Ms: Float = 0f,
    val frametimes: List<Float> = emptyList(),
    val jankCount: Int = 0,
    val method: FpsMethod = FpsMethod.NONE,
    val access: PrivilegeTier? = null,
    val packageName: String? = null,
    val isStale: Boolean = false
) {
    companion object {
        val ZERO = FpsSnapshot(
            currentFps = 0f,
            method = FpsMethod.NONE
        )
    }

    fun isValid(): Boolean = currentFps > 0f && method != FpsMethod.NONE && !isStale
}

enum class FpsMethod {
    DMA_FENCE,
    SURFACEFLINGER,
    GFXINFO,
    DISPLAY,
    NONE
}

fun FpsMethod.label(): String = when (this) {
    FpsMethod.DMA_FENCE -> "DMA"
    FpsMethod.SURFACEFLINGER -> "SF"
    FpsMethod.GFXINFO -> "GFX"
    FpsMethod.DISPLAY -> "REF"
    FpsMethod.NONE -> "—"
}
