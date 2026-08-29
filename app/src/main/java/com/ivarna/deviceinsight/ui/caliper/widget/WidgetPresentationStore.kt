package com.ivarna.deviceinsight.ui.caliper.widget

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/** Snapshots presented to home widgets after the cadence gate has allowed them through. */
object WidgetPresentationStore {
    private val states = ConcurrentHashMap<Int, MutableStateFlow<PublishedWidgetSnapshot?>>()

    fun stateFor(appWidgetId: Int): StateFlow<PublishedWidgetSnapshot?> =
        states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }

    fun seedIfEmpty(appWidgetId: Int, snapshot: PublishedWidgetSnapshot) {
        val state = states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }
        synchronized(state) {
            if (state.value == null) state.value = snapshot
        }
    }

    fun present(appWidgetId: Int, snapshot: PublishedWidgetSnapshot): Boolean {
        val state = states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }
        synchronized(state) {
            val current = state.value
            if (current != null && snapshot.snapshot.timestamp < current.snapshot.timestamp) return false
            if (current != snapshot) state.value = snapshot
            return true
        }
    }

    fun remove(appWidgetId: Int) { states.remove(appWidgetId) }

    fun clearForTests() { states.clear() }
}

/** In-process bridge for configuration changes while a Glance composition is alive. */
object WidgetConfigStore {
    private val states = ConcurrentHashMap<Int, MutableStateFlow<BenchConfig?>>()

    fun stateFor(appWidgetId: Int): StateFlow<BenchConfig?> =
        states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }

    fun seedIfEmpty(appWidgetId: Int, config: BenchConfig) {
        val state = states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }
        synchronized(state) {
            if (state.value == null) state.value = config
        }
    }

    fun publish(appWidgetId: Int, config: BenchConfig) {
        states.computeIfAbsent(appWidgetId) { MutableStateFlow(null) }.value = config
    }

    fun remove(appWidgetId: Int) { states.remove(appWidgetId) }

    fun clearForTests() { states.clear() }
}
