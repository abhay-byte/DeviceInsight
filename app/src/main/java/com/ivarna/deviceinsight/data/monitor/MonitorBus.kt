package com.ivarna.deviceinsight.data.monitor

import com.ivarna.deviceinsight.ui.caliper.widget.BenchSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitorBus @Inject constructor() {
    private val _snap = MutableStateFlow(BenchSnapshot())
    val snapshot: StateFlow<BenchSnapshot> = _snap.asStateFlow()
    private val _slow = MutableStateFlow(HudSlow())
    val slow: StateFlow<HudSlow> = _slow.asStateFlow()
    private val _fast = MutableStateFlow(HudFast())
    val fast: StateFlow<HudFast> = _fast.asStateFlow()

    fun pushSlow(snap: BenchSnapshot, slow: HudSlow) {
        _snap.value = snap
        _slow.value = slow
        GlobalSnapshot.last = snap
    }

    fun pushFast(fast: HudFast) {
        _fast.value = fast
    }

    fun current(): BenchSnapshot = _snap.value
}

object GlobalSnapshot {
    @Volatile var last: BenchSnapshot? = null
    fun current(): BenchSnapshot? = last
}
