# DeviceInsight - Premium UI/UX Design Specification

## 🎨 Design Philosophy: "Elegant Glassmorphism"

DeviceInsight aims to be the most visually stunning system monitor on Android. The design language combines **Glassmorphism**, **Neomorphism** accents, and **Vibrant Gradients** to create a feeling of depth and premium quality.

### Core Principles
1.  **Depth & Translucency**: Use blurred backgrounds (`backdrop-filter: blur`), semi-transparent surfaces, and subtle gradients to create layering.
2.  **Vibrant & Neon**: Metrics should "pop" against dark backgrounds using neon accent colors (Cyan, Magenta, Electric Green).
3.  **Fluid Motion**: All value changes should animate (e.g., gauge needles swooping, numbers counting up).
4.  **OLED Optimized**: All themes (except specialized ones) are rooted in deep blacks to look stunning on AMOLED screens.

---

## 🖌️ Theme System

The app features 5 distinct premium themes to suit different aesthetics.

### 1. Tech-Noir (Default)
*   **Vibe**: Futuristic, High-Tech, Cybernetic.
*   **Primary**: `Cyber Blue #00E5FF`
*   **Secondary**: `Electric Purple #D500F9`
*   **Background**: Deep Black
*   **Gradients**: Blue-to-Purple subtle linear gradients.

### 2. Cyberpunk Edge
*   **Vibe**: Night City, Neon, Aggressive.
*   **Primary**: `Neon Yellow #FCEE0A`
*   **Secondary**: `Neon Blue #00F0FF`
*   **Background**: Dark Blue-Black
*   **Accents**: Hot Pink details.

### 3. Deep Ocean
*   **Vibe**: Calming, Professional, Clean.
*   **Primary**: `Ice Blue #4FC3F7`
*   **Secondary**: `Cyan Accent #00E5FF`
*   **Background**: Navy Blue (`#011627`)
*   **Elements**: Frosted glass containers.

### 4. Digital Matrix
*   **Vibe**: Hacker, Retro-Console, Terminal.
*   **Primary**: `Matrix Green #00FF41`
*   **Secondary**: `Dark Green #008F11`
*   **Background**: Purer Black
*   **Font**: Monospace preference for data.

### 5. Dracula's Castle
*   **Vibe**: Elegant, Soft, Contrast.
*   **Primary**: `Dracula Purple #BD93F9`
*   **Secondary**: `Smooth Pink #FF79C6`
*   **Background**: Dark Grey-Blue (`#282A36`)
*   **Surface**: Soft Grey.

---

## 🧩 Component Library

### GradientCard
*   **Visuals**: Using a specialized wrapper that applies a subtle vertical gradient (White 5% -> Transparent) and a border stroke.
*   **Corner Radius**: `24.dp` for large containers, `16.dp` for small items.
*   **Interaction**: Scale down slightly (98%) on touch.

### Premium Circular Gauge
*   **Design**: Uncapped arcs (270 degrees).
*   **Animation**: Smooth interpolation for value updates (1000ms tween).
*   **Glow**: Option to add a shadow/glow behind the arc matching the primary color.

### Quick Metric Grid
*   **Layout**: Balanced grid of small glass cards.
*   **Content**: Icon (Top Left), Value (Center/Bold), Label (Bottom).

---

## 📱 Screen Specifications

### 1. Dashboard
*   **Header**: Glass sticky header with blurred background.
*   **Hero Section**: Two large 50% width gauges (CPU & RAM) side-by-side.
*   **Secondary**: Full-width card for GPU & Temp details.
*   **Grid**: 3-column grid for Battery, Storage, Network.
*   **Decor**: Subtle radial gradient glow in the top-right corner matching the active theme.

### 2. Settings
*   **Theme Picker**: Horizontal scrolling list or Grid of circular theme previews.
*   **Switching**: Instant transition using `animateColorAsState` for smooth cross-fading.
*   **OLED Mode**: Toggle to force background to pure `#000000` regardless of theme.

---

## 🏃 Animation Guidelines

*   **Page Transitions**: Slide + Fade (Standard Android 14+ predictive back style).
*   **Data Updates**: Never snap value 0 -> 50. Always animate linearly over 300-500ms.
*   **Charts**: Graphs should "fill" from the left or fade in upward.

---
*This document serves as the single source of truth for all UI decisions.*
