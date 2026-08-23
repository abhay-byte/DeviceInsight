# Plan Review: PLAN_caliper_field — PASS 1 ITER 1

## Verdict: APPROVE
## Counts: CRITICAL 0 MAJOR 0 MINOR 2 SUGGESTIONS 4

### Findings
#### [MINOR] Pin EXTRA_APPWIDGET_PREVIEW bitmap size / binder TX limit not sized
- Location: `docs/plans/PLAN_caliper_field.md:210-215` (Phase 2 pin) + `WidgetsSheet.kt:173-193`
- Problem: Preview bitmap decoded via `BitmapFactory.decodeResource(ctx.resources, previewResFor(kind))` for `AppWidgetManager.EXTRA_APPWIDGET_PREVIEW` is a full-resolution 15 WEBP asset (e.g. 280+ dp). Bundling an unscaled bitmap in the `extras` Bundle risks `TransactionTooLargeException` (>1 MB) on some OEM launchers; the plan does not size/downsample to the 72 dp strip size it proposes for the sheet UI.
- Evidence: `app/src/main/res/drawable-nodpi/preview_*_paper.webp` exist at ~152 kB compressed but decode to ARGB 8888 bitmaps ~ 2×–4× larger; `AppWidgetManager.requestPinAppWidget` docs note extras delivered via binder. Current repo `grep requestPinAppWidget` shows `null` extras (so limit never hit) — new path will hit it.
- Impact: Silent pin failure / OEM sheet no-preview on some devices; not a crash but field QA would miss preview.
- Required planner change: Add one line to plan Phase 2 snippet: downscale preview bitmap to ≤ 256×256 or `createScaledBitmap(... 120dp @ density)` before `putParcelable`, or note “if `TransactionTooLarge` catch, retry with scaled bitmap”. One sentence in implementation notes is enough to keep verdict APPROVE.

#### [MINOR] DashboardMetrics.netUpHistory extension left optional — acceptance for Network channel ambiguous
- Location: `docs/plans/PLAN_caliper_field.md:411` + `DashboardRepositoryImpl.kt:102-104,203` + `DashboardMetrics.kt:49`
- Problem: Plan adds `netUpHistory: LinkedList` in Dashboard loop as honest tx spark source, but marks “if QA slips the second list, show live ↑ readout without a fake hist.” DashboardMetrics currently has `netHistory: List<Float>` only (single rx hist at `DashboardMetrics.kt:49`). DashboardRepositoryImpl pushes only rx at `netHistory.add(traffic.rxBps.toFloat())` (`:203`). If worker adds new `netUpHistory` field, they must also extend `DashboardMetrics` and the Network channel page; if they skip, the Network page must degrade gracefully. The plan’s “optional” language leaves the worker to guess whether to add the field or show placeholder, and the acceptance at Phase 6 does not specify which branch is testable.
- Evidence: `DashboardMetrics.kt:48-49` has only `netHistory`; `DashboardRepositoryImpl.kt` history lists `gpuHistory, netHistory` (2), no `netUpHistory`. Plan Phase 6 table says “Add `netUpHistory` LinkedList … do not fake … If QA slips … show live ↑ readout without fake hist.”
- Impact: Low — honest fallback exists, but two valid worker interpretations cause test gap (either always add list or never add). Needs deterministic acceptance line.
- Required planner change: Pin one sentence: “Network channel Phase 6 acceptance = if `DashboardMetrics.netUpHistory` present, render dual spark (rx channel color + tx ink40); if absent, render single rx spark + live tx value row only — no fake curve. Worker MAY add the field in the same commit as channel pages, but compile must pass with either shape.” Keeps APPROVE.

### Suggestions
#### [SUGGESTION] HorizontalPager transitive dependency — clarify for worker
- Location: `docs/plans/PLAN_caliper_field.md:39,368`
- Problem: Plan states “`HorizontalPager` is on the BOM — no extra dep” but does not note that BOM only provides version, artifact is `androidx.compose.foundation:foundation` which is transitively present today via `material3`/`ui` (verified via `./gradlew :app:dependencies | grep foundation` showing `foundation:1.7.6`). New checkout without material3 would still need explicit `implementation("androidx.compose.foundation:foundation")`. The claim is true in this tree, but could mislead a worker who cleans dependencies.
- Evidence: `app/build.gradle.kts:107-163` has no explicit foundation dep; dependencies tree does show `foundation:1.7.6` via material3 path.
- Impact: None in current tree; future-proofing only.
- Required planner change: None required for APPROVE. Optional one-line note: “Verify foundation transitive; if missing, add `androidx.compose.foundation:foundation` explicitly — no version needed (BOM).”

#### [SUGGESTION] Channel pages duplicate NavHost — extract shared builder to avoid drift
- Location: `docs/plans/PLAN_caliper_field.md:388-422` + `SystemStatsApp.kt:200,210-222 and 251,261-272`
- Problem: `SystemStatsApp` has two `NavHost` trees (wide ≥600dp and narrow) with identical composable sets. Plan correctly lists “both NavHosts in SystemStatsApp.kt:200 and :251” but leaves worker to duplicate 6 new channel `composable(...)` entries. Drift between the two blocks is a recurring regression in this repo (seen in earlier m2 rail rework).
- Evidence: `SystemStatsApp.kt:200-221` wide host vs `:251-272` narrow host — both `NavHost(navController, startDestination=Dashboard)` with same 5 composables.
- Impact: Low — compile catches missing route in one branch only at runtime on specific width.
- Required planner change: Optional: suggest extracting `fun NavGraphBuilder.caliperGraph(onChannel: ...)` and calling it in both hosts, or add checklist item “add channels to both hosts”. Not blocking APPROVE.

#### [SUGGESTION] Activity-alias enable/disable race on first cold start
- Location: `docs/plans/PLAN_caliper_field.md:334-351,352-359`
- Problem: Plan’s alias helper enables new alias first then disables others with `DONT_KILL_APP`, and suggests an optional sync in `SystemStatsApplication` on first composition. If DataStore already holds CARBON/BLUEPRINT but default alias enabled in XML is Paper, there is a window where two aliases are briefly enabled (launcher may show duplicate icon) or Paper remains enabled until Settings writes. OEM launchers cache icons and may show stale Paper icon until reboot.
- Evidence: `AndroidManifest.xml:23-24` icon points to `@mipmap/ic_launcher` (adaptive); `mipmap-anydpi-v26/ic_launcher.xml:3-5` background `@color/ic_launcher_background` `#0B0B12` (will become `#F4F1E8` per Phase 4). No alias entries yet.
- Impact: Cosmetic duplicate icon for one frame; documented as MarginNote on Settings media per plan, so mitigated.
- Required planner change: None for APPROVE. Optional hardening: document that `SystemStatsApplication` sync should `first()` the mediumFlow before touching PackageManager, and never call `setComponentEnabledSetting` on the main thread without IO dispatcher.

#### [SUGGESTION] HudPanel header LED isolation — ensure child-only fast read
- Location: `docs/plans/PLAN_caliper_field.md:244-269` + `HudPanel.kt:42-76` + `HudModules.kt:41-60`
- Problem: Plan’s HudPanel split creates `HudFpsBand(fast)` as only full fast reader and `HudHeaderGate` that reads slow for clock. But header LED also needs `fast.isNoSignal()` at 10 Hz. If `HudHeaderGate` reads both `slow` and `fast` at its own level, the entire header recomposes at 10 Hz. The comment “Header LED may read `fast.isNoSignal()` in a 1-line child” captures the intent, but snippet `HudHeaderGate(slow, fast, ...)` suggests gate still holds both states. A stricter split is header clock wrapper + LED child wrapper.
- Evidence: `HudPanel.kt:44-45` currently `val s = slow.value; val f = fast.value` at root (10 Hz invalidates every band); `HudTheme.kt:97-102` barHDp exists but `HudAtoms.kt:161,209` defaults 6/12 dp ignore it; `HudDemo.kt:26-38` 500 ms demo ticks.
- Impact: Extra 10 Hz recompositions of header row only — not a crash; phase 3 FPS acceptance still passes if FPS band alone recomposes, but plan’s “FPS band only” wording is stricter.
- Required planner change: None for APPROVE. Optional clarification: show two wrappers `HudHeaderClock(slow)` and `HudLedDot(fast)` inside header, so header Row itself reads no state.


## 5-Axis Verification Summary
### 1. Architecture & Ownership — PASS
- Cited files verified real via `glob`/`read`: `BenchGlance.kt`, `BenchConfigActivity.kt`, `BenchModel.kt`, `WidgetsSheet.kt`, `OverlayScreen.kt`, `OverlayViewModel.kt`, `HudPanel.kt:61,67`, `HudModules.kt`, `HudAtoms.kt`, `HudTheme.kt`, `HudDemo.kt`, `OverlayService.kt:334`, `HardwareScreen.kt`, `HardwareViewModel.kt`, `CpuTab.kt`, `GpuTab.kt`, `DashboardScreen.kt`, `SystemStatsApp.kt:89-98,200,251`, `CaliperData.kt:89-92`, `DashboardMetrics.kt`, `AndroidManifest.xml:41-47`, `mipmap-anydpi-v26/ic_launcher*.xml`, `values/ic_launcher_background.xml`, `ic_tile_caliper.xml`, `SocLogoRepository.kt`, `GpuLogoRepository.kt`, assets `soc_*.png`/`gpu_*.{jpg,jpeg,webp}`. No invented files; `presentation/dashboard/channels/` correctly marked missing. Ownership respects layering (Glance widgets vs Compose UI vs data/repository vs service).

### 2. Data / Control Flow & API Contracts — PASS
- Glance config contract (`setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_APPWIDGET_ID, id))`, `GlanceAppWidget.update`, `widgetFeatures=reconfigurable`, no `configuration_optional`) matches official webfetch pins 2026-08-08. `ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)` + `actionStartActivity<BenchConfigActivity>` + `GlanceAppWidgetManager.getAppWidgetId/getGlanceIdBy/getGlanceIds` verified via grep (WidgetsSheet uses `getAppWidgetId`). `BenchState` keys `KEY_FOLLOW/KEY_MEDIUM/KEY_CADENCE` exist at `BenchModel.kt:403-425`. `resolvedMedium` logic honored. `DashboardRepositoryImpl` history size 61 ≈60 s supports 30s/60s timebase pin.

### 3. Lifecycle & Threading — PASS
- F2 400 ms re-check moved into `ON_RESUME` observer with `rememberCoroutineScope + delay(400)` correctly replaces `LaunchedEffect(hasOverlay)` stale key. `OverlayViewModel` now reads `OverlayService.isRunning.get()` in `loadInitialState`/`checkPermissions` (AtomicBoolean). Lock DIP wired via `viewModel.setLocked`. Opacity debounce `opacityJob` on `viewModelScope` with `Dispatchers.IO` + `delay(150)` isolates DataStore writes from UI state. HudPanel split isolates 10 Hz fast to `HudFpsBand` only. No `runBlocking` on main; `mediumFlow.first()` via `lifecycleScope.launch`. All dispatchers explicit.

### 4. Persistence / Storage & Error Handling — PASS
- `caliper` DataStore remains single source (no `overlay_prefs` dual store; migration only in `SystemStatsApplication.migrateOverlayPrefs`). `KEY_CADENCE`/`KEY_PLACED` write-once semantics preserved. `BenchUpdater.nudge` empty catch replaced with `Log.w`; `cachedIds` empty-map guard avoids 30 s freeze. `BandBitmap` keys now include `snap.timestamp` for hatch/fuel/rail. Play policy honored: no exact alarm, no widget FGS, 15 min BUDGET floor. Error toasts via `MarginNote`, never crash. Icon alias never leaves zero LAUNCHERs (enable-before-disable).

### 5. Compatibility & Testing & Edge Cases & Scope — PASS
- `minSdk 26 target 36 compileSdk 36` (`app/build.gradle.kts:17,47-48`) consistent with APIs used (`requestPinAppWidget` API 26, monochrome API 33 safely ignored, `FLAG_BLUR_BEHIND` API 31 guarded, `reconfigurable` API 31). Testing: per-phase acceptance is device-testable and objective (monospace footers, preview theft check, START within ~1 s on API 26–27, clip/spacedBy/heightIn, launcher monochrome vs wallpaper tint, tab switch jank, tap→ channel pages). Edge cases covered: null preview extras fallback, OEM broadcast dropped (delay+ON_RESUME primary), launcher cache reboot note, generic logo fallback, no fake hist, transaction limit (minor). Scope minimal: no recreation of Phase 0 bus/parser/WM/receivers, no `providePreview`, no IBM Plex in Glance, no `Modifier.blur`, no fake GPU freq.

## PASS 2 Regression Check
- Previous review `docs/reviews/PLAN_widgets_overlay-impl-review-PASS5.md` (REVISE, 0C/6M/3m) re-checked. All 6 MAJOR and 3 MINOR items are landed as Phase 0 in this plan exactly as reviewer prescribed: `BenchGlance.kt:691,693,731,733` Monospace, `BenchConfigActivity.kt:48-55,110,119` gate + theme + 5 s snap, `OverlayScreen.kt:44-56` 400 ms on ON_RESUME + `OverlayViewModel.kt:77,165-189` isRunning, lock DIP at `:131`, `HudPanel.kt:61,67` clip/spacedBy/heightIn. No regression; file:line citations re-verified against current tree. No reopening of “Already correct” items (F1 split, W1 T2 Row defaultWeight, WRAP_CONTENT, etc.).

## Next Agent: Worker
## Next Action: Implement phases in canonical order 0→6 per §9; stop at end of each phase if `./gradlew :app:compileDebugKotlin` red. Do not start Phase 6 until Overview tap→ can land without invented Hardware wrappers. No product code was written in this review pass (read-only).

