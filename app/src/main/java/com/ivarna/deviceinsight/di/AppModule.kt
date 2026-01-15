package com.ivarna.deviceinsight.di

import android.content.Context
import com.ivarna.deviceinsight.data.repository.DashboardRepositoryImpl
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import com.ivarna.deviceinsight.utils.DisplayRefreshRateUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCpuUtilizationUtils(
        @ApplicationContext context: Context
    ): CpuUtilizationUtils {
        return CpuUtilizationUtils(context)
    }

    @Provides
    @Singleton
    fun provideDashboardRepository(
        @ApplicationContext context: Context,
        cpuUtilizationUtils: CpuUtilizationUtils,
        displayRefreshRateUtils: DisplayRefreshRateUtils
    ): DashboardRepository {
        return DashboardRepositoryImpl(context, cpuUtilizationUtils, displayRefreshRateUtils)
    }

    @Provides
    @Singleton
    fun provideHardwareRepository(
        @ApplicationContext context: Context
    ): com.ivarna.deviceinsight.domain.repository.HardwareRepository {
        return com.ivarna.deviceinsight.data.repository.HardwareRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        @ApplicationContext context: Context
    ): com.ivarna.deviceinsight.domain.repository.TaskRepository {
        return com.ivarna.deviceinsight.data.repository.TaskRepositoryImpl(context)
    }
}
