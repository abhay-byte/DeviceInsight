package com.ivarna.deviceinsight.data.monitor

import android.content.Context
import com.ivarna.deviceinsight.domain.repository.TaskRepository
import com.ivarna.deviceinsight.ui.caliper.widget.Consumer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TopConsumersProvider @Inject constructor(
    private val taskRepository: TaskRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun loadTopConsumers(max: Int = 5): List<Consumer> {
        if (!taskRepository.hasUsageStatsPermission()) return emptyList()
        return try {
            val procs = taskRepository.getRunningProcesses()
            procs.take(max).map { info ->
                Consumer(
                    pkg = info.packageName,
                    label = info.appName.take(16),
                    rssMb = 0
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}
