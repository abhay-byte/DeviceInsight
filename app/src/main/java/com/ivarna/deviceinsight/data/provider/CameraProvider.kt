package com.ivarna.deviceinsight.data.provider

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import com.ivarna.deviceinsight.domain.model.CameraInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class CameraProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    @Volatile
    private var cachedCameras: List<CameraInfo>? = null

    fun getCameraInfo(): List<CameraInfo> {
        // CALIPER: camera roster is gated — never trigger a system popup from the provider.
        // If not granted, return empty and let the UI render CHANNEL LOCKED hatch.
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        cachedCameras?.let { return it }
        val cameras = mutableListOf<CameraInfo>()
        // Deduplicate HAL duplicates: same sensor exposed multiple times with identical
        // facing + max JPEG size + focal length appears 5× on RMX1931 (4608×3456).
        // Keep first of each unique physical module; filter tiny auxiliary depth/macro.
        val seen = mutableSetOf<String>()
        val rearCounter = mutableMapOf<Int, Int>() // Track index per facing
        
        try {
            val ids = cameraManager.cameraIdList
            val facingCounts = mutableMapOf<String, Int>()

            for (id in ids) {
                val chars = cameraManager.getCameraCharacteristics(id)

                val configs = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val jpegSizes = configs?.getOutputSizes(ImageFormat.JPEG)
                
                // Skip cameras that don't support JPEG (internal/logical-only often don't)
                if (jpegSizes.isNullOrEmpty()) continue
                val maxJpeg = jpegSizes.maxByOrNull { it.width * it.height } ?: continue
                val maxArea = maxJpeg.width * maxJpeg.height
                // Filter tiny auxiliary (depth/macro) <2MP on back — user-visible expects 3+1, not 10
                // Front is kept even if small; back tiny is usually depth (1.9MP on RMX1931)
                val rawFacing = chars.get(CameraCharacteristics.LENS_FACING)
                val isBack = rawFacing == CameraCharacteristics.LENS_FACING_BACK
                if (isBack && maxArea < 2_000_000) {
                    // still log for diagnostics but don't add to roster
                    android.util.Log.d("CameraProvider", "filter small back id=$id ${maxJpeg.width}x${maxJpeg.height}")
                    continue
                }

                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                // dedup key: facing + max size + first focal (physical module identity)
                val focalKey = focalLengths?.firstOrNull()?.let { String.format(java.util.Locale.US, "%.2f", it) } ?: "unk"
                val dedupKey = "${rawFacing ?: -1}|${maxJpeg.width}x${maxJpeg.height}|$focalKey"
                if (!seen.add(dedupKey)) {
                    android.util.Log.d("CameraProvider", "dedup $id key=$dedupKey")
                    continue
                }

                val facingType = when (rawFacing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front-Facing"
                    CameraCharacteristics.LENS_FACING_BACK -> "Rear-Facing"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                    else -> "Unknown"
                }

                // Increment counter only for kept cameras (post-dedup/filter)
                val currentCount = (facingCounts[facingType] ?: 0) + 1
                facingCounts[facingType] = currentCount
                
                val displayName = if (currentCount == 1) {
                    "$facingType Camera"
                } else {
                    "$facingType Camera #$currentCount"
                }

                val resolution = "${
                    (maxArea / 1_000_000f).roundToInt()
                } MP (${maxJpeg.width} × ${maxJpeg.height})"

                val videoResolution = configs?.getOutputSizes(android.graphics.SurfaceTexture::class.java)?.maxByOrNull { it.width * it.height }?.let {
                    val mp = (it.width * it.height / 1_000_000f).let { res -> "%.1f".format(res) }
                    "$mp MP (${it.width} × ${it.height})"
                } ?: "Unknown"

                val focalLengthStr = focalLengths?.joinToString(", ") { "%.2f mm".format(it) } ?: "Unknown"

                val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                val focusModes = afModes?.map {
                    when (it) {
                        CameraCharacteristics.CONTROL_AF_MODE_AUTO -> "auto"
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "continuous-picture"
                        CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "continuous-video"
                        CameraCharacteristics.CONTROL_AF_MODE_EDOF -> "edof"
                        CameraCharacteristics.CONTROL_AF_MODE_MACRO -> "macro"
                        CameraCharacteristics.CONTROL_AF_MODE_OFF -> "fixed/off"
                        else -> "unknown"
                    }
                }?.distinct() ?: emptyList()

                val videoSnapshotSupported = true // Most modern cameras support this
                val videoStabilizationModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                val videoStabilizationSupported = videoStabilizationModes?.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF } ?: false

                val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
                val zoomSupported = maxZoom > 1f
                val smoothZoomSupported = false // Legacy Camera API feature, not directly in Camera2 characteristics

                val aeLockSupported = chars.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) ?: false
                val awbLockSupported = chars.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) ?: false
                val flashSupported = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                cameras.add(
                    CameraInfo(
                        id = id,
                        facing = displayName,
                        resolution = resolution,
                        videoResolution = videoResolution,
                        focalLength = focalLengthStr,
                        focusModes = focusModes,
                        videoSnapshotSupported = videoSnapshotSupported,
                        videoStabilizationSupported = videoStabilizationSupported,
                        zoomSupported = zoomSupported,
                        smoothZoomSupported = smoothZoomSupported,
                        autoExposureLockingSupported = aeLockSupported,
                        autoWhiteBalanceLockingSupported = awbLockSupported,
                        flashSupported = flashSupported
                    )
                )
            }
            if (cameras.isNotEmpty()) {
                cachedCameras = cameras
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cameras
    }
}
