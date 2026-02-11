package com.ivarna.deviceinsight.data.repository

import android.content.Context
import android.os.Build
import com.ivarna.deviceinsight.data.provider.*
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.repository.HardwareRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HardwareRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceProvider: DeviceProvider,
    private val batteryProvider: BatteryProvider,
    private val storageProvider: StorageProvider,
    private val memoryProvider: MemoryProvider,
    private val networkProvider: NetworkProvider,
    private val displayProvider: DisplayProvider,
    private val sensorProvider: SensorProvider,
    private val securityProvider: SecurityProvider,
    private val cpuProvider: CpuProvider
) : HardwareRepository {

    override fun getHardwareInfo(): HardwareInfo {
        val memInfo = memoryProvider.getMemoryInfo()
        val storageInfo = storageProvider.getInternalStorageInfo()
        val externalStorageInfo = storageProvider.getExternalStorageInfo()
        val batteryInfo = batteryProvider.getBatteryInfo()
        val cpuFeatures = cpuProvider.getFeatures()
        
        return HardwareInfo(
            deviceName = Build.DEVICE,
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            product = Build.PRODUCT,
            serial = deviceProvider.getSerial(),
            deviceType = deviceProvider.getDeviceType(),
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            
            totalRam = memInfo.first,
            availableRam = memInfo.second,
            totalStorage = storageInfo.first,
            availableStorage = storageInfo.second,
            totalExternalStorage = externalStorageInfo.first,
            availableExternalStorage = externalStorageInfo.second,
            
            networkOperator = networkProvider.getNetworkOperatorName(),
            networkType = networkProvider.getNetworkType(),
            ipAddress = networkProvider.getIpAddress(),
            
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
            kernelVersion = deviceProvider.getKernelVersion(),
            buildId = Build.ID,
            isRooted = securityProvider.isRooted(),
            upTime = deviceProvider.getUpTime(),

            socModel = cpuProvider.getSocModel(),
            cpuArchitecture = cpuProvider.getCpuArchitecture(),
            manufacturingProcess = cpuProvider.getManufacturingProcess(),
            cpuRevision = cpuProvider.getCpuRevision(),
            cpuClockRange = cpuProvider.getCpuClockRange(),
            cpuUtilization = cpuProvider.getCpuUtilization(),
            coreClocks = cpuProvider.getCpuCoreFrequencies(),
            supported64BitAbis = Build.SUPPORTED_64_BIT_ABIS.toList(),
            hasAes = cpuFeatures["aes"] == true,
            hasNeon = cpuFeatures["neon"] == true,
            hasPmull = cpuFeatures["pmull"] == true,
            hasSha1 = cpuFeatures["sha1"] == true,
            hasSha2 = cpuFeatures["sha2"] == true,
            
            resolution = displayProvider.getScreenResolution(),
            density = displayProvider.getDensityString(),
            densityDpi = context.resources.displayMetrics.densityDpi,
            refreshRate = displayProvider.getRefreshRate(),
            
            batteryTechnology = batteryInfo.technology,
            batteryHealth = batteryInfo.health,
            batteryLevel = batteryInfo.level,
            batteryStatus = batteryInfo.status,
            batteryVoltage = batteryInfo.voltage,
            batteryTemperature = batteryInfo.temperature,
            isCharging = batteryInfo.isCharging,
            batteryCapacity = batteryInfo.capacity,
            
            sensorCount = sensorProvider.getSensorCount(),
            availableSensors = sensorProvider.getSensorList(),
            fingerprintSensorPresent = sensorProvider.hasFingerprintSensor()
        )
    }
}
