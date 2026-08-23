# Plan Review: PLAN_widgets_overlay — PASS 2 ITER 2

## Verdict: APPROVE
## Counts: CRITICAL 0 MAJOR 0 MINOR 1 SUGGESTIONS 1

### Findings

#### [MINOR] BenchBudget.cancelIfNone suspend signature not explicit
- Location: docs/plans/PLAN_widgets_overlay.md:310-316, BenchGlance.kt:627, GlanceAppWidgetManager.kt: getGlanceIds suspend
- Problem: Plan's helper `BenchBudget.cancelIfNone(ctx)` is shown as non-suspend `fun` with direct `mgr.getGlanceIds(it.java).size` inside `runCatching`, but `GlanceAppWidgetManager.getGlanceIds` is `suspend fun getGlanceIds(Class<T>, Continuation<List<GlanceId>>)` (verified via `javap` on glance-appwidget-1.1.0.aar). Calling it from `GlanceAppWidgetReceiver.onDisabled` (non-suspend) without coroutine launch/runBlocking will not compile.
- Evidence: `javap GlanceAppWidgetManager`: `public final Object getGlanceIds(Class, Continuation)`; existing call sites are inside `scope.launch` (BenchUpdater.kt:90) or `suspend fun refreshInstruments` (WidgetsSheet.kt:123).
- Impact: Minor — worker will discover at compile and wrap with `CoroutineScope(Dispatchers.IO).launch { ... }` or `runBlocking { }` or make helper `suspend`. No data loss, quick fix.
- Required planner change (optional before worker): Amend §0.7 snippet to `suspend fun cancelIfNone(ctx)` and show call site `override fun onDisabled(ctx){ super...; CoroutineScope(Dispatchers.IO).launch{ BenchBudget.cancelIfNone(ctx)} }` or `runBlocking`. Not blocking APPROVE.

#### [SUGGESTION] Add DataStore migration eventual-consistency note for FpsMonitor cache
- Location: docs/plans/PLAN_widgets_overlay.md:347-354, SystemStatsApplication.kt:15
- Problem: HudSettingsCache is `@Volatile var fpsMode="AUTO"` fed by `ctx.hudFpsModeFlow.collect` launched from `SystemStatsApplication`/`cache init` on `Dispatchers.IO`. Migration in `SystemStatsApplication.onCreate` is `launch(IO){ read overlay_prefs → edit caliper → clear }` with `runCatching`. For a few ms after first upgrade, `FpsMonitor.getAccessType()` may see default AUTO before collector emits migrated ROOT. Honest but brief inconsistency.
- Evidence: CaliperPrefs single file, DataStore collect is async.
- Impact: Negligible — first HUD START within ~100ms of upgrade could use stale mode; retry on next FPS tick picks correct.
- Required planner change (nice-to-have): Note that `HudSettingsCache` initial value should be seeded via `runBlocking { ctx.dataStore.data.first()[hudFpsMode] ?: migratedValue }` if worker wants strict consistency, or document that brief AUTO fallback is acceptable. Not blocking.

### Prior Findings Resolution

All 3 CRITICAL + 6 MAJOR + 2 MINOR + 1 SUGGESTION from PASS 1 ITER 1 verified FIXED in PASS 2:

| Prior ID | Severity | Status | Evidence in PASS 2 |
|---|---|---|---|
| HUD persistence split / dual stores | CRITICAL | FIXED | §0.old-overlay pinned + §0.9 single source `caliper` + `CaliperKeys.hud*` + migration 0.9 steps 1-4 + `HudSettingsCache` + greps `overlay_prefs`=0 acceptance line 394, `or` removed line 701 |
| Single-writer bus vs BUDGET sampler | CRITICAL | FIXED | §0.1 invariant reword line 207 + §0.7 BUDGET table lossy fields + MemInfoParser reuse line 298 + acceptance grep checks |
| Memory parser fractions math | CRITICAL | FIXED | §0.4 pure `MemInfoParser.parse` exact formula MemTotal denominator, swap dedup `max(0,swapRaw-zramF)`, free `1-sum`, normalization 261, order, harness line 806 |
| Fast 10Hz ticker threading/backoff | MAJOR | FIXED | §0.2 `FpsTicker` `Dispatchers.IO` `SupervisorJob` owned by OverlayService onCreate→onDestroy, adaptive 100→1000ms after 5×"—", layer cache 30s line 214-229, verified FpsMonitor blocking via `process.waitFor()` |
| WorkManager enqueue/cancel race | MAJOR | FIXED | §0.7 BenchBudget helper enqueue/cancelIfNone counting all 5 kinds atomically `GlanceAppWidgetManager.getGlanceIds` sumOf, KEEP rationale, onEnabled/onDisabled per receiver lines 307-324, 367-368, files table |
| Top consumers source | MAJOR | FIXED | §0.9 TopConsumersProvider `data/monitor/TopConsumersProvider.kt` with `TaskRepository.kt:6` + `TaskRepositoryImpl.kt:22-38,40-94`, permission-gated, empty-hide vs label-only, ponytail Shizuku/rss note line 356-362, 500 |
| WidgetsSheet refresh lifecycle | MAJOR | FIXED | §2.3 delay(1200)+ON_RESUME via LocalLifecycleOwner, no PendingIntent trampoline to SettingsActivity, WidgetsSheet.kt:33/152 vs SettingsScreen.kt:38 context line 614-615 |
| Hairline frame T1 | MAJOR | FIXED | §1.2 exact Glance Box{Column(pad12){1dp top; Row(defaultWeight){1dp left; Column(defaultWeight,pad9){content};1dp right};1dp bottom}} verified `defaultWeight` via `javap RowScope/ColumnScope` in glance-1.1.0.aar, T1 PAPER screenshot QA line 419-442 |
| HUD medium vs widget follow-system | MAJOR | FIXED | §3.1 distinct `HudMedium {PAPER,CARBON,BLUEPRINT}` + mapping fns `toCaliperMedium/fromMedium`, §0.9 keys, §3.3 note no follow flag line 642-647 |
| Glance providePreview | MINOR | FIXED | Research AAR verification line 70-71: 0 matches across 477 classes via javap+grep, §0.9/1.10/Verification no providePreview, previewImage only line 372, 552, Risks table line 776 |
| BenchFrames recycle + onDeleted global clear | MINOR | FIXED | §1.3 remove `entryRemoved recycle()` line 451-452 (BenchModel.kt:382), §1.4 per-id onDeleted for all 5 receivers + per-id lastPush eviction lines 468-474, §0.9 |
| Receiver FQN immutability test | SUGGESTION | FIXED | §0.9 WidgetReceiversExistTest Class.forName 5 FQNs AndroidManifest.xml:79,91,103,115,127 + assembleRelease R8 keep note line 374, Verification 813 |

No regressions detected:
- Verified `app/build.gradle.kts:67` isMinifyEnabled true still, work-runtime consumer keep covers BenchBudgetWorker; proguard-rules.pro already broad keep.
- Verified `AndroidManifest.xml` 5 receivers still FQN `ui.caliper.widget.*Receiver`, minSdk 26, target 36 unchanged.
- Verified `DashboardRepositoryImpl.kt:61` 1s loop still exists to be changed to 500ms per plan; no second sampler introduced.
- Verified `SystemStatsApplication.kt:15` bare Application still exists to add migration+enqueue.
- Verified `providePreview` absence via local AAR extraction (§Research hard evidence) matches `javap GlenAppWidget` only `provideGlance`.

Worker can implement without rediscovering architecture. All pinned decisions cite file:line or bash output.

## Next Agent: Worker
## Next Action: Kick off implementation with worker on Phase 0 (shared foundation: MonitorBus, HudFeed, MemInfoParser, BenchBudget/BenchBudgetWorker, HudSettingsCache, TopConsumersProvider, CaliperPrefs migration, DashboardRepositoryImpl 2Hz + FpsMonitor source, WorkManager). PASS 2 approved — no planner rework needed.
