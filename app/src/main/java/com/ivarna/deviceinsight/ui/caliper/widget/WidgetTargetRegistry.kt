package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager

data class WidgetTarget(
    val appWidgetId: Int,
    val glanceId: GlanceId,
    val kind: WidgetKind,
    val config: BenchConfig
)

/** Short-lived target discovery cache. Empty results are never cached. */
object WidgetTargetRegistry {
    private val cache = mutableMapOf<Class<out GlanceAppWidget>, List<GlanceId>>()
    private var cachedAt = 0L

    @Synchronized
    fun invalidate() {
        cache.clear()
        cachedAt = 0L
    }

    suspend fun targets(context: Context, entries: List<Pair<GlanceAppWidget, WidgetKind>>): List<WidgetTarget> {
        val now = System.currentTimeMillis()
        val refresh = synchronized(this) { now - cachedAt >= CACHE_MS || cache.isEmpty() }
        val manager = GlanceAppWidgetManager(context)
        if (refresh) {
            entries.forEach { (widget, _) ->
                val ids = runCatching { manager.getGlanceIds(widget::class.java) }.getOrElse { t ->
                    Log.e("DeviceInsightWidget", "TARGET_LOOKUP_FAIL class=${widget::class.java.simpleName}", t)
                    emptyList()
                }
                if (ids.isNotEmpty()) synchronized(this) {
                    cache[widget::class.java] = ids
                }
            }
            synchronized(this) { cachedAt = now }
        }
        return entries.flatMap { (widget, kind) ->
            val ids = synchronized(this) { cache[widget::class.java].orEmpty() }
            ids.mapNotNull { id ->
                try {
                    val appWidgetId = manager.getAppWidgetId(id)
                    val config = BenchState.config(context, id)
                    if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                        WidgetConfigStore.publish(appWidgetId, config)
                    }
                    WidgetTarget(appWidgetId, id, kind, config)
                } catch (t: Throwable) {
                    Log.e("DeviceInsightWidget", "TARGET_BUILD_FAIL kind=$kind glanceId=$id", t)
                    null
                }
            }
        }
    }

    private const val CACHE_MS = 5_000L
}
