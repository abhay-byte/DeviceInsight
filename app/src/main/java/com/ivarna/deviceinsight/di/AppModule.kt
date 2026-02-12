package com.ivarna.deviceinsight.di

import android.content.Context
import com.ivarna.deviceinsight.data.repository.DashboardRepositoryImpl
import com.ivarna.deviceinsight.data.repository.HardwareRepositoryImpl
import com.ivarna.deviceinsight.data.repository.TaskRepositoryImpl
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import com.ivarna.deviceinsight.domain.repository.HardwareRepository
import com.ivarna.deviceinsight.domain.repository.TaskRepository
import com.ivarna.deviceinsight.utils.CpuUtilizationUtils
import com.ivarna.deviceinsight.utils.DisplayRefreshRateUtils
import com.ivarna.deviceinsight.data.fps.FpsMonitor
import com.ivarna.deviceinsight.data.mapper.GpuMapper
import com.ivarna.deviceinsight.data.mapper.SocMapper
import com.ivarna.deviceinsight.data.provider.BatteryProvider
import com.ivarna.deviceinsight.data.provider.CameraProvider
import com.ivarna.deviceinsight.data.provider.CpuProvider
import com.ivarna.deviceinsight.data.provider.DeviceProvider
import com.ivarna.deviceinsight.data.provider.DisplayProvider
import com.ivarna.deviceinsight.data.provider.GpuProvider
import com.ivarna.deviceinsight.data.provider.MemoryProvider
import com.ivarna.deviceinsight.data.provider.NetworkProvider
import com.ivarna.deviceinsight.data.provider.NetworkTrafficProvider
import com.ivarna.deviceinsight.data.provider.PowerProvider
import com.ivarna.deviceinsight.data.provider.SecurityProvider
import com.ivarna.deviceinsight.data.provider.SensorProvider
import com.ivarna.deviceinsight.data.provider.StorageProvider
import com.ivarna.deviceinsight.data.provider.ThermalProvider
import com.ivarna.deviceinsight.data.provider.UsbProvider
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
    ): CpuUtilizationUtils = CpuUtilizationUtils(context)

    @Provides
    @Singleton
    fun provideNetworkTrafficProvider(): NetworkTrafficProvider = NetworkTrafficProvider()

    @Provides
    @Singleton
    fun provideBatteryProvider(@ApplicationContext context: Context): BatteryProvider = BatteryProvider(context)

    @Provides
    @Singleton
    fun provideMemoryProvider(@ApplicationContext context: Context): MemoryProvider = MemoryProvider(context)

    @Provides
    @Singleton
    fun provideStorageProvider(@ApplicationContext context: Context): StorageProvider = StorageProvider(context)

    @Provides
    @Singleton
    fun provideDeviceProvider(
        @ApplicationContext context: Context,
        securityProvider: SecurityProvider
    ): DeviceProvider = DeviceProvider(context, securityProvider)

    @Provides
    @Singleton
    fun providePowerProvider(@ApplicationContext context: Context): PowerProvider = PowerProvider(context)

    @Provides
    @Singleton
    fun provideThermalProvider(): ThermalProvider = ThermalProvider()

    @Provides
    @Singleton
    fun provideCpuProvider(
        cpuUtilizationUtils: CpuUtilizationUtils,
        socMapper: SocMapper
    ): CpuProvider = CpuProvider(cpuUtilizationUtils, socMapper)

    @Provides
    @Singleton
    fun provideGpuMapper(): GpuMapper = GpuMapper()

    @Provides
    @Singleton
    fun provideGpuProvider(
        @ApplicationContext context: Context,
        gpuMapper: GpuMapper
    ): GpuProvider = GpuProvider(context, gpuMapper)

    @Provides
    @Singleton
    fun provideDisplayProvider(
        @ApplicationContext context: Context,
        gpuMapper: GpuMapper
    ): DisplayProvider = DisplayProvider(context, gpuMapper)

    @Provides
    @Singleton
    fun provideNetworkProvider(@ApplicationContext context: Context): NetworkProvider = NetworkProvider(context)

    @Provides
    @Singleton
    fun provideSensorProvider(@ApplicationContext context: Context): SensorProvider = SensorProvider(context)

    @Provides
    @Singleton
    fun provideSecurityProvider(): SecurityProvider = SecurityProvider()

    @Provides
    @Singleton
    fun provideDashboardRepository(
        @ApplicationContext context: Context,
        cpuUtilizationUtils: CpuUtilizationUtils,
        displayRefreshRateUtils: DisplayRefreshRateUtils,
        fpsMonitor: FpsMonitor,
        networkTrafficProvider: NetworkTrafficProvider,
        batteryProvider: BatteryProvider,
        memoryProvider: MemoryProvider,
        storageProvider: StorageProvider,
        deviceProvider: DeviceProvider,
        powerProvider: PowerProvider,
        thermalProvider: ThermalProvider,
        cpuProvider: CpuProvider
    ): DashboardRepository {
        return DashboardRepositoryImpl(
            context,
            cpuUtilizationUtils,
            displayRefreshRateUtils,
            fpsMonitor,
            networkTrafficProvider,
            batteryProvider,
            memoryProvider,
            storageProvider,
            deviceProvider,
            powerProvider,
            thermalProvider,
            cpuProvider
        )
    }

    @Provides
    @Singleton
    fun provideHardwareRepository(
        @ApplicationContext context: Context,
        deviceProvider: DeviceProvider,
        batteryProvider: BatteryProvider,
        storageProvider: StorageProvider,
        memoryProvider: MemoryProvider,
        networkProvider: NetworkProvider,
        displayProvider: DisplayProvider,
        sensorProvider: SensorProvider,
        securityProvider: SecurityProvider,
        cpuProvider: CpuProvider,
        gpuProvider: GpuProvider,
        cameraProvider: CameraProvider,
        usbProvider: UsbProvider
    ): HardwareRepository {
        return HardwareRepositoryImpl(
            context,
            deviceProvider,
            batteryProvider,
            storageProvider,
            memoryProvider,
            networkProvider,
            displayProvider,
            sensorProvider,
            securityProvider,
            cpuProvider,
            gpuProvider,
            cameraProvider,
            usbProvider
        )
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        @ApplicationContext context: Context
    ): TaskRepository {
        return TaskRepositoryImpl(context)
    }
}
