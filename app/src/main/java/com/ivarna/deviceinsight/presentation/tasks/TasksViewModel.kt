package com.ivarna.deviceinsight.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.deviceinsight.domain.model.AppProcessInfo
import com.ivarna.deviceinsight.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    private val _runningApps = MutableStateFlow<List<AppProcessInfo>>(emptyList())
    val runningApps: StateFlow<List<AppProcessInfo>> = _runningApps.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    fun checkPermissionAndLoadApps() {
        val hasPermCheck = repository.hasUsageStatsPermission()
        _hasPermission.value = hasPermCheck
        
        if (hasPermCheck) {
            viewModelScope.launch {
                _runningApps.value = repository.getRunningProcesses()
            }
        }
    }
}
