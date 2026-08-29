package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.CancellationException
import kotlin.math.min
import kotlin.math.roundToInt

/** Inflates the exact RemoteViews size, then uniformly scales the completed view to its host. */
private class ExactRemoteViewsHost(
    context: Context,
    private var contentWidthPx: Int,
    private var contentHeightPx: Int
) : FrameLayout(context) {
    init {
        clipChildren = true
        clipToPadding = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(contentWidthPx, widthMeasureSpec)
        val height = resolveSize(contentHeightPx, heightMeasureSpec)
        setMeasuredDimension(width, height)
        getChildAt(0)?.measure(
            MeasureSpec.makeMeasureSpec(contentWidthPx, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(contentHeightPx, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val child = getChildAt(0) ?: return
        child.layout(0, 0, contentWidthPx, contentHeightPx)
        val scale = min(
            measuredWidth.toFloat() / contentWidthPx,
            measuredHeight.toFloat() / contentHeightPx
        )
        child.pivotX = 0f
        child.pivotY = 0f
        child.scaleX = scale
        child.scaleY = scale
    }

    fun replace(remoteViews: RemoteViews?) {
        removeAllViews()
        remoteViews?.let {
            addView(it.apply(context, this), LayoutParams(contentWidthPx, contentHeightPx))
        }
        requestLayout()
    }

    fun updateContentSize(widthPx: Int, heightPx: Int) {
        if (contentWidthPx == widthPx && contentHeightPx == heightPx) return
        contentWidthPx = widthPx
        contentHeightPx = heightPx
        requestLayout()
    }
}

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun LiveWidgetPreview(
    kind: WidgetKind,
    config: BenchConfig,
    snapshot: BenchSnapshot,
    exactSize: DpSize,
    appWidgetId: Int,
    snapshotSource: WidgetSnapshotSource = WidgetSnapshotSource.ON_DEMAND,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }
    LaunchedEffect(kind, config, snapshot, exactSize, appWidgetId, snapshotSource) {
        val medium = resolvedMedium(context, config)
        remoteViews = runCatching {
            GlanceRemoteViews().compose(context, exactSize) {
                val render = buildWidgetRenderState(kind, appWidgetId, exactSize, medium, config, snapshot,
                    snapshotSource = snapshotSource)
                InstrumentBody(
                    render.kind,
                    render.tier,
                    render.medium,
                    render.config,
                    render.snapshot,
                    render.calibrating,
                    render.appWidgetId,
                    render.snapshotSource
                )
            }.remoteViews
        }.getOrElse { t ->
            if (t is CancellationException) throw t
            Log.e("DeviceInsightWidget", "PREVIEW_RENDER_FAIL kind=$kind appWidgetId=$appWidgetId size=$exactSize", t)
            null
        }
    }
    val density = context.resources.displayMetrics.density
    val exactWidthPx = (exactSize.width.value * density).roundToInt().coerceAtLeast(1)
    val exactHeightPx = (exactSize.height.value * density).roundToInt().coerceAtLeast(1)
    AndroidView(
        factory = { ExactRemoteViewsHost(it, exactWidthPx, exactHeightPx) },
        update = { host ->
            host.updateContentSize(exactWidthPx, exactHeightPx)
            host.replace(remoteViews)
        },
        modifier = modifier
    )
}
