package com.ivarna.deviceinsight.di

import android.content.Context
import com.ivarna.deviceinsight.data.repository.DashboardRepositoryImpl
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
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
    fun provideDashboardRepository(
        @ApplicationContext context: Context
    ): DashboardRepository {
        return DashboardRepositoryImpl(context)
    }
}
