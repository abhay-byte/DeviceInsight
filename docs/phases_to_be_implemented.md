# Phased Implementation Roadmap

Based on the core features already implemented (Dashboard, Hardware, Tasks, Settings, Overlay), the following phases outline the roadmap to fully feature-complete the application as per `feature_list.md`.

## Phase 4: Home Screen Widgets
**Goal**: Provide at-a-glance system monitoring directly from the Android Home Screen.
- [ ] **Small Widgets (2x2)**:
    - [ ] CPU Gauge Widget
    - [ ] RAM Gauge Widget
    - [ ] Battery Status Widget
- [ ] **Medium Widgets (4x2)**:
    - [ ] System Overview Grid (CPU/RAM/Battery/Storage)
    - [ ] Network Sparkline Monitor
- [ ] **Large Widgets (4x4)**:
    - [ ] "Command Center" Dashboard Widget
- [ ] **Widget Configuration Activity**: UI to customize widget transparency and refresh rate.

## Phase 5: Deep Hardware Analysis (Advanced)
**Goal**: Expand the `HardwareScreen` with deep-dive technical details, some requiring lower-level APIs.
- [ ] **Advanced CPU**:
    - [ ] Real-time per-core usage & frequency (requires parsing `/proc/stat` or `/sys/devices`).
    - [ ] CPU Governor information.
    - [ ] Cache sizes (L1/L2/L3).
- [ ] **Advanced GPU**:
    - [ ] OpenGL ES Extensions list.
    - [ ] Vulkan API capabilities & extensions.
- [ ] **Advanced Camera**:
    - [ ] Camera 2 API integration to read sensor capabilities (Aperture, ISO range, Focal lengths).
- [ ] **Advanced Storage**:
    - [ ] Partition breakdown (`/system`, `/vendor`, `/data`) using `StatFs` and mount points.

## Phase 6: Root Features & Process Control
**Goal**: Unlock powerful features for rooted devices.
- [ ] **Root Detection**: Implement reliable root check (e.g., checking `su` binary).
- [ ] **Process Killing**: Implement `su` commands to force-kill processes from `TasksScreen`.
- [ ] **Advanced GPU Stats**: Read GPU load/frequency from system files (usually requires root on Qualcomm devices).
- [x] **FPS Monitoring**: Reliable frame tracking using a hybrid approach of `SurfaceFlinger` latency for games and `gfxinfo` for UI apps. Supports 120 FPS. See [fps_monitor_implementation.md](fps_monitor_implementation.md) for details.

## Phase 7: Polish & Optimization
**Goal**: Refine the user experience and performance.
- [ ] **Graph Optimization**: Improve Vico chart rendering performance for 60fps scrolling.
- [ ] **Localization**: Support multiple languages.
- [ ] **Accessibility**: Full TalkBack support for all Glass UI elements.
- [ ] **Onboarding**: "Welcome" tutorial explaining permissions (Usage Stats, Overlay).
