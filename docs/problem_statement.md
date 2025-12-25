# Problem Statement: The Gap in Android System Monitoring and Task Management

## 1. Introduction
While Android has matured into a sophisticated operating system powering billions of devices, its tools for detailed system monitoring and process management remain surprisingly limited compared to desktop counterparts like Windows. Power users, developers, and enthusiasts often simply lack visibility into what is effectively happening "under the hood" of their device.

## 2. Absence of a Comprehensive Task Manager
Unlike the **Windows Task Manager**, which provides a centralized control hub for the entire system, Android lacks a native, user-accessible equivalent.

### Current Limitations:
- **Limited Visibility**: The standard "Recents" menu only displays user-facing activities, completely hiding background services, system daemons, and hidden processes.
- **"Running Services" Inaccessibility**: While Developer Options contains a "Running Services" view, it is buried deep within settings, has a clunky UI, and offers limited interaction capabilities.
- **Process Control**: Users cannot easily view the resource consumption (CPU, RAM, Network) of individual processes or forcibly terminate specific services/programs effectively.

### The Need:
A robust Task Manager that lists:
- All running user applications.
- Background services and system processes.
- PID, Uptime, and User owners for each process.
- Real-time resource usage per process.

## 3. Fragmented System Statistics
Gaining a complete picture of an Android device's health and specifications currently requires installing multiple disparate applications. There is no unified "Device Manager" that exposes deep hardware and software insights.

### Required Modules:
- **CPU**: Real-time per-core usage, clock frequencies, governor settings, and thermal throttling status.
- **GPU**: Real-time load, frequency, memory usage, and supported renderers (Vulkan/OpenGL).
- **RAM**: Detailed breakdown of Active, Cached, Free, and ZRAM/Swap usage.
- **Storage**: Partition-level details (`/system`, `/data`, `/vendor`), I/O read/write speeds, and flash memory health.
- **Sensors**: Raw data output from accelerometer, gyroscope, magnetometer, proximity, light, and barometer sensors.
- **Hardware Info**: Display pane technology/refresh rate, Battery health/cycles/voltage, Camera sensor capabilities, and SoC architecture.
- **Software Info**: Android version, API level, Kernel info, Build ID, and partition schemes (A/B slots).
- **Security**: SELinux status (Enforcing/Permissive), Root access detection, Bootloader lock status, and Security Patch levels.

## 4. Lack of Overlay Performance Monitoring (OSD)
PC gamers and overclockers have long relied on tools like **RivaTuner Statistics Server (RTSS)** or **MSI Afterburner** to monitor system performance while in-game.

### The Problem on Android:
- **Missing OSD**: Android lacks a standard, high-performance overlay that displays critical metrics (FPS, CPU Temp, GPU Load, RAM) directly on top of fullscreen applications.
- **Intrusive Alternatives**: Existing solutions are often resource-heavy, have obstructive UIs, or require complex setups (Root/ADB) just to show basic frame rates.

### The Solution:
A minimal, highly configurable "Heads-Up Display" (HUD) overlay that:
- Floats over any application or game.
- Shows real-time metrics with minimal performance overhead.
- Is visually unobtrusive (customizable opacity, size, position).
