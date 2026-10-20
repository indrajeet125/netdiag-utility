package com.netdiag.tool.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Process
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netdiag.tool.ui.components.AppPermission
import com.netdiag.tool.ui.components.rememberPermissionGate
import com.netdiag.tool.ui.theme.StatusAmber
import com.netdiag.tool.ui.theme.StatusGreen

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Lists every permission the app uses with its live status and a Grant / Revoke action.
 *
 * Android only lets an app drop its own runtime permissions on Android 13+
 * (revokeSelfPermissionsOnKill), and the change applies when the app process restarts.
 * Special accesses (usage access, accessibility, overlay) and older Android versions can only
 * be changed from the matching system Settings page, so Revoke opens that page instead.
 */
@Composable
fun PermissionManagerSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val gate = rememberPermissionGate()

    // Bumped on resume so statuses update after returning from a Settings page
    var refreshKey by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Runtime permissions queued for removal; they stay granted until the app restarts
    var pendingRevoke by remember { mutableStateOf(setOf<AppPermission>()) }
    var confirmRevoke by remember { mutableStateOf<AppPermission?>(null) }

    AppPermission.values().forEachIndexed { index, perm ->
        val granted = remember(refreshKey, perm) { perm.isGranted(context) }
        val isPending = perm in pendingRevoke

        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = perm.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when {
                        isPending -> "Will be removed when the app restarts"
                        granted -> "Allowed"
                        else -> "Not allowed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isPending -> StatusAmber
                        granted -> StatusGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            when {
                isPending -> Unit
                granted -> OutlinedButton(onClick = {
                    if (perm.isRuntime && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        confirmRevoke = perm
                    } else {
                        // Special access, the Location switch, or Android 12 and older: only Settings can turn it off
                        try { context.startActivity(perm.settingsIntent(context)) } catch (_: Exception) { }
                    }
                }) {
                    Text(if (perm.isRuntime || perm == AppPermission.LOCATION_SERVICE) "Revoke" else "Turn off")
                }
                else -> Button(onClick = { gate.require(perm) { refreshKey++ } }) {
                    Text("Allow")
                }
            }
        }
    }

    if (pendingRevoke.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                // revokeSelfPermissionsOnKill only applies once every process of the app is gone
                context.findActivity()?.finishAffinity()
                Process.killProcess(Process.myPid())
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close app to apply")
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    confirmRevoke?.let { perm ->
        AlertDialog(
            onDismissRequest = { confirmRevoke = null },
            title = { Text("Revoke ${perm.title}?") },
            text = {
                Text(
                    "Android removes it the next time the app restarts. Features that need it will ask again " +
                        "before they run."
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.revokeSelfPermissionsOnKill(perm.runtimePermissions)
                    }
                    pendingRevoke = pendingRevoke + perm
                    confirmRevoke = null
                }) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRevoke = null }) { Text("Cancel") }
            }
        )
    }
}
