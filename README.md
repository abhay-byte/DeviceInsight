# DeviceInsight - Android System Monitor & Task Manager

<div align="center">

**A comprehensive Android system monitoring application with advanced hardware introspection and performance overlay capabilities.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

## 📱 Overview

DeviceInsight is a powerful system monitoring tool for Android that provides deep visibility into your device's hardware, running processes, and real-time performance metrics. Unlike basic task managers, DeviceInsight offers comprehensive hardware introspection similar to desktop tools like CPU-Z, GPU-Z, and Windows Task Manager combined.

### Key Features

- **📊 Advanced Task Manager**: Monitor all running processes with CPU, RAM, and network usage per process
- **💻 Comprehensive Hardware Info**: Detailed CPU, GPU, Memory, Display, and Sensor information
- **🎮 Performance Overlay**: Customizable HUD for real-time monitoring during gaming
- **📈 Real-time Graphs**: 30-second historical tracking for CPU, GPU, Memory, and Power consumption
- **🔧 Deep System Introspection**: OpenGL/Vulkan extensions, cache configuration, kernel details
- **🎨 Modern UI**: Material Design 3 with dark theme optimized for OLED displays

## 🎯 Problem Statement

Android lacks a comprehensive system monitoring solution that combines:
- Windows Task Manager-like process control
- Deep hardware specifications (CPU-Z/GPU-Z equivalent)
- Performance overlay (RivaTuner/MSI Afterburner equivalent)

DeviceInsight fills this gap by providing all three in a single, optimized application.

## ✨ Features

### Dashboard
- **System Health Overview**: CPU, RAM, GPU usage with circular gauges
- **Power Consumption**: Real-time power draw monitoring with 30s graph
- **Quick Metrics**: Battery, Storage, Network, Display refresh rate
- **Device Info**: Model, Android version, uptime

### Task Manager
- **Process Monitoring**: View all user apps, system services, and background processes
- **Resource Usage**: Per-process CPU, RAM, and network consumption
- **Process Control**: Force stop, kill process (root), view app info
- **Search & Filter**: Quick search with filters for user/system/all processes
- **Swipe Actions**: Intuitive swipe gestures for quick actions

### Hardware Information (7 Tabs)

#### CPU Tab
- Utilization graph (30s history)
- Per-core frequency monitoring (current/max MHz)
- Architecture details (ARM64, core clusters)
- Cache configuration (L1/L2/L3)
- CPU governor information
- ARM Neon support

#### GPU Tab
- GPU utilization graph (requires root)
- Frequency monitoring (requires root)
- OpenGL ES information with 97+ extensions
- Vulkan API details with features and extensions
- Memory heaps configuration
- Driver version and build info

#### Memory Tab
- Memory usage graph (30s history)
- System summary (processes, packages, services count)
- RAM information (Total, Available, Swap)
- Storage details (Total, Free, partitions)

#### Screen Tab
- Display metrics (resolution, DPI, physical size)
- Refresh rate (current and max)
- HDR support (Dolby Vision, HDR10, HLG, HDR10+)
- Wide color gamut support

#### OS Tab
- Android version and API level
- Security patch date
- Root access status
- Firmware and build information
- Kernel version and architecture
- Java VM version
- System uptime

#### Hardware Tab
- Battery (level, voltage, current, temperature, health, capacity)
- Connectivity (5G, WiFi speed/frequency, Bluetooth, NFC)
- Camera modules (resolution, aperture, focal length)
- Audio & Media (Widevine level, supported codecs)
- Peripherals (biometrics, USB OTG, SIM slots)
- Security information

#### Sensors Tab
- Sensor capabilities (15/23 sensors supported)
- Real-time sensor data (Accelerometer, Gyroscope, etc.)
- Live X/Y/Z/W values
- Vendor and power consumption info

### Performance Overlay
- Customizable HUD for gaming/monitoring
- Configurable metrics (FPS, CPU, GPU, RAM, Temp, Network)
- Style customization (size, opacity, color, position)
- Live preview
- Minimal performance overhead

### Settings
- Theme selection (System, Dark, OLED Black)
- Temperature unit (Celsius/Fahrenheit)
- Update interval configuration
- Permission management
- About and licenses

## 🏗️ Architecture

- **Pattern**: MVVM (Model-View-ViewModel)
- **UI**: Jetpack Compose with Material Design 3
- **DI**: Hilt (Dagger)
- **Navigation**: Jetpack Navigation Component
- **Concurrency**: Kotlin Coroutines + Flow
- **Charts**: Vico Charts Library

## 🛠️ Tech Stack

- **Language**: Kotlin 2.1.0
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Gradle**: 9.2.0
- **Compose BOM**: 2024.12.01

### Dependencies
- Jetpack Compose
- Material Design 3
- Hilt for Dependency Injection
- Navigation Component
- Kotlin Coroutines
- Vico Charts
- Accompanist Permissions
- DataStore Preferences

## 📋 Permissions

### Required
- `PACKAGE_USAGE_STATS` - Monitor running processes
- `SYSTEM_ALERT_WINDOW` - Display performance overlay
- `FOREGROUND_SERVICE` - Overlay service
- `ACCESS_NETWORK_STATE` - Network monitoring
- `ACCESS_WIFI_STATE` - WiFi information

### Optional
- Root access - For advanced GPU monitoring and process control

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Android SDK 26+
- Gradle 9.2.0+

### Building

1. Clone the repository
```bash
git clone https://github.com/ivarna/deviceinsight.git
cd deviceinsight
```

2. Open in Android Studio

3. Sync Gradle files

4. Run on device or emulator (API 26+)

### Development

```bash
# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

## 📂 Project Structure

```
app/
├── src/main/
│   ├── java/com/ivarna/deviceinsight/
│   │   ├── data/              # Data layer (repositories, data sources)
│   │   ├── domain/            # Business logic (use cases, models)
│   │   ├── presentation/      # UI layer (Compose screens, ViewModels)
│   │   │   ├── dashboard/
│   │   │   ├── tasks/
│   │   │   ├── hardware/
│   │   │   ├── overlay/
│   │   │   ├── settings/
│   │   │   ├── components/
│   │   │   └── theme/
│   │   ├── service/           # Overlay service
│   │   ├── util/              # Utilities
│   │   └── di/                # Dependency injection
│   └── res/                   # Resources
└── docs/                      # Documentation
```

## 📖 Documentation

- [Problem Statement](docs/problem_statement.md)
- [Feature List](docs/feature_list.md)
- [UI/UX Design](docs/ui_ux_design.md)
- [Implementation Plan](.gemini/antigravity/brain/*/implementation_plan.md)

## 🎨 Design

- **Theme**: Tech-Noir / Professional Dark Mode
- **Colors**: 
  - Cyber Blue (#00E5FF) - CPU/Processing
  - Electric Purple (#D500F9) - GPU/Graphics
  - Amber (#FFC400) - RAM/Memory
  - Signal Red (#FF1744) - Critical/High Load
  - Success Green (#00E676) - Normal/Healthy
- **Typography**: Inter/Roboto for UI, JetBrains Mono for data
- **Motion**: 60/90/120fps animations

## 🔄 Development Status

### Phase 1: Project Setup ✅
- [x] Project structure
- [x] Gradle configuration
- [x] Theme and design system
- [x] Navigation setup

### Phase 2: Dashboard (In Progress)
- [ ] Data layer implementation
- [ ] CPU/RAM/GPU monitoring
- [ ] Power consumption tracking
- [ ] Quick metrics grid

### Phase 3-7: Upcoming
- [ ] Task Manager
- [ ] Hardware tabs (7 tabs)
- [ ] Performance overlay
- [ ] Settings
- [ ] Polish & optimization

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 👨‍💻 Developer

**Ivarna**
- Company: [Ivarna](https://ivarna.com)
- Package: `com.ivarna.deviceinsight`

## 🙏 Acknowledgments

- Material Design 3 by Google
- Vico Charts Library
- Android Jetpack Libraries
- Kotlin Coroutines

---

<div align="center">
Made with ❤️ by Ivarna
</div>
