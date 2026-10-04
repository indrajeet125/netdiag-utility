package com.netdiag.tool.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.data.repository.AppPreferencesRepository
import com.netdiag.tool.data.repository.AppThemeMode
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.onboarding.PermissionChecker
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onResetOnboarding: () -> Unit,
    preferencesRepository: AppPreferencesRepository
) {
    val context = LocalContext.current
    val currentThemeMode by preferencesRepository.themeMode.collectAsState()
    val isDynamicColor by preferencesRepository.dynamicColorEnabled.collectAsState()
    val oneTapEnabled by preferencesRepository.oneTapEnabled.collectAsState()

    var isAccessibilityActive by remember { mutableStateOf(PermissionChecker.isAccessibilityEnabled(context)) }
    var canDrawOverlays by remember { mutableStateOf(PermissionChecker.canDrawOverlays(context)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Group 1: Appearance & Theme
            SettingsGroup(title = "APPEARANCE & THEME") {
                Text(
                    text = "Theme Mode",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val themeOptions = listOf(
                    AppThemeMode.SYSTEM_DEFAULT to "System default",
                    AppThemeMode.LIGHT to "Light mode",
                    AppThemeMode.DARK to "Dark mode (Recommended)"
                )

                themeOptions.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { preferencesRepository.setThemeMode(mode) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentThemeMode == mode,
                            onClick = { preferencesRepository.setThemeMode(mode) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                SettingsToggleRow(
                    title = "Dynamic Color (Material You)",
                    subtitle = "Derive accent tones from Android wallpaper (Android 12+)",
                    isChecked = isDynamicColor,
                    onCheckedChange = { preferencesRepository.setDynamicColorEnabled(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 2: OneTap Floating Tool
            SettingsGroup(title = "ONETAP FLOATING TOOL") {
                SettingsToggleRow(
                    title = "Enable OneTap Tool",
                    subtitle = "Show movable shortcut over other apps",
                    isChecked = oneTapEnabled,
                    onCheckedChange = { preferencesRepository.setOneTapEnabled(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                SettingsClickableRow(
                    title = "Accessibility Service",
                    subtitle = if (isAccessibilityActive) "Active & configured" else "Tap to enable in Android settings",
                    icon = Icons.Outlined.TouchApp,
                    valueColor = if (isAccessibilityActive) StatusGreen else StatusAmber,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                SettingsClickableRow(
                    title = "Display Over Other Apps",
                    subtitle = if (canDrawOverlays) "Granted" else "Tap to grant overlay access",
                    icon = Icons.Outlined.Layers,
                    valueColor = if (canDrawOverlays) StatusGreen else StatusAmber,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 3: Network & Diagnostics
            SettingsGroup(title = "DIAGNOSTICS & PING") {
                SettingsClickableRow(
                    title = "Default Ping Target",
                    subtitle = "google.com",
                    icon = Icons.Outlined.Dns,
                    onClick = {}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                SettingsClickableRow(
                    title = "Ping Timeout",
                    subtitle = "3000 ms",
                    icon = Icons.Outlined.Timer,
                    onClick = {}
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                SettingsClickableRow(
                    title = "Cellular Poll Interval",
                    subtitle = "2000 ms (balanced)",
                    icon = Icons.Outlined.Refresh,
                    onClick = {}
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 4: Permissions & Setup Reset
            SettingsGroup(title = "SETUP & PERMISSIONS") {
                SettingsClickableRow(
                    title = "App Permissions Center",
                    subtitle = "Manage Android system runtime permissions",
                    icon = Icons.Outlined.Security,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                SettingsClickableRow(
                    title = "Reset Onboarding Experience",
                    subtitle = "Re-launch the introductory walkthrough & checklist",
                    icon = Icons.Outlined.RestartAlt,
                    onClick = {
                        preferencesRepository.resetOnboarding()
                        onResetOnboarding()
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group 5: About
            SettingsGroup(title = "ABOUT") {
                KeyValueRow(label = "Application", value = "Network Tools", isMonospace = false)
                KeyValueRow(label = "Version", value = "1.0.0")
                KeyValueRow(label = "Architecture", value = "Clean Architecture + Hilt", isMonospace = false)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    valueColor: androidx.compose.ui.graphics.Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = valueColor ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
