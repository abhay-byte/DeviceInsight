# Plan Review: di-wf-001-widget-parity — PASS 1 ITER 0

## Verdict: APPROVE

## Counts: CRITICAL 0 MAJOR 0 MINOR 3 SUGGESTIONS 3

Every architectural claim in the plan was re-verified against repo evidence and the pinned Glance 1.1.0 artifacts (javap of AARs from Gradle cache). All six handoff validation points check out. Findings below are non-blocking; none breaks correctness, contracts, or scope.

## Handoff Items — Verification Results

| # | Claim | Result | Evidence |
|---|---|---|---|
| 1 | D2: BenchPanel horizontal inset = 44dp total (not 24) | **VERIFIED** | `BenchGlance.kt:204-218`: Column `.padding(12.dp)` → hairline Box `width(1.dp)` → inner Column `.padding(horizontal = 9.dp)` = 22dp/side ⇒ **44dp**. Current `BandBitmap` fallback `(tier.wDp - 24)` (BenchGlance.kt:172) indeed over-renders vs truth — D2 fix justified |
| 2 | SizeMode.Exact ⇒ real cell size + provideGlance re-invocation on resize | **VERIFIED** | javap glance-appwidget-1.1.0: `SizeMode$Exact.INSTANCE` exists; `GlanceAppWidgetReceiver.onAppWidgetOptionsChanged(Context, AppWidgetManager, int, Bundle)` present ⇒ resize triggers update path. `GlanceRemoteViews$compose$2$1.class` references `LocalSize` ⇒ compose() provides LocalSize from passed DpSize (critical for F6 capture correctness since new BandBitmap reads `LocalSize.current`) |
| 3 | GlanceRemoteViews.compose signature + result.remoteViews | **VERIFIED** | javap: `compose-YuIfr8w(Context, long/*DpSize*/, Object/*stateDef?*/, Bundle, Function2, Continuation): RemoteViewsCompositionResult`; `RemoteViewsCompositionResult.getRemoteViews(): android.widget.RemoteViews`. `RemoteViews.apply(ctx, FrameLayout)` → measure/layout/draw to ARGB_8888 is standard View pipeline. densityDpi=480 ⇒ density 3.0 matches "tier×3 px" matrix |
| 4 | InstrumentBody lift preserves ALL branches | **VERIFIED** | Enumerated in current code: Fuel not-fitted (:527-537), Raster not-fitted (:610-628), Raster gpuRootLocked (:629-655), calibrating branches (:364,:400,:482,:564), tier branching T1..T5 (:377,:416,:560,:576,:742), consumers rows (:486-498), TileBitmap CH-01..CH-06 (:319-351), ledger-vs-tiled split (:697/:715). Receivers (:777-840) + `BenchUpdater` (:97-154) untouched per plan. All 17 BandBitmap call sites enumerated correctly in F3 (grep-confirmed: Scope spark/scope/thermal/rail, Stack hatchBar/memSpark, Fuel fuel/wattTrace, Raster gpuSpark/lockedField, 6 tiles + benchRail) |
| 5 | F8 feasibility | **VERIFIED with 1 gap (MINOR-3)** | `applicationId = "com.ivarna.deviceinsight"`, NO suffix (build.gradle.kts:46) ⇒ `am start -n com.ivarna.deviceinsight/.ui.caliper.widget.PreviewStudioActivity` correct; shell holds START_ANY_ACTIVITY so exported=false is startable via adb. Guard-test compatibility confirmed: `previewDrawablesExist` (WidgetReceiversExistTest.kt:38-73) asserts exact webp filenames exist AND XML contains `@drawable/preview_` — keeping webps + naming PNGs `preview_*_paper_*` satisfies both. Gap: see MINOR-3 (run-as extraction) |
| 6 | No public API breaks | **VERIFIED** | 5 receiver classes keep FQNs (BenchGlance.kt:777,790,803,816,829; manifest :114-172); `benchBudgetKeepSemantics` requires literal class names ScopeWidget/StackWidget/FuelWidget/RasterWidget/BenchWidgetAll in BenchBudget.kt (:34) — plan keeps them, only swaps sizeMode. Existing `BenchSelfCheckTest.tierOf` (:77-84) passes under F1's exact-bounds formula (all 6 assertions hold without tolerance). BenchState/BenchBudget/BenchUpdater untouched |

Additional spot-checks that passed: Glance version pinned `androidx.glance:glance-appwidget:1.1.0` (build.gradle.kts:136); `TextKt.Text(String, GlanceModifier, TextStyle, int maxLines, …)` — maxLines EXISTS; `ImageKt.Image(provider, desc, modifier, contentScale:int, colorFilter)` + `ContentScale.Companion.getFillBounds` — both EXIST (plan line 19 citations exact); `RowScope.defaultWeight()` exists. All cited Caliper symbols exist at the cited lines: CaliperTheme (:160), Masthead (CaliperChrome.kt:42), ScreenHeader (:264), PanelCard (CaliperData.kt:60), HardKey (CaliperPrimitives.kt:147), EndOfSheet (:91), Modifier.caliperGrid (CaliperDraw.kt:90), `Caliper.type.dataS` (CaliperTheme.kt:131). Panel hexes Paper `0xFFFBF9F3` (:78) / Carbon `0xFF1C1B17` (:87) exact; ink60 hexes available at :79 (`0x99191713`) / :88 (`0x99EDE7DA`) for F4. `Tier.of -20` tolerance confirmed at BenchModel.kt:35. Repo state matches plan assumptions: no `src/debug`, no `res/layout`, `values/colors.xml` exists, no `values-night/`, 15 webps in drawable-nodpi, `widget_desc_*` strings at strings.xml:144-148, all 5 XMLs use `glance_default_loading_layout` + `android:configure=BenchConfigActivity`. All BenchArt Canvas extensions referenced by BandBitmap bodies exist (hatchBar/spark/scope/fuelGauge/wattTrace/coreRailRows/thermalRamp/lockedField/calibrating, renderSync at BenchArt.kt:19).

wSp damping math sanity: eff = base/(1+(fontScale−1)·0.35) → rendered size at fontScale=1.3 is ≈1.18×base (vs 1.3× raw) — monotonic, damped, identity at 1.0. Sound.

## Findings

#### [MINOR][PLAN_GAP] F9 boundary-test enumeration contradicts F1's own formula
- Location: `docs/plans/di-wf-001-widget-parity-plan.md:116` vs `:59-62`
- Problem: "(139→T1,140/141→T1/T2 edges,279/280→T2,…)" is wrong under the new exact-bounds `Tier.of`: T2 requires width ≥ 280, so `of(141,·)=T1` and `of(279,·)=T1`; only 280 crosses into T2. Literal encoding of the parenthetical produces failing assertions.
- Evidence: F1 snippet (plan:60) `entries.lastOrNull { wDp >= it.wDp && hDp >= it.hDp } ?: T1`
- Impact: One wasted test iteration if worker transcribes literally; no shipped-code risk (formula itself is correct).
- Required change: Derive expectations from F1, e.g. `139→T1, 140×140→T1, 279×140→T1, 280×140→T2, 280×209→T2, 280×210→T3, 349×280→T4, 350×280→T5, 1000×1000→T5`.

#### [MINOR][PLAN_GAP] Parameter-name mismatch between D5 and F2 dispatcher
- Location: plan:41 (`stateKeyHint`) vs plan:69-72 (`awId`)
- Problem: Same slot named two ways across sections.
- Impact: Cosmetic ambiguity only; F2 snippet is authoritative.
- Required change: Use `awId` consistently (it feeds `openConfig(awId)` inside lifted bodies).

#### [MINOR][PLAN_GAP] F8 file extraction lacks run-as mechanics
- Location: plan:110 ("adb pull files/previews")
- Problem: PreviewStudio writes to app-internal `filesDir/previews` (/data/data/com.ivarna.deviceinsight/files/previews); plain `adb pull <relpath>` cannot read it even for debuggable builds.
- Evidence: Android debuggable-app access requires `adb exec-out run-as com.ivarna.deviceinsight cat files/previews/<f> > <f>` or run-as cp to /sdcard then pull.
- Impact: Worker improvises one command; low risk but plan claims an executable workflow.
- Required change: Specify `run-as com.ivarna.deviceinsight` extraction (or have the activity copy to external files dir).

#### [SUGGESTION] Scope split-trace width constant
- True half-width in the split Row is (W−44−6)/2 = W/2−25 (6dp spacer at BenchGlance.kt:393); plan's `W/2−26` (plan:85) is 1dp conservative. Fine given the FillBounds seatbelt; optional exactness pass.

#### [SUGGESTION] maxResizeWidth/maxResizeHeight are API 31+ attributes
- Ignored below API 31 (harmless; minSdk 26). Don't chase resize-bound differences during A5 on the RMX1931 if it reports older APIs; `resizeMode="horizontal|vertical"` already permits resizing.

#### [SUGGESTION] Existing demo snapshot in BenchConfigActivity
- `BenchConfigActivity.kt:423 benchDemoSnapshot(kind)` + `PreviewPanel` (:220) already fake data for config previews. Note whether `BenchDemo.previewSnapshot()` intentionally diverges (deterministic/hash-stable) or should align, to avoid two drifting fakes.

## Next Agent: Worker

## Next Action: Start implementation at F1. Apply the three MINOR corrections inline while implementing (F9 expectations derived from F1 formula; unify param name to `awId`; use `run-as` for PNG extraction). Planner may opportunistically patch plan lines 41/110/116, but no re-review cycle is needed for these.
