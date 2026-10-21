# NetDiag Utility

**Network tests, cell & SIM details, and a one-tap cache cleaner — in one Android app.**

[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026--35)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-teal.svg)](https://developer.android.com/jetpack/compose)
[![No ads or analytics](https://img.shields.io/badge/Ads%20%2F%20Analytics-None-success.svg)](#-privacy)

NetDiag replaces three apps you'd normally install separately:

| Instead of… | NetDiag gives you |
|---|---|
| A **ping / speed test** app | Speed test, ping (HTTP / TCP / ICMP), DNS, port, website and traceroute tests, with history |
| **NetMonster** or a hidden `*#*#4636#*#*` menu | Live signal (RSRP / RSRQ / SINR), serving & neighbour cells, bands, 4G/5G (NSA/SA), dual-SIM, VoLTE status |
| A **one-tap cache cleaner** | Real per-app cache sizes and automatic "Clear cache" through Android Settings |

No root, no ads, no accounts, no analytics.

---

## Contents
- [Features](#-features)
- [Install](#-install)
- [Permissions](#-permissions)
- [Privacy](#-privacy)
- [What Android doesn't allow](#-what-android-doesnt-allow)
- [Build from source](#-build-from-source)
- [Project structure](#-project-structure)
- [Changelog](#-changelog)

---

## ✨ Features

The app has four tabs — **Home · Tests · My Phone · Tools** — plus **Settings** (⚙). On tablets, foldables and in landscape the tabs move to a side rail.

### Home
- **Check my connection** — one tap runs a full checkup (network, router, DNS, internet, HTTP/HTTPS) and shows what's wrong.
- **Connection status** — Wi-Fi or mobile data, signal and carrier at a glance.
- Shortcuts to Speed test, Ping and Signal; your last test result.
- **Search** any tool and **pin favourites**; reorder Home sections.

### Tests
| Test | What it tells you |
|---|---|
| **Speed Test** | Download, upload, ping and jitter (via Cloudflare), with a live graph |
| **Ping** | HTTP / TCP / ICMP latency, broken down into DNS → connect → TLS → first byte; live latency graph and packet-loss grid |
| **Full Checkup** | Step-by-step check of every link from phone to internet |
| **Website · DNS · Port · Traceroute** | Reachability of a site, DNS resolution, open ports, and the route to a host |
| **History** | Every past result, repeatable with one tap |

### My Phone
- **Signal & cells** — RSRP, RSRQ, RSSI, SINR with quality colours; serving and neighbour cells with PCI, TAC, cell ID, eNodeB/gNodeB, band (e.g. `B3 1800 MHz`, `n78 3500 MHz`) and tower distance from timing advance. Adapts to 2G / 3G / 4G / 5G.
- **5G mode** — Non-Standalone (NSA) vs Standalone (SA).
- **Voice** — what calls use right now (VoLTE, VoNR, Wi-Fi calling, or 2G/3G fallback), what your carrier supports, and a **"last call" check measured during a real call**.
- **Dual SIM** — switch between SIM 1 and SIM 2.
- **Device** — RAM, storage, CPU, display, Android version, battery health and temperature.

### Tools
- **Cache Cleaner** — real cache sizes per app (from Android's storage stats, no made-up numbers). Select apps and NetDiag opens each app's Storage page and taps **Clear cache** for you. Works in any phone language. Never touches app data, logins, photos or files.
- **OneTap bubble** — a floating button to reach NetDiag's tools from any app.
- **IP Information** — public IPv4 / IPv6, ISP and location, **NAT type** (router NAT, carrier-grade NAT, 464XLAT), every network including **VoLTE / IMS** when visible, DNS, gateway, MTU, and **how each IP was assigned** (DHCP, carrier APN, SLAAC, VPN).
- **Signal & SIM** — full cell view; **SIM Identity** with IMSI, phone number and ICCID masked until you tap to reveal.
- **Subnet calculator** and **Network mode (4G/5G)** shortcut.

### Settings
- Light / dark / system theme, Material You colours.
- **Permissions** — see every permission's status and **Allow** or **Revoke** it.

---

## 📲 Install

NetDiag isn't on the Play Store yet. Install the APK directly:

1. Get `netdiag-v1.2.0-debug.apk` (or [build it](#-build-from-source)).
2. Open it on your phone and allow **Install unknown apps** for the app you opened it from.
3. Launch NetDiag and grant permissions when a feature asks for them — each request explains why.

**Turning on the cache cleaner / OneTap (Android 13+):** because the app was installed from an APK, Android blocks its Accessibility switch with *"App was denied access to this setting"*. To allow it:
1. Try the Accessibility switch once (so Android shows that message).
2. Open **Settings → Apps → NetDiag**, tap **⋮** (top right) → **Allow restricted settings**.
3. Turn on **Accessibility → OneTap Utility**.

Installing over USB (`./gradlew installDebug`) avoids this restriction.

---

## 🔐 Permissions

Every permission is asked for only when a feature needs it, with an explanation. You can review or revoke them anytime in **Settings → Permissions**.

| Permission | Used for | Without it |
|---|---|---|
| **Phone** (`READ_PHONE_STATE`) | SIM list, carrier, network type, voice status | SIM details and dual-SIM switching unavailable |
| **Phone numbers** (`READ_PHONE_NUMBERS`) | Your SIM's phone number (Android 11+) | Number shown as unavailable |
| **Location** + the phone's **Location switch** | Android only reveals cell towers (PCI, TAC, signal) with location access, and only while Location is on | Cell and signal details empty; everything else works |
| **Usage access** (special) | Reading per-app cache sizes | Cache Cleaner can't list sizes |
| **Accessibility** (special) | Tapping "Storage" → "Clear cache" in Settings for you; the OneTap bubble | Clear cache manually; OneTap off |
| **Display over other apps** (special) | The OneTap floating bubble | Bubble unavailable |
| Internet, network & Wi-Fi state, app list | Tests, IP info, listing apps for the cleaner | — |

The Accessibility service only reacts to Settings screens during a cleanup **you** started. It doesn't read what you type or what other apps show.

---

## 🛡 Privacy

- **No ads, analytics, trackers or accounts.**
- **Cell, SIM and device details never leave the phone.** Test history and settings are stored only on the device.
- **What does go online — only when you run that feature:**

| Feature | Contacts |
|---|---|
| Speed Test | `speed.cloudflare.com` |
| IP Information (public IP, ISP, location) | `api4.ipify.org`, `api6.ipify.org`, `ipv4.icanhazip.com`, `ipinfo.io` |
| Checkup / connectivity checks | `dns.google`, `1.1.1.1`, `connectivitycheck.gstatic.com`, `www.google.com` |
| Ping, Port, Website, DNS, Traceroute | The host you enter (default `google.com`) |

---

## ⚠ What Android doesn't allow

NetDiag shows **only real values** — when Android doesn't provide something, the app says so instead of guessing.

- **Full IMSI** — Android 10+ only gives it to system/carrier apps. NetDiag shows the public network part (MCC + MNC); full IMSI on Android 9 and older.
- **Phone number** — only available if your carrier stored it on the SIM.
- **IMS / VoLTE registration** — no public API for normal apps. NetDiag uses carrier settings, the live voice network, and a check during real calls instead.
- **Cell data with Location off** — Android returns nothing; NetDiag offers to open Location settings.
- **Silent cache clearing** — only system apps can clear other apps' caches directly, so NetDiag automates the official Settings screen.
- **Network mode / band locking** — needs system permissions; NetDiag links to the right Android settings.

---

## 🛠 Build from source

**Requirements:** Android Studio (Ladybug or newer), JDK 17, Android SDK 35.

```bash
git clone https://github.com/indrajeet125/netdiag-utility.git
cd netdiag-utility

./gradlew assembleDebug     # APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug      # install on a phone connected with USB debugging
```

| | |
|---|---|
| Min / target SDK | 26 (Android 8.0) / 35 |
| Kotlin / AGP | 2.0.21 / 8.7.3 |
| Compose BOM | 2024.12.01 |
| Hilt | 2.51.1 |

> **Windows:** keep `kotlin.compiler.execution.strategy=in-process` in `gradle.properties`; the daemon strategy can time out on low-memory machines.

---

## 🗂 Project structure

Single-activity app, Jetpack Compose UI, MVVM with `StateFlow`, Hilt for dependency injection.

```
app/src/main/java/com/netdiag/tool/
├── data/repository/        Telephony, network, ping, preferences and test-history repositories
├── domain/
│   ├── model/              Data models, routes, dashboard sections, service registry entries
│   ├── registry/           Searchable list of all tools
│   └── strategy/           Ping strategies (HTTP / TCP / ICMP)
├── service/                OneTapAccessibilityService — floating bubble + cache-clearing automation
└── ui/
    ├── navigation/         Tabs, side rail, routes and back-stack rules
    ├── dashboard/          Home
    ├── tests/ ping/ speed/ history/   Tests tab
    ├── device/ network/    My Phone: device info, signal & cells, SIM identity, voice
    ├── tools/ cache/       Tools: IP info, subnet calculator, cache cleaner
    ├── settings/           Settings and the Permissions manager
    ├── onboarding/         First-run walkthrough and permission checklist
    ├── components/         Shared UI: permission prompt, charts, cards, search sheet
    └── theme/              Colours and typography
```

---

## 📝 Changelog

See **[CHANGELOG.md](CHANGELOG.md)** for what changed in each version (v1.0.0 → v1.2.0).
