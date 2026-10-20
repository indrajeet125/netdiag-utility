package com.netdiag.tool.ui.components

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netdiag.tool.ui.onboarding.PermissionChecker

/**
 * Permissions an operation can depend on. Runtime permissions go through the system
 * permission prompt; special-access ones can only be granted from an Android Settings page.
 */
enum class AppPermission(
    val title: String,
    val reason: String,
    val runtimePermissions: List<String> = emptyList()
) {
    PHONE(
        title = "Phone & SIM information",
        reason = "Reads SIM subscriptions, carrier and cellular network state.",
        runtimePermissions = listOf(Manifest.permission.READ_PHONE_STATE)
    ),
    // Since Android 11 the SIM's own number (MSISDN) needs this, READ_PHONE_STATE is no longer enough
    PHONE_NUMBERS(
        title = "Phone numbers",
        reason = "Reads your SIM's phone number (MSISDN), if the carrier stored it on the SIM.",
        runtimePermissions = listOf(Manifest.permission.READ_PHONE_NUMBERS)
    ),
    LOCATION(
        title = "Location",
        reason = "Android requires it to read serving / neighbour cell towers and signal metrics.",
        runtimePermissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    ),
    // Not a permission: the system Location switch. With it off, Android returns no cell info even when LOCATION is granted.
    LOCATION_SERVICE(
        title = "Location turned on",
        reason = "Serving cell, PCI and signal data stay empty while the phone's Location switch is off."
    ),
    USAGE_ACCESS(
        title = "Usage access",
        reason = "Needed to measure the cache size of installed apps."
    ),
    ACCESSIBILITY(
        title = "Accessibility service",
        reason = "Lets OneTap Utility tap through Android Settings on your behalf."
    ),
    OVERLAY(
        title = "Display over other apps",
        reason = "Needed to show the floating OneTap bubble."
    );

    val isRuntime: Boolean get() = runtimePermissions.isNotEmpty()

    fun isGranted(context: Context): Boolean = when (this) {
        PHONE, PHONE_NUMBERS, LOCATION -> runtimePermissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        USAGE_ACCESS -> {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        }
        LOCATION_SERVICE -> (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
            ?.let { LocationManagerCompat.isLocationEnabled(it) } ?: false
        ACCESSIBILITY -> PermissionChecker.isAccessibilityEnabled(context)
        OVERLAY -> PermissionChecker.canDrawOverlays(context)
    }

    fun settingsIntent(context: Context): Intent = when (this) {
        LOCATION_SERVICE -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    }.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
}

internal class PendingOperation(val permissions: List<AppPermission>, val onGranted: () -> Unit)

class PermissionGate internal constructor(private val context: Context) {
    internal var pending by mutableStateOf<PendingOperation?>(null)

    /** Runs [onGranted] right away if every permission is held, otherwise shows the "Permission required" dialog first. */
    fun require(vararg permissions: AppPermission, onGranted: () -> Unit = {}) {
        val list = permissions.toList()
        if (list.all { it.isGranted(context) }) onGranted() else pending = PendingOperation(list, onGranted)
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Returns a [PermissionGate] for this screen and hosts its dialog. Wrap any operation that needs a
 * permission in `gate.require(...) { ... }`: the user is told which permissions are missing and why,
 * can grant them, and the operation runs once they are granted.
 */
@Composable
fun rememberPermissionGate(): PermissionGate {
    val context = LocalContext.current
    val gate = remember(context) { PermissionGate(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var permanentlyDenied by remember { mutableStateOf(false) }
    // Bumped on resume / prompt result so the dialog re-reads grant state
    var refreshKey by remember { mutableStateOf(0) }

    fun completeIfGranted() {
        val op = gate.pending ?: return
        if (op.permissions.all { it.isGranted(context) }) {
            gate.pending = null
            permanentlyDenied = false
            op.onGranted()
        }
        refreshKey++
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val op = gate.pending
        val activity = context.findActivity()
        // Denied without a rationale after asking means "Don't ask again": only Settings can grant it now
        permanentlyDenied = op != null && activity != null && op.permissions.any { p ->
            p.isRuntime && !p.isGranted(context) &&
                p.runtimePermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        }
        completeIfGranted()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) completeIfGranted()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val op = gate.pending
    if (op != null) {
        val missing = remember(op, refreshKey) { op.permissions.filter { !it.isGranted(context) } }
        val missingRuntime = missing.filter { it.isRuntime }
        val opensSettings = missingRuntime.isEmpty() || permanentlyDenied

        AlertDialog(
            onDismissRequest = { gate.pending = null },
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = if (missing.size == 1) "Permission required" else "Permissions required",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This operation needs the following to continue:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    missing.forEach { perm ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium)
                            Column {
                                Text(perm.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text(
                                    perm.reason,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                    if (permanentlyDenied) {
                        Text(
                            text = "Permission was denied. Enable it from Android Settings.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (opensSettings) {
                        // Runtime perms denied for good share the app-details page; otherwise open the first special-access page
                        val target = missingRuntime.firstOrNull() ?: missing.first()
                        try { context.startActivity(target.settingsIntent(context)) } catch (_: Exception) { }
                    } else {
                        launcher.launch(missingRuntime.flatMap { it.runtimePermissions }.toTypedArray())
                    }
                }) {
                    Text(if (opensSettings) "Open Settings" else "Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { gate.pending = null }) { Text("Not now") }
            }
        )
    }
    return gate
}
