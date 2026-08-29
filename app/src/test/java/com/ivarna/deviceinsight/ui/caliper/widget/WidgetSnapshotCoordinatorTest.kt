package com.ivarna.deviceinsight.ui.caliper.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.ivarna.deviceinsight.ui.caliper.Medium
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WidgetSnapshotCoordinatorTest {
    @Test
    fun freshestSampleWinsOverStaleGlobal() {
        val now = 10_000L
        val stale = published(1_000L, WidgetSnapshotSource.APP_MONITOR, now)
        val live = published(9_000L, WidgetSnapshotSource.APP_MONITOR, now)

        assertSame(live, chooseFreshest(now, listOf(stale, live)))
    }

    @Test
    fun invalidFutureSampleIsIgnored() {
        val now = 10_000L
        val future = published(now + 10_000L, WidgetSnapshotSource.BUDGET, now)
        assertNull(chooseFreshest(now, listOf(future)))
    }

    @Test
    fun budgetCannotReplaceNewerLiveSnapshot() {
        WidgetSnapshotCoordinator.clearForTests()
        WidgetSnapshotCoordinator.publish(BenchSnapshot(timestamp = 9_000L), WidgetSnapshotSource.APP_MONITOR, now = 10_000L)
        WidgetSnapshotCoordinator.publish(BenchSnapshot(timestamp = 8_000L), WidgetSnapshotSource.BUDGET, now = 10_000L)

        assertEquals(9_000L, WidgetSnapshotCoordinator.latest.value?.snapshot?.timestamp)
        assertEquals(WidgetSnapshotSource.APP_MONITOR, WidgetSnapshotCoordinator.latest.value?.source)
    }

    @Test
    fun publishedStateMovesFromAtoBForActiveCompositions() {
        WidgetSnapshotCoordinator.clearForTests()
        WidgetSnapshotCoordinator.publish(BenchSnapshot(timestamp = 1_000L), WidgetSnapshotSource.APP_MONITOR, now = 1_000L)
        assertEquals(1_000L, WidgetSnapshotCoordinator.latest.value?.snapshot?.timestamp)

        WidgetSnapshotCoordinator.publish(BenchSnapshot(timestamp = 2_000L), WidgetSnapshotSource.APP_MONITOR, now = 2_000L)
        assertEquals(2_000L, WidgetSnapshotCoordinator.latest.value?.snapshot?.timestamp)
    }

    @Test
    fun liveRequiresAnActiveProducer() {
        val cfg = BenchConfig(cadence = Cadence.LIVE)
        val budget = published(9_000L, WidgetSnapshotSource.BUDGET, 10_000L)
        val app = published(9_000L, WidgetSnapshotSource.APP_MONITOR, 10_000L)

        assertEquals(WidgetEngineState.BUDGET_ONLY, effectiveCadence(cfg, budget).state)
        assertEquals(1_000L, effectiveCadence(cfg, app).intervalMs)
    }

    @Test
    fun exactSizeAndTierAreSharedByHomeAndPreview() {
        val size = DpSize(300.dp, 178.dp)
        val snapshot = BenchSnapshot(timestamp = 8_000L)
        val home = buildWidgetRenderState(WidgetKind.BENCH, 42, size, Medium.PAPER, BenchConfig(), snapshot)
        val preview = buildWidgetRenderState(WidgetKind.BENCH, 42, size, Medium.PAPER, BenchConfig(), snapshot)

        assertEquals(home, preview)
        assertEquals(Tier.T2, home.tier)
        assertEquals(size, home.exactSize)
    }

    @Test
    fun optionResolverKeepsLauncherFootprint() {
        assertEquals(DpSize(300.dp, 178.dp), WidgetSizeResolver.fromDimensions(300, 178))
    }

    private fun published(timestamp: Long, source: WidgetSnapshotSource, publishedAt: Long) =
        PublishedWidgetSnapshot(BenchSnapshot(timestamp = timestamp), source, publishedAt)
}
