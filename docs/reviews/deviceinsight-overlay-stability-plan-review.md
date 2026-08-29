# Plan Review: deviceinsight-overlay-stability — PASS 1 ITER 1

## Verdict: REVISE
## Counts: CRITICAL 1 MAJOR 4 MINOR 0 SUGGESTIONS 0

### Findings
#### [CRITICAL] Blur mechanism has no viable owner or contract
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:37-38,92-93`
- Problem: Plan proposes `Window.setBackgroundBlurRadius(int)` in `OverlayWindowHost`. Current overlay is `WindowManager.addView(ComposeView, LayoutParams)` from `Service`, not an Android `Window`; host cannot call `Window` API. Plan also forbids `LayoutParams.blurBehindRadius` and screen capture. It defers API feasibility to worker, then allows disabling blur if proof fails, although acceptance requires API31+ bounded blur.
- Evidence: `app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:128-139,172-184` constructs `TYPE_APPLICATION_OVERLAY` `LayoutParams` and calls `WindowManager.addView`; no `Window` exists. Existing source only removes `FLAG_BLUR_BEHIND` at lines 190-196. Plan admits overlay blur semantics unverified at lines 92-93.
- Impact: Worker must rediscover whether requested effect is possible. Could build against nonexistent owner/API, ship no API31 blur, or violate no-screen-capture/no-display-wide-blur scope.
- Required planner change: State exact visual contract and exact compile-SDK-36 mechanism with owner. If requirement means backdrop blur beneath overlay, document supported overlay API proof or explicitly escalate/re-scope acceptance because `Service` overlay has no `Window`. If requirement means blur inside HUD panel, name composable/view layer and bounds, explain why it meets criterion, and define scrim fallback. Do not leave runtime feasibility decision to worker.

### Findings
#### [MAJOR] Pre-attach startup sequence lacks serialization and lifecycle cleanup
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:36,41`
- Problem: Plan says read snapshot on IO before attach, then start collectors after attach, but does not define service state machine across `onCreate`, repeated `onStartCommand`, permission loss during read, attach failure, and `onDestroy` while read continues. It also does not state when lifecycle/saved-state owners move to `RESUMED` or are destroyed if snapshot/read/attach fails.
- Evidence: Current `OverlayService` sets `isRunning` and lifecycle `RESUMED` before permission and attach (`app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:111-144`); duplicate starts return `START_STICKY` without attach guard (150-154); scope is Main-immediate (99); current startup collectors load position after attach (228-246).
- Impact: Duplicate starts or stop/permission transitions during preload can still attach two views, leak lifecycle/composition, or race stale snapshot into first layout.
- Required planner change: Specify serialized states and transitions: start FGS immediately; single startup job; `ATTACHING` blocks/reuses duplicate starts; cancel/check job before attach; recheck overlay permission on Main immediately before add; create/set view owners and mark lifecycle only for attach attempt; on every failed/cancelled path dispose composition/lifecycle once and clear references; publish `isRunning` only after add; collector ownership/cancellation and idempotent detach rules.

### Findings
#### [MAJOR] Shared snapshot plan omits FPS consumer and atomic persistence boundary
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:35-36,49-53`
- Problem: Plan calls for snapshot containing FPS mode and all HUD fields but does not name parser input type or route FPS mode to real runtime consumer. It also does not say all setters that alter related fields write atomically in one `DataStore.edit` when needed, versus current independently-debounced writes.
- Evidence: `OverlayService.hudConfigFlow()` omits `hudFpsModeFlow` (`app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:209-225`). `OverlayViewModel.loadInitialState()` independently builds config/FPS (`app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayViewModel.kt:85-101`). Actual FPS consumer is process-wide `HudSettingsCache`, separately collecting `hudFpsModeFlow` (`app/src/main/java/com/ivarna/deviceinsight/data/monitor/HudSettingsCache.kt:18-29`).
- Impact: Parser unification still leaves FPS mode outside first snapshot and service/VM/cache can observe different revisions. Acceptance requires first-frame persisted FPS mode and no mismatch.
- Required planner change: Define `Preferences`-to-`HudRuntimeConfig` function, including validated `FpsMode`; list all consumers (`OverlayService`, `OverlayViewModel`, `HudSettingsCache`/`FpsRepository`) and exact flow/snapshot each consumes. Define whether one DataStore transaction is required for reset/migration/default initialization, plus expected mixed-revision behavior for individual UI writes.

### Findings
#### [MAJOR] First app frame theme plan does not choose default or render gate
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:40,55,96`
- Problem: Plan requires persisted app medium before first visible app frame but leaves fresh-install medium ambiguous and gives no concrete gate between `MainActivity.setContent` and first themed content. Existing nullable flow deliberately selects system theme before DataStore emits.
- Evidence: `SettingsViewModel.medium` starts `null` (`app/src/main/java/com/ivarna/deviceinsight/presentation/settings/SettingsViewModel.kt:23-28`); `MainActivity` immediately passes it to `SystemStatsTheme` (`app/src/main/java/com/ivarna/deviceinsight/MainActivity.kt:36-46`); bridge maps null to `isSystemInDarkTheme()` (`app/src/main/java/com/ivarna/deviceinsight/presentation/theme/Theme.kt:12-19`). `SystemStatsApplication` only marks HUD migration on fresh install and does not establish `medium` (`SystemStatsApplication.kt:48-90`).
- Impact: Worker must choose product default and loading/splash behavior; current system-theme flash remains likely, and changing null semantics can break documented follow-system behavior.
- Required planner change: Choose and document fresh-install app-medium policy, including compatibility of existing absent `medium` values. Name exact first-frame solution: e.g. SplashScreen/render gate until first DataStore result, with deterministic fallback on read failure, then pass resolved non-null medium into `SystemStatsTheme`/`SystemStatsApp`. State test for no themed app content before resolution and immediate updates after resolution.

### Findings
#### [MAJOR] Static test scope contradicts existing valid layout code
- Location: `docs/plans/deviceinsight-overlay-stability-plan.md:42`
- Problem: Proposed source scan forbids production `MATCH_PARENT` without limiting scan to overlay/HUD files. Repository has `MATCH_PARENT` in widget configuration UI, unrelated to floating overlay and permitted by task.
- Evidence: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:264-265` uses `ViewGroup.LayoutParams.MATCH_PARENT`; plan line 42 lists global forbidden pattern. Acceptance bans full-screen overlay, not all app/widget layouts.
- Impact: Planned test fails baseline or drives unrelated layout changes.
- Required planner change: Scope source assertions to `OverlayService.kt`, new host, and HUD overlay composition, or assert `MATCH_PARENT` absent only in overlay `WindowManager.LayoutParams` construction. Likewise scope blur checks to production HUD paths, not unrelated UI.

## Next Agent: Planner
## Next Action: Revise plan with viable blur implementation/contract, serialized service startup state machine, FPS snapshot consumer path, deterministic app-theme first-frame policy, and scoped static tests.
