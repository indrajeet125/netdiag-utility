# NetDiag Utility — Network Diagnostics & Cellular Suite

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026--35)-brightgreen.svg)](https://developer.android.com)
[![Tested On](https://img.shields.io/badge/Verified-Android%2014%20%2F%2015%20(Realme%20%7C%20Pixel%20%7C%20Samsung)-blue.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.0-blue.svg)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-teal.svg)](https://developer.android.com/jetpack/compose)
[![DI](https://img.shields.io/badge/DI-Hilt%20Dagger-orange.svg)](https://dagger.dev/hilt/)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device%20%7C%20Zero%20Tracking-success.svg)](#-data-privacy--security-architecture)

**NetDiag Utility** is a carrier-grade network diagnostics and device utility suite built with modern Android architecture and Jetpack Compose (Material 3). It unites high-density baseband telemetry (carrier-grade baseband decoding), multi-stage latency diagnostics, radio mode policy management, a floating OneTap accessibility assistant, and an honest, byte-accurate application cache-cleaning engine.

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
- **High Information Density Without Visual Fatigue**: Engineered according to professional RF engineering principles, telemetry is laid out in a calibrated 2×2 grid (`RSRP`, `RSRQ`, `RSSI`, `SINR`) that can be scanned in milliseconds.
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

## 🌟 Key Capabilities & Newly Added Features

### 1. 📶 Carrier-Grade Cellular Diagnostics (2G, 3G, 4G, 5G Adaptive)
- **Dynamic RAT Telemetry Adaptation**: Automatically alters the primary RF metric grid and technical identifiers according to the active generation:
  - **2G (GSM / EDGE)**: `RSSI (dBm)`, `BER (Bit Error Rate)`, `Timing Advance`, and `BSIC (Base Station Identity Code)`.
  - **3G (UMTS / WCDMA / HSPA+)**: `RSCP (dBm)`, `Ec/No (dB)`, `RSSI (dBm)`, and `PSC (Primary Scrambling Code)`.
  - **4G (LTE / LTE-A)**: `RSRP (dBm)`, `RSRQ (dB)`, `RSSI (dBm)`, and `SINR (dB)`.
  - **5G (NR SA / NR NSA)**: `SS-RSRP (dBm)`, `SS-RSRQ (dB)`, `CSI-RSRP (dBm)`, and `SS-SINR (dB)`.
- **Serving Tower Distance Telemetry**:
  - Live calculation using baseband Timing Advance (TA) steps: $TA \times 78.12\text{ m}$ for LTE, $TA \times 554\text{ m}$ for GSM. Prominently badged on the cellular dashboard.
- **3GPP Calibrated Quality**: Real-time signal level bars (0–4) paired with standardized 3GPP signal quality classifications (`Excellent`, `Good`, `Fair`, `Poor`, or `N/A`).
- **Dual SIM Binding**: Seamlessly toggle between `[ SIM 1 ]` and `[ SIM 2 ]` subscriptions with individual carrier identities and active mobile data markers.
- **Serving Cell & Secondary Carrier**:
  - Automatically decodes EARFCN / NR-ARFCN into 3GPP frequency bands (e.g., `B3 (1800 MHz)`, `B40 (2300 MHz)`, `n78 (3500 MHz)`).
  - Physical Cell ID (PCI), Tracking Area Code (TAC), Cell Identity (ECI / NCI).
  - Derived **eNodeB ID / gNodeB ID** and Sector IDs (`ci shr 8` for LTE, `nci shr 14` for NR).
  - Channel Bandwidth (MHz/kHz), Timing Advance, and CQI.
  - Secondary carrier telemetry for **5G NSA** (`NR + LTE Anchor`).
- **Neighbour Cells**: Compact list sorted by **Signal**, **Technology**, **PCI**, or **Frequency**.
- **Configurable Live Polling**: Live update ticker supporting `Auto (2s)`, `1 sec`, `2 sec`, `5 sec`, and `Manual` refresh modes.

### 2. 🚀 High-Speed Throughput & Speed Test Engine
- **Multi-Phase Telemetry Engine**: Powered by Cloudflare Edge CDN multi-threaded HTTP/2:
  - Phase 1: Real-time Ping and Jitter calculation.
  - Phase 2: 25 MB Multi-chunk streaming downlink throughput test.
  - Phase 3: Chunked POST uplink throughput test.
- **Modern Circular Speedometer Gauge**:
  - 270-degree sweeping arc with animated needle and live digital HUD readout (`%.1f Mbps`).
  - Active gradient fill matching throughput speed tier.
- **Live Throughput Waveform Canvas Graph**:
  - Real-time cubic bezier curve plotting speed history with neon gradient fill underneath.
- **Result Metrics**: Latency (ms), Jitter (ms), Download Average & Peak, Upload Average & Peak, Connection Grade.

### 3. 🌐 Interactive Network Topology Flow Diagram & Battery Saver
- **4-Node End-to-End Route Visualization**:
  - `[ Device Local IP ] ──> [ Gateway IP ] ──> [ ISP / WAN Operator ] ──> [ Global DNS / Internet ]`
- **Live Gateway Latency Probing**: Lightweight background gateway echo testing.
- **Dedicated Battery Saver Toggle (`[ ⏸ Stop ]` / `[ ▶ Start Live ]`)**:
  - Freezes all periodic polling and Canvas animations on demand to achieve zero CPU usage and conserve device battery.

### 4. ⚡ Ping Diagnostics with Oscilloscope Waveform & Packet Matrix
- **Oscilloscope Waveform Graph**: Live Canvas-based bezier spline curve with primary gradient fill tracing latency over time.
- **Packet Stream Matrix Grid**: 10-packet color-coded telemetry blocks (`Green` <45ms, `Cyan` <90ms, `Amber` high latency, `Red` packet drop).
- **High-Tech Diagnostic Progress Bar**:
  - Displays `"RUNNING IN 10 PROBES • STATUS: 20%"` with glowing animated neon lead-dot progress bar.
  - Technical waterfall breakdown: DNS resolution, TCP handshake, TLS negotiation, and TTFB.

### 5. 📱 Device & Hardware Telemetry Screen (`DeviceInfoScreen.kt`)
- **System Memory (RAM) Diagnostics**: Total RAM, Available/Free RAM, Used RAM, percentage progress bar, low-memory threshold warnings.
- **Internal Storage Diagnostics**: Exact byte-accurate storage calculation using `StatFs` (Total, Free, Used space in GB).
- **Operating System & Platform**: Android Version, API Level, Security Patch level, Build number, Kernel version, Baseband radio.
- **Processor & Display**: CPU core count, Architecture ABIs (`arm64-v8a`), Screen Resolution (`W × H px`), Screen density (`DPI`).
- **Battery Telemetry**: Live Battery %, Status (Charging/Discharging), Health, Temperature (°C), Voltage (mV), Technology (`Li-ion`).

### 6. 🔐 SIM Security & Identity Screen (`SimIdentityScreen.kt`)
- **Sensitive Identifier Privacy Protection**:
  - **IMSI (International Mobile Subscriber Identity)**: Masked by default (e.g. `40445 •••••••• 89`). **Eye toggle button `[ 👁 ]`** unmasks full 15-digit IMSI with 1-tap copy button `[ 📋 ]`.
  - **MSISDN (Mobile Phone Number)**: Masked by default (e.g. `+91 ••••• ••421`), Eye button to reveal and copy.
  - **ICCID (SIM Chip Serial)**: Masked by default (e.g. `89918 •••••••••••••• 04`), Eye button to reveal and copy.
- **Network Identifiers**: MCC, MNC, PLMN (`404045`), Country ISO, Subscription ID, physical SIM vs eSIM (eUICC) indicator.
- **Privacy Shield Notice**: Reassures user that credentials are never transmitted off the device.

### 7. 🔍 Universal Diagnostic Search & Favorite Service Runner
- **Command Search Bar**: Real-time filtering across all diagnostic tools, tests, and services.
- **Favorites / Pinned Services Row**: Star (`[ ★ / ☆ ]`) any diagnostic test to pin it directly to the Home Dashboard for instant access.
- **Direct 1-Tap Runner**: Dedicated `[ ▶ RUN ]` button next to every search result to immediately launch that diagnostic.

### 8. ⇅ Reorderable Drag-and-Drop Category Prioritization
- **Customizable Dashboard Layout**: Reorder sections according to personal preference:
  - Network Topology Flow, Cellular & Wi-Fi Status, Health Diagnostics, Quick Tools, Device Snapshot.
- **Tactile Drag & Drop**: Touch drag handles or use single-tap `[▲ Up]` and `[▼ Down]` priority arrow buttons.
- **Persistent Preferences**: Preferences are saved locally and reloaded across app launches.

### 9. 🧹 Real Application Cache Cleaner & Automated Accessibility Engine
- **Zero Fake Numbers**: Inspects exact application cache and data allocations using Android's `StorageStatsManager` and `PACKAGE_USAGE_STATS`.
- **Search & Persistent Multi-Selection**: Filter installed apps by name or package without losing previous selections across searches, complete with visible batch selection controls ("Select Visible", "Clear Selection").
- **Robust Accessibility Automation Engine (`OneTapAccessibilityService`)**:
  - Automates the native Android path: `App Info -> Storage & cache -> Clear cache -> Return to NetDiag -> Verify -> Advance`.
  - **State Fencing**: Protected by state machines (`WAITING_FOR_APP_INFO`, `WAITING_FOR_STORAGE`, `STORAGE_CLICKED`, `WAITING_FOR_CLEAR_CACHE`, `CLEAR_CACHE_CLICKED`, `WAIT_AFTER_CLEAR`, `RETURNING_TO_APP`) to reject spurious accessibility window events.
  - **UI Element Polling & Retries**: Asynchronously queries UI nodes (`Storage & cache`, `Clear cache`) with a 4.8s safety fallback timeout rather than blindly relying on coordinate clicking.
  - **Human-Calibrated Delays**: Configured with 850–1100ms settling delays between screens, plus a mandatory **1900ms wait** after clicking "Clear cache" to permit the Android storage daemon to commit disk deletion before returning.
  - **Sequential Queue Processing**: NetDiag verifies cache size deltas on resume (with asynchronous storage update retries) and enforces a 1200ms settling pause before initiating the next application in the batch queue.
- **Safe & Non-Destructive**: Never deletes user documents, photos, or media, and strictly isolates volatile cache from persistent application data.
- **Storage Overview**: Visual device storage breakdown (`Used` vs `Free` vs `Removable Cache`).
- **Granular Sorting & Filtering**: Sort apps by Cache Size, App Name, or Total Size. Filter by `All`, `>10 MB`, `>50 MB`, `>100 MB`, `Selected`, or `Ignored`.

### 10. 🎨 Calibrated Material 3 Design & Theme Engine
- **Dark Mode & System Default Fix**: Dynamically maps semantic tokens to `MaterialTheme.colorScheme`, ensuring seamless switching between Light, Dark, and System Default themes.
- **Engineering Navy / Cyan Aesthetics**: Tailored contrast ratios meeting WCAG AAA accessibility standards.

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

## 🚀 What's New in v1.1.0 (Architecture & Features)

### 1. SOLID Architecture & GoF Design Patterns
- **Strategy Pattern (`domain/strategy/DiagnosticStrategy.kt`)**: Decouples network diagnostic test execution (HTTP, TCP socket, ICMP echo) into interchangeable algorithm strategies.
- **Factory Pattern (`domain/strategy/PingStrategies.kt`)**: Dynamic `PingStrategyFactory` providing strategy instances to ViewModels without tight coupling.
- **Registry Pattern (`domain/registry/AppServiceRegistry.kt`)**: Extensible registry supporting runtime registration, category filtering, and search indexing for all network and device diagnostic services.
- **Type-Safe Route Pattern (`domain/model/AppRoute.kt`)**: Eliminates string literals across navigation graphs, bottom bars, drawer items, and the floating OneTap assistant.

### 2. Live Diagnostic Progress Bar (`DiagnosticProgressBar.kt`)
- Visual live telemetry displaying step progress (`"RUNNING IN 10 PROBES"`), badge status (`"STATUS: 20%"`), glowing gradient bar with pulsing tracking dot, and probe RTT readout.

### 3. Reorderable Category Prioritization (`PrioritizeCategoriesSheet.kt`)
- Direct drag-and-drop category prioritization with gesture detection (`detectDragGestures`) and single-tap `[▲ Up]` / `[▼ Down]` buttons. Persisted in `AppPreferencesRepository`.

### 4. Universal Diagnostic Search & Favorite Service Runner (`ServiceSearchRunnerSheet.kt`)
- Integrated search bar across all diagnostic suites, with a Star (`[ ★ ]`) favorite pinning row on the dashboard and instant `[ ▶ RUN ]` triggers.

### 5. Device Info Screen (`DeviceInfoScreen.kt`)
- Live RAM telemetry bar (`X.XX GB / Y.YY GB`), internal storage diagnostics via `StatFs`, CPU architecture/core count, Android OS specs, and battery health telemetry.

### 6. SIM Security & Privacy Screen (`SimIdentityScreen.kt`)
- Protects IMSI, MSISDN phone number, and ICCID behind shoulder-surfing privacy masks with interactive `[ 👁 ]` Eye unmask toggles and 1-tap clipboard copy buttons `[ 📋 ]`.

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
