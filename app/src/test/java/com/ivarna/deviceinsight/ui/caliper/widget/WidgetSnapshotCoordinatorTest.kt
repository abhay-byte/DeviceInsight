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
    fun onlyOldLiveCandidateIsRejected() {
        val now = 20_000L
        val old = published(1_000L, WidgetSnapshotSource.APP_MONITOR, now)
        assertNull(chooseFreshest(now, listOf(old)))
        assertEquals(false, isCandidateFresh(now, old))
    }

    @Test
    fun budgetCandidateUsesLongerBackgroundWindow() {
        val now = 20 * 60_000L
        val budget = published(1L, WidgetSnapshotSource.BUDGET, now)
        assertEquals(true, isCandidateFresh(now, budget))
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
    fun coordinatorPublishesNewerSnapshots() {
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
    fun presentationStateIsPerWidgetAndRejectsOlderSnapshots() {
        WidgetPresentationStore.clearForTests()
        val a = published(1_000L, WidgetSnapshotSource.APP_MONITOR, 1_000L)
        val b = published(2_000L, WidgetSnapshotSource.APP_MONITOR, 2_000L)
        val old = published(1_500L, WidgetSnapshotSource.BUDGET, 2_000L)

        WidgetPresentationStore.present(42, a)
        WidgetPresentationStore.present(43, b)
        WidgetPresentationStore.present(42, b)
        assertEquals(2_000L, WidgetPresentationStore.stateFor(42).value?.snapshot?.timestamp)
        assertEquals(2_000L, WidgetPresentationStore.stateFor(43).value?.snapshot?.timestamp)
        assertEquals(false, WidgetPresentationStore.present(42, old))
        assertEquals(2_000L, WidgetPresentationStore.stateFor(42).value?.snapshot?.timestamp)
    }

    @Test
    fun cadenceGateDoesNotPresentRawHalfSecondSamples() {
        val live = EffectiveCadence(WidgetEngineState.LIVE_ACTIVE, 1_000L)
        assertEquals(true, isUpdateDue(1_000L, 0L, live))
        assertEquals(false, isUpdateDue(1_500L, 1_000L, live))
        assertEquals(true, isUpdateDue(2_000L, 1_000L, live))
    }

    @Test
    fun ambientAndBudgetDoNotFollowForegroundRawRate() {
        val ambient = EffectiveCadence(WidgetEngineState.AMBIENT_ACTIVE, 30_000L)
        val budget = EffectiveCadence(WidgetEngineState.BUDGET_ONLY, 900_000L)
        assertEquals(false, isUpdateDue(1_500L, 1_000L, ambient))
        assertEquals(false, isUpdateDue(1_500L, 1_000L, budget))
        assertEquals(true, isUpdateDue(31_000L, 1_000L, ambient))
    }

    @Test
    fun configStatePublishesPerWidget() {
        WidgetConfigStore.clearForTests()
        val initial = BenchConfig(medium = Medium.PAPER)
        val changed = BenchConfig(medium = Medium.BLUEPRINT, cadence = Cadence.BUDGET)
        WidgetConfigStore.seedIfEmpty(42, initial)
        WidgetConfigStore.publish(42, changed)
        assertEquals(changed, WidgetConfigStore.stateFor(42).value)
        assertNull(WidgetConfigStore.stateFor(43).value)
    }

    @Test
    fun removingWidgetStateStopsFutureUpdates() {
        WidgetPresentationStore.clearForTests()
        WidgetConfigStore.clearForTests()
        val snapshot = published(2_000L, WidgetSnapshotSource.APP_MONITOR, 2_000L)
        WidgetPresentationStore.present(42, snapshot)
        WidgetConfigStore.publish(42, BenchConfig())

        WidgetPresentationStore.remove(42)
        WidgetConfigStore.remove(42)

        assertNull(WidgetPresentationStore.stateFor(42).value)
        assertNull(WidgetConfigStore.stateFor(42).value)
    }

    @Test
    fun optionResolverKeepsLauncherFootprint() {
        assertEquals(DpSize(300.dp, 178.dp), WidgetSizeResolver.fromDimensions(300, 178))
    }

    private fun published(timestamp: Long, source: WidgetSnapshotSource, publishedAt: Long) =
        PublishedWidgetSnapshot(BenchSnapshot(timestamp = timestamp), source, publishedAt)
}
