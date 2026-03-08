# Changelog

All notable changes to AppControlX.

## [4.0.0] - 2026-03-09 (Native UI Migration)

### 🎉 Complete Native Migration
This is a complete migration from WebView/React to full native Kotlin with Jetpack Compose.

### Added
- **Jetpack Compose UI** - 100% native UI with Material Design 3
  - No WebView, no React, no JavaScript bridge
  - Direct Kotlin/Compose for maximum performance
- **Setup Screen** - 4-step wizard (Welcome, Mode, Theme, Ready)
- **Dashboard Screen** - System stats cards with modals
  - CPU, RAM, Storage, Battery, Network, Display
  - Device info with processor mapping
- **App List Screen** - Full-featured app management
  - Search, filters (User/System/All)
  - Selection mode with batch actions
  - App detail bottom sheet
  - Action confirmation dialogs
- **Tools Screen** - Hidden settings launcher
  - Quick actions (Activity Launcher, App Info)
  - Categorized hidden settings
- **Activity Launcher Screen** - Launch any activity
  - Search and filter apps
  - Expandable activity list
- **Settings Screen** - App configuration
  - Execution mode selection (Root/Shizuku/None)
  - Theme toggle (Light/Dark/System)
  - Action history with rollback
  - App information
- **About Screen** - App info and credits
- **Theme System** - Light/Dark/System modes with dynamic colors
- **Navigation** - Compose Navigation with type-safe routes

### Changed
- **Architecture** - Migrated from WebView/React to Jetpack Compose
- **MainActivity** - Now uses ComponentActivity with Compose setContent
- **State Management** - StateFlow in ViewModels (no more Zustand)
- **Dependency Injection** - Hilt for all ViewModels
- **Data Persistence** - DataStore for preferences

### Removed
- **WebView** - No longer needed
- **NativeBridge** - JavaScript bridge removed
- **React UI** - Entire web folder removed
- **appcompat** - Replaced with Compose
- **activity_main.xml** - Layout XML removed

### Technical Details
- 100% offline, no network calls
- All native methods preserved (ShellManager, AppScanner, etc.)
- Material Design 3 components throughout
- Edge-to-edge display support
- Dark mode with dynamic color support

---

## [3.1.0] - 2026-02-07 (Performance Optimization)

### 🚀 Major Performance Improvements

#### Performance Metrics
- **Startup Time**: 87% faster (1500ms → <200ms)
- **App List Load**: 78% faster (2300ms → <500ms)
- **Memory Usage**: 67% reduction (180MB → ~60MB)
- **Bundle Size**: 40% smaller (850KB → 510KB)
- **Monitor Updates**: 10x faster (2000ms → 200-300ms)

### Added
- **Lazy Icon Loading** - Icons load on-demand as you scroll (IntersectionObserver)
  - New `getAppIcon(packageName)` native method for individual icon loading
  - 70% memory reduction on app list
  - Progressive loading for smooth UX
- **Real-time Monitor Consolidation** - Unified fast monitor for CPU frequencies + temperatures
  - `startRealtimeMonitor()` - 200ms interval for CPU frequencies, CPU temp, GPU temp
  - `stopRealtimeMonitor()` - Stop real-time monitoring
- **Route Code Splitting** - Lazy load all non-critical pages
  - Dashboard loads instantly
  - Other pages (Apps, Tools, Settings, etc) load on-demand
  - 40% reduction in initial bundle size
- **LazyAppIcon Component** - Smart icon loading with viewport detection
- **useDebounce Hook** - 150ms debouncing for search input (smooth 60fps)
- **SkeletonPage Component** - Instant skeleton UI during route transitions
- **Icon Cache Store** - `appIcons` map in state for loaded icons

### Changed
- **AppScanner.scanAllApps()** - Now accepts `includeIcons: Boolean = false` parameter
  - Skips icon loading by default for 5x faster scan
  - Only loads metadata (name, package, version, size, etc)
  - Icons loaded separately via `getAppIcon()`
- **System Monitor Intervals** - Ultra-fast real-time updates
  - Real-time monitor: 400ms → **200ms** (CPU/GPU temps)
  - System stats monitor: 2000ms → **300ms** (RAM/Storage/Battery)
- **App Initialization** - Async non-blocking startup
  - System stats load in parallel with Promise.all
  - App list loads asynchronously (setTimeout)
  - Dashboard appears instantly with skeleton
- **App List Component** - Uses LazyAppIcon for progressive loading
- **App Detail Sheet** - Uses LazyAppIcon for detail view
- **Dashboard** - Memoized computations for app counts and frequencies
  - `useMemo` for userApps count
  - `useMemo` for systemAppsCount
  - `useMemo` for frequency array
- **Search Filtering** - Debounced for smooth typing (150ms delay)

### Optimized
- **IntersectionObserver** - Icons load 100px before entering viewport
- **App Store** - Non-blocking initialization pattern
- **Memoization** - Expensive filter/count operations cached
- **Code Splitting** - React.lazy() + Suspense for all routes
- **Cache Strategy** - 30s TTL for app list, persistent icon cache

### Technical Details
- All optimizations are **100% offline** (no network calls)
- Web layer remains design + UI only
- Backend remains 100% Kotlin native
- All 23 native methods intact and functional
- Zero features removed, only performance improved

---

## [3.0.0] - 2026-02-07 (v3 UI Rewrite)

### 🎉 Complete UI Rewrite
This is a complete UI rewrite with React + TypeScript hybrid architecture.

### Added
- **Solarized Light Theme** - Beautiful cream-colored light theme (#FDF6E3) as default
- **Premium Dark Theme** - Purple accent dark mode with glassmorphism
- **Dashboard Modals** - Clickable cards with detailed info:
  - Memory Modal (RAM + ZRAM usage)
  - Storage Modal (Apps, System, Available breakdown)
  - Display Modal (GPU, Resolution, Refresh rate)
  - Battery Modal (Health, Temperature, Voltage, Remaining time)
  - Network Modal (WiFi/Mobile status, Signal strength)
- **Real-time CPU Monitoring** - 400ms polling for live CPU frequency display
- **Enhanced Device Info** - Comprehensive device information with processor name mapping:
  - Device model, brand, and marketing name
  - Processor name (Snapdragon 8 Gen 4, etc.) with SoC model mapping
  - Android version with codename (Baklava, Vanilla Ice Cream, etc.)
  - Uptime and deep sleep time with percentage
  - GPU name detection from SoC model
  - ZRAM usage monitoring
  - Storage filesystem detection (f2fs, ext4)
  - Network info (WiFi SSID, speed, signal strength, mobile type)
- **Action History with Rollback** - View past actions and rollback freeze/unfreeze operations
- **Execution Mode Selection** - Choose between Root, Shizuku, or View-Only from Settings
- **Access Loss Detection** - Automatic detection and warning when Root/Shizuku access is lost
- **Setup Wizard** - Guided first-time setup with theme selection
- **Functional Settings Modals**:
  - Execution Mode selector
  - Accent Color info
  - Animations toggle
  - Notifications toggle
  - App Information

### Changed
- **Architecture** - Migrated to React + TypeScript + Tailwind CSS in WebView
- **Theme System** - CSS variables with `:root` and `.dark` class switching
- **State Management** - Zustand with persist middleware for theme/settings
- **Button Styling** - Rounded corners (0.75rem) with proper active states
- **Dashboard** - No more hardcoded values, all data from native bridge
- **App List** - Removed "isRunning" detection (unreliable on modern Android)

### Removed
- Legacy Jetpack Compose UI
- Running app detection badge and filter
- Mock data in production builds (only available in dev mode)

### Fixed
- Theme not applying correctly on first load
- Text colors not adapting to light/dark theme
- Click highlight color showing wrong color
- Build errors with unused imports

---

## [2.0.0] - 2026-01 (v2 Rewrite)

### 🎉 Complete Rewrite
This is a complete rewrite of AppControlX with modern architecture and new features.

### Added
- **Dashboard** - System monitoring with real-time updates
  - CPU usage and temperature
  - Battery status and temperature
  - RAM and Storage usage
  - Network status
  - Display info (resolution, refresh rate)
  - GPU info (requires root)
  - Device info with uptime and deep sleep time
- **Setup Wizard** - Guided first-time setup with mode selection
- **Mode Loss Detection** - Automatic detection when Root/Shizuku access is lost
- **Display Refresh Rate Control** - Set min/max refresh rate (Root/Shizuku)
- **Feature Quick Access Cards** - Navigate to features from Dashboard
- **Batch Progress UI** - Visual progress during batch operations

### Changed
- **Architecture** - Complete rewrite with clean MVVM + Hilt DI
- **App Detection** - More accurate using dumpsys + PackageManager
- **Material 3** - Updated to latest Material Design 3
- **Navigation** - Bottom navigation with Dashboard, Apps, Settings

---

## [1.1.0] - 2025-12

### Added
- Showcase website (index.html) with responsive design, 3 themes, image gallery
- Expanded background ops viewer in app detail
- Other Projects backlinks section in website

### Fixed
- App info sheet stacking bug
- Duplicate Activity Launcher in Tools layout
- ProGuard rules for Rollback/ActionLog Gson serialization

### Changed
- Autostart Manager now supports 13 OEM brands

---

## [1.0.0] - 2025-12

### Added
- Setup wizard with mode selection (Root/Shizuku/View-Only)
- App list with User/System app filter
- Batch selection and operations with progress tracking
- Freeze/Unfreeze, Force Stop, Background Restriction
- Clear Cache/Data, Uninstall, Launch App
- Activity Launcher with search
- Action Logs & Rollback
- Settings with theme/language selection
- Safety validation for system apps

### Platform Support
- Root mode via libsu
- Shizuku mode with UserService
- View-Only mode for browsing

---

## Architecture (v3)

```
┌─────────────────────────────────────┐
│     React UI (WebView)              │
│     TypeScript + Tailwind           │
├─────────────────────────────────────┤
│     JavaScript Bridge               │
│     @JavascriptInterface            │
├─────────────────────────────────────┤
│     Kotlin Native Layer             │
│     libsu + Shizuku                 │
└─────────────────────────────────────┘
```

### Web Stack
- React 18
- TypeScript 5
- Vite 5
- Tailwind CSS 3
- Zustand (State)
- Recharts (Graphs)
- Lucide React (Icons)

### Native Stack
- Kotlin 1.9
- Hilt DI
- libsu 5.2.2
- Shizuku-API 13.1.5
- kotlinx.serialization

## Tech Requirements

- Android 10+ (API 29+)
- Target SDK 34 (Android 14)
- Root access (Magisk/KernelSU) OR Shizuku
