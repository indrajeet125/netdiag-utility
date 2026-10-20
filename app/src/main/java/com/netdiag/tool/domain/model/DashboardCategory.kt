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
        title = "Connection Path",
        subtitle = "How your phone reaches the internet: router, ISP and DNS"
    ),
    NETWORK_STATUS(
        id = "network_status",
        title = "Connection Status",
        subtitle = "Wi-Fi or mobile data, signal strength and carrier"
    ),
    HEALTH_CHECKUP(
        id = "health_checkup",
        title = "Check My Connection",
        subtitle = "One tap test with an overall health score"
    ),
    QUICK_ACTIONS(
        id = "quick_actions",
        title = "Quick Actions",
        subtitle = "Speed test, ping, OneTap and network mode"
    ),
    DEVICE_INFO(
        id = "device_info",
        title = "My Device",
        subtitle = "Model, battery, storage and Android version"
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
        // First-time users see "check my connection" first; less common views go last
        val DEFAULT_ORDER = listOf(
            HEALTH_CHECKUP,
            NETWORK_STATUS,
            QUICK_ACTIONS,
            DEVICE_INFO,
            TOPOLOGY
        )
    }
}
