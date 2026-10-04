# NetDiag Utility — Network Diagnostics & Cellular Suite

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-brightgreen.svg)](https://developer.android.com)
[![Tested On](https://img.shields.io/badge/Verified-Android%2014%20%2F%2015%20(Realme%20%7C%20Pixel%20%7C%20Samsung)-blue.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-teal.svg)](https://developer.android.com/jetpack/compose)
[![DI](https://img.shields.io/badge/DI-Hilt%20Dagger-orange.svg)](https://dagger.dev/hilt/)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20%7C%20Zero%20Tracking-success.svg)](#-data-privacy--security-architecture)

**NetDiag Utility** is a carrier-grade network diagnostics and device utility suite built with modern Android architecture and Jetpack Compose (Material 3). It unites high-density baseband telemetry (comparable to NetMonster), multi-stage latency diagnostics, radio mode policy management, a floating OneTap accessibility assistant, and an honest, byte-accurate application cache-cleaning engine.

---

## 🎯 What Problems Does NetDiag Utility Solve?

### 1. The "Fake Cleaner" & Battery Drain Epidemic
Most mobile cleaners on the Play Store rely on deceptive tactics:
- They claim to "boost RAM" by aggressively killing background processes—triggering immediate system respawns that consume CPU cycles and drain battery life.
- They show fake animation sweepers and fabricated megabyte counts.
- **The NetDiag Solution**: Zero fake numbers. NetDiag queries Android's official `StorageStatsManager` to measure real application cache sizes down to the exact byte. It exclusively targets volatile cache directories (`cache/` and `code_cache/`), completely isolating and safeguarding user data, credentials, databases, and media.

### 2. Locked & Inconsistent Baseband Menus
Secret dialer codes like `*#*#4636#*#*` are frequently disabled or locked by OEMs (Samsung, Xiaomi, Realme, carrier-branded devices).
- **The NetDiag Solution**: Provides an instant, carrier-grade radio engineering dashboard. Without root or proprietary codes, users can view live Physical Cell ID (PCI), Tracking Area Code (TAC), 3GPP Band designations (`B3`, `B40`, `n78`), derived eNodeB/gNodeB IDs, Timing Advance distance, and real-time signal quality.

### 3. Data-Hungry Speed Tests vs. True Latency Breakdown
Traditional internet speed tests download 50–150 MB of data to measure peak throughput, quickly consuming cellular data caps while failing to identify why a connection feels laggy.
- **The NetDiag Solution**: Executes lightweight, zero-waste multi-protocol latency probes. In under 1 MB, NetDiag dissects the full connection lifecycle: **DNS Resolution**, **TCP Handshake**, **TLS Negotiation**, and **Time-To-First-Byte (TTFB)**.

### 4. Fragmented Network Tools
Users typically need 3–4 separate apps to inspect cell towers, ping servers, switch network modes, and inspect device storage.
- **The NetDiag Solution**: A single, unified, privacy-respecting engineering suite that unifies all four domains into a cohesive Material 3 design system.

---

## ⚙️ SDE Engineering Pillars: Usability, Maintainability & UI Ergonomics

As a software development engineer (SDE) architecting this system, the codebase was constructed on four core software engineering tenets:

### 1. 🎯 Frictionless Usability (Ease of Use)
- **Zero-Friction Access**: Telemetry is immediately visible upon launch. Users never need to navigate through nested settings trees or expand collapsed accordions to find RSRP, SINR, or Band numbers.
- **Single-Tap Dual SIM State Switching**: A single tap on `[ SIM 1 ]` or `[ SIM 2 ]` re-binds the entire telemetry engine, preventing cross-SIM cognitive confusion.
- **Movable Floating Access**: The `OneTapAccessibilityService` brings live baseband telemetry and cache diagnostic triggers anywhere in the OS via an edge-snapping, draggable overlay.
- **Defensive Confirmation Boundaries**: Destructive actions are guarded by explicit confirmation dialogs that clearly articulate the estimated removable bytes and verify that application databases will remain untouched.

### 2. 🧩 High Maintainability & Modular Architecture
- **Clean Architecture & Strict Separation of Concerns (SoC)**: Business logic, baseband decoding algorithms, and raw storage measurements are decoupled from UI composables.
- **Unidirectional Data Flow (UDF)**: ViewModels emit immutable, strongly-typed `StateFlow<T>` models. Composables emit lambda events back to ViewModels. Zero two-way mutable state leakage.
- **Dependency Inversion via Hilt / Dagger**: All repositories, schedulers, and framework services are injected as singletons. Concrete classes depend on domain abstractions rather than direct HAL calls, ensuring testability.
- **Zero Leaky Framework Abstractions**: Android platform classes (`CellInfoLte`, `CellInfoNr`, `StorageStats`) are immediately mapped into immutable domain models (`CellData`, `SimSubscription`, `AppStorageInfo`) at the repository boundary, shielding the presentation layer from OS deprecations.
- **Robust Code Hygiene & DRY Principles**: Reusable components are centralized in `CommonComponents.kt`; redundant prototype files are purged; hardcoded OEM brand models are replaced with dynamic system queries.

### 3. 🖥️ Clear UI & Visual Ergonomics (Reduced Cognitive Load)
- **High Information Density Without Visual Fatigue**: Inspired by professional RF tools like NetMonster, telemetry is laid out in a calibrated 2×2 grid (`RSRP`, `RSRQ`, `RSSI`, `SINR`) that can be scanned in milliseconds.
- **Calibrated 3GPP Semantic Color Hierarchy**: Diagnostic health is color-coded using established 3GPP thresholds:
  - `StatusGreen` (`#00E676`): Excellent radio reception / successful probe
  - `AccentCyan` (`#00E5FF`): Good reception / primary action highlight
  - `StatusAmber` (`#FFB300`): Fair signal / special access required
  - `StatusRed` (`#FF5252`): Poor signal / packet drop / unsupported operation
- **Monospace Numerical Clarity**: All critical values (decibels, milliseconds, frequencies, byte sizes) are rendered in monospace typography with explicit units (`dBm`, `dB`, `ms`, `MHz`, `MB`), preventing ambiguities.
- **Zero Gimmicks / Deceptive Progress**: No fake spinning gauges, phone "temperature" thermometers, or artificial percent boosters. Progress bars strictly track real completed operations ($X / Y$).

### 4. ⚡ Runtime Performance & Hardware Resilience
- **Non-Blocking Asynchronous Concurrency**: All telephony queries, storage iterations, and network probes are executed off the main thread on `Dispatchers.IO`.
- **Low Memory & CPU Footprint**: The application maintains a lightweight memory footprint (<60 MB RSS) and strictly cancels background coroutines when screens leave the backstack.
- **Defensive Multi-Version Fallbacks**: Built to gracefully handle device quirks and API variations across Android 8.0 through Android 15 (e.g., handling missing CSI metrics on MediaTek HALs or restricted carrier permissions).

---

## 📱 Compatibility & System Requirements

### Android OS Version Support

| Android Version | API Level | Support Status | Notes |
|:---|:---|:---|:---|
| **Android 15** | API 35 | **Full Support (Target)** | Modern 16 KB page size compatible, strict edge-to-edge layout |
| **Android 14** | API 34 | **Full Support (Tested)** | Verified on Realme UI 5.0 (`RMX5116IN`), Pixel, and One UI 6 |
| **Android 13** | API 33 | **Full Support** | Per-app language, runtime notification permissions |
| **Android 12 / 12L**| API 31–32 | **Full Support** | Material You Dynamic Color integration |
| **Android 11** | API 30 | **Full Support** | Enhanced NR NSA/SA telephony callbacks |
| **Android 10** | API 29 | **Full Support** | CellInfoNr baseband metrics |
| **Android 9.0 Pie** | API 28 | **Full Support** | CellIdentity bandwidth metrics |
| **Android 8.0 / 8.1**| API 26–27 | **Minimum Supported** | Base requirements (Adaptive icons, Notification channels) |

### Verified Device Ecosystems
- **Realme & Oppo** (Realme UI / ColorOS — verified on device `c314e283`)
- **Google Pixel** (Stock Android 14/15, Tensor & Qualcomm platforms)
- **Samsung Galaxy** (One UI 5/6, Exynos & Snapdragon platforms)
- **OnePlus** (OxygenOS 13/14)
- **Xiaomi / Redmi / POCO** (HyperOS / MIUI)
- **Motorola & Nothing OS**

### Hardware Prerequisites
- **Cellular Radio**: Baseband modem supporting GSM, UMTS, LTE (4G), or 5G NR (Sub-6 / mmWave).
- **Dual SIM**: Supports Dual SIM Dual Standby (DSDS) and Dual SIM Active (DSDA) hardware configurations.
- **Memory**: Minimum 2 GB RAM (optimized memory footprint < 60 MB at runtime).

---

## 🌟 Key Capabilities

### 1. 📶 NetMonster-Grade Cellular Diagnostics
- **Immediate 2×2 Signal Grid**: `RSRP`, `RSRQ`, `RSSI`, and `SINR` are displayed front-and-center without hidden sub-menus or extra taps.
- **3GPP Calibrated Quality**: Real-time signal level bars (0–4) paired with standardized 3GPP signal quality classifications (`Excellent`, `Good`, `Fair`, `Poor`, or `N/A`).
- **Dual SIM Binding**: Seamlessly toggle between `[ SIM 1 ]` and `[ SIM 2 ]` subscriptions with individual carrier identities and active mobile data markers.
- **Serving Cell & Secondary Carrier**:
  - Automatically decodes EARFCN / NR-ARFCN into 3GPP frequency bands (e.g., `B3 (1800 MHz)`, `B40 (2300 MHz)`, `n78 (3500 MHz)`).
  - Physical Cell ID (PCI), Tracking Area Code (TAC), Cell Identity (ECI / NCI).
  - Derived **eNodeB ID / gNodeB ID** and Sector IDs (`ci shr 8` for LTE, `nci shr 14` for NR).
  - Channel Bandwidth (MHz/kHz), Timing Advance (with approximate distance in meters), and CQI.
  - Secondary carrier telemetry for **5G NSA** (`NR + LTE Anchor`).
- **Network Identifiers**: Real-time inspection of `MCC`, `MNC`, `PLMN Numeric` (e.g., `405861`), Country ISO, and Roaming state.
- **IMS & Advanced Voice**: Real hardware indicators for **IMS Registration**, **VoLTE**, **VoWiFi / IWLAN**, and **Video Calling (ViLTE)**.
- **Neighbour Cells**: Compact list sorted by **Signal**, **Technology**, **PCI**, or **Frequency**.
- **Configurable Live Polling**: Live update ticker supporting `Auto (2s)`, `1 sec`, `2 sec`, `5 sec`, and `Manual` refresh modes.
- **Technical Framework Drawer**: Deep-dive framework inspector for raw CellInfo objects, timestamps (ns), and measurement ages.

### 2. ⚡ Multi-Protocol Ping & Latency Diagnostics
- **HTTP / Application Ping**: Measures full connection lifecycle including DNS resolution, TCP handshake, TLS negotiation, Time-To-First-Byte (TTFB), and response transfer time.
- **TCP Socket Ping**: Raw socket connection latency against any destination host and port.
- **ICMP Ping**: Supported low-level echo checks where permitted by the platform.
- **Diagnostics History & Statistics**: Tracks packet loss percentage, min/avg/max latency, and historical session logs.

### 3. 🧹 Real Application Cache Cleaner
- **Zero Fake Numbers**: Inspects exact application cache and data allocations using Android's `StorageStatsManager` and `PACKAGE_USAGE_STATS`.
- **Safe & Non-Destructive**: Never deletes user documents, photos, or media, and strictly isolates volatile cache from persistent application data.
- **Storage Overview**: Visual device storage breakdown (`Used` vs `Free` vs `Removable Cache`).
- **Granular Sorting & Filtering**: Sort apps by Cache Size, App Name, or Total Size. Filter by `All`, `Large cache (>10 MB)`, `Selected`, or `Ignored`.
- **App Storage Details**: Deep inspection per app with dedicated actions for cache cleaning and exclusion management.
- **Batch & Selected Cleaning**: Confirmation dialogs, real-time progress indicators, and comprehensive cleanup result summaries.
- **Honest Capability Detection**: Distinguishes between automated clearing via Accessibility, system-restricted boundaries, and settings fallback.

### 4. 🎛️ OneTap Floating Utility
- **Accessibility Integration**: Draggable floating shortcut pill with edge snapping and elevation.
- **Quick Action Menu**: Instant access to live cellular telemetry, ping latency tests, dual SIM details, system radio settings, and the Cache Cleaner from any application.

### 5. 🎨 Design & Aesthetic System
- **Engineering Dark Palette**: Deep navy canvas (`#0C1017`), elevated surfaces (`#141A23`, `#1C2430`), and electric teal/cyan primary accents (`#00E5FF`).
- **Clean Daylight Light Palette**: High-contrast, clean daylight theme for bright outdoor environments.
- **Dynamic Color Support**: Adapts to Android 12+ wallpaper-extracted Material You palettes.
- **Custom AI Brand Identity**: Custom vector radio cell tower logo with pulse waves on deep navy.

---

## 🔐 Permissions & Access Architecture

NetDiag Utility operates with complete transparency regarding system privileges. Permissions are requested contextually and verified in real time:

| Permission | Category | Why It Is Needed | If Denied |
|:---|:---|:---|:---|
| **`ACCESS_FINE_LOCATION`** | Runtime | **Cellular Tower IDs**: Android security architecture couples baseband tower identifiers (`PCI`, `TAC`, `ECI`, `CellInfo`) to Location permissions because tower IDs can physically pinpoint a device. | Baseband RF metrics cannot be read; general connectivity still functions. |
| **`READ_PHONE_STATE`** | Runtime | **Multi-SIM Subscriptions**: Required to read active subscription cards, carrier names, slot indices, and network registration states. | Multi-SIM selector defaults to slot 0 with generic carrier labels. |
| **`PACKAGE_USAGE_STATS`** | Special App Access | **Real Cache Sizes**: Required by Android's `StorageStatsManager` to inspect byte-accurate app storage breakdown (Cache, Data, App Code) without faking. | Cache Cleaner displays a permission setup card; fake numbers are never shown. |
| **`SYSTEM_ALERT_WINDOW`** | Special App Access | **OneTap Overlay**: Enables the draggable floating shortcut pill above third-party applications. | OneTap floating pill is disabled; in-app diagnostic features work normally. |
| **`BIND_ACCESSIBILITY_SERVICE`** | System Service | **OneTap Assistant**: Allows the floating trigger pill to capture touch events and provides assisted navigation to storage settings. | Floating overlay cannot be launched; all manual app features work. |
| **`INTERNET`** | Normal | **Latency Diagnostics**: Required to execute HTTP and TCP socket latency probes against target hosts. | Ping and latency tests will fail. |
| **`ACCESS_NETWORK_STATE`** | Normal | **Transport Observation**: Identifies whether the active connection is Wi-Fi, Cellular, Ethernet, or VPN, and checks internet validation. | Automatic transport identification is degraded. |

---

## 🛡️ Data Privacy & Security Architecture

### 1. 100% On-Device & Zero Remote Tracking
- **Zero Analytics SDKs**: NetDiag contains no Firebase Analytics, no Facebook Graph SDK, no Google Analytics, and no marketing trackers.
- **No User Account Required**: The app functions completely without registration, email logins, phone numbers, or user profiling.
- **Zero Network Telemetry**: Cellular telemetry measurements (PCI, TAC, Cell ID, signal power, neighbour cells) remain strictly in volatile device memory (RAM) and are **never uploaded** to any server or commercial tower database.

### 2. Strict Non-Destructive Storage Operation
- **Volatile Target Isolation**: Cache cleaning is restricted to operating-system cache partitions (`Context.getCacheDir()` and `Context.getCodeCacheDir()`).
- **Data Protection Guarantee**: The cleaner will **never**:
  - Delete user photos, videos, audio, or gallery files.
  - Delete downloads or documents.
  - Clear user databases, session tokens, or app login credentials.
  - Uninstall applications or alter operating system configurations.

### 3. Accessibility Service Privacy Policy
The optional `OneTapAccessibilityService` complies with strict privacy guidelines:
- **No Keystroke Logging**: It does not inspect text input fields, read passwords, or log user typing.
- **No Screen Scraping**: It does not capture or transmit on-screen content or private messages.
- **Zero Autonomous Destructive Actions**: It never clicks buttons, changes settings, or deletes files without explicit, direct user initiation.

---

## 🏗️ High-Level System Architecture

NetDiag Utility is engineered around **Clean Architecture** principles and **Unidirectional Data Flow (UDF)**. Business logic, telemetry decoders, and storage analyzers are cleanly decoupled from the Jetpack Compose presentation layer:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER (COMPOSE)                              │
│                                                                                        │
│   NetworkScreen       PingScreen        CacheCleanerScreen     DashboardScreen         │
│   ToolsScreen         TestsScreen       OneTapOverlayPill      AppNavigation           │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ User Intents & UI Events
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              VIEWMODEL LAYER (STATEFLOW)                               │
│                                                                                        │
│   NetworkViewModel    PingViewModel     CacheViewModel         DashboardViewModel      │
│   • Multi-SIM State   • Latency Streams • Storage Scan State   • Health Calculation    │
│   • Live Refresh Job  • Probe Aggregator• Batch Cleanup Queue  • Onboarding Evaluator  │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Domain Invocations (Coroutines)
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN & REPOSITORY LAYER                                 │
│                                                                                        │
│   TelephonyRepository    NetworkRepository     PingRepository      AppPreferencesRepo  │
│   • 3GPP Band Decoding   • Interface Validator • DNS / TCP / TLS   • DataStore Proto   │
│   • eNB / gNB Math       • Routing / DNS State • TTFB Measurement  • Theme / OneTap    │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │ Android Framework Queries
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                         ANDROID PLATFORM & HARDWARE SUBSYSTEMS                         │
│                                                                                        │
│   TelephonyManager       ConnectivityManager   StorageStatsManager  WindowManager      │
│   SubscriptionManager    LinkProperties        StatFs / AppOps      Accessibility      │
│   CellInfo (LTE / 5G NR) NetworkCapabilities   UserHandle           Overlay Service    │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Architectural Layer Responsibilities

#### 1. Presentation Layer (`ui/`)
- **Declarative & Stateless**: Built exclusively with Jetpack Compose and Material 3 design tokens.
- **Unidirectional Data Flow (UDF)**: Composables only observe immutable `StateFlow` state and emit lambda events back to ViewModels.
- **Single Activity Architecture**: All navigation transitions occur within `MainActivity` hosted by `AppNavigation.kt` (`NavHost`, `ModalNavigationDrawer`, and segmented bottom bar).

#### 2. ViewModel Layer (`ui/*/ViewModel.kt`)
- **Lifecycle Aware**: Survives configuration changes and manages asynchronous coroutine jobs (`viewModelScope`).
- **Reactive State Holders**: Exposes read-only `StateFlow<T>` streams to the UI, backed by internal `MutableStateFlow<T>`.
- **Hardware Isolation**: Manages continuous background telemetry polling loops (with user-selected refresh rates) without blocking the Android UI thread.

#### 3. Domain & Repository Layer (`data/repository/`, `domain/model/`)
- **Data Normalization**: Transforms heterogeneous, raw framework objects (`CellInfoLte`, `CellInfoNr`, `StorageStats`) into clean, strongly-typed Kotlin domain models (`CellData`, `SimSubscription`, `AppStorageInfo`).
- **Mathematical Decoders**: Houses pure mathematical calculations, including 3GPP channel-to-band conversions (`RadioBandUtils`), eNodeB / gNodeB derivations, and timing advance propagation estimates.

#### 4. Android Platform & Subsystem Services
- **Multi-SIM Telephony Engine**: Binds to individual SIM slots using `telephonyManager.createForSubscriptionId(subId)` to prevent cross-SIM data contamination.
- **Byte-Accurate Storage Engine**: Interacts with Android's `StorageStatsManager` and `StatFs` to query real partition byte sizes.
- **Floating Window Subsystem**: Manages the draggable overlay pill using `WindowManager` and Android's `AccessibilityService` APIs.

---

## 💻 Technology Stack Summary

- **Language**: Kotlin 2.0
- **UI Toolkit**: Jetpack Compose with Material 3 Design Tokens
- **Dependency Injection**: Hilt / Dagger (Singletons & ViewModels)
- **Concurrency & Reactivity**: Kotlin Coroutines & `StateFlow`
- **Navigation**: Jetpack Compose Navigation (`NavHost`, `ModalNavigationDrawer`, Segmented Tabs)
- **Data Persistence**: AndroidX DataStore Preferences (`AppPreferencesRepository`)
- **System Telephony**: `TelephonyManager`, `SubscriptionManager`, `CellInfoLte`, `CellInfoNr`, `CellSignalStrength`
- **Storage & Diagnostics**: `StorageStatsManager`, `StatFs`, `ConnectivityManager`
- **Accessibility Service**: `AccessibilityService` for overlay window management

---

## 📁 Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/netdiag/tool/
│   ├── NetDiagApp.kt                  # Application class with @HiltAndroidApp
│   ├── data/
│   │   └── repository/
│   │       ├── Repositories.kt        # Network, Telephony, Ping Repositories & RadioBandUtils
│   │       └── AppPreferencesRepository.kt # DataStore preferences (Theme, OneTap, Onboarding)
│   ├── di/
│   │   └── AppModule.kt               # Hilt DI bindings for Singletons
│   ├── domain/
│   │   └── model/
│   │       ├── Models.kt              # Ping, Cellular, SIM, Radio RAT, and Transition models
│   │       └── CacheModels.kt         # AppStorageInfo, CacheScanSummary, CleanupResult models
│   ├── service/
│   │   └── OneTapAccessibilityService.kt # Floating overlay pill & quick actions
│   └── ui/
│       ├── MainActivity.kt            # Single Activity entry point
│       ├── cache/
│       │   ├── CacheCleanerScreen.kt  # Full Cache Cleaner UI & sheets
│       │   └── CacheViewModel.kt      # StorageStatsManager scanner & clean orchestrator
│       ├── components/
│       │   └── CommonComponents.kt   # Reusable KeyValueRow, EmptyState, NetworkStatusCard
│       ├── dashboard/
│       │   ├── DashboardScreen.kt     # Technical overview hub
│       │   └── DashboardViewModel.kt
│       ├── navigation/
│       │   └── AppNavigation.kt       # 4-destination bottom bar, drawer, and routes
│       ├── network/
│       │   ├── NetworkScreens.kt      # NetMonster-grade cellular screen & mode screen
│       │   └── NetworkViewModel.kt    # Dual-SIM binding, live ticker & neighbor sorting
│       ├── onboarding/
│       │   ├── SplashScreen.kt        # Animated AI brand logo launch
│       │   ├── WelcomeScreen.kt
│       │   ├── FeatureSlidesScreen.kt
│       │   ├── PermissionChecklistScreen.kt # Live permission validator
│       │   └── OneTapSetupScreen.kt
│       ├── ping/
│       │   ├── PingScreen.kt          # HTTP / TCP latency runner
│       │   └── PingViewModel.kt
│       ├── settings/
│       │   └── SettingsScreen.kt      # Appearance theme & diagnostics options
│       ├── tests/
│       │   └── TestsScreen.kt         # Segmented Ping / Checkup / History hub
│       ├── theme/
│       │   ├── Color.kt               # Navy/Cyan engineering palette & semantic tokens
│       │   ├── Theme.kt               # Material 3 Light/Dark/Dynamic scheme
│       │   └── Type.kt                # Typography tokens
│       └── tools/
│           └── ToolsScreen.kt         # Utilities hub (OneTap, Mode, Cache Cleaner)
└── res/
    ├── drawable/
    │   └── app_logo.png               # AI-generated brand launcher icon
    ├── values/
    │   ├── colors.xml
    │   ├── strings.xml
    │   └── themes.xml
    └── xml/
        └── accessibility_service_config.xml
```

---

## 🛠️ Build & Installation

### Prerequisites
- **Android Studio**: Iguana (2023.2.1) or newer
- **JDK**: OpenJDK 17 or 21
- **Android SDK**: Compile SDK `35` / Target SDK `35` / Min SDK `26` (Android 8.0)

### Building via Command Line

```bash
# Clone the repository
git clone https://github.com/your-username/netdiag-utility.git
cd netdiag-utility

# Build Debug APK
./gradlew assembleDebug

# Install on connected device via ADB
./gradlew installDebug
```

---

## 🛡️ Safety & Integrity Guarantees

1. **No Artificial / Fake Data**: If a modem or OEM firmware does not report a metric (e.g., CSI-SINR or neighbour cells), the UI displays `N/A`. It never fabricates numbers.
2. **Non-Destructive Storage Operation**: Cache cleaning strictly targets temporary cache directories. User data, logins, downloads, and media files are never removed.
3. **Respect for Platform Boundaries**: System-restricted actions (such as direct radio band locking without `MODIFY_PHONE_STATE`) are honestly communicated, and users are routed to native Android carrier settings.
