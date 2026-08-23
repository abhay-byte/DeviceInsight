# PLAN — Field QA (widgets / overlay / icons / device / overview)

Doc ID: DI-PLAN-003 · PASS 1 · Status: READY — 2026-08-23. Research + planning only this pass — **no product code**. Successor to DI-PLAN-002 (`docs/plans/PLAN_widgets_overlay.md`). Lands leftover PASS 5 MAJOR/MINOR from `docs/reviews/PLAN_widgets_overlay-impl-review-PASS5.md` first, then the 7 device-filed field bugs.

**This is not a greenfield build.** Phase 0 bus/parser/WM/previews/hairline/receivers, W1–W7/W9 layouts, F1 scroll split, F2 START `when` gate, OverlayService `WRAP_CONTENT` + startForeground-before-stopSelf, and the HudPanel rewrite are **already correct**. Do not recreate them. Do not rewrite HudAtoms/Modules/Panel/Demo. Do not regenerate the 15 WEBPs.

**Source-of-truth (still bound):** `docs/design/widgets.md` (DI-WD-001) visual · `docs/design/overlay_redesign.md` (DI-HD-001) probe · `docs/design/CALIPER.md` S-01..S-06 channel pages · this plan for Glance 1.1.0 + platform contracts. WI sketches stay uncompilable — do not copy `widget.bench`.

---

## Field-bug table

| # | Surface | Symptom (device) | Root cause (verified in tree) | Phase |
|---|---|---|---|---|
| **F-review** | PASS 5 leftover | Reviewer REVISE: 0 CRITICAL / **6 MAJOR** / 3 MINOR | W8 BENCH footers omit Monospace (`BenchGlance.kt:691,693,731,733`). W10 `setContent` not gated on `mediumFlow.first()`; `CaliperTheme` ignores MEDIA SegKey; snap uses stale `GlobalSnapshot` / `BenchBudgetSnapshot.last` (`BenchConfigActivity.kt:48-55,110,119`). F2 400 ms re-check is `LaunchedEffect(hasOverlay)` not ON_RESUME (`OverlayScreen.kt:44-56`). `isServiceRunning` never reads `OverlayService.isRunning` (`OverlayViewModel.kt:77,165-189`). No lock DIP (`OverlayScreen.kt:131`; `setLocked` dead). HudPanel missing `clipToBounds` / `spacedBy(6.dp)` / `heightIn(min=22.dp)` (`HudPanel.kt:61,67`). | **0** |
| **F-W** | Home widget vs Calibrate | Home = Carbon SCOPE, OEM-rounded, tap does nothing useful, frozen. Calibrate = PAPER `ScopeTrace` with FOLLOW SYSTEM DIP still ON after PAPER pick. | **Theme:** `BenchConfig.followSystem` default `true` (`BenchModel.kt:43`); `resolvedMedium` (`:454-459`) ignores `cfg.medium` while follow is on. Config SegKey writes local `medium` only; DIP stays on (`BenchConfigActivity.kt:111,133-135`). Preview uses Compose `CaliperTheme` from a **different** medium than Glance. **Preview anatomy:** `PreviewPanel` is `OdometerText`+`ScopeTrace` (`:181-194`); home is Glance `Hero`+`BandBitmap` (`BenchGlance.kt:341-411`) — never pixel-identical. **Tap:** `Header(..., onClick)` **never applied** (`:209-229` Row is not `.clickable`). Footer `.clickable(open("overview"))` (`:241`) → Overview, not config. `setResult(RESULT_CANCELED)` **without** `EXTRA_APPWIDGET_ID` (`BenchConfigActivity.kt:46`). **Frozen:** `DashboardRepositoryImpl` `@Singleton` 2 Hz + `BenchUpdater.nudge` (`BenchGlance.kt:100-136`); empty `catch (_: Exception) {}` at `:133`; `cachedIds` can store empty lists for 30 s (`:109-118`); STACK hatch / FUEL gauge BandBitmap keys omit `snap.timestamp` (`:442`, `:540`). Default cadence AMBIENT 30 s. Process dead → only `BenchBudgetWorker` 15 min. | **1** |
| **F-icon** | Launcher icon | App icon ≠ masthead crosshair; no real monochrome; in-app Paper/Carbon/Blueprint does not change launcher icon. | Masthead `CrosshairMark` (`CaliperChrome.kt:106-116`). QS tile already `ic_tile_caliper.xml`. Launcher `mipmap/ic_launcher` adaptive reuses colorful `ic_launcher_foreground` for **both** `<foreground>` and `<monochrome>` (`mipmap-anydpi-v26/ic_launcher.xml:3-5`). Overlay notification `R.mipmap.ic_launcher` (`OverlayService.kt:334`). Themed icons follow **wallpaper**, not DataStore — no platform API for in-app medium → system tint. | **4** |
| **F-sheet** | Settings → INSTRUMENTS | No back in header (BACK is after ADD×5); five fat cards, no previews; pin does nothing useful. | `WidgetsSheet.kt:62` header has no leading BACK; `:132` BACK after the list. `:68-85` five `PanelCard`s. `requestPin` (`:173-193`) `requestPinAppWidget(cn, null, null)` — null extras, null success callback. Context may be a `ContextWrapper`, not Activity. | **2** |
| **F-overlay-fps** | Overlay **page** (not only the window) | Probe looks small on a huge empty stage; S/M/L type barely grows; stutter / low FPS. | Host centers 196/260/300 on ~412 dp with `probeW+24` + 12 dp pad (`OverlayScreen.kt:220-234`). `HudScales` S/M/L hero 24/28/32 (`HudTheme.kt:97-102`) exist but `MemBar`/`FuelMicro` default 6/12 dp and **never receive `m.barHDp`** (`HudAtoms.kt:161,209`; `HudModules` callers). `HudPanel` reads `slow.value` **and** `fast.value` at root (`HudPanel.kt:45-46`) so 10 Hz FPS invalidates every band. Demo ticks **500 ms** both feeds (`HudDemo.kt:26-38`). `setHudOpacity` persists on every fader move (`OverlayViewModel.kt:114-116`). Also carry R0 F2 400 ms / isRunning / lock DIP. | **3** |
| **F-device-tabs** | Device dossier | Tab switch stutters; CPU/GPU leftover Material glass. | `when(tab)` inside one `verticalScroll` (`HardwareScreen.kt:147-152`) destroys the previous tree. `LaunchedEffect(tab) { scroll.animateScrollToItem(tab) }` (`:113-116`). `HardwareViewModel` emits a **new** `HardwareInfo` every 1 s (`HardwareViewModel.kt:29-35`). CpuTab/GpuTab still `MaterialTheme` + `Brush` gradients. | **5** |
| **F-logos** | CPU / GPU tabs | Vector SoC glyphs / Material `DeveloperBoard`; real PNG/JPG/WEBP sit unused in assets. | Assets on disk: `soc_{snapdragon,mediatek,tensor}.png`, `soc_exynos.jpg`, `gpu_{adreno.jpeg,arm_mali.jpg,powervr.jpg,xclipse.webp}`. `SocLogoRepository.logoUrlFor` already returns `file:///android_asset/…` (`SocLogoRepository.kt:21-28`) but CpuTab uses `painterResource(logoDrawableResFor)` → `R.drawable.ic_soc_*` (`CpuTab.kt:63-70`). Coil `AsyncImage` imported and unused. GpuTab `Icons.Filled.DeveloperBoard` (`GpuTab.kt:67-72`); `GpuLogoRepository.urlFor` unused. | **5** (same files) |
| **F-overview** | Overview tiles | Every tile shows `tap →` and does nothing. GPU spark is CPU hist. No channel pages. | `ReadoutTile` default `onClick = {}` still paints `tap →` (`CaliperData.kt:89-92,113`). `DashboardScreen.kt:46,61,72,84` pass `{}`. GPU block uses `m.cpuHistory` (`:92`). No `presentation/dashboard/channels/`. Widget `di_route` CH-01/02 → Dashboard (`SystemStatsApp.kt:90`); CH-03..06 → Hardware spec tabs (`:91-94`). CALIPER §5.3 / S-02..S-06 + `design_implementation.md:2234` `CpuScreen` are the template. | **6** |

**Already correct — worker MUST NOT re-do:** F1 scroll split · W1 T2 `Row` `defaultWeight` · W2 y-labels · W3 STACK `%` · W4 composition Subline · W5 FUEL watt+% · W6 RASTER name·vulkan · W7 `chunked(2)` · W9 semantics · F2 START `when` gate · OverlayService `WRAP_CONTENT` + startForeground-before-stopSelf · HudAtoms/Modules/Panel/Demo · 15 WEBPs · receiver FQNs · no `providePreview` · no `recycle` · `overlay_prefs` migration-only · `calibrate`/`hud-config` in **both** NavHosts.

---

## 0 · Research sources

### Local (authoritative)

- `docs/reviews/PLAN_widgets_overlay-impl-review-PASS5.md` — REVISE; 6 MAJOR / 3 MINOR. **Phase 0 of this plan.** <source: file>
- `docs/plans/PLAN_widgets_overlay.md` (DI-PLAN-002) — historical pins (WD vs WI, Glance 1.1, no providePreview, WRAP_CONTENT, no Modifier.blur, F1/F2/F3). <source: file>
- `docs/design/widgets.md` (DI-WD-001) §2 OEM clip / 12 dp inset · §3 **PAPER · CARBON · BLUEPRINT · FOLLOW SYSTEM** as four picker options · §7 states. <source: file>
- `docs/design/CALIPER.md` S-01 Overview tap → channel page · S-02..S-06 templates. <source: file>
- `docs/design/design_implementation.md:2234` `CpuScreen` sketch (hero GHz, ScopeTrace, CoreRail, ThermalGauge). <source: file>
- `docs/design/overlay_redesign.md` (DI-HD-001) — wrap-to-scale, FPS-only recomposition. <source: file>
- Tree: `BenchGlance.kt`, `BenchModel.kt`, `BenchConfigActivity.kt`, `WidgetsSheet.kt`, `OverlayScreen.kt`, `OverlayViewModel.kt`, `HudPanel.kt`, `HudModules.kt`, `HudDemo.kt`, `HudTheme.kt`, `HardwareScreen.kt`, `CpuTab.kt`, `GpuTab.kt`, `DashboardScreen.kt`, `SystemStatsApp.kt`, `AndroidManifest.xml`, `ic_tile_caliper.xml`, 8 logo assets. <source: file>
- Compose BOM `2024.12.01`, Glance `1.1.0`, Coil `2.7.0`, minSdk 26, target 36. `androidx.compose.foundation.pager.HorizontalPager` is on the BOM — **no extra dep**. <source: `app/build.gradle.kts`>
- Context7 `npx ctx7@latest` was **blocked** (untrusted remote). Official webfetch used instead. Do not pretend Context7 succeeded.

### Official / platform (webfetch 2026-08-23)

| Source | Pin |
|---|---|
| [Glance configuration](https://developer.android.com/develop/ui/compose/glance/configuration) (updated **2026-08-06**) | Config activity must always return a result **including** `EXTRA_APPWIDGET_ID`. Initial `setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_APPWIDGET_ID, id))`. Host does **not** send `APPWIDGET_UPDATE` on first bind — activity must `GlanceAppWidget.update`. `widgetFeatures=reconfigurable` = long-press **Reconfigure** (API 31+). `configuration_optional` skips first config — **do not add it** (ADD must open Calibrate). |
| [Manage and update GlanceAppWidget](https://developer.android.com/develop/ui/compose/glance/glance-app-widget) (updated **2026-08-20**) | Widgets live in **another process**. In-memory state dies with the app. Must call `GlanceAppWidget.update(context, glanceId)` to rebuild RemoteViews. App may update whenever it is running. `updatePeriodMillis` is host-side and floored (~30 min) — XML already `0`. Do not update every minute when the process is dead (Play / battery). |
| [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive) (updated **2026-08-13**) | Foreground + background **108×108 dp**; logo in **66×66 safe zone**. `<monochrome>` = **single-color** (black on transparent) for Android 13 themed icons. Tint comes from **user wallpaper**, not the app. Reusing the colorful foreground as monochrome is wrong. |
| `AppWidgetManager.requestPinAppWidget(componentName, extras, successCallback)` + [SO 74953434](https://stackoverflow.com/questions/74953434) | extras: `EXTRA_APPWIDGET_PREVIEW` bitmap. successCallback: `PendingIntent.getBroadcast` **or** `getActivity` (some OEMs drop broadcast). Unwrap Activity from `ContextWrapper`. |
| [Compose side-effects / state reads](https://developer.android.com/develop/ui/compose/side-effects) | A composable invalidates when it **reads** a changing `State`. `HudPanel` root must not read 10 Hz `fast`. Isolate readers. Debounce DataStore writes. |
| Nested scroll F1 | Already fixed in DI-PLAN-002 Phase 2.0. **Do not reopen.** |
| Play policy | No exact `AlarmManager` for 1 s widget ticks. No second FGS “just for widgets”. BUDGET 15 min WorkManager is the dead-process floor. |

---

## 1 · Exists vs missing

| Surface | Path | This effort |
|---|---|---|
| Glance family T1–T5, 4-side hairline, palettes, cadence ladder, 15 WEBPs, receivers | `ui/caliper/widget/BenchGlance.kt` + XML | **Keep.** Fix clickable, theme, Monospace footers, BandBitmap keys, nudge logging. |
| Config activity | `BenchConfigActivity.kt` | W10 gate + 5 s snap + 4-way MEDIA SegKey + Glance-anatomy preview + `RESULT_CANCELED` extras. |
| `resolvedMedium` / `BenchState.save` `KEY_CADENCE` | `BenchModel.kt:412-460` | Persist followSystem from 4-way picker. Cadence already written. |
| Instruments sheet | `WidgetsSheet.kt` | Restructure UX + pin extras/callback. F1 scroller stays the only one. |
| Overlay sheet + VM | `OverlayScreen.kt`, `OverlayViewModel.kt` | R0 F2/lock + compact spacing + isolate preview + debounce opacity. |
| HudPanel / scales / demo | `ui/caliper/hud/*` | Split fast/slow reads; `clipToBounds`/`spacedBy`/`heightIn`; pass `barHDp`. Do not rewrite atoms. |
| Overlay service | `OverlayService.kt` | Notification icon only. WRAP_CONTENT / 10 Hz IO ticker **stay**. |
| Launcher icons | `mipmap-anydpi-v26/ic_launcher*.xml`, `ic_tile_caliper.xml` | Replace foreground + dedicated monochrome + activity-alias triple. |
| Device dossier | `presentation/hardware/HardwareScreen.kt`, `components/CpuTab.kt`, `GpuTab.kt` | Pager + load-once + Coil assets + strip Material leftover. |
| Overview | `DashboardScreen.kt`, `CaliperData.kt` | Wire `onClick` → new channel routes. GPU uses `gpuHistory`. |
| Channel pages | — | **Missing.** Create `presentation/dashboard/channels/`. |
| Logo URLs | `data/mapper/{Soc,Gpu}LogoRepository.kt` | **Exist.** CpuTab/GpuTab must call them. |
| Coil | `io.coil-kt:coil-compose:2.7.0` | Use `AsyncImage`. `file:///android_asset/` is Coil 2 `AssetUriFetcher`. |

**Do not create:** MonitorBus, MemInfoParser, TopConsumers, HudSettingsCache, BenchBudgetWorker, 15 WEBPs, HudAtoms/Modules/Demo (exist), `widget.bench` package, widget-only FGS, `providePreview`, overlay_prefs store.

---

## 2 · Phases

### Phase 0 — PASS 5 leftover (land first)

**Do not rewrite already-correct W1–W7/W9/F1/F2-when/F3-host.** Phase 0 keeps the **3-way MEDIA SegKey + follow DIP** exactly as the review wrote; the 4-way picker is Phase 1. Do not implement FOLLOW-as-fourth-option here.

| # | File:line | Change |
|---|---|---|
| W8 | `BenchGlance.kt:691,693,731,733` | Add `fontFamily = FontFamily.Monospace` on the four BENCH footer `TextStyle`s. No IBM Plex. |
| W10 theme | `BenchConfigActivity.kt:48-55,110` | `lifecycleScope.launch { val m = runCatching { mediumFlow.first() }.getOrNull() ?: Medium.PAPER; setContent { … } }`. **No** `runBlocking`. **No** PAPER-first `setContent`. Inner `var medium by remember { mutableStateOf(initial) }`; wrap screen in `CaliperTheme(medium = medium)` so the 3-way SegKey drives the preview. (Phase 1 replaces DIP with 4-way and uses resolved preview medium.) |
| W10 snap | `:119` | `val live = GlobalSnapshot.current(); val snap = if (live != null && now - live.timestamp in 0 until 5_000L) live else benchDemoSnapshot(kind)`. **Drop** `BenchBudgetSnapshot.last` for config preview. |
| F2 resume | `OverlayScreen.kt:44-56` | `val scope = rememberCoroutineScope()`. Inside existing ON_RESUME observer: `refreshPermissions()` **and** `scope.launch { delay(400); viewModel.refreshPermissions() }`. **Delete** `LaunchedEffect(state.permissions.hasOverlay)`. Do **not** auto-start after grant. |
| F2 running | `OverlayViewModel.kt:77,96-99,180-188` | In `loadInitialState` **and** `checkPermissions`, copy `isServiceRunning = OverlayService.isRunning.get()`. |
| Lock DIP | `OverlayScreen.kt` after blur DipSwitch (~`:131`) | `DipSwitch(checked = state.config.locked, onCheckedChange = { viewModel.setLocked(it) }, label = "lock (touch passthrough)")`. |
| F3 internals | `HudPanel.kt:61,67` + HudModules band roots | Root Box: `.clipToBounds()` after `background`. Column: `spacedBy(6.dp)`, drop ad-hoc `Spacer(padDp/2)` pairs around hairlines (keep one `HairlineH()`). Band rows: `fillMaxWidth().wrapContentHeight().heightIn(min = 22.dp)`. **No** `height(16.dp)`. **No** `Modifier.blur`. |

**MINOR (same files if touched):** T2+ `Spacer(8.dp)` between bands in SCOPE/STACK/FUEL/RASTER (`tier != T1`); keep T1 at 4 dp. Strip review-history comments (`WidgetsSheet.kt:37`, `SettingsScreen.kt` “plan §2.0”, `BenchGlance` `// W1`/`// W3`/`// W5`/`// W7` prefixes).

**Phase 0 files to modify:** `BenchGlance.kt`, `BenchConfigActivity.kt`, `OverlayScreen.kt`, `OverlayViewModel.kt`, `HudPanel.kt`, `HudModules.kt` (band row modifiers), `WidgetsSheet.kt` (comment only), `SettingsScreen.kt` (comment only).

**Do not create.**

---

### Phase 1 — Widget field (theme / tap→config / preview bands / nudge)

#### 1a Theme — FOLLOW SYSTEM is a fourth picker option (WD §3)

Selecting PAPER / CARBON / BLUEPRINT **unchecks followSystem** (explicit media wins). Follow-system is **not** a DIP overlay on three.

Load **saved** widget prefs before `setContent` (today the screen always starts PAPER + follow=true and never reads `BenchState.config` — that is why Calibrate lies after a previous SAVE):

```kotlin
lifecycleScope.launch {
    val systemM = runCatching { mediumFlow.first() }.getOrNull() ?: Medium.PAPER
    val glanceId = runCatching { GlanceAppWidgetManager(this@…).getGlanceIdBy(appWidgetId) }.getOrNull()
    val saved = glanceId?.let { runCatching { BenchState.config(this@…, it) }.getOrNull() }
    setContent { BenchConfigRoot(kind, systemMedium = systemM, initial = saved ?: BenchConfig(), …) }
}
```

```kotlin
// BenchConfigScreen — replace 3-way SegKey + DipSwitch
enum class MediaPick { PAPER, CARBON, BLUEPRINT, FOLLOW }
var pick by remember { mutableStateOf(if (initialFollow) MediaPick.FOLLOW else MediaPick.valueOf(initialMedium.name)) }
val followSystem = pick == MediaPick.FOLLOW
val medium = when (pick) {
    MediaPick.FOLLOW -> systemMedium   // mediumFlow / night→CARBON
    else -> Medium.valueOf(pick.name)
}
SegKey(options = MediaPick.entries, selected = pick, onSelect = { pick = it },
    labelFor = { if (it == MediaPick.FOLLOW) "FOLLOW" else it.name })  // SegKey already uppercases
```

- Persist via existing `BenchState.save` (`KEY_FOLLOW` + `KEY_MEDIUM`) — already writes both (`BenchModel.kt:419-421`).
- `resolvedMedium` stays: follow **only** when `followSystem==true`. Do not change that function’s logic beyond keeping it honest.
- Config preview theme = **same** medium Glance will use: `if (followSystem) systemMedium else selected`.
- Data-class default `followSystem=true` may stay (new placements = FOLLOW SYSTEM). Do not force PAPER.

#### 1b Preview anatomy = Glance T2 bands, not in-app ScopeTrace

Glance has no Compose `ScopeTrace`. Honest match, not pixel-identical.

- Rewrite `PreviewPanel` for SCOPE / STACK / FUEL / RASTER / BENCH as a **Compose facsimile of the Glance T2 tree**: header (ch · name · LIVE), hero, freq/temp or equivalent sublines, Canvas spark / hatch / fuel / gpu spark (polyline from hist — **not** `ScopeTrace`), footer `upd`. Caption under the card: `home screen · glance`.
- Do **not** embed Glance composables in the Activity.
- Launcher **will clip 0 dp-radius panels to the OEM mask**. WD §2 already: “OEM launchers may clip corners — 12 dp inset keeps hairlines clear”. `res/drawable/rounded_widget_background.xml` exists — **do not re-apply to Glance**. Document as known launcher clip, not a code bug.

#### 1c Tap widget → Calibrate (official config)

```kotlin
private val APPWIDGET_ID = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID) // "appWidgetId"

private fun openConfig(appWidgetId: Int) =
    actionStartActivity<BenchConfigActivity>(parameters = actionParametersOf(APPWIDGET_ID to appWidgetId))

// provideGlance:
val awId = GlanceAppWidgetManager(context).getAppWidgetId(id)

// BenchPanel root Box — chain clickable. Header onClick is currently unused; pass openConfig or drop the param.
Box(GlanceModifier.fillMaxSize().background(...).semantics { contentDescription = desc }
    .clickable(openConfig(awId)))
```

- Keep `widgetFeatures=reconfigurable` (already on all 5 XML). Long-press Reconfigure stays.
- Manifest `ACTION_APPWIDGET_CONFIGURE` on `BenchConfigActivity` stays (`AndroidManifest.xml:41-47`).
- `onCreate`: `setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_APPWIDGET_ID, appWidgetId))` — **with extras**. Invalid-id early-return too.
- SAVE already puts extra + `widget.update` (`:94-97`) — keep.
- Do **not** route the whole widget to CH-01 Hardware / Overview. Footer: drop `.clickable(open("overview"))` so the root config tap wins. BENCH T3+ per-tile `open(chId)` stays as in-app channel later (Phase 6); root still opens config if the tap is on masthead/gutter. Pin: **masthead + unused chrome → config**; do not send users to Hardware spec tabs.
- Skip `configuration_optional`.

#### 1d Updates (Play-legal, honest)

- Default cadence stays **AMBIENT (30 s)**. LIVE 1 s only while charging **or** HUD running (`cadenceMs` already — `BenchModel.kt:448-452`). Do not add a widget FGS. Do not exact-alarm.
- `BenchState.save` already writes `KEY_CADENCE` — verify config CADENCE SegKey still flows into `cfg` on SAVE (it does via `BenchConfig(...)`).
- After SAVE, `widget.update` stays.
- `BenchUpdater.nudge` (`BenchGlance.kt:133`): replace empty catch with `Log.w("BenchUpdater", "update failed id=$id", e)`.
- **Do not cache empty id maps:** if `getGlanceIds` throws, keep previous `cachedIds` for that class (or skip the write). Empty-cache + 30 s `lastIdFetch` is a freeze.
- BandBitmap keys **must include `snap.timestamp`**: STACK hatch `:442`, FUEL gauge `:540`, BENCH rail `:724`. SCOPE / STACK spark / FUEL watt / RASTER hist already do.
- Process **dead**: 15 min BUDGET is the floor. SIGNAL LOST after 2× cadence stays the honest UI (`BenchSnapshot.stale`).

**Phase 1 files to modify:** `BenchModel.kt` (only if picker helper needs a type — prefer local enum in Activity), `BenchConfigActivity.kt`, `BenchGlance.kt`.  
**Do not create.** Do not touch XML `previewImage` / `updatePeriodMillis`.

---

### Phase 2 — Instruments sheet UX + pin

**UI (CALIPER, simple). Do not change `ScreenHeader`’s global signature.**

`WidgetsSheet` Column, top to bottom:

1. `HardKey("← BACK", SECONDARY, onClick = onBack)` **immediately** — not after the list. Keep `EndOfSheet` at bottom; drop the duplicate bottom BACK **or** keep a quiet EndOfSheet only.
2. `ScreenHeader("№ 05.1 — INSTRUMENTS", …)` as today.
3. Compact **instrument strip**: 5 mini tiles (SCOPE STACK FUEL RASTER BENCH). Each: kind name + `NOT PLACED` / `×N` + 72 dp `Image(painterResource(preview_*_paper))`. **Glance composables are not interoperable with Compose UI** — use the existing WEBPs (`preview_scope_paper`, `preview_stack_paper`, `preview_fuel_paper`, `preview_raster_paper`, `preview_bench_paper`). Selected tile is a `SegKey` **or** equally-weighted clickable cells.
4. Below: selected kind’s one-line personality + `ADD TO HOME SCREEN` HardKey + ACTIVE list **for that kind** (or all, but shorter rows: kind · medium · cadence · `upd` + CALIBRATE).
5. Drop the five fat `PanelCard`s. Collapse MANUAL PATH into one `MarginNote` (01 long-press home · 02 Widgets · 03 DeviceInsight · 04 pick kind). Keep `isRequestPinAppWidgetSupported==false` MarginNote.

**Pin-to-home (official + SO 74953434):**

```kotlin
fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

val activity = ctx.findActivity()
if (activity == null) { /* MarginNote: cannot pin from this context */; return }
val preview = BitmapFactory.decodeResource(ctx.resources, previewResFor(kind)) // preview_*_paper
val extras = Bundle().apply { putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview) }
val success = PendingIntent.getBroadcast(
    ctx, kind.ordinal,
    Intent(ctx, PinSuccessReceiver::class.java),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)
mgr.requestPinAppWidget(cn, extras, success)
```

- Prefer `getBroadcast` to a tiny `PinSuccessReceiver` that logs + LocalBroadcast / (or no-op). **delay(1200) + ON_RESUME stays primary refresh** (already). Do **not** trampoline to `SettingsActivity` (NavHost host).
- SO 74953434: some OEMs only fire `getActivity`. If broadcast is silent, delay+resume still works. Do not `getActivity(BenchConfigActivity)` as the success callback — the launcher already launches configure because XML has `android:configure` and **not** `configuration_optional`.
- After successful pin, configure opens Calibrate. If OEM skips configure, widget places with defaults (SKIP contract) — keep.

**Phase 2 files to modify:** `WidgetsSheet.kt`.  
**Create:** `ui/caliper/widget/PinSuccessReceiver.kt` (log + no-op) + manifest `<receiver android:exported="false">`.  
**Do not create** a new Settings route. F1 single-scroller stays.

---

### Phase 3 — Overlay sheet scale + FPS

Carry Phase 0 F2 400 ms, `isRunning`, lock DIP if not already landed.

**Scale (keep wrap-to-scale, grow type, reduce chrome):**

- Probe widths **stay** 196/260/300. Do not `MATCH_PARENT` the probe (F3 still in force).
- Host: vertical padding 12→**6**; stage width = `probeW` (**no +24**) (`OverlayScreen.kt:227-231`).
- **Every** HudModules / HudAtoms bar/text size from `LocalHudMetrics.current`. Pass `height = m.barHDp.dp` into `MemBar` and `FuelMicro` (they default 6/12 today and **ignore** S/M/L). Header row: drop hard `height(24.dp)` in favor of `heightIn(min=22.dp)` + metrics. Audit `HudModules.kt` + `HudAtoms.kt` for leftover `13.sp` / fixed dp; grep `hudStyle(13` / `.height(16` / `.height(24`.
- Compact the sheet: DipSwitch list `spacedBy(4.dp)` not 8+12; combine STYLE & MODULES density (one card or tighter spacers). Preview stays top.
- No `Modifier.blur` on preview. Window blur = `FLAG_BLUR_BEHIND` only.

**FPS:**

```kotlin
// HudPanel — root MUST NOT read slow.value or fast.value. Preserve modules.sorted() order.
@Composable
fun HudPanel(..., slow: State<HudSlow>, fast: State<HudFast>, ...) {
    val c = HudPalettes.of(config.medium)
    val m = HudScales.of(config.scale)
    Box(modifier.width(m.widthDp.dp).wrapContentHeight().background(...).clipToBounds().hudFrame(...)) {
        Column(Modifier.padding(m.padDp.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HudHeaderGate(slow, fast, config, onLock) // header clock reads slow; LED child reads fast.isNoSignal only
            config.modules.sorted().forEachIndexed { i, module ->
                if (i > 0) HairlineH()
                when (module) {
                    HudModule.FPS -> HudFpsBand(fast)                 // ONLY full fast reader
                    HudModule.CPU -> HudCpuGate(slow, config.showCoreBank)
                    HudModule.MEMORY -> HudMemoryGate(slow)
                    HudModule.POWER -> HudPowerGate(slow)
                    HudModule.GPU -> HudGpuGate(slow)
                    HudModule.NETWORK -> HudNetGate(slow)
                    HudModule.TRACE -> {}
                }
            }
        }
    }
}
@Composable private fun HudCpuGate(slow: State<HudSlow>, showBank: Boolean) { HudCpuBand(slow.value, showBank) }
@Composable private fun HudFpsBand(fast: State<HudFast>) { val f = fast.value; /* existing HM-1 */ }
```

Do **not** pull FPS out of `modules` order. `Hud*Gate` wrappers exist so 10 Hz `fast` cannot invalidate CPU/MEM/PWR/GPU/NET. Header LED may read `fast.isNoSignal()` in a 1-line child; the header clock stays on `slow`.

- `rememberHudDemo(animate)` delay **1000 ms** (was 500). Isolate so demo ticks cannot recompose PERMISSIONS / STYLE: keep `rememberHudDemo()` **inside** `HudPreviewHost` (already) and **do not** collect `hudFast` in `OverlayScreen` itself. `movableContentOf` not needed.
- OverlayService 10 Hz ticker stays on IO (`OverlayService.kt:241-250`); `withContext(Main)` only if you must set a Compose `MutableState` on the window. Service already `monitorBus.pushFast` from IO + collects into `fastState` — keep. OverlayScreen must **not** collect 10 Hz unless `isServiceRunning` (already gated inside host).
- Debounce opacity persist **150 ms**:

```kotlin
private var opacityJob: Job? = null
fun setHudOpacity(opacity: Float) {
    _uiState.value = _uiState.value.copy(config = _uiState.value.config.copy(opacity = opacity.coerceIn(0.4f, 0.9f)))
    opacityJob?.cancel()
    opacityJob = viewModelScope.launch(Dispatchers.IO) {
        delay(150); runCatching { context.setHudOpacity(opacity) }
    }
}
```

UI state updates immediately; DataStore does not storm.

**Phase 3 files to modify:** `OverlayScreen.kt`, `OverlayViewModel.kt`, `HudPanel.kt`, `HudModules.kt`, `HudAtoms.kt` (`MemBar`/`FuelMicro` call sites), `HudDemo.kt`.  
**Do not create.** Do not rewrite palettes.

---

### Phase 4 — App icon crosshair + monochrome + activity-alias media

**Adaptive (official 108 / 66):**

| Layer | File | Pin |
|---|---|---|
| Foreground | `drawable/ic_launcher_foreground.xml` | Adapt `ic_tile_caliper.xml` (circle + crosshair + 2 dp center). Ink on **transparent**. Viewport 108; wrap with `inset` 21 dp so the mark sits in the 66 dp safe zone. |
| Background default | `color/ic_launcher_background` | Paper `#F4F1E8` (replace current `#0B0B12`). |
| Monochrome | `drawable/ic_launcher_monochrome.xml` | **Black `#000000` strokes only**, no fill background, same crosshair. **Do not** reuse the colored foreground. |
| Adaptive XML | `mipmap-anydpi-v26/ic_launcher.xml` **and** `_round.xml` | `<foreground android:drawable="@drawable/ic_launcher_foreground"/>` `<monochrome android:drawable="@drawable/ic_launcher_monochrome"/>`. |

**In-app theme → launcher icon (activity-alias, three enabled-one-at-a-time):**

| Alias | Background | Enabled at ship |
|---|---|---|
| `.MainActivityPaper` | `#F4F1E8` | **true** (default) |
| `.MainActivityCarbon` | `#141310` | false |
| `.MainActivityBlueprint` | `#0C2338` | false |

Same crosshair foreground on each. Additional adaptive XMLs: `mipmap-anydpi-v26/ic_launcher_{paper,carbon,blueprint}.xml` (or color-only background drawables).

```xml
<!-- AndroidManifest: strip LAUNCHER from MainActivity itself (keep the Activity). Aliases own MAIN+LAUNCHER. -->
<activity-alias
    android:name=".MainActivityPaper"
    android:enabled="true"
    android:icon="@mipmap/ic_launcher_paper"
    android:roundIcon="@mipmap/ic_launcher_paper"
    android:targetActivity=".MainActivity">
    <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
    </intent-filter>
</activity-alias>
<!-- Carbon / Blueprint: android:enabled="false" -->
```

On Settings media change (`SettingsViewModel.setMedium`):

```kotlin
fun applyLauncherAlias(ctx: Context, medium: Medium) {
    val pm = ctx.packageManager
    val target = when (medium) {
        Medium.PAPER -> ".MainActivityPaper"
        Medium.CARBON -> ".MainActivityCarbon"
        Medium.BLUEPRINT -> ".MainActivityBlueprint"
    }
    val all = listOf(".MainActivityPaper", ".MainActivityCarbon", ".MainActivityBlueprint")
    // Enable the new alias FIRST, then disable the others — never zero LAUNCHERs.
    pm.setComponentEnabledSetting(ComponentName(ctx, ctx.packageName + target),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
    all.filter { it != target }.forEach {
        pm.setComponentEnabledSetting(ComponentName(ctx, ctx.packageName + it),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}
```

- Default alias = Paper. At process start, if DataStore medium is already Carbon/Blueprint, `SystemStatsApplication` (or first Settings/Main composition) may sync aliases — do not leave the user on a disabled Paper alias after a theme change.
- **Document:** OEM launchers cache icons; may need reboot / launcher restart; pinned shortcuts can break. Do not remove MAIN from a live alias without enabling another first.
- **Do NOT** try to tint the system themed-icon from DataStore — wallpaper-only (official).
- Overlay notification small icon: `OverlayService.kt:334` `R.mipmap.ic_launcher` → `R.drawable.ic_tile_caliper` (already vector). QS tile stays `ic_tile_caliper`.

**Phase 4 files to modify:** `AndroidManifest.xml`, `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `values/ic_launcher_background.xml`, `OverlayService.kt`, `SettingsViewModel.kt` (call alias helper), optionally `SystemStatsApplication.kt` (sync on start).  
**Create:** `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`, paper/carbon/blueprint adaptive XMLs + background colors, small `LauncherAlias.kt` helper (or a function in `CaliperPrefs.kt` — prefer `ui/caliper/LauncherAlias.kt`).  
**Do not** rasterize new mipmap PNGs if vectors + anydpi adaptive cover API 26+.

---

### Phase 5 — Device tabs pager + real logos + strip Material leftover

**Tabs (jank):**

- Narrow: `HorizontalPager(state, beyondViewportPageCount = 0, userScrollEnabled = false)` so tabs are the only switch. Tab strip `clickable` → `pagerState.scrollToPage(i)` (**not** `animateScrollToItem` on the LazyRow every change). LazyRow `itemsIndexed(tabs, key = { i, _ -> i })`. If swipe is added later, keep the strip in sync without animation loops — **this pass: swipe off**.
- Wide two-pane (≥560 dp): keep key list + one sheet; **no** pager.
- `HardwareViewModel`: spec sheets are static. **Delete the 1 s while-loop.** Load once in `init`; `loadHardwareInfo()` on `ON_RESUME` (HardwareScreen DisposableEffect). Do not emit a new object at 2 Hz.

**Logos (real assets, not SVG):**

- CpuTab (`presentation/hardware/components/CpuTab.kt`): if `logoRepo.logoUrlFor(info.socModel)` is non-null → `AsyncImage(model = url, …, error = painterResource(R.drawable.ic_soc_generic))`. If null (`UNKNOWN`) → `Image(painterResource(R.drawable.ic_soc_generic))`. Do not pass a null model.
- GpuTab (`presentation/hardware/components/GpuTab.kt`): `GpuLogoRepository().urlFor(gpu.openGlRenderer, gpu.openGlVendor)` the same way. Fallback generic / CH-06 tick. **Never** `Icons.Filled.DeveloperBoard`.
- Prefer `file:///android_asset/…` (already in the repositories). Copy into `res/drawable-nodpi/` **only if** Coil asset URI is flaky on QA. Do not redraw SVGs.
- Missing vendors (Intel Arc, Apple, Tegra): UNKNOWN fallback. PowerVR already has jpg. Optional Kirin/Unisoc → generic. **Do not invent fake logos.**

**Strip Material leftover** on CpuTab / GpuTab while touching them: no `MaterialTheme.colorScheme`, no `Brush` glass, no `RoundedCornerShape` fill. Wrap content in CALIPER `PanelCard` + existing `ScreenHeader` from HardwareScreen. `SpecRow` / `Text(Caliper.type.*)` only.

**Phase 5 files to modify:** `presentation/hardware/HardwareScreen.kt`, `HardwareViewModel.kt`, `presentation/hardware/components/CpuTab.kt`, `presentation/hardware/components/GpuTab.kt`.  
**Do not create** new logo files. Do not touch other tabs unless a compile break.

---

### Phase 6 — Overview channel pages + wire `tap →`

**Routes (both NavHosts in `SystemStatsApp.kt:200` and `:251`):**

`processor` · `memory` · `network` · `power` · `storage` · `gpu`

Do **not** add them to `railRoutes` / ModeRail. Back pops to Overview.

**Overview:**

- `DashboardScreen` takes `onChannel: (Channel) -> Unit` (or six lambdas). `SystemStatsApp` `navigate("processor")` etc.
- Every `ReadoutTile` gets a real `onClick`. `ReadoutTile` already paints `tap →` when `onClick != null` — keep that, just stop passing `{}`.
- GPU: convert the placeholder `PanelCard` to `ReadoutTile(Channels.GPU, …, spark = m.gpuHistory, onClick = …)`. **Not** `m.cpuHistory`.
- Add CH-05 STORAGE `ReadoutTile` (`storageUsedGb` / `storageTotalGb` / `storageUsedPerc` already on `DashboardMetrics`).
- Widget **header / root** tap (Phase 1c) goes to **config**, not these pages. In-app overview tap goes to channel pages.
- Remap widget **BENCH tile** `di_route` in **both** `LaunchedEffect(initialRoute)` maps (`SystemStatsApp.kt:89-98`): `CH-01→processor`, `CH-02→memory`, `CH-03→network`, `CH-04→power`, `CH-05→storage`, `CH-06→gpu`. `overview` still Dashboard. `processes` / `calibrate` / `hud-config` unchanged. Do **not** send CH-* to Hardware spec tabs.

**Channel page template** (CALIPER S-02..S-06 + `CpuScreen` sketch). Package: `presentation/dashboard/channels/`.

Shared `ChannelScaffold(sheetLabel, title, sub, onBack) { … }` = `Column(fillMaxSize.verticalScroll)` + `HardKey("← BACK")` + `ScreenHeader` + slot + `EndOfSheet`.

| Route | Title | Hero | Trace | Blocks (honesty) |
|---|---|---|---|---|
| `processor` | Processor. | `OdometerText` GHz (`BenchSnapshot.freqGHz` / Dashboard max) | `ScopeTrace(cpuHistory)` | `CoreRail` from `snap.cores` (`CoreReading` already), `ThermalGauge(cpuTemperature)`, governor SpecRow, cluster if present. |
| `memory` | Memory. | `6.81 / 12 GB` | mem hist spark | `HatchBar` from `snap.memComposition` (MonitorBus — **not** Hardware tab). Swap/zram gauges. Top consumers → `navigate(Tasks)` . |
| `network` | Network. | `↓ 18.1 ↑ 2.4` | dual spark | Down = existing `netHistory` (rx). **Add** `netUpHistory` LinkedList in `DashboardRepositoryImpl` (txBps) — do not fake an up curve from rx. If QA slips the second list, show live ↑ readout without a fake hist. |
| `power` | Power. | signed watts | `wattHist` / powerHistory | `LinearGauge(batteryPct)`, remaining, T4 health/cycles **only if non-null**. |
| `storage` | Storage. | used / total | — | used/total bar (`HatchBar` or `LinearGauge`). No fake per-directory map this pass. |
| `gpu` | Raster. | `% · MHz` | `gpuHistory` | `name · vulkan` Subline. `CHANNEL LOCKED` / `NOT FITTED` from `snap.gpuFitted` / `gpuRootLocked`. |

- Timebase SegKey: `30s` / `2m` **if** hist length supports it (61 samples ≈ 60 s at 1 s — default **60 s**; hide 2 m / 10 m / 1 h unless hist is actually that long). Do not fake longer windows.
- Data: collect existing `DashboardViewModel.uiState` + `MonitorBus.snapshot` (Hilt). **No second sampler.**
- Long-press label → glossary is **out of scope**.
- Do **not** reuse `HardwareScreen` tabs as the channel page (those are S-10 dossier plates and they stutter).

**Phase 6 files to modify:** `SystemStatsApp.kt` (both NavHosts + Dashboard lambdas), `DashboardScreen.kt`, `DashboardRepositoryImpl.kt` (optional `netUpHistory`), `DashboardMetrics.kt` (optional `netUpHistory`).  
**Create:** `presentation/dashboard/channels/ChannelScaffold.kt`, `ProcessorChannel.kt`, `MemoryChannel.kt`, `NetworkChannel.kt`, `PowerChannel.kt`, `StorageChannel.kt`, `GpuChannel.kt` (or one file with six composables if they stay small).  
**Do not create** a new ViewModel unless Hilt injection of MonitorBus into the screen is uglier — prefer `@Composable fun ProcessorChannel(vm: DashboardViewModel = hiltViewModel(), bus: MonitorBus = …)` via an `@HiltViewModel ChannelViewModel` that only exposes `snap + metrics` if needed. Pin: **one** `ChannelViewModel` sharing DashboardRepository + MonitorBus is OK; six VMs is not.

---

## 3 · Acceptance (device-testable, per phase)

**Phase 0**
- BENCH T2 and T3+ footers are monospace.
- Calibrate: SegKey PAPER/CARBON/BLUEPRINT actually paints that medium (after Phase 1 the fourth option exists; in Phase 0 three-way + `CaliperTheme(medium)` is enough).
- Fresh process, no live bus: preview is `benchDemoSnapshot`, not an empty BUDGET panel.
- Overlay: grant overlay on API 26–27 emulator → return → **within ~1 s START appears**. STOP shows if HUD already running after process recreation. Lock DIP unlocks a passthrough probe. No auto-start.
- HudPanel S: bands ≥22 dp, no 16 dp clip, no blur. Phase 0 still has 3-way MEDIA + follow DIP.

**Phase 1**
- Reopen Calibrate on a saved PAPER / follow=false widget: SegKey shows PAPER (not FOLLOW). Pick PAPER on a dark device → home widget is Paper. Pick FOLLOW → widget follows night.
- Calibrate preview bands match home (hero / spark / footer `upd`), caption `home screen · glance`. Rounded OEM clip is accepted.
- Tap widget → `BenchConfigActivity` for **that** id. Long-press Reconfigure still works on API 31+. CANCEL/back during first bind does not leave a ghost widget (extras on `RESULT_CANCELED`).
- Process alive, AMBIENT: `upd` advances ~every 30 s (logcat `BenchUpdater` on binder failure). Kill process: SIGNAL LOST until 15 min BUDGET, not a spinning fake.

**Phase 2**
- INSTRUMENTS: BACK is the first control. Strip shows 5 previews. One ADD key. Pin on Pixel shows system sheet + paper preview bitmap; Calibrate opens (no `configuration_optional`). Unsupported launcher: MarginNote, no crash.

**Phase 3**
- Overlay sheet: probe not full-bleed; S/M/L **type and bars grow**; STYLE fader does not freeze the sheet. Demo ~1 Hz. Live HUD 10 Hz **FPS band only** (other bands 2 Hz). Lock DIP present. START/STOP honest.

**Phase 4**
- Launcher icon is the crosshair. Themed-icons (Android 13+, wallpaper tint) use the black mono layer. Settings → CARBON updates the enabled alias (may need launcher restart — write that in a MarginNote on Settings media). HUD notification uses `ic_tile_caliper`. App still launches after alias swaps (never zero enabled LAUNCHERs).

**Phase 5**
- Device CPU/GPU tabs: raster logos from assets, no DeveloperBoard, no Brush glass. Tab switches without destroying-and-inflating jank; hardwareInfo does not refresh at 1 Hz. TalkBack tab roles stay.

**Phase 6**
- Overview `tap →` opens Processor/Memory/Network/Power/Storage/Gpu with live instruments and BACK to Overview. GPU spark is GPU hist. CHANNEL LOCKED / NOT FITTED honest. Widget tap still Calibrate.

`./gradlew :app:compileDebugKotlin` green after each phase.

---

## 4 · Risks + mitigations

| Risk | Mitigation |
|---|---|
| Glance `actionStartActivity` extras key mismatch | Key **name** must be `AppWidgetManager.EXTRA_APPWIDGET_ID` (`"appWidgetId"`). Device-test tap **and** first-bind. |
| Nested Glance `.clickable` (root vs BENCH tiles vs STACK consumers) | Root = config. Keep explicit child actions (processes, GRANT IN APP, BENCH tiles). Footer no longer steals to Overview. |
| OEM pin callback never fires | delay(1200)+ON_RESUME is the acceptance gate; extras preview is best-effort. |
| Activity-alias leaves user with no launcher icon | Enable-new-then-disable-old. Default Paper enabled in XML. Never `COMPONENT_ENABLED_STATE_DISABLED` on the last alias. |
| OEM icon cache | MarginNote on Settings media. Not a code retry loop. |
| Coil `file:///android_asset` flaky | Fallback painter; only then copy to `drawable-nodpi`. |
| HorizontalPager extra composition | `beyondViewportPageCount = 0`, swipe off, two-pane unchanged. |
| Channel pages 2 Hz full-tree recompose | Same as Overview today (collect metrics). Do not collect `hudFast`. ScopeTrace already peaks internally. |
| Dual net spark without tx hist | Add `netUpHistory` or show live ↑ only — **no fake**. |
| Widget LIVE 1 s vs Play | Unchanged contract: LIVE only charging/HUD. No second FGS. |

---

## 5 · Open questions — pinned defaults

| Q | Default (do not bikeshed) |
|---|---|
| New widget media | FOLLOW SYSTEM (`followSystem=true` data default stays). |
| Default cadence | AMBIENT 30 s. |
| Preview vs Glance pixels | Honest T2 band facsimile + caption. Not ScopeTrace. |
| Launcher rounded clip | Known OEM mask. Do not paint `rounded_widget_background` on Glance. |
| Channel timebase | 60 s default; 30 s if hist ≥30; hide 2 m+ until hist exists. |
| Net up hist | Add `netUpHistory` in the same Dashboard loop (one extra float per tick). |
| Pager swipe | Off this pass. |
| Alias default | Paper. Sync from DataStore on process start. |
| Pin success | `getBroadcast` + delay/resume. Not SettingsActivity. |
| Cpu/Gpu Material leftover on other tabs | Out of scope unless you already have the file open for logos (CPU/GPU only). |

---

## 6 · Out of scope

- Recreating Phase 0 (MonitorBus, MemInfoParser, TopConsumers, HudSettingsCache, BenchBudgetWorker, 15 WEBPs, 4-side hairline, receiver FQNs).
- Renaming receivers or `OverlayService`.
- Glance `providePreview`, IBM Plex in Glance, `Bitmap.recycle`, global `BenchFrames.clear`.
- 16 dp band gaps on T1. Fake GPU freq hist. Fake battery % curve. Fake net-up hist from rx.
- Widget-only FGS. Exact `AlarmManager`. `overlay_prefs` dual store.
- `Modifier.blur` on HUD/preview. Overlay WM `MATCH_PARENT`. Auto-start HUD after grant. Usage-stats as START gate.
- Long-press glossary. Hardware tabs as channel pages. Copying WI `widget.bench`.
- Tinting Android 13 themed icons from DataStore.
- Regenerating 15 picker WEBPs unless a screenshot QA of the **picker** fails (home clip is OEM, not picker).
- Kirin / Unisoc / Arc / Apple logo art.
- Reopening F1 nested scroll.

---

## 7 · Implementation notes / cheat snippets

**Glance tap → config**

```kotlin
val APPWIDGET_ID = ActionParameters.Key<Int>(AppWidgetManager.EXTRA_APPWIDGET_ID)
fun openConfig(id: Int) = actionStartActivity<BenchConfigActivity>(
    parameters = actionParametersOf(APPWIDGET_ID to id)
)
// provideGlance: val awId = GlanceAppWidgetManager(context).getAppWidgetId(id)
```

**Config RESULT_CANCELED (official 2026-08-06)**

```kotlin
val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
setResult(Activity.RESULT_CANCELED, resultValue)
```

**resolved preview medium**

```kotlin
val previewMedium = if (followSystem) {
    runCatching { context.mediumFlow.first() }.getOrNull()
        ?: if (night) Medium.CARBON else Medium.PAPER
} else medium
CaliperTheme(medium = previewMedium) { /* screen */ }
```

**W10 5 s gate**

```kotlin
val now = System.currentTimeMillis()
val live = GlobalSnapshot.current()
val snap = if (live != null && now - live.timestamp in 0 until 5_000L) live else benchDemoSnapshot(kind)
```

**Pin extras**

```kotlin
extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewBitmap)
mgr.requestPinAppWidget(componentName, extras, successCallback)
```

**HudPanel split** — see Phase 3 snippet. Root reads **zero** feeds. `HudFpsBand` is the only `fast` reader.

**Opacity debounce** — see Phase 3 `opacityJob`.

**F2 400 ms on resume**

```kotlin
if (event == Lifecycle.Event.ON_RESUME) {
    viewModel.refreshPermissions()
    scope.launch { delay(400); viewModel.refreshPermissions() }
}
```

**activity-alias enable-first** — see Phase 4. Component names are `applicationId + alias android:name`.

**Coil logos**

```kotlin
AsyncImage(
    model = logoRepo.logoUrlFor(info.socModel),
    contentDescription = "${info.socModel} logo",
    modifier = Modifier.size(56.dp),
    fallback = painterResource(R.drawable.ic_soc_generic),
    error = painterResource(R.drawable.ic_soc_generic)
)
```

---

## 8 · Safety pins (copy onto the worker)

- Do not recreate Phase 0 (MonitorBus, MemInfoParser, TopConsumers, HudSettingsCache, BenchBudgetWorker, 15 WEBPs, 4-side hairline, receiver FQNs).
- Do not rename receivers or OverlayService.
- No 16 dp band gaps on T1. No fake GPU freq hist / battery % curve. No IBM Plex in Glance (Monospace only). No `providePreview`.
- Overlay WM `WRAP_CONTENT` never `MATCH_PARENT`. No `Modifier.blur` on HUD or sheet preview. No auto-start HUD after grant. Usage-stats is not a START gate.
- No `overlay_prefs` dual store. No `Bitmap.recycle`. No global `BenchFrames.clear`.
- WD wins over WI on layout. Package stays `com.ivarna.deviceinsight.ui.caliper.widget`.
- Do not add a widget-only FGS. Do not exact `AlarmManager`.
- Do not copy WI `widget.bench` package.

---

## 9 · Worker execute order (canonical)

0. PASS 5 leftover (W8, W10, F2 400 ms + isRunning, lock DIP, HudPanel clip/spacedBy/heightIn). Optional MINOR spacers + comment strip if those files are open.
1. Widget field (theme followSystem as 4-way SegKey, Header/root clickable → config, RESULT_CANCELED extras, nudge logging + empty-cache, BandBitmap timestamps, preview bands + caption).
2. Instruments sheet UX + pin extras/callback + `PinSuccessReceiver`.
3. Overlay sheet FPS split + debounce + HudScales/`barHDp` actually used + compact spacing.
4. App icon crosshair + monochrome + activity-alias media + HUD notification icon.
5. Device tabs pager + logo bitmaps + strip Material leftovers on Cpu/Gpu tabs + hardwareInfo load-once.
6. Overview channel pages + wire `tap →`. GPU tile uses `gpuHistory`.

Stop at the end of each phase if compile is red. Do not start Phase 6 until Overview `tap →` can land without inventing Hardware-tab wrappers.

**Next agent: Worker. Next action: Phase 0 in order, then 1→6. No product code in this planning pass.**
