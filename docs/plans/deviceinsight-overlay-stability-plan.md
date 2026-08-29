# Plan: DeviceInsight Overlay Blur, Theme, and Startup Stability

## Task Summary

Stabilize floating HUD startup, geometry, blur, theme, typed FPS settings, and lifecycle. Preserve `minSdk 26`, `targetSdk 36`, `TYPE_APPLICATION_OVERLAY`, special-use foreground service, telemetry, drag/lock/tap behavior, existing DataStore keys, Paper/Carbon/Blueprint media, and no screen capture or full-screen overlay.

## Research Sources

- `<source: app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt:111-205,228-303,320-388>` — current raw `WindowManager` attach, `WRAP_CONTENT` plus `FLAG_LAYOUT_NO_LIMITS`, raw position writes, swallowed failures, Compose-owned blur listener, and teardown.
- `<source: app/src/main/java/com/ivarna/deviceinsight/ui/caliper/CaliperPrefs.kt:17-35,69-114>` — sole `caliper` DataStore, exact keys/types/defaults, and legacy string FPS storage.
- `<source: app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudModel.kt:7-29>` — current HUD config and free-form `fpsMode` string/parser.
- `<source: app/src/main/java/com/ivarna/deviceinsight/presentation/overlay/OverlayViewModel.kt:43-100,150-160>` — current presentation-only `FpsMode { AUTO, ROOT, SHIZUKU }`, duplicate config parser, and persistence.
- `<source: app/src/main/java/com/ivarna/deviceinsight/data/monitor/HudSettingsCache.kt:15-35>` — raw string cache with no initial snapshot guarantee and empty catch.
- `<source: app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsRepository.kt:18-36>` and `<source: app/src/main/java/com/ivarna/deviceinsight/data/fps/privilege/ShellGateway.kt:14-29>` — FPS consumer and string-to-`PrivilegeMode` bridge.
- `<source: app/src/main/java/com/ivarna/deviceinsight/SystemStatsApplication.kt:30-90>` — asynchronous migration and defaults.
- `<source: app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudTheme.kt:17-129>` and `<source: app/src/main/java/com/ivarna/deviceinsight/ui/caliper/CaliperTheme.kt:154-205>` — separate HUD/app theme authorities.
- `<source: app/src/main/AndroidManifest.xml:92-100>` — exported=false special-use FGS declaration.
- `<source: app/build.gradle.kts:46-55,107-168>` — SDK, Compose, DataStore, coroutines, Robolectric, and test versions.
- `<source: app/src/test/java/com/ivarna/deviceinsight/CaliperSelfCheckTest.kt:73-93>` — one-DataStore and exact-key invariant.
- `<source: https://developer.android.com/reference/android/view/Window#setBackgroundBlurRadius(int)>` — official fetch timed out; compile-SDK-36 verification required.
- `<source: https://developer.android.com/develop/ui/views/layout/blur>` — official blur guide fetch timed out; runtime behavior remains unverified.
- `<source: https://github.com/search?q=TYPE_APPLICATION_OVERLAY+Dialog+setBackgroundBlurRadius&type=code>` — GitHub code search requires authentication; no implementation copied.
- `<source: repo: /tmp/opencode/android-platform-samples>` — cloned platform samples; no matching bounded overlay-blur sample found.
- `<source: command: npx ctx7@latest library "AndroidX" "WindowManager overlay Dialog lifecycle and layout params">` — `fetch failed`; API signatures require SDK-36 compile proof.

## Current Architecture

`OverlayService` sets `isRunning` and lifecycle RESUMED before attach, creates `ComposeView`, then calls raw `WindowManager.addView`; failures are swallowed. Config and position arrive through separate flows after attach. `FLAG_LAYOUT_NO_LIMITS` and raw coordinates make bounds undefined (`OverlayService.kt:111-148,172-205,228-275`).

`CaliperPrefs` owns storage, but `HudModel`, `OverlayViewModel`, `HudSettingsCache`, and `SystemStatsApplication` duplicate defaults/parsing. Presentation owns `FpsMode`, while data consumers use strings (`CaliperPrefs.kt:71-113`, `OverlayViewModel.kt:43`, `HudSettingsCache.kt:18-29`, `FpsRepository.kt:33-36`).

## Implementation Steps

1. **Instrument before changing behavior.** Log service start, permission checks, snapshot read, Dialog construction/show, attach/update/dismiss, listener registration/removal, and destroy. Log API/device, permission, bounds, measured size, requested/clamped coordinates, flags, and exception class/message/stack. Never use empty catches. Reproduce fresh install, non-default persisted state, revoked permission, duplicate starts, rotation/density change, screen off/on, background/foreground, and 20 force-stop/start cycles. Do not name crash root cause without captured evidence.

2. **Create one validated runtime snapshot.** Add smallest shared `HudRuntimeConfig`/parser beside HUD model. Fields: `HudMedium`, `HudScale`, opacity, blur, lock, modules, core-bank, raw persisted `x/y`, and canonical `com.ivarna.deviceinsight.data.fps.model.FpsMode`. Parser validates integer syntax/range representability only; it does **not** clamp screen coordinates because parser has no display or measured-window inputs. Defaults match `CaliperPrefs` exactly, including `hudModules` containing `NETWORK`; invalid enum/CSV/opacity values use safe defaults. Preserve `CaliperKeys` and migration. `OverlayService`, `OverlayViewModel`, and cache consume same parser; remove duplicate defaults only with migration tests.

3. **Canonical typed FPS path.** Move/define `FpsMode` in non-presentation package `com.ivarna.deviceinsight.data.fps.model` with exact values `AUTO`, `ROOT`, `SHIZUKU`; `OverlayViewModel.FpsMode` is deleted and `OverlayUiState.fpsMode` uses canonical type. Persist `CaliperKeys.fpsMode` as exact enum `.name`; parser maps null, malformed, or legacy unknown strings to `AUTO`; no write occurs merely because fallback happened. `OverlayViewModel.loadInitialState` and live UI updates use parsed snapshot; setter writes enum name. `HudSettingsCache` stores `@Volatile var fpsMode: FpsMode = FpsMode.AUTO`, initializes from `caliperDataStore.data.first()` through parser before live collection, and exposes typed `setImmediate`. `FpsRepository.getFps` reads typed cache. `ShellGateway` accepts typed `setMode(mode: FpsMode)` and maps `AUTO/ROOT/SHIZUKU` to existing `PrivilegeMode`; retain `setModeFromString` only if migration callers require it, delegating through parser. Tests cover initial and live paths, malformed storage, and cache-to-repository-to-gateway mapping.

4. **Preload one snapshot before visible attach.** Read `caliperDataStore.data.first()` on IO, parse once, initialize config/FPS input/raw position before host attach; no `runBlocking`. Read failure logs stack and uses safe defaults. Related reset/migration writes use one `DataStore.edit` transaction. Independent controls may remain debounced, but each complete store emission becomes one mixed-revision snapshot.

5. **Define geometry contract in `OverlayWindowHost`.** Host, not parser, owns display geometry. Obtain current usable display frame from current window metrics/display bounds and subtract system-bar insets where applicable; use physical pixel coordinates in `TOP|START` gravity. Measure/layout `WRAP_CONTENT` HUD content before first visible frame, obtain measured width/height, then clamp requested persisted `x/y` to `[usable.left, usable.right - measuredWidth]` and `[usable.top, usable.bottom - measuredHeight]` (when range collapses, use usable edge). Negative and oversized legacy values clamp to nearest edge. Initial attach uses clamped coordinates before `show`/first visible frame, so no post-show correction jump. Drag computes candidate coordinates, clamps against current measured geometry, updates window, and debounced-persists the clamped values. Orientation, density, insets, or size changes remeasure and re-clamp; persist only when clamping changed coordinates, using debounced writes. External stored x/y updates follow same host clamp path. Tests cover zero/normal bounds, negative/oversized values, exact edge, orientation/density, measured HUD larger than frame, and write-back/no-write-back policy.

6. **Use Dialog window as bounded blur owner.** `OverlayWindowHost` owns one `Dialog` created with overlay-compatible context/theme. Before show: request no title, transparent window background/decor, no dim (`clearFlags(FLAG_DIM_BEHIND)`), `WRAP_CONTENT` width/height, `TOP|START` gravity, `TYPE_APPLICATION_OVERLAY`, `PixelFormat.TRANSLUCENT`, and baseline `FLAG_NOT_FOCUSABLE` plus required layout flags. Deliberately do not use `FLAG_BLUR_BEHIND`, `LayoutParams.blurBehindRadius`, or `MATCH_PARENT`. Install Compose content and lifecycle/saved-state owners on Dialog content; retain `DisposeOnDetachedFromWindow`. Measure and clamp before first visible frame, then show/attach. Set `isRunning` only after successful show/attach. Dismiss before destroying composition/owners; make detach idempotent and safe after partial show.

7. **Preserve interaction through host attributes.** Host computes `FLAG_NOT_TOUCHABLE` from lock state: locked means pass-through; unlocked removes it for drag and tap-to-config. On lock, blur, scale, geometry, or configuration changes update a copied `dialog.window.attributes` object, preserving type/gravity/size/position, then call `setAttributes`/relayout. Re-measure and re-clamp after scale, orientation, density, or inset change. Update failures log stack and trigger clean stop only when window is unusable. Dialog show/attach, update, and dismiss are serialized on Main; duplicate attach is idempotent.

8. **Move blur capability out of Compose.** On API 31+, host registers exactly one `addCrossWindowBlurEnabledListener`, removes it exactly once on detach, and applies `Window.setBackgroundBlurRadius(int)` only when persisted blur is enabled and capability is true. Radius constant and API gate require compile-SDK-36 proof. Unsupported API 26–30, disabled capability, exception, or OEM failure means no blur, stronger scrim, unchanged persisted preference, service remains alive. Compose receives only `blurSupported/effectiveOpacity`; it never mutates window flags or registers listeners. Toggle updates host live without restart. Device screenshot must show outside text sharp; if not, mark bounded blur unsupported and ship fallback, never display-wide blur.

9. **Unify theme authority and cold-start rendering.** Keep one palette/metrics mapping for HUD, floating HUD, preview, and app preview; preserve independent HUD/app persisted keys. Paper/Carbon/Blueprint changes update HUD live. MainActivity gates first content on first resolved app medium, uses deliberate `Medium.PAPER` fallback for absent/invalid/error, then renders `SystemStatsTheme`/`SystemStatsApp`; later changes remain live. Do not overwrite persisted values during fallback. Test no system-themed frame before resolution and no flash.

10. **Serialize service lifecycle.** Define Main-thread `STOPPED -> STARTING -> RUNNING -> STOPPING -> STOPPED`. Start FGS immediately in `onStartCommand`; one startup Job; duplicate STARTING reuses it and RUNNING is idempotent. Check active state, not stopping/destroyed, and `Settings.canDrawOverlays` immediately before show. Advance lifecycle to RESUMED only after attach. On failure/cancel/destroy, cancel collectors, dispose content, destroy saved-state/lifecycle owners once, remove Dialog once, clear references, set `isRunning=false` before detach, and log failures. Preserve FGS contract, permission stop, telemetry, FPS ticker, drag, lock, tap, reset, and launcher-alias cleanup.

11. **Tests and validation.** Add focused parser/FPS/geometry/host/lifecycle tests, Compose theme and scrim tests, DataStore migration tests, and scoped source scans. Forbid production overlay blur flags, display-wide Compose blur, screen capture, service `runBlocking`, empty catches, and unsafe overlay `MATCH_PARENT`. Run `./gradlew :app:testDebugUnitTest`, `./gradlew :app:assembleDebug`, `./gradlew :app:lintDebug`; run `./gradlew --stop` after each command. Then manual API 26–30, API 31+ capable/disabled, and OEM-failure matrix with logs, screenshots, interaction checks, and 20 cycles.

## File-Level Change Map

| File | Change | Reason |
|---|---|---|
| `app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt` | Thin lifecycle/FGS orchestration, snapshot preload, logging, feeds | Remove race and raw window ownership |
| `app/src/main/java/com/ivarna/deviceinsight/service/OverlayWindowHost.kt` | New Dialog, geometry, flags, blur listener, relayout, idempotent attach/detach | Single window contract |
| `app/src/main/java/com/ivarna/deviceinsight/ui/caliper/hud/HudModel.kt` or new `HudRuntimeConfig.kt` | Shared parser/defaults and raw-coordinate snapshot | Remove duplicate config parsing |
| `app/src/main/java/com/ivarna/deviceinsight/data/fps/model/FpsMode.kt` | Canonical `AUTO/ROOT/SHIZUKU` enum | Non-presentation ownership |
| `CaliperPrefs.kt`, `OverlayViewModel.kt`, `HudSettingsCache.kt`, `FpsRepository.kt`, `ShellGateway.kt` | Typed FPS conversion and shared snapshot consumers | Eliminate string mismatch |
| `CaliperTheme.kt`, `HudTheme.kt`, `SystemStatsApp.kt`, `MainActivity.kt` | One theme mapping and first-frame gate | No theme flash/parity drift |
| `app/src/test/java/com/ivarna/deviceinsight/...` | Minimum parser, geometry, host, FPS, lifecycle, theme, scan tests | Objective regression coverage |

## Acceptance Criteria

- No production HUD `FLAG_BLUR_BEHIND`, `LayoutParams.blurBehindRadius`, display-wide blur, screen capture, or overlay `MATCH_PARENT`.
- API 31+ blur applies only to measured Dialog bounds; outside text remains sharp in screenshot. Unsupported/failing blur uses stronger scrim, no crash, preference unchanged.
- Parser preserves raw persisted x/y; host clamps against current usable frame after measured content size, before first visible frame. Drag persists clamped coordinates. Reconfiguration re-clamps and persists only adjusted values.
- One canonical non-presentation `FpsMode` has exact `AUTO/ROOT/SHIZUKU` values. Null/unknown storage becomes `AUTO`; initial and live cache, VM, repository, and ShellGateway paths use typed conversion.
- Dialog has transparent/no-title/no-dim decor, `WRAP_CONTENT`, `TOP|START`, `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE`, dynamic lock pass-through, live attribute relayout, and idempotent show/dismiss.
- Persisted HUD config is used on first HUD frame; Paper/Carbon/Blueprint, modules, opacity, scale, blur, lock, core-bank, position, and FPS remain consistent across VM/cache/service/preview.
- App first visible frame uses resolved persisted medium or explicit Paper fallback; no system-theme flash.
- FGS starts immediately; permission loss stops cleanly; attach/update failures log class/message/stack; `isRunning` reflects successful attach only; duplicate starts create no duplicate windows; lifecycle cleanup occurs once.
- Unit/Compose/static tests, build, lint, device matrix, screenshots/logs, and 20 consecutive force-stop/start cycles pass.

## Risks & Mitigations (NEW_RISKS)

- Official Android pages and Context7 failed/time out. Worker must compile against SDK 36 and verify exact `Dialog` overlay and `setBackgroundBlurRadius` APIs; no unverified bounded-blur claim.
- Android Dialog overlay behavior and OEM cross-window blur bounds remain inconclusive. Device proof gates API31 blur; fallback is valid only with no display-wide blur.
- Current working tree has staged/modified source and untracked planning/review files. Worker must inspect diff and avoid reset or unrelated edits.
- Asynchronous migration can race first service start. Snapshot defaults must not overwrite user values; migration tests cover absent keys.
- `Medium` app state and `HudMedium` HUD state differ. Theme mapping tests must prove equivalence before deleting either type.

## Handoff to Plan Reviewer

Validate geometry split and exact clamp/write-back/no-jump contract; canonical FPS owner plus every VM/cache/repository/ShellGateway initial/live conversion; Dialog decor, flags, lock pass-through, attribute relayout, measured pre-show ordering, lifecycle ownership, and idempotent cleanup. Validate source citations, fallback policy, and full prior feature/FGS/telemetry/device acceptance set. Reject any bounded-blur claim without compile and device screenshot proof.
