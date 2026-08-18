# DeviceInsight - UI/UX Specification

> **Note**: This document describes **what exists on each page and what each component does**. Visual styling (colors, gradients, exact spacing values, typography sizes, animation curves) is the responsibility of the design tool and is intentionally omitted here. When implementing, follow the visual design selected in Stitch / Figma.

---

## 📱 App Overview

DeviceInsight is an Android system monitor app built with Jetpack Compose. It has 4 top-level destinations reachable from a bottom navigation bar:

1. **Dashboard** — live device metrics overview
2. **Tasks** — running processes / app manager
3. **Hardware** — detailed device specs with sub-tabs
4. **Overlay** — configure a floating system-wide widget

Plus a **Settings** screen (separate Activity, reached from the TopAppBar action).

---

## 🧭 Navigation

### Bottom Navigation Bar
*   4 items: Dashboard, Tasks, Hardware, Overlay.
*   Each item: icon + uppercase label.
*   Selected state: highlighted with primary color + scaled icon.
*   Floats above content (glassmorphism style — backdrop blur + subtle border).
*   Vertical position: anchored to the bottom with a gap above the system nav bar.

### Top App Bar
*   App logo + "DeviceInsights" title on the left.
*   Settings icon action on the right (opens SettingsActivity).

### Hardware Sub-Tabs
*   11 horizontal pill tabs at the top of the Hardware screen:
    1. System
    2. CPU
    3. Display
    4. GPU
    5. Network
    6. Battery
    7. Android
    8. Hardware (cameras + USB)
    9. Thermal
    10. Storage
    11. Sensors
*   Tabs float above scrollable content with a translucent backdrop.
*   Auto-scrolls to keep the selected tab visible.

---

## 📄 Screen Specifications

### 1. Dashboard

**Purpose**: At-a-glance live device metrics.

**Sections (top to bottom)**:

1. **DeviceCard** (first, prominent)
    *   Large SOC brand logo on the left (loaded from network or local fallback).
    *   "SYSTEM ONLINE" badge + device name (e.g. "POCO X6 Pro 5G", "OnePlus 13R").
    *   Uptime display (right side).
    *   Row of 3 detail pills: **SOC** (chip name), **RAM** (used/total in GB), **SWAP** (used/total in MB or "OFF").
    *   Row of 1 detail pill: **GPU** (GPU model).

2. **CPU & RAM Hero Gauges** (50/50 side-by-side)
    *   Each: circular gauge (140dp) with animated arc, percentage in center, label below.
    *   Below each gauge: 3 stat blocks — CPU shows Temp/Freq/Cores, RAM shows Used/Total/Swap.

3. **GPU & Thermal Strip**
    *   GPU model on the right.
    *   3 columns: GPU Load %, GPU Temp °C, GPU Freq (current/max).

4. **Quick Metric Grid** (3 columns)
    *   Battery: % + status (Full/Charging/Discharging) + progress bar.
    *   Storage: % + free space in GB + progress bar.
    *   Network: download speed + upload speed.

5. **Power + FPS Strip**
    *   Power icon + power draw in watts (left).
    *   FPS + videogame icon (right).

**Behavior**:
*   All values update live (no manual refresh needed).
*   Initial state: may show a permission request card ("Usage Access") if permission is missing.

---

### 2. Tasks

**Purpose**: View and manage running processes.

**Sections**:

1. **Header**: "Task Manager" title.
2. **Search bar**: filter processes by name/package.
3. **Filter chips**: User Apps / System / All.
4. **Process list** (scrollable): each item shows
    *   App icon + name + package name.
    *   CPU % and RAM usage.
    *   Overflow menu: Force Stop, App Info, Kill Process (root), Copy Package Name.

**Behavior**:
*   Force Stop launches the system app info screen.
*   Kill Process requires root and runs `su -c kill <pid>`.
*   Copy Package copies the package name to the clipboard.

---

### 3. Hardware

**Purpose**: Comprehensive device hardware information.

**Common structure for all tabs**:
*   Optional header card summarizing the tab's content.
*   One or more sections, each with a section title (e.g. "Internal Storage", "Cameras").
*   Each section is a card containing rows of label-value pairs.
*   Content starts with a top spacer to clear the floating tab bar.

#### 3.1 System Tab
*   Device info (model, manufacturer, brand, board, hardware, platform, product, serial).
*   Memory (installed RAM spec, total/available in bytes).
*   Internal Storage (total/free).
*   Connectivity (Bluetooth version).
*   Device Features (all system features as Yes/No list).

#### 3.2 CPU Tab
*   SoC info: model, architecture, manufacturing process, revision, clock range.
*   CPU governor and current frequency.
*   Per-core frequencies and CPU features (AES, NEON, PMULL, SHA1, SHA2).
*   Supported 32/64-bit ABIs.

#### 3.3 Display Tab
*   Resolution, technology, physical size, diagonal size.
*   Density (dpi), x/y dpi.
*   GPU basics (vendor, renderer, cores).
*   Refresh rate, default orientation.

#### 3.4 GPU Tab
*   Renderer, vendor, OpenGL version.
*   Vulkan features, GLSL version.
*   Max viewport size, max texture size, max vertex attribs, max varying vectors.
*   Max fragment uniform vectors, max vertex uniform vectors.

#### 3.5 Network Tab
*   Operator, type (WiFi/Cellular), IP address.
*   Detailed network info (subtype, generation, signal strength, link speed, DNS, gateway, subnet, DHCP server).

#### 3.6 Battery Tab
*   Technology, health, level, status, voltage, temperature, isCharging, capacity.

#### 3.7 Android Tab
*   Android version, API level, security patch, kernel version, build ID, uptime, rooted status.

#### 3.8 Hardware / Devices Tab
*   **Header card**: total camera count + USB device count.
*   **Camera cards** (one per camera): facing (Rear/Front/External), resolution, video resolution, focal length, focus modes, supported features (OIS, zoom, AE lock, AWB lock, flash).
*   **USB device cards** (one per device): product name, manufacturer, serial, VID-PID, class, protocol, revision, USB version, speed.
*   **Empty state**: "No connected devices" card when both lists are empty.

#### 3.9 Thermal Tab
*   **Header card**: overall thermal status (Cool / Normal / Warm / Critical) + peak temperature.
*   **Quick stats row**: MAX / AVG / MIN temperatures.
*   **Sensor cards grouped by category** (CPU, GPU, Battery, Skin, System, Other):
    *   Sensor name, category, current temperature in °C, animated progress bar (relative to peak).
    *   Temperature color encodes hotness.
*   **Empty state**: "No thermal sensors detected (may require root access)".

#### 3.10 Storage Tab
*   **Header card**: storage overview with used/total and percent + animated progress bar (red at 90%+).
*   **Internal Storage card**: usage breakdown with progress bar + Used / Free / Total meta chips.
*   **External Storage card** (if present): same layout as Internal.
*   **Directory Paths section**: 4 cards for Data, Root, JavaHome, Download/Cache paths.
*   **Mount Points section**: cards with path, device, filesystem, access mode (RO badge for read-only).
*   **Empty mount points**: section hidden.

#### 3.11 Sensors Tab
*   **Header card**: total sensor count + fingerprint availability badge.
*   **Category summary row** (4 columns): Motion / Position / Environment / Biometric counts with category-colored icons.
*   **Sensor list grouped by category**:
    *   Each card: type icon, human-readable type name, vendor, version.
    *   Meta chips: Range, Resolution, Power (mA).
    *   "WAKE" badge for wake-up sensors.
*   **Empty state**: "No sensors detected" card.

---

### 4. Overlay

**Purpose**: Configure a system-wide floating widget that displays real-time metrics over any app.

**Architecture**: State managed by a dedicated `OverlayViewModel` (Hilt-injected).

**Permissions required**:
*   `SYSTEM_ALERT_WINDOW` (Display Over Apps) — **mandatory** to run.
*   `PACKAGE_USAGE_STATS` (Usage Access) — needed for the "Current App" metric.
*   Shizuku permission — needed for accurate FPS in some modes.

**Sections**:

1. **Header card**
    *   Status indicator: Active (running) / Ready / Permission Required.
    *   Permission chips (Overlay / Usage / Shizuku) shown when not granted; tap to grant.
    *   Green check icon when service is running.

2. **Style & Layout section**
    *   Horizontal Layout toggle (switch) — switches widget between vertical and horizontal orientation.
    *   Overlay Scale slider (0.5x – 2.0x) with 4 markers (0.5 / 1.0 / 1.5 / 2.0) and live value display.

3. **FPS Monitor Mode section**
    *   "Active source: ..." indicator showing which provider is active (Shizuku / Root / Display).
    *   3-pill selector: **AUTO** / **ROOT** / **SHIZUKU**, each showing Active/Inactive state.
    *   Tapping SHIZUKU when not authorized triggers the Shizuku permission request.

4. **Customize Metrics section**
    *   Header with "X / 15" enabled counter.
    *   Draggable list of 15 metric cards in a `ReorderableList`. Long-press to drag.
    *   Each metric card: drag handle, category-colored icon, name, category label, custom switch.
    *   Metrics grouped conceptually by category in the source definition (Performance, Memory, Power, Thermal, Display, Network, System).

5. **Action button** (56dp tall, full width, anchored at bottom)
    *   When not running: **"START OVERLAY"** (primary gradient). Disabled state shows **"GRANT PERMISSION"** if overlay permission missing.
    *   When running: **"STOP OVERLAY"** (error/red gradient) with stop icon.

**Behavior**:
*   All changes are persisted to `SharedPreferences` immediately.
*   Starting the service launches `OverlayService` (foreground service) with all config as Intent extras.
*   Stopping calls `stopService`.

---

### 5. Settings (separate Activity)

**Purpose**: App-wide preferences.

**Sections**:
1. **Theme picker**: list/grid of available themes.
2. **OLED Black toggle**: forces pure black background regardless of theme.
3. **Temperature unit**: Celsius / Fahrenheit.
4. **Update interval**: refresh rate for live metrics.
5. **About**: app version, credits, links.

---

## 🧩 Component Library (purpose-only, no styling)

This section documents **what each component is for**. Visual styling is left to the design tool.

### Cards
*   **GlassCard** — base card container with translucent background and border. Used as the default card primitive.
*   **GradientCard** — card with gradient background overlay.
*   **AnimatedGlassCard** — GlassCard with scale-in entrance animation.
*   **SectionCard** — section grouping card (used in Sensors, Storage, Thermal, Devices, Overlay sections).
*   **HeaderCard** — large summary card at the top of a screen (used for status overview).

### Detail & Meta
*   **InfoRow** — single label-value row inside an InfoSection.
*   **InfoSection** — titled card containing multiple InfoRows.
*   **DetailPill** — compact icon + label + value block (used in Dashboard device card).
*   **StatBadge** — centered value with uppercase label below (used in Thermal quick stats).
*   **CategoryHeader** — colored bar + uppercase title + count (section separator).
*   **CategoryBadge** — icon-in-circle + monospace count + label (category summary item).
*   **MetaChip** — small monospace pill for inline metadata (sensor range, USB serial, etc.).
*   **FeaturePill** — enabled/disabled indicator pill (camera features).
*   **PermissionChip** — colored dot + label, clickable to grant (overlay permissions).
*   **FpsModePill** — 3-column mode selector pill showing Active/Inactive state.

### Data Visualization
*   **CircularGauge** — animated arc gauge (CPU/RAM hero).
*   **ProgressBar (animated)** — horizontal fill bar with animated width (storage usage, thermal relative temp).
*   **GlowStatBlock** — small stat with glow effect (CPU temp/freq/cores).

### Controls
*   **Switch** (Material3) — standard toggle with custom colors.
*   **PremiumSwitch** — custom 44×26dp switch with check icon when on (Overlay metrics).
*   **PillTab** — pill-style tab for hardware tab bar.
*   **Slider** (Material3) — scale slider with markers.

### Lists
*   **ReorderableList** — drag-to-reorder list (Overlay metrics).

---

## 📊 Data Models (UI-facing)

These are the user-facing entities the UI surfaces. Backed by providers that read from Android system services.

*   **HardwareInfo** — aggregate of all device hardware data.
*   **DashboardMetrics** — live metrics (CPU, RAM, battery, etc.).
*   **SensorDetail** — name, type, category, vendor, version, resolution, range, power, wake-up flag.
*   **ThermalSensor** — name, temperature.
*   **CameraInfo** — id, facing, resolution, video resolution, focal length, focus modes, feature flags.
*   **UsbDeviceInfo** — product, manufacturer, serial, VID-PID, class, protocol, revision, version, speed.
*   **MountPoint** — path, device, file system, read-only flag.
*   **DirectoryInfo** — data/root/java/download paths + external storage paths.
*   **OverlayMetricItem** — id, name, category, icon, enabled, order.

---

## 🔒 Permissions

| Permission | Where used | Required |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Overlay | Yes (to start) |
| `PACKAGE_USAGE_STATS` | Overlay (Current App metric) + Dashboard | For full features |
| `INTERNET` | SOC logo loading (Coil) | No (graceful fallback) |
| `FOREGROUND_SERVICE` | OverlayService | Yes (for overlay) |
| Root | FPS monitor, thermal sensors, kill process | For advanced features |
| Shizuku | FPS monitor, app detection | Optional alternative to root |

---

## 🔄 Empty / Edge States

Every list-based screen handles empty data with a dedicated card:

*   **Sensors empty**: "No sensors detected"
*   **Thermal empty**: "No thermal sensors detected — may require root access"
*   **Devices empty**: "No connected devices"
*   **Storage mount points empty**: section hidden, not shown
*   **Overlay no permission**: header shows "Permission Required" state with chips to grant
*   **Overlay no usage stats**: "Current App" metric toggle redirects to Usage Access settings
*   **Dashboard no usage stats**: shows permission request card

---

*This document is the single source of truth for **what** the UI is. The **how** (visuals) is owned by the design tool.*