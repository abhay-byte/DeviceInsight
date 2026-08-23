# Impl Review: PLAN_caliper_field — PASS 1 ITER 1 (worker vs DI-PLAN-003)

## Verdict: REVISE
## Counts: CRITICAL 1 MAJOR 1 MINOR 3 NIT 2

Phases 0–6 are in the tree and match the plan’s shape: 4-way MEDIA SegKey with `BenchState.config` preload, Glance root/masthead → `BenchConfigActivity`, Instruments BACK-first + pin extras ≤256 px, HudPanel root reads zero feeds, Paper/Carbon/Blueprint aliases enable-first, pager + Coil logos, six channel routes in both NavHosts, `netUpHistory` honest tx. `./gradlew :app:compileDebugKotlin` succeeded. Two claimed-DONE pins fail: Calibrate CANCEL drops `EXTRA_APPWIDGET_ID` (ghost widget on first bind), and Raster channel hero double-scales `snap.gpuPct`.

### Findings

#### [CRITICAL][CODE_DEFECT] Calibrate CANCEL drops EXTRA_APPWIDGET_ID → first-bind ghost widget → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:75`
- Problem: Worker claimed “RESULT_CANCELED always carries id”. `onCreate` correctly does `setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_APPWIDGET_ID, appWidgetId))` at :52 and :60, but the CANCEL HardKey overwrites that with `setResult(RESULT_CANCELED)` and no extras. Official Glance 2026-08-06 contract: the activity must always return the id, including cancel. System BACK still uses the extras-bearing result; the labeled CANCEL does not.
- Evidence: `:75` `onCancel = { setResult(RESULT_CANCELED); finish() }`. SAVE/SKIP go through `saveConfig` which sets `RESULT_OK` + extra (`:112-114`). Invalid-id early-return at `:52` does include the extra.
- Impact: First-bind CANCEL leaves a ghost home-screen widget. Phase 1 acceptance (“CANCEL/back during first bind does not leave a ghost widget”) fails on the CANCEL path.
- Required worker change: `BenchConfigActivity.kt:75` — `onCancel = { setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)); finish() }`. Do not call bare `setResult(RESULT_CANCELED)`. Leave SAVE/SKIP and the onCreate default result alone.

#### [MAJOR][CODE_DEFECT] Raster channel hero double-scales snap.gpuPct (4100% vs 41%) → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/channels/GpuChannel.kt:45`
- Problem: `val pct = (snap.gpuPct?.times(100f)) ?: metrics.gpuUsage * 100f`. `BenchSnapshot.gpuPct` is already 0–100 (`BenchModel.kt:227` `gpuUsage * 100f`; sampler `:329` same). Glance RASTER hero uses `snap.gpuPct?.toInt()` with no extra scale (`BenchGlance.kt:660`). Fallback `metrics.gpuUsage * 100f` is correct because `DashboardMetrics.gpuUsage` is 0–1.
- Evidence: Live bus `gpuPct = 41f` → `"4100% · N MHz"`. Phase 6 table: hero `% · MHz` from snap/metrics, honest.
- Impact: Overview `tap →` Raster shows a nonsense percent whenever MonitorBus has a fitted GPU. Phase 6 GPU channel acceptance fails.
- Required worker change: `GpuChannel.kt:45` — `val pct = snap.gpuPct ?: (metrics.gpuUsage * 100f)`. Do not `times(100f)` on `snap.gpuPct`. Do not change Glance or BenchModel scaling.

#### [MINOR][CODE_DEFECT] GpuChannel NOT FITTED extra name-blank conjuncts diverge from Raster widget → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/channels/GpuChannel.kt:34`
- Problem: Plan: `CHANNEL LOCKED` / `NOT FITTED` from `snap.gpuFitted` / `snap.gpuRootLocked`. Channel requires `!gpuFitted && gpuName.isBlank() && gpuModel.isBlank()` before NOT FITTED. GLES renderer strings are almost always present, so the page shows CHANNEL LOCKED (known vendor) or a 0% hero where `RasterWidget` (`BenchGlance.kt:610`) shows NOT FITTED first and still prints the name.
- Evidence: Sampler sets `gpuName = gm.renderer` even when `!gpuFitted` (`BenchModel.kt:332-334`); `gpuRootLocked = !gpuFitted && vendor != UNKNOWN` (`:327`).
- Impact: Channel honesty label disagrees with the home RASTER instrument on unrooted devices. Not a crash; LOCKED still covers the common known-vendor case.
- Required worker change: `GpuChannel.kt:33-42` — same order as RasterWidget: `!snap.gpuFitted` → NOT FITTED (name if non-blank); `snap.gpuRootLocked` → CHANNEL LOCKED. Drop the blank-name conjuncts.

#### [MINOR][CODE_DEFECT] Overview GPU tile paints `50% %` → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/DashboardScreen.kt:103`
- Problem: `value = "${(m.gpuUsage * 100).toInt()}%"` plus `unit = "%"`. `ReadoutTile` concatenates `value` + ` unit`. CPU tile uses `Fmt.pct` and no unit.
- Evidence: `:103-104`. Spark correctly uses `m.gpuHistory` (`:109`), not `cpuHistory`.
- Impact: Cosmetic; tap still opens `gpu`.
- Required worker change: `DashboardScreen.kt:103-104` — either drop `unit = "%"` or pass the number without a trailing `%` in `value`. Do not revert spark to `cpuHistory`.

#### [MINOR][CODE_DEFECT] T2+ Header→band spacer still 6 dp (PASS 5 leftover on a touched file) → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt:374`
- Problem: Plan Phase 0 MINOR: T2+ spacers 8 dp between bands; T1 stays 4 dp; never 16 dp on T1. Worker did convert SCOPE T2+ after the split, STACK/FUEL T2 sparks to 8 dp. Header→content is still `Spacer(6.dp)` on every tier: SCOPE `:374`, STACK `:454`, FUEL `:541`, RASTER live `:659`, BENCH T3+ `:720`.
- Evidence: `grep Spacer(GlanceModifier.height(6.dp))` in BenchGlance.kt (header/row gaps).
- Impact: T2+ still a hair tight vs WD; T1 remains safe.
- Required worker change: For `tier != Tier.T1` only, Header→next band `Spacer(GlanceModifier.height(8.dp))`. Leave T1 at 4–6 dp. Do not introduce 16 dp on T1.

#### [NIT][CODE_DEFECT] Duplicate imports → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudModules.kt:11` and `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/channels/MemoryChannel.kt:12`
- Problem: `heightIn` imported twice; `material3.Text` imported twice. Compiles; noise.
- Evidence: HudModules.kt:10-11 identical `heightIn` lines; MemoryChannel.kt:6 and :12.
- Impact: None at runtime.
- Required worker change: Delete the duplicate import on each file.

#### [NIT][CODE_DEFECT] Unused android.graphics imports in BenchConfigActivity → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:5`
- Problem: `android.graphics.Canvas`, `Paint` (`:5-6`), and `drawscope.DrawScope` (`:23`) are unused; preview uses Compose `Canvas`.
- Evidence: Preview helpers use `androidx.compose.foundation.Canvas`.
- Impact: None at runtime.
- Required worker change: Remove those three unused imports. Do not rewrite PreviewPanel.

### Already correct — do not re-do

- **Phase 0 leftover:** BENCH T2/T3+ footers `FontFamily.Monospace` (`BenchGlance.kt:710,712,750,752`). `setContent` gated on `mediumFlow.first()` (no `runBlocking`, no PAPER-first frame). 5 s `GlobalSnapshot` gate; `BenchBudgetSnapshot.last` dropped from config snap. Overlay F2: 400 ms on `ON_RESUME` via `rememberCoroutineScope`; `LaunchedEffect(hasOverlay)` gone; no auto-start. `isServiceRunning = OverlayService.isRunning.get()` in `loadInitialState` (`OverlayViewModel.kt:100`) and `checkPermissions` (`:197`). Lock DIP wired (`OverlayScreen.kt:143-147`). HudPanel `clipToBounds` / `spacedBy(6.dp)` / `heightIn(min=22.dp)`. START `when` gate unchanged (`OverlayScreen.kt:191-212`).
- **Phase 1 widget field:** 4-way `MediaPick` including FOLLOW (`BenchConfigActivity.kt:119-126`); saved prefs via `getGlanceIdBy` + `BenchState.config` before `setContent` (`:64-72`); `followSystem = pick == FOLLOW` persisted through existing `BenchState.save` KEY_FOLLOW+KEY_MEDIUM; `resolvedMedium` untouched and still follow-only-when-true (`BenchModel.kt:454-459`). Preview medium = FOLLOW→`systemMedium` else selected; caption `home screen · glance`. Root `Box.clickable(openConfig(awId))`; Header `onClick` param dropped; Footer no longer `open("overview")`; BENCH T3+ tiles still `open(chId)`. Nudge `Log.w`; empty `getGlanceIds` does not overwrite a non-empty cache; BandBitmap keys include `snap.timestamp` on STACK hatch / FUEL gauge / BENCH rail / tiles. T2 facsimile is Compose Canvas, not `ScopeTrace`.
- **Phase 2 sheet:** BACK is the first control; 5 paper WEBPs at 72 dp; one ADD; per-kind ACTIVE; MANUAL `MarginNote`. `findActivity` unwraps `ContextWrapper`. Pin extras `EXTRA_APPWIDGET_PREVIEW` downscaled ≤256; `PendingIntent.getBroadcast` `kind.ordinal` + `FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE`; `PinSuccessReceiver` log-only; delay(1200)+ON_RESUME still primary refresh. Manifest receiver `exported=false`.
- **Phase 3 overlay FPS:** Probe stage width = `probeW` (no +24), vertical pad 6. `MemBar(..., height = m.barHDp.dp)`; `FuelMicro(..., height = m.barHDp.dp * 2)` so S/M/L bars grow. Header `heightIn(min=22)`. HudPanel root does not read `slow.value`/`fast.value`. `HudHeaderGate` clock on slow; `HudLedDot` is the only header fast reader (`isNoSignal`). `HudFpsGate` is the only full fast reader. CPU/MEM/PWR/GPU/NET gates on slow; TRACE empty; `modules.sorted()` order kept; lock/drag still on the panel Box. Demo `delay(1000)`. Opacity `opacityJob` 150 ms. OverlayScreen sheet root does **not** collect `hudFast` (only `HudPreviewHost` while `isServiceRunning`). DipSwitch modules `spacedBy(4.dp)`.
- **Phase 4 icons:** 108 viewport, group translate 21 / scale 2.75 (66 safe zone). Monochrome `#000000` only (`ic_launcher_monochrome.xml`). Paper bg `#F4F1E8`. Adaptive paper/carbon/blueprint XMLs; dark aliases use light strokes. MainActivity has no MAIN/LAUNCHER; aliases `exported=true`; Paper enabled in XML. `LauncherAlias.apply` enables target first then disables others (`LauncherAlias.kt:25-40`). `SystemStatsApplication` `mediumFlow.first()` on `Dispatchers.IO` then apply. Settings `setMedium` applies alias. HUD notification `R.drawable.ic_tile_caliper`. Settings media `MarginNote` for OEM cache.
- **Phase 5 device:** `HorizontalPager(userScrollEnabled=false, beyondViewportPageCount=0)`; strip `scrollToItem` + `scrollToPage` (no `animateScrollToItem`); `itemsIndexed(..., key={i,_->i})`. Wide ≥560 two-pane, no pager. `HardwareViewModel` load-once + ON_RESUME; 1 s loop gone. CpuTab/GpuTab Coil `AsyncImage` from Soc/Gpu logo repos with generic fallback; no null model; no `DeveloperBoard`; no `MaterialTheme`/`Brush` on those two tabs.
- **Phase 6 channels:** Routes `processor|memory|network|power|storage|gpu` in **both** NavHosts; not in `railRoutes`. Dashboard `onChannel` wired; STORAGE tile added; GPU spark `m.gpuHistory`. Widget `CH-01..06` remapped to channel routes (not Hardware tabs). One `ChannelViewModel` collects existing `@Singleton` `DashboardRepository` + `MonitorBus` (no second sampler). `netUpHistory` LinkedList of real `txBps`; Network page dual spark only if `netUpHistory.any { it > 0f }`, else live ↑ readout — no fake tx-from-rx. Channel pages have hero/spark/rails, not empty stubs. BACK `popBackStack`.
- **Safety pins held:** OverlayService `WRAP_CONTENT`×`WRAP_CONTENT` (no `MATCH_PARENT`). No `Modifier.blur`. No `providePreview`. No `Bitmap.recycle`. No `BenchFrames.clear`. No IBM Plex in Glance (Monospace only). `overlay_prefs` migration-only (`SystemStatsApplication.kt:48`). No `configuration_optional` on widget XML (`widgetFeatures=reconfigurable` only). No widget FGS / exact AlarmManager. Package stays `com.ivarna.deviceinsight.ui.caliper.widget`. Receivers / OverlayService not renamed. MonitorBus / MemInfoParser / TopConsumers / HudSettingsCache / BenchBudgetWorker not recreated. F1 scroll split untouched. W1–W7/W9 layouts kept. 15 WEBPs not regenerated.

### Grep / compile notes

- `recycle(` / `BenchFrames.clear` in `app/src`: 0
- `Modifier.blur`: 0
- `providePreview`: 0
- `MATCH_PARENT` in `OverlayService.kt`: 0 (`WRAP_CONTENT` at `:161-162`)
- `configuration_optional`: 0
- `HardKey("START"`: 1, inside `when { running → STOP; hasOverlay → START; else → {} }` with `canDrawOverlays` check
- `overlay_prefs`: 1, migration only (`SystemStatsApplication.kt:48`)
- `LaunchedEffect(.*hasOverlay)`: 0
- `runBlocking` in BenchConfigActivity: 0
- Receiver FQNs unchanged (`SingleChannelWidgetReceiver` / `DualChannelWidgetReceiver` / `FuelWidgetReceiver` / `RasterWidgetReceiver` / `BenchWidgetReceiver`); new `PinSuccessReceiver` `exported=false`
- `IBM Plex` in Glance: 0 (`FontFamily.Monospace` in BenchGlance.kt including the four BENCH footers)
- `./gradlew :app:compileDebugKotlin`: **succeeded** (exit 0). Tests/R8 not re-run this pass.

Do **not** recreate HudPanel, F1, W1–W7, aliases, channel routes, or pin extras. Surgical fixes: CANCEL extras, GpuChannel percent (and optionally the NOT FITTED order), GPU tile unit, T2+ header spacer.
