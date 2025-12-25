package com.ivarna.deviceinsight.domain.repository

import com.ivarna.deviceinsight.domain.model.AppProcessInfo

interface TaskRepository {
    fun hasUsageStatsPermission(): Boolean
    suspend fun getRunningProcesses(): List<AppProcessInfo>
}
