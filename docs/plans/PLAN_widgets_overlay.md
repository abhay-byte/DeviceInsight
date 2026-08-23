# PLAN — BENCH Widgets + Settings Instruments + Scope Probe HUD

> **PASS 6 successor (2026-08-23).** Remaining PASS 5 impl-review items (W8 / W10 / F2 400 ms-on-resume / `isRunning` / lock DIP / HudPanel `clipToBounds`) **and** device field-QA now live in [`docs/plans/PLAN_caliper_field.md`](PLAN_caliper_field.md) (**DI-PLAN-003**, Status READY). Execute that plan, not this one.
>
> This file (DI-PLAN-002) is **historical pins only**. Worker must **not** recreate Phase 0 or re-do W1–W7 / W9 / F1 / F2-when / F3-host / HudPanel rewrite. Pins that still bind the successor:
> - **WD wins over WI** on layout. Package stays `com.ivarna.deviceinsight.ui.caliper.widget`. Do not copy WI `widget.bench`.
> - Glance **1.1.0** — `FontFamily.Monospace` only; **no `providePreview`** (absent in the 1.1.0 AAR).
> - Overlay WindowManager **WRAP_CONTENT** never `MATCH_PARENT`. **No `Modifier.blur`** on HUD or sheet preview (`FLAG_BLUR_BEHIND` only). No auto-start after overlay grant.
> - **F1** nested-scroll split, **F2** START `when` gate, **F3** wrap-to-scale 196/260/300 remain in force.
>
> Worker execute order lives on DI-PLAN-003 (Phase 0 leftover review → widget field → instruments pin → overlay FPS → icons → device tabs/logos → overview channels).

Doc ID: DI-PLAN-002 · PASS 4 · Iteration 4 · Status: READY — REVISED 2026-08-23 after a code-vs-design audit of `ui.caliper.widget` + overlay vs DI-WD-001 / DI-WI-001 / DI-HD-001. PASS 3 field bugs F1/F2/F3 remain in force. Prior PASS 2 pins from `docs/reviews/PLAN_widgets_overlay-plan-review.md` remain in force.
Task: close the remaining visual + overlay gap. **Research + planning only this pass — no product code.** Do **not** recreate Phase 0 files or re-do work already in the tree (see PASS 4 audit).

**This is not a greenfield build.** Phase 0 bus/parser/WM/previews/hairline/receivers are largely landed. Widgets exist but several instrument *layouts* still do not match DI-WD-001. Overlay *window* is still the pre-CALIPER rounded-card View hierarchy — **not** the Scope Probe in `overlay_redesign.md`. Plan is: skip re-creating foundation → finish widget layouts to WD → F1 Settings crash → replace overlay internals (F2/F3 + HudPanel).

**Source-of-truth conflict (pinned, PASS 4):** `docs/design/widgets.md` (DI-WD-001) is the **visual** spec. `docs/design/widgets_implementations.md` (DI-WI-001) is **pseudo-code that will not compile** (wrong package, sketch APIs). Where they disagree on layout, follow **WD**. Where they disagree on Glance APIs, follow **this plan + Glance 1.1.0**. Do not copy WI file split / `widget.bench` package / `MonitorBus` as `object`.

**PASS 3 field bugs (pinned, ship in the phases named below — do not treat as polish):**

| # | Surface | Symptom | Root cause (verified in tree) | Phase |
|---|---|---|---|---|
| F1 | Settings → INSTRUMENTS (`WidgetsSheet`) | Page **crashes on open** | Nested `verticalScroll` + `fillMaxSize`: `SettingsScreen.kt:69` parent `Column(fillMaxSize().caliperGrid().verticalScroll)` hosts `WidgetsSheet` `Column(fillMaxSize().verticalScroll)` at `:58`. Compose `checkScrollableContainerConstraints` throws `IllegalStateException: Vertically scrollable component was measured with an infinity maximum height constraints`. Glance `getGlanceIds` is already try/caught — **not** the crash. | **Phase 2.0 first commit** (can land before widget polish) |
| F2 | Overlay sheet action row | **START is composed when overlay permission is denied** (`OverlayScreen.kt:152-163` `HardKey("START", enabled = state.permissions.hasOverlay)`). Disabled is still visible. User: START must not appear until `Settings.canDrawOverlays` is true. | Permission is special-app-access, not a runtime dialog. Platform + GitHub overlay libs (FloatingWidgetCompose, JetOverlay, OverlayHelper) all **gate start**, they do not show a disabled start. GRANT OVERLAY already lives in the PERMISSIONS card (`:73-80`). | **Phase 3.3** (also a one-file OverlayScreen change that can land earlier as a hotfix commit of PR-C) |
| F3 | Overlay sheet demo HUD | Preview is **full-bleed, cramped, and overlapping**: SF stamp rectangle collides with FPS row; GPU `71%` collides with RAM/TEMP; corner brackets clip numerals; scrim rectangle sits flush on PERMISSIONS. Evidence: `docs/testers/caliper-001/caliper-001-03-overlay.png` + `CaliperHud.kt`. | `CaliperHud` uses `Modifier.fillMaxWidth()` (design M width is **260dp**, S 196 / L 300 — `overlay_redesign.md` §3 / `HudScales`). `HudRow` hard `height(16.dp)` clips 13sp meta + 12dp spark. `StampBadge` is a 1.5dp bordered box. `drawBehind` brackets share the content box (inset 10dp, no inner pad). `Modifier.blur(8.dp)` on `matchParentSize` scrim expands drawing bounds into neighbors. Compose layout docs: siblings without `Column` spacing / min height **stack on top of each other**. | **Phase 3.3** (preview host on OverlayScreen + `HudPanel`/`CaliperHud` internals) |

### PASS 4 — code vs design (2026-08-23). Worker must not re-do the DONE column.

Read against current tree (`BenchGlance.kt`, `BenchArt.kt`, `BenchModel.kt`, `BenchConfigActivity.kt`, `WidgetsSheet.kt`, `CaliperHud.kt`, `HudTheme.kt`, `HudModel.kt`, `OverlayScreen.kt`, `OverlayService.kt`) and the three design docs. Official Glance 1.1 layout docs (developer.android.com/develop/ui/compose/glance/build-ui, last updated 2026-08-20): `Box`/`Column`/`Row` only (Box = `RelativeLayout` — siblings **stack/overlap** unless in Column/Row); `defaultWeight()` is the real API; `SizeMode.Responsive` calls `provideContent` once per size; **custom fonts are not supported** in Glance — `FontFamily.Monospace` is the documented family. GitHub overlay: handstandsam Compose OverlayService gist uses `WRAP_CONTENT` WindowManager params, never `MATCH_PARENT` for a probe.

**Authority:** DI-WD-001 visual · DI-WI-001 API sketch (do not copy) · DI-HD-001 overlay · this plan for Glance 1.1.0.

#### Widgets — already matches WD / WI enough. Do not rewrite.

| Item | Evidence |
|---|---|
| 5 Glance widgets + legacy receiver FQNs | `ScopeWidget`/`StackWidget`/`FuelWidget`/`RasterWidget`/`BenchWidgetAll`; `SingleChannel`/`DualChannel`/`Fuel`/`Raster`/`Bench` receivers |
| `SizeMode.Responsive` T1–T5; BENCH omits T1 | `AllSizes` + `BenchSizes = T2..T5` (`BenchGlance.kt:67-70`) — WD §5 min BENCH is 4×2 |
| 4-side 1dp hairline inside 12dp inset | `BenchPanel` top/left/right/bottom strips (`:169-190`) — PASS 2 §1.2 **done**. Do not replace. |
| 3 media + Blueprint all-ink traces | `WidgetPalettes.fromCaliper(..., isBlueprint)` (`BenchModel.kt:140-159`) |
| Cadence LIVE/AMBIENT/BUDGET 15 min + WM | `cadenceMs` + `BenchBudget`/`BenchBudgetWorker`; XML `updatePeriodMillis="0"` |
| 15 picker previews | `drawable-nodpi/preview_{scope,stack,fuel,raster,bench}_{paper,carbon,blueprint}.webp` + XML `previewImage=@drawable/preview_*_paper` — §1.10 **done**. Do not regenerate unless a screenshot QA fails. |
| Receivers `onEnabled`/`onDisabled`/`onDeleted` per-id | all 5 (`BenchGlance.kt:655-718`); no global `BenchFrames.clear()` |
| `placedAt` write-once | `BenchState.save` (`BenchModel.kt:415-417`) |
| No `Bitmap.recycle()` on LruCache eviction | `BenchSelfCheckTest` asserts source has no `recycle()` |
| SIGNAL LOST footer + LED off | `Footer` stamp; `Header(..., ledOn = !stale && !calibrating)` |
| FUEL NOT FITTED / RASTER CHANNEL LOCKED + `[ GRANT IN APP ]` | `FuelWidget`/`RasterWidget` branches |
| FUEL remaining + T4 spec when real | subline `remainingMin`; health/cycles/design only if non-null |
| STACK hatch uses `memComposition`; hide 0 MB RSS | `TopConsumersProvider` + skip `rssMb==0` |
| `Fmt.wattsSigned` already prefixes `≈` | `CaliperUtils.kt` `"≈ %+.2f W"` |
| MonitorBus + MemInfoParser + 2 Hz loop | `DashboardRepositoryImpl` `delay(500)` + `monitorBus.pushSlow`; `GlobalSnapshot` is a `@Volatile` mirror for Glance (no Hilt in `provideGlance`) |
| Settings ADD / ACTIVE copy | `ADD TO HOME SCREEN`, `NOT PLACED` / `PLACED ×N`, ON_RESUME refresh. **F1 crash still blocks the page.** |

#### Widgets — remaining visual gaps vs DI-WD-001 (Phase 1.6). These *are* the widget UI work.

Do **not** force WD §2 “16dp between bands” on T1 140×140 — that clips hero+trace. Pin spacing: **T1 = 4–6dp** (current); **T2+ = 8dp** between bands. Outer pad 12dp stays.

| Gap | WD | Code now | Pin |
|---|---|---|---|
| **W1 SCOPE T2 split** | 4×2: hero+freq+temp **left**, gridded trace **right**, thermal under | Stacked Column (hero, full-width scope, thermal) — matches WI sketch, **not** WD | Glance `Row { Column(defaultWeight){Hero; Subline freq; Subline temp}; BandBitmap(scope) }`. T1 stays stacked. Official: `defaultWeight` on Row children. T2 content box ≈256×116 after inset — split fits; do not add a 3rd column. |
| **W2 SCOPE y-labels** | T2 ascii has 0/25/50/75/100 | `Canvas.scope` draws grid, **no** `drawText` labels (`BenchArt.kt:156-211`) | T2–T3: `0 / 50 / 100` at 10sp ink40 on the right edge of the bitmap. T4+: all five. Do not put labels in Glance `Text` beside the bitmap (RemoteViews alignment is worse). |
| **W3 STACK header %** | header trailing `57%` | header status is `LIVE` / `SIGNAL LOST` | Trailing status = `Fmt.pct(used/total)` when live; keep SIGNAL LOST / CALIBRATING when those states win. |
| **W4 STACK T2 labels** | labeled cadastral (active/cached/zram/free) | `hatchBar` has no text | One Glance `Subline` under the bar: `active · cached · zram · free` from `memComposition` labels (or skip a zero segment). **Do not** draw type on the bitmap (Glance text stays text — WI §9). |
| **W5 FUEL secondary %** | wattage hero + `78%` as its own line | only the hero (watt **or** percent) | If `wattHero`: Hero watts, next line `Fmt.pct(batteryPct*100,0)` at 11sp. If not: hero %, subline watts. Remaining format: `Xh Ym remaining` when `remainingMin>=60`, else `N min remaining`. T3 discharge-curve bitmap: **defer** unless `batteryPct` history exists on `BenchSnapshot` (it does not — do not fake a % hist from wattHist). |
| **W6 RASTER live datasheet line** | `adreno 740 · vulkan 1.3` on T1 | name/vulkan only in LOCKED / NOT FITTED | Live branch: `Subline("${gpuName} · ${gpuVulkan}")` when non-blank. T2 dual-trace (load over freq): **only if** a freq hist exists; it does not — keep single `gpuHist` spark (honest). Do not invent a second hist. |
| **W7 BENCH tiles** | T3+ **2×3** miniature ReadoutTiles with a sub-instrument each | `chunked(3)` = **3 columns**; only CH-03 spark + CH-05 hatch | `chunked(2)`. Each tile: label + value + one 14dp bitmap (CH-01 spark, CH-02 hatch, CH-03 spark, CH-04 fuelGauge, CH-05 hatch, CH-06 spark). T3 = 5 tiles (last row one cell + spacer); T4 = 6; T5 + full-width core rail (already). Masthead: add 6dp LED box + `HH:mm` (not seconds) — WD `● 14:32`. |
| **W8 Glance type** | IBM Plex; tabular | no `fontFamily` on Glance `TextStyle` | **`FontFamily.Monospace`** on every Glance `TextStyle` (hero, header, subline, footer). Official 1.1: custom fonts **not supported**. Config activity (Compose UI) keeps Plex. |
| **W9 Panel a11y** | spoken summary on the widget | `BenchPanel(pal, desc)` **never applies `desc`** (`:164-191` unused param) | Put `desc` on the root `Box` via `GlanceModifier.semantics { contentDescription = desc }` (`androidx.glance.semantics`). If the symbol fails to compile on 1.1.0, attach `desc` to the first `BandBitmap` **and** keep the param used (do not leave it dead). |
| **W10 Config preview** | live CALIPER `OdometerText` / `ScopeTrace` / `HatchBar` / `LinearGauge` (WI §8) | `PreviewPanel` is two `Text`s (`BenchConfigActivity.kt:181-218`); `getInitialMedium()` always PAPER | Wrap in `CaliperTheme(medium)`. SCOPE: `OdometerText` + existing `ScopeTrace` if the Compose component exists, else `Canvas` spark from `cpuHist`. STACK: `HatchBar`. FUEL: `LinearGauge` or `OdometerText` watts. Do not embed Glance composables in the Activity. Read `mediumFlow.first()` in `onCreate` before `setContent` (not `runBlocking` on main after composition). |

**Explicitly out of widget UI this pass (do not invent):** per-core real load, dossier deep links, T3 FUEL discharge curve without a % history, RASTER T2 dual-trace without freq hist, IBM Plex inside Glance, `providePreview`, renaming receivers, 16dp band gaps on T1, hatch labels painted into the bitmap.

#### Overlay — **not** done vs DI-HD-001. Do not call `CaliperHud` the Scope Probe.

| DI-HD-001 | Tree now |
|---|---|
| `HudTheme.kt` palettes + `HudScales` 196/260/300 + `HudTheme{}` locals | `HudTheme.kt` is **enums + mapping only** (34 lines). No `HudPalettes`, no `HudScales`, no `CompositionLocal`. |
| `HudAtoms.kt` / `HudModules.kt` / `HudPanel.kt` / `HudDemo.kt` | **Missing.** `CaliperHud.kt` is a Carbon-only stub (`fillMaxWidth`, `HudRow height(16.dp)`, `StampBadge`, `Modifier.blur(8.dp)`). |
| `HudPanel` `.width(m.widthDp.dp)` wrap height, modules from `HudConfig`, `StrokedText`, `SparkPen` square pen, `CoreBank`, `FuelMicro`, `HudStamp` | Overlay **sheet** preview is the stub. Overlay **window** is still `OverlayService` View soup (`TYPE_APPLICATION_OVERLAY` + ProgressBars + `OverlayGraphView.kt`). |
| OverlayScreen = medium S/M/L, opacity, module DIPs, lock, blur, wrap-to-scale preview | Still `STYLE & LAYOUT` horizontal + scale fader (`OverlayScreen.kt:95-109`). `OverlayViewModel.OverlayUiState` still `scaleFactor`/`isHorizontal`. DataStore hud keys exist (`CaliperPrefs`) but the sheet does not expose them as HUD controls. |
| START gated on `canDrawOverlays`; service WRAP_CONTENT probe | F2/F3 **unchanged**. Gist pattern: `WRAP_CONTENT` × `WRAP_CONTENT` for Compose overlay (handstandsam). Never MATCH_PARENT. |

Phase 3 stays: create `HudAtoms`/`HudModules`/`HudPanel`/`HudDemo`; fill `HudTheme` with palettes/scales from DI-HD-001 hex (copy from `CaliperColors`, do not fork); rewrite `OverlayService` internals; replace OverlayScreen chrome. **F2/F3 are the first OverlayScreen commit** and can land on the stub host before `HudPanel` exists.

**Package decision (pinned):** keep `com.ivarna.deviceinsight.ui.caliper.widget`. Do **not** move to sketch package `widget.bench`. Manifest, Settings, tests, and home-screen placements already bind the current names. Sketch file split (`BenchPanel.kt` / `BenchWidgets.kt` / `BenchUpdater.kt`) is optional later; do not churn paths this effort.

**Service decision (pinned):** keep class `com.ivarna.deviceinsight.service.OverlayService`. Rewrite internals to host Compose HUD. Do not add a second `HudService`. OverlayScreen START already targets this class.

**Old overlay (pinned):** delete, no flag. `OverlayComponents.kt` (unused by OverlayScreen) and `service/overlay/OverlayGraphView.kt` go in the HUD PR. **HUD config single source of truth (pinned, already in tree):** `caliper` DataStore is the only store. `CaliperKeys.hudMedium/hudScale/hudOpacity/hudBlur/hudLocked/hudModules/hudShowCoreBank/hudX/hudY/fpsMode/hudMigrated` exist (`CaliperPrefs.kt`). One-shot migration already runs in `SystemStatsApplication.onCreate` (legacy `overlay_prefs` read only there). Do not add a second store. Remaining overlay work is **UI + service rewrite**, not keys.

Plan file: `docs/plans/PLAN_widgets_overlay.md` (planner write-scope is `docs/plans/` only; not repo-root `PLAN.md`).

---

## 0 · Research Sources

### Local (authoritative)

- `docs/design/widgets.md` (DI-WD-001) — 5 instruments, 5-band anatomy, 3 media, SizeMode.Responsive T1–T5, cadence ladder, §7 states, 15 picker previews, a11y. <source: file>
- `docs/design/widgets_implementations.md` (DI-WI-001) — Kotlin sketches. **Pseudo-code. Will not compile.** Flags: `hatchBar` `sp_4` placeholder, `coreRailBitmap`/`h0` stubs, `WidgetScopeRASTER_PLACEHOLDER`. <source: file>
- `docs/design/overlay_redesign.md` (DI-HD-001) — Scope Probe. Flags: SavedStateRegistryController, `canDrawOverlays` before start. <source: file>
- `docs/design/new_design.md` S-11 HUD / S-12 widgets / S-13 settings. <source: file>
- `app/build.gradle.kts` — minSdk 26, target/compile 36, Compose BOM `2024.12.01`, Glance `1.1.0` + `glance-material3:1.1.0`, DataStore `1.1.1`, Hilt 2.51.1, Navigation 2.8.5. **No WorkManager.** <source: file>
- `AndroidManifest.xml` — 5 Glance receivers (SCOPE/STACK/FUEL/RASTER/BENCH), `BenchConfigActivity` + `APPWIDGET_CONFIGURE`, `OverlayService` `foregroundServiceType=specialUse`, `FOREGROUND_SERVICE_SPECIAL_USE`, `SYSTEM_ALERT_WINDOW`. <source: file>
- Existing widget impl: `ui/caliper/widget/{BenchModel,BenchArt,BenchGlance,BenchConfigActivity}.kt`. <source: file>
- Existing Settings: `presentation/settings/{SettingsScreen,WidgetsSheet}.kt`. <source: file>
- Existing overlay: `service/OverlayService.kt` (965-line View HUD), `ui/caliper/hud/CaliperHud.kt` (preview stub), `presentation/overlay/{OverlayScreen,OverlayViewModel,OverlayComponents}.kt`. <source: file>
- Data: `DashboardRepositoryImpl` 1 Hz loop writes `BenchSnapshotCache` then `BenchUpdater.nudge`. Providers: Cpu/Memory/GpuUsage/Power/Battery/Thermal/NetworkTraffic/Storage/FpsMonitor. <source: file>
- CALIPER tokens exist: `Medium`, `HatchPattern`, `Channels`, `CoreReading` (`components/CaliperData.kt:324`), palettes, `Fmt.*`. <source: file>
- Tests: `app/src/test/.../widget/BenchSelfCheckTest.kt` (stale/tier/palette/mapping). <source: file>
- Deep links: `MainActivity` extra `di_route`; `SystemStatsApp` maps CH-01/02→Dashboard, CH-03→Hardware tab 4, CH-04→tab 5, CH-05→tab 9, CH-06→tab 3, processes/calibrate. Glance key `ROUTE = ActionParameters.Key<String>("di_route")`. <source: file>
- Hardware tabs pinned: `SYSTEM=0 CPU=1 DISPLAY=2 GPU=3 NETWORK=4 BATTERY=5 ANDROID=6 HARDWARE=7 THERMAL=8 STORAGE=9 SENSORS=10` (`HardwareScreen.kt:38`). No MEMORY tab — CH-02 stays Overview. <source: file>

### Glance / platform facts (verified against this project's SDK + 1.1.0, not the sketch)

Sketch APIs that **do not exist / must not be copied**:

| Sketch | Real Glance 1.1.0 |
|---|---|
| `widget.updateState(ctx, id, PreferencesGlanceStateDefinition)` | `updateAppWidgetState(ctx, PreferencesGlanceStateDefinition, id) { MutablePreferences -> }` then `widget.update(ctx, id)` |
| `widget.currentState(ctx, id, def).firstOrNull()` | `getAppWidgetState(ctx, PreferencesGlanceStateDefinition, id)` returns `Preferences` (suspend) |
| `BandBitmap` + `LaunchedEffect` + `mutableStateOf` | **Forbidden.** Glance composition is not a full Compose effect host. Pre-render bitmaps in `provideGlance` (suspend, Default dispatcher) **before** `provideContent`. Current `renderSync` during composition is the correct direction; move it *out* of `@Composable` for cleanliness. |
| `isSystemInDarkTheme()` inside Glance | Not a Glance API. Current `resolvedMedium()` (DataStore `mediumFlow` + `Configuration.UI_MODE_NIGHT`) is correct. |
| `android.graphics.Color.valueOf(...).toCompose()` | Use `androidx.compose.ui.graphics.Color(argb)` / `ColorProvider(composeColor)`. Current `WidgetPalette` already holds Compose `Color`. |
| `MonitorBus` as `object` + `StateFlow` | Fine as a Hilt `@Singleton`, not a process-global object if tests need fakes. Current `BenchSnapshotCache` is a `@Volatile` var — upgrade. |
| `actionStartActivity<MainActivity>(parameters = actionParametersOf(ROUTE to route))` | **Real API in 1.1.0.** Extras land under the Key name (`di_route`). MainActivity already reads it. Keep. |
| `GlanceAppWidgetManager.requestPinGlanceAppWidget` | Exists in 1.1 as a wrapper. Current `AppWidgetManager.requestPinAppWidget(ComponentName, extras, successCallback)` is the platform API (API 26 = minSdk). Keep platform API; add success `PendingIntent` so Settings list refreshes. |
| `SizeMode.Responsive(setOf(DpSize…))` | Real. Glance generates a RemoteViews per size in the set. 5 sizes × heavy trees = binder cost. Pin: SCOPE/STACK/FUEL/RASTER get T1–T5; **BENCH omits T1** (design min is 4×2 / T2). |
| `updatePeriodMillis` | Platform floor is 30 min (`1800000`). Current XML uses that. Design wants 1s/30s/15min via service push + WorkManager. Set `android:updatePeriodMillis="0"` and drive updates ourselves. |
| Picker previews | XML `previewImage` + API 31+ `previewLayout`. Glance 1.1 adds `GlanceAppWidget.providePreview`. Ship 15 static WEBP/PNG as `previewImage` (required for the picker on all APIs) **and** `providePreview` for generated API 35+ widgets. |
| `contentDescription` | `Image(..., contentDescription)` + root. TalkBack uses this; Glance has no Canvas text-stroke. |
| RemoteViews limits | No Canvas, no `border()`, no arbitrary fonts (IBM Plex via Glance `TextStyle` is best-effort; **system monospace + `fontFeatureSettings="tnum"`** is the hard requirement). Hairline = 1dp `Box` strips. LED = 6dp square. |

WorkManager: **not in Gradle**. Periodic 15 min is the only legal background BUDGET path once the process dies. `DashboardRepositoryImpl` loop dies with the process.

Overlay blur: `WindowManager.LayoutParams.FLAG_BLUR_BEHIND` + `setBlurBehindRadius` API 31+; `WindowManager.addCrossWindowBlurEnabledListener`. If OEM disables cross-window blur, **raise scrim opacity +0.10**, never assume both. `Modifier.blur` on a Compose scrim (current `CaliperHud`) blurs the HUD itself, not the wallpaper — wrong API for a probe. HUD must use window-level blur-behind.

Compose-in-Service: `ComposeView` needs `ViewTreeLifecycleOwner` + `ViewTreeSavedStateRegistryOwner`. Prod pattern: Service implements `LifecycleOwner` + `SavedStateRegistryOwner`; `SavedStateRegistryController.create(this)` + `performRestore(null)` in `onCreate`; `lifecycleRegistry.currentState = RESUMED`; `setViewTreeLifecycleOwner` / `setViewTreeSavedStateRegistryOwner`. Sketch's `SavedStateRegistryOwner { lifecycle }` is not a type. <source: DI-HD-001 prod note + AndroidX savedstate>

`FLAG_NOT_TOUCHABLE` when locked = full passthrough. Unlocked = `FLAG_NOT_FOCUSABLE` only, drag + tap. `TYPE_APPLICATION_OVERLAY`. minSdk 26: no `TYPE_PHONE` branch needed (current OverlayService still has the pre-O branch — delete).

### ctx7 / webfetch / clone

- PASS 4: `npx ctx7@latest library "Jetpack Glance"` **blocked** in this environment (untrusted remote `@latest`). Used official docs instead: [Build UI with Glance](https://developer.android.com/develop/ui/compose/glance/build-ui) (updated 2026-08-20) — Box/Column/Row, `defaultWeight`, `SizeMode.Responsive` per-size `provideContent`, **`FontFamily.Monospace` only (custom fonts unsupported)**. Nested-scroll crash: AOSP `checkScrollableContainerConstraints` + [SO 69394543](https://stackoverflow.com/questions/69394543/fillmaxsize-modifier-not-working-when-combined-with-verticalscroll-in-jetpack-co) / [cashapp/paparazzi#1364](https://github.com/cashapp/paparazzi/issues/1364). Overlay WRAP_CONTENT: [handstandsam Compose OverlayService gist](https://gist.github.com/handstandsam/6ecff2f39da72c0b38c07aa80bbb5a2f). PASS 3 Context7 IDs (`/websites/developer_android`, Compose, Glance) still apply; do not re-resolve unless docs drift.
- `webfetch` of developer.android.com Glance pages timed out. <source: command>
- `git clone` of android/user-interface-samples blocked by environment bash policy except the allowlist; clone into `/tmp/opencode/` was denied as a non-matching pattern in one attempt and not retried. <source: NEW_RISKS>
- **Glance 1.1.0 AAR verification (PASS 2, hard evidence):** extracted `~/.gradle/caches/modules-2/files-2.1/androidx.glance/glance-appwidget/1.1.0/.../glance-appwidget-1.1.0.aar` → `classes.jar` (477 classes). `javap -p GlanceAppWidget` shows only `provideGlance(Context, GlanceId, Continuation)`; `grep -rl providePreview` across all 477 classes returned 0 matches (control `provideGlance` matched 3 files). Same for `glance-1.1.0.aar` core (Row/Column verified). **Conclusion: `providePreview` does NOT exist in Glance 1.1.0. Do NOT implement it. Static `previewImage` drawable is the only picker preview path.** <source: bash `unzip -l classes.jar | wc -l` + `javap` + `grep -rl` 2026-08-22>

### PASS 3 field-feedback research (2026-08-23)

**Context7 library resolve (npx ctx7@latest library):**
- Android overlay APIs → `/websites/developer_android` (high, 78k snippets)
- Jetpack Compose nested scroll → `/websites/developer_android_develop_ui_compose` (high, 4.2k snippets)
- Jetpack Glance widgets → `/websites/developer_android_develop_ui_compose_glance` (high, 166 snippets)

**Official docs (webfetch + reference):**
- `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (API 23): "Show screen for controlling which apps can draw on top of other apps." **Package URI is optional and only guaranteed prior to API 30 / `Build.VERSION_CODES.R`** — on R+ the intent often lands on the *list*, not the per-app toggle. Keep `Uri.parse("package:$pkg")` (many OEMs still honor it) but do not assume the toggle is one tap. Constant: `"android.settings.action.MANAGE_OVERLAY_PERMISSION"`. <source: developer.android.com/reference/android/provider/Settings>
- `Settings.canDrawOverlays(Context)` (API 23): true only if manifest `SYSTEM_ALERT_WINDOW` **and** the user granted special-app-access. Not a runtime permission dialog. <source: same + learn.microsoft.com/dotnet/api android.provider.Settings.CanDrawOverlays>
- Compose layout: without a `Column`/`Row`, composables **stack on top of each other**. `fillMaxWidth` consumes parent max width. Nested same-direction scroll is disallowed. <source: developer.android.com/develop/ui/compose/layouts/basics — "Compose stacks the text elements on top of each other, making them unreadable">
- Glance widgets are **not interoperable** with Compose UI elements (`Caution` on the Glance landing page). Settings instruments page is Compose UI talking to Glance via `GlanceAppWidgetManager.getGlanceIds` (suspend) — that path is fine; mixing Glance composables into `WidgetsSheet` is not. <source: developer.android.com/develop/ui/compose/glance, last updated 2026-08-10>
- Nested scroll crash is `androidx.compose.foundation.checkScrollableContainerConstraints`: "Vertically scrollable component was measured with an infinity maximum height constraints, which is disallowed. One of the common reasons is nesting layouts like LazyColumn and Column(Modifier.verticalScroll())." <source: AOSP `CheckScrollableContainerConstraints.kt`; SO 73165390 / 79177673>

**GitHub / field patterns (overlay start gating):**
- [pascaladitia/FloatingWidgetCompose](https://github.com/pascaladitia/FloatingWidgetCompose) — demo exposes **Grant Permission / Start Floating / Stop** as three distinct actions. `if (!canDrawOverlays) { openOverlayPermissionSettings(); return }`. Never starts from a composable body (recomposition would re-add the window). `SizeMode.WRAP` for a probe; `FULL` only for a drag layer.
- [YazanAesmael/JetOverlay](https://github.com/YazanAesmael/JetOverlay) — Compose overlay SDK, FGS persistence, isolated Lifecycle/ViewModel; permission is a precondition of `start`.
- [gist KONFeature ComposeOverlayViewService](https://gist.github.com/KONFeature/2f84436e1c0a1926505cac934d470f90) — `WRAP_CONTENT` WindowManager params + `SavedStateRegistryController.performRestore(null)` + `setViewTreeSavedStateRegistryOwner`. Matches DI-HD-001 prod note.
- [gist sjf overlay](https://gist.github.com/sjf/ae050683a8d790dcb3260b5ffc610b87) — `checkDrawOverlayPermission()` then `startForegroundService`; if denied, only the settings intent fires.
- OpenLumen `OverlayEngine.installView`: `if (SDK>=23 && !canDrawOverlays) { log; return false }` **before** `addView`.
- Android 8.0 `canDrawOverlays` stale after grant ([issuetracker 62047810](https://issuetracker.google.com/issues/62047810), SO 46173460): value can stay false for several seconds. Mitigation: `ON_RESUME` already in `OverlayScreen`; add a **400 ms delayed re-check** after returning from the overlay-permission intent. Do **not** busy-loop 15 s with a ProgressDialog (old SO workaround — hostile).

**Not the widgets crash:** `GlanceAppWidgetManager.getGlanceIds` is suspend and already wrapped `try/catch` in `WidgetsSheet.refreshInstruments` (`:143-167`) and `SettingsScreen.refreshCount` (`:42-55`). A Glance lookup failure degrades to `NOT PLACED` / empty ACTIVE, it does not abort composition.

### Reference pattern (from training + local usage, marked)

Glance 1.1 `GlanceAppWidget`:

```kotlin
class ScopeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(sizes)
    override val stateDefinition = PreferencesGlanceStateDefinition
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cfg = BenchState.config(context, id)
        val snap = MonitorBus.current()
        // pre-render bitmaps here
        provideContent { /* Glance composables only */ }
    }
}
```

Pinning (API 26+):

```kotlin
val mgr = AppWidgetManager.getInstance(ctx)
if (!mgr.isRequestPinAppWidgetSupported) { /* manual path */ return }
mgr.requestPinAppWidget(ComponentName(ctx, receiver), /* extras */ null, successCallback)
```

---

## 1 · Architecture Map — exists vs missing

```
                    ┌─ FpsMonitor (SF then GFX, Int only, no source stamp)
                    │
 Providers 1 Hz ──▶ DashboardRepositoryImpl.collectMetrics()
                    │  writes BenchSnapshotCache.last  (@Volatile)
                    │  BenchUpdater.nudge()            (900 ms throttle)
                    ▼
         GlanceAppWidget.update() ── provideGlance ── BenchArt bitmaps + Glance text
                    │
 OverlayService ◀── dashboardRepository.getDashboardMetrics()  (separate collector,
                    rounded card Views, 1 Hz, no Compose, no lock flag)
```

### Exists (keep / extend)

| Surface | Path | Notes |
|---|---|---|
| Tokens | `ui/caliper/CaliperTheme.kt` | `Medium`, `HatchPattern`, `Channels`, palettes |
| Draw | `ui/caliper/CaliperDraw.kt` | Compose `DrawScope.hatch` — widget twin is `BenchArt.kt` Canvas hatch |
| CoreReading | `ui/caliper/components/CaliperData.kt:324` | `id, load, freqKhz` |
| Fmt | `ui/caliper/CaliperUtils.kt` | `pct/hz/temp/rate/wattsSigned/bytes` |
| DataStore | `ui/caliper/CaliperPrefs.kt` | single `caliper` file; keys `medium/showGrid/hatchingEnabled/caliperMigrated` |
| Snapshot + state | `ui/caliper/widget/BenchModel.kt` | kinds, tiers, cadence, palettes, `BenchState` (correct Glance APIs), `BenchSampler` fallback, `toBenchSnapshot()` |
| Renderer | `ui/caliper/widget/BenchArt.kt` | spark/scope/hatchBar/fuelGauge/wattTrace/coreRailRows/thermalRamp/lockedField/calibrating. **No `sp_4` placeholder.** Period passed as `sp(ctx, 4f)`. |
| Widgets + receivers | `ui/caliper/widget/BenchGlance.kt` | 5 widgets, SizeMode.Responsive T1–T5, BandBitmap sync, receivers keep legacy names |
| Config | `ui/caliper/widget/BenchConfigActivity.kt` | APPWIDGET_CONFIGURE, kind from provider className, SKIP/SAVE |
| Settings card + page | `SettingsScreen.kt` §03 + `WidgetsSheet.kt` | pin via `requestPinAppWidget`, list via `getGlanceIds` |
| XML + strings | `res/xml/*_widget_info.xml`, `widget_desc_*` | descriptions match DI-WD-001 §8; previewImage = launcher icon |
| Overlay config UI | `OverlayScreen.kt` + `OverlayViewModel.kt` | CALIPER sheet, permissions, metric DIPs, START/STOP |
| HUD preview stub | `ui/caliper/hud/CaliperHud.kt` | **not** DI-HD-001. Carbon-only, `fillMaxWidth`, `HudRow height(16.dp)`, `StampBadge`, `Modifier.blur`. F3. |
| HUD types (partial) | `ui/caliper/hud/{HudTheme,HudModel}.kt` | enums + `HudConfig` only. No palettes/scales/HudPanel/HudAtoms/HudModules/HudDemo. |
| Overlay window | `service/OverlayService.kt` | **old product** — rounded Views + `OverlayGraphView.kt`. Not Compose, not wrap-to-scale. |
| Nav | `SystemStatsApp.kt` | `di_route` handling |
| Tests | `BenchSelfCheckTest.kt` | mapping/stale/tier/blueprint-ink |

### Missing or wrong (this effort)

**Shared foundation (PASS 4: landed. Residual only.)**

- `MonitorBus` Hilt singleton + `GlobalSnapshot` `@Volatile` mirror for Glance: **exists**. HUD still does not *collect* `slow`/`fast` (no OverlayService Compose). Wire in Phase 3, do not recreate the bus.
- Dashboard loop is **2 Hz** (`delay(500)`). Fast 10 Hz ticker still belongs in OverlayService (Phase 3) — not running today because the window is Views.
- `getCurrentFpsWithSource()` **exists**. HUD must use it (honest `—`), not display-refresh fallback.
- `MemInfoParser` + `memComposition` **exists** and is pushed from `DashboardRepositoryImpl`.
- `gpuHist` / `netHist` / `remainingMin` / `TopConsumersProvider` / WorkManager 15 min: **in tree**.
- Per-core load = `freq/max` proxy — **out of scope** (unchanged).

**Plan A — widgets (PASS 4: layout gaps only, do not rewrite the family)**

Foundation that PASS 2 listed as missing is **in the tree** — 4-side hairline, BENCH T2–T5, WM + `updatePeriodMillis=0`, 15 `previewImage` WEBPs, LED off, `placedAt` write-once, no `recycle()`, remainingMin + T4 spec, NOT FITTED / CHANNEL LOCKED, cadastral `memComposition`. **Do not re-implement those.**

Remaining (PASS 4 W1–W10, see audit table): SCOPE T2 `Row` split + y-labels in `BenchArt.scope`; STACK header `%` + composition Subline; FUEL secondary % + `Xh Ym` remaining (no fake discharge curve); RASTER live name/vulkan Subline (no fake freq hist); BENCH `chunked(2)` tiles with one bitmap each + masthead LED/clock; Glance `FontFamily.Monospace`; apply `BenchPanel` `contentDescription`; config `PreviewPanel` uses real CALIPER Compose, not two Texts.

`BenchUpdater.nudge` stays fire-and-forget (900 ms throttle + id cache 30 s) — good enough. Do not switch to `collectLatest` per GlanceId this pass.

**Plan B — Settings**

- Card + page copy is **done** (`ADD TO HOME SCREEN`, per-kind `NOT PLACED` / `PLACED ×N`, ON_RESUME + delay refresh). Do not re-copy.
- **PASS 3 F1 (crash, blocks the page):** still true. `SettingsScreen.kt:69-73` wraps `WidgetsSheet` in an outer `verticalScroll` while `WidgetsSheet.kt:58` is itself `fillMaxSize().verticalScroll`. Opening INSTRUMENTS throws Compose nested-scroll `IllegalStateException`. Fix in Phase 2.0 **before** any other Settings work. After F1, Plan B is done.

**Plan C — overlay (PASS 4: HUD UI is not implemented. Window is not DI-HD-001.)**

- Window is still rounded card + ProgressBars + `OverlayGraphView`. Not Scope Probe. `HudPanel.kt` / `HudAtoms.kt` / `HudModules.kt` / `HudDemo.kt` **do not exist**.
- `CaliperHud` is Carbon-only, no S/M/L, no modules, no lock, wrong blur, `fillMaxWidth` (F3).
- `HudTheme.kt` / `HudModel.kt` are stubs (enums + `HudConfig`). Palettes/scales/locals from DI-HD-001 §6 are **not** in code.
- `OverlayScreen` still exposes horizontal layout + scale fader (old product). Replace with HudConfig: medium (explicit, never follow-system), scale S/M/L, opacity 40–90%, module DIPs, blur DIP, lock, position reset. DataStore keys already exist — wire the sheet, do not add keys.
- **PASS 3 F2:** START HardKey is always composed, merely `enabled=hasOverlay` (`OverlayScreen.kt:152-163`). User pin: **do not compose START** unless `Settings.canDrawOverlays`. GRANT OVERLAY already in PERMISSIONS (`:73-80`). Service `onCreate` (`OverlayService.kt:73-81`) never checks `canDrawOverlays` before `createOverlayView`/`addView` (`:402`).
- **PASS 3 F3:** In-sheet demo HUD is a full-width cramped rectangle. `CaliperHud.kt:46` `fillMaxWidth`; `HudRow` `:87` `height(16.dp)`; SF `StampBadge` bordered box; `drawBehind` brackets share the content box; `Modifier.blur(8.dp)` on `matchParentSize` scrim. Tester PNG `docs/testers/caliper-001/caliper-001-03-overlay.png` shows GPU `71%` overlapping RAM/TEMP and the SF stamp colliding with the FPS row. Design width is wrap-to-scale (`HudScales` 196/260/300), never page width.
- No dual-rate recomposition.
- No `FLAG_NOT_TOUCHABLE`.
- No SavedStateRegistry on the overlay window (N/A today because not Compose).
- `OverlayComponents.kt` dead. Delete.

---

## 2 · Shared foundation (Phase 0) — **mostly landed. Do not recreate.**

**PASS 4:** `MonitorBus.kt`, `HudFeed.kt`, `MemInfoParser.kt`, `HudSettingsCache.kt`, `TopConsumersProvider.kt`, `BenchBudget.kt`, `BenchBudgetWorker.kt`, hud* DataStore keys, `delay(500)`, `getCurrentFpsWithSource()`, migration in `SystemStatsApplication` **already exist and are wired**. Worker: **grep before create**. If the file is there, skip the "Create" row. Residual Phase 0 is only: confirm `Grep overlay_prefs` is the migration site only (it is), `assembleDebug` still green, no duplicate `MonitorBus` object.

**Goal (original):** one metrics pipeline feeding widgets **and** HUD. No second sampler. Keep the pins below as the contract, not as a todo list to re-type.

### 0.1 `MonitorBus` Hilt singleton

Create `data/monitor/MonitorBus.kt` (or `domain/monitor/`):

```kotlin
@Singleton
class MonitorBus @Inject constructor() {
    private val _snap = MutableStateFlow(BenchSnapshot())
    val snapshot: StateFlow<BenchSnapshot> = _snap.asStateFlow()
    private val _slow = MutableStateFlow(HudSlow())
    val slow: StateFlow<HudSlow> = _slow.asStateFlow()
    private val _fast = MutableStateFlow(HudFast())
    val fast: StateFlow<HudFast> = _fast.asStateFlow()
    fun pushSlow(snap: BenchSnapshot, slow: HudSlow) { _snap.value = snap; _slow.value = slow }
    fun pushFast(fast: HudFast) { _fast.value = fast }
    fun current(): BenchSnapshot = _snap.value
}
```

- Replace `BenchSnapshotCache` with `MonitorBus.current()` / `snapshot`. Keep a deprecated typealias **one release** if tests import the cache — tests live in-module, just retarget.
- **Invariant (pinned, fixes CRITICAL 2):** Foreground truth = `DashboardRepositoryImpl` is the **only** writer of `BenchSnapshot` → `MonitorBus` (`DashboardRepositoryImpl.kt:61` `while(true){emit(collectMetrics()); delay(1000)}` + `:242 BenchSnapshotCache.last = …toBenchSnapshot` is the single write site plus `:251 BenchUpdater.nudge`). HUD service collects `slow`/`fast` as `State` so FPS band recomposes alone (`HudPanel(slow: State<HudSlow>, fast: State<HudFast>)`). **BUDGET (process dead / WM) is NOT a MonitorBus writer** — see 0.7. `Grep BenchSnapshotCache` must be 0; BUDGET path must not call `MonitorBus.push*`.

### 0.2 Dual-rate inside the existing loop

Do **not** start a second 1 Hz repository. Split:

- **Slow (2 Hz):** change `DashboardRepositoryImpl` delay `1000` → `500` (`DashboardRepositoryImpl.kt:69` `delay(1000)`). Map to `BenchSnapshot` + `HudSlow`. Widgets throttle via cadence; they will not update at 2 Hz unless LIVE.
- **Fast (10 Hz) — pinned threading/backoff (fixes MAJOR 1):** Only when `OverlayService.isRunning` (`OverlayService.kt` `isRunning` AtomicBoolean). Create `FpsTicker` as a `CoroutineScope(SupervisorJob()+Dispatchers.IO)` owned by `OverlayService` — started in `onCreate`, cancelled in `onDestroy` with the service lifecycle (never on `Dispatchers.Main`; current `OverlayService.kt:548` `CoroutineScope(Dispatchers.Main).launch{getDashboardMetrics().collect}` is the anti-pattern). Loop (IO):

  ```kotlin
  var consecutiveDash = 0
  var cachedLayer: String? = null; var cachedPkg: String? = null
  while(isActive){
    val sample = withContext(Dispatchers.IO){ fpsMonitor.getCurrentFpsWithSource() } // internally caches layer name 30s
    if(sample.source=="—") consecutiveDash++ else consecutiveDash=0
    _fast.tryEmit(sample.toHudFast()) // or MonitorBus.pushFast
    val delayMs = if(consecutiveDash>=5) 1000L else 100L // adaptive: 1 Hz after 5× "—"
    delay(delayMs)
  }
  ```

  `FpsMonitor.getCurrentFpsWithSource()` must be IO-safe (it calls `executeCommand` → `Shizuku.newProcess` reflection `FpsMonitor.kt:100-106` + `process.waitFor()` at `:115,:82` — 100–300 ms blocking). Cache `findSurfaceFlingerLayer` result keyed by package+30s window inside `FpsMonitor` to avoid `dumpsys SurfaceFlinger --list` every 100 ms. When `sample.source=="—"` HUD renders `"—"` + `NO SIGNAL`; `DashboardRepositoryImpl.kt:126-128` fallback `displayRefreshRateUtils.getRefreshRate()` is NOT used for `HudFast` (honest signal only HUD).

Pin: **do not** run 10 Hz dumpsys globally, nor on Main. `dumpsys SurfaceFlinger --latency` is expensive and needs root/Shizuku.

### 0.3 `FpsMonitor` source stamp

Add:

```kotlin
data class FpsSample(val fps: Int, val source: String) // "SF" | "GFX" | "—"
fun getCurrentFpsWithSource(): FpsSample
```

Keep `getCurrentFps()` as `getCurrentFpsWithSource().fps` so DashboardMetrics stays compiling. Source: SF if `getSurfaceFlingerFps>0`, else GFX if `getGfxInfoFps>0`, else `"—"`. Overlay currently falls back to display refresh rate when fps≤0 (`DashboardRepositoryImpl:126-128`) — that **lies**. HUD must show `"—"` + `NO SIGNAL` when both dumpsys paths fail, not the panel Hz. Dashboard tile may keep the refresh-rate fallback (out of scope) but **HudFast.source must be honest**.

### 0.4 Memory composition parser (pinned math, fixes CRITICAL 3)

New pure function `data/monitor/MemInfoParser.kt: parse(meminfo: String, zramBytes: Long?): MemComposition` — unit-testable without device. Caller `MemoryProvider` or `DashboardRepositoryImpl` reads file, passes string.

Parse `/proc/meminfo` keys: `MemTotal`, `Active` (or `Active(anon)` fallback), `Cached`, `SwapTotal`, `SwapFree`, plus `MemAvailable` for fallback only. ZRAM: `/sys/block/zram0/mm_stat` first field (orig_data_size) if readable; else null.

**Exact fractions (MemTotal is the denominator for ALL; never `ramTotal+swapTotal` like `BenchModel.kt:203`):**

```kotlin
val activeF  = (Active ?: ActiveAnon ?: fallbackActive) / MemTotal.toFloat()
 // fallbackActive = (MemTotal - MemAvailable - Cached - (zramBytes?:0)) / MemTotal when Active missing
val cachedF  = Cached / MemTotal
val zramF    = (zramBytes?.toFloat()?.div(MemTotal) ?: 0f)
val swapRawF = (SwapTotal - SwapFree).toFloat() / MemTotal
val swapF    = max(0f, swapRawF - zramF) // de-duplicate ZRAM backing already counted in swap
var freeF    = 1f - (activeF + cachedF + zramF + swapF)
freeF = freeF.coerceAtLeast(0f)
// normalize if sum>1 due to overlapping Active/Cached on some kernels
val sum = activeF+cachedF+zramF+swapF+freeF
if(sum>1f){ val s=1f/sum; activeF*=s; cachedF*=s; zramF*=s; swapF*=s; freeF*=s }
```

Build `List<MemSeg>` in fixed order `[active(SOLID,CH-02), cached(DIAGONAL,CH-03), zram(CROSS,CH-04), swap(CROSS subline only if swapRawF>0), free(NONE)]` where swap as a bar segment is included only when `zramF==0 && swapF>0` (swap-without-zram); otherwise swap lives as a subline (`swapUsed/swapTotal`) independent of bar fractions. Blueprint hatched bar uses same fractions (hatch identity).

On parse failure (missing MemTotal) return today's `usedFraction = ramUsed/ramTotal` + single SOLID segment — never invent ZRAM.

Evidence: `MemoryProvider.kt:12` only `totalMem/availMem`; `DashboardRepositoryImpl.kt:256-274` SwapTotal/SwapFree manual parse but no Active/Cached; `BenchModel.kt:198-206` `usedFraction` + `swapFrac/(ram+swap)` wrong denominator, no normalization, free may be negative. `BenchArt.hatchBar` currently `coerceAtMost(w)` hides overflow — fix parser instead of clamping.

### 0.5 History tails for GPU / NET

In `DashboardRepositoryImpl.collectMetrics`, add `LinkedList` gpuHist (usage %) and netHist (rxBps as float). Same `HISTORY_SIZE=61`. Expose on `DashboardMetrics` (two new fields with defaults so existing tests compile) and map in `toBenchSnapshot`.

### 0.6 Battery fields for FUEL T4 / remaining

Extend `toBenchSnapshot` / `BenchSnapshot`:

- `currentMa` from `BATTERY_PROPERTY_CURRENT_NOW` (µA → mA). Already in `BenchSampler`; missing in `toBenchSnapshot` (sets 0).
- `remainingMin`: if charging, `computeChargeTimeRemaining()/60000`; if discharging, omit (`0` → subline hides). Do not fake.
- `batteryHealth`, `cycleCount: Int?`, `designMah: Int?` from `BatteryProvider`. **If cycle extra missing, pass null, never 835.**
- `batteryPresent` from `EXTRA_PRESENT`.

### 0.7 WorkManager dependency + BUDGET cold path (pinned race-free, fixes CRITICAL 2 + MAJOR 2)

`app/build.gradle.kts`:

```
implementation("androidx.work:work-runtime-ktx:2.9.1") // AOSP-friendly, no GMS
```

**BUDGET semantics (pinned, fixes CRITICAL 2):**
`BenchBudgetWorker : CoroutineWorker` does **NOT** write `MonitorBus`. It calls `BenchSampler.sample(context)` (`BenchModel.kt:256` — separate collector fabricating `BenchSnapshot` directly from sysfs) and then drives widgets via direct `GlanceAppWidget.update(context, id)` per placed id. When the app process is dead (the BUDGET case) the in-process `MonitorBus` graph is gone — the "write to bus if graph alive" in PASS 1 was a no-op; remove it. Document that BUDGET snapshot is intentionally lossy:

| Field | BUDGET value |
|---|---|
| `cpuHist`, `memHist`, `gpuHist`, `netHist`, `wattHist` | empty (no DashboardRepositoryImpl history) |
| `memComposition` | via same `MemInfoParser` (0.4) if available else single SOLID used-segment (`BenchModel.kt:352` style) — pin sampler to reuse `MemInfoParser` so foreground vs BUDGET hatch bar does not diverge |
| `currentMa`, `batteryPresent`, `voltage`, `charging` | real (BatteryManager) |
| `topConsumers` | empty (see 0.9 provider — not available cold without usage-stats) |

Foreground vs BUDGET widgets may show different history tails — that is expected and documented; tests for `toBenchSnapshot` mapping still cover the foreground path, `BenchSampler` smoke-tested separately.

Pin: do **not** spin the 1 Hz `DashboardRepositoryImpl` loop from WM.

**Enqueue/cancel race-free (fixes MAJOR 2):**
Single helper `ui/caliper/widget/BenchBudget.kt`:

```kotlin
object BenchBudget {
  const val UNIQUE = "bench-budget"
  fun enqueue(ctx: Context){ WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<BenchBudgetWorker>(15, MINUTES).build()) }
  fun cancelIfNone(ctx: Context){ // atomic total count
    val mgr = GlanceAppWidgetManager(ctx)
    val total = listOf(ScopeWidget::class, StackWidget::class, FuelWidget::class, RasterWidget::class, BenchWidgetAll::class).sumOf{ runCatching{ mgr.getGlanceIds(it.java).size }.getOrDefault(0) }
    if(total==0) WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE)
  }
}
```

- `PeriodicWorkRequest` 15 min, `ExistingPeriodicWorkPolicy.KEEP` (interval-stable; `UPDATE` would reschedule on every `onEnabled` and drift the 15-min wall time — use `KEEP`).
- `enqueue` called from `SystemStatsApplication.onCreate` **and** each receiver's `onEnabled`.
- `cancelIfNone` called from each receiver's `onDisabled` (after `super.onDisabled`). It counts across all 5 kinds atomically before cancelling — fixes current bug where one kind's `onDisabled` could cancel WM while another kind still placed. Only when total==0 does it cancel.
- Current code: `BenchGlance.kt:627-632` only `SingleChannelWidgetReceiver.onDeleted{ BenchFrames.clear() }` (global clear bug), no `onEnabled`/`onDisabled` anywhere. See 1.4 for per-id `lastPush` eviction fix.

`updatePeriodMillis=0` on all five XML providers. `AndroidManifest.xml` needs no WM entry (auto-init). min interval 15 min matches design BUDGET. Release `isMinifyEnabled=true` (`app/build.gradle.kts:67`) — verify `BenchBudgetWorker` is kept; `work-runtime` ships consumer keep rules for `ListenableWorker` subclasses, no extra rule needed unless R8 strips it (see proguard note in 0.9 table).

### 0.8 Mapping `HudSlow` / `HudFast`

New types live in `ui/caliper/hud/HudModel.kt` (created in Phase 3, but the mapper can sit next to `toBenchSnapshot` in Phase 0 as internal data classes if HUD files aren't there yet). **Pin:** define `HudSlow`/`HudFast` in Phase 0 in `data/monitor/HudFeed.kt` so OverlayService can compile against them before UI lands; HUD UI imports the same types. Do not duplicate.

`HudSlow` from `DashboardMetrics` + meminfo + cores (`CoreStat(id, loadPct, freqMhz)` from existing `CoreReading`). `clusterSizes` heuristic: split cores by unique `cpuCoreMaxFrequencies` runs (little/mid/big). If unknown, `listOf(cores.size)`.

`swapTotalMb == 0` → HUD `NOT FITTED`.

### 0.9 HUD config persistence + migration + helpers (pinned, fixes CRITICAL 1 + MAJOR 3/5 + MINOR 1/2 + SUGGESTION)

**Single source of truth (CRITICAL 1):** `caliper` DataStore (`CaliperPrefs.kt:14` `preferencesDataStore(name="caliper")`) is the only store for HUD. Adds to `CaliperKeys` (`CaliperPrefs.kt:16-21` currently 4 keys):
`hudMedium` (string PAPER/CARBON/BLUEPRINT), `hudScale` (string S/M/L), `hudOpacity` (float 0.4–0.9), `hudBlur` (bool), `hudLocked` (bool), `hudModules` (csv of `HudModule` names), `hudShowCoreBank` (bool), `hudX`/`hudY` (int), `fpsMode` (string AUTO/ROOT/SHIZUKU), `hudMigrated` (bool guard). See also MAJOR 6 for distinct `HudMedium` enum.

**One-shot migration** in `SystemStatsApplication.onCreate` (applicationScope `launch(Dispatchers.IO)`, `runCatching` so failure never crashes startup — `SystemStatsApplication.kt:15` currently bare, add `applicationScope` via `CoroutineScope(SupervisorJob()+Dispatchers.IO)`):
1. if `hudMigrated==true` return.
2. read `overlay_prefs` (`OverlayViewModel.kt:56` / `:71-95` — the only writer `savePreferences():167-180` keys: `show*` booleans per metric `:79`, `scaleFactor` float `:93`, `isHorizontal` bool `:94`, `fps_mode` string `:92`, `metricOrder` csv `:71`). Also read `FpsMonitor.kt:26` `fps_mode`.
3. copy `fps_mode` → `fpsMode`; map `showFps→FPS`, `showCpu→CPU`, `showRam`|`showSwap`→MEMORY, `showPower`|`showBattery`→POWER, `showNetwork`→NETWORK, `showCpuGraph`|`showPowerGraph`→TRACE (canonical list from plan 3.3 line 592). `scaleFactor`/`isHorizontal`/`metricOrder`/`showTime` etc. have no HUD equivalent — use defaults (`hudScale=M`, `hudLocked=false`). Do NOT copy `or`-ambiguity — new keys go only to `caliper`.
4. `caliperDataStore.edit{ it[hudMigrated]=true }`; on success `overlay_prefs.edit().clear().apply()` (clean cut, all readers switched in same release). Evidence: grep `overlay_prefs` = only those 2 files — after migration + edits below, grep `overlay_prefs` must be 0.

**FpsMonitor (fixes CRITICAL 1 continuation):** `FpsMonitor.kt:25-27` currently `getSharedPreferences("overlay_prefs")`. Replace with injected `HudSettingsCache`:
```kotlin
@Singleton class HudSettingsCache @Inject constructor(@ApplicationContext ctx: Context){
  @Volatile var fpsMode: String = "AUTO"
  init { CoroutineScope(Dispatchers.IO).launch{ ctx.hudFpsModeFlow.collect{ fpsMode = it } } }
}
```
`FpsMonitor` injects `HudSettingsCache`, `getAccessType()` reads `cache.fpsMode` (synchronous). Collector started from `SystemStatsApplication` (or cache init). Keeps `executeCommand` `FpsMonitor.kt:67-94` still `process.waitFor()` blocking but now mode is immediate (no DataStore suspend on hot path). Also add cached layer name (`findSurfaceFlingerLayer` result, 30s TTL) inside `FpsMonitor` per MAJOR 1.

**TopConsumersProvider (fixes MAJOR 3):** New `data/monitor/TopConsumersProvider.kt` authoritative for STACK T3 ledger. Interface:
```kotlin
class TopConsumersProvider @Inject constructor(private val taskRepo: TaskRepository, @ApplicationContext private val ctx: Context){
  suspend fun loadTopConsumers(max:Int=5): List<Consumer>
}
```
Impl: if `!taskRepo.hasUsageStatsPermission()` (`TaskRepository.kt:6`, `TaskRepositoryImpl.kt:22-38` `AppOpsManager.OPSTR_GET_USAGE_STATS`) return `emptyList()`. Else `taskRepo.getRunningProcesses()` (`TaskRepositoryImpl.kt:40-94` UsageStatsManager last-24h, sorted by `lastTimeUsed`) take `max`, map to `Consumer(pkg=packageName, label=appName.take(16), rssMb=0)`. Note: `TaskRepositoryImpl.getRunningProcesses` carries `AppProcessInfo(packageName, appName, icon, totalTimeInForeground, lastTimeUsed, isSystemApp)` and keeps RSS 0 — RSS per-app is not obtainable for 3p on API 26+ via `ActivityManager.runningAppProcesses` (`BenchGlance.kt:614` current fake RSS=0) nor `getProcessMemoryInfo` without visible pids. Hide ledger when empty: `BenchSnapshot.topConsumers` empty → STACK hides ledger rows (see 1.6). Do NOT render fake 0 MB rows. Future ponytail: if Shizuku/root present, fill `rssMb` via `dumpsys meminfo`.

**BenchFrames + receivers (fixes MINOR 2):** `BenchModel.kt:379-389` `LruCache.sizeOf` already `/1024` KB-correct at `:381`; remove `entryRemoved` `recycle()` at `:382-384` (dangerous while launcher holds Bitmap). For **all 5** receivers (only `SingleChannelWidgetReceiver onDeleted BenchFrames.clear()` at `BenchGlance.kt:629` today): add per-id cleanup:
```kotlin
override fun onDeleted(ctx: Context, ids: IntArray){ super.onDeleted(ctx,ids); ids.forEach{ BenchFrames.remove(idToKey(it)); BenchUpdater.lastPush.remove(idToString(it)) }; }
override fun onEnabled(ctx: Context){ super.onEnabled(ctx); BenchBudget.enqueue(ctx) }
override fun onDisabled(ctx: Context){ super.onDisabled(ctx); BenchBudget.cancelIfNone(ctx) }
```
Do NOT `clear()` globally.

**Glance providePreview (fixes MINOR 1):** Verified missing in Glance 1.1.0 (0 matches across 477 classes). Do NOT implement `providePreview`; static `previewImage` drawable per widget (PREVIEWS in 1.10) is mandatory and sufficient. No `@RequiresApi` on Glance method.

**Receiver FQN + WM keep (SUGGESTION, optional but pin):** Guard immutability: `app/src/test/.../widget/WidgetReceiversExistTest.kt` asserts `Class.forName("com.ivarna.deviceinsight.ui.caliper.widget.SingleChannelWidgetReceiver")` etc. for all 5 FQNs (`AndroidManifest.xml:79,91,103,115,127` `android:name=".ui.caliper.widget.*Receiver"` must never rename — placements bind them). Note `app/build.gradle.kts:67` `isMinifyEnabled=true` release — `work-runtime` ships consumer keep for `ListenableWorker`; no extra `proguard-rules.pro` keep needed unless R8 strips `BenchBudgetWorker` (verify `assembleRelease`).

### Phase 0 files (updated)

| Create | Modify |
|---|---|
| `data/monitor/MonitorBus.kt` | `DashboardRepositoryImpl.kt` (`delay 1000→500`, inject MonitorBus, drop BenchSnapshotCache) |
| `data/monitor/HudFeed.kt` (`HudSlow`/`HudFast`/`CoreStat`) | `domain/model/DashboardMetrics.kt` (gpuHist, netHist defaults) |
| `data/monitor/MemInfoParser.kt` (pure `parse` + normalization) | `data/fps/FpsMonitor.kt` (cache fpsMode via HudSettingsCache, cache layer, getCurrentFpsWithSource) |
| `ui/caliper/widget/BenchBudgetWorker.kt` | `ui/caliper/CaliperPrefs.kt` (hud* keys + hudMigrated + flows) |
| `ui/caliper/widget/BenchBudget.kt` (enqueue/cancelIfNone) | `data/provider/MemoryProvider.kt` (or caller — invoke parser) |
| `data/monitor/HudSettingsCache.kt` | `ui/caliper/widget/BenchModel.kt` (drop BenchSnapshotCache object, fill extra fields, fix BenchFrames) |
| `data/monitor/TopConsumersProvider.kt` | `app/build.gradle.kts` (work-runtime-ktx) |
| | `SystemStatsApplication.kt` (migration + enqueue WM + fpsMode collector) |
| | `ui/caliper/widget/BenchGlance.kt` (receivers onEnabled/onDisabled/onDeleted per-id) |
| | `app/src/test/.../BenchSelfCheckTest.kt` (bus, parser, fps source, migration) |
| | `app/src/test/.../WidgetReceiversExistTest.kt` (optional) |

### Phase 0 acceptance (updated per review)

- Single writer of `BenchSnapshot`: `Grep BenchSnapshotCache` = 0 **and** `Grep overlay_prefs` = 0; `MonitorBus` is only foreground writer (`DashboardRepositoryImpl→MonitorBus`). BUDGET path does NOT call `MonitorBus.push*` (verified via code search + `adb shell dumpsys jobscheduler` widget timestamp advances while `logcat | grep MonitorBus` shows no pushes when process cold).
- Overlay off + app background: WM fires ≤15 min (log tag `BenchBudget`; `BenchBudgetWorker` direct `sample→update` only).
- Overlay on: `HudFast` updates ~10 Hz on `Dispatchers.IO` ticker in log; `dumpsys` not called when overlay off; after 5× `—` ticker slows to 1 Hz; layer name cached 30s.
- `/proc/meminfo` parse unit-tested with fixture string: `activeF+cachedF+zramF+swapF+freeF` sums to 1 ±0.02, no negative segments, `BenchArt.hatchBar` `x+segW ≤ w`.
- `FpsSample.source` is one of `SF|GFX|—`; HUD shows `—` + `NO SIGNAL` when both SF/GFX fail, never falls back to `displayRefreshRateUtils`.
- Cycle count null when OEM hides it; widget/HUD never show `835`.
- Migration one-shot: fresh install with legacy `overlay_prefs` containing `fps_mode=ROOT` + `showCpu=false` upgrades to `caliper hudMigrated=true`, `fpsMode=ROOT`, HUD module mapping correct, `overlay_prefs` cleared; `FpsMonitor.getAccessType` returns ROOT without reading prefs.

---

## 3 · Plan A — BENCH widgets (Phase 1)

Work **in place** on `ui/caliper/widget/*`. Do not copy DI-WI-001 files.

### 1.1 Split files only if BenchGlance.kt stays painful

`BenchGlance.kt` is 648 lines. Optional split (same package, no behavior change):

- `BenchPanel.kt` — panel/header/footer/atoms/BandBitmap
- `BenchWidgets.kt` — five `GlanceAppWidget` classes
- `BenchReceivers.kt` — five receivers (keep class names)
- `BenchUpdater.kt` — nudge + worker hook

Do this as the first commit of Phase 1 if the worker touches most of the file anyway. **Receivers stay public with current FQNs.**

### 1.2 Hairline frame (4 sides) — **DONE (PASS 4). Do not rewrite `BenchPanel`.**

Current `BenchGlance.kt:169-190` already uses the pinned 4-side hierarchy (top 1dp, weight `Row` with left/right 1dp, bottom 1dp, 12dp outer pad, 9dp inner). Official Glance `defaultWeight()` docs match. QA remaining: T1 PAPER on cream wallpaper screenshot (acceptance below). If hairline is clipped on an OEM launcher, shrink inner pad 9→8 — do not change the Box/Column/Row structure.

Historical pin (kept so reviewers can match PASS 2):

```kotlin
@Composable private fun BenchPanel(pal: WidgetPalette, contentDescription: String, content: @Composable ColumnScope.()->Unit){
  Box(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(pal.panel)), contentAlignment = Alignment.TopStart){
    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.Top){
      Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(pal.hairline))) {}
      Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), verticalAlignment = Alignment.Top){ // weight absorbs remaining height
        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(ColorProvider(pal.hairline))) {}
        Column(modifier = GlanceModifier.defaultWeight().padding(horizontal = 9.dp), verticalAlignment = Alignment.Top){ content() }
        Box(modifier = GlanceModifier.width(1.dp).fillMaxHeight().background(ColorProvider(pal.hairline))) {}
      }
      Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(ColorProvider(pal.hairline))) {}
    }
  }
}
```

Notes: bottom hairline is inside the outer `Column` below the weight `Row`, not inside `Row` — so it never clips when `Row` weight is 0. Left/right hairlines are inside the weight `Row` so they stretch to full panel height via `fillMaxHeight`. `9.dp` inner horizontal padding (not 10) keeps T1 single-column content ≥116dp usable width (`140 - 2*12 inset - 2*1 hairline - 2*9 inner = 114dp`). QA: **T1 on PAPER cream wallpaper must show hairline** (manual screenshot gate); bottom hairline visible on light wallpapers (cream-on-cream), content weight does not collapse on launchers that give T1 exact height. Keep `contentDescription` on root `Box` for TalkBack.

### 1.3 Bitmap pipeline (correct Glance)

In `provideGlance` (suspend):

1. Read cfg + snap + tier from a *guess* is wrong — tier comes from `LocalSize` inside composition.
2. Compromise: render **all bands used by any tier** keyed by `BenchFrames.key(...)` on Default dispatcher using `snap` hashes, **or** render inside composition via `renderSync` as today.

**Pin:** keep `renderSync` (already off-main-safe, it's CPU on the Glance binder thread). Glance `provideGlance` already runs off main. Calling `renderSync` inside `provideContent` composable is synchronous during composition — acceptable if cache hits. Ensure cache key includes `contentHash` + medium + tier + band + lamp frame.

**Do not** `bitmap.recycle()` in `LruCache.entryRemoved` (`BenchModel.kt:382-384` `oldValue.recycle()` — remove override entirely; `sizeOf` `:381` `/1024` KB already correct, let GC reclaim. Fixes MINOR 2).

Pre-render in `provideGlance` before `provideContent` for the **current** snapshot at all 5 (or 4) sizes is too many bitmaps. Stick to composition-time `renderSync` with cache.

`BandBitmap` stays Glance `Image(ImageProvider(bmp), contentDescription, modifier.height(bandHeightDp.dp).fillMaxWidth())`. Fallback text `CALIBRATING…` only if bitmap alloc fails.

### 1.4 Cadence ladder

`cadenceMs()` already matches LIVE/AMBIENT. Change BUDGET `1_800_000` → `15 * 60_000`.

`BenchUpdater.nudge`:

- LIVE 1 s when `snap.charging || snap.serviceRunning` **and** cfg.cadence == LIVE (or cfg LIVE's inner rule — already in `cadenceMs`).
- Pulse 1 s when charging or critical regardless of cadence (lamp frame-swap) — already `pulse` branch.
- SIGNAL LOST: if `snap.stale(cadence)` still `update()` once to paint the stamp, then stop. Don't spin.

Receivers (pinned per-id, fixes MINOR 2 + MAJOR 2): For **each** of 5 receivers (`SingleChannelWidgetReceiver` at `BenchGlance.kt:627` today only `onDeleted{BenchFrames.clear()}` globally — bug; `DualChannel/Bench/Fuel/RasterWidgetReceiver` have no callbacks): add
```kotlin
override fun onEnabled(ctx: Context){ super.onEnabled(ctx); BenchBudget.enqueue(ctx) }
override fun onDisabled(ctx: Context){ super.onDisabled(ctx); BenchBudget.cancelIfNone(ctx) }
override fun onDeleted(ctx: Context, ids: IntArray){ super.onDeleted(ctx, ids); ids.forEach{ id -> BenchUpdater.evict(id); BenchFrames.remove(id) } }
```
where `BenchUpdater.evict` removes `lastPush` entry for that `appWidgetId` (store per-id), `BenchFrames.remove` per-id — do NOT `BenchFrames.clear()` globally. See Phase 0.9 for recycle removal and 0.7 for `BenchBudget` KEEP semantics.

XML: `updatePeriodMillis=0`, keep `widgetFeatures="reconfigurable"`, `configure=...BenchConfigActivity`.

BENCH `SizeMode.Responsive` = T2..T5 only (`280×140`, `280×210`, `280×280`, `350×280`). Others keep T1..T5.

### 1.5 States (§7)

| State | Implementation |
|---|---|
| Loading / CALIBRATING | `placedAt==0` write-once then sweep `<6s`; `BenchArt.calibrating`; LED off |
| Live | LED on (accent square); `upd HH:mm:ss` |
| Warning | temp>60 or batt<20% discharging → LED fault; affected numeral `fault` color |
| Critical | FUEL fill + % fault; lamp frame-swap |
| Charging | `CHARGING` stamp; wattage `+`; knob accent |
| Root locked | RASTER datasheet; `⚷`; `[ GRANT IN APP ]` → `open("calibrate")` |
| Not fitted | FUEL if `!batteryPresent`; RASTER if `!gpuFitted`; strike/`NOT FITTED` |
| No signal | empty hist → flat line + `NO SIGNAL` in bitmap (already in spark/scope) |
| Signal lost | LED off (`ink40` box); numerals ink/40; footer `SIGNAL LOST` |

`KEY_PLACED`: in `save()`, if key absent, set now; never overwrite.

### 1.6 Per-instrument remaining work (PASS 4 W1–W7)

Do **not** re-plumb data. `memComposition`, consumers, remainingMin, T4 spec, locked/not-fitted branches already render. This section is **layout**.

**WT-01 SCOPE — W1 + W2**
- T1: stacked hero + 28dp spark (keep).
- **T2+:** Glance `Row(fillMaxWidth)` — left `Column(defaultWeight)` = Hero + freq Subline + temp Subline; right `BandBitmap` scope (`defaultWeight`, 48dp T2 / 56dp T3+). Thermal ramp **below the Row** full width. Official Glance `defaultWeight` (build-ui docs). This is WD §4 T2; WI stacked layout is **wrong** here.
- Y-labels **inside** `Canvas.scope`: T2–T3 draw `100`/`50`/`0` at 10sp `pal.ink40` on the right 18dp of the bitmap (`showYLabels` param, default false). T4+: also 75/25. Do not add Glance `Text` in a third column (RemoteViews will overlap the trace).
- Governor subline, core rail T3+, header→`CH-01`, footer→`overview`: already. W8/W9 apply.

**WT-02 STACK — W3 + W4**
- Header trailing status = used% when live (`Fmt.pct(100f * memUsed/memTotal, 0)`), else CALIBRATING / SIGNAL LOST.
- Under hatch: one Subline joining non-zero `memComposition` labels (`active · cached · zram · free`). Text, not bitmap type.
- Consumers + empty-hide: already. Route stays `processes`. Do **not** invent `dossier:{pid}`.

**WT-03 FUEL — W5**
- `wattHero==true`: Hero `Fmt.wattsSigned`; next line battery `%` at 11sp; then gauge. `wattHero==false`: Hero `%`; next line watts.
- Remaining: format `remainingMin` as `Xh Ym remaining` if `>=60`, else `N min remaining`. Hide when 0.
- T4 spec rows: already. **No** T3 discharge-curve bitmap — `BenchSnapshot` has no % history. Do not plot wattHist as a % curve.

**WT-04 RASTER — W6**
- Live: Subline `"$gpuName · $gpuVulkan"` when either non-blank (WD T1 datasheet line). Locked/NOT FITTED already have it.
- Single `gpuHist` spark. No second freq hist in the snapshot — do not fake dual-trace.
- `GRANT IN APP` → `calibrate` → Overlay (1.8). Confirm mapper: `gpuFitted` vs `gpuRootLocked` against `GpuUsageProvider.sourceLabel` only if a device QA fails; do not invert without evidence.

**WT-05 BENCH — W7**
- Ledger T2: keep 4 `ChannelRow`s from `cfg.compactChannels`. Masthead add 6dp LED (`pal.accent` if !stale) + `HH:mm` (`SimpleDateFormat("HH:mm")`) after LIVE/SIGNAL LOST.
- T3+: **`chunked(2)`** (WD 2×3), not `chunked(3)`. Each cell `Column(defaultWeight)`: `CH-xx · NAME`, value, 14dp bitmap (CH-01 spark / CH-02 hatch / CH-03 spark / CH-04 fuelGauge / CH-05 hatch / CH-06 spark). T3 shows 5 cells (last row: one tile + `Spacer(defaultWeight)`). T4 all six. T5 + existing core rail. Click per tile → that CH route.

### 1.7 Config activity (live preview) — W10

Replace static `PreviewPanel` two-`Text` body (`BenchConfigActivity.kt:181-218`) with real CALIPER Compose inside `CaliperTheme(medium)`: `ChannelTick` + `OdometerText` + `ScopeTrace`/`HatchBar`/`LinearGauge` when those composables exist in `ui.caliper.components`. If a Compose twin is missing, draw a small `Canvas` spark from the hist — still Compose UI, never Glance. Demo feed: `GlobalSnapshot.current()` if `timestamp` age < 5 s, else `benchDemoSnapshot`. Follow-system DIP stays. SegKey media stays (three live mini-panels are nice-to-have; do not block SAVE on them).

`getInitialMedium()` currently always PAPER (`:76-79`). Read `mediumFlow.first()` in `onCreate` **before** `setContent` via `lifecycleScope.launch` + a `mutableStateOf` default, **not** `runBlocking` on the main thread.

Kind-specific: SCOPE window 60/300; FUEL wattHero; BENCH compact channel sets — already in the form. SAVE/SKIP/CANCEL contract already correct.

### 1.8 Deep links

Keep `actionStartActivity<MainActivity>(actionParametersOf(ROUTE to route))`. Extend `SystemStatsApp` `when (initialRoute)`:

| route | dest |
|---|---|
| `overview`, `CH-01`, `CH-02` | Dashboard |
| `CH-03` | Hardware tab 4 |
| `CH-04` | Hardware tab 5 |
| `CH-05` | Hardware tab 9 |
| `CH-06` | Hardware tab 3 |
| `processes` | Tasks |
| `calibrate` | **Overlay** (was Settings) |
| `hud-config` | Overlay |
| `dossier:*` | Tasks (until dossier route exists) |

`MainActivity.onNewIntent` already updates `diRoute`. `singleTop` OK.

### 1.9 A11y

**W9:** `BenchPanel(..., contentDescription)` currently **drops** the string (`BenchGlance.kt:164-191` unused param). Apply it: `GlanceModifier.semantics { contentDescription = desc }` on the root `Box` (`androidx.glance.semantics`). If 1.1.0 fails to compile that symbol, attach `desc` to the first `BandBitmap` **and** keep the param referenced (do not leave it dead). Glance `Image` already has `contentDescription`.

Example: `"Scope. CPU 38.4 percent. 2.84 gigahertz. 46 degrees. Updated 14:32:07."`

Min text 11sp (`MetaStyle`). No 9sp in tiles (`BenchWidgetAll` currently uses 11 — keep). Sketch's 9sp tile subline is **below spec** — do not copy.

### 1.10 Picker previews (15) — **DONE (PASS 4)**

All 15 `drawable-nodpi/preview_{scope,stack,fuel,raster,bench}_{paper,carbon,blueprint}.webp` exist; every `*_widget_info.xml` uses `previewImage=@drawable/preview_*_paper` (not `@mipmap/ic_launcher`). `WidgetReceiversExistTest` asserts this. **`providePreview` does NOT exist in Glance 1.1.0 — do not add it.** Do not regenerate WEBPs unless picker QA shows the launcher icon or a blank. Receiver labels + XML descriptions already match WD §8.

### Phase 1 files

| Modify | Do not create |
|---|---|
| `BenchGlance.kt` (W1 SCOPE T2 Row, W3–W7 STACK/FUEL/RASTER/BENCH, W8 Monospace, W9 semantics) | `preview_*.webp` (exist) |
| `BenchArt.kt` (W2 `scope(..., showYLabels)`) | `TopConsumersProvider.kt` (exist) |
| `BenchConfigActivity.kt` (W10 live Compose preview + mediumFlow) | 4-side `BenchPanel` rewrite |
| `SystemStatsApp.kt` (`calibrate` → Overlay, both NavHosts) | `MonitorBus` / WM / parser |

### Phase 1 acceptance

- Five picker entries, CALIPER descriptions, non-launcher preview images.
- Hairline visible on light and dark wallpapers (cream-on-cream).
- Resize SCOPE 2×2→4×2 **splits** hero left / trace right (W1); 4×4 **adds** core rail (growth, not stretch). Y-labels visible on T2+ bitmap (W2).
- STACK live header shows used %, not the word LIVE (W3). Composition Subline under hatch (W4).
- FUEL watt-hero shows a `%` line under watts (W5). BENCH T3+ is **2 columns** with a 14dp bitmap per tile (W7), not 3-col text.
- Unplug + force-stop app: within 2× cadence widget shows `SIGNAL LOST`, last numbers remain, LED off.
- Charging: FUEL `+` watts + `CHARGING`; LIVE widgets refresh ~1 s while app or overlay process alive (`adb shell dumpsys activity services` + log).
- Process dead: next update ≤15 min via WM (`adb shell dumpsys jobscheduler` / WorkManager dump).
- RASTER on a device with no GPU sysfs: `NOT FITTED`. On Adreno without read perm: `CHANNEL LOCKED` + GRANT.
- Config SKIP places widget with defaults. Reconfigure from launcher long-press works (`widgetFeatures=reconfigurable`).
- TalkBack on SCOPE reads a full sentence including `Updated`.
- Blueprint: traces ink-colored, hatch+`CH-xx` still identify channels.
- `BenchSnapshotCache` gone; `hatchBar` has no `sp_4` field.

---

## 4 · Plan B — Settings Instruments page (Phase 2)

Depends on Phase 1 receivers (already true). **PASS 3 F1 crash does not depend on Phase 1** — it is a Compose layout bug in already-landed Settings code. Land it as commit 0 of PR-B (or a tiny hotfix PR) before stamp/copy work.

### 2.0 CRITICAL — WidgetsSheet crash (PASS 3 F1)

**Repro:** Settings → tap INSTRUMENTS (`SettingsScreen.kt:116 onClick { showWidgets = true }`) → process dies.

**Throw site (Compose foundation, not Glance):**

```
java.lang.IllegalStateException: Vertically scrollable component was measured with
an infinity maximum height constraints, which is disallowed.
One of the common reasons is nesting layouts like LazyColumn and
Column(Modifier.verticalScroll()).
```

`verticalScroll` (and LazyColumn) call `checkScrollableContainerConstraints`. A parent `verticalScroll` measures children with `Constraints.maxHeight = Infinity`. The child `WidgetsSheet` Column is also `verticalScroll` + `fillMaxSize` → abort.

**Evidence in tree:**

```kotlin
// SettingsScreen.kt:69-73
Column(Modifier.fillMaxSize().caliperGrid().verticalScroll(rememberScrollState())) {
    if (showWidgets) {
        WidgetsSheet(onBack = { showWidgets = false })  // crash
    } else if (!showColophon) { /* settings body */ }
}

// WidgetsSheet.kt:58
Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { … }
```

**Not the crash:** `GlanceAppWidgetManager.getGlanceIds` (suspend, try/caught `:143-167`). A Glance failure already degrades to empty ACTIVE / `NOT PLACED`. Do not add more Glance try/catch as the "fix".

**Pinned fix (one scroller, bounded height). Do not `heightIn(max=9999.dp)` or `disableNestedScrollCrash` hacks.**

```kotlin
// SettingsScreen — outer Column is NOT scrollable. Each branch owns scroll.
Column(Modifier.fillMaxSize().caliperGrid()) {
    if (showWidgets) {
        WidgetsSheet(onBack = { showWidgets = false })   // fillMaxSize + verticalScroll OK: parent is bounded
    } else if (showColophon) {
        /* existing colophon, keep its own scroll if it has one */
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // existing settings body (header, media, INSTRUMENTS card, …)
        }
    }
}
```

`WidgetsSheet` keeps `Modifier.fillMaxSize().verticalScroll(...)` as the **only** scroller on that branch. Parent now has a finite maxHeight (`fillMaxSize` of the screen), so the child's scroll is legal.

**Padding:** `WidgetsSheet` Column pads 16.dp **and** `ScreenHeader` pads 16.dp (`CaliperChrome.kt:265`) → 32.dp double inset. After the crash fix, drop the Column's extra `padding(16.dp)` (header already insets; ADD/ACTIVE cards use `PanelCard` full-bleed inside the sheet). Not required to un-crash; do it in the same commit so the page is not cramped after it opens.

**BackHandler:** keep `if (showWidgets) BackHandler { showWidgets = false }` **outside** the scroll Column (current `:66` is fine). Do not put `BackHandler` inside `WidgetsSheet` as well.

**Acceptance for 2.0 (device, before any pin work):**
- Settings → INSTRUMENTS opens `№ 05.1` without crash, both with 0 widgets and with ≥1 placed.
- Back key / `← BACK TO SETTINGS` returns to `№ 05` without crash.
- Settings body (media / grid DIP / INSTRUMENTS card) still scrolls when the sheet is closed.
- No second `verticalScroll` remains in the `showWidgets` hierarchy (grep the two files: exactly one scroll modifier composed per branch).

### 2.1 Keep in-Settings boolean sheet

Do not add a NavHost route unless Overlay/Settings chrome fights BackHandler. Boolean `showWidgets` stays. Masthead hidden on Settings (`isSettings`) — sheet is full-bleed. **The current wrapper is not "works"** — it crashes (2.0). After 2.0 the boolean sheet is the right shape.

### 2.2 Card (№ 05)

Already `03 WIDGETS` / `INSTRUMENTS` / `$n ON BENCH` | `NO SIGNAL`. Keep. On resume of Settings, recount (already `LaunchedEffect(showWidgets)` when leaving the sheet — also refresh when Settings composable starts).

Copy: status stamp `NOT PLACED` when 0, `PLACED ×N` when N>0.

### 2.3 Page `№ 05.1 — INSTRUMENTS`

Restructure `WidgetsSheet` into two sections matching the ask:

**01 ADD** — one `PanelCard` per `WidgetKind` with:

- one-line personality (existing)
- status `NOT PLACED` | `PLACED ×N`
- HardKey `ADD TO HOME SCREEN` → `requestPinAppWidget` if `isRequestPinAppWidgetSupported`, else no-op + `MarginNote` (already)
- Manual 01–04 path (already)

**02 ACTIVE** — flatten to one row **per placed instance** (already) **plus** a roll-up per kind. Show medium · cadence · `upd`. HardKey `CALIBRATE` → `BenchConfigActivity` with `EXTRA_APPWIDGET_ID` (already). Note: launcher owns removal (already).

Refresh (pinned, fixes MAJOR 4): Primary = `delay+refresh` always. `WidgetsSheet` obtains `LocalLifecycleOwner` and registers `LifecycleEventObserver` for `ON_RESUME` → `refreshInstruments(ctx){c,list->...}` (current `WidgetsSheet.kt:33-36` only `LaunchedEffect(Unit)` once; `SettingsScreen.kt:38-54` only recounts when leaving sheet at `LaunchedEffect(showWidgets)` — both miss pin success). `requestPin` (currently `WidgetsSheet.kt:152-165` `mgr.requestPinAppWidget(cn,null,null)` with null callback) always launches `scope.launch { delay(1200); refreshInstruments(ctx) }` as the reliable path regardless of `isRequestPinAppWidgetSupported`. **Do NOT use `PendingIntent.getActivity(... SettingsActivity ...)` trampoline when hosted in `SystemStatsApp` NavHost** (`SystemStatsApp` hosts Settings via NavHost `SettingsScreen(...)`, not `SettingsActivity` except standalone — `WidgetsSheet` has no SettingsActivity context to deliver `PendingIntent.getActivity`). Optionally add `PendingIntent.getBroadcast` to a `PinSuccessReceiver` that sends a local broadcast, but do not make it required — delay+ON_RESUME is the gate for Phase 2 acceptance.

API 26 = minSdk: no `<26` branch. `isRequestPinAppWidgetSupported` false on some OEM launchers (Samsung/Xiaomi sometimes) — manual path is the degrade.

Do not switch to `GlanceAppWidgetManager.requestPinGlanceAppWidget` unless it gives us a GlanceId in the callback; platform API + ComponentName of the **existing receiver classes** is required so the launcher binds the same providers.

### Phase 2 files

| Modify | Why |
|---|---|
| `presentation/settings/SettingsScreen.kt` | **2.0** split scroll: outer Column not scrollable; settings body scrolls; `WidgetsSheet` is a sibling branch. Stamp copy on the INSTRUMENTS card. |
| `presentation/settings/WidgetsSheet.kt` | **2.0** remains the only scroller when shown; drop duplicate 16.dp padding. 2.3 ADD/ACTIVE copy already largely present. |

### Phase 2 acceptance

- **F1:** Settings → INSTRUMENTS never throws nested-scroll `IllegalStateException` (2.0 gate, blocking).
- Settings card shows `NOT PLACED` or `PLACED ×N`.
- ADD TO HOME SCREEN on Pixel launcher shows the system pin sheet; widget appears; ACTIVE list updates without killing the app.
- On a launcher that returns `isRequestPinAppWidgetSupported==false`, key is visible but MarginNote explains the manual path; no crash.
- CALIBRATE opens config for that instance; SAVE updates only that instance.
- TalkBack: each HardKey named; counts announced.

---

## 5 · Plan C — Scope Probe HUD (Phase 3)

Depends on Phase 0 feeds. Independent of widgets after that.

### 3.1 Package layout (`ui/caliper/hud/`)

Sketch names, **adapted to compile**, next to existing `CaliperHud.kt` (replace it):

| File | Role |
|---|---|
| `HudTheme.kt` | Distinct `enum class HudMedium { PAPER, CARBON, BLUEPRINT }` + `fun HudMedium.toCaliperMedium(): Medium` / `fun fromMedium(Medium)` (fixes MAJOR 6). Never a typealias over `Medium`; HUD never follows system (`CaliperTheme.kt:29` `Medium` without FOLLOW, `BenchModel.kt:42` `followSystem` is widget-only, `CaliperPrefs.kt:16` nullable medium for follow-system — HUD stores `HudMedium` directly with no `followSystem` bool). Palettes **copy Carbon/Paper/Blueprint from `CaliperColors`** — do not fork hex. |
| `HudModel.kt` | `HudConfig`, `HudModule`, `HudController` (or inject via OverlayViewModel + DataStore — prefer DataStore over a process object so config survives). Reuse `HudSlow`/`HudFast` from Phase 0. `FmtHud` can delegate to `Fmt`. |
| `HudAtoms.kt` | `hudFrame` corner brackets, `StrokedText`, `LedPulse`, `HairlineH`, `HudTick`, `SparkPen`, `MemBar`, `FuelMicro`, `CoreBank`, `MiniOdometer`, `thermalColor`, `HudStamp`, `clickableNoIndication`. |
| `HudModules.kt` | HM-0..HM-7 bands |
| `HudPanel.kt` | assembly + drag modifier |
| `HudDemo.kt` | preview / config-sheet live fake |

**Do not** implement sketch `demoHudFlows()` with `GlobalScope`.

`StrokedText`: `BasicText` + `SpanStyle(drawStyle=Stroke)` requires Compose UI 1.7 / BOM 2024.12 — present. Fallback: draw twice (stroke color then fill) as the sketch does.

`LedPulse`: honor `rememberReducedMotion()` from Caliper (already exists).

### 3.2 Rewrite `OverlayService`

Keep FQCN, FGS notification, `isRunning` AtomicBoolean (widgets use it for LIVE).

Replace View hierarchy with `ComposeView`:

1. `class OverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner`
2. `LifecycleRegistry` + `SavedStateRegistryController.create(this)`
3. `onCreate`: `performRestore(null)`; `lifecycle.currentState = RESUMED`; `startForeground(...)` **then** `canDrawOverlays` if false → `stopSelf(); return` (never `addView`; never `stopSelf` before `startForeground` on 26+). See §3.3.a.
4. `WindowManager.LayoutParams`: `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS`, `FLAG_NOT_TOUCHABLE` iff locked, `PixelFormat.TRANSLUCENT`, gravity START|TOP, x/y from prefs
5. API 31+: `FLAG_BLUR_BEHIND` + `setBlurBehindRadius((10*density).toInt())` if config.blurBehind; register `OnCrossWindowBlurEnabledListener`; if `false`, clear flag and pass `effectiveOpacity = (opacity+0.10f).coerceAtMost(0.97f)`
6. `ComposeView` + `ViewCompositionStrategy.DisposeOnDetachedFromWindow` + view-tree owners
7. Collect `MonitorBus.slow/fast` and `HudConfig` DataStore
8. `onDestroy`: `lifecycle.currentState = DESTROYED`; removeView; cancel scope; `isRunning=false`
9. Fast ticker 100 ms only while service running (Phase 0.2)
10. Drag: `onDrag` mutates params.x/y, persist
11. Lock: `HudController.update { locked=true }` → `updateViewLayout` flags
12. Tap (unlocked): `startActivity(MainActivity` extra `di_route=hud-config` `FLAG_ACTIVITY_NEW_TASK)`

Delete: programmatic ProgressBars, `OverlayGraphView`, `rounded_widget_background`, collapse/expand/snap-to-edge. Collapse is replaced by **lock** (passthrough). Close: keep a way to stop — overlay config STOP key, and/or a long-press on header while unlocked. **Pin: no close button on the probe; STOP lives on OverlayScreen.** Unlocked header crosshair locks.

Foreground notification: keep (FGS contract). Title `DeviceInsight HUD`.

`onStartCommand`: stop reading `showCpu`/`isHorizontal`/`scaleFactor`. Read HudConfig from DataStore. `START_STICKY`.

Intent extras from `OverlayViewModel.buildServiceIntent()`: shrink to empty / `action=START`. Config is DataStore, not extras (extras were losing module order on process death).

### 3.3 OverlayScreen = HUD config sheet (S-11)

Replace horizontal/scale UI with:

- Live `HudPanel` preview (uses `MonitorBus` if overlay running, else `HudDemo`) — **hosted per 3.3.b, never `fillMaxWidth` CaliperHud**
- `SegKey` S / M / L
- `SegKey` PAPER / CARBON / BLUEPRINT — **no follow-system**
- `FaderKey` opacity 0.40–0.90
- `DipSwitch` blur-behind (disabled/gray + MarginNote if SDK < 31)
- `DipSwitch` per `HudModule` (FPS, CPU, MEMORY, POWER, GPU, NETWORK, TRACE)
- `DipSwitch` showCoreBank
- HardKey `RESET POSITION`
- PERMISSIONS card (GRANT OVERLAY / GRANT USAGE / AUTHORIZE SHIZUKU) — keep
- Action row per **3.3.a** (START is not always present)
- Keep FPS mode AUTO/ROOT/SHIZUKU (feeds `FpsMonitor`)
- Drop metric-order drag list (`OverlayViewModel.METRIC_DEFINITIONS` time/cpuGraph/… at `OverlayViewModel.kt:207-223`) — modules replace it. Migration (pinned, fixes CRITICAL 1): map old `showFps`→FPS, `showCpu`→CPU, `showRam`/`showSwap`→MEMORY, `showPower`/`showBattery`→POWER, `showNetwork`→NETWORK, `showCpuGraph`/`showPowerGraph`→TRACE — done once in `SystemStatsApplication.onCreate` (Phase 0.9), not here. **Persist new keys ONLY in `caliper` DataStore** (`CaliperPrefs.kt:14` single file) — `CaliperKeys.hudMedium/hudScale/hudOpacity/hudBlur/hudLocked/hudModules(csv)/hudShowCoreBank/hudX/hudY/fpsMode/hudMigrated`. HUD stores distinct `HudMedium` (PAPER/CARBON/BLUEPRINT, never follow-system — see 3.1 MAJOR 6). No `or overlay_prefs`.

Per-app profiles (S-11): **out of scope** (ponytail: add when OverlayViewModel grows a package→modules map).

### 3.3.a START must not compose without overlay permission (PASS 3 F2)

Current (`OverlayScreen.kt:142-165`):

```kotlin
Row(...) {
    if (state.isServiceRunning) HardKey("STOP", ...)
    else HardKey("START", enabled = state.permissions.hasOverlay, onClick = { startForegroundService(...) })
}
```

Disabled START is still a primary-looking key at the bottom of a long sheet. User pin: **if overlay permission is not granted, START does not come**. `ui_ux_design.md:224` ("disabled state shows GRANT PERMISSION") is **superseded** — GRANT OVERLAY already exists in the PERMISSIONS card (`OverlayScreen.kt:73-80`). Do not duplicate it as a disabled START.

**Pinned composition (action row):**

```kotlin
when {
    state.isServiceRunning -> HardKey("STOP", variant = DESTRUCTIVE, /* always, even if permission later revoked */)
    state.permissions.hasOverlay -> HardKey("START", variant = PRIMARY, onClick = startHud)
    else -> { /* nothing — GRANT OVERLAY in PERMISSIONS is the only CTA */ }
}
```

**Pinned start path (defense in depth — UI is not the only caller; START_STICKY / notification / process restart can re-enter the service):**

1. `startHud` onClick: `if (!Settings.canDrawOverlays(context)) return@onClick`. Then `startForegroundService`. Pattern from FloatingWidgetCompose / sjf gist / OpenLumen: **check, then start, never start then hope**.
2. `OverlayService.onCreate` **first** (before `createOverlayView` / `addView` at `:79/:402`):
   ```
   if (!Settings.canDrawOverlays(this)) {
       startForeground(...)   // API 26+: MUST call startForeground before stopSelf or the FGS contract ANRs
       stopSelf()
       return
   }
   ```
   Plan 3.2 already says `canDrawOverlays` if false → `stopSelf()`. **Pin the `startForeground` order** — `stopSelf()` without `startForeground` after `startForegroundService` is a crash/ANR on 26+ (`ForegroundServiceDidNotStartInTimeException`). Current `onCreate` starts the view then `startForegroundService()` at `:80` — reorder to: permission check → `startForeground` → addView (or stopSelf).
3. `addView` stays in try/catch (`:401-405`) for OEM revoke mid-flight (`BadTokenException` / `WindowManager$BadTokenException`). On catch: `stopSelf()`.

**Refresh after grant:** `OverlayScreen` already `ON_RESUME` → `viewModel.refreshPermissions()` (`:35-40`). Keep. Add one `delay(400)` re-check in that observer (Android 8 `canDrawOverlays` stale — issuetracker 62047810). After grant, START **appears** (not enables). Do not auto-start the HUD on return from Settings — user taps START.

**API 30+ overlay settings intent:** `ACTION_MANAGE_OVERLAY_PERMISSION` with `package:` URI is "optionally … prior to R" (platform docs). Keep the URI (current `:77`); if the OEM lands on the list, the user still finds the app. No second deep-link API exists for 3p.

**Usage-stats is NOT a START gate.** Overlay window can run without it (Current App / STACK consumers degrade). Only `SYSTEM_ALERT_WINDOW` gates START.

**Acceptance:**
- Overlay permission denied: PERMISSIONS shows GRANT OVERLAY; action row has **zero** START keys (enabled or disabled). `adb shell dumpsys window` shows no `TYPE_APPLICATION_OVERLAY` for this pkg after tapping around the sheet.
- Grant overlay, return to sheet: within ~1 s START appears. Tap START → service + window.
- Revoke overlay while running: STOP still shown; service `addView`/`updateViewLayout` fails → `stopSelf`; next resume shows GRANT again, no START.
- TalkBack: START is absent from the tree when denied (not "disabled button").

### 3.3.b Demo HUD preview — wrap-to-scale, no overlapping rectangles (PASS 3 F3)

**Repro (in-sheet, no overlay permission needed):** Overlay tab. The demo probe is a full-width dark rectangle jammed under the header; SF stamp box overlaps the FPS numeral; GPU `71%` overlaps RAM/TEMP; corner brackets clip the TEMP line. Screenshot: `docs/testers/caliper-001/caliper-001-03-overlay.png`. Current host: `OverlayScreen.kt:61` `CaliperHud(preview, Modifier.fillMaxWidth().padding(horizontal = 16.dp))`.

**Root causes (all in `CaliperHud.kt` + host, not the live `OverlayService` window):**

| Cause | File | Why it overlaps / cramps |
|---|---|---|
| Preview is page-width | `CaliperHud` host `fillMaxWidth`; inner `HudRow` `fillMaxWidth` | Design probe is **196 / 260 / 300 dp** (`HudScales.of`, `overlay_redesign.md:187-189`, `HudPanel` `:980 .width(m.widthDp.dp)`). Stretching to ~360–412 dp makes a "card" that reads as another settings panel, then the PERMISSIONS `PanelCard` rectangle sits flush against it. |
| Fixed 16.dp rows | `HudRow` `:87` `Modifier.fillMaxWidth().height(16.dp)` | 13sp meta + 6dp tick + 12dp spark cannot fit. Compose clips; GPU value paints on top of the RAM `Row` below. Official layout docs: siblings without spacing **stack**. |
| SF stamp is a rectangle | `StampBadge` (`CaliperPrimitives.kt:130-137`) `border(1.5.dp)` + `padding(10×4)` + `graphicsLayer` scale 1.12 | The bordered box is taller than the FPS `Row`; it overflows into the hairline / CPU row. HUD already passes `rotation = 0f` — keep 0; still needs a reserved slot. |
| Brackets share the content box | `:57` `padding(10.dp).drawBehind { drawCornerBrackets(..., inset=10, len=12) }` | Inset equals padding, so ⌜⌝ sit on the numerals. Bottom-right bracket collides with TEMP. |
| Scrim blur is a bleeding rectangle | `:48-54` `Box(matchParentSize.background(scrim).blur(8.dp))` | `Modifier.blur` expands drawing bounds. The dark rectangle halo overlaps ScreenHeader above and PERMISSIONS below. Plan already forbids `Modifier.blur` as wallpaper blur (3.2 uses `FLAG_BLUR_BEHIND`). **Also forbid it on the config-sheet preview.** |
| Packed demo state | `OverlayScreen.kt:44-48` always feeds fps+cpu+gpu+ram+temp+net | Preview should follow **current `HudConfig.modules`**, default M = FPS+CPU+MEMORY+POWER (no GPU/NET/TRACE until toggled). Fewer bands = not cramped. |

**Pinned preview host on OverlayScreen (stage, not a second WindowManager overlay — that would require the permission we just gated):**

```kotlin
val scale = state.hudScale // S/M/L from DataStore, default M
val probeW = when (scale) { HudScale.S -> 196.dp; HudScale.L -> 300.dp; else -> 260.dp }
Box(
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),   // gap vs header AND vs PERMISSIONS
    contentAlignment = Alignment.TopCenter
) {
    Box(  // graph-paper stage so the probe reads as a clip, not a page card
        Modifier
            .width(probeW)
            .wrapContentHeight()
            .background(Caliper.colors.panel)
            .padding(12.dp)          // stage pad — brackets never clip the stage edge
    ) {
        HudPanel(                    // or CaliperHud until HudPanel lands; same width contract
            config = hudConfigFromState,
            /* demo or live */,
            modifier = Modifier.width(probeW).wrapContentHeight()
        )
    }
}
```

Never `fillMaxWidth` on the probe itself. Center it. 12.dp vertical gap on the host so the dark rectangle cannot kiss PERMISSIONS.

**Pinned probe internals (`HudPanel` / interim `CaliperHud`):**

- Width = `HudScales.of(scale).widthDp.dp`. Height = wrap. `clipToBounds()` on the scrim Box so the rectangle cannot paint outside.
- Scrim = `background(medium.scrim.copy(alpha = opacity))`, radius **0**. **No `Modifier.blur`.** Window blur is service-only (`FLAG_BLUR_BEHIND`, 3.2).
- Frame = corner brackets as an overlay Box (`hudFrame`) with **content padding ≥ 12.dp inside the brackets** (`HudScales.padDp` is 10/12/14). Brackets are 1.5.dp, inset 0 from the scrim edge, length 12.dp. Numerals never sit on ⌜⌝.
- Column: `verticalArrangement = Arrangement.spacedBy(6.dp)` (4.dp grid × 1.5). Hairlines between modules, not between every micro-row.
- Rows: `wrapContentHeight()` + `heightIn(min = 22.dp)`. **Delete `height(16.dp)`.** Spark sits in a reserved `40×12.dp` slot on M+, omitted on S.
- FPS stamp: not `StampBadge` (that's a page chrome rectangle). Use `HudStamp` — 9sp meta in `[SF]` with 1.dp hairline box **or** no box, aligned `TopEnd` in a `Row` that gives the odometer `weight(1f)` so the stamp cannot overlap the numeral. No `graphicsLayer` scale pop on the HUD.
- Modules rendered = `config.modules` only. Demo feed (`HudDemo`) still animates, but GPU/NET/TRACE are off until the DIP is on. Default set already in `HudConfig` (`HudModel.kt:17`).
- Live vs demo: if `OverlayService.isRunning` collect `MonitorBus`; else `HudDemo`. Preview is not `TYPE_APPLICATION_OVERLAY`.

**Do not** implement the config-sheet preview as a real overlay window "to look more real" — that fights F2 (permission) and would cover the sheet.

**Acceptance:**
- Overlay tab, permission denied or granted: preview is a **260.dp-wide** (M) probe centered, with visible gap above PERMISSIONS. No full-bleed dark rectangle.
- No two strings share pixels: FPS numeral vs `[SF]`; CPU row vs GPU row; GPU vs RAM/TEMP; brackets vs TEMP. Screenshot gate against `caliper-001-03-overlay.png` (that PNG is the **fail** fixture).
- S / M / L SegKey changes preview width 196 / 260 / 300 and which bands appear (S drops CoreBank + TRACE; L adds them) — growth, not font stretch (DI-HD-001 §3).
- Toggling GPU / NETWORK DIPs adds a band without overlapping existing ones.
- Preview never calls `WindowManager.addView`.

### 3.4 Delete

- `presentation/overlay/OverlayComponents.kt`
- `service/overlay/OverlayGraphView.kt`
- usages of `R.drawable.rounded_widget_background` from overlay (drawable may stay if widgets don't use it — they don't)

### 3.5 Sketch compile fixes (do not copy blindly)

- `clickableNoIndication` — real `Modifier.composed { clickable(indication=null, interactionSource=remember{MutableInteractionSource()}, onClick) }`
- `StrokeCap` import: `androidx.compose.ui.graphics.StrokeCap` not `drawscope.StrokeCap`
- `Box.border` needs `foundation.border`
- `HudController` as DataStore, not a writable object from the service **and** the activity without a process
- `MonitorRepository.slow` in the sketch → `MonitorBus`
- No `GlobalScope`

### Phase 3 files

| Create | Modify | Delete |
|---|---|---|
| `ui/caliper/hud/HudAtoms.kt` | `ui/caliper/hud/HudTheme.kt` (**fill** palettes/scales/locals from DI-HD-001 — file exists as enums only) | `presentation/overlay/OverlayComponents.kt` |
| `ui/caliper/hud/HudModules.kt` | `ui/caliper/hud/HudModel.kt` (keep `HudConfig`; add nothing that duplicates `HudFeed`) | `service/overlay/OverlayGraphView.kt` |
| `ui/caliper/hud/HudPanel.kt` | `service/OverlayService.kt` (rewrite to ComposeView WRAP_CONTENT) | `ui/caliper/hud/CaliperHud.kt` (replaced by HudPanel) |
| `ui/caliper/hud/HudDemo.kt` | `presentation/overlay/OverlayScreen.kt` (**F2 START `when` + F3 wrap-to-scale preview host** — land even if HudPanel is still in flight) | |
| | `presentation/overlay/OverlayViewModel.kt` (drop `scaleFactor`/`isHorizontal`; expose HudConfig) | |
| | `AndroidManifest.xml` (unchanged service name; confirm specialUse) | |
| | `SystemStatsApp.kt` (`hud-config` / `calibrate` → Overlay, **both** NavHosts) | |

### Phase 3 acceptance (from DI-HD-001 checklist)

- Corner brackets 1.5 dp, radius 0, no rounded card.
- Channel color always with `CH-xx` + tick; Blueprint all-ink.
- FPS band ~10 Hz; other bands skip when `HudSlow` equal (Layout Inspector / log recompositions in debug).
- Blur unsupported → opacity +10 pt; panel still legible on white/black/mid-gray (stroke test).
- `+0.00 W` signed; `SWP — NOT FITTED` when swapTotal==0.
- Locked = passthrough (`FLAG_NOT_TOUCHABLE`); unlocked = drag + tap-⌖-to-lock + tap opens OverlayScreen.
- Thermal recolor 65 / 75 °C.
- Reduced motion: no LED breathe, no spring overshoot.
- `canDrawOverlays==false` → service does not add a view.
- **F2:** Overlay sheet with overlay permission denied: START is **not in the composition** (Layout Inspector / TalkBack). GRANT OVERLAY is the only overlay CTA. After grant + resume, START appears; tap starts FGS. Service `onCreate` without permission: `startForeground` then `stopSelf`, no `addView`.
- **F3:** Overlay sheet preview is wrap-to-scale (M=260.dp), centered, 12.dp gap to PERMISSIONS; no row/stamp/bracket overlap; no `Modifier.blur` on the preview; modules follow DIPs. Fail-fixture = `docs/testers/caliper-001/caliper-001-03-overlay.png`.
- Process death: config (medium/scale/modules/opacity/xy/fpsMode) restored from `caliper` DataStore only (migration already cleared `overlay_prefs`; no dual-read).

---

## 6 · Dependency graph

```
Phase 0  Shared bus + meminfo + fps source + WM + snapshot fields
   │
   ├──────────────► Phase 1  Widgets to spec (Plan A)
   │                    │
   │                    └──► Phase 2  Settings stamps/pin polish (Plan B)
   │
   └──────────────► Phase 3  Overlay rewrite (Plan C)   [parallel with 1 after 0]

Phase 4  (thin) 15 previews if not in P1 · instrumented HUD/widget QA · delete leftovers
```

P1 and P3 both consume `MonitorBus` — **already in tree**. Do not land a second Phase 0. Branch P1 layouts and P3 HUD in parallel after F1.

Settings P2 polish can start as soon as receivers exist (they do) but should wait for P1 hairline/previews so the pin preview isn't the launcher icon. **P2.0 F1 crash fix does not wait** — ship it as soon as Settings is touched.

Suggested PR slice (PASS 4 — Phase 0 and 15 previews already in tree):

1. ~~PR-0 foundation~~ **skip** unless `assembleDebug` fails on existing bus/parser/WM
2. PR-A widgets **layouts only** (W1–W10: SCOPE T2 Row, y-labels, STACK %, FUEL secondary %, RASTER subline, BENCH 2-col, Monospace, semantics, config Compose preview)
3. PR-B settings: **only Phase 2.0 nested-scroll crash (F1)** — copy/pin polish already landed
4. PR-C overlay rewrite + OverlayScreen **including F2 START gate + F3 preview layout** (F2/F3 can land as the first OverlayScreen commit of PR-C even before HudPanel exists — apply the START `when` and the wrap-to-scale `CaliperHud` host immediately, then swap CaliperHud → HudPanel)
5. ~~PR-previews~~ **skip** — 15 WEBPs on disk

**Hotfix exception:** F1 is a crash in shipped Settings. It does not need Phase 0/1. If PR-B is delayed, F1 may ship as a one-file `SettingsScreen.kt` PR. F2 is a one-file `OverlayScreen.kt` composition change and may ship the same way. F3 internals want `HudPanel` but the host (`width(260.dp)` + `spacedBy` + drop `height(16.dp)` + drop `Modifier.blur`) can land on `CaliperHud` without waiting for the full HUD rewrite.

---

## 7 · Risks / unknowns + mitigations

| Risk | Mitigation |
|---|---|
| Glance `providePreview` — VERIFIED ABSENT in 1.1.0 (PASS 2: `javap` + `grep -rl` across 477 classes, 0 matches; control `provideGlance` matched) | Do NOT implement `providePreview`; static `previewImage` drawable is mandatory and sufficient; no `@RequiresApi` on Glance method (Glance API, not SDK gate) |
| `LaunchedEffect` in Glance silently no-ops | Never use effects in Glance; pre-render / `renderSync` |
| RemoteViews tree too big (5 sizes × tiles × consumers) | BENCH drops T1; STACK consumers max 5; bitmaps not nested layouts |
| `Bitmap.recycle` while launcher holds it (`BenchModel.kt:382-384`) | Remove `entryRemoved` `recycle()` override; `sizeOf` `:381` `/1024` KB already correct |
| WM 15 min inexact + doze | Accept; SIGNAL LOST is the honest state. `setRequiresBatteryNotLow(false)`. No exact alarms (Play policy) |
| LIVE 1 s kills battery if overlay+widgets | LIVE 1 s **only** when charging **or** overlay service running (already). Ambient 30 s otherwise |
| `dumpsys` 10 Hz too heavy / blocked without root (`FpsMonitor.kt:67-94` `executeCommand` `waitFor()` 100–300 ms) | `FpsTicker` on `Dispatchers.IO` (never Main — `OverlayService.kt:548` anti-pattern), started/cancelled with service lifecycle; cache layer name 30s; adaptive 1000 ms after 5× `—`; honest `—` for `HudFast.source`, no `displayRefreshRateUtils` fallback for HUD |
| OEM `CURRENT_NOW` units (µA vs mA) | `PowerProvider` already assumes µA; if \|watts\| > 20, treat as mA (`ponytail` in mapper). Don't block |
| `computeChargeTimeRemaining` only while charging | Hide remaining on discharge |
| Pin unsupported (OEM launchers) | Manual path; no crash |
| Existing placed widgets bound to `SingleChannelWidgetReceiver` | **Never rename receivers** |
| ComposeView in overlay crashes without SavedStateRegistry | Wire controller as specified; add a try/catch `stopSelf` + log if `addView` throws |
| `FLAG_BLUR_BEHIND` ignored on many OEMs | Opacity fallback is mandatory, not optional |
| `Modifier.blur` mistaken for wallpaper blur **and** bleeds the preview rectangle (F3) | Do not use `Modifier.blur` on HUD **or** the OverlayScreen preview. Window blur = `FLAG_BLUR_BEHIND` only |
| Nested `verticalScroll` in Settings→WidgetsSheet (F1) | One scroller per branch; outer `SettingsScreen` Column is not scrollable when the sheet is showing (`checkScrollableContainerConstraints`) |
| START composed while overlay permission denied (F2) | Do not compose START; GRANT OVERLAY in PERMISSIONS is the CTA. Service: `startForeground` then `stopSelf` if `!canDrawOverlays` |
| `canDrawOverlays` stale on Android 8 after grant (issuetracker 62047810) | `ON_RESUME` + 400 ms delayed re-check; do not auto-start |
| `ACTION_MANAGE_OVERLAY_PERMISSION` package URI ignored on API 30+ | Keep URI; user may land on the list; START appears only after `canDrawOverlays==true` |
| FGS `startForegroundService` then `stopSelf` without `startForeground` (API 26+) | Permission-fail path must `startForeground` first |
| Demo HUD `fillMaxWidth` + `HudRow height(16.dp)` overlap (F3) | Wrap-to-scale 196/260/300; `heightIn(min=22.dp)` + `spacedBy(6.dp)`; no StampBadge on HUD; brackets overlay with inner pad |
| Glance IBM Plex missing | `FontFamily.Monospace` + `tnum` |
| `runningAppProcesses` empty (API 26+, `BenchGlance.kt:614` RSS=0) | `TopConsumersProvider` via `TaskRepository.hasUsageStatsPermission()` (`TaskRepositoryImpl.kt:22-38`); hide ledger when empty; label-only rows when RSS unavailable (honest, no fake 0 MB) |
| Battery cycle mock 835 leaks into FUEL T4 | Null-safe; never default 835 on widget/HUD path. Fix `BatteryProvider` default to -1/null in the same PR if touched |
| Overlay START from OverlayScreen vs FGS type `specialUse` Play declaration | Manifest property already `"Performance monitoring overlay"` — keep |
| Dual NavHost copies in `SystemStatsApp` (wide vs narrow) | `calibrate`/`hud-config` must be edited in **both** `NavHost` blocks, or extract `navGraph` lambda (small refactor, do it) |
| minSdk 26 vs sketch `TYPE_PHONE` | Delete pre-O branch |
| WorkManager + F-Droid | `work-runtime-ktx` is AOSP-friendly; no GMS |

---

## 8 · Verification

### Unit (every phase)

- `MemInfoParser` fixture: `activeF+cachedF+zramF+swapF+freeF` sums to 1 ±0.02, de-duplicated ZRAM (`swapF = max(0, swapRaw - zramF)`), normalized if sum>1, free never negative (covers CRITICAL 3)
- `toBenchSnapshot` mapping (extend `BenchSelfCheckTest`): currentMa, gpuHist, netHist, batteryPresent, cycleCount null (no 835)
- `stale` / `warning` / `Tier.of` / Blueprint ink (already)
- `cadenceMs` LIVE/AMBIENT/BUDGET numbers (BUDGET = `15*60_000`)
- `FpsSample` source mapping with a fake (extract pure function if dumpsys is hard) — source in `{SF,GFX,—}`
- HudConfig round-trip csv `hudModules` + `HudMedium` mapping + `HudSettingsCache` fpsMode cache
- `caliperRailOrder` untouched
- `WidgetReceiversExistTest` (optional): `Class.forName` 5 FQNs (`AndroidManifest.xml:79,91,103,115,127`)

### Compile

`./gradlew :app:assembleDebug` after each phase (and `:app:assembleRelease` once after Phase 0 to verify `BenchBudgetWorker` survives R8 `isMinifyEnabled=true`). **No `providePreview` implementation** — it does not exist in Glance 1.1.0, no `@RequiresApi` needed. Static `previewImage` drawable per widget is mandatory.

### Manual device QA

Widgets:

```
adb shell appwidget grantbind --package com.ivarna.deviceinsight --user 0
# pin from Settings, or:
adb shell am start -a android.appwidget.action.APPWIDGET_CONFIGURE
```

- Place all 5; screenshot PAPER/CARBON/BLUEPRINT (config).
- Resize each through T1–T5 (BENCH T2–T5). Confirm growth not stretch.
- `adb shell am force-stop com.ivarna.deviceinsight` then watch SIGNAL LOST.
- `adb shell dumpsys battery set status 2` (charging) → LIVE 1 s while overlay running.
- TalkBack on each widget.
- Tap header → correct Hardware/Overview tab.
- Cream wallpaper + PAPER widget: hairline (4-side 1dp strips inside 12dp inset, pinned 1.2 hierarchy) still visible — T1 PAPER 140dp screenshot gate; verify bottom/left/right not clipped via launcher with exact heights.

Settings:

- **F1 crash gate (do this first):** Settings → INSTRUMENTS. Must open. Rotate, back, reopen. `adb logcat *:E` must not contain `checkScrollableContainerConstraints` / `infinity maximum height`.
- Card stamp; pin SCOPE; list updates; CALIBRATE; launcher without pin support (or mock by short-circuit) shows note.

HUD:

```
# F2 — denied: START must not exist
adb shell appops set com.ivarna.deviceinsight SYSTEM_ALERT_WINDOW deny
# open Overlay tab: Layout Inspector / uiautomator dump → no node with text START
# granted: START appears, tap starts window
adb shell appops set com.ivarna.deviceinsight SYSTEM_ALERT_WINDOW allow
# START from Overlay sheet
adb shell dumpsys window | grep -A2 DeviceInsight   # overlay attached
```

- **F3 screenshot gate:** Overlay tab preview vs fail-fixture `docs/testers/caliper-001/caliper-001-03-overlay.png`. Probe width ~260.dp (M), centered, gap above PERMISSIONS, no overlapping numerals/stamp/brackets. S/M/L changes width. GPU DIP off → no GPU row.

- Drag while unlocked; tap ⌖ locks; touches pass through to launcher/game.
- API 31 emulator with blur disabled (dev option “Disable blur” if present) → opacity bump.
- FPS stamp SF on a game with Shizuku, GFX on a UI app, `—` with no elevated perms.
- S/M/L add instruments; Blueprint over a bright scene still readable (stroke).
- STOP removes view; notification gone.

### A11y

Extend `CaliperA11yTest` only for OverlayScreen/Settings HardKeys if cheap. Widgets need a device; no reliable Glance Espresso. Manual TalkBack is the gate.

---

## 9 · Open questions (non-blocking defaults pinned)

| Q | Default unless user overrides |
|---|---|
| minSdk bump? | **No.** Stay 26. |
| Keep old overlay behind a flag? | **No.** Delete internals. |
| Rename OverlayService → HudService? | **No.** Keep FQCN. |
| Move package to `widget.bench`? | **No.** |
| Rename receivers (SingleChannel→Scope)? | **No.** Breaks placements. |
| Per-app HUD profiles (S-11)? | **Defer.** |
| Process dossier deep link? | Route to Tasks until dossier exists. |
| 15 marketing previews blocking? | Must ship before Play screenshots; can be last commit of Plan A, not a separate release. |
| WorkManager vs only `updatePeriodMillis=30min`? | **Add WM.** 30 min is not design BUDGET. |
| Follow-system on HUD? | **Never.** Explicit medium only. |
| Fake battery cycles (835)? | **Stop** on widget/HUD path. |

No blocking product decision remains. **Worker: do not start Phase 0 from scratch** — those files are in the tree. Execute **F1 (`SettingsScreen` scroll split) first** (crash), then Phase 1 W1–W10 widget layouts, then Phase 3 overlay (F2/F3 first OverlayScreen commit, then HudPanel + OverlayService rewrite).

---

## 10 · Out of scope

- New CPU per-core *load* from `/proc/stat` (keep freq/max proxy; ponytail stays)
- Real process dossier sheet
- Per-app HUD profiles
- Changing Compose BOM / Glance major
- Play Store listing copy beyond picker descriptions
- QS tile (already `MediaTileService`)
- Recalibrating Overview/Hardware pages

---

## 11 · Lazy alternative (named, not chosen)

Ship only Phase 0 + hairline + WM + OverlayService Compose wrap of **existing** `CaliperHud` stub — skip 15 previews, skip STACK cadastral, skip HUD modules. That is a weekend. It is **not** DI-WD/DI-HD. Full plan above is the asked scope.

`ponytail:` per-core load, zram mm_stat on locked devices, per-app HUD profiles, dossier routes — add when those surfaces exist.

---

## 12 · Implementation notes for the worker (Glance/HUD cheat sheet)

```kotlin
// state — real 1.1.0 (only correct APIs)
updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { p ->
    p.toMutablePreferences().apply { this[KEY] = value }
}
widget.update(context, id)
val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id) // suspend, returns Preferences directly, no firstOrNull()

// pin — platform (MAJOR 4: delay+ON_RESUME, never trampoline to SettingsActivity when in NavHost)
val pinOk = AppWidgetManager.getInstance(ctx).isRequestPinAppWidgetSupported
if(pinOk) AppWidgetManager.getInstance(ctx).requestPinAppWidget(ComponentName(ctx, receiver), null, null)
scope.launch { delay(1200); refreshInstruments(ctx) } // primary refresh — always

// HUD migration (CRITICAL 1)
// caliper DataStore only — never overlay_prefs; fpsMode via HudSettingsCache @Volatile fed by caliperDataStore
// SystemStatsApplication.onCreate: migrate overlay_prefs → caliper hud* if !hudMigrated, then clear()

// BUDGET WM (MAJOR 2)
BenchBudget.enqueue(ctx)  // from Application.onCreate + each onEnabled
BenchBudget.cancelIfNone(ctx) // from each onDisabled — counts all 5 kinds
// PeriodicWork 15min KEEP, BUDGET worker: BenchSampler.sample() → direct widget.update(), never MonitorBus

// MemInfoParser math (CRITICAL 3): MemTotal denominator; de-dup zram; normalize; order active,cached,zram,swap,free
// HudMedium (MAJOR 6): distinct enum {PAPER,CARBON,BLUEPRINT}, no followSystem, mapping fns to/from Medium
// Hairline (MAJOR 5): Box(fillMaxSize.bg(panel)) { Column(padding 12dp) { 1dp top; Row(defaultWeight){ 1dp left; Column(defaultWeight,pad 9dp){content}; 1dp right }; 1dp bottom } }
// BenchFrames (MINOR 2): never recycle(), per-id eviction in onDeleted, not clear()
// FpsTicker (MAJOR 1): Dispatchers.IO scope owned by OverlayService onCreate→onDestroy, 100ms adaptive to 1000ms after 5× "—"

// overlay owners
savedStateRegistryController = SavedStateRegistryController.create(this)
savedStateRegistryController.performRestore(null)
lifecycleRegistry.currentState = Lifecycle.State.RESUMED
view.setViewTreeLifecycleOwner(this)
view.setViewTreeSavedStateRegistryOwner(this)

// F2 — START composition (OverlayScreen action row). GRANT OVERLAY lives in PERMISSIONS only.
when {
    running -> HardKey("STOP", DESTRUCTIVE)
    Settings.canDrawOverlays(ctx) -> HardKey("START", PRIMARY) { startForegroundService(...) }
    else -> Unit
}
// OverlayService.onCreate: if (!canDrawOverlays) { startForeground(...); stopSelf(); return }

// F3 — preview host. Probe is wrap-to-scale, never fillMaxWidth. No Modifier.blur on the sheet.
Box(Modifier.fillMaxWidth().padding(horizontal=16.dp, vertical=12.dp), Alignment.TopCenter) {
    HudPanel(..., Modifier.width(HudScales.of(scale).widthDp.dp).wrapContentHeight())
}

// F1 — Settings instruments. ONE verticalScroll per branch. Never nest.
Column(Modifier.fillMaxSize().caliperGrid()) {
    if (showWidgets) WidgetsSheet(...)  // this Column is the scroller
    else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { /* settings */ }
}

// W1 — SCOPE T2+ (Glance). T1 stays stacked. Official defaultWeight.
Row(GlanceModifier.fillMaxWidth()) {
    Column(GlanceModifier.defaultWeight()) { Hero(...); Subline(freq); Subline(temp) }
    BandBitmap(..., "scope", ...) { c, w, h, d -> c.scope(..., showYLabels = tier >= Tier.T2) }
}
// W7 BENCH tiles: chunked(2), not 3. W8: TextStyle(fontFamily = FontFamily.Monospace) on every Glance Text.
// Overlay WindowManager: WRAP_CONTENT x WRAP_CONTENT (handstandsam gist), never MATCH_PARENT.
```

Do not copy `widget.updateState`, `currentState().firstOrNull()`, `LaunchedEffect` in Glance, `MonitorRepository`, `HudService`, `GlobalScope`, `TYPE_PHONE`, `Modifier.blur` as wallpaper blur **or as the OverlayScreen preview scrim**, `BatteryProvider` 835, `BenchFrames` recycle, `placedAt` overwrite, `calibrate`→Settings, `providePreview` (does not exist in 1.1.0), `widget.updateState` sketch API, `isSystemInDarkTheme()` in Glance, `android.graphics.Color.valueOf` in Glance, `MonitorBus as object` (use Hilt @Singleton), disabled START as a stand-in for GRANT OVERLAY, `HudRow height(16.dp)`, nested `verticalScroll` in SettingsScreen→WidgetsSheet, IBM Plex inside Glance (unsupported — Monospace only), WI stacked SCOPE T2, BENCH `chunked(3)`, 16dp band gaps on T1, recreate `MonitorBus`/`MemInfoParser`/`preview_*.webp`/`BenchPanel` 4-side frame, call `CaliperHud` the Scope Probe.

---
## 13 · PASS 2 Handoff — what the Plan Reviewer must re-validate

This revision (PASS 2) addressed `docs/reviews/PLAN_widgets_overlay-plan-review.md` PASS 1 ITER 1 verdict REVISE (3 CRITICAL + 6 MAJOR + 2 MINOR + 1 SUGGESTION). Each finding is pinned to a section; reviewer should verify the exact change, not re-discover the repo.

PASS 3 (this file, 2026-08-23) adds three **field defects** that were not in the plan-review: F1 Settings widgets nested-scroll crash (§2.0), F2 Overlay START visible without `canDrawOverlays` (§3.3.a), F3 Overlay demo HUD cramped/overlapping rectangles (§3.3.b). Sources: Context7 `/websites/developer_android` + Compose + Glance library IDs; platform `ACTION_MANAGE_OVERLAY_PERMISSION` / `canDrawOverlays`; AOSP `checkScrollableContainerConstraints`; GitHub FloatingWidgetCompose / JetOverlay / ComposeOverlayViewService; tester PNG `docs/testers/caliper-001/caliper-001-03-overlay.png`. Worker implements F1 before any Settings polish; F2/F3 with OverlayScreen (F2 is composition-only and can hotfix).

PASS 4 (this file, 2026-08-23) is a **code-vs-design audit**, not a new field-bug pass. Widgets are a family (5 Glance instruments, 4-side frame, 15 previews, 3 media, cadence/WM) but several **layouts** still follow the WI stacked sketch instead of WD. Overlay is **not** DI-HD-001 — `HudPanel` does not exist; the window is still Views. Do not recreate Phase 0. Sources: current `BenchGlance.kt`/`CaliperHud.kt`/`OverlayService.kt`; DI-WD-001 / DI-WI-001 / DI-HD-001; Glance build-ui (2026-08-20) `FontFamily.Monospace` + `defaultWeight` + Box-stacks; handstandsam WRAP_CONTENT overlay gist. Context7 CLI was blocked this pass (`npx ctx7@latest`); used official webfetch + PASS 3 library IDs.

| Finding | Severity | Where pinned |
|---|---|---|
| HUD config persistence split + migration undefined — dual stores (`CaliperPrefs.kt:14`, `FpsMonitor.kt:26`, `OverlayViewModel.kt:56`, plan `:592 or`) | **CRITICAL** | §0.9 single source `caliper` DataStore `hudMedium/Scale/Opacity/Blur/Locked/Modules/ShowCoreBank/X/Y/fpsMode/hudMigrated` + one-shot migration in `SystemStatsApplication.onCreate` + `HudSettingsCache` for `FpsMonitor` sync read + grep `overlay_prefs`=0 acceptance; old-overlay pinned § + 3.3 `or` removed |
| Single-writer bus contradicts BUDGET cold sampler (`DashboardRepositoryImpl.kt:61/242`, `BenchModel.kt:256`) | **CRITICAL** | §0.1 invariant reword + §0.7 BUDGET semantics (sampler → direct `update()`, never `MonitorBus`; lossy field table; `MemInfoParser` reuse) + Phase 0 acceptance grep `BenchSnapshotCache`=0 + no `MonitorBus.push*` in BUDGET |
| Memory composition parser math under-specified fractions may exceed 1 (`BenchModel.kt:195-207`, `MemoryProvider.kt:12`, `DashboardRepositoryImpl.kt:256`) | **CRITICAL** | §0.4 pure `MemInfoParser.parse` with exact formula `activeF/Cached/zramF/swapF=max(0,swapRaw-zramF)/freeF=1-sum`, MemTotal denominator, fallback, normalization, order, verification harness |
| Fast 10 Hz ticker threading/cost/backoff not pinned (`FpsMonitor.kt:67`, `OverlayService.kt:548`) | **MAJOR** | §0.2 `FpsTicker` on `Dispatchers.IO` owned by `OverlayService` lifecycle, adaptive `100→1000ms` after 5× `—`, layer cache 30s, honest `—` |
| WorkManager enqueue/cancel race across 5 receivers + policy (`SystemStatsApplication.kt:12`, `BenchGlance.kt:627`) | **MAJOR** | §0.7 + §0.9 `BenchBudget` helper `enqueue`/`cancelIfNone` counting all 5 kinds atomically, `KEEP` rationale, `onEnabled`/`onDisabled` per receiver, Phase 0 files table |
| Top consumers source contradicts usage-stats restriction (`BenchGlance.kt:614`, `TaskRepository` not named) | **MAJOR** | §0.9 `TopConsumersProvider` (`data/monitor/TopConsumersProvider.kt`) + `TaskRepository.kt:6` / `TaskRepositoryImpl.kt:22-38,40-94` evidence, `loadTopConsumers(max=5)` permission-gated, empty-hide vs label-only rows |
| WidgetsSheet/Settings refresh lifecycle incomplete — pin callback vs NavHost (`SettingsScreen.kt:38`, `WidgetsSheet.kt:35,152`) | **MAJOR** | §2.3 pinned primary `delay(1200)+refreshInstruments` + `ON_RESUME` observer via `LocalLifecycleOwner`; no `PendingIntent.getActivity(SettingsActivity)` trampoline when in NavHost |
| Hairline frame Glance implementation under-specified for T1 (`BenchGlance.kt:154`) | **MAJOR** | §1.2 exact verified Glance `Box{Column(pad12){1dp top; Row(defaultWeight){1dp left; Column(defaultWeight,pad9){content};1dp right};1dp bottom}}` with `defaultWeight` existence proof (`glance-1.1.0.aar` core `RowScope`/`ColumnScope`), T1 PAPER screenshot QA |
| HUD medium enum vs widget Medium follow-system confusion (`CaliperTheme.kt:30`) | **MAJOR** | §3.1 distinct `HudMedium {PAPER,CARBON,BLUEPRINT}` + mapping fns, no follow flag, §0.9 CaliperKeys explicit, §3.3 note |
| Glance `providePreview` handling risks compile break (`app/build.gradle.kts:136`) | **MINOR** | §Research verified absent in Glance 1.1.0 via AAR `javap`+`grep` across 477 classes, §0.9 + §1.10 + Verification + Risks table corrected (no `@RequiresApi`, previewImage only) |
| BenchFrames recycle + onDeleted bug not fully fixed (`BenchModel.kt:382`, `BenchGlance.kt:629`) | **MINOR** | §1.3 remove `recycle()` + §1.4 per-id `onDeleted` for all 5 receivers + §0.9 per-id `lastPush` eviction, not global `clear()` |
| Receiver FQN immutability test + WorkManager F-Droid note (`AndroidManifest.xml:79`, `app/build.gradle.kts:136`) | **SUGGESTION** | §0.9 `WidgetReceiversExistTest` (Class.forName 5 FQNs) + §Verification `assembleRelease` R8 keep note (`isMinifyEnabled=true :67`) |
| Settings INSTRUMENTS nested `verticalScroll` crash (`SettingsScreen.kt:69`, `WidgetsSheet.kt:58`) | **CRITICAL (PASS 3 F1)** | §2.0 one scroller per branch; outer Column not scrollable when sheet shown; Glance `getGlanceIds` is not the crash |
| Overlay START composed when `!canDrawOverlays` (`OverlayScreen.kt:152`, `OverlayService.kt:73`) | **MAJOR (PASS 3 F2)** | §3.3.a START only if permission; GRANT OVERLAY stays in PERMISSIONS; service `startForeground` then `stopSelf`; 400 ms re-check on resume |
| Overlay demo HUD full-bleed / overlapping rectangles (`CaliperHud.kt:46,87`, tester PNG) | **MAJOR (PASS 3 F3)** | §3.3.b wrap-to-scale 196/260/300, `spacedBy(6.dp)`, no `height(16.dp)`, no `Modifier.blur` on preview, no `StampBadge` on HUD, modules follow DIPs |
| Widget layouts vs DI-WD-001 (SCOPE stacked not T2 split; BENCH `chunked(3)`; unused `BenchPanel` desc; Glance no Monospace; config two Texts) | **MAJOR (PASS 4 W1–W10)** | §PASS 4 audit + §1.6/1.7. WD visual wins over WI stacked sketch. Do not recreate Phase 0 / hairline / 15 previews |
| Overlay window vs DI-HD-001 (`HudPanel` missing; Views HUD; OverlayScreen still scale fader) | **CRITICAL (PASS 4)** | §Plan C + §3.1–3.3. `CaliperHud` is not the Scope Probe. Create HudAtoms/Modules/Panel; rewrite OverlayService; F2/F3 first OverlayScreen commit |

Evidence backing each pin is cited inline as `file:line` or bash command output (`/tmp/opencode/glance-inspect` AAR extraction 2026-08-22). Worker must not guess remaining choices.
