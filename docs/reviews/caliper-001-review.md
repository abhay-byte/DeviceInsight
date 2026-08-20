# CALIPER-001 Plan Review — PASS 2

Verdict: **APPROVE** (0 CRITICAL, 0 MAJOR, 6 MINOR, 1 SUGGESTION)

Scope checked: `docs/plans/caliper-001-plan.md` (pass 2) vs repo state (master + uncommitted worktree). Each pass-1 finding re-checked against plan text AND repo evidence.

## Pass-1 findings — all verified fixed

- **M1 Settings reachable — FIXED.** Plan pins Settings as in-app NavHost destination `№ 05 — SETTINGS`, entry = Masthead trailing gear HardKey (`SystemStatsApp.kt:174` TopAppBar gear confirmed as only current entry; removal would orphan Settings). Alternative 5th rail key explicitly rejected with rationale. Acceptance bullet updated. Note: snippet §7 `Masthead()` has no gear param — plan must extend signature; see SUGGESTION.
- **M2 QS tile — FIXED.** Grep `TileService|quicksettings|QS_TILE` = 0 today; plan now delivers full spec: `service/MediaTileService : TileService` cycling Paper→Carbon→Blueprint, manifest block with `BIND_QUICK_SETTINGS_TILE` + `QS_TILE` action + exported=true, `res/drawable/ic_tile_caliper.xml`, `onClick`/`onStartListening`, `updateTile`, locked/boot handling, shared `medium` DataStore key.
- **M3 Graphs.kt — FIXED, evidence stronger than claimed.** All 25 vico refs live in `presentation/components/Graphs.kt`; grep for `CpuUtilizationGraph|MemoryUsageGraph|…` = zero callers repo-wide (Graphs.kt is dead code; plan said "used only by Dashboard" — actually unused). Deletion + dropping 3 vico deps (`build.gradle.kts:136-138`) safe, no other consumer.
- **m1 BOM — FIXED.** `build.gradle.kts:113,164` confirmed `2024.12.01`; plan drops snippet `2024.09.03`, no downgrade.
- **m2 nav order — FIXED.** Pinned `[1] OVERVIEW (Dashboard) · [2] DEVICE (Hardware) · [3] OVERLAY · [4] PROCESSES (Tasks)`, `bottomNavItems = [Dashboard, Hardware, Overlay, Tasks]`, deviation from design §5.2/§6 documented.
- **m3 HUD blur — FIXED.** API 31+ `RenderEffect` gate, no-blur fallback 70% scrim + hairline, minSdk 26 untouched.
- **m4 marker — FIXED.** `design_implementation.md:1016` marker + doc warning 1351; plan pins strip-before-copy.
- **m5 fonts — FIXED.** OFL URLs pinned (Instrument Serif + Plex Mono 300/400/500), fallback serif/mono, `R.font` verify; `res/font/` absent today (glob confirmed) so bundling is real work now specified.
- **m6 drift — FIXED.** `git status` confirms 10 modified files (DashboardScreen 788±, strings.xml +8, providers); plan instructs worker rebase on current worktree before Phase 0.
- **S1 diff gate — ADDED.** Acceptance requires `diff new_design.md CALIPER.md` = 0.

## New findings (pass 2)

### MINOR

**P2-1. S1 diff vs "Record decision in §15" contradiction**
Location: plan Goal B item 4 vs acceptance S1
Problem: `new_design.md` §15 already contains Rev A row (line 839). Goal B4 says "Record decision in CALIPER.md §15 Document History Rev A" — if worker appends/edits, copy is no longer byte-identical and S1 diff fails.
Impact: acceptance self-contradiction; worker may edit CALIPER.md and fail gate.
Required planner change: state §15 already has Rev A in source; copy only, no edit.

**P2-2. Settings theme migration not pinned (compile-break surface)**
Location: `SettingsViewModel.kt:6,19,26` (imports `AppTheme`, `theme: StateFlow<AppTheme>`), `SettingsActivity.kt:15,29-31`, `SettingsRepositoryImpl.kt` (SharedPreferences `settings_prefs`)
Problem: Phase 0 deletes `AppTheme`/`SystemStatsTheme`. SettingsViewModel/SettingsActivity/SettingsRepositoryImpl still typed to `AppTheme` — code stops compiling until migrated. Plan never states SettingsViewModel signature change (`AppTheme` → `Medium` + DataStore) nor fate of `settings_prefs`/`overlay_prefs` for non-theme settings.
Impact: worker rediscovers Settings persistence wiring mid-Phase 6.
Required planner change: pin SettingsViewModel/SettingsScreen migration to `Medium` via DataStore; keep `settings_prefs`/`overlay_prefs` (SharedPreferences) for other keys unless migrated wholesale.

**P2-3. DataStore accessor singleton not pinned**
Location: none in repo (`grep DataStore` = 0; only `datastore-preferences:1.1.1` dep)
Problem: Plan introduces `medium` shared across MainActivity/SettingsViewModel/TileService/widget/Glance but doesn't pin single top-level `preferencesDataStore(name=…)` + key names. Two delegates on same file throw "multiple DataStores active for same file".
Impact: crash if worker creates per-package delegates; "same file" in plan is stated once (Phase 6) but not pinned structurally.
Required planner change: pin one shared accessor (`preferencesDataStore(name = "caliper")`, `ui/caliper` or `data`) + exact keys `medium`, `showGrid`, `hatchingEnabled`, `caliperMigrated`.

**P2-4. Double Masthead — scaffold-level vs screen-snippet-level**
Location: plan Phase 1 scaffold = global `Masthead + NavHost + ModeRail`; snippet §6 `ProcessesScreen` (design_implementation.md:1608) and §10 `OverviewScreen`/`DeviceInsightApp` embed their own `Masthead()` + `EndOfSheet`
Problem: copy snippet verbatim (Goal C) into screens beneath global scaffold → duplicated masthead/double-rule; demo shell §10 (`selected: Int`, `when`) also conflicts with Navigation-compose refactor plan (popUpTo/restoreState acceptance).
Impact: worker must reconcile assembly layout vs snippets; risk of stray duplicates.
Required planner change: note snippet screen-assemblies embed chrome; adapt to global scaffold (drop embedded `Masthead()`), use `SystemStatsApp.kt` refactor not §10 demo shell.

**P2-5. Widget snippet hardcodes Paper colors — violates §8 agreement**
Location: `design_implementation.md` §9 `ChannelWidgetContent` (`Color(0xFFFBF9F3)`, `0xFF191713`, `0x99191713`), plan Phase 7 + acceptance "Widget/HUD/app agree on every channel's color"
Problem: snippet ignores medium; Carbon/Blueprint widgets stay paper-styled. §8 mandates widgets follow medium like app+HUD. Copying verbatim fails acceptance under Carbon.
Impact: acceptance bullet "Widget/HUD/app never disagree" unmet; spec-vs-snippet contradiction unresolved.
Required planner change: instruct parameterized medium colors in widget (read DataStore `medium` in Glance provider, pass palette), or explicitly accept Paper-only widgets.

**P2-6. Existing widget receivers: replace-or-keep unspecified**
Location: manifest 3 receivers `DashboardWidgetReceiver`/`CpuWidgetReceiver`/`BatteryWidgetReceiver` + `res/xml/*_widget_info`
Problem: Plan designs new Glance 2×2/4×2/4×4 but never states old classes/receivers deleted/reused. Stale glassmorphism-less-but-old widgets contradict "every surface redesign".
Impact: worker decides; risk of leftover duplicate widget entry in picker.
Required planner change: map old 3 widgets → new 3 sizes, delete or keep receivers, update manifest + xml explicitly.

### SUGGESTION

**S1. Extend snippet `Masthead` signature for gear**
Snippet `Masthead(degraded, rootVerified)` (design_implementation.md:1672) has no trailing gear/`onSettings` param. Add `onSettingsClick: (() -> Unit)?` + keep 48dp target + `contentDescription="Settings"`; announce in TalkBack as button. Plan mentions gear but not snippet divergence.

## Verdict rationale

APPROVE. All pass-1 MAJOR/MINOR/S1 fixed with pinned text; M3 verified against repo (vico dead code, zero consumers). Pass-2 findings are minor consistency/migration clarifications — worker can implement without rediscovering architecture. No BLOCK: foundations (task mapping Tasks=Application Active, mode-rail reorder, theme swap, snippet coverage §0-§10) all valid.

NEXT_AGENT: Worker