package com.ivarna.deviceinsight.domain.model

data class CameraInfo(
    val id: String,
    val facing: String, // Rear, Front, External
    val resolution: String, // e.g. "64 MP (9248 × 6944)"
    val videoResolution: String, // e.g. "8.3 MP (3840 × 2160)"
    val focalLength: String, // e.g. "4.71 mm"
    val focusModes: List<String>,
    val videoSnapshotSupported: Boolean,
    val videoStabilizationSupported: Boolean,
    val zoomSupported: Boolean,
    val smoothZoomSupported: Boolean,
    val autoExposureLockingSupported: Boolean,
    val autoWhiteBalanceLockingSupported: Boolean,
    val flashSupported: Boolean
)
