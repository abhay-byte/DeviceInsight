# FPS Monitoring Implementation

This document details the technical implementation and resolution of the FPS monitoring feature in DeviceInsight.

## Initial Problem
The FPS (Frames Per Second) counter was consistently reporting `0`. This was due to several factors:
1.  **Android 15 Compatibility**: Traditional methods of finding the "Current Focus" via `dumpsys window` have changed, making it difficult to identify the active window/layer.
2.  **Layer Name Instability**: SurfaceFlinger layer names on modern Android often include dynamic numeric suffixes (e.g., `com.package/activity#1234`) that change every time the app is restarted.
3.  **Rendering Pipeline**: Many games (OpenGL/Vulkan) bypass the standard Android HWUI pipeline, rendering `dumpsys gfxinfo` ineffective for them.

## Technical Solution

### 1. Foreground Package Detection
Instead of relying on `mCurrentFocus`, we now parse `mFocusedApp` from `dumpsys window`.
- **Command**: `dumpsys window | grep mFocusedApp`
- **Regex**: Extracts the package name from `ActivityRecord{... u0 com.package/activity ...}`.

### 2. Hybrid FPS Calculation Strategy
We implemented a two-tier approach to ensure compatibility with both standard apps and high-performance games.

#### Tier 1: SurfaceFlinger Latency (Priority)
Used for games and apps with dedicated rendering surfaces (SurfaceView or BLAST layers).
- **Discovery**: Searches `dumpsys SurfaceFlinger --list` for layers matching the package name, prioritizing `BLAST` and `SurfaceView` types.
- **Data Collection**: Executes `dumpsys SurfaceFlinger --latency '<layer_name>'`.
- **Parsing**: 
    - The command returns a circular buffer of the last 127 frames.
    - We parse the `ActualPresentTime` (second column).
    - We count frames where `ActualPresentTime` is within the last 1 second, relative to `System.nanoTime()`.
- **Refresh Rate Support**: Accurate for 60Hz, 90Hz, and 120Hz displays.

#### Tier 2: GfxInfo Framestats (Fallback)
Used for standard UI-driven applications.
- **Command**: `dumpsys gfxinfo <package> framestats`.
- **Parsing**: Processes the CSV output and filters `IntendedVsync` timestamps from the last 1,000,000,000 nanoseconds.

### 3. Verification Process
- **Logs**: Added specific debug logging in `FpsMonitor` (`"GfxInfo FPS for..."` and `"SurfaceFlinger FPS for..."`).
- **ADB Validation**: Verified using `adb logcat | grep FpsMonitor` while switching between the dashboard and other apps (e.g., Termux:X11, which uses a BLAST layer).

## Files Modified
- [app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsMonitor.kt](../app/src/main/java/com/ivarna/deviceinsight/data/fps/FpsMonitor.kt): Core logic for command execution and parsing.
- [app/src/main/java/com/ivarna/deviceinsight/data/repository/DashboardRepositoryImpl.kt](../app/src/main/java/com/ivarna/deviceinsight/data/repository/DashboardRepositoryImpl.kt): Integrated the FPS monitor into the metrics flow.

## Future Considerations
- **Shizuku Integration**: Ensure the app has sufficient permissions to run `dumpsys` commands without manual ADB intervention after initial setup.
- **Polling Optimization**: Fine-tune the 1-second interval to minimize CPU impact while maintaining accuracy.
