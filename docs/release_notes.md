# DeviceInsight Release Notes

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
