# Plan Review: PLAN_widgets_overlay — PASS 4 ITER 4

## Verdict: APPROVE
## Counts: CRITICAL 0 MAJOR 0 MINOR 2 SUGGESTIONS 2

### Findings
#### [MINOR] Glance defaultWeight scope-receiver ambiguity in BenchPanel snippet
- Location: docs/plans/PLAN_widgets_overlay.md:512-524, app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt:181-188
- Problem: Plan pins `Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight())` and `Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 9.dp))`. Real Glance 1.1.0 defines `defaultWeight()` as member of `RowScope`/`ColumnScope` (verified in plan §0 Research via `javap RowScope/ColumnScope` 477-class AAR), not as `GlanceModifier` extension. Staged `BenchGlance.kt:181` compiles because it calls `GlanceModifier.defaultWeight()` outside scope (fallback no-op extension at earlier commit was removed) — on strict 1.1.0 this would not compile without `with(RowScope){}`. Impl-review PASS2 already flagged no-op `fun GlanceModifier.defaultWeight():GlanceModifier=this` at `BenchGlance.kt:59` and side Boxes `height(80.dp)` vs `fillMaxHeight`; staged PASS4 code now uses `fillMaxHeight` but still `GlanceModifier.defaultWeight()` on Row/Column.
- Evidence: `read BenchGlance.kt:181-183` shows `Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight())` and `Column(modifier = GlanceModifier.defaultWeight()...)`; plan Research `javap -p GlanceAppWidget` only `provideGlance`, `grep -rl providePreview 0 matches` confirms API research method; same method should be cited for `RowScope.defaultWeight` but snippet assumes extension.
- Impact: Low — worker will discover at compile and wrap with scope receiver (`RowScope.defaultWeight()` / `with(RowScope){ GlanceModifier.defaultWeight() }`). Panel already 4-side with 12dp outer + 9dp inner = 114dp usable T1 width, passes cream-on-cream legibility; no data loss. Documented as known limitation in impl-review MINOR.
- Required planner change (optional): Amend §1.2 snippet to scope-aware form: `Row { Box(GlanceModifier.width(1.dp).fillMaxHeight()...) }` inside `RowScope` where `GlanceModifier.defaultWeight()` is invoked with receiver, or `Row(modifier = GlanceModifier.fillMaxWidth().weight(1f))` style. Add note: if member-only limitation prevents compile, verify screenshot on BENCH T5 280dp bottom gap. Not blocking APPROVE.

#### [MINOR] Config preview initial medium flicker — getInitialMedium still PAPER
- Location: docs/plans/PLAN_widgets_overlay.md:613-615, app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:72-79
- Problem: Plan W10 says replace two-Text `PreviewPanel` with real CALIPER `OdometerText`/`ScopeTrace`/`HatchBar`/`LinearGauge` inside `CaliperTheme(medium)` and read `mediumFlow.first()` via `lifecycleScope.launch + mutableStateOf` before `setContent`, not `runBlocking` on main. Staged `BenchConfigActivity.kt:72-79` still `getInitialMedium() = runBlockingMedium() -> PAPER` (hard-coded). Plan's primary path (`mediumFlow.first()` + state) will show PAPER for one frame then update, causing preview theme flicker PAPER→CARBON on dark device. Not a crash but violates §3 three-media preview fidelity.
- Evidence: `read BenchConfigActivity.kt:72-79` `private fun getInitialMedium(): Medium { return try { runBlockingMedium() } ...} private fun runBlockingMedium(): Medium { return Medium.PAPER }`; `read CaliperPrefs.kt:39-42` `mediumFlow: Flow<Medium?>` exists; plan §1.7 correctly cites not to use `runBlocking`.
- Impact: Minor UX — preview shows wrong medium until DataStore collects; no data loss; worker can seed via `runBlocking { dataStore.data.first() }` on IO or pass initial medium as extra from pin request. Not blocking.
- Required planner change (optional): Clarify acceptance: preview may flicker one frame or gate `setContent` behind `lifecycleScope.launch { val m = mediumFlow.first() ?: PAPER; setContent{ CaliperTheme(m)...}}` with splash. Keep current W10 pin; add `benchDemoSnapshot` fallback age <5s already.

#### [SUGGESTION] Add regression test for BENCH chunked(2) vs chunked(3)
- Location: docs/plans/PLAN_widgets_overlay.md:608-609, app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt:595
- Problem: W7 correctly identifies `chunked(3)` = 3 columns bug vs WD 2×3. Staged `BenchGlance.kt:595-598` still `allChannels.chunked(3)` with per-tile `CH-05` hatch / `CH-03` spark only. No unit guard. Easy to regress after fix.
- Evidence: `read BenchGlance.kt:595` `val chunked = allChannels.chunked(3)`; widgets.md §4 WT-05 T2 `2×3 tiles`, WD visual spec.
- Impact: No blocking; testing harness already has `BenchSelfCheckTest` for palette/mapping; add one assertion `assert(chunked(2).size == 2)` or layout screenshot gate.
- Required planner change: None required; worker may add `BenchSelfCheckTest.tileColumns` check as nice-to-have.

#### [SUGGESTION] Extract navGraph lambda for dual NavHost calibrate/hud-config routes
- Location: docs/plans/PLAN_widgets_overlay.md:1071, presentation/SystemStatsApp.kt:88-96, 213
- Problem: Plan correctly notes `calibrate`/`hud-config` must be edited in **both** NavHost blocks (wide vs narrow) or extract `navGraph` lambda. Staged `SystemStatsApp.kt` still duplicates NavHost. Easy to miss second host when wiring `di_route`.
- Evidence: `read SystemStatsApp.kt` two `NavHost` definitions; `grep di_route` maps CH-01..CH-06 → tabs; `OverlayComponents.kt` dead.
- Impact: Low — manual QA will catch missing route on tablet; no data loss.
- Required planner change: None required; worker may do small refactor as pinned.

### Prior Findings Resolution (PASS 1→2→4)
All 3 CRITICAL + 6 MAJOR from PASS1 verified FIXED in PASS2 (see docs/reviews/PLAN_widgets_overlay-PASS2-review.md:24-39). PASS4 does not reopen them; PASS4 adds field bugs F1/F2/F3 and widget layout W1–W10 + overlay not-done, all pinned:

| ID | Severity | Status in PASS4 | Evidence |
|---|---|---|---|
| HUD persistence dual-store / migration | CRITICAL | FIXED, not regressed | CaliperPrefs.kt:25-35 `hud*` 11 keys + SystemStatsApplication.kt:27-79 one-shot migration `hudMigrated` + legacy `overlay_prefs` clear; grep `overlay_prefs` now 1 hit migration comment only |
| Single-writer bus vs BUDGET sampler | CRITICAL | FIXED | MonitorBus.kt:10-35 `pushSlow` single writer, BenchBudgetWorker.kt:17 direct `sample→update` never MonitorBus, DashboardRepositoryImpl.kt:246 `monitorBus.pushSlow` only foreground |
| MemInfoParser fractions | CRITICAL | FIXED | MemInfoParser.kt:12-99 exact `MemTotal` denominator, `swapF=max(0,swapRaw-zramF)`, `freeF coerceAtLeast 0`, normalization if sum>1, order SOLID/DIAGONAL/CROSS/NONE; used by DashboardRepositoryImpl.kt:268-282 and BenchBudgetWorker.kt:21 |
| FpsTicker threading/backoff | MAJOR | FIXED in plan, partially in code | Plan §0.2 pins `Dispatchers.IO` SupervisorJob owned by OverlayService, adaptive 100→1000ms after 5×"—", layer cache 30s; staged FpsMonitor.kt:16 `HudSettingsCache` + `cachedLayerName` 30s TTL + `FpsSample` SF→GFX→"—"; FpsTicker hosting deferred to Phase 3 (acceptable per handoff) |
| WM enqueue/cancel race | MAJOR | FIXED | BenchBudget.kt:10-36 `suspend cancelIfNone` sumOf 5 kinds atomically, KEEP policy; receivers BenchGlance.kt:655-718 per-id onEnabled/onDisabled IO launch |
| TopConsumersProvider | MAJOR | FIXED in plan, wired in code | TopConsumersProvider.kt:13 `hasUsageStatsPermission()` gate; DashboardRepositoryImpl.kt:290 `loadTopConsumers(5)`; StackWidget BenchGlance.kt:334 `snap.topConsumers` hide when empty |
| WidgetsSheet refresh lifecycle | MAJOR | FIXED | WidgetsSheet.kt:28-50 DisposableEffect ON_RESUME + LaunchedEffect + `requestPin` delay 1200; SettingsScreen.kt:60-64 ON_RESUME observer |
| Hairline T1 | MAJOR | FIXED (4-side done) | BenchGlance.kt:164-191 pinned hierarchy `Box(fillMaxSize.bg){Column(pad12){1dp top; Row(defaultWeight){1dp left; Column(defaultWeight pad9){content};1dp right};1dp bottom}}`; acceptance T1 PAPER screenshot |
| HudMedium vs widget followSystem | MAJOR | FIXED | HudTheme.kt:13 distinct `HudMedium {PAPER,CARBON,BLUEPRINT}` + `toCaliperMedium/fromHudMedium`; no followSystem |
| providePreview | MINOR | FIXED | Verified absent via AAR 477 classes grep 0; no implementation; 15 WEBPs staged drawable-nodpi/preview_*.webp |
| BenchFrames recycle + onDeleted global clear | MINOR | FIXED | BenchModel.kt:390-399 no `entryRemoved recycle()`, sizeOf /1024; BenchGlance.kt:662-666 per-id remove/evict |
| F1 nested verticalScroll crash | CRITICAL (PASS3) | Plan correct, code not yet landed — worker action required | SettingsScreen.kt:69-73 outer `Column(fillMaxSize().caliperGrid().verticalScroll)` hosts WidgetsSheet.kt:58 `Column(fillMaxSize.verticalScroll)` → `checkScrollableContainerConstraints` throw; plan §2.0 pins outer not scrollable, sibling branch scroll |
| F2 START without permission | MAJOR (PASS3) | Plan correct | OverlayScreen.kt:152-163 `HardKey("START", enabled=hasOverlay)` visible disabled; plan §3.3.a `when {running->STOP; hasOverlay->START; else->Unit}` + service `startForeground` then `stopSelf` |
| F3 demo HUD cramped | MAJOR (PASS3) | Plan correct | CaliperHud.kt:46 `fillMaxWidth`, `:87 height(16.dp)`, `StampBadge` bordered, `drawBehind` brackets inset==padding, `Modifier.blur(8.dp)` bleed; tester PNG docs/testers/caliper-001/caliper-001-03-overlay.png; plan §3.3.b wrap-to-scale 196/260/300, `heightIn(min 22.dp)`, `spacedBy(6.dp)`, no blur |
| W1–W10 widget layouts vs WD | MAJOR (PASS4) | Correctly audited | Table matches tree: SCOPE stacked not Row split, y-labels missing, STACK header LIVE not %, FUEL no secondary %, RASTER live missing, BENCH chunked(3), no Monospace, dead desc, two-Text preview; plan pins WD visual over WI sketch, safe pins (no 16dp T1, no fake hist) |
| Overlay window vs DI-HD-001 | CRITICAL (PASS4) | Correctly flagged | HudPanel/HudAtoms/HudModules/HudDemo missing; HudTheme.kt 34 lines enums only; OverlayService still Views rounded card |

No regressions detected. Plan correctly marks DONE vs TODO (five Glance instruments, 4-side hairline, BENCH T2+, 15 picker WEBPs, 3 media Blueprint, cadence/WM, SIGNAL LOST, NOT FITTED/CHANNEL LOCKED, composition hatch, remainingMin, Settings copy as DONE).

### 5-Axis Verification Summary
**1. Architecture & Ownership:** PASS. Correct authoritative files/modules identified, pinned package `com.ivarna.deviceinsight.ui.caliper.widget` retained (manifest receivers `SingleChannel/DualChannel/Fuel/Raster/Bench` FQNs), service `OverlayService` retained not new `HudService`. Missing files `HudPanel/HudAtoms/HudModules/HudDemo` correctly listed vs existing `CaliperHud.kt` stub. Verifies via `read`/`glob`.

**2. Data / Control Flow & API Contracts:** PASS. Glance 1.1.0 contracts verified (table `updateAppWidgetState`/`getAppWidgetState` vs sketch `widget.updateState`/`currentState`), `BandBitmap` + `LaunchedEffect` forbidden correctly, `isSystemInDarkTheme` not Glance API correctly, `actionStartActivity` with `di_route` correct, `FontFamily.Monospace` only (custom Plex not supported) per 2026-08-20 build-ui docs. Intent extras/DataStore keys enumerated.

**3. Lifecycle & Threading:** PASS. Correct dispatchers (Default for bitmap, IO for FpsTicker 10Hz, Main for compose), lifecycle owners for Overlay ComposeView (`SavedStateRegistryController` + `ViewTreeLifecycleOwner`), rotation/process death via DataStore + WorkManager BUDGET 15min, cancellation via `isRunning` AtomicBoolean. F1 nested scroll correctly diagnosed via `checkScrollableContainerConstraints`.

**4. Persistence / Storage & Error Handling:** PASS. Single source `caliper` DataStore, `CaliperKeys.hud*` 11 keys, one-shot migration in `SystemStatsApplication.onCreate` with `runCatching` not crashing startup, `overlay_prefs` cleared, `placedAt` write-once, no `Bitmap.recycle()`, `/proc/meminfo` fallback, retry/backoff for dumpsys, permission `SYSTEM_ALERT_WINDOW`, FileProvider not needed, scoped storage not affected.

**5. Compatibility & Testing & Edge Cases & Scope:** PASS. minSdk 26 vs API 31 blur-behind handled with opacity fallback + disabled DIP, `WRAP_CONTENT` overlay per handstandsam gist not MATCH_PARENT, WorkManager `work-runtime-ktx` AOSP-friendly. Testing strategy lists unit + manual (MemInfoParser fixtures, toBenchSnapshot mapping, stale/tier, cadence BUDGET 900000, FpsSample source, HudConfig CSV, receivers FQN). Edge cases covered (empty hist NO SIGNAL, free negative, swap dedup, 0 MB RSS hide, fake 835 never). Scope minimal: explicitly out-of-scope per-core load, dossier, per-app HUD, renaming, old overlay flag.

## Next Agent: Worker
## Next Action: Execute from docs/plans/PLAN_widgets_overlay.md PASS 4 in order: F1 crash fix (SettingsScreen scroll split) first commit, then widget layouts W1–W10 (SCOPE Row split W1, y-labels W2, STACK % W3, labels W4, FUEL % W5, RASTER subline W6, BENCH chunked(2) W7, Monospace W8, semantics W9, config preview W10), then overlay F2/F3 (START gating + wrap-to-scale preview host) and HudPanel/service rewrite per Phase 3. Do not recreate Phase 0 / hairline / 15 WEBPs.
