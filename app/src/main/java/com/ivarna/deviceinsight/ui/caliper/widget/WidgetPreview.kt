package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
import android.view.ViewGroup
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
    LaunchedEffect(kind, config, snapshot, exactSize, appWidgetId) {
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
            Log.e("DeviceInsightWidget", "PREVIEW_RENDER_FAIL kind=$kind appWidgetId=$appWidgetId size=$exactSize", t)
            null
        }
    }
    AndroidView(
        factory = { FrameLayout(it) },
        update = { host ->
            host.removeAllViews()
            remoteViews?.let { rv ->
                val view = rv.apply(host.context, host)
                host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        },
        modifier = modifier
    )
}
