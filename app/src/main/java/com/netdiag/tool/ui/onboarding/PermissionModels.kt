package com.netdiag.tool.ui.onboarding

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

enum class PermissionStatus {
    GRANTED,
    ACTION_REQUIRED,
    SPECIAL_ACCESS,
    OPTIONAL
}

data class PermissionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isRequired: Boolean,
    val status: PermissionStatus,
    val androidPermission: String? = null,
    val whyNeeded: String,
    val whatWeUse: List<String>,
    val whatWeDoNotDo: List<String>
)

object PermissionChecker {

    fun checkAll(context: Context): List<PermissionItem> {
        val hasPhone = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val hasLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return listOf(
            PermissionItem(
                id = "internet",
                title = "Internet & Network State",
                subtitle = "Active • Basic network connectivity",
                isRequired = true,
                status = PermissionStatus.GRANTED,
                whyNeeded = "Required to test network latency and measure socket connections.",
                whatWeUse = listOf("TCP/HTTP ping probes", "Network capability queries", "DNS resolution"),
                whatWeDoNotDo = listOf("Transmit private files", "Modify network adapters", "Access contacts or storage")
            ),
            PermissionItem(
                id = "phone_state",
                title = "Phone & SIM Information",
                subtitle = if (hasPhone) "Granted" else "Tap to allow",
                isRequired = true,
                status = if (hasPhone) PermissionStatus.GRANTED else PermissionStatus.ACTION_REQUIRED,
                androidPermission = Manifest.permission.READ_PHONE_STATE,
                whyNeeded = "This permission allows the app to inspect available SIM subscriptions and cellular network state.",
                whatWeUse = listOf("SIM information", "Network registration", "Cellular technology (LTE / 5G)", "Signal strength telemetry"),
                whatWeDoNotDo = listOf("Make phone calls", "Send or read SMS", "Modify SIM or profile data")
            ),
            PermissionItem(
                id = "location",
                title = "Cell Tower & Location Access",
                subtitle = if (hasLocation) "Granted" else "Tap to allow",
                isRequired = true,
                status = if (hasLocation) PermissionStatus.GRANTED else PermissionStatus.ACTION_REQUIRED,
                androidPermission = Manifest.permission.ACCESS_FINE_LOCATION,
                whyNeeded = "Android requires location permission for an application to read serving cell IDs, PCI, TAC, and RF signal metrics.",
                whatWeUse = listOf("Serving cell ID (PCI, TAC, ARFCN)", "Neighboring cell signal metrics", "Frequency band identification"),
                whatWeDoNotDo = listOf("Track user movement or GPS history", "Share coordinates with third parties", "Run background GPS tracking")
            ),
            PermissionItem(
                id = "notifications",
                title = "Notifications",
                subtitle = if (hasNotification) "Granted" else "Optional • Not configured",
                isRequired = false,
                status = if (hasNotification) PermissionStatus.GRANTED else PermissionStatus.OPTIONAL,
                androidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
                whyNeeded = "Allows Network Tools to alert you when your network technology transitions (e.g. 5G to 4G) or during long ping tests.",
                whatWeUse = listOf("Status alerts", "Long-running test completion alerts"),
                whatWeDoNotDo = listOf("Send promotional ads", "Trigger sound spam")
            )
        )
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expectedServiceName = "com.netdiag.tool/.service.OneTapAccessibilityService"
        val expectedAlt = "com.netdiag.tool.service.OneTapAccessibilityService"
        return enabledServices.any { 
            it.id.contains("OneTapAccessibilityService") || it.id.contains("com.netdiag.tool") 
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
}
