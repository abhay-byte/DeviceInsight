package com.ivarna.deviceinsight.domain.repository

import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getDashboardMetrics(): Flow<DashboardMetrics>
    fun refreshMetrics()
}
