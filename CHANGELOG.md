# Changelog

All notable changes to **NetDiag Utility**, version by version.
NetDiag combines three tools in one app: a **ping / network test tool**, a **NetMonster-style cell & SIM monitor**, and a **one-tap cache cleaner**.

---

## v1.2.0 — in progress (branch `upgrade/v1.2.0`, versionCode 3)

### Easier to use
- **One navigation system**: side drawer removed; everything lives under the bottom bar **Home · Tests · My Phone · Tools**. Settings is the ⚙ icon.
- **Home**: one big **"Check my connection"** button (opens and runs the full checkup) plus Speed test / Ping / Signal shortcuts; plain-language section names.
- **Tests**: everyday tests first (Speed Test, Ping, Full Checkup), advanced after (Website, DNS, Port, Traceroute, History).
- **Tools**: adds Signal & SIM and SIM Identity; duplicate permission/settings cards removed.
- **Responsive layout**: side navigation rail and centred content on tablets, foldables and landscape; Speed Test gauge scales on small phones; buttons grow with large system font instead of clipping.
- **Cleaner look**: normal sans-serif text (monospace only for numbers), larger small text, softer colours that read well in light and dark mode, fainter borders.
- **Smoother graphs**: Ping and Speed Test use a smooth curve with eased scaling; Ping scrolls as a sliding window.

### New features
- **IP Information**: public IPv4 + IPv6, ISP and location, NAT type (router NAT, carrier-grade NAT, 464XLAT); every network incl. **VoLTE / IMS** when visible, with all addresses, gateway, DNS, Private DNS, MTU, NAT64; plain-language **"how this IP was assigned"** (DHCP, carrier APN, SLAAC, VPN); all network interfaces.
- **Network test suite**: Port, Website, DNS and Traceroute tests, IP tools and **test history**.
- **Permission pop-ups**: before any operation that needs a permission, a dialog explains what is missing and why, then requests it or opens the right Settings page.
- **Settings → Permissions**: every permission with live status and **Allow / Revoke** (Android 13+ self-revoke; Settings page otherwise).
- **Honest VoLTE / VoNR / Wi-Fi calling**: carrier support from carrier config, live voice network ("4G LTE (VoLTE)" or "3G — calls don't use VoLTE"), and **last-call confirmation** measured during real calls.
- **SIM Identity**: reads the phone number (MSISDN) with the Phone numbers permission; shows the public MCC+MNC part of the IMSI where Android 10+ blocks the rest.
- **Location switch check**: explains that serving-cell data needs the phone's Location turned on.

### Fixes
- Crash opening Network / Device Info (ViewModel initialisation order).
- Crash tapping Home's Checkup / "View all" links (missing `tests?tab=` route).
- **Back stack**: Back from any tab returns to Home; screens no longer open twice; the OneTap bubble no longer restarts the app.
- **OneTap cleaner**: works in any phone language / brand (labels read from the phone's Settings app), no longer skips apps mid-batch, about 2× faster per app.
- Accessibility **"App was denied access"** on Android 13+: the pop-up explains *Allow restricted settings*.
- Cache Cleaner keeps selections across app search; accessibility clearing flow stabilised.
- Onboarding completion is remembered.
- Wrong LTE (B26 / B28) and 5G (n77 / n79 / n261) band names.
- MSISDN unreadable on Android 11+.

### Battery
- OneTap accessibility service listens only to window changes (was every UI event in every app).
- Live cell refresh "Auto" every 5 s (was 2 s); live monitoring stops when the screen is not visible.
- Connection Path card starts paused; router ping every 10 s.

---

## v1.1.0 — 2026-10-06 (tag `v1.1.0`, on `main`)

### New features
- **Speed Test**: download / upload / ping / jitter with a circular speedometer and live throughput graph.
- **Device & Hardware screen**: RAM, storage, CPU, display, Android version and battery details.
- **SIM Security & Identity screen**: IMSI, phone number and ICCID masked by default, with reveal and copy.
- **Network Topology** card: Device → Router → ISP → Internet route.
- **Ping**: live progress bar, oscilloscope-style latency graph and packet matrix.
- **Dashboard**: reorder sections (drag or ▲ / ▼) and remember the order.
- **Search & favourites**: search every tool, pin favourites to Home, run with one tap.

### Under the hood
- SOLID architecture with Strategy (ping types), Factory and Registry patterns; type-safe routes.

---

## v1.0.0 — 2026-10-05 (initial release)

- **Cellular diagnostics** (NetMonster-style): serving and neighbour cells, PCI / TAC / Cell ID, LTE & 5G bands, RSRP / RSRQ / SINR, dual-SIM support.
- **Ping tool**: HTTP, TCP and ICMP latency with DNS / connect / TLS / first-byte breakdown.
- **Cache Cleaner**: real per-app cache sizes (no fake numbers) with accessibility-assisted "Clear cache".
- **OneTap floating bubble**: quick access to tools from any app.
- Material 3 design with light / dark themes.
