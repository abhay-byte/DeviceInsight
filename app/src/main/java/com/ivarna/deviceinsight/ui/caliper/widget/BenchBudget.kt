package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BenchBudget {
    const val UNIQUE = "bench-budget"

    fun enqueue(ctx: Context) {
        try {
            val req = PeriodicWorkRequestBuilder<BenchBudgetWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, req)
        } catch (t: Throwable) { Log.e("DeviceInsightWidget", "BUDGET_ENQUEUE_FAIL", t) }
    }

    suspend fun cancelIfNone(ctx: Context) {
        try {
            val mgr = GlanceAppWidgetManager(ctx)
            val total = listOf(
                ScopeWidget::class.java,
                StackWidget::class.java,
                FuelWidget::class.java,
                RasterWidget::class.java,
                BenchWidgetAll::class.java
            ).sumOf { cls ->
                try { mgr.getGlanceIds(cls).size } catch (t: Throwable) {
                    Log.e("DeviceInsightWidget", "BUDGET_TARGET_LOOKUP_FAIL class=${cls.simpleName}", t)
                    0
                }
            }
            if (total == 0) {
                WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
            }
        } catch (t: Throwable) { Log.e("DeviceInsightWidget", "BUDGET_CANCEL_FAIL", t) }
    }
}
