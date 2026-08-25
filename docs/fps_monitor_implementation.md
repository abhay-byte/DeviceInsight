# FPS Monitoring Implementation

> Last updated: 2026-08-25 — reflects code after FactualStats/ApexCore port.

DeviceInsight HUD FPS overlay reports **real external app/game FPS** via privileged `dumpsys` pipelines. Zero is never fabricated. Unavailable state is rendered as `—` / `NO SIGNAL`.

---

## 1. Architecture

```
data/fps/
  FpsMonitor.kt                 # Compatibility facade -> FpsRepository -> FpsSnapshot -> FpsSample(fps, source)
  FpsRepository.kt              # Game vs UI routing, last-good TTL, 3-sample median smoothing
  model/FpsSnapshot.kt          # Typed snapshot with FpsMethod enum (SURFACEFLINGER/GFXINFO/DMA_FENCE/NONE)
  source/
    FpsDataSource.kt            # Interface
    SurfaceFlingerFpsDataSource.kt  # --list + --latency, Android 15+ RequestedLayerState parsing
    GfxinfoFpsDataSource.kt     # framestats FrameCompleted deltas + histogram fallback (UI only)
  util/
    ForegroundAppResolver.kt    # Dumpsys window + activity fallback, self-filter, OEM tolerant
    ShellExecutor.kt            # su/sh executor with cached hasSu
  privilege/
    PrivilegeMode.kt            # AUTO/ROOT/SHIZUKU/STANDARD
    PrivilegeTier.kt            # ROOT/SHIZUKU/STANDARD
    PrivilegePolicy.kt          # Fail-closed chain resolution
    ShellGateway.kt             # Policy-aware executeChain, mode-aware cache invalidation
    ShizukuAccess.kt            # Binder + UserService (IShellService.aidl) shell bridge
    ShellUserService.kt         # Shizuku UserService (sh -c)
    IShellService.aidl          # IPC for exec

Integration: OverlayService --1 Hz--> FpsMonitor.getCurrentFpsWithSource() -> MonitorBus.pushFast(HudFast) -> HudFast -> HudPanel
DashboardRepository still calls fpsMonitor.getCurrentFps() (runBlocking facade) but never falls back to display refresh rate.
```

`FpsMonitor` is intentionally a thin facade so existing `OverlayService` and `DashboardRepository` call sites stay compiling. All measurement logic lives in `FpsRepository` and the `*DataSource` classes.

---

## 2. Source Priority / Routing

Resolved foreground package is classified as **game-like** if any of:
- package prefix matches known game/benchmark prefixes (`com.miHoYo.`, `com.tencent.`, `com.epicgames.`, `com.unity3d.`, `com.garena.`, `com.activision.`, `com.roblox.`, `com.mojang.`, `com.futuremark.`, `com.antutu.`, `com.primatelabs.`, `com.benchmark.` etc)
- `dumpsys SurfaceFlinger --list` contains a layer owned by the package that itself contains `SurfaceView`, `NativeActivity`, `Vulkan`, `GLSurfaceView`, `UnityPlayer`, `Unreal`, or `Cocos2dx`
- `dumpsys window ... | grep mCurrentFocus` layer line for that package contains one of the above markers

BLAST alone is **not** a game signal — normal Android UI uses BLAST.

Routing (`FpsRepositoryImpl`):

```
if game:
  SF (--latency) -> real render-surface frametime
  never gfxinfo  -> honest NONE if SF unavailable
else: // UI / normal app
  SF -> gfxinfo (FrameCompleted deltas, then histogram fallback)
  -> NONE

DMA_FENCE:
  Code defines FpsMethod.DMA_FENCE but no DmaFenceFpsDataSource is active.
  Intentionally deferred: raw kgsl dma_fence counting inflates FPS on Adreno,
  and generic DMA tracks vsync rather than game rate on some GPUs.
  If ported, it must use Adreno adreno_cmdbatch hybrid path and never override
  a valid SurfaceFlinger game result with a vsync-like 120 Hz DMA value.
```

On source change, the 3-sample median window is cleared. On package change, last-good, median window, and gfxinfo timestamp history are cleared.

---

## 3. SurfaceFlinger Parser

### 3.1 Layer Discovery

```bash
dumpsys SurfaceFlinger --list 2>/dev/null
```

- Parses each line with `parseLayerName()` (see §4).
- Keeps only lines owned by the foreground package (contains full package or short name >=4 chars).
- Rejects `ActivityRecord` and `InputSink`.
- Prefers first `SurfaceView`/`NativeActivity`/`Vulkan`/`GLSurfaceView`.
- Fallback: first layer containing `#` (concrete surface), then first owned layer.
- Cache: 800 ms TTL, invalidated on package change, latency empty, surface not found, or `dumpsys --latency` failure. Correctness > saving one `--list`.

### 3.2 Latency

```bash
dumpsys SurfaceFlinger --latency "<layer>" 2>/dev/null
```

Output:

```
16666666                     # refresh period ns (16.66ms = 60Hz)
1000000000 1010000000 1016666666  # triple: frameStart/desired, ..., frameComplete/presentReady
1016666666 1025000000 1033333332
...
```

Parser (`parseLatency`):

```
first line = refreshPeriodNs (reject <=0 or Long.MAX_VALUE)
for each remaining line split into 3 longs:
  A = parts[0] (frame start), C = parts[2] (complete)
  reject if A==0 or C==0 or Long.MAX_VALUE, C <= A
  frameTimeNs = C - A
  reject if <=0 or >2_000_000_000 ns
  frameTimeMs = frameTimeNs / 1_000_000
  reject if <=0 or >2000ms
  keep, track ceil(frameTimeNs / refreshPeriod) for jank
avg = mean(frametimes)
fps = (1000 / avg).coerceIn(1, 240)
```

- Returns `null` if <2 lines (Android 15 may return only refresh period) or no valid triples.
- Retains `frametimes` and `jankCount` in `FpsSnapshot` for HUD/debug.
- Hard ceiling 240 FPS; refresh-rate-relative ceiling is applied in gfxinfo path.

---

## 4. Android 15+ Layer Parsing

Modern `SurfaceFlinger --list` may emit:

```
RequestedLayerState{com.example.game/com.example.GameActivity#1183 parentId=42}
RequestedLayerState{3fa18c4 com.example.game/com.example.GameActivity#1183 parentId=42}
RequestedLayerState{3fa18c4 SurfaceView[com.example.game/com.example.GameActivity]#1183 z=10}
```

Legacy:

```
SurfaceView[com.example.game/com.example.GameActivity]#123
com.example.game/com.example.GameActivity#456
```

`parseLayerName(raw)`:

1. If matches `RequestedLayerState{([^}]+)}`, extract body; else use trimmed line.
2. Strip optional leading hex handle `^[0-9a-fA-F]+\s+`.
3. Strip metadata suffixes ` parentId=...`, ` z=...`, ` relativeParentId=...` (greedy to EOL).
4. Trim and return non-empty name, preserving optional `#id`.

Never returns the hex handle alone. Tests cover both old and Android 15+ formats.

---

## 5. gfxinfo (UI-only)

```bash
dumpsys gfxinfo <package> framestats 2>/dev/null
```

- Locates header `Flags, ... FrameCompleted, ...` by name (not position).
- For each row after `---PROFILEDATA---`, computes `delta = current FrameCompleted - previous FrameCompleted`.
- Plausible window: `0 < deltaMs <= 100`.
- On first call with empty delta window (fresh view), bootstraps from intra-output deltas (`parseProfileBootstrap`) limited to last 90 frames.
- Package change resets `lastFrameCompletedNs`, `lastPollTimeMs`, `profileBootstrapped`.
- Fallback to `HISTOGRAM:` then `GPU HISTOGRAM:` if PROFILEDATA yields nothing.
- Calls `fpsFromFrametimes`: if >=2 frames, `1000/avg` capped to `refreshRateHz`; if 1 frame but pollDeltaSec>0, `size/pollDelta`; else `1000/avg`.
- Refresh ceiling `coerceIn(1,240)`.

**Never used as game FPS.** `FpsRepository` routes games to SF-only; UI apps may use gfxinfo.

---

## 6. Foreground Resolution

`ForegroundAppResolver.resolve()`:

1. `dumpsys window ... | grep -E 'mCurrentFocus|mFocusedApp'`
   - Filter empty and lines containing `=null`.
   - `extractPackage(line)` tries `u0 <pkg>`, then ` {<pkg>/`, then slash heuristic.
   - `extractPackage` sanitizes `"null"` to `null`.
   - Skip `appContext.packageName` (never measure self, survives overlay visible).
   - First valid wins.
2. Fallback: `dumpsys activity activities ... | grep -E 'ResumedActivity|mResumedActivity' | head -5` with same extraction.
3. `pidOf(package)` via `pidof <pkg> | awk '{print $1}'`.
4. `readActiveRenderFrameRate()` → `WindowManager.display.refreshRate` else `dumpsys display` `mActiveRenderFrameRate` / `renderFrameRate`, else `60f`.

Helper `isGameLikeSurface(package)` uses `known prefixes + --list hasGameLayer + mCurrentFocus hasGameMarker` (no bare BLAST).

State cleared when `foreground.packageName != lastPackage` (both non-null).

---

## 7. Privilege / Shell

Modes (stored in `caliper` DataStore `fpsMode`, cached via `HudSettingsCache`):

- `AUTO` → chain `ROOT, SHIZUKU, STANDARD` (try in order; unavailable tier skipped, not terminal)
- `ROOT` → `ROOT` only, honest unavailable if `su` not present
- `SHIZUKU` → `SHIZUKU` only, honest unavailable if binder/permission/userService missing, never silently falls back to root
- `STANDARD` → `STANDARD` only

Previous bug: AUTO checked `Shizuku.pingBinder()` first; if binder existed but permission missing, it returned `NONE` and never tried root. Fixed via `PrivilegePolicy.chain(DEFAULT_CHAIN)` and `ShellGateway.executeChain` which iterates allowed tiers and checks `canRoot()/canShizuku()` per tier.

`ShellExecutor` caches `hasSu` (`su -c id` + uid check, 3 s timeout) until `clearCache()` on mode change. `ShizukuAccess` uses supported `Shizuku.bindUserService(UserServiceArgs{ ShellUserService })` + `IShellService.exec()` (not `Shizuku.newProcess` reflection). Binds on `binderReceived` and after permission grant, tracks `binderAlive/ready` StateFlows. `ShellGateway.execute(policy)` returns `(ShellResult, PrivilegeTier?)` so `FpsSnapshot.access` is honest.

Capability checks are cached/cheap; full `su -c "echo"` is not executed every 1 s second.

---

## 8. Sampling & Smoothing

- `OverlayService.startFastTicker()` runs on `Dispatchers.IO` owned by `SupervisorJob` lifecycle (cancelled in `onDestroy`), loop `while(isActive && isRunning)`:
  ```kotlin
  val snap = fpsMonitor.getCurrentFpsWithSource() // IO, ~1 s measurement window
  monitorBus.pushFast(HudFast(snap.fps, snap.source))
  delay((1000 - elapsed).coerceAtLeast(200))
  ```
  Approximately 1 Hz expensive sample. `MonitorBus.pushFast` holds the last value so HUD composables read a stable StateFlow. No 10 Hz `dumpsys` storm.

- `FpsRepository` holds last valid snapshot for `LAST_GOOD_HOLD_MS = 3500 ms`:
  - Returns `copy(isStale = true)` on transient failure if same package and age < TTL.
  - After TTL expires, returns `FpsSnapshot.ZERO` (`method=NONE`, `fps=0`, source `—`).
  - Cleared on package/source/privilege change, so stale data never crosses apps.

- Median smoothing: `recentDisplayFps` `ArrayDeque<Float>(3)`, median of sorted window. Cleared on source change and package change. Visual jitter is smoothed without EMA lag.

- All shell work `withContext(Dispatchers.IO)` (facade) and inside sources/gateway already on IO.

---

## 9. Data Model & HUD Contract

```kotlin
data class FpsSnapshot(
  val currentFps: Float,
  val frametimeAvgMs: Float = 0f,
  val frametimeP1Ms: Float = 0f,
  val frametimeP01Ms: Float = 0f,
  val frametimes: List<Float> = emptyList(),
  val jankCount: Int = 0,
  val method: FpsMethod = FpsMethod.NONE,
  val access: PrivilegeTier? = null,
  val packageName: String? = null,
  val isStale: Boolean = false
)
enum class FpsMethod { DMA_FENCE, SURFACEFLINGER, GFXINFO, NONE }
fun FpsMethod.label() = when(this){ DMA_FENCE->"DMA"; SURFACEFLINGER->"SF"; GFXINFO->"GFX"; NONE->"—" }

data class FpsSample(val fps: Int, val source: String) // "SF"|"GFX"|"DMA"|"—"
data class HudFast(val fps: Int = 0, val source: String = "—")
fun HudFast.isNoSignal() = source == "—"
```

HUD shows `—` + `NO SIGNAL` when `source == "—"`. Dashboard history writes honest 0, not display refresh Hz.

---

## 10. Diagnostics

```bash
adb shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'
adb shell dumpsys activity activities | grep -E 'ResumedActivity|mResumedActivity'
adb shell dumpsys SurfaceFlinger --list | grep <package>
adb shell dumpsys SurfaceFlinger --latency "<selected layer>"
adb shell dumpsys gfxinfo <package> framestats
adb shell dumpsys display | grep -E 'mActiveRenderFrameRate|renderFrameRate'
adb logcat | grep -E 'Fps|FPS|DeviceInsight|ShizukuAccess'
# In-app log per sample:
# FpsMonitor: FPS 60 SF pkg=com.example.game access=ROOT isStale=false
```

SurfaceFlinger logs surface selection and latency result; `surface == null` and `isSuccess==false` invalidate the 800 ms cache.

---

## 11. Known Limitations (never hidden behind fake FPS)

- OEM builds may block `dumpsys SurfaceFlinger --latency` via Shizuku/shell (SELinux, missing permission) → returns `—` honestly.
- Android build may return refresh period only (no frame triples) → `parseLatency` null → `—`.
- Some engines recreate surfaces on rotation/loading screen; 800 ms cache + invalidation handles most but one sample may briefly flash stale then `—`.
- No root/vendor-specific GPU daemon active in this build; DMA is intentionally not selected for games until Adreno `adreno_cmdbatch_submitted` hybrid path is validated. Generic `kgsl-timeline dma_fence` counting is not used because it can report vsync rate.
- Shizuku `UserService` binding takes ~0–3 s after permission grant; samples before bind report `—` (binder not ready).
- `gfxinfo` may be empty before HWUI draws or when package uses `android:hardwareAccelerated="false"` → fallback to histogram or `—`.
- `pidof` and `dumpsys display` may be restricted on some OEMs; refresh defaults to 60 Hz without failing the whole pipeline.

---

## 12. Files Changed (this fix)

- `app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsMonitor.kt` — facade over repository
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsRepository.kt` — routing, TTL, median
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/model/FpsSnapshot.kt` — typed model
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/source/*` — SF, Gfxinfo, FpsDataSource
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/util/ForegroundAppResolver.kt` — resolver + game classifier
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/util/ShellExecutor.kt` — cached su
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/privilege/*` — PrivilegeMode/Policy/Tier, ShellGateway, ShizukuAccess, ShellUserService, IShellService.aidl
- `app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsModule.kt` — Hilt binding
- `app/src/main/java/com/ivarna/deviceinsight/data/repository/DashboardRepositoryImpl.kt` — removed display-Hz fallback
- `app/src/main/java/com/ivarna/deviceinsight/service/OverlayService.kt` — 1 Hz IO ticker
- `app/build.gradle.kts` — `aidl = true`, `mockk` for tests
- `app/src/test/java/com/ivarna/deviceinsight/data/fps/**/*` — parser/routing/privilege/TTL tests
- `app/src/main/aidl/.../IShellService.aidl`
- `docs/fps_monitor_implementation.md` — this document

Build: `./gradlew test` (all green), `./gradlew assembleDebug`, `./gradlew lintDebug` (pre-existing warnings only).

