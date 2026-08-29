package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import android.util.Log
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

            // Publish the exact BUDGET sample through the same coordinator as APP_MONITOR.
            // There is no temporary holder to clear and race with a newer live value.
            BenchUpdater.publishAndForceUpdate(
                applicationContext,
                enriched,
                WidgetSnapshotSource.BUDGET
            )
            Result.success()
        } catch (e: Exception) {
            Log.e("DeviceInsightWidget", "BUDGET_SAMPLE_FAIL", e)
            Result.retry()
        }
    }
}
