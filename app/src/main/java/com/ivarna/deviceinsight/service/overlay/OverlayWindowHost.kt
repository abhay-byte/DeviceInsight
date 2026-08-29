package com.ivarna.deviceinsight.service.overlay

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.ui.caliper.hud.HudRuntimeConfig
import java.util.function.Consumer

private const val TAG = "DeviceInsightOverlay"

interface OverlayWindowHost {
    val isAttached: Boolean
    val position: OverlayPosition
    val blurAvailable: Boolean

    fun attach(content: ComposeView, initial: HudRuntimeConfig)
    fun updatePosition(x: Int, y: Int)
    fun updateLocked(locked: Boolean)
    fun updateBackgroundBlur(enabled: Boolean, radiusPx: Int)
    fun updateContentLayout()
    fun detach()
}

data class OverlayPosition(val x: Int, val y: Int)
data class OverlayBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

object OverlayGeometry {
    fun clampPosition(requestedX: Int, requestedY: Int, usable: OverlayBounds, width: Int, height: Int): OverlayPosition {
        val maxX = (usable.right - width).coerceAtLeast(usable.left)
        val maxY = (usable.bottom - height).coerceAtLeast(usable.top)
        return OverlayPosition(
            requestedX.coerceIn(usable.left, maxX),
            requestedY.coerceIn(usable.top, maxY)
        )
    }
}

private fun copyAttributes(
    source: WindowManager.LayoutParams,
    width: Int = source.width,
    height: Int = source.height,
    format: Int = source.format,
    gravity: Int = source.gravity,
    x: Int = source.x,
    y: Int = source.y,
    flags: Int = source.flags
): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
    copyFrom(source)
    this.width = width
    this.height = height
    this.format = format
    this.gravity = gravity
    this.x = x
    this.y = y
    this.flags = flags
}

abstract class BaseOverlayWindowHost(
    protected val context: Context,
    protected val windowManager: WindowManager,
    private val onBlurAvailabilityChanged: (Boolean) -> Unit
) : OverlayWindowHost {
    protected var content: ComposeView? = null
    protected var attached = false
    protected var currentPosition = OverlayPosition(0, 0)

    override val isAttached: Boolean get() = attached
    override val position: OverlayPosition get() = currentPosition.copy()

    protected fun reportBlurAvailability(available: Boolean) {
        onBlurAvailabilityChanged(available)
    }

    protected fun usableFrame(decor: View? = null): OverlayBounds {
        val rawBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            android.graphics.Rect().also { windowManager.defaultDisplay.getRectSize(it) }
        }
        val bounds = OverlayBounds(rawBounds.left, rawBounds.top, rawBounds.right, rawBounds.bottom)
        if (decor == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return bounds
        val insets = decor.rootWindowInsets?.getInsets(android.view.WindowInsets.Type.systemBars())
            ?: return bounds
        return OverlayBounds(
            bounds.left + insets.left,
            bounds.top + insets.top,
            bounds.right - insets.right,
            bounds.bottom - insets.bottom
        ).also { if (it.right <= it.left || it.bottom <= it.top) return bounds }
    }

    protected fun measureContent(decor: View? = null): Pair<Int, Int> {
        val view = requireNotNull(content)
        val frame = usableFrame(decor)
        view.measure(
            View.MeasureSpec.makeMeasureSpec(frame.width.coerceAtLeast(1), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(frame.height.coerceAtLeast(1), View.MeasureSpec.AT_MOST)
        )
        val width = view.measuredWidth.coerceAtLeast(1)
        val height = view.measuredHeight.coerceAtLeast(1)
        view.layout(0, 0, width, height)
        return width to height
    }

    protected fun clampToContent(x: Int, y: Int, decor: View? = null): OverlayPosition {
        val view = requireNotNull(content)
        val frame = usableFrame(decor)
        return OverlayGeometry.clampPosition(
            x, y, frame, view.measuredWidth.coerceAtLeast(1), view.measuredHeight.coerceAtLeast(1)
        )
    }

    protected fun logUpdateFailure(operation: String, throwable: Throwable) {
        Log.e(TAG, "$operation failed (${throwable::class.java.simpleName}: ${throwable.message})", throwable)
    }

}

@RequiresApi(Build.VERSION_CODES.S)
class DialogOverlayWindowHost(
    context: Context,
    windowManager: WindowManager,
    onBlurAvailabilityChanged: (Boolean) -> Unit
) : BaseOverlayWindowHost(context, windowManager, onBlurAvailabilityChanged) {
    private var dialog: Dialog? = null
    private var blurListener: Consumer<Boolean>? = null
    private var crossWindowBlurEnabled = false
    private var backgroundBlurRequested = false
    private var backgroundBlurRadiusPx = 0

    override val blurAvailable: Boolean get() = crossWindowBlurEnabled

    override fun attach(content: ComposeView, initial: HudRuntimeConfig) {
        check(!attached) { "overlay dialog already attached" }
        Log.d(TAG, "HOST_ATTACH_BEGIN host=dialog api=${Build.VERSION.SDK_INT} config=$initial")
        val overlayDialog = Dialog(android.view.ContextThemeWrapper(context, R.style.Theme_DeviceInsight_OverlayWindow))
        overlayDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = requireNotNull(overlayDialog.window) { "dialog window unavailable" }
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0f)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        window.attributes = copyAttributes(
            window.attributes,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            format = android.graphics.PixelFormat.TRANSLUCENT,
            gravity = Gravity.TOP or Gravity.START,
            x = initial.x,
            y = initial.y
        )
        overlayDialog.setCanceledOnTouchOutside(false)
        overlayDialog.setCancelable(false)
        content.visibility = View.INVISIBLE
        currentPosition = OverlayPosition(initial.x, initial.y)
        overlayDialog.setContentView(content)
        overlayDialog.show()

        val shownWindow = requireNotNull(overlayDialog.window) { "dialog window disappeared" }
        shownWindow.decorView.visibility = View.INVISIBLE
        attached = true
        this.content = content
        dialog = overlayDialog
        registerBlurListener()
        // Compose resolves its parent WindowRecomposer during measurement. The dialog has
        // to be attached first; measuring here would fail before the ViewRoot exists.
        shownWindow.decorView.post {
            if (!attached) return@post
            try {
                measureAndApply(initial.x, initial.y)
                shownWindow.decorView.visibility = View.VISIBLE
                content.visibility = View.VISIBLE
                Log.d(TAG, "HOST_LAYOUT_OK host=dialog position=${currentPosition.x},${currentPosition.y} size=${content.measuredWidth}x${content.measuredHeight}")
            } catch (t: Throwable) {
                logUpdateFailure("HOST_INITIAL_LAYOUT", t)
                shownWindow.decorView.visibility = View.VISIBLE
                content.visibility = View.VISIBLE
            }
        }
        Log.d(TAG, "HOST_ATTACH_OK host=dialog position=${currentPosition.x},${currentPosition.y}")
    }

    private fun registerBlurListener() {
        crossWindowBlurEnabled = try {
            windowManager.isCrossWindowBlurEnabled
        } catch (t: Throwable) {
            logUpdateFailure("BLUR_CAPABILITY_READ", t)
            false
        }
        reportBlurAvailability(crossWindowBlurEnabled)
        val listener = Consumer<Boolean> { enabled ->
            crossWindowBlurEnabled = enabled
            Log.d(TAG, "BLUR_CAPABILITY enabled=$enabled")
            val current = dialog?.window ?: return@Consumer
            try {
                current.setBackgroundBlurRadius(if (backgroundBlurRequested && enabled) backgroundBlurRadiusPx else 0)
            } catch (t: Throwable) {
                logUpdateFailure("BLUR_RESET", t)
                crossWindowBlurEnabled = false
                reportBlurAvailability(false)
            }
            if (crossWindowBlurEnabled) reportBlurAvailability(enabled)
        }
        try {
            windowManager.addCrossWindowBlurEnabledListener(listener)
            blurListener = listener
            Log.d(TAG, "BLUR_LISTENER_REGISTERED")
        } catch (t: Throwable) {
            logUpdateFailure("BLUR_LISTENER_REGISTER", t)
            crossWindowBlurEnabled = false
            reportBlurAvailability(false)
        }
    }

    private fun measureAndApply(requestedX: Int, requestedY: Int) {
        val d = requireNotNull(dialog)
        val window = requireNotNull(d.window)
        val (width, height) = measureContent(window.decorView)
        currentPosition = clampToContent(requestedX, requestedY, window.decorView)
        val attrs = copyAttributes(
            window.attributes,
            width = width,
            height = height,
            gravity = Gravity.TOP or Gravity.START,
            x = currentPosition.x,
            y = currentPosition.y
        )
        window.attributes = attrs
        window.setLayout(width, height)
    }

    override fun updatePosition(x: Int, y: Int) {
        if (!attached) return
        val window = dialog?.window ?: return
        try {
            currentPosition = clampToContent(x, y, window.decorView)
            window.attributes = copyAttributes(
                window.attributes,
                gravity = Gravity.TOP or Gravity.START,
                x = currentPosition.x,
                y = currentPosition.y
            )
        } catch (t: Throwable) {
            logUpdateFailure("POSITION_UPDATE", t)
        }
    }

    override fun updateLocked(locked: Boolean) {
        val window = dialog?.window ?: return
        try {
            val flags = if (locked) {
                window.attributes.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                window.attributes.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            }
            window.attributes = copyAttributes(window.attributes, flags = flags)
        } catch (t: Throwable) {
            logUpdateFailure("LOCK_UPDATE", t)
        }
    }

    override fun updateBackgroundBlur(enabled: Boolean, radiusPx: Int) {
        val window = dialog?.window ?: return
        backgroundBlurRequested = enabled
        backgroundBlurRadiusPx = radiusPx
        try {
            window.setBackgroundBlurRadius(if (enabled && crossWindowBlurEnabled) radiusPx else 0)
            Log.d(TAG, "BLUR_APPLY enabled=$enabled available=$crossWindowBlurEnabled radius=${if (enabled && crossWindowBlurEnabled) radiusPx else 0}")
        } catch (t: Throwable) {
            logUpdateFailure("BLUR_APPLY", t)
            crossWindowBlurEnabled = false
            reportBlurAvailability(false)
            try { window.setBackgroundBlurRadius(0) } catch (reset: Throwable) { logUpdateFailure("BLUR_RESET", reset) }
        }
    }

    override fun updateContentLayout() {
        if (!attached) return
        val d = dialog ?: return
        d.window?.decorView?.post {
            if (attached) {
                try { measureAndApply(currentPosition.x, currentPosition.y) }
                catch (t: Throwable) { logUpdateFailure("CONTENT_RELAYOUT", t) }
            }
        }
    }

    override fun detach() {
        if (!attached && dialog == null) return
        attached = false
        blurListener?.let {
            try { windowManager.removeCrossWindowBlurEnabledListener(it) }
            catch (t: Throwable) { logUpdateFailure("BLUR_LISTENER_REMOVE", t) }
        }
        blurListener = null
        dialog?.window?.let {
            try { it.setBackgroundBlurRadius(0) } catch (t: Throwable) { logUpdateFailure("BLUR_RESET", t) }
        }
        try { dialog?.dismiss() } catch (t: Throwable) { logUpdateFailure("HOST_DISMISS", t) }
        dialog = null
        content = null
        Log.d(TAG, "HOST_DETACHED host=dialog")
    }
}

class RawOverlayWindowHost(
    context: Context,
    windowManager: WindowManager,
    onBlurAvailabilityChanged: (Boolean) -> Unit
) : BaseOverlayWindowHost(context, windowManager, onBlurAvailabilityChanged) {
    private var params: WindowManager.LayoutParams? = null

    override val blurAvailable: Boolean get() = false

    override fun attach(content: ComposeView, initial: HudRuntimeConfig) {
        check(!attached) { "raw overlay already attached" }
        Log.d(TAG, "HOST_ATTACH_BEGIN host=raw api=${Build.VERSION.SDK_INT} config=$initial")
        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initial.x
            y = initial.y
        }
        content.visibility = View.INVISIBLE
        currentPosition = OverlayPosition(initial.x, initial.y)
        windowManager.addView(content, p)
        this.content = content
        this.params = p
        attached = true
        reportBlurAvailability(false)
        content.post {
            if (!attached) return@post
            try {
                measureContent()
                applyLayout(initial.x, initial.y)
                content.visibility = View.VISIBLE
                Log.d(TAG, "HOST_ATTACH_OK host=raw position=${currentPosition.x},${currentPosition.y} size=${content.measuredWidth}x${content.measuredHeight}")
            } catch (t: Throwable) {
                logUpdateFailure("HOST_INITIAL_LAYOUT", t)
                content.visibility = View.VISIBLE
            }
        }
    }

    private fun applyLayout(x: Int, y: Int) {
        val view = requireNotNull(content)
        val p = requireNotNull(params)
        currentPosition = clampToContent(x, y)
        p.x = currentPosition.x
        p.y = currentPosition.y
        windowManager.updateViewLayout(view, p)
    }

    override fun updatePosition(x: Int, y: Int) {
        if (!attached) return
        try { applyLayout(x, y) } catch (t: Throwable) { logUpdateFailure("POSITION_UPDATE", t) }
    }

    override fun updateLocked(locked: Boolean) {
        if (!attached) return
        val view = content ?: return
        val p = params ?: return
        try {
            p.flags = if (locked) p.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            else p.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
            windowManager.updateViewLayout(view, p)
        } catch (t: Throwable) { logUpdateFailure("LOCK_UPDATE", t) }
    }

    override fun updateBackgroundBlur(enabled: Boolean, radiusPx: Int) {
        // Raw compatibility host deliberately has no cross-window blur implementation.
        reportBlurAvailability(false)
    }

    override fun updateContentLayout() {
        val view = content ?: return
        view.post {
            if (attached) {
                try {
                    measureContent()
                    applyLayout(currentPosition.x, currentPosition.y)
                } catch (t: Throwable) { logUpdateFailure("CONTENT_RELAYOUT", t) }
            }
        }
    }

    override fun detach() {
        if (!attached && content == null) return
        attached = false
        content?.let {
            try { windowManager.removeViewImmediate(it) }
            catch (t: Throwable) { logUpdateFailure("HOST_REMOVE", t) }
        }
        content = null
        params = null
        Log.d(TAG, "HOST_DETACHED host=raw")
    }
}
