# Plan Review — fix-homepage-ui-simplicity
Pass: 1 | Iteration: 1

## Verdict: APPROVE (with minor notes for Worker)

The plan is architecturally sound. Target files are correct, data model is sufficient,
existing components (`CircularGauge`, `GlassCard`, `QuickMetricCard`, `GlowStatBlock`,
`SectionDivider`) are all confirmed present and will work for the proposed new layout.
No new dependencies required.

---

## Findings

### [MINOR] `CpuRamHeroSection` is a new composable — plan doesn't name its location

**Location**: `DashboardScreen.kt`
**Problem**: Plan calls for replacing `CpuAllCoresCard` with a new `CpuRamHeroSection`, and
merging `RamStorageCard` into it. The plan doesn't state whether this lives in
`DashboardScreen.kt` (as a private fun, consistent with every other card) or in
`DashboardComponents.kt`. Both are reasonable; ambiguity may cause Worker to scatter it.
**Evidence**: Every current card (`CompactDeviceCard`, `CpuAllCoresCard`, `GpuAllClocksCard`,
`RamStorageCard`, `QuickMetricGrid`, `PowerFpsStrip`) is a `private fun` inside
`DashboardScreen.kt`. `DashboardComponents.kt` only holds shared primitives.
**Impact**: Low — Worker will almost certainly put it in `DashboardScreen.kt`, but the plan
should be explicit.
**Required planner change**: State "add `CpuRamHeroSection` as a `private fun` in
`DashboardScreen.kt`, consistent with existing card composables."

---

### [MINOR] Dead imports after removal of three card composables

**Location**: `DashboardScreen.kt` import block
**Problem**: After removing `CpuAllCoresCard`, `GpuAllClocksCard`, and `RamStorageCard`,
at least these imports become unused and will trigger compiler warnings (or lint errors
if the project treats warnings as errors):
- `import coil.compose.AsyncImage` (already unused in current file)
- `import androidx.compose.material.icons.filled.SwapHoriz` (used only in `RamStorageCard`)
- `import androidx.compose.material.icons.filled.DeveloperBoard` (used only in `GpuAllClocksCard`)
- Potentially `SdStorage`, `Speed`, depending on what the new GPU card uses.

**Evidence**: `AsyncImage` import present at line 64 with zero call sites confirmed by grep.
`SwapHoriz` at line 38 used only in `RamStorageCard` (lines 1212–1216). `DeveloperBoard`
at line 32 used only in `GpuAllClocksCard` (line 833).
**Impact**: Build will still succeed (unused imports are warnings not errors in Kotlin),
but plan claims "compiles and builds cleanly" so Worker should clean them up.
**Required planner change**: Add a step: "After removing the three replaced composables,
remove their orphaned imports from `DashboardScreen.kt`."

---

### [MINOR] `docs/ui_ux_design.md` update listed but no concrete diff specified

**Location**: `docs/ui_ux_design.md`
**Problem**: Plan lists this file as "Authoritative Files Affected" and says to update
the spec. However, the plan gives no guidance on *what* to change — which section,
which lines. The doc currently describes the Dashboard sections accurately (lines 57–86),
including the old CPU gauges + RAM hero design described in section 2 of the spec
(`CPU & RAM Hero Gauges`, circular 140dp gauges). This actually matches what the plan
*wants* to restore — so the spec is closer to the target than the current code.
**Evidence**: `docs/ui_ux_design.md` lines 66–82 describe exactly the target hierarchy
(Hero Gauges → GPU Thermal Strip → Quick Metric Grid → Power+FPS Strip), which aligns
with the plan's target layout.
**Impact**: Low. Worker may over-edit or skip this file. The spec doesn't need wholesale
rewriting; it needs minor delta for any GPU card simplification.
**Required planner change**: Clarify whether `docs/ui_ux_design.md` needs changes at all
given it already matches the target layout, OR specify exactly which lines to update.

---

### [SUGGESTION] `CircularGauge` is already imported but currently unused

**Location**: `DashboardScreen.kt` line 68
**Problem**: `CircularGauge` is imported but not called anywhere in current code. The plan
relies on it for `CpuRamHeroSection`. Worker must actually call it — plan correctly
identifies this but the Worker should know it's already imported (no import to add).
**Evidence**: `import com.ivarna.deviceinsight.presentation.components.CircularGauge` at
line 68; zero call sites confirmed by grep.
**Impact**: None — informational for Worker.

---

### [SUGGESTION] `GpuAllClocksCard` → plan leaves GPU represented only in `QuickMetricGrid`

**Location**: `DashboardScreen.kt` — GPU section
**Problem**: Plan removes `GpuAllClocksCard` (the large GPU section) and proposes a
simplified GPU card in the 2×2 grid. However, `DashboardMetrics` has `gpuUsage`,
`gpuFreqMhz`, `gpuTemp`, `gpuModel`, `gpuVendor` — all available. The plan says
GPU card in grid: "Usage %, Current Clock, Temp" but `QuickMetricCard` in
`DashboardComponents.kt` only supports a single `value`, `subtext`, and `progress` —
it can show only 2 strings + a bar. Worker will need to choose which two GPU values
to surface without modifying `QuickMetricCard`'s signature.
**Evidence**: `QuickMetricCard` signature: `value: String, subtext: String? = null,
progress: Float? = null` (DashboardComponents.kt lines 36–44).
**Impact**: Low — plan intent is clear (simplify), Worker will pick the right subset.
Noting so Worker doesn't silently modify `QuickMetricCard` unnecessarily.

---

## Summary

| Severity   | Count |
|------------|-------|
| CRITICAL   | 0     |
| MAJOR      | 0     |
| MINOR      | 3     |
| SUGGESTIONS| 2     |

Plan is implementable as-is. The minor gaps (composable location, orphaned imports, doc
delta clarity) are small enough that a competent Worker will handle them without explicit
guidance, but documenting them reduces ambiguity.
