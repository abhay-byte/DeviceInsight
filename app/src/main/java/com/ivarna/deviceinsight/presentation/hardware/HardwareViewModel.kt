package com.ivarna.deviceinsight.presentation.hardware

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.deviceinsight.domain.model.HardwareInfo
import com.ivarna.deviceinsight.domain.repository.HardwareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HardwareViewModel @Inject constructor(
    private val repository: HardwareRepository
) : ViewModel() {

    private val _hardwareInfo = MutableStateFlow<HardwareInfo?>(null)
    val hardwareInfo: StateFlow<HardwareInfo?> = _hardwareInfo.asStateFlow()

    init {
        // spec sheets are static — read once; ON_RESUME refreshes from the screen
        loadHardwareInfo()
    }

    fun loadHardwareInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            _hardwareInfo.value = repository.getHardwareInfo()
        }
    }
}
