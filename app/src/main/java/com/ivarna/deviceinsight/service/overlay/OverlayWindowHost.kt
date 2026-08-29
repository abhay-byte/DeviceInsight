package com.ivarna.deviceinsight.service.overlay

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.ComposeView
import com.ivarna.deviceinsight.R
import com.ivarna.deviceinsight.ui.caliper.hud.HudRuntimeConfig
import java.util.function.Consumer

private const val TAG = "DeviceInsightOverlay"
private const val EDGE_MARGIN_DP = 8

interface OverlayWindowHost {
    val isAttached: Boolean
    val position: OverlayPosition
    val size: OverlaySize
    val blurAvailable: Boolean

    fun attach(content: ComposeView, initial: HudRuntimeConfig)
    fun updatePosition(x: Int, y: Int)
    fun updateLocked(locked: Boolean)
    fun updateBackgroundBlur(enabled: Boolean, radiusPx: Int)
    fun requestContentLayout()
    fun detach()
}

data class OverlayPosition(val x: Int, val y: Int)
data class OverlaySize(val width: Int, val height: Int)
data class OverlayBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

data class OverlayInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

object OverlayGeometry {
    fun safeFrame(display: OverlayBounds, insets: OverlayInsets, edgeMarginPx: Int): OverlayBounds {
        val margin = edgeMarginPx.coerceAtLeast(0)
        val left = display.left + insets.left + margin
        val top = display.top + insets.top + margin
        val right = (display.right - insets.right - margin).coerceAtLeast(left)
        val bottom = (display.bottom - insets.bottom - margin).coerceAtLeast(top)
        return OverlayBounds(left, top, right, bottom)
    }

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
    private val onBlurAvailabilityChanged: (Boolean) -> Unit,
    private val onPositionAdjusted: (OverlayPosition) -> Unit
) : OverlayWindowHost {
    protected var content: ComposeView? = null
    protected var attached = false
    protected var currentPosition = OverlayPosition(0, 0)
    protected var currentSize = OverlaySize(0, 0)
    private var insetsView: View? = null
    private var firstLayoutComplete = false
    private var lastSafeFrame: OverlayBounds? = null

    override val isAttached: Boolean get() = attached
    override val position: OverlayPosition get() = currentPosition.copy()
    override val size: OverlaySize get() = currentSize.copy()

    protected fun reportBlurAvailability(available: Boolean) {
        onBlurAvailabilityChanged(available)
    }

    protected fun usableFrame(decor: View? = insetsView): OverlayBounds {
        val rawBounds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            @Suppress("DEPRECATION")
            android.graphics.Rect().also { windowManager.defaultDisplay.getRectSize(it) }
        }
        val display = OverlayBounds(rawBounds.left, rawBounds.top, rawBounds.right, rawBounds.bottom)
        val insets = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsets = windowManager.currentWindowMetrics.windowInsets
            @Suppress("NewApi")
            val systemInsets = windowInsets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            OverlayInsets(systemInsets.left, systemInsets.top, systemInsets.right, systemInsets.bottom)
        } else {
            @Suppress("DEPRECATION")
            val rootInsets = decor?.rootWindowInsets
            if (rootInsets == null) {
                OverlayInsets()
            } else {
                @Suppress("DEPRECATION")
                OverlayInsets(
                    rootInsets.systemWindowInsetLeft,
                    rootInsets.systemWindowInsetTop,
                    rootInsets.systemWindowInsetRight,
                    rootInsets.systemWindowInsetBottom
                )
            }
        }
        val marginPx = (context.resources.displayMetrics.density * EDGE_MARGIN_DP).toInt().coerceAtLeast(1)
        val safe = OverlayGeometry.safeFrame(display, insets, marginPx)
        if (safe != lastSafeFrame) {
            Log.d(TAG, "INSETS_CHANGED display=${display.left},${display.top},${display.right},${display.bottom} " +
                "insets=${insets.left},${insets.top},${insets.right},${insets.bottom} " +
                "edgeMarginPx=$marginPx frame=${safe.left},${safe.top},${safe.right},${safe.bottom}")
            lastSafeFrame = safe
        }
        return safe
    }

    protected fun observeContent(view: ComposeView, decor: View? = null) {
        content = view
        insetsView = decor
        currentSize = OverlaySize(0, 0)
        firstLayoutComplete = false
        view.addOnLayoutChangeListener(contentLayoutListener)
        decor?.addOnLayoutChangeListener(decorLayoutListener)
        decor?.setOnApplyWindowInsetsListener { _, insets ->
            if (attached) {
                Log.d(TAG, "INSETS_CHANGED delivered=${insets.systemWindowInsetLeft},${insets.systemWindowInsetTop}," +
                    "${insets.systemWindowInsetRight},${insets.systemWindowInsetBottom}")
                reclampCurrentPosition("insets")
            }
            insets
        }
    }

    protected fun stopObservingContent(decor: View? = insetsView) {
        content?.removeOnLayoutChangeListener(contentLayoutListener)
        decor?.removeOnLayoutChangeListener(decorLayoutListener)
        decor?.setOnApplyWindowInsetsListener(null)
        insetsView = null
    }

    protected fun onActualContentSizeChanged(width: Int, height: Int) {
        val newSize = OverlaySize(width.coerceAtLeast(1), height.coerceAtLeast(1))
        if (newSize == currentSize) return
        val oldSize = currentSize
        currentSize = newSize
        val frame = usableFrame()
        Log.d(TAG, "CONTENT_SIZE_CHANGED old=${oldSize.width}x${oldSize.height} " +
            "new=${newSize.width}x${newSize.height}")
        onContentSizeChanged(newSize)
        reclampCurrentPosition("content-size", frame)
        Log.d(TAG, "GEOMETRY_STABLE size=${newSize.width}x${newSize.height} " +
            "position=${currentPosition.x},${currentPosition.y} frame=${frame.left},${frame.top},${frame.right},${frame.bottom}")
        if (!firstLayoutComplete) {
            firstLayoutComplete = true
            onFirstLayoutComplete()
        }
    }

    protected fun reclampCurrentPosition(reason: String, frame: OverlayBounds = usableFrame()) {
        if (currentSize.width <= 0 || currentSize.height <= 0) return
        val requested = currentPosition
        val actual = OverlayGeometry.clampPosition(
            requested.x, requested.y, frame, currentSize.width, currentSize.height
        )
        Log.d(TAG, "POSITION_CLAMP reason=$reason requested=${requested.x},${requested.y} " +
            "actual=${actual.x},${actual.y} size=${currentSize.width}x${currentSize.height} " +
            "frame=${frame.left},${frame.top},${frame.right},${frame.bottom}")
        if (actual == requested) return
        try {
            applyPosition(actual)
            currentPosition = actual
            onPositionAdjusted(actual)
        } catch (t: Throwable) {
            logUpdateFailure("POSITION_CLAMP_UPDATE", t)
        }
    }

    private val contentLayoutListener = View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        val width = right - left
        val height = bottom - top
        val oldWidth = oldRight - oldLeft
        val oldHeight = oldBottom - oldTop
        if (width > 0 && height > 0 && (width != oldWidth || height != oldHeight)) {
            onActualContentSizeChanged(width, height)
        }
    }

    private val decorLayoutListener = View.OnLayoutChangeListener { _, _, _, _, _, oldLeft, oldTop, oldRight, oldBottom ->
        if (attached && (oldRight - oldLeft > 0 || oldBottom - oldTop > 0)) {
            reclampCurrentPosition("window-layout")
        }
    }

    protected open fun onContentSizeChanged(size: OverlaySize) = Unit

    protected open fun onFirstLayoutComplete() = Unit

    protected abstract fun applyPosition(position: OverlayPosition)

    protected fun logUpdateFailure(operation: String, throwable: Throwable) {
        Log.e(TAG, "$operation failed (${throwable::class.java.simpleName}: ${throwable.message})", throwable)
    }

}

@RequiresApi(Build.VERSION_CODES.S)
class DialogOverlayWindowHost(
    context: Context,
    windowManager: WindowManager,
    onBlurAvailabilityChanged: (Boolean) -> Unit,
    onPositionAdjusted: (OverlayPosition) -> Unit = {}
) : BaseOverlayWindowHost(context, windowManager, onBlurAvailabilityChanged, onPositionAdjusted) {
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
        content.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        content.visibility = View.INVISIBLE
        currentPosition = OverlayPosition(initial.x, initial.y)
        overlayDialog.setContentView(content, content.layoutParams)
        val shownWindow = requireNotNull(overlayDialog.window) { "dialog window disappeared" }
        shownWindow.decorView.visibility = View.INVISIBLE
        dialog = overlayDialog
        attached = true
        observeContent(content, shownWindow.decorView)
        registerBlurListener()
        Log.d(TAG, "WINDOW_LAYOUT_REQUEST width=WRAP_CONTENT height=WRAP_CONTENT")
        overlayDialog.show()
        Log.d(TAG, "HOST_ATTACH_OK host=dialog position=${currentPosition.x},${currentPosition.y}")
    }

    override fun onContentSizeChanged(size: OverlaySize) {
        val window = dialog?.window ?: return
        try {
            Log.d(TAG, "WINDOW_LAYOUT_REQUEST width=WRAP_CONTENT height=WRAP_CONTENT size=${size.width}x${size.height}")
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
        } catch (t: Throwable) {
            logUpdateFailure("CONTENT_LAYOUT_REQUEST", t)
        }
    }

    override fun onFirstLayoutComplete() {
        val window = dialog?.window ?: return
        window.decorView.visibility = View.VISIBLE
        content?.visibility = View.VISIBLE
        Log.d(TAG, "HOST_LAYOUT_OK host=dialog position=${currentPosition.x},${currentPosition.y} size=${size.width}x${size.height}")
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

    override fun updatePosition(x: Int, y: Int) {
        if (!attached) return
        try { applyPosition(OverlayPosition(x, y)) }
        catch (t: Throwable) { logUpdateFailure("POSITION_UPDATE", t) }
    }

    override fun applyPosition(position: OverlayPosition) {
        val window = requireNotNull(dialog?.window)
        val actual = if (currentSize.width > 0 && currentSize.height > 0) {
            OverlayGeometry.clampPosition(
                position.x, position.y, usableFrame(window.decorView), currentSize.width, currentSize.height
            )
        } else position
        currentPosition = actual
        window.attributes = copyAttributes(
            window.attributes,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            gravity = Gravity.TOP or Gravity.START,
            x = actual.x,
            y = actual.y
        )
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

    override fun requestContentLayout() {
        if (!attached) return
        try {
            reclampCurrentPosition("layout-request")
            content?.requestLayout()
        } catch (t: Throwable) { logUpdateFailure("CONTENT_LAYOUT_REQUEST", t) }
    }

    override fun detach() {
        if (!attached && dialog == null) return
        attached = false
        stopObservingContent(dialog?.window?.decorView)
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
    onBlurAvailabilityChanged: (Boolean) -> Unit,
    onPositionAdjusted: (OverlayPosition) -> Unit = {}
) : BaseOverlayWindowHost(context, windowManager, onBlurAvailabilityChanged, onPositionAdjusted) {
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
        content.layoutParams = p
        content.visibility = View.INVISIBLE
        currentPosition = OverlayPosition(initial.x, initial.y)
        observeContent(content, content)
        windowManager.addView(content, p)
        this.params = p
        attached = true
        reportBlurAvailability(false)
        Log.d(TAG, "WINDOW_LAYOUT_REQUEST width=WRAP_CONTENT height=WRAP_CONTENT")
        Log.d(TAG, "HOST_ATTACH_OK host=raw position=${currentPosition.x},${currentPosition.y}")
    }

    override fun applyPosition(position: OverlayPosition) {
        val view = requireNotNull(content)
        val p = requireNotNull(params)
        val actual = if (currentSize.width > 0 && currentSize.height > 0) {
            OverlayGeometry.clampPosition(
                position.x, position.y, usableFrame(), currentSize.width, currentSize.height
            )
        } else position
        currentPosition = actual
        p.width = ViewGroup.LayoutParams.WRAP_CONTENT
        p.height = ViewGroup.LayoutParams.WRAP_CONTENT
        p.x = currentPosition.x
        p.y = currentPosition.y
        windowManager.updateViewLayout(view, p)
    }

    override fun onContentSizeChanged(size: OverlaySize) {
        val view = content ?: return
        val p = params ?: return
        try {
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT
            Log.d(TAG, "WINDOW_LAYOUT_REQUEST width=WRAP_CONTENT height=WRAP_CONTENT size=${size.width}x${size.height}")
            windowManager.updateViewLayout(view, p)
        } catch (t: Throwable) {
            logUpdateFailure("CONTENT_LAYOUT_REQUEST", t)
        }
    }

    override fun onFirstLayoutComplete() {
        content?.visibility = View.VISIBLE
        Log.d(TAG, "HOST_LAYOUT_OK host=raw position=${currentPosition.x},${currentPosition.y} size=${size.width}x${size.height}")
    }

    override fun updatePosition(x: Int, y: Int) {
        if (!attached) return
        try { applyPosition(OverlayPosition(x, y)) } catch (t: Throwable) { logUpdateFailure("POSITION_UPDATE", t) }
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

    override fun requestContentLayout() {
        val view = content ?: return
        try {
            reclampCurrentPosition("layout-request")
            view.requestLayout()
        } catch (t: Throwable) { logUpdateFailure("CONTENT_LAYOUT_REQUEST", t) }
    }

    override fun detach() {
        if (!attached && content == null) return
        attached = false
        stopObservingContent(content)
        content?.let {
            try { windowManager.removeViewImmediate(it) }
            catch (t: Throwable) { logUpdateFailure("HOST_REMOVE", t) }
        }
        content = null
        params = null
        Log.d(TAG, "HOST_DETACHED host=raw")
    }
}
