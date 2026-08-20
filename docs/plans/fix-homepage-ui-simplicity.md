# Implementation Plan - Simplify & Declutter Homepage (Dashboard) UI

## Problem Statement
The current Dashboard (Home page) is overly complicated, dense, and visually cramped:
1. **Device Identity Card**: Cluttered with 4-5 micro-chips (`8 Cores · ARM64`, `4.1/7.3G`, `86.36 GB Free`, `Android 16`) that duplicate data shown directly below it.
2. **CPU Card Overload**: Shows every single individual core (e.g. 8 individual core pill boxes with load bars, cluster tags, frequencies, max frequencies) creating massive vertical sprawl and sensory overload on the home screen. This granular breakdown belongs in the Hardware CPU tab.
3. **GPU Card Overload**: Contains 3 stacked rows (Cores, Load, Temp + Frequency spectrum with Min/Cur/Max + DVFS status) taking excessive height.
4. **Hero Metrics Missing Clean Visual Hierarchy**: The original clean dual hero gauge design (or clean summary stat hero) was replaced by dense text pills and micro-progress bars that look like a dense terminal rather than a modern, glanceable Android dashboard.
5. **Card Spacing & Padding**: Low breathing room, cramped vertical rhythm, high visual noise, and repetitive borders/gradients making it difficult to read critical device metrics at a glance.

## Target Experience & Visual Hierarchy
A clean, premium, modern Android dashboard (inspired by Material 3 / Clean Glassmorphic utility design):
1. **Device Header Hero Card**:
   - Sleek SoC branding with logo, chip name, device model, and live status beacon + uptime.
   - Clean, non-repetitive metadata (e.g. Android version & OS architecture or subtle badges).
2. **System Health & Load Hero (CPU & RAM)**:
   - Beautiful side-by-side or unified hero section featuring clean circular gauges or modern progress metrics for CPU utilization & RAM usage.
   - Clean summary badges below gauges (CPU clock/temp, RAM used/total).
3. **Hardware Snapshot Row / Grid (GPU, Battery, Storage, Network)**:
   - Balanced 2x2 grid (or streamlined quick-metric cards) with clear icons, bold primary values, secondary status, and smooth progress indicators.
   - GPU: Clean single card with GPU model/load & clock or temperature.
   - Battery: Clean card with percentage, charging status/voltage.
   - Storage: Clean card with percent used, free GB remaining.
   - Network / Thermal / Power: Clean, easy-to-read cards.
4. **Live Performance Footer Strip (Power Draw & FPS)**:
   - Clean, balanced horizontal pill strip showing current Power Draw (Watts) and Refresh Rate / FPS with proper contrast and spacing.

## Authoritative Files Affected
- `app/src/main/java/com/ivarna/deviceinsight/presentation/dashboard/DashboardScreen.kt` - Main dashboard UI composables and layout hierarchy.
- `app/src/main/java/com/ivarna/deviceinsight/presentation/components/DashboardComponents.kt` - Shared quick metric / gauge components.
- `app/src/main/java/com/ivarna/deviceinsight/presentation/components/CircularGauge.kt` - Circular gauge component refinement if needed.
- `docs/ui_ux_design.md` - Update specification to match the cleaned up, simplified dashboard layout.

## Detailed Changes

### 1. Simplify `CompactDeviceCard`
- Keep SoC Logo, SoC Name, Device Model Name, and Online beacon + Uptime.
- Clean up chips: Remove duplicated RAM/Storage chips. Show only clean badges: OS/Android Version and CPU Arch / Total Cores.
- Increase padding and rounded corners for a modern airy look (24.dp).

### 2. Streamline CPU & RAM into Clean Glanceable Hero (`CpuRamHeroSection`)
- Replace the 8-core sprawling grid on the home page with a high-level, glanceable CPU & RAM Hero overview (either dual circular gauges with `CircularGauge` or sleek dual metric cards).
- Display:
  - CPU: Usage %, Temp badge, Current/Max frequency, Governor.
  - RAM: Usage %, Used GB / Total GB, Available / ZRAM.
- Provide clean navigation hint or keep detailed per-core breakdown in Hardware -> CPU tab where users expect deep hardware inspection.

### 3. Simplify GPU & System Metrics into Balanced 2x2 Grid (`QuickMetricGrid`)
- Create a clean, un-cramped 2x2 or 4-card grid for core metrics:
  - **GPU**: Usage %, Current Clock, Temp.
  - **Battery**: Level %, Charging status, Voltage/Health.
  - **Storage**: Used %, Free GB, Progress bar.
  - **Network / Power**: Speed (Up/Down) or Live Bandwidth.
- Ensure consistent card heights, proper typography hierarchy, and comfortable padding (14-16dp).

### 4. Refine Power & FPS Strip (`PowerFpsStrip`)
- Retain power draw (W) and display refresh rate / FPS.
- Polish spacing, icon sizes, and typography contrast so it doesn't collide with bottom navigation.

### 5. Spacing & Theming Polish
- Set consistent spacing in `LazyColumn`: `verticalArrangement = Arrangement.spacedBy(16.dp)` and `contentPadding = PaddingValues(horizontal = 16.dp, top = 8.dp, bottom = 120.dp)`.
- Ensure all typography styles conform to MaterialTheme typography with good line heights and letter spacing.

## Acceptance Criteria
- [x] Home page is significantly simplified, clean, and not cramped.
- [x] Sprawling per-core CPU breakdown removed from Dashboard (remains in Hardware tab).
- [x] Visual hierarchy is clear: Device Info -> CPU & RAM Hero -> GPU & System Grid -> Power & FPS Strip.
- [x] UI fits comfortably on various screen sizes with clean scrolling and no overlapping elements.
- [x] All live metrics (CPU, RAM, GPU, Battery, Storage, Network, Power, FPS) update reactively and smoothly.
- [x] Compiles and builds cleanly with `./gradlew assembleDebug`.
