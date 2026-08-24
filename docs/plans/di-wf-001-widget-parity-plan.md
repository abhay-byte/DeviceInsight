# Plan: di-wf-001-widget-parity — picker preview ≡ live Glance widget

## Task Summary
Kill every source of divergence between the launcher's widget picker preview (`previewImage`) and the live home-screen widget:

1. **SizeMode.Exact** — `LocalSize` becomes the real launcher size; bands sized to truth.
2. **Top-level `InstrumentBody`** — one composable renders both the live widget AND the preview capture.
3. **Font-scale damping + `maxLines=1`** — text can't explode at user font scale.
4. **`BandBitmap` keyed/rendered by actual pixel width** — no stretch/letterbox after resize.
5. **Styled `bench_initial.xml` first paint** — never Glance spinner.
6. **`BenchPreviewGenerator`** — captures previews from the REAL Glance→RemoteViews pipeline at 3× density, fontScale 1.0; debug-only **PreviewStudioActivity** runs capture on-device; generated PNGs replace marketing webp `previewImage`s.

Adapted from user-provided spec DI-WF-001 with repo-verified deltas (**D1–D11** below — the pasted plan assumed a `widget/bench/` layout and a different baseline that does NOT match this repo).

## Research Sources
- <source: repo file app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/BenchGlance.kt> — all 5 widgets, BandBitmap (L159-184), BenchPanel w/ hairline frame (L189-220), atoms L222-315, TileBitmap L319-351, receivers L777-840
- <source: repo file .../widget/BenchModel.kt> — Tier.of has `-20` tolerance (L34-36); BenchFrames LruCache (L390-399); BenchState datastore keys; cadenceMs L448; resolvedMedium L454
- <source: artifact javap androidx.glance:glance-appwidget:1.1.0 AAR (in Gradle cache)> — `GlanceRemoteViews.compose(Context, DpSize, stateDef?, Bundle?, @Composable): RemoteViewsCompositionResult`; `RemoteViewsCompositionResult.getRemoteViews(): android.widget.RemoteViews`. API verified against the exact version in `app/build.gradle.kts:136`
- <source: artifact javap androidx.glance:glance 1.1.0> — `TextKt.Text(text, modifier, style, maxLines:int)` → maxLines EXISTS in 1.1.0; `ImageKt.Image(provider, desc, modifier, contentScale:int-mangled, colorFilter)` → ContentScale param EXISTS
- <source: github.com/android/platform-samples cloned to /tmp/opencode/platform-samples> — `samples/user-interface/appwidgets/.../glance/weather/WeatherGlanceWidget.kt` (Responsive pattern we are replacing), `layout/utils/PreviewAnnotations.kt` (reference cell sizes), `glance/layout/utils/ImageUtils.kt`
- <source: https://developer.android.com/develop/ui/compose/glance/setup> (fetch timed out; superseded by direct AAR inspection of the pinned 1.1.0 artifacts — stronger evidence)
- <source: repo tests app/src/test/java/com/ivarna/deviceinsight/ui/caliper/widget/{BenchSelfCheckTest,WidgetReceiversExistTest}.kt> — guards receiver FQNs, KEEP policy, preview drawables presence
- <source: adb devices> — realme X2 Pro (RMX1931) attached ⇒ on-device capture + verification possible

## Current Architecture
- Package root is `com.ivarna.deviceinsight.ui.caliper.widget` (**not** `widget.bench`).
- `BenchGlance.kt` holds everything: 5 `GlanceAppWidget` classes with bodies inline inside `provideContent {}`, shared private atoms (`BenchPanel`, `Header`, `Footer`, `Hero`, `Subline`, `ChannelRow`, `BenchMasthead`, `TileBitmap`), `BandBitmap` (sync render via `renderSync` + `BenchFrames` LRU), receivers.
- Sizing today: `SizeMode.Responsive(AllSizes)` / `(BenchSizes)` (BenchGlance.kt:78-79) — root cause #2.
- Root panel already does `fillMaxSize().background(panel)` (BenchGlance.kt:199) — fix #6 partially present; hairline frame + a11y semantics + tap-to-config MUST be preserved.
- Horizontal content inset of BenchPanel = 12(col pad) + 1(hairline bar) + 9(inner pad) = **22dp per side ⇒ 44dp total** (BenchGlance.kt:204-218). The pasted plan's "24dp" would overflow by 20dp → right-edge clip returns.
- Widget XMLs: `res/xml/{single_channel,dual_channel,fuel,raster,bench_widget}_info.xml` — all use `glance_default_loading_layout` (fix #7 target), all have `android:configure="...BenchConfigActivity"` (must keep), descriptions already exist as `@string/widget_desc_*`.
- Existing preview assets: `drawable*/preview_<kind>_<medium>.webp` (marketing renders) referenced by all 5 XMLs; guard test counts them.
- No `res/layout/` dir yet; no bench colors in `values/colors.xml`; no `src/debug/` source set.
- CALIPER components for Preview Studio verified: `CaliperTheme` (CaliperTheme.kt:160), `Masthead()` (CaliperChrome.kt:42), `ScreenHeader(sheetLabel,title,sub)` (:264), `PanelCard(title){}` (CaliperData.kt:60), `HardKey(label,onClick,variant)` (CaliperPrimitives.kt:147), `EndOfSheet()` (:91), `Modifier.caliperGrid()` (CaliperDraw.kt:90), `Caliper.type.dataS`. Panel hexes: Paper `0xFFFBF9F3`, Carbon `0xFF1C1B17` (CaliperTheme.kt:78,87).

## Plan Deltas vs Pasted DI-WF-001 (binding)
- **D1 Paths**: all code lives in `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/widget/`.
- **D2 Band inset = 44dp**, not 24 (see architecture above).
- **D3 Keep current type scale** (Hero 30sp, value 16sp/14sp, meta 11sp) — route through `wSp()` damping + `maxLines=1`. Do NOT change sizes to 28/15/10 (visual churn without parity benefit).
- **D4 BenchPanel signature unchanged** (`pal, contentDescription, configTap`) — keep hairline frame, semantics, root tap.
- **D5 No `sweep` param** — symbol doesn't exist in this repo. `InstrumentBody(kind, tier, medium, cfg, snap, calibrating, stateKeyHint)`.
- **D6 All five widgets incl. `BenchWidgetAll` switch to `SizeMode.Exact`**; delete Responsive sets.
- **D7 Generator applies `result.remoteViews`** to off-screen `FrameLayout`, measures EXACTLY `tier*3px`, draws ARGB_8888 (verified API).
- **D8 `previewSnapshot()` matches real ctor order/names** (`Consumer(pkg,label,rssMb)`, `MemSeg(fraction,pattern,channelId)`, includes `netHist/gpuGles/batteryPresent=true`).
- **D9 Widget XMLs keep `android:configure`** and `@string/widget_desc_*` ids; only add initialLayout/minResize/maxResize (+ later swap previewImage name).
- **D10 Colors mirror Caliper tokens exactly** (paper/carbon ink60 from CaliperTheme.kt:76-96) so initial layout matches panel.
- **D11 Device IS attached** ⇒ F8 PNG generation happens in-pipeline (worker builds+installs debug, drives PreviewStudioActivity, pulls PNGs into `drawable-nodpi/`, rewires XMLs, commits).

## Affected Components & Dependencies
- Modify: `ui/caliper/widget/BenchGlance.kt`, `BenchModel.kt`
- New: `ui/caliper/widget/BenchPreviewGenerator.kt` (main srcset — generator object is safe in release; only the Activity is debug-gated), `app/src/debug/java/.../PreviewStudioActivity.kt`, `app/src/debug/AndroidManifest.xml`, `res/layout/bench_initial.xml`, `res/values/colors.xml`(+night), `drawable-nodpi/preview_*.png` ×5 paper variants
- Edit: 5× `res/xml/*_info.xml`
- Tests: extend `BenchSelfCheckTest` (Tier.of boundaries, previewSnapshot determinism)
- Deps: none added (Glance 1.1.0 already provides maxLines/ContentScale/GlanceRemoteViews — verified above)

## Implementation Steps (ordered, smallest correct change)

### F1 — Tier.of exact bounds (BenchModel.kt:31-37)
```kotlin
fun of(wDp: Int, hDp: Int): Tier =
    entries.lastOrNull { wDp >= it.wDp && hDp >= it.hDp } ?: T1
```

### F2 — SizeMode.Exact + InstrumentBody extraction (BenchGlance.kt)
- Delete `T?Size` sets + `Responsive` vals (L70-79); every widget: `override val sizeMode: SizeMode = SizeMode.Exact`.
- Lift each inline body into top-level `@Composable ScopeBody(tier, medium, cfg, snap, calibrating, awId, context)` … same for Stack/Fuel/Raster/Bench (mechanical move of existing code, params explicit; keep ALL current logic: stale, consumers rows, not-fitted branches, T-branching, TileBitmap).
- New dispatcher:
```kotlin
@Composable fun InstrumentBody(
    kind: WidgetKind, tier: Tier, medium: Medium, cfg: BenchConfig,
    snap: BenchSnapshot, calibrating: Boolean, awId: Int
) { when (kind) { SCOPE -> ScopeBody(...); STACK -> ...; } }
```
- Each `provideGlance` keeps its prep (cfg/snap/placedAt/awId) then calls `provideContent { InstrumentBody(...) }` reading `LocalSize.current` for tier. Receivers/BenchUpdater untouched.

### F3 — Style hardening (BenchGlance.kt)
- `wSp(base):TextUnit` = `base / (1f + (fontScale-1f)*0.35f)` per DI-WF-001 §3 (reads LocalContext configuration).
- Apply inside TextStyle factories for: Header/Footer/Subline/Meta texts (11→`wSp(11)`), Hero (30), ChannelRow value (16), tile label/value (11/14), Fuel/Raster NOT-FITTED & LOCKED heroes (22/18/16), CHARGING/[GRANT IN APP] accents (11). Every `Text(` gets `maxLines = 1`.
- `BandBitmap` replacement (drop `tier` & `bitmapWidthDp` params):
```kotlin
val size = LocalSize.current
val wPx = ((size.width.value - PANEL_INSET_DP) * density).toInt().coerceAtLeast(8) // PANEL_INSET_DP = 44 (D2)
val key = "$stateKey|$band|$medium|${wPx}x${hPx}"   // width in key (root cause #8)
```
Display `fillMaxWidth().height(bandHeightDp.dp)`, `contentScale = ContentScale.FillBounds` as seatbelt (param exists in 1.1.0 — verified). Update ALL call sites (Scope spark/scope/thermal/rail, Stack hatchBar/memSpark, Fuel fuel/wattTrace, Raster gpuSpark/lockedField, Bench tiles+rail). Scope's side-by-side trace column: bitmap now takes natural half-width via `defaultWeight()` container — compute wPx from `size.width.value/2 - 26` when in split layout (pass explicit `widthFraction` param instead of removed bitmapWidthDp: `BandBitmap(..., widthDp: ((Float)->Int)? = null)`).

### F4 — Initial layout + colors
- `res/layout/bench_initial.xml`: FrameLayout `match_parent` bg `@color/bench_panel`; centered "CALIBRATING…" monospace 11sp letterSpacing .08 textColor `@color/bench_ink60` (per DI-WF-001 §5).
- `res/values/colors.xml`: `bench_panel=#FBF9F3`, `bench_ink60=<PaperColors.ink60 hex>`; `res/values-night/colors.xml`: `#1C1B17` + carbon ink60. Worker copies exact ink60 hexes from `CaliperTheme.kt:78-96`.

### F5 — Widget provider XMLs (all 5)
Add: `android:initialLayout="@layout/bench_initial"`, `android:minResizeWidth="140dp"`, `android:minResizeHeight="110dp"`, `android:maxResizeWidth="450dp"`, `android:maxResizeHeight="320dp"` (bench: min 250×110). KEEP configure/description/targetCell*/previewImage (names rewired in F8).

### F6 — BenchPreviewGenerator.kt (new)
Per DI-WF-001 §6 verbatim structure with D7/D8 adaptations:
- `createConfigurationContext(densityDpi=480, fontScale=1f)`
- `GlanceRemoteViews().compose(ctx, DpSize(tier.wDp.dp,tier.hDp.dp)) { InstrumentBody(kind, Tier.of(w,h), medium, cfg, snap, calibrating=false, awId=-1) }`
- `result.remoteViews.apply(ctx, host)` → measure/layout EXACTLY `tier*3` px → draw → PNG `files/previews/preview_<kind>_paper_<w>x<h>.png` (paper variant only for shipping; matrix still generates all 3 media for QA) + `DROP_IN_MANIFEST.txt`.
- Shot matrix: SCOPE/STACK/FUEL/RASTER @ T2, BENCH @ T4 × {PAPER,CARBON,BLUEPRINT}.
- Add `BenchDemo.previewSnapshot()` (deterministic, D8 field names).

### F7 — Debug Preview Studio
- `app/src/debug/java/com/ivarna/deviceinsight/ui/caliper/widget/PreviewStudioActivity.kt` — CaliperTheme(PAPER), Masthead(), ScreenHeader("№ DEBUG","PREVIEW STUDIO",…), PanelCard(title="GENERATE"){ HardKey("CAPTURE ALL (5 WIDGETS × 3 MEDIA)", onClick={…generateAll…}, variant=PRIMARY) }, status Text(Caliper.type.dataS), EndOfSheet().
- `app/src/debug/AndroidManifest.xml`: `<activity android:name=".ui.caliper.widget.PreviewStudioActivity" android:exported="false"/>` merged into app namespace.

### F8 — Generate + wire PNGs (device in loop)
```
./gradlew :app:installDebug
adb shell am start -n com.ivarna.deviceinsight.debug? NO — same appId; use: am start -n com.ivarna.deviceinsight/.ui.caliper.widget.PreviewStudioActivity  (verify actual applicationId from build.gradle first)
tap CAPTURE ALL (via android-mcp Click) → adb pull files/previews → cp 5 paper PNGs → app/src/main/res/drawable-nodpi/
repoint each *_info.xml android:previewImage → preview_<kind>_paper_280x140 (bench: _280x280)
```
Keep old webps (guard test may count them — verify `previewDrawablesExist` assertion before touching).

### F9 — Tests
Extend `BenchSelfCheckTest`: `tierExactBounds` (139→T1,140/141→T1/T2 edges,279/280→T2,350×280→T5,1000→T5), `previewSnapshotDeterministic` (sizes/hashes stable across two calls).

## File-Level Change Map
| File | Change | Rationale |
|---|---|---|
| BenchModel.kt | Tier.of tolerance removal | root cause #2/#1 sizing truth |
| BenchGlance.kt | Exact mode, InstrumentBody lift, wSp/maxLines, BandBitmap rewrite | roots #2,#3,#4,#5,#8 |
| res/layout/bench_initial.xml | new styled loading layout | root #7 |
| res/values{,-night}/colors.xml | bench_panel/ink60 tokens | root #7 visual continuity |
| res/xml/*.xml ×5 | initialLayout + resize bounds (+F8 previewImage) | roots #7,#1 |
| BenchPreviewGenerator.kt | real-pipeline capture | root #1 architecturally |
| src/debug/…PreviewStudioActivity + Manifest | on-device capture UI | root #1 workflow |
| drawable-nodpi/preview_*_paper_*.png ×5 | committed captures | root #1 deliverable |
| BenchSelfCheckTest.kt | +2 tests | regression guard |

## Testing Strategy
- Unit: `./gradlew :app:testDebugUnitTest --tests "*BenchSelfCheck*"` (new cases) + `--tests "*WidgetReceiversExist*"`.
- Build gates: `:app:assembleDebug` and `:app:assembleRelease` (release proves main-srcset compiles WITHOUT debug activity).
- On-device (realme X2 Pro via android-mcp): install, place SCOPE 2×2 & 4×2 & BENCH 4×2 from picker; screenshot placed vs pulled PNG overlay comparison; font_scale 1.3 via `adb shell settings put system font_scale 1.3` → relayout check; resize 2×2→4×3 → one-tick re-render; fresh-add shows CALIBRATING initial.

## Acceptance Criteria (objective, verifiable)
- [ ] A1 `:app:assembleDebug` + `:app:assembleRelease` pass.
- [ ] A2 Unit tests green (Tier boundaries + snapshot determinism + existing suites).
- [ ] A3 On device: placed widget background reaches all four edges; no launcher card; band bitmaps flush to hairline frame (no right-edge gap).
- [ ] A4 font_scale=1.3: no wrapped/clipped labels; text growth visibly damped vs before.
- [ ] A5 Resize 2×2→4×3: traces re-render at new width within one update tick; no stretched bitmap.
- [ ] A6 Fresh add: first frame is styled CALIBRATING (no spinner).
- [ ] A7 Picker preview PNG == placed widget at same nominal size (side-by-side screenshots attached to docs/testers report).
- [ ] A8 Five committed PNGs referenced by the five XMLs.

## Reviewer Corrections Folded In (plan-review PASS1 APPROVE)
- **C1** F9 exact-bound expectations: `139→T1, 140×140→T1, 279×140→T1, 280×140→T2, 280×209→T2, 280×210→T3, 349×280→T4, 350×280→T5, 1000×400→T5`.
- **C2** Dispatcher param is `awId` everywhere (feeds `openConfig`); no `stateKeyHint` param — state keys stay internal to bodies as today.
- **C3** F8 extraction uses app-internal filesDir ⇒ pull via `adb exec-out run-as com.ivarna.deviceinsight tar -cf - files/previews` (or have generator ALSO copy to `getExternalFilesDir` when available; prefer run-as — deterministic).
- Suggestion adopted: split-trace width `(W−44−6)/2` (=W/2−25) for exactness.
- Suggestion noted: maxResize* attrs are API31+; harmless below.
- Note: `BenchConfigActivity.kt:423` already fakes a demo snapshot — `previewSnapshot()` is its widget-domain sibling; keep both (different shapes).

## Risks & Mitigations (NEW_RISKS)
- **GlanceRemoteViews compose on API < 28 quirk**: device is API-level unknown; if compose crashes (<P), fall back to capturing at device density (still real pipeline). Log + manifest note.
- **`previewDrawablesExist` guard test may require ≥N webps** — do not delete webps; only add PNGs.
- **Realme ColorOS picker caches previews** — after reinstall, force-stop launcher (`adb shell pm reset <launcher>` or reboot) before A7 comparison.
- **Exact-mode resize recomposition latency** — Glance re-runs provideGlance on resize; BandBitmap sync render on main thread could jank one frame; acceptable (existing behavior), noted.
- **debug appId suffix** may exist (check `build.gradle.kts` `applicationIdSuffix`); am start must use released applicationId with exported=false → use `adb shell am start -n` which works regardless of exported for shell.
- Official docs page fetch timed out; compensated by javap of the exact pinned artifacts (stronger than prose docs).

## Handoff to Plan Reviewer
Validate: (1) D2 inset math against BenchGlance.kt:204-218; (2) SizeMode.Exact claim (Glance re-invokes provideGlance with new LocalSize on resize); (3) GlanceRemoteViews signature usage compiles against 1.1.0; (4) InstrumentBody lift preserves every existing branch (not-fitted/locked/consumers/T-tiering); (5) F8 feasibility given attached device + guard test constraints; (6) no public API breaks for receivers/tests.
