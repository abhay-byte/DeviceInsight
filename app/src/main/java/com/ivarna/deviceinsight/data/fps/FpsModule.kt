package com.ivarna.deviceinsight.data.fps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FpsModule {
    @Binds
    @Singleton
    abstract fun bindFpsRepository(impl: FpsRepositoryImpl): FpsRepository
}
