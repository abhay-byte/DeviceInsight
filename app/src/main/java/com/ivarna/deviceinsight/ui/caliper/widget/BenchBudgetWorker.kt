package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ivarna.deviceinsight.data.monitor.MemInfoParser

class BenchBudgetWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            // BUDGET path: direct sample, never MonitorBus
            val snap = BenchSampler.sample(applicationContext)

            // Attempt to enrich with MemInfoParser for composition if possible
            val enriched = try {
                val meminfoStr = MemInfoParser.readMeminfoString()
                val zram = MemInfoParser.readZramBytes()
                if (meminfoStr != null) {
                    val comp = MemInfoParser.parse(meminfoStr, zram)
                    snap.copy(memComposition = comp.segs)
                } else snap
            } catch (_: Exception) { snap }

            // Push to widgets directly
            val mgr = GlanceAppWidgetManager(applicationContext)
            val widgets: List<GlanceAppWidget> = listOf(
                ScopeWidget(), StackWidget(), FuelWidget(), RasterWidget(), BenchWidgetAll()
            )
            // BUDGET uses direct sampler; widgets read via GlobalSnapshot or fallback sampler.
            // To avoid double sampling drift, store in holder for provideGlance to reuse.
            // Foreground vs BUDGET histories intentionally differ (lossy BUDGET per plan).
            BenchBudgetSnapshot.last = enriched

            widgets.forEach { w ->
                try {
                    val ids = mgr.getGlanceIds(w::class.java)
                    ids.forEach { id ->
                        try { w.update(applicationContext, id) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            // clear bypass after short delay to avoid stale
            kotlinx.coroutines.delay(2000)
            BenchBudgetSnapshot.last = null
        }
    }
}

/**
 * Temporary holder for BUDGET snapshot so provideGlance can reuse the same sample
 * without double-sampling. Foreground path uses MonitorBus, not this.
 */
object BenchBudgetSnapshot {
    @Volatile var last: BenchSnapshot? = null
}
