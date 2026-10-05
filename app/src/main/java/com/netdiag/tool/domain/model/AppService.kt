package com.netdiag.tool.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class AppService(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val keywords: List<String>,
    val route: String,
    val icon: ImageVector
) {
    companion object {
        val ALL_SERVICES = listOf(
            AppService(
                id = "speed_test",
                title = "High-Speed Throughput Test",
                category = "TESTING",
                description = "Measure real-time ping, jitter, download & upload bandwidth with live speedometer gauge",
                keywords = listOf("speed", "download", "upload", "bandwidth", "mbps", "throughput", "gauge", "fast", "internet"),
                route = "speed_test",
                icon = Icons.Outlined.Speed
            ),
            AppService(
                id = "ping_diagnostics",
                title = "Ping & Latency Diagnostics",
                category = "TESTING",
                description = "Real-time HTTP/TCP ping with oscilloscope waveform chart and packet stream matrix",
                keywords = listOf("ping", "latency", "rtt", "jitter", "packet loss", "waveform", "oscilloscope", "socket"),
                route = "tests",
                icon = Icons.Outlined.NetworkCheck
            ),
            AppService(
                id = "cell_telemetry",
                title = "Cellular Signal & Tower Distance",
                category = "CELLULAR",
                description = "Carrier-grade 2G/3G/4G/5G RF metrics (RSRP, SINR, RSSI) and live Tower Distance via Timing Advance",
                keywords = listOf("cell", "tower", "distance", "timing advance", "rsrp", "rsrq", "sinr", "2g", "3g", "4g", "5g", "gsm", "lte", "nr"),
                route = "network",
                icon = Icons.Outlined.CellTower
            ),
            AppService(
                id = "sim_identity",
                title = "SIM Security & IMSI Vault",
                category = "CELLULAR",
                description = "Masked IMSI, MSISDN phone number, ICCID and PLMN with privacy eye reveal toggles",
                keywords = listOf("imsi", "msisdn", "iccid", "phone number", "sim", "security", "privacy", "carrier", "plmn", "mcc", "mnc"),
                route = "sim_identity",
                icon = Icons.Outlined.Badge
            ),
            AppService(
                id = "device_telemetry",
                title = "Device & RAM Telemetry",
                category = "SYSTEM",
                description = "Live RAM memory usage, internal storage, CPU processor cores, battery health & Android OS version",
                keywords = listOf("device", "ram", "memory", "storage", "cpu", "battery", "hardware", "android version", "kernel", "temperature"),
                route = "device_info",
                icon = Icons.Outlined.PhoneAndroid
            ),
            AppService(
                id = "network_mode",
                title = "Network Mode Policy",
                category = "RADIO",
                description = "Inspect and select preferred radio access technology policies (LTE Only, 5G SA/NSA, WCDMA)",
                keywords = listOf("network mode", "4g only", "5g only", "lte", "rat", "band", "radio policy", "nr"),
                route = "network_mode",
                icon = Icons.Outlined.SettingsInputAntenna
            ),
            AppService(
                id = "cache_cleaner",
                title = "Storage & Cache Cleaner",
                category = "CLEANER",
                description = "Scan app caches, leftover APKs, thumbnail junk and free up device internal storage",
                keywords = listOf("cache", "clean", "junk", "storage", "space", "cleaner", "ram", "temp files"),
                route = "cache_cleaner",
                icon = Icons.Outlined.CleaningServices
            ),
            AppService(
                id = "onetap_overlay",
                title = "OneTap Floating Quick Switcher",
                category = "UTILITY",
                description = "Instant floating bubble overlay to switch networks and run diagnostics with one tap",
                keywords = listOf("onetap", "floating", "bubble", "overlay", "shortcut", "widget", "accessibility"),
                route = "onetap_setup",
                icon = Icons.Outlined.TouchApp
            )
        )
    }
}
