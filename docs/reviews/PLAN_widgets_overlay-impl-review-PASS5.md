# Impl Review: PLAN_widgets_overlay — PASS 5 ITER 5 (worker vs DI-PLAN-002 PASS 4)

## Verdict: REVISE
## Counts: CRITICAL 0 MAJOR 6 MINOR 3 NIT 0

Worker landed the right shape. F1 nested-scroll, the F2 START `when` gate, OverlayService `WRAP_CONTENT` + `startForeground`-before-`stopSelf`, W1–W7/W9 layouts, and the HudPanel rewrite are in place. `./gradlew :app:compileDebugKotlin` succeeded. Do **not** recreate Phase 0 or rewrite the items in “Already correct”. Fix only the six MAJOR items below, then the MINOR HudPanel pin if you touch that file.

Grep (app/src, this pass): `HardKey("START"` = 1 (gated); `canDrawOverlays` = 6; `MATCH_PARENT` in OverlayService = 0; `Modifier.blur` in overlay/hud/service = 0; `chunked(2)` = 1, `chunked(3)` = 0; `providePreview` = 0; `overlay_prefs` = 1 (migration only); `TYPE_PHONE` = 0; `BenchFrames.clear` / `recycle()` in production widget code = 0. Deleted: `CaliperHud.kt`, `OverlayComponents.kt`, `OverlayGraphView.kt`.

### Findings

#### [MAJOR][CODE_DEFECT] F2 400 ms re-check is not on ON_RESUME — Android 8 grant-stale never retriggers → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayScreen.kt:44`
- Problem: Plan F2 pin is ON_RESUME **plus** a 400 ms delayed re-check because Android 8 `canDrawOverlays` stays stale after grant (issuetracker 62047810; minSdk 26). `DisposableEffect` only refreshes immediately on resume. The delayed call is `LaunchedEffect(state.permissions.hasOverlay)` at `:53` — it runs once on first composition and again only when `hasOverlay` flips. After GRANT OVERLAY the user returns with `hasOverlay` still false, so the effect does not restart, START never appears until a later composition, and F2 acceptance (“within ~1 s START appears”) fails on the platform the delay was written for. Comment at `:42` claims the delayed re-check exists; it does not run on resume.
- Evidence: Reviewer read OverlayScreen ON_RESUME observer vs `LaunchedEffect(state.permissions.hasOverlay)`. Keyed on the boolean that is stale, so the delay never fires on the grant-return path.
- Impact: On API 26–27 (and some OEM 8.x forks) START stays absent after the user grants overlay and returns to the sheet.
- Required worker change: In `OverlayScreen`, drop `LaunchedEffect(state.permissions.hasOverlay)`. Add `val scope = rememberCoroutineScope()`. Inside the existing ON_RESUME observer, call `viewModel.refreshPermissions()` immediately **and** `scope.launch { delay(400); viewModel.refreshPermissions() }`. Do **not** auto-start the HUD after grant.

#### [MAJOR][CODE_DEFECT] OverlayScreen never observes OverlayService.isRunning — START shown while FGS is up → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayViewModel.kt:77`
- Problem: `isServiceRunning` defaults false and is only written by `setServiceRunning` from OverlayScreen START/STOP taps (`OverlayScreen.kt:188` / `:201`). `loadInitialState` and `checkPermissions` never read `OverlayService.isRunning`. Reopening the Overlay tab (or process-recreating the ViewModel) while the FGS window is up composes START instead of STOP, so the action-row `when { running -> STOP }` contract is wrong and the user cannot stop the probe from this screen without a dummy START tap.
- Evidence: `isServiceRunning` only mutated from button clicks; `OverlayService.isRunning` AtomicBoolean is unused by the VM.
- Impact: Action row lies after navigation / process recreation. User cannot STOP without a no-op START first. F2 `when { running -> STOP }` fails.
- Required worker change: In `OverlayViewModel.checkPermissions()` (and `init` / `loadInitialState`), set `_uiState.value = _uiState.value.copy(isServiceRunning = OverlayService.isRunning.get(), permissions = …)`. OverlayScreen ON_RESUME already calls `refreshPermissions()`, so STOP will appear after return. Do not invent a second running flag.

#### [MAJOR][CODE_DEFECT] No lock DIP on OverlayScreen — locked probe has no unlock path → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayScreen.kt:102`
- Problem: Crosshair lock writes `hudLocked=true` (`OverlayService.onLock` → `setHudLocked(true)`) and the service applies `FLAG_NOT_TOUCHABLE`. OverlayScreen STYLE & LAYOUT has scale / medium / opacity / blur / RESET POSITION but no lock DIP. `OverlayViewModel.setLocked` exists at `:124` and is never called from UI. RESET POSITION does not clear `hudLocked`. STOP does not clear it either, so the next START comes back locked. Because a locked probe cannot be tapped (passthrough), OverlayScreen is the only unlock path — and it is missing.
- Evidence: `setLocked` is dead from UI; no `DipSwitch` bound to `state.config.locked`.
- Impact: User who taps ⌖ on the probe is stuck with a passthrough overlay they cannot unlock or usefully retarget from the sheet.
- Required worker change: In `OverlayScreen` STYLE & LAYOUT `PanelCard`, after the blur `DipSwitch` (around `:131`), add `DipSwitch(checked = state.config.locked, onCheckedChange = { viewModel.setLocked(it) }, label = "lock (touch passthrough)")`. Do not auto-start after grant. Do not add a close button on the probe.

#### [MAJOR][CODE_DEFECT] W8 — BENCH footers omit FontFamily.Monospace → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt:691`
- Problem: W8 requires `FontFamily.Monospace` on every Glance `TextStyle`. BENCH T2 ledger footer (`:691`, `:693`) and T3+ tiled footer (`:731`, `:733`) use `TextStyle(color=…, fontSize=11.sp)` with no `fontFamily`. Those four strings render in the Glance default family while the rest of the instrument is monospace.
- Evidence: Grep of `BenchWidgetAll` footers; other TextStyles already set Monospace.
- Impact: BENCH footer typeface diverges from SCOPE/STACK/FUEL/RASTER and from W8 acceptance.
- Required worker change: On all four `Text(…, style = TextStyle(…))` calls in `BenchWidgetAll` footers (`BenchGlance.kt:691`, `:693`, `:731`, `:733`), add `fontFamily = FontFamily.Monospace`. Do not touch other TextStyles that already set it. Do not use IBM Plex inside Glance.

#### [MAJOR][CODE_DEFECT] W10 — CaliperTheme ignores MEDIA SegKey → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:54`
- Problem: W10 PreviewPanel is real CALIPER Compose, but the theme does not follow the MEDIA SegKey. Activity `setContent` wraps `CaliperTheme(medium = initialMedium.value)` (`:54-55`) from DataStore (or PAPER until `mediumFlow.first()`). `BenchConfigScreen` then does `var medium by remember { mutableStateOf(Medium.PAPER) }` at `:110` and builds `cfg` / `PreviewPanel` from that local state. Changing SegKey updates `cfg.medium` only; `Caliper.colors` / `OdometerText` / `ScopeTrace` / `HatchBar` / `LinearGauge` keep the outer theme. Selecting BLUEPRINT while DataStore is CARBON still paints a CARBON preview. Also, `setContent` is not gated on `mediumFlow.first()`.
- Evidence: Outer `CaliperTheme(initialMedium)` vs inner `mutableStateOf(Medium.PAPER)` that never wraps the theme.
- Impact: Config preview lies about the chosen medium. W10 acceptance (three-media preview fidelity) fails.
- Required worker change: In `BenchConfigActivity.onCreate`, `lifecycleScope.launch { val m = runCatching { mediumFlow.first() }.getOrNull() ?: Medium.PAPER; setContent { BenchConfigRoot(kind, initial = m, onSave, onSkip, onCancel) } }` — do not call `setContent` before `first()`, do not `runBlocking` on main. Inside the composable, `var medium by remember { mutableStateOf(initial) }` and wrap the screen in `CaliperTheme(medium = medium)` so SegKey drives the preview. Pass `kind`/`save` as today.

#### [MAJOR][CODE_DEFECT] W10 — preview uses stale GlobalSnapshot / BUDGET instead of 5 s gate → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchConfigActivity.kt:119`
- Problem: W10 demo feed pin is `GlobalSnapshot.current()` only when `timestamp` age < 5 s, else `benchDemoSnapshot`. Current line is `GlobalSnapshot.current() ?: BenchBudgetSnapshot.last ?: benchDemoSnapshot(kind)` — a hours-old bus/BUDGET snapshot (empty hists, stale %) is shown as the live preview instead of the designed demo instruments.
- Evidence: No age check on `live.timestamp`; `BenchBudgetSnapshot.last` is a 15 min lossy sample, not a config demo.
- Impact: Config activity shows a dead/empty instrument after process death instead of the designed demo feed.
- Required worker change: In `BenchConfigScreen`, replace the snap line with `val now = System.currentTimeMillis(); val live = GlobalSnapshot.current(); val snap = if (live != null && now - live.timestamp in 0 until 5_000L) live else benchDemoSnapshot(kind)`. Do not use `BenchBudgetSnapshot.last` for this preview.

#### [MINOR][CODE_DEFECT] F3 internals — HudPanel missing clipToBounds / spacedBy(6.dp) / heightIn(min=22.dp) → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudPanel.kt:61`
- Problem: F3 internals pin `clipToBounds()` on the scrim Box, `Column(verticalArrangement = Arrangement.spacedBy(6.dp))`, and rows `wrapContentHeight() + heightIn(min = 22.dp)`. Current root Box is `width(m.widthDp.dp).wrapContentHeight().background(…).hudFrame(…)` with no `clipToBounds`. Inner `Column` at `:67` has only `padding(m.padDp.dp)` — spacers are `padDp/2` (5/6/7 dp), not `spacedBy(6.dp)`. `HudFpsBand` / other module rows have no `heightIn(min=22.dp)`. `height(16.dp)` and `Modifier.blur` are gone, so the old overlap crash-cause is fixed, but S/M packed bands can still sit tighter than the pin and the scrim can paint past brackets.
- Evidence: HudPanel.kt:61, :67; HudModules band roots.
- Impact: Residual F3 tightness on S; not the original overlap crash. Do in the same HUD pass if touching HudPanel.
- Required worker change: On the HudPanel root Box (`HudPanel.kt:61`), chain `.clipToBounds()` after `background`. Change the inner Column (`:67`) to `Column(Modifier.padding(m.padDp.dp), verticalArrangement = Arrangement.spacedBy(6.dp))` and drop the ad-hoc `Spacer(padDp/2)` pairs around hairlines (keep a single `HairlineH()` between modules). On `HudHeaderBand` / `HudFpsBand` / `HudCpuBand` / `HudMemoryBand` / `HudPowerBand` / `HudGpuBand` / `HudNetBand` root rows, use `Modifier.fillMaxWidth().wrapContentHeight().heightIn(min = 22.dp)` — do not reintroduce `height(16.dp)`.

#### [MINOR][CODE_DEFECT] T2+ band gaps still 4–6 dp vs pinned 8 dp → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt:359`
- Problem: W1 pin is T1 band gap 4–6 dp (kept) and T2+ 8 dp between bands. SCOPE T2+ still uses `Spacer(4.dp)` after the split Row (`:390`) and 6 dp after Header (`:359`). STACK/FUEL/RASTER T2+ similarly use 4–6 dp. T1 16 dp was correctly not forced.
- Evidence: Spacer heights in Scope/Stack/Fuel/Raster provideContent.
- Impact: Visual tightness vs WD on T2+; T1 remains safe.
- Required worker change: In `ScopeWidget` / `StackWidget` / `FuelWidget` / `RasterWidget` `provideContent`, keep T1 spacers at 4.dp; for `tier != Tier.T1` (SCOPE: the `else` branch at `:369`) use `Spacer(GlanceModifier.height(8.dp))` between header, split/hero, thermal, and the next band. Do not change T1 or outer 12 dp inset. Do **not** force 16 dp on T1.

#### [MINOR][NARRATION] Review-history comments in product code → FIX_ROUTE: WORKER
- Location: `app/src/main/java/com/ivarna/deviceinsight/presentation/settings/WidgetsSheet.kt:37`
- Problem: Comments restate the change and embed review history (`fixes MAJOR 4`, `F1 (plan §2.0)`, `W1 (WD §4 T2)`, `W7 (WD §7)`). They do not explain a non-obvious constraint.
- Evidence: WidgetsSheet.kt:37; SettingsScreen.kt:69-70; BenchGlance W1/W3/W5/W7 prefixes.
- Impact: Noise; no functional break.
- Required worker change: Delete `WidgetsSheet.kt:37` (`// Initial load + ON_RESUME refresh (fixes MAJOR 4)`). In `SettingsScreen.kt:69-70` keep one short WHY if needed (“outer Column must not scroll — nested fillMaxSize+verticalScroll throws”) and drop “plan §2.0”. In `BenchGlance.kt` drop the `// W1` / `// W3` / `// W5` / `// W7` prefixes; keep only the constraint (e.g. T1 stacked vs T2 split). Do not add architecture-history comments while fixing.

## Already correct — do not re-do

- **F1:** `SettingsScreen.kt:71` outer `Column(fillMaxSize().caliperGrid())` is not scrollable; `WidgetsSheet` (`:61`) is the only scroller on the instruments branch; settings/colophon body uses one `verticalScroll` (`SettingsScreen.kt:75`). BackHandler stays outside. Extra WidgetsSheet `padding(16.dp)` dropped. No `heightIn(max=9999)` hack.
- **W1:** T1 stacked (`BenchGlance.kt:362-368`); T2+ `Row` with both children `GlanceModifier.defaultWeight()` (`:371-382`); split BandBitmap does **not** use `fillMaxWidth` (modifier overridden). T1 gaps stay 4 dp.
- **W2:** `Canvas.scope` `drawText` 0/50/100 (T2–T3) and five labels (T4+) at 10sp ink40 on the right (`BenchArt.kt:182-192`). No Glance Text y-axis.
- **W3:** STACK header trailing `%` when live; CALIBRATING / SIGNAL LOST win (`BenchGlance.kt:433-437`).
- **W4:** one Subline under hatch from non-zero composition; type not painted on the bitmap (`:448-460`).
- **W5:** wattHero → Hero watts + 11sp `%`; else Hero `%` + watts subline; `remainingText` `Xh Ym` / `N min`; no fake % hist (`:525-557`, `:82-87`).
- **W6:** live `gpuName · gpuVulkan` Subline; single `gpuHist` spark (`:645-654`).
- **W7:** `chunked(2)`; T3 = 5 tiles + `Spacer(defaultWeight)`; T4 = 6; T5 core rail; 14 dp bitmaps CH-01 spark / CH-02 hatch / CH-03 spark / CH-04 fuelGauge / CH-05 hatch / CH-06 spark; masthead 6 dp LED + `HH:mm` (`:277-337`, `:697-727`).
- **W9:** `GlanceModifier.semantics { contentDescription = desc }` on `BenchPanel` root Box (`:185-187`). Compiles on 1.1.0.
- **F2 composition:** `when { running -> STOP; hasOverlay -> START; else -> {} }` (`OverlayScreen.kt:183-204`); START onClick `if (!canDrawOverlays) return`; usage-stats is not a START gate; GRANT OVERLAY stays in PERMISSIONS; no auto-start after grant.
- **F2 service:** `startForegroundNotification()` then `if (!canDrawOverlays) { stopSelf(); return }` (`OverlayService.kt:117-125`); no `addView` without permission; addView in try/catch then `stopSelf`; `TYPE_PHONE` deleted; params `WRAP_CONTENT`×`WRAP_CONTENT` (`:159-171`).
- **F3 host:** centered wrap-to-scale 196/260/300, stage not a WindowManager overlay, 12 dp gap before PERMISSIONS, no `Modifier.blur`, no `StampBadge`, modules from `HudConfig` (`OverlayScreen.kt:210-260`; `HudPanel.kt:78-93`).
- **Phase 3 files:** `HudAtoms` / `HudModules` / `HudPanel` / `HudDemo` / filled `HudTheme` palettes+scales+locals; OverlayService ComposeView + LifecycleOwner + SavedStateRegistryOwner + `FLAG_BLUR_BEHIND` only; OverlayViewModel exposes `HudConfig` from caliper DataStore (no `scaleFactor`/`isHorizontal`, no `overlay_prefs`); FQCN `OverlayService` kept; `CaliperHud.kt` / `OverlayComponents.kt` / `OverlayGraphView.kt` deleted.
- **Phase 0 pins held:** `MonitorBus` is `@Singleton` not `object`; receivers not renamed; no `providePreview`; no Glance IBM Plex; no `Bitmap.recycle`; no global `BenchFrames.clear()`; `overlay_prefs` only in `SystemStatsApplication` migration; `calibrate`/`hud-config` mapped in **both** NavHosts (`SystemStatsApp.kt:96-97`, overlay routes `:212` and `:263`).
- Package remains `com.ivarna.deviceinsight.ui.caliper.widget`.

## Required worker fixes (ordered)

1. `BenchGlance.kt:691,693,731,733` — add `fontFamily = FontFamily.Monospace` on the four BENCH footer `TextStyle`s. (W8)
2. `BenchConfigActivity.kt:48-55,110,119` — gate `setContent` on `mediumFlow.first()` inside `lifecycleScope.launch` (no `runBlocking`, no PAPER-first `setContent`). Drive `CaliperTheme(medium)` from the MEDIA SegKey state. Replace snap selection with `GlobalSnapshot` only when `now - timestamp < 5000`, else `benchDemoSnapshot(kind)`. Do not use `BenchBudgetSnapshot.last`. (W10)
3. `OverlayScreen.kt:44-56` — move the 400 ms `refreshPermissions()` into the ON_RESUME observer (`scope.launch { delay(400); … }`). Remove `LaunchedEffect(state.permissions.hasOverlay)`. Do not auto-start after grant. (F2)
4. `OverlayViewModel.kt:77,165-189` — copy `isServiceRunning = OverlayService.isRunning.get()` in `checkPermissions` / initial load so STOP shows when the FGS is already up. (F2 action row)
5. `OverlayScreen.kt:131` — add lock `DipSwitch` bound to `state.config.locked` / `viewModel.setLocked`. (HUD lock; only unlock path)
6. `HudPanel.kt:61,67` plus HudModules band rows — `clipToBounds()`, `spacedBy(6.dp)`, `heightIn(min=22.dp)` as in the MINOR F3 finding. Do this in the same HUD pass if touching HudPanel.

Then optional MINOR: T2+ 8 dp spacers; strip review-history comments.

## Safety pins the worker must still obey while fixing

- Do not recreate Phase 0 (MonitorBus, MemInfoParser, TopConsumersProvider, HudSettingsCache, BenchBudgetWorker, 15 WEBPs, 4-side hairline, receiver FQNs).
- Do not rename receivers or OverlayService.
- Do not force 16dp band gaps on T1 140×140.
- Do not fake GPU freq hist or battery % curve.
- Do not use IBM Plex / custom fonts inside Glance — `FontFamily.Monospace` only.
- Do not implement Glance `providePreview` (absent in 1.1.0).
- Overlay WindowManager params `WRAP_CONTENT` never `MATCH_PARENT` for the probe.
- No `Modifier.blur` on HUD or overlay-sheet preview; window blur = `FLAG_BLUR_BEHIND` only.
- Do not auto-start HUD after overlay permission grant.
- Usage-stats is NOT a START gate.
- Do not reintroduce `overlay_prefs` dual store (only `SystemStatsApplication` migration may mention it).
- No `Bitmap.recycle` on LruCache eviction; no global `BenchFrames.clear()` on onDeleted.
- WD (`docs/design/widgets.md`) wins over WI sketch on layout. Keep package `com.ivarna.deviceinsight.ui.caliper.widget`.

## Evidence

- Reviewer subagent `01a02cfa-09dd-7421-b120-c2dadf5e62ce` against local uncommitted tree vs `docs/plans/PLAN_widgets_overlay.md` PASS 4.
- Scratch notes: `/tmp/grok-abhaybyte/grok-review-39e935e6.md`
- `./gradlew :app:compileDebugKotlin` succeeded (reviewer).
- git: 61 tracked files changed, 4326+/2363-; 13 untracked (HUD files + docs).

## Next Agent: Worker
## Next Action: Fix the 6 MAJOR items in order (W8 footers → W10 config theme+5s gate → F2 400ms on ON_RESUME → OverlayViewModel isRunning → lock DipSwitch), then HudPanel `clipToBounds`/`spacedBy`/`heightIn` if touching that file. Do not recreate Phase 0. Stop after those fixes; do not start a new HUD rewrite.
