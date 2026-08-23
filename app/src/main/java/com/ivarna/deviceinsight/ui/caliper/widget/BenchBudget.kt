package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
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
        } catch (_: Exception) {}
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
                try { mgr.getGlanceIds(cls).size } catch (_: Exception) { 0 }
            }
            if (total == 0) {
                WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
            }
        } catch (_: Exception) {}
    }
}
