# Impl Review: PLAN_widgets_overlay — PASS 3 ITER 3

## Verdict: APPROVE
## Counts: CRITICAL 0 MAJOR 0 MINOR 1

### Findings — Re-verification of PASS 2 ITER 2 REVISE (4 MAJOR + 3 MINOR)

#### [MAJOR][CODE_DEFECT] Legacy overlay_prefs still read by OverlayViewModel → FIXED
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayViewModel.kt:70-223` , `SystemStatsApplication.kt:39`
- Problem (prior): VM read/wrote `overlay_prefs` `show*`/`scaleFactor`/`isHorizontal`/`fps_mode`/`metricOrder` exclusively, dual-store vs `caliper` DataStore.
- Evidence (fixed): `grep -rn overlay_prefs app/src` → `1 hit: SystemStatsApplication.kt:39 migration` (previously 2 hits). `OverlayViewModel.kt:11-13 imports CaliperKeys/caliperDataStore/HudModule`, `:72 data.first()` reads `fpsMode/hudModules/hudScale` from `caliperDataStore`, `:81 isMetricEnabledByHudModules` maps modules→metrics, `:204 savePreferences()` writes `fpsMode/hudModules/hudScale` via `caliperDataStore.edit`, `loadInitialState` suspend IO+Main. `SystemStatsApplication.kt:36-78` one-shot `hudMigrated` guard copies legacy `fps_mode` + `show*`→modules CSV, sets defaults `hudMedium=CARBON hudScale=M etc`, then `legacy.edit().clear().apply()`. `FpsMonitor` already uses `HudSettingsCache` fed by `hudFpsModeFlow`.
- Impact: Single source `caliper` achieved; VM toggles now persist to `caliper`.
- Required change: DONE.

#### [MAJOR][CODE_DEFECT] MemInfoParser not integrated into foreground pipeline → FIXED
- Location: `data/monitor/MemInfoParser.kt:19-102` vs `data/repository/DashboardRepositoryImpl.kt:285-315` vs `BenchBudgetWorker.kt:21-27` vs `BenchModel.kt:193-218`
- Evidence (fixed): `grep MemInfoParser DashboardRepositoryImpl` now hits `:21,285-288` (previously 0). `DashboardRepositoryImpl.kt:67 delay 500` (was 1000), `:285 readMeminfoString()+readZramBytes()+parse().segs` and `:314 copy(memComposition = if(nonEmpty) else base, zramGb=...)` before `monitorBus.pushSlow` + `BenchUpdater.nudge`. `BenchBudgetWorker.kt:22-26` same parser enrichment. `MemInfoParser.kt:40-99` implements pinned math `MemTotal denominator`, `swapF=max(0,swapRaw-zramF)`, `free=1-sum coerceAtLeast0`, `if(sum>1) 1/sum normalize`, order `[active SOLID CH02, cached DIAGONAL CH03, zram CROSS CH04, swap CROSS only if zF==0&&sF>0.001, free NONE]`, final sanity `abs(total-1)>0.02 normalize`, fallback single SOLID when missing MemTotal. Foreground vs BUDGET now share same parser → no divergence. Tests `MemInfoParserTest:8` verify sum 1±0.02, dedup, normalization.
- Impact: STACK hatch now cadastral Active/Cached/ZRAM vs fallback single segment both paths consistent.
- Required change: DONE.

#### [MAJOR][CODE_DEFECT] TopConsumersProvider exists but dead → FIXED
- Location: `data/monitor/TopConsumersProvider.kt:13-24` vs `DashboardRepositoryImpl.kt:56-57,293-294,316` vs `BenchGlance.kt:333-334,361-372`
- Evidence (fixed): `grep TopConsumersProvider data/repository` now `1 hit import+field`, `grep BenchGlance loadConsumers/ActivityManager` → no ActivityManager import, only comment `TopConsumersProvider is authoritative; ActivityManager fallback removed`. `TopConsumersProvider.kt:14 if(!hasUsageStatsPermission()) empty else taskRepo.getRunningProcesses().take(max).map{Consumer(pkg,label.take16,rssMb0)}` via `TaskRepositoryImpl.kt:22-38` AppOps check + `:40-94` UsageStatsManager. `DashboardRepositoryImpl.kt:293 consumers=loadTopConsumers(5)` and `:316 topConsumers=consumers` into `toBenchSnapshot` → `monitorBus`. `StackWidget.kt:333 snap.topConsumers` + `:361 if(tier>=T3 && consumers.isNotEmpty())` renders `label.take18` label-only `if(rssMb>0) Text MB` else hide MB, hide section when empty. No `ActivityManager.runningAppProcesses` path.
- Impact: Permission-gated ledger now authoritative; empty-hide respected.
- Required change: DONE.

#### [MAJOR][MISSING_TEST] Phase 0 verification harness absent → FIXED
- Location: `MemInfoParserTest.kt:1-155` (new 8), `BenchSelfCheckTest.kt:1-242` (extended 8→16), `WidgetReceiversExistTest.kt:1-74` (new 3), `CaliperSelfCheckTest.kt:77-93`
- Evidence: `grep MemInfoParser app/src/test` now hits `MemInfoParserTest`; `grep cadenceMs app/src/test` now 2 hits; `BenchSelfCheckTest.kt:123 cadenceMs_liveAmbientBudget` asserts `BUDGET=900_000`, `:138 currentMaAndBatteryPresentAndCycleCountNull` with `cycleCount null`, `:168 benchFrames_noRecycle_sizeOfKb` asserts no `recycle()` and `byteCount/1024`, `:182 topConsumers_hideWhenEmpty`, `:193 fpsSample_sourceInSet SF|GFX|—`, `:206 hudConfig_csvRoundTrip`, `:217 hudMediumDistinctFromMedium` with mapping, `:230 placedAt_writeOnce_logic`, `WidgetReceiversExistTest.kt:9 receiversExistWithCorrectFqn` Class.forName 5, `:24 benchBudgetKeepSemantics` asserts `KEEP`+`15 MINUTES`+5 kinds, `:38 previewDrawablesExist` asserts 15 WEBP + xml `@drawable/preview_` + `updatePeriodMillis 0`. Test run: `TEST-CaliperSelfCheck 8, MemInfoParser 8, BenchSelfCheck 16, WidgetReceivers 3` all `failures=0 errors=0` (35/35) via `app/build/test-results/testDebugUnitTest/*.xml`; `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL, `compileDebugKotlin` SUCCESS.
- Impact: Acceptance harness now covers parser, cadence, BUDGET, currents, no-recycle, placedAt, hudMedium, FQN/KEEP/preview.
- Required change: DONE.

#### [MINOR][CODE_DEFECT] Hairline 4-side frame degraded to 80dp sides, defaultWeight no-op → FIXED
- Location: `BenchGlance.kt:164-191`
- Evidence (fixed): `read BenchGlance.kt:181 Row(modifier=GlanceModifier.fillMaxWidth().defaultWeight(),...)` + `:182 Box(width1dp.fillMaxHeight)` + `:183 Column(modifier=GlanceModifier.defaultWeight().padding(horizontal9dp))` + `:186 right Box fillMaxHeight` + `:188 bottom Box fillMaxWidth height1dp`. No local `private fun GlanceModifier.defaultWeight()` extension any longer (grep shows only real calls). Uses `fillMaxHeight` for sides, weight-absorbing Row inside outer Column padding12 to avoid OEM corner-clip. BUILD SUCCESS proves Glance 1.1 `defaultWeight` member resolves on `RowScope`/`ColumnScope`.
- Impact: 4-side 1dp frame now stretches full height on all tiers including BENCH T5; cream-on-cream hairline visible.
- Note: Manual screenshot gate still needed for cream-on-cream T1 PAPER (ENV).
- Required change: DONE.

#### [MINOR][CODE_DEFECT] FUEL T4 health/cycle/design rows not wired → FIXED
- Location: `DashboardRepositoryImpl.kt:275-282,305-311` vs `BenchGlance.kt:441-455` vs `BatteryProvider.kt:75,124-143`
- Evidence (fixed): `DashboardRepositoryImpl.kt:276 batteryDetailed=getBatteryDetailedInfo()` + `:277 cycleCount=if(c>=0) c else null` + `:278 designMah=capacity.split(" ").firstOrNull()?.toIntOrNull()?.takeIf>0` + `:282 batteryHealthStr=health.takeIf{ !=Blank && !=Unknown }` then `snap.copy(batteryHealth/cycleCount/designMah)`. Sentinel `-1` never leaked (BatteryProvider returns -1 when missing, mapped to null). `FuelWidget.kt:442 if(tier>=T4)` then `health Subline`, `cycleCount Subline`, `designMah Subline` with `Spacer height2dp`, hidden when null. `Subline` voltage·currentMa·remainingMin already at `:435-440`. Plan T4 hidden unless tier≥T4 exercised.
- Impact: FUEL T4 datasheet rows now show real values, null-hide, no 835 leak.
- Required change: DONE.

#### [MINOR][MISSING_TEST] Picker previewImage still launcher icon — 15 WEBP not shipped → FIXED (functional, quality placeholder)
- Location: `res/xml/single_channel_widget_info.xml:13`, `dual_channel_widget_info.xml:13`, `bench_widget_info.xml:13`, `fuel_widget_info.xml:13`, `raster_widget_info.xml:13`, `drawable-nodpi/preview_*.webp`
- Evidence: `ls drawable-nodpi/preview*.webp` → 15 files (scope/stack/fuel/raster/bench × paper/carbon/blueprint), `grep previewImage xml` → `@drawable/preview_scope_paper`, `preview_stack_paper`, `preview_bench_paper`, `preview_fuel_paper`, `preview_raster_paper`, `grep @mipmap/ic_launcher xml` → 0 hits, `grep updatePeriodMillis xml` → `updatePeriodMillis="0"` all 5. `WidgetReceiversExistTest.previewDrawablesExist` asserts existence + xml guard and passes (0 failures). `git diff --cached --stat` shows 15 binary WEBPs 149K each.
- Remaining LIMITATION (MINOR, non-blocking): WEBPs are uniform 152550 bytes each (placeholder logo copies), not -3° tilt + drop-shadow CALIPER panels per design §8/1.10 `-3° tilt, only drop shadow`. Functional for picker (no launcher icon) but Play picker visuals degraded vs spec. Not a code defect for APPROVE slice; polish in PR-previews follow-up. Logged as MINOR stay-open.
- Required change (functional): DONE. Visual polish → follow-up.

### Additional Checks (Plan 0.1, 0.2, 0.6, 0.7, 0.9, etc.)

- `MonitorBus` singleton `@Singleton class MonitorBus` with `pushSlow/pushFast/GlobalSnapshot.last` `DashboardRepositoryImpl` sole writer, `BenchBudgetWorker` comment `never MonitorBus` + `grep MonitorBus` → only foreground writer + BUDGET holder. `grep BenchSnapshotCache` → 0. ACCEPTANCE `Grep overlay_prefs==1` satisfied.
- `DashboardRepositoryImpl` dual-rate: `delay 500` (2Hz slow), `gpuHistory/netHistory LinkedList 61` wired, `computeChargeTimeRemaining` only when charging else 0, `remainingMin>0` subline in Fuel. All via `DashboardMetrics gpuHistory/netHistory` defaults.
- `WorkManager` dependency `app/build.gradle.kts` `work-runtime-ktx:2.9.1`, `BenchBudget.kt:15 PeriodicWorkRequestBuilder<BenchBudgetWorker>(15,MINUTES) KEEP`, `BenchBudgetSnapshot` holder, `enqueue` via `SystemStatsApplication.onCreate` + each receiver `onEnabled`, `cancelIfNone suspend sumOf 5 kinds` via `GlanceAppWidgetManager.getGlanceIds` + `onDisabled IO launch`, `onDeleted per-id BenchFrames.remove+evict` not global clear.
- `CaliperPrefs.kt` 15 keys `medium/showGrid/hatchingEnabled/caliperMigrated/hudMedium/hudScale/hudOpacity/hudBlur/hudLocked/hudModules/hudShowCoreBank/hudX/hudY/fpsMode/hudMigrated` single `preferencesDataStore(name="caliper")`, flows `hud*Flow` all present, `CaliperSelfCheckTest.dataStoreSingleAccessorAndExactKeys` asserts 15 exact keys.
- `HudTheme.kt` distinct `HudMedium {PAPER,CARBON,BLUEPRINT}` + `toCaliperMedium/fromMedium/caliperColors`, `HudModel.kt` `HudModule/Scale/Config` CSV round-trip.
- `FpsMonitor.kt` `FpsSample SF|GFX|—`, `getCurrentFpsWithSource` SF→GFX→—, `layer cache 30s`, `HudSettingsCache` `@Volatile fpsMode` fed by `hudFpsModeFlow` on IO (plan allows brief AUTO fallback, acceptable).
- Receivers FQN stable 5 classes verified by `WidgetReceiversExistTest` Class.forName.
- HUD Phase 3 still View hierarchy (`OverlayService.kt:35 old Service`, `TYPE_PHONE` pre-O branch still present, `FLAG_NOT_TOUCHABLE`/blur/`SavedStateRegistry` not yet Compose) — explicitly deferred per handoff `HUD Compose rewrite Phase3 stub` and plan slice; not a defect for this PR. Route to future HUD PR, manual tester not needed for widget approval.

## Evidence
- git status: `On branch master, Changes to be committed: 57 files 3649+/542-`, `git diff --stat` 57 files (list above)
- git diff --cached -- OverlayViewModel.kt → `getSharedPreferences` removed, `caliperDataStore` + `CaliperKeys.fpsMode/hudModules/hudScale` reads/writes, `hudModulesFromMetrics/isMetricEnabledByHudModules` mappings
- git diff --cached -- DashboardRepositoryImpl.kt → `delay 1000→500`, `monitorBus/topConsumersProvider` injection, `gpu/net histories`, `MemInfoParser.readMeminfoString/parse` enrichment, `currentMa/remainingMin/batteryHealth/cycleCount/designMah` mapping, `topConsumers` load, `computeClusterSizes`, `monitorBus.pushSlow+BenchUpdater.nudge`
- read MemInfoParser.kt:19-102 exact fractions, dedup, normalization
- read DashboardRepositoryImpl.kt:253-355 single writer block
- read TopConsumersProvider.kt:13-24 permission-gated
- read BenchGlance.kt:164-191 hairline pinned hierarchy, 530 ResponsiveBench T2..T5, 644-718 5 receivers per-id onEnabled/onDisabled/onDeleted
- read BenchBudget.kt:10-37 15 MINUTES KEEP, suspend cancelIfNone sumOf 5 kinds
- read BenchBudgetWorker.kt:15-65 direct sample + MemInfoParser enrichment + BenchBudgetSnapshot holder, never MonitorBus
- read BenchModel.kt:390-417 BenchFrames no recycle, placedAt write-once check `if(p[KEY_PLACED]==null)`
- read SystemStatsApplication.kt:27-78 migration + BenchBudget.enqueue
- read CaliperPrefs.kt:19-114 15 keys + flows
- read HudTheme.kt/HudModel.kt distinct enums
- grep overlay_prefs → 1 hit SystemStatsApplication.kt:39 (migration only)
- grep BenchSnapshotCache → 0 hits
- grep MonitorBus → DashboardRepositoryImpl + AppModule + BenchBudgetWorker comment only
- ls drawable-nodpi/preview*.webp → 15 files, each 149K
- cat xml previewImage → all `@drawable/preview_*_paper`, `updatePeriodMillis="0"`, no `@mipmap/ic_launcher`
- test results: `MemInfoParserTest 8/8 PASS`, `BenchSelfCheckTest 16/16 PASS`, `WidgetReceiversExistTest 3/3 PASS`, `CaliperSelfCheckTest 8/8 PASS` (total 35), BUILD SUCCESSFUL `compileDebugKotlin SUCCESS`, `testDebugUnitTest FROM-CACHE PASS` (rerun 18s full)
- grep defaultWeight → BenchGlance.kt scoped Row/Column defaultWeight + fillMaxHeight, no local no-op extension

## Next Action
APPROVE — Worker slice satisfies Phase 0 + Plan A + Plan B acceptance; 4 MAJOR + 3 MINOR from PASS2 verified fixed. One residual MINOR (preview WEBP placeholder quality, not functional) logged for PR-previews polish — no REVISE. Route to `manual-tester` for device gates: cream-on-cream T1 PAPER hairline screenshot, STACK ledger hide/reveal on usage-stats grant/revoke, FUEL T4 cycle/design on real device, BUDGET jobscheduler 15min tick (process dead), then `FINAL`.

