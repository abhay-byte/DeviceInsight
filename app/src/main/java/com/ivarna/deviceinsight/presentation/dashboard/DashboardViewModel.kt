package com.ivarna.deviceinsight.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.deviceinsight.data.provider.CpuProvider
import com.ivarna.deviceinsight.data.provider.DeviceProvider
import com.ivarna.deviceinsight.domain.model.DashboardMetrics
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DeviceCardInfo(
    val deviceName: String,
    val cpuModel: String,
    val gpuModel: String,
    val manufacturer: String = "",
    val androidVersion: String = "",
    val cpuArchitecture: String = "",
    val totalCores: Int = 8
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val deviceProvider: DeviceProvider,
    private val cpuProvider: CpuProvider
) : ViewModel() {

    val uiState: StateFlow<DashboardMetrics?> = repository.getDashboardMetrics()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _powerMultiplier = MutableStateFlow(1f)
    val powerMultiplier: StateFlow<Float> = _powerMultiplier

    private val staticDeviceInfo = DeviceCardInfo(
        deviceName = deviceProvider.getDeviceModelName(),
        cpuModel = cpuProvider.getSocModel(),
        gpuModel = "",
        manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
        androidVersion = "Android ${android.os.Build.VERSION.RELEASE}",
        cpuArchitecture = cpuProvider.getCpuArchitecture(),
        totalCores = Runtime.getRuntime().availableProcessors()
    )

    /**
     * Combined state: live metrics enriched with the latest GPU renderer (which
     * comes from a lazy OpenGL query and is only known after the first sample).
     */
    val deviceCard: StateFlow<DeviceCardInfo> =
        combine(uiState, MutableStateFlow(staticDeviceInfo)) { m, info ->
            info.copy(
                deviceName = info.deviceName,
                cpuModel = if (info.cpuModel.isNotBlank() && info.cpuModel != "QCOM" && info.cpuModel != "UNKNOWN") info.cpuModel else cpuProvider.getSocModel(),
                gpuModel = m?.gpuModel?.takeIf { it.isNotBlank() } ?: info.gpuModel,
                cpuArchitecture = if (m?.cpuArchitecture?.isNotBlank() == true) m.cpuArchitecture else info.cpuArchitecture
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = staticDeviceInfo
        )

    fun setPowerMultiplier(multiplier: Float) {
        _powerMultiplier.value = multiplier
    }
}