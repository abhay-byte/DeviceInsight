package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/** Serializes widget fan-out and always renders the newest published snapshot. */
object WidgetUpdateCoordinator {
    private const val TAG = "DeviceInsightWidget"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val requests = Channel<Request>(Channel.CONFLATED)
    private val lastUpdateAt = ConcurrentHashMap<Int, Long>()
    private val widgets: List<Pair<GlanceAppWidget, WidgetKind>> = listOf(
        ScopeWidget() to WidgetKind.SCOPE,
        StackWidget() to WidgetKind.STACK,
        FuelWidget() to WidgetKind.FUEL,
        RasterWidget() to WidgetKind.RASTER,
        BenchWidgetAll() to WidgetKind.BENCH
    )

    init {
        scope.launch {
            for (request in requests) {
                process(request)
            }
        }
    }

    fun publish(
        context: Context,
        snapshot: BenchSnapshot,
        source: WidgetSnapshotSource,
        force: Boolean = false
    ) {
        WidgetSnapshotCoordinator.publish(snapshot, source)
        requests.trySend(Request(context.applicationContext, force))
    }

    fun requestRefresh(context: Context, force: Boolean = true) {
        requests.trySend(Request(context.applicationContext, force))
    }

    fun evict(appWidgetId: Int) {
        lastUpdateAt.remove(appWidgetId)
        WidgetPresentationStore.remove(appWidgetId)
        WidgetConfigStore.remove(appWidgetId)
        WidgetTargetRegistry.invalidate()
    }

    fun invalidateTargets() = WidgetTargetRegistry.invalidate()

    private suspend fun process(request: Request) {
        val published = WidgetSnapshotCoordinator.latest.value ?: return
        val now = System.currentTimeMillis()
        val targets = WidgetTargetRegistry.targets(request.context, widgets)
        Log.d(TAG, "UPDATE_BEGIN source=${published.source} age=${now - published.snapshot.timestamp} targets=${targets.size} force=${request.force}")
        for (target in targets) {
            WidgetConfigStore.publish(target.appWidgetId, target.config)
            val effective = effectiveCadence(target.config, published)
            val previous = lastUpdateAt[target.appWidgetId] ?: 0L
            if (!isUpdateDue(now, previous, effective, request.force)) continue
            val widget = widgets.firstOrNull { it.second == target.kind }?.first ?: continue
            val start = System.currentTimeMillis()
            try {
                WidgetPresentationStore.present(target.appWidgetId, published)
                widget.update(request.context, target.glanceId)
                lastUpdateAt[target.appWidgetId] = System.currentTimeMillis()
                Log.d(TAG, "UPDATE_END appWidgetId=${target.appWidgetId} source=${published.source} state=${effective.state} durationMs=${System.currentTimeMillis() - start}")
            } catch (t: Throwable) {
                Log.e(TAG, "UPDATE_FAIL appWidgetId=${target.appWidgetId} kind=${target.kind}", t)
            }
        }
    }

    private data class Request(val context: Context, val force: Boolean)
}

internal fun isUpdateDue(
    now: Long,
    lastPresentedAt: Long,
    effective: EffectiveCadence,
    force: Boolean = false
): Boolean = force || effective.intervalMs == null || now - lastPresentedAt >= effective.intervalMs
