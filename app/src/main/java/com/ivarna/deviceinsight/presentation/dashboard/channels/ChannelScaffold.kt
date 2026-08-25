package com.ivarna.deviceinsight.presentation.dashboard.channels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivarna.deviceinsight.domain.repository.DashboardRepository
import com.ivarna.deviceinsight.data.monitor.MonitorBus
import com.ivarna.deviceinsight.ui.caliper.components.EndOfSheet
import com.ivarna.deviceinsight.ui.caliper.components.HardKey
import com.ivarna.deviceinsight.ui.caliper.components.HardKeyVariant
import com.ivarna.deviceinsight.ui.caliper.components.LoadThenShow
import com.ivarna.deviceinsight.ui.caliper.components.ScreenHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One VM for all six channel pages — DashboardRepository + MonitorBus, no second sampler. */
@HiltViewModel
class ChannelViewModel @Inject constructor(
    repository: DashboardRepository,
    val bus: MonitorBus
) : ViewModel() {
    val metrics: StateFlow<com.ivarna.deviceinsight.domain.model.DashboardMetrics?> =
        repository.getDashboardMetrics()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

/** Channel page template: BACK · header · instruments · EndOfSheet. */
@Composable
fun ChannelScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    ready: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HardKey(
            "← BACK", variant = HardKeyVariant.SECONDARY,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), onClick = onBack
        )
        ScreenHeader(title, subtitle)
        Spacer(Modifier.height(12.dp))
        LoadThenShow(ready = ready) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                content()
                Spacer(Modifier.height(24.dp))
                EndOfSheet()
            }
        }
    }
}
