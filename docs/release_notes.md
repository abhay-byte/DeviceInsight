# DeviceInsight Release Notes

## Version 1.0.5 (Build 6) — Truthful FPS HUD & Correct Display Refresh

### 📊 FPS HUD — Truthful Signal, Not 0/—

- **SurfaceFlinger real frametime** — Fixed `SurfaceFlingerFpsDataSource.kt:118-162` to measure `(frameComplete - frameStart)/1e6` avg → `1000/avgMs` and `ceil(frameTime/refreshPeriod)` jank, replaced `System.nanoTime()-1s` math and `10Hz` dumpsys storm with `1Hz` `Dispatchers.IO` ticker (`OverlayService.kt:1Hz` → `MonitorBus` → `HudFast` → `HudPanel`). `parseLayerName` now strips `RequestedLayerState{ pkg/SurfaceView ... }` hex handle + `parentId/z` (Android 15+) and matches short pkg (`ForegroundAppResolver.kt` sanitize `null`).
- **Layer selection + cache** — Prefers `SurfaceView/NativeActivity/Vulkan/GLSurfaceView/#` and filters `ActivityRecord/InputSink`; short `800ms` cache invalidated on package change/latency empty (was `30s` brittle). Package change clears `lastGood` so old game's FPS never leaks to new app, median-3 smoothing for display, `LAST_GOOD_HOLD 3500ms`.
- **Routing honest by surface** — `FpsRepository.kt:55-92` games `SF` only (never `gfxinfo` which measures UI), UI `SF → gfxinfo FrameCompleted deltas (with header by name + package reset) → histogram`. `DMA` deferred (Adreno `dma_fence` inflates). `FpsMethod.SURFACEFLINGER/GFXINFO/DMA_FENCE/DISPLAY/NONE` with labels `SF/GFX/DMA/REF/—`, `FpsMonitor.kt:33-41` mapping.
- **Privilege chain AUTO fixed** — `PrivilegePolicy.kt` `ROOT (su, cached hasSu) → SHIZUKU UserService IShellService.aidl` (not reflection) `→ STANDARD`; `ShellGateway.kt` cached `hasSu`, `ShellUserService.kt` binds `ShizukuAccess.kt` permission. `ForegroundAppResolver.kt:22-54` `resolve()` `mCurrentFocus|mFocusedApp` → `ResumedActivity` → `UsageStatsManager` (10s window), `pidOf` + `refreshRateHz`, self-filter, `extractPackage()` sanitize `"null"`.

### 🖥️ STANDARD Mode — Honest REF Instead of Fake 60

- **Fallback for unprivileged** — `FpsRepository.kt:82-92` when `raw==null && !isGame` ( `dumpsys window|SurfaceFlinger --list --latency|gfxinfo|display` blocked `Permission Denial` for `u0_aXXX` on Android 14/15/16, only `shell uid2000` can dump) shows `FpsSnapshot(DISPLAY=REF, STANDARD)` with `foreground.refreshRateHz ?: getDisplayRefreshHz() ?: 60` coerced `1..240`. Games intentionally keep `—` (no fake `REF`). `FpsMonitor` logs `FPS 90 REF pkg=chrome access=STANDARD`.
- **Foreground UsageStats + correct panel Hz** — `ForegroundAppResolver.kt:30-56` `readFromUsageStats()` filtered self, `readActiveRenderFrameRate()` now `DisplayManager.getDisplay(DEFAULT_DISPLAY).mode.refreshRate` + `supportedModes` active `modeId` lookup → `max`, then `WindowManager.display.refreshRate`, then privileged `dumpsys display mActiveRenderFrameRate/renderFrameRate` (blocked in STANDARD) else `60`. Previously `WindowManager` returned app bucket `normal=60` (`FrameRateCategoryRate`) so `realme X2 Pro` panel `90Hz` (`modeId2 90` `mActiveRenderFrameRate=90`) showed `60 REF`; now correctly shows `90 REF` (and `60 REF` when panel `modeId1 60`). Verified `logcat FPS 90 REF` on `RMX1931` Chrome/Firefox vs `—` before.
- **Tests & F-Droid** — `FpsRepositoryTest.kt` (8→9, `DISPLAY` fallback), `ForegroundAppResolverTest.kt` generic `executePolicy(any(),any())`, `Gfxinfo` package reset, `SurfaceFlinger` fixtures, `ShellGatewayIntegrationTest` 5, `PrivilegePolicyTest` 7, `mockk:1.13.10`, `aidl=true`, `lintDebug BUILD SUCCESSFUL`.

### 🔧 Build

- **Version bump** — `versionCode 5 → 6`, `versionName 1.0.4 → 1.0.5` in `app/build.gradle.kts` + `com.ivarna.deviceinsight.yml` (`CurrentVersion 1.0.5 / CurrentVersionCode 6`). `SettingsScreen.kt:277` still `${BuildConfig.VERSION_NAME} · Build ${BuildConfig.VERSION_CODE}` dynamic — no hardcoded UI strings.
- **Artifacts** — `bundleRelease` R8 minify + `shrinkResources` `AAB` + `assembleRelease` `APK` signed via `../keys/deviceinsight-key.properties` (`storeFile`), tested `adb install -r` on `RMX1931 realme X2 Pro` (`Android 16 90Hz` `mActiveRenderFrameRate=90`) and `OPD2403` (`120Hz`): UI `90 REF` STANDARD, no crash on missing `Shizuku`/`su`, `01 USAGE` + `Chrome` foreground verified.

---

## Version 1.0.4 (Build 5) — Connected Calibration, Overlay Preview & Navigation Integrity

### 📖 Calibration — Connected Pages, No REV

- **Restored full 6-page wizard with swipe** — Reverted 2-page `Welcome → Setup` simplification (`832cfcc`/`06e7cd3`) back to `4ba82ab` full sheet set: `00 Cover`, `01 USAGE ACCESS`, `02 CAMERA`, `03 OVERLAY`, `04 ROOT PROBE`, `Certificate`. Pages are now `HorizontalPager(rememberPagerState(pageCount={6}))` with `beyondViewportPageCount=1` — swipe left/right connects every sheet; `HardKey` callbacks `scope.launch { pager.animateScrollToPage(n) }` also animate.
- **Numbering follows Caliper design** — Bottom row `00 01 02 03 04` with `✓` for completed (`i < current` or `current>=5` all ✓), `ink` for active/visited vs `ink40` for pending, exactly as `4ba82ab` progress row. Pager `currentPage` drives it; no separate `step` state.
- **REV/DOC removed** — Deleted `DOC № DI-0001 · REV 2.0` from `CoverSheet` (top) and `CertificateSheet` (`DeviceInsight` + date now only) and changed `INSTRUMENT · CALIPER  REV A` panel to `INSTRUMENT · CALIPER` per request. No repository revision shown in onboarding.
- **Cover hint** — Added `swipe to navigate →` `meta` `ink40` hint under calibration note.

### 🖥️ Overlay Preview — No More Clipping

- **ScaledPreview fix** — `OverlayScreen.kt:288` custom `Layout` now measures HUD at true size (`Constraints.Infinity`) then `placeWithLayer(0,0){ scaleX/Y + TransformOrigin(0f,0f) }` with `0f,0f` top-left origin. Previously default center-origin shifted scaled `L` (340dp) at narrow width (e.g. 324dp inner → 0.95 scale) by `8.5dp` causing right-edge clipping against outer `clipToBounds`. Now visual aligns to layout `scaledWidth/Height` without shift.
- **Single source of truth** — `HudPreviewHost` `hudWidth = HudScales.of(scale).widthDp.dp` (156/252/340 for S/M/L) + `frameWidth = min(desired, available)` + `innerAvailable = frameWidth - 16dp`; scaleFactor coerced `0.1..1f`. No duplicated constants.

### 🧭 Navigation — Shared Rail Logic

- **Extracted `selectRailTab`** — `SystemStatsApp.kt:96` `internal fun NavController.selectRailTab(route: ScreenRoute)` implements: `isAlreadyOnRoot` no-op else `isInSameGraph` → `popBackStack(route,false)` with fallback `navigate { popUpTo(route){inclusive=false} launchSingleTop }` else `navigateToTopLevel`. `ModeRail.onSelect` now single call `navController.selectRailTab(route)` instead of duplicated hierarchy checks.
- **Regression test now exercises production branch** — `NavigationRegressionTest.kt` `selectTab` delegates to `navController.selectRailTab`; `overviewReselectionFromMemoryReturnsToOverviewRoot()` now `navController.navigate("memory")` → `selectTab(Dashboard)` asserts same-graph `popBackStack` to `dashboard` and second `selectTab(Dashboard)` no-op. Previously directly called `navigateToTopLevel` and never hit `popBackStack` branch.
- **Bottom rail icons restored** — `CaliperChrome.kt:142` `ModeRail` horizontal/vertical lost `NumberKeyBox` (18dp numbered square `1-4`). Restored: horizontal `Row{ NumberKeyBox(n,sel); if(warning) LedDot }` + `Spacer(4.dp)` + `Text(label)`; vertical `Row{ NumberKeyBox; Spacer(10.dp); Text }`. `contentDescription` now `[n] label` for a11y. Verified on `RMX1931` bottom bar: `1 OVERVIEW` filled ink, `2 DEVICE`, `3 OVERLAY`, `4 PROCESSES`.

### 🔐 Settings Wording — Optional ≠ Required

- **Header** `SettingsScreen.kt:175` `ALL GRANTED/REQUIRED` → `ALL ENABLED / SETUP AVAILABLE` (both `ink60`, no `fault` red for optional). 
- **Rows** `SpecRow` `REQUIRED` → `NOT GRANTED` for `usage/overlay/camera` to match `CalibrationSheet` `NOT GRANTED`/`GRANTED` stamps. `Overlay`/`Camera` are optional; presenting as `REQUIRED` was misleading.

### 🔧 Lint & Build

- **4 pre-existing lint errors fixed** — `MissingPermission` `Build.getSerial` (`DeviceProvider.kt:193` `SuppressLint`), `NewApi` `Tile.subtitle` `API 29` (`MediaTileService.kt:46` `SDK>=Q` guard), `NewApi` `wifiStandard` `API 30` (`NetworkProvider.kt:135` `SDK<R` return), `PermissionImpliesUnsupportedChromeOsHardware` (`AndroidManifest.xml:15` `<uses-feature hardware.camera required=false>`). `lintDebug` now `0` errors.
- **Version bump** — `versionCode 4 → 5`, `versionName 1.0.3 → 1.0.4` in `app/build.gradle.kts` and `com.ivarna.deviceinsight.yml` (`CurrentVersion 1.0.4 / CurrentVersionCode 5`). `SettingsScreen` still uses `BuildConfig.VERSION_NAME · Build CODE` dynamically — no hardcoded strings.
- **Artifacts** — `bundleRelease` (R8 minify + shrinkResources) `AAB` + `assembleRelease` `APK` signed via `../keys/deviceinsight-release.jks` (`storeFile deviceinsight-release.jks`), tested `adb uninstall/install` on `RMX1931` (realme X2 Pro) and verified swipe, overlay `L` at narrow width, `OVERVIEW → memory → OVERVIEW` reselection.

---

## Version 1.0.3 (Build 4) — CALIPER Calibration, Camera & Thermal Integrity

### 📖 Cover & First-Launch
- **New 00 · Device Insights cover** — Paper drafting sheet at the very front with CALIPER spec logo (96 dp paper square 2dp radius, ink crosshair, 3dp accent dot, 24dp inner hairline), `Device` / `Insights.` serif 44sp, `DEVICE  INSIGHTS` mono caps and `INSTRUMENT · CALIPER REV A` panel. `BEGIN CALIBRATION` HardKey (ink fill) + 2-min optional note. First thing users see on fresh install — establishes Paper language before any permission.
- **Forced Paper theme for entire calibration** — `SystemStatsApp` now wraps `CalibrationScreen` in `CaliperTheme(Medium.PAPER)`. Previously only `05 MEDIA` was light while `01-04` were Carbon (dark) on dark system. Now `00-04 + Certificate` are all warm paper `#F4F1E8 / #FBF9F3` with ink hairlines, graph-paper grid and correct status-bar icons. `certificate` still shows `CALIBRATED · DI-0001` stamp.
- **Fixed first-launch gate** — `caliperMigratedFlow` `initialValue true` + `remember(!migrated)` never updated, so fresh installs stayed on Overview and never showed calibration. Added `LaunchedEffect(migrated){ if(!migrated) showCalibration=true }` — now correctly appears on first launch after `uninstall`/`clear`, and not for already-migrated users.
- **Media step removed** — User reported media not used why asked. Removed `05 · MEDIA` (Paper/Carbon/Blueprint picker). Calibration now `00 COVER → 01 USAGE → 02 CAMERA → 03 OVERLAY → 04 ROOT → Certificate` (4 numbered permission sheets + cover). Default medium is now `PAPER` persisted via `settingsViewModel.setMedium(PAPER)` on entry and on finish; no user choice required, but `Settings → Presentation` still allows switching.

### 🔐 Permissions — Never Auto-Popup, Always Explicit
- **Removed auto `CAMERA` request on launch** — `MainActivity.requestPermissions()` deleted (`Manifest.permission.CAMERA` no longer requested in `onCreate`). The purple system dialog from the screenshot now never appears automatically. All permissions are gated to an explicit `HardKey` tap.
- **01 · USAGE ACCESS — now actually grants** — Was `CalibSheet` with dummy `step=1`. Now `UsageAccessSheet` checks `AppOpsManager OPSTR_GET_USAGE_STATS`, shows `FIG. 1 — USAGE PATH` with `LOCKED/GRANTED` stamp, dotted hatch preview, `GRANT USAGE ACCESS` → `Settings.ACTION_USAGE_ACCESS_SETTINGS` (new task) and `onResume` re-checks via `LifecycleEventObserver`. `CONTINUE → CAMERA` appears when granted; `SKIP (≈ ESTIMATES)` shows `≈` ledger. Hatch until granted.
- **02 · CAMERA — CALIPER gate** — `CameraCalibSheet` with `ActivityResultContracts.RequestPermission` only on `GRANT CAMERA` tap. Shows `FIG.1 CAMERA ROSTER` with `LOCKED/GRANTED`, `CAM · hardware roster` vs dotted hatch, `NOTE 002` rationale, `SKIP — LIMITED ROSTER` and `OPEN SYSTEM SETTINGS` on permanent deny. Never auto-launched.
- **03 · OVERLAY — now actually asks** — Was `NEXT → OVERLAY` no-op. Now `OverlaySheet` checks `Settings.canDrawOverlays()`, shows `FIG. 2 — HUD OVERLAY` with hatch, `GRANT OVERLAY` → `ACTION_MANAGE_OVERLAY_PERMISSION` (`package:` URI) and `onResume` re-check. Text clarifies *system sheet appears only when you tap GRANT. No auto popup.* `CONTINUE → ROOT` when granted.
- **Settings → 02 PERMISSIONS small card** — New `PanelCard CAMERA · ROSTER LOCKED` in `SettingsScreen` (only when `CAMERA` not granted) with `12dp` dotted hatch, `NOTE 002`, `GRANT CAMERA` (only place that opens system popup) and `OPEN SYSTEM SETTINGS` on permanent deny. Plus `onResume` re-check.
- **Hardware roster locked state** — `DevicesTab` now shows `CAMERAS · CHANNEL LOCKED` hatch with `LOCKED` fault stamp, `GRANT CAMERA` + `OPEN SYSTEM SETTINGS` when permission denied, and auto-reloads `HardwareViewModel.loadHardwareInfo()` on grant (via `onCameraGrantedReload` propagated through `HardwareScreen → DossierPage`).

### 📸 Camera — 10 → 4, No More False Info
- **HAL duplicate dedup** — RMX1931 (realme X2 Pro) `dumpsys media.camera` reports `10` HAL devices (5× `4608×3456` same sensor, 2× `3264×2448` etc.) with identical focal. Added `seen` set keyed by `facing|maxSize|focal` (e.g. `1|4608x3456|5.58`) — keeps first of each physical module, filters 5 duplicates to 1. Logs `dedup $id key=$key` for diagnostics.
- **Tiny auxiliary filter** — Back cameras with `maxArea < 2_000_000` (1.9 MP `1600×1200` depth on RMX1931) filtered with `filter small back id=$id` — user expects `3 back + 1 front = 4`, not `10`. Front kept even if small. After filter + dedup, `MISC 4 cameras · 2 USB` in lightbox, verified on device.
- **Display name fix** — `facingCounts` increment now post-filter/dedup, so `Rear-Facing Camera #2` etc. are sequential without gaps.
- **Provider gated** — `CameraProvider.getCameraInfo()` returns `emptyList()` early if `CAMERA` not granted — UI renders `CHANNEL LOCKED` hatch, never triggers system popup.

### 🌡️ Thermal — 596°C / -274°C → Real Silicon
- **Bogus filter** — `ThermalProvider` reads `/sys/class/thermal/thermal_zone*/temp` (milli-C) but HAL exposes -274°C (absolute zero) and 588-618°C `pa-therm2` spikes that skew `PEAK/AVG/MIN` to `596°C / 41°C / -274°C` and force `CRITICAL`. Now filters `temp in -20..110` (real phone silicon range) and logs `filter bogus $type=$temp°C raw=$tempRaw`. `72 → 65` sensors, `PEAK 596°C → 92°C`, `AVG 41°C → 49°C`, `MIN -274°C → 4°C`, `595.8°C` zone text fixed. `ThermalHeader` `CRITICAL/WARM/NORMAL/COOL` and `ThermalGauge` ramp now honest.

### 🎨 CALIPER Details
- All calibration sheets use `0dp` radius, `1dp` hairline, `1dp` dotted leaders, `PanelCard` + `StampBadge -3°`, `HardKey` 48dp with `0.98` press, `graph-paper` 24dp minor / 120dp major at `ink 3%/5%`, `MarginNote NOTE 002`, `LedDot`, `CaliperMotion.Ease` `0.2,0,0,1`. Paper surface `#F4F1E8`, panel `#FBF9F3`, ink `#191713`, accent never for data.

### 📦 Build & Release
- **Version bump** — `versionCode 3 → 4`, `versionName 1.0.2 → 1.0.3` in `app/build.gradle.kts` and `com.ivarna.deviceinsight.yml` (`CurrentVersion 1.0.3 / CurrentVersionCode 4`). UI in code uses `BuildConfig.VERSION_NAME` via `Paper` defaults; no hardcoded strings left.
- **Release artifacts** — `assembleRelease --no-daemon` (R8 minify + shrinkResources) now `11M` `app-release.apk` signed via `../keys/deviceinsight-release.jks`. Installed on RMX1931 + duchamp via `adb uninstall/install`.
- **Gradle hygiene** — `gradlew --stop` + `pkill kotlin` after release as requested; `clean` before debug verification.

---

## Version 1.0.2 (Build 3) — CALIPER Dossier Polish & Flat Rendering

### 🎯 CALIPER Redesign
- **Battery (CH-04 POWER)**: rebuilt as drafting-paper fuel sheet — `LinearGauge` with needle spring, voltage/temp Odometer, `ThermalGauge` ramp and dotted-leader spec rows. Fixes Health/voltage truncation from gradient overflow.
- **Hardware — Cameras & USB (MISC)**: flat `PanelCard` inventory with `FIG.1` caption, `SpecRow` dotted leaders and aerospace `FeatureRow` (`● FITTED/○ NOT FITTED`), focus modes as hairline chips. No Brush/Gradient overdraw.
- **Thermal**: `ThermalHeader` with `THERMAL · SENSORS` status stamp and ramp gauge, `StatBadge` max/avg/min, per-sensor `PanelCard` + 6 dp hairline ramp bar (amber→vermilion→fault via `Channels`), memoized grouping.
- **Sensors**: `SENSORS · SUITE` header with `FP` stamp, flat category summary, grouped ledger with `SpecRow` per sensor and `Mono` powder. Removed `CircleShape`/`Brush` CIF.
- **Storage (CH-05)**: `HatchBar` allocation map (`SOLID` used / `DOTS` free) with legend, `CH-05 · STORAGE` `PanelCard`s for internal/external, dotted directory/mount spec rows.

### ⚡ Optimizations
- Removed all `Brush.linear/verticalGradient` shader allocs; flat `panel/hairline` fills → lower GPU overdraw, 88/10/2 ink ratio compliant.
- Memoized `voltage/temp` formatters, `groupBy` and `fraction` via `remember`; `Arrangement.spacedBy(12.dp)`; tabular `tnum` throughout.
- Eliminated 56 dp top / 160 dp bottom spacers; outer `verticalScroll` + `EndOfSheet` handles dossier paging.

---

## Version 1.0.1 (Build 2) — Performance & Smoothness Update

### 🚀 Highlights & Performance Improvements
- **Zero-Stutter Tab Switching**: Eliminated navigation FPS drops and UI freezes when transitioning between Dashboard, Tasks, Hardware, and Overlay screens.
- **Static Hardware Telemetry Caching**:
  - Camera details, sensor inventory, device build metadata, filesystem mount points, and CPU architecture are now cached in-memory after initial read, completely eliminating repeated heavy Binder IPC and filesystem queries during app usage.
  - EGL context creation for OpenGL and Vulkan GPU specs is now performed once and cached.
- **Main Thread Offloading**:
  - All hardware querying and periodic monitoring routines now execute strictly on `Dispatchers.IO`, preventing main UI thread blocking.
- **Optimized Glassmorphism & GPU Rendering**:
  - Tuned Haze glassmorphic effects for maximum framerate by eliminating per-pixel noise shader passes and optimizing blur radii.
  - Replaced software CPU rasterization filters (`BlurMaskFilter`) in gauges with hardware-accelerated drawing pipelines.
  - Added lightweight, fast crossfade transitions for Jetpack Compose Navigation.
- **Tasks Screen Enhancements**:
  - Replaced legacy `AndroidView` embedded views in `LazyColumn` with native Compose `AsyncImage` loaders for smoother list scrolling and instant tab rendering.

### 🎨 UI & Design Enhancements
- Updated high-resolution application branding and icon assets across all screen densities.
- Enhanced bottom glass navigation bar with refined indicator glow and smooth tab selection animations.
- Polished Floating HUD overlay toggle buttons and permission guidance workflows.

---

## Version 1.0.0 (Build 1) — Initial Release

- Initial public release of DeviceInsight featuring Material 3 Glassmorphism UI.
- Real-time telemetry dashboard covering CPU, GPU, RAM, Storage, Battery, and Network bandwidth.
- Comprehensive hardware inspector (System, CPU, Display, GPU, Network, Battery, Android, Thermal, Storage, Sensors).
- Tasks monitor with foreground usage tracking.
- Floating overlay telemetry HUD for real-time monitoring inside games and benchmarks.
- Home screen widgets support (via Jetpack Glance).
