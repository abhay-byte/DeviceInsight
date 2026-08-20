# CALIPER-001 Plan — Nav Reorder + CALIPER Redesign

Doc ID: DI-PLAN-001 | PASS 2 | Iteration 2 | Status: READY (REVISE applied)
Task: (a) move Application Active page to last nav position, (b) retire glassmorphism → CALIPER, (c) redesign every surface per `new_design.md` + `design_implementation.md`.
Review response: addresses caliper-001-review.md PASS1 REVISE (3 MAJOR M1-M3, 6 MINOR m1-m6, S1). Worktree drift noted [m6].

## 0 · Research Sources

- Local: `app/src/main/java/com/ivarna/deviceinsight/presentation/SystemStatsApp.kt` — sealed `Screen` + `bottomNavItems` + `GlassBottomNav`/`GlassNavItem` (Haze blur, 24dp radius, glow). <source: file>
- Local: `app/src/main/java/com/ivarna/deviceinsight/presentation/theme/Theme.kt` — 10-theme enum `AppTheme` (TechNoir … GoldenLuxe), all `darkColorScheme`. <source: file>
- Local: `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/DashboardScreen.kt` — HeroCard/CoresCard/GpuCard/Connectivity/Battery with `GlassCard`+gradients+radial glows. <source: file>
- Local: `app/src/main/java/com/ivarna/deviceinsight/presentation/hardware/HardwareScreen.kt` — 11 pill tabs, `LazyRow` pill bar + `when(selectedTabIndex)` dispatch. <source: file>
- Local: `app/src/main/res/values/strings.xml` — `nav_dashboard|nav_tasks|nav_hardware|nav_overlay`. <source: file>
- Local: `docs/design/new_design.md` (844 lines, DI-DS-002 Rev A) — full CALIPER language, tokens, components, S-00..S-14. Supersedes Elegant Glassmorphism. <source: file>
- Local: `docs/design/design_implementation.md` — drop-in `ui/caliper/` Compose snippets: `CaliperTheme.kt`, `CaliperDraw.kt`, `CaliperUtils.kt`, `CaliperPrimitives.kt`, `CaliperData.kt`, etc. Package `com.ivarna.deviceinsight.ui.caliper`. <source: file>
- Local: `app/src/main/java` glob 91 files — inventory below. <source: glob>
- Official docs (to fetch before impl): `developer.android.com/jetpack/compose`, `developer.android.com/jetpack/glance`, `developer.android.com/reference/android/service/quicksettings/TileService`, `developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY`, `m3.material.io` for `lightColorScheme`/`darkColorScheme`. <source: webfetch todo>
- ctx7 docs todo: `androidx.compose:compose-bom`, `androidx.glance:glance-appwidget:1.1.0`, `androidx.compose.animation:animatedContent`, `VibrationEffect`, `android.service.quicksettings.TileService`. Run `npx ctx7@latest library "Jetpack Compose" "AnimatedContent CubicBezierEasing SpringSpec"` etc before impl. <source: ctx7 todo>
- Reference pattern todo: clone similar CALIPER-like instrument UIs (e.g. `github search: jetpack compose oscilloscope canvas`, `instrument dashboard compose`) into `/tmp/opencode/` to extract `Canvas`+`PathEffect.dashPathEffect` patterns — currently blocked by read-only bash; record as risk. <source: NEW_RISKS>
- Fonts OFL pinned: `Instrument Serif` OFL https://fonts.google.com/specimen/Instrument+Serif + https://github.com/google/fonts/tree/main/ofl/instrumentserif (Regular+Italic), `IBM Plex Mono` OFL https://fonts.google.com/specimen/IBM+Plex+Mono + https://github.com/IBM/plex (Light 300/Regular 400/Medium 500 + italics). Licensed OFL, bundle under `res/font/`, never downloadable-font runtime. Fallback if TTF missing: serif → `serif` system, mono → `monospace`. Verify `R.font` IDs after copy. <source: new_design.md §4.3 + design_implementation.md §0; fixes review m5>
- BOM pinned: keep existing `androidx.compose:compose-bom:2024.12.01` from `app/build.gradle.kts:113`. Snippet §0 suggests `2024.09.03` — DO NOT add/downgrade, drop that line. <source: build.gradle.kts vs design_implementation.md §0; fixes review m1>
- Snippet stray marker: `design_implementation.md:1016` line `@ComotifyPreviewBugPlaceholder@` is marker only — strip before copy; plan instructs verbatim minus markers. <source: docs/design/design_implementation.md:1016; fixes review m4>

## 1 · Current Architecture & Inventory

### Nav (authoritative)
`SystemStatsApp.kt:87-106` defines `sealed class Screen { Dashboard, Tasks, Hardware, Overlay }` and `bottomNavItems = [Dashboard, Tasks, Hardware, Overlay]`. `GlassBottomNav` uses `HazeState`+`hazeChild(RoundedCornerShape(24dp), blur 16dp)` + radial glow Canvas. TopAppBar `TopAppBar` with logo + Settings gear → `SettingsActivity`. Transitions `fadeIn/fadeOut 150ms`. `strings.xml` maps titles.

Interpretation of “Application Active page” = `Tasks` (process list, `TasksScreen.kt` / `TasksViewModel.kt` / `data/repository/TaskRepositoryImpl.kt` / `domain/model/AppProcessInfo.kt`). Confirmation via `TasksScreen` reads `TaskRepository` + shows `Filter: ALL|APPS|SYSTEM` and kill actions. Image 1 presumably shows Tasks currently 2nd; must move to 4th/last.

### All Pages / Surfaces (complete inventory)
1. **Dashboard** — `presentation/dashboard/DashboardScreen.kt` (HeroCard, CoresCard, GpuCard, ConnectivityCards, BatteryCard), `DashboardViewModel.kt`, `DashboardRepositoryImpl.kt`, `DashboardMetrics.kt`, components `DashboardComponents.kt`, `Graphs.kt`, `CircularGauge.kt`, `GradientCard.kt`, `GlassCard.kt`.
2. **Tasks / Processes (Application Active)** — `presentation/tasks/TasksScreen.kt`, `TasksViewModel.kt`, `domain/model/AppProcessInfo.kt`, `data/repository/TaskRepositoryImpl.kt`.
3. **Hardware (Device Dossier)** — `presentation/hardware/HardwareScreen.kt` (11 tabs), `HardwareViewModel.kt`, `domain/model/HardwareInfo.kt`, 12 tab files: `CpuTab.kt`, `GpuTab.kt`, `MemoryTabs?` (memory via `MemoryProvider`), `DisplayTab.kt`, `NetworkTab.kt`, `BatteryTab.kt`, `AndroidTab.kt`, `SensorsTab.kt`, `StorageTab.kt` (likely), `DevicesTab.kt`, `DirectoriesTab.kt`, `CommonComponents.kt`, `Thermal` (inside `HardwareScreen.kt`).
4. **Overlay / HUD** — `presentation/overlay/OverlayScreen.kt`, `OverlayViewModel.kt`, `OverlayComponents.kt`, `service/OverlayService.kt` (`TYPE_APPLICATION_OVERLAY`), `data/fps/FpsMonitor.kt`.
5. **Settings** — `presentation/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `SettingsActivity.kt`, `data/repository/SettingsRepositoryImpl.kt`.
6. **Widgets** — `presentation/widgets/DashboardWidget.kt/.Receiver`, `CpuWidget.kt/.Receiver`, `BatteryWidget.kt/.Receiver`, `theme/WidgetTheme.kt` (Glance).
7. **App shell** — `MainActivity.kt`, `SystemStatsApplication.kt`, `presentation/theme/{Theme,Color,Type}.kt`, `presentation/components/ReorderableList.kt`, `di/AppModule.kt`.
8. **Onboarding/Calibration** — not present as dedicated flow (will be added S-00).
9. **Store / Icon** — `assets/`, `fastlane/`, `screenshot_*.png`, mipmaps.

Total distinct user-visible screens to redesign: 6 groups + onboarding + HUD + 3 widget sizes = 11 surfaces. Every one must lose glassmorphism.

### Theme System
`Theme.kt:15` enum 10 themes (TechNoir, Cyberpunk, DeepOcean, Matrix, Dracula, SunsetMirage, ForestSpirit, NeonNights, NordicIce, GoldenLuxe). All `darkColorScheme`. `Color.kt` holds hex. `Type.kt` likely inter + mono. CALIPER requires exactly 3 media (Paper `#F4F1E8`, Carbon `#141310`, Blueprint `#0C2338`) with `Medium` enum — full replacement.

### Data Layer (providers already exist)
`data/provider/{Cpu,Memory,Gpu,Power,Network,Storage,Sensor,Thermal,Battery,...}Provider.kt` — map directly to channels CH-01..CH-06 + thermal ramp. No new providers needed.

## 2 · Goals

### Goal A — Nav Reorder (isolated, ship first) [pinned — fixes m2]
Move Tasks (Application Active) to last position in `ModeRail`. Current `listOf(Dashboard,Tasks,Hardware,Overlay)` → **`listOf(Dashboard,Hardware,Overlay,Tasks)` pinned**. Numbering **pinned** `[1] OVERVIEW (Dashboard) · [2] DEVICE (Hardware) · [3] OVERLAY · [4] PROCESSES (Tasks/Application Active)` — Tasks last satisfies task. Deviation from design §5.2/§6 IA (OVER­VIEW/ACTIVITY/PROCESSES/DEVICE) is intentional minimal 4-key diff; documented in Phase 1. Minimal diff: only reorder list and update string ordering; do not mix with theming changes in same commit. Acceptance: bottom rail shows Tasks last, selection/caret/accent follows, `popUpTo`+`restoreState` still works, TalkBack order matches visual.

### Goal B — Design Doc Migration
1. Create `docs/design/CALIPER.md` as verbatim copy of `docs/design/new_design.md` (thesis: “The screen is a sheet of drafting paper. The phone is the instrument.”). Header notes `Save as CALIPER.md · supersedes Elegant Glassmorphism`.
2. Deprecate `docs/ui_ux_design.md` (add banner “Superseded by CALIPER.md (DI-DS-002)”) — do not delete history.
3. Keep `docs/design/design_implementation.md` as normative snippet library; extract tokens into code (do not duplicate spec in comments).
4. Record decision in `docs/design/CALIPER.md §15 Document History` Rev A.

### Goal C — Full CALIPER Redesign (strict)
Follow `new_design.md` §4-§12 + `design_implementation.md` §§0-5 code verbatim where provided. No improvisation on tokens/components. Enforce invariants: 0dp radius, hairlines 1dp, 4dp grid, ≤10% channel color, ≤2% accent, tabular numerals, hatch redundancy, serif only for page titles.

## 3 · Target Architecture

```
app/src/main/java/com/ivarna/deviceinsight/ui/caliper/
  CaliperTheme.kt        Medium enum, Channel registry CH-01..06, Paper/Carbon/Blueprint palettes, InstrumentSerif+PlexMono, CaliperMotion, Caliper locals, rememberReducedMotion()
  CaliperDraw.kt         hatch(), dashedBorder(), caliperGrid(), noteBox()
  CaliperUtils.kt        Fmt (bytes/hz/pct/temp/rate/duration/index), CaliperHaptics, rememberCaliperHaptics()
  components/
    CaliperPrimitives.kt LedDot, ChannelTick, DoubleRule, EndOfSheet, SpecRow, StampBadge, HardKey, DipSwitch, FaderKey, SegKey, OdometerText, MarginNote, BaselineField
    CaliperData.kt       PanelCard, ReadoutTile, Sparkline, ScopeTrace (§5.5 custom Canvas), CoreRail/CoreBar, LinearGauge, HatchBar
    CaliperLedger.kt     LedgerTable, Dossier (perforated), SafetyLatch (ARM rail), ProcessesScreen
    CaliperChrome.kt     Masthead (⌖ DEVICEINSIGHT + UTC clock + LED/DEGRADED/ROOT VERIFIED), ModeRail (64dp bottom, number+caps+caret, 600dp→left rail), ScreenHeader (№ + serif title), Loading/Empty/Fault, CalibrationSweep
  hud/CaliperHud.kt      corner-brackets overlay, 70% scrim + 8dp blur (sole exception), reorderable rows
  widget/ChannelWidget.kt Glance widgets 2×2/4×2/4×4 sharing hatch painter as bitmap

res/font/
  instrument_serif_regular.ttf, instrument_serif_italic.ttf,
  ibmplexmono_light.ttf, ibmplexmono_regular.ttf, ibmplexmono_medium.ttf
```

## 4 · Screen-by-Screen Redesign Plan

Do in strict order; each phase is shippable and ends with `— END OF SHEET —` present.

### Phase 0 — Foundations (blocks everything) [fixes m1, m4, m5, m6]
- Bundle fonts OFL via `res/font/` — **pinned sources**: `Instrument Serif` OFL `https://fonts.google.com/specimen/Instrument+Serif` + raw TTFs `https://github.com/google/fonts/tree/main/ofl/instrumentserif` (`InstrumentSerif-Regular.ttf`, `InstrumentSerif-Italic.ttf`); `IBM Plex Mono` OFL `https://fonts.google.com/specimen/IBM+Plex+Mono` + `https://github.com/IBM/plex/releases` (`IBMPlexMono-Light 300`, `Regular 400`, `Medium 500` + italics). Licensed OFL, no downloadable-font runtime — bundle only. Fallback if TTF missing: serif → `FontFamily.Serif`, mono → `FontFamily.Monospace`. Verify `R.font` IDs. Wire `InstrumentSerifFamily` + `PlexMonoFamily`, `fontFeatureSettings="tnum"` [fixes m5].
- Keep Compose BOM at existing `2024.12.01` (`app/build.gradle.kts:113`) — snippet §0 suggests `2024.09.03` **dropped**, do not add/downgrade [fixes m1].
- Strip snippet marker `@ComotifyPreviewBugPlaceholder@` (§5 Sparkline line 1016) before copy [fixes m4].
- **Worktree drift (m6):** `git status` shows 10 modified files + untracked `assets/`, `GpuLogoRepository.kt`, `docs/design/` divergence from HEAD; Worker must `git status`/`git diff` rebase inventory before Phase 0, treat this plan as layered on current worktree, not clean HEAD.
- Implement `CaliperTheme.kt` exactly from snippet §1: `Medium`, `Channel`, `Channels` registry (§4.1 hex), `CaliperColors` (paper/carbon/blueprint incl `hairline` 14%/18%/20% + `gridMinor/Major`), `CaliperType` scale (display1 40sp italic, display2 28sp italic, readout xl 54sp light, etc.), `CaliperMotion` (Ease 0.2,0,0,1; Needle 0.82/420; Snap 1.0/700; tFast 140, tBase 200, tSweep 420).
- `CaliperDraw.kt` hatch 6 patterns (solid/diag/cross/dots/vert/horiz) via `DrawScope.hatch()`, `dashedBorder()`, `caliperGrid()` (24dp minor @3% + 120dp major @5% toggle via `Settings → Presentation → Grid`).
- `CaliperUtils.kt` `Fmt` + `CaliperHaptics` (tick 8ms, confirm 15/20/15, arm 15×3 ascending, fault 40, stamp 12) + `rememberReducedMotion()` via `Settings.Global.ANIMATOR_DURATION_SCALE`.
- Replace `presentation/theme/Theme.kt` `AppTheme` + `SystemStatsTheme` with `CaliperTheme(medium)`; add `DataStore` key `medium` + `showGrid` + `hatchingEnabled` + `caliperMigrated`. Map legacy 10 themes → nearest medium (dark→Carbon, light→Paper) for migration prompt `Your instrument has been recalibrated [ INSPECT ]`.
- Remove Haze dependency where possible (keep single HUD 8dp scrim blur with API 31+ gate); delete `GradientCard`, `GlassCard` usages; delete radial glow Boxes.

### Phase 1 — Chrome (Masthead + ModeRail) + Nav Reorder + Settings Access [fixes M1, m2]
- Implement `CaliperChrome.kt`: `Masthead` 52dp, crosshair ⌖ + `DEVICEINSIGHT` mono caps 13sp left, UTC clock `14:32:07 UTC ●` right (colon blinks 1Hz, heartbeat), `DEGRADED` stamp replaces LED on permission loss, `ROOT VERIFIED` mini-stamp. Double-rule 2 hairlines 3dp apart under masthead.
- **Settings entry (M1 fix):** TopAppBar removal would orphan Settings (today `TopAppBar` gear → `SettingsActivity` at `SystemStatsApp.kt:174`). Pin Settings as in-app `NavHost` destination `№ 05 — SETTINGS` (IA §6) reachable via **Masthead trailing HardKey gear** (caliper-styled `HardKey`/IconButton 48dp, ink border, `contentDescription="Settings"`). Alternative 5th ModeRail key considered and rejected (crowds 64dp rail, conflicts with 4-key task). Acceptance updated to require Masthead gear visible on every sheet and navigates to `№ 05`. Keep `SettingsActivity` as thin wrapper launching same Compose route for intent compatibility until fully migrated.
- Implement `ModeRail`: bottom 64dp, hairline top, **exactly 4 keys** pinned order **[1] OVERVIEW (Dashboard) · [2] DEVICE (Hardware) · [3] OVERLAY · [4] PROCESSES (Tasks/Application Active)** — Tasks last satisfies task; labels match `strings.xml` renames (`nav_dashboard→OVERVIEW`, `nav_hardware→DEVICE`, `nav_overlay→OVERLAY`, `nav_tasks→PROCESSES`). Active = ink-filled number + caret `▲` + accent underline (`#FF4D00` Paper / `#FF5A1F` Carbon / `#63C7FF` Blueprint), LED per key warning, 120ms blink+tick. ≥600dp becomes left rail with labels + two-pane. Note deviation from design §5.2/§6 IA (which orders OVERVIEW/ACTIVITY/PROCESSES/DEVICE) is intentional minimal diff; document in code comment [fixes m2].
- Refactor `SystemStatsApp.kt`: remove `GlassBottomNav`, `TopAppBar`, radial glows, `HazeState`; scaffold becomes `Box(caliperGrid) + Masthead + NavHost + ModeRail`. Reorder `bottomNavItems = listOf(Dashboard, Hardware, Overlay, Tasks)` (was Dashboard,Tasks,Hardware,Overlay). Update `Screen` route titles to CALIPER labels as above. Keep `fadeIn/fadeOut 160ms + 8dp rise` per §4.7.
- TalkBack: each key `role=Tab`, `contentDescription` includes number+label. Masthead gear announces as button.

### Phase 2 — Dashboard → Overview (S-01 system ledger)
- Replace `DashboardScreen` LazyColumn of GlassCards with `№ 01 — OVERVIEW REV 2.0` header (serif `Overview.` + meta subline `all channels nominal` / `1 channel warning` stampRed).
- Each metric becomes `ReadoutTile` with `PanelCard` frame: hairline 1dp, 0dp radius, 16dp padding, header `channel tick 3dp square + meta label + trailing status (● LIVE/value/stamp)`, 60% rule under header. Tiles: `CH-01 CPU` (38.4% + sparkline), `CH-02 MEMORY` (6.81/12 GB + bar peak-hold), `CH-03 NETWORK` (↑↓ + dual spark), `CH-04 POWER` (fuel gauge). `ReadoutL` 34sp medium tabular, odometer roll on change (staggered 24ms). Pull-to-refresh = “re-ink” all traces 420ms sweep. End `— END OF SHEET —`. 2-col grid on tablet.

### Phase 3 — Channel Pages (S-02..S-06) + Hardware Tabs → Channels [fixes M3, m4]
- **Vico demolition (M3 fix):** `presentation/components/Graphs.kt` (33 `vico` refs, used only by Dashboard) is deleted; Dashboard switches to `Sparkline` + `ScopeTrace` custom Canvas per snippet §5. Grep confirms no other vico consumer — remove all 3 deps from `app/build.gradle.kts:136-138` (`vico:compose`, `vico:compose-m3`, `vico:core 2.0.0-alpha.28`) in same commit. Do not restyle-and-keep; full replacement.
- **Snippet marker (m4 fix):** When copying `design_implementation.md` verbatim, strip line `@ComotifyPreviewBugPlaceholder@` at §5 Sparkline (line 1016) — marker only, not code, would break compile.
- Create shared template: serif title (`Processor.` etc.), hero readout row, `ScopeTrace` full-width (timebase `SegKey: 30s·2m·10m·1h`), instrument blocks.
- `ScopeTrace` Canvas custom (replaces Vico): grid 24dp minor ink 4% / 120dp major 8%, Y 5 ticks meta, X 4-6 time labels, trace 2dp square caps channel color, pen dot 3dp, crosshair vertical hairline + `LeaderNote` (value/time/min/max/avg top-right), dual trace network (down channel, up ink 40%), empty `NO SIGNAL`, draw-in 420ms pen sweep, `contentDescription` summary.
- S-02 CPU: `CoreRail` rows 28dp, bar 8dp, tick scale 25/50/75/100, peak-hold ⌃ decay 2s, thermal ramp gauge amber→red + zone label, governor notes SpecRow.
- S-03 Memory: `HatchBar` composition (active/cached/free/ZRAM/swap) 16dp + hatch patterns, mini-ledger top consumers 5 rows → Processes filter.
- S-04 Network: dual-trace, hero rates odometer, session/today counters, per-app table labeled `≈`.
- S-05 Power: `LinearGauge` 12dp track ticks every 10% labeled 25/50/75/100, fill CH-04 amber / <20% stampRed pulse, discharge ScopeTrace negated area 20%.
- S-06 Storage: `HatchBar` cadastral map + legend dotted leaders + drill-in partitions.
- Map existing `HardwareScreen` 11 tabs onto these 5 channel pages + Device Dossier; keep tab navigation as `SegKey` within each channel page if needed. Preserve all `HardwareInfo` fields (SoC, Vulkan, codecs, DRM, etc.) via `SpecRow` dotted leaders.

### Phase 4 — Processes (S-09 ledger → dossier → SafetyLatch)
- Replace `TasksScreen` with `LedgerTable`: index column `%04d` ink/40, row 56dp two lines (package + CPU%/RSS; pid/uptime/state mono 11sp), sticky section headers with double-rule, `[SELF]` stamp, `†` for kill history, sort by tapping headers `▲/▼`, re-rank throttled 2s crossfade (not per-frame), `FIND: ▮` BaselineField, `DIPSwitch` for kernel threads.
- Row tap → `Dossier` bottom sheet perforated edge (dashed 1dp), mini CPU/MEM traces 60s, PSS/USS, uid/oom_adj/seccomp, `FORCE STOP` + `TERMINATE ⏻` (latter behind `SafetyLatch` hatched ARM rail: drag square knob → ARM + ascending haptic → unlock red `KILL` HardKey stamp-in; `ABORT` always available 200ms fade). Disabled `TERMINATE` = dashed+hatch+key glyph, long-press MarginNote. After kill: stamp `TERMINATED 14:32:07` + `MarginNote` + log to `KILL LOG` cited by ledger index.
- Move Tasks nav entry to last position validated here.

### Phase 5 — Device Dossier (S-10 hardware)
- `№ 04 — DEVICE DOSSIER` with `SegKey [SUMMARY][COMPUTE][DISPLAY][SENSORS][CODECS]`. Spec sheets `SpecRow` dotted leaders. Plates as line art (`FIG. n` serif captions): cluster topology, display chain, camera map, sensor inventory. Sensors tab channel strips (index, name, raw+units, 24dp sparkline, rate SegKey normal/game/fast), compass rose, 6h pressure trace. Widevine/codec/Vulkan monospace tables hairline rules sticky header + `FIND:`.

### Phase 6 — HUD Overlay (S-11) + Settings/Colophon (S-13) + Quick Settings Tile [fixes M2, m3]
- HUD: corner brackets only frame, 70% ink scrim + 1dp hairline (+8dp blur exception with API fallback: `if (Build.VERSION.SDK_INT >= 31)` apply 8dp `RenderEffect blur` on scrim; else no blur, 70% scrim + hairline only for legibility [fixes m3]; `minSdk 26` respected, `build.gradle.kts:47` unchanged), mono `FPS 119.8 [SF]` odometer, per-metric rows channel ticks, drag via crosshair handle, config sheet size S/M/L SegKey + opacity fader 40-90% live preview + per-metric DIPs + per-app profiles + color mode ink/paper/channel; update 500ms values/100ms FPS, 1dp stroke for legibility.
- Settings: `№ 05 — SETTINGS` sections `01 PRESENTATION` (media SegKey Paper/Carbon/Blueprint, grid DIP, hatching DIP, key clicks DIP), `02 MONITORING` (sample rate fader 250ms-2s, history 60s, keep awake DIP), `03 HUD` defaults+profiles, `04 PROCESSES` kernel DIP + kill log, `05 SYSTEM` haptics/reduced-motion/locale, `06 ABOUT → COLOPHON` (“Set in Instrument Serif & IBM Plex Mono. Drawn on 4pt grid. No gradients…”, revisions Rev A…).
- **Quick Settings tile (M2 fix):** New `service/MediaTileService : TileService` toggling `Medium` cycle `Paper → Carbon → Blueprint → Paper` (Blueprint deliberate but included). Manifest: `<service android:name=".service.MediaTileService" android:exported="true" android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"><intent-filter><action android:name="android.service.quicksettings.action.QS_TILE"/></intent-filter></service>` + `res/drawable/ic_tile_caliper.xml` crosshair ⌖ monochrome (1.5dp stroke). Shares single `DataStore` key `medium` with `SettingsViewModel`/`MainActivity`/widget (`DataStore` Preferences, same file). `onClick` reads current, writes next, updates `qsTile.label/subtitle/state/icon` + `qsTile.updateTile()` + `requestListeningState`. `onStartListening` syncs tile to DataStore. Handles locked/boot. Acceptance requires tile visible in QS edit tray and cycles media.

### Phase 7 — Widgets (S-12 bench instruments) + Calibration (S-00)
- Widgets Glance `2×2 SINGLE / 4×2 DUAL / 4×4 BENCH` using same ticks/readouts/hatch bitmaps, picker previews tilted −3° on graph paper, deep link to channel page, update 30s (1s charging), `upd 14:32:07` timestamp mandatory.
- Calibration onboarding 4 sheets: 01 USAGE ACCESS, 02 OVERLAY, 03 ROOT PROBE (stamp ROOT VERIFIED/CHANNELS PARTIALLY LOCKED), 04 MEDIA swatches; certificate device name + date + `CALIBRATED` stamp + `SHARE CERTIFICATE` PNG. Skip surfaces as MarginNotes not blocking dialogs. First-launch sweep 1.2s L→R + stamp.

### Phase 8 — System States (S-14) + Motion/A11y Polish
- Edge states: permission revoked → masthead DEGRADED + MarginNote + `≈`; root locked panels hatch+key+dashed; `NO SIGNAL` flat line + `—`; sensor absent strikethrough + `NOT FITTED`; work profile headers + `WORK` stamp; foldable/tablet left rail two-pane.
- Motion tokens centralized `CaliperMotion` only; reduced motion branches (sweep→120ms fade, odometer instant, needle critically damped, LED static) via `rememberReducedMotion()`. Haptics/sound each honoring system setting.
- A11y: contrast ≥12:1 ink/paper, traces ≥4.5:1, accent ≥3:1 large targets; ≥48dp targets; font scale to 200% (serif wrap, readout tier down xl→l→m, tables horizontal scroll); RTL mirror tick leading, trace L→R unchanged; TalkBack readouts “CPU load, 38.4 percent”, chart summary min/max/avg, stamps announce.

## 5 · Migration & Deletion List

Remove per §2 Demolition: `haze` blur, neon gradients/glows, 16-28dp rounded cards, elevation shadows, FAB, pill chips, generic chart styling, 5 vibe themes. Keep only HUD 8dp scrim blur **gated API 31+ with fallback (no blur <31)** [m3]. Delete `GlassCard.kt`, `GradientCard.kt` after replacement (or adapt as PanelCard). **Delete `presentation/components/Graphs.kt` + remove `vico:compose/compose-m3/core 2.0.0-alpha.28` deps (`app/build.gradle.kts:136-138`) — replaced by custom Canvas Sparkline/ScopeTrace [M3].** Map `AppTheme` → `Medium` via `DataStore caliperMigrated` one-time MarginNote migration. **Do not add BOM 2024.09.03; keep 2024.12.01 [m1]. Strip `@ComotifyPreviewBugPlaceholder@` marker [m4]. Pin fonts OFL via bundled TTFs §0 [m5]. Apply to current worktree (`git status` drift) not clean HEAD [m6].**

## 6 · Risks

- Font licensing/OFL bundling size + runtime fallback if ttf missing → verify R.font ids, add lint check; sources pinned OFL Google Fonts/IBM Plex GitHub, never downloadable-font [m5].
- `PathEffect.dashPathEffect` + `hatch()` performance on low-end (60 traces) → bitmap caching for HatchBar/Glance.
- HUD `TYPE_APPLICATION_OVERLAY` + Glance `1.1.0` compatibility with compileSdk 36 → ctx7 version pin needed; HUD blur gated API 31+ fallback no-blur [m3].
- Reduced-motion detection via `ANIMATOR_DURATION_SCALE` not reactive — need `ContentResolver` observer.
- Vico resolved: delete `Graphs.kt` + drop 3 vico deps, replaced by Canvas Sparkline/ScopeTrace; no restyling option [M3].
- BOM downgrade resolved: keep `2024.12.01`, drop snippet `2024.09.03` [m1]; marker stripped [m4].
- Reordering nav without updating deep links/widget intents → audit `AndroidManifest.xml` + `DashboardWidget.kt` pending intents; pinned order [1] OVERVIEW [2] DEVICE [3] OVERLAY [4] PROCESSES [m2].
- Settings entry resolved via Masthead gear HardKey → №05 [M1]; TileService new `MediaTileService` shares DataStore `medium` [M2].
- Worktree drift: 10 modified + untracked `assets/`/`GpuLogoRepository.kt`/`docs/design/` divergence vs HEAD [m6]; worker rebases before Phase 0.
- Suggest S1 applied: test gate diff-check CALIPER.md vs new_design.md.

## 7 · Test Strategy (manual + assert)

- Visual: screenshot ratio 88/10/2 check via eyedropper; no radius >0 except LEDs/stamps; hairline renders 1dp on mdpi/hdpi/xhdpi.
- Interaction: nav reorder tap each item restores state; ModeRail LED warning simulation; SafetyLatch two-step cannot bypass; DPSwitch snap+LED; HUD drag+opacity fader.
- Motion: toggle `Animator duration scale 0` → verifies reduced motion branches; haptics with `Vibrator.hasVibrator()`.
- A11y: TalkBack traverse readouts+charts+ledger; font scale 200%; RTL layout; 48dp target ruler.
- Data: long-press every metric label → MarginNote glossary appears; crosshair drag shows LeaderNote; pull-to-refresh sweeps.
- Widget: add 2×2/4×2/4×4, verify timestamp+deep link, 30s update via adb.
- One runnable check per component (assert-based self-check): `Fmt.bytes(1536)=1.50 KB`, `Medium.CARBON channel(CPU)=#FF6B4A`, `hatch(DOTS)` draws non-empty — add `CaliperSelfCheck.kt` with `check{}` blocks run in `Debug` build.

## 8 · ACCEPTANCE_CRITERIA / FEATURE_SET (manual tester gate)

- [ ] `docs/design/CALIPER.md` exists identical to `docs/design/new_design.md` — verified via `diff docs/design/new_design.md docs/design/CALIPER.md` returns 0 [S1]; docs/design/CALIPER.md header states supersedes glassmorphism; old design marked superseded.
- [ ] Bottom nav `ModeRail` shows Tasks (Application Active / PROCESSES) as **last item** (position 4 of 4). **Pinned order: [1] OVERVIEW (Dashboard) · [2] DEVICE (Hardware) · [3] OVERLAY · [4] PROCESSES (Tasks)** [m2]. Visual order matches TalkBack order. Selecting last item highlights with ink-filled number + caret ▲ + accent underline + LED blink 120ms + tick haptic; `popUpTo+restoreState` works. `bottomNavItems = [Dashboard, Hardware, Overlay, Tasks]` verified in code.
- [ ] Nav bar matches spec: 64dp height, hairline top edge, mono caps labels with numbers, 0dp radius, no blur/gradient; ≥600dp becomes left rail with full labels + two-pane ledger/dossier.
- [ ] `Masthead` 52dp present on all sheets: ⌖ + DEVICEINSIGHT mono caps 13sp left, UTC clock right colon blinks 1Hz + ● LED; double-rule below; shows DEGRADED stamp on revoked permission and ROOT VERIFIED stamp when verified (stamp-in −3° 180ms + haptic). **Masthead trailing gear HardKey (48dp, ink border) navigates to `№ 05 — SETTINGS`; Settings reachable after TopAppBar removal [M1]; SettingsActivity preserved as thin wrapper if needed.**
- [ ] Theme system is exactly 3 media: Paper `#F4F1E8/#FBF9F3/#191713/#FF4D00`, Carbon `#141310/#1C1B17/#EDE7DA/#FF5A1F`, Blueprint `#0C2338/#12314E/#EAF2FF/#63C7FF`. No other themes selectable. **Compose BOM kept at `2024.12.01` (not snippet `2024.09.03`) [m1].** Settings → Presentation SegKey switches media; **Quick Settings `MediaTileService : TileService` cycles Paper→Carbon→Blueprint, manifest `BIND_QUICK_SETTINGS_TILE`, `ic_tile_caliper.xml`, shares DataStore `medium` [M2];** follows system dark (light Paper/dark Carbon) by default, Blueprint deliberate only. Old 10 themes migrated with “recalibrated” MarginNote once (`caliperMigrated` flag).
- [ ] Typography: page/section titles serif italic 40sp/28sp max 8 words; all else IBM Plex Mono with tabular figures (`tnum`); meta 11sp 500 +0.08em caps; numerals never shift layout.
- [ ] Grid & radius: 4dp base, 16dp margins, 12dp panel gap, 1dp hairlines, 0dp radius (only LEDs circles/stamps 2dp/avatars 2dp), graph-paper grid 24dp minor @3% +120dp major @5% toggleable, `— END OF SHEET —` 11sp ink/40 centered at end of every scrollable sheet.
- [ ] Channel registry enforced: CH-01 CPU `#E5482B/#FF6B4A` solid, CH-02 MEMORY `#2E5BE0/#6B8CFF` diag, CH-03 NETWORK `#0E9F6E/#2FD3B0` cross, CH-04 POWER `#F0A419/#FFB84D` dots, CH-05 STORAGE `#8757D6/#B08CFF` vertical, CH-06 GPU `#D6409F/#F06BB0` horiz; color only on tick/trace/peak/LED with mandatory label+hatch redundancy; thermal is amber→red ramp only; accent Signal Orange only on interactive.
- [ ] Iconography 1.5dp square caps geometric on 24dp grid; custom `di_ic_*`; no emoji headers.
- [ ] Components present and spec-compliant: `PanelCard` (hairline, 16dp, channel tick, 60% rule), `ReadoutTile` (readout/l 34sp, subline, PeakBar 6dp, sparkline pen dot), `ScopeTrace` (engineering grid, 2dp square caps, 3dp pen, crosshair+LeaderNote, dual trace, NO SIGNAL, 420ms sweep, a11y summary), `CoreRail` (28dp rows, 8dp bars peak-hold ⌃ 2s decay), `LinearGauge` (12dp track, <20% stampRed pulse), `HatchBar` (16dp, min 8dp), `LedgerTable` (index %04d, 56dp rows, sticky headers double-rule, [SELF]/†), `Dossier` (perforated), `SafetyLatch` (ARM drag then KILL), `DIPSwitch` 48×32dp square knob +LED, `FaderKey` hairline rail, `SegKey` joined borders, `HardKey` 48dp 1.5dp/caps (primary/secondary/destructive/disabled dashed+hatch), `StampBadge` −3° 1.5dp, `MarginNote` NOTE nnn + action + dismiss 4s, Empty/Loading/Fault states as spec.
- [ ] Motion tokens only: `Ease 0.2,0,0,1`, `Needle 0.82/420`, `Snap 1.0/700`, tFast 140/tBase 200/tSweep 420; odometer stagger 24ms, pen draw 420ms, needle spring, stamp scale 1.12→1.0 −3° 180ms, LED 2s sine, sheet 160ms crossfade+8dp rise; reduced-motion respected (sweeps→120ms fade, odometer instant, needle damped, LED static).
- [ ] Haptics: tick 8ms, confirm 15/20/15, arm 15/15/15 ascending 40→120, fault 40, stamp 12; sound off by default.
- [ ] Formatting: `38%`, `2.84 GHz`, `6.81 GB`, `46.2°C`, `18.1 MB/s`, `6h 12m`, `—` null, `≈` estimates, `● LIVE`, thin-space thousands, all per §4.9 incl `Fmt` self-checks.
- [ ] S-00 Calibration onboarding 4 sheets + certificate share PNG shows correctly and is skippable.
- [ ] S-01 Overview ledger tiles with sparklines, warning LED+stampRed threshold, pull-to-refresh sweep works; S-02..S-06 channel pages hero+ScopeTrace+instrument blocks match deltas; S-09 Processes sorting/filter 2s re-rank crossfade + FIND + SafetyLatch; S-10 Device Dossier tabs + SpecRow dotted leaders + plates FIG.n line art; S-11 HUD corner brackets 70% scrim + limited 8dp blur + reorder + config; S-12 Widgets 2×2/4×2/4×4 Glance tiles with timestamp; S-13 Settings control panel + Colophon + revisions; S-14 edge states `DEGRADED`/`CHANNEL LOCKED`/`NO SIGNAL`/`NOT FITTED`/`WORK`.
- [ ] `Graphs.kt` deleted, `Sparkline`/`ScopeTrace` custom Canvas replaces it; `vico` grep returns 0; `app/build.gradle.kts` has no `vico` lines [M3]; `@ComotifyPreviewBugPlaceholder@` absent from codebase [m4]; BOM `2024.12.01` retained [m1].
- [ ] HUD scrim 70% + hairline with 8dp blur **only on API 31+**, no blur fallback on API <31 verified (emulator or build check) [m3]; no other blur/glass elsewhere.
- [ ] Fonts bundled OFL from pinned URLs (Instrument Serif + IBM Plex Mono 300/400/500), `R.font` ids resolve, no downloadable-font request, fallback serif/mono defined [m5]; worktree applied on current `git status` drift not clean HEAD [m6].
- [ ] Color ratio ≥88% paper/ink ≤10% channel ≤2% accent per screenshot; no glass/blur/gradient outside HUD scrim (HUD blur is sole exception gatekept API 31+).
- [ ] A11y gate: contrast 12:1 ink/paper, 4.5:1 traces, focus ring 2dp accent+2dp offset, ≥48dp targets, Ledger 56dp, TalkBack reads readouts/charts/crosshair actions/stamps, font scale 200% steps readout tiers, tables horizontal scroll, RTL mirrored.
- [ ] QA checklist §14 all 10 items + Do/Don’t table verified.

## 9 · Implementation Order (PR slicing)

PR1: Foundations + `CALIPER.md` + migration flag (no UI yet). PR2: ModeRail+Masthead+nav reorder. PR3: Overview. PR4: ScopeTrace+CoreRail+Gauges+HatchBar. PR5: Processes/Dossier/SafetyLatch. PR6: Device Dossier + plates. PR7: HUD. PR8: Widgets + Calibration. PR9: A11y/motion/edge polish.

## 10 · Out of Scope / Non-Goals

No new data providers; no FAB revival; no per-theme layout differences; no accent customization; no rounded cards reintroduced; no emoji/gradient revival; no backend changes.

