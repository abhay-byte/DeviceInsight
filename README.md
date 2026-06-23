<div align="center">
  <img src="assets/logo.webp" width="180" alt="DeviceInsight Logo" />
  <h1>DeviceInsight</h1>
  <p><strong>The Ultimate Android System Monitor & Task Manager</strong></p>

  <p>
    <a href="https://android.com"><img src="https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android" alt="Platform"></a>
    <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin" alt="Kotlin"></a>
    <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL_v3-blue.svg" alt="License"></a>
  </p>
</div>

<p align="center">
  <img src="docs/storelisting/store_listing_promo.gif" width="100%" style="background: url('https://raw.githubusercontent.com/Codelessly/FlutterLoadingGIFs/master/packages/cupertino_activity_indicator.gif') center center no-repeat; min-height: 250px;" alt="DeviceInsight Promo Animation" loading="lazy" />
</p>

## 📖 Overview

While Android has matured into a sophisticated OS, its tools for detailed system monitoring and process management remain surprisingly limited. **DeviceInsight** bridges this gap, providing a comprehensive, power-user-friendly hub to monitor what's effectively happening "under the hood" of your device.

Built with a stunning, futuristic **"Elegant Glassmorphism"** interface, DeviceInsight delivers real-time analytics for CPU, RAM, Storage, Network, Battery, and more, rivaling desktop-class task managers.

---

## ✨ Core Features

### 📊 System Dashboard
An at-a-glance overview of critical system health metrics with real-time monitoring.
* **CPU Monitor**: Live load history with monotonic scrolling graphs, per-core utilization, frequency tracking, and thermal monitoring.
* **RAM & GPU**: Memory usage breakdown (Active, Cached, Free, ZRAM/Swap) and GPU load/frequency tracking.
* **Power Tracking**: Real-time wattage estimation, voltage, current, and historical consumption charting.
* **Network**: Real-time upload/download sparkline graphs.

### ⚙️ Advanced Task Manager
A comprehensive process management system that provides deep visibility into all running processes.
* **Process Details**: View all user apps, system services, and daemons with PID, package name, and uptime.
* **Real-time Metrics**: Monitor CPU, RAM, and network activity per process.
* **Process Control**: Force stop apps and force-kill processes (Root).

### 📱 Performance Overlay (HUD)
A customizable heads-up display for real-time monitoring directly over games or applications.
* **Hybrid FPS Monitoring**: Accurate, hardware-level FPS tracking (supports 120Hz) using `SurfaceFlinger` for games and `gfxinfo` for standard UI apps.
* **Metrics**: Floating display for CPU load, GPU load, Temperature, RAM, and Network speed.
* **Unobtrusive Design**: Adjustable opacity, size, color, and positioning.

### 🧰 Deep Hardware Analysis
Comprehensive hardware specifications and real-time sensor data.
* **CPU & Graphics**: SoC identifier, topology, ARM Neon, OpenGL/Vulkan capabilities and extensions.
* **Display & Media**: Panel resolution, HDR capabilities, refresh rates, Widevine DRM levels, and audio/video codecs.
* **Sensor Data**: Raw data output from accelerometer, gyroscope, magnetometer, proximity, and more.

### 🖼️ Home Screen Widgets
Customizable, beautifully animated widgets for at-a-glance system monitoring.
* Available in multiple sizes: Small (2x2), Medium (4x2), Large (4x4), and Extra Large (6x4).

---

## 🎨 Premium UI & Themes

DeviceInsight is designed to be the most visually stunning system monitor on Android. The UI combines depth, translucency (`backdrop-filter: blur`), and vibrant gradients optimized for OLED displays. 

Choose from 5 distinct premium themes:
* **Tech-Noir (Default)**: Futuristic cybernetic vibe with Cyber Blue & Electric Purple.
* **Cyberpunk Edge**: Aggressive neon styling with Yellow & Neon Blue.
* **Deep Ocean**: Calming, professional Navy Blue and Cyan.
* **Digital Matrix**: Retro-console hacker vibe with Matrix Green.
* **Dracula's Castle**: Elegant, soft contrast using Dracula Purple & Pink.

---

## 🛠️ Tech Stack & Architecture

Built with modern Android development practices:
* **Language**: Kotlin
* **UI Framework**: Jetpack Compose (featuring a custom Glassmorphism UI system powered by Haze)
* **Architecture**: MVVM + Clean Architecture
* **Dependency Injection**: Hilt
* **Asynchrony**: Coroutines & Flow
* **Charting**: Vico (v2.0.0-alpha.28)
* **Build System**: Gradle 8.9 (Kotlin DSL)

---

## 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/DeviceInsight.git
   ```
2. **Open the project**: Open the folder in Android Studio (Ladybug or newer recommended).
3. **Sync**: Allow Gradle to sync the project dependencies.
4. **Build & Run**: Deploy to a physical device or emulator (Android 8.0 / API 26+ required).

### Required Permissions
* `PACKAGE_USAGE_STATS`: For process monitoring.
* `SYSTEM_ALERT_WINDOW`: For the HUD overlay functionality.
* `BATTERY_STATS`: For detailed power consumption data.
* *Root access (optional)*: Unlocks advanced process control, FPS monitoring without ADB, and deeper hardware analysis.

---

## 🗺️ Roadmap

We are actively developing DeviceInsight. Our upcoming roadmap includes:

* **Phase 4**: Home Screen Widgets (System overview, RAM/CPU gauges, quick stats).
* **Phase 5**: Advanced Hardware Analysis (Per-core frequencies via `/sys/devices`, Camera 2 API details, storage partition breakdowns).
* **Phase 6**: Root Features (Process killing, advanced GPU stats, native root detection).
* **Phase 7**: Polish & Optimization (Graph optimizations, localization, accessibility).

---

## 🤝 Contribution

Contributions are welcome and appreciated! Whether it's fixing a bug, enhancing the UI, or adding a new feature from the roadmap.
1. Fork the repository.
2. Create a new branch for your feature (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

---
<p align="center"><i>Built with ❤️ by Ivarna</i></p>
