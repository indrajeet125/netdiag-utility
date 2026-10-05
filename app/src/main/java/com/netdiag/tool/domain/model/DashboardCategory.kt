package com.netdiag.tool.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.graphics.vector.ImageVector

enum class DashboardCategory(
    val id: String,
    val title: String,
    val subtitle: String
) {
    TOPOLOGY(
        id = "topology",
        title = "Network Topology Flow",
        subtitle = "Live interactive route from Device IP to ISP Gateway & DNS"
    ),
    NETWORK_STATUS(
        id = "network_status",
        title = "Cellular & Wi-Fi Status",
        subtitle = "Carrier telemetry, live 2G-5G RF metrics & Tower distance"
    ),
    HEALTH_CHECKUP(
        id = "health_checkup",
        title = "Connection Health Diagnostics",
        subtitle = "Comprehensive health score, DNS lookup & latency checks"
    ),
    QUICK_ACTIONS(
        id = "quick_actions",
        title = "Quick Diagnostic Tools",
        subtitle = "Instant access to Speed Test, Ping, OneTap & Radio modes"
    ),
    DEVICE_INFO(
        id = "device_info",
        title = "Device System Snapshot",
        subtitle = "Hardware model, radio chipset, battery & Android telemetry"
    );

    val icon: ImageVector
        get() = when (this) {
            TOPOLOGY -> Icons.Outlined.Hub
            NETWORK_STATUS -> Icons.Outlined.CellTower
            HEALTH_CHECKUP -> Icons.Outlined.HealthAndSafety
            QUICK_ACTIONS -> Icons.Outlined.Speed
            DEVICE_INFO -> Icons.Outlined.PhoneAndroid
        }

    companion object {
        val DEFAULT_ORDER = listOf(
            NETWORK_STATUS,
            TOPOLOGY,
            HEALTH_CHECKUP,
            QUICK_ACTIONS,
            DEVICE_INFO
        )
    }
}
