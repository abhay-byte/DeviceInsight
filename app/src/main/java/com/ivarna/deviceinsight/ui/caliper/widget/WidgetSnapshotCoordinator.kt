package com.ivarna.deviceinsight.ui.caliper.widget

import android.content.Context
import com.ivarna.deviceinsight.data.monitor.GlobalSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WidgetSnapshotSource {
    APP_MONITOR,
    FOREGROUND_SERVICE,
    BUDGET,
    ON_DEMAND
}

data class PublishedWidgetSnapshot(
    val snapshot: BenchSnapshot,
    val source: WidgetSnapshotSource,
    val publishedAt: Long
)

/** The only in-process owner of the snapshot rendered by widgets and previews. */
object WidgetSnapshotCoordinator {
    private val _latest = MutableStateFlow<PublishedWidgetSnapshot?>(null)
    val latest: StateFlow<PublishedWidgetSnapshot?> = _latest.asStateFlow()

    fun publish(
        snapshot: BenchSnapshot,
        source: WidgetSnapshotSource,
        now: Long = System.currentTimeMillis()
    ) {
        if (snapshot.timestamp <= 0L || snapshot.timestamp > now + FUTURE_SKEW_MS) return
        val next = PublishedWidgetSnapshot(snapshot, source, now)
        val current = _latest.value
        if (current == null || snapshot.timestamp >= current.snapshot.timestamp) {
            _latest.value = next
        }
    }

    suspend fun resolveInitial(context: Context): PublishedWidgetSnapshot {
        val now = System.currentTimeMillis()
        val candidates = listOfNotNull(
            _latest.value,
            GlobalSnapshot.current()?.let {
                PublishedWidgetSnapshot(it, WidgetSnapshotSource.APP_MONITOR, now)
            }
        )
        val selected = chooseFreshest(now, candidates)
        if (selected != null) {
            _latest.value = selected
            return selected
        }
        val sampled = BenchSampler.sample(context)
        val fallback = PublishedWidgetSnapshot(sampled, WidgetSnapshotSource.ON_DEMAND, now)
        if (sampled.timestamp > 0L) _latest.value = fallback
        return fallback
    }

    fun clearForTests() {
        _latest.value = null
    }

    private const val FUTURE_SKEW_MS = 5_000L
}

fun chooseFreshest(
    now: Long,
    candidates: Iterable<PublishedWidgetSnapshot>
): PublishedWidgetSnapshot? = candidates
    .filter { it.snapshot.timestamp in 1L..(now + 5_000L) }
    .maxWithOrNull(compareBy<PublishedWidgetSnapshot> { it.snapshot.timestamp }.thenBy { it.publishedAt })

enum class WidgetEngineState {
    LIVE_ACTIVE,
    AMBIENT_ACTIVE,
    BUDGET_ONLY,
    PAUSED
}

data class EffectiveCadence(
    val state: WidgetEngineState,
    val intervalMs: Long?
)

fun effectiveCadence(cfg: BenchConfig, published: PublishedWidgetSnapshot): EffectiveCadence = when {
    cfg.cadence == Cadence.LIVE && published.source in setOf(
        WidgetSnapshotSource.APP_MONITOR,
        WidgetSnapshotSource.FOREGROUND_SERVICE
    ) -> EffectiveCadence(WidgetEngineState.LIVE_ACTIVE, 1_000L)
    cfg.cadence == Cadence.LIVE -> EffectiveCadence(WidgetEngineState.BUDGET_ONLY, 15 * 60_000L)
    cfg.cadence == Cadence.AMBIENT -> EffectiveCadence(WidgetEngineState.AMBIENT_ACTIVE, 30_000L)
    cfg.cadence == Cadence.BUDGET -> EffectiveCadence(WidgetEngineState.BUDGET_ONLY, 15 * 60_000L)
    else -> EffectiveCadence(WidgetEngineState.PAUSED, null)
}
