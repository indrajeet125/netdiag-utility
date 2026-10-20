package com.netdiag.tool.ui.device

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.app.ActivityCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.CellData
import com.netdiag.tool.domain.model.RadioAccessTechnology
import com.netdiag.tool.domain.model.SimSubscription
import com.netdiag.tool.ui.components.AppPermission
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.components.rememberPermissionGate
import com.netdiag.tool.ui.dashboard.DashboardViewModel
import com.netdiag.tool.ui.network.NeighborSort
import com.netdiag.tool.ui.network.NetworkViewModel
import com.netdiag.tool.ui.theme.*
import java.util.Locale

enum class DeviceInfoTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Outlined.GridView),
    SIM1("SIM 1", Icons.Outlined.SimCard),
    SIM2("SIM 2", Icons.Outlined.SimCard),
    CELLULAR("Cellular", Icons.Outlined.CellTower),
    BATTERY("Battery", Icons.Outlined.BatteryChargingFull),
    HARDWARE("Hardware", Icons.Outlined.DeveloperBoard),
    SOFTWARE("Software", Icons.Outlined.Android)
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    networkViewModel: NetworkViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableStateOf(DeviceInfoTab.OVERVIEW) }

    val sim1 by networkViewModel.sim1.collectAsState()
    val sim2 by networkViewModel.sim2.collectAsState()
    val networkState by dashboardViewModel.networkState.collectAsState()
    val currentRat by networkViewModel.currentRat.collectAsState()
    val neighborSort by networkViewModel.neighborSort.collectAsState()

    val hasPhonePermission by networkViewModel.hasPhonePermission.collectAsState()
    val hasLocationPermission by networkViewModel.hasLocationPermission.collectAsState()
    val hasPermissions by networkViewModel.hasPermissions.collectAsState()

    var permissionRequestedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionGate = rememberPermissionGate()
    val refreshWithPermissions: () -> Unit = {
        permissionGate.require(AppPermission.PHONE, AppPermission.LOCATION, AppPermission.LOCATION_SERVICE) {
            networkViewModel.checkPermissions()
            networkViewModel.refresh()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionRequestedOnce = true
        networkViewModel.checkPermissions()
        networkViewModel.refresh()
        dashboardViewModel.refresh()
    }

    val missingPermissions = remember(hasPhonePermission, hasLocationPermission) {
        buildList {
            if (!hasLocationPermission) add("• Location")
            if (!hasPhonePermission) add("• Phone / SIM information")
        }
    }

    val isPermanentlyDenied = permissionRequestedOnce && (
        (!hasPhonePermission && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.READ_PHONE_STATE)) ||
        (!hasLocationPermission && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_FINE_LOCATION))
    )

    val onGrantPermissions: () -> Unit = {
        val perms = buildList {
            if (!hasPhonePermission) add(android.Manifest.permission.READ_PHONE_STATE)
            if (!hasLocationPermission) {
                add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    val onOpenSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    val activeSim = when {
        sim1.info.isDefaultData && sim1.info.subscriptionId != -1 -> sim1
        sim2.info.isDefaultData && sim2.info.subscriptionId != -1 -> sim2
        sim1.info.subscriptionId != -1 -> sim1
        sim2.info.subscriptionId != -1 -> sim2
        else -> null
    }

    val dataSimDisplay = when {
        sim1.info.isDefaultData && sim1.info.subscriptionId != -1 -> "SIM 1"
        sim2.info.isDefaultData && sim2.info.subscriptionId != -1 -> "SIM 2"
        sim1.info.subscriptionId != -1 -> "SIM 1"
        sim2.info.subscriptionId != -1 -> "SIM 2"
        !hasPhonePermission -> "Permission Required"
        else -> "No Active SIM"
    }

    val carrierDisplay = when {
        activeSim != null && activeSim.info.carrierName.isNotBlank() && activeSim.info.carrierName != "Unknown" && activeSim.info.carrierName != "Unavailable" -> activeSim.info.carrierName
        !hasPhonePermission -> "Permission Required"
        else -> "Unavailable"
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Run live RF/cell monitoring if user is on Cellular/SIM tabs AND screen is visible
    val needsLiveMonitoring = selectedTab in listOf(DeviceInfoTab.CELLULAR, DeviceInfoTab.SIM1, DeviceInfoTab.SIM2)

    DisposableEffect(lifecycleOwner, needsLiveMonitoring) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                networkViewModel.checkPermissions()
                networkViewModel.refresh()
                dashboardViewModel.refresh()
                if (needsLiveMonitoring) {
                    networkViewModel.startLiveMonitoring()
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                networkViewModel.stopLiveMonitoring()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        networkViewModel.checkPermissions()
        networkViewModel.refresh()
        dashboardViewModel.refresh()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) && needsLiveMonitoring) {
            networkViewModel.startLiveMonitoring()
        } else if (!needsLiveMonitoring) {
            networkViewModel.stopLiveMonitoring()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            networkViewModel.stopLiveMonitoring()
        }
    }

    // ── RAM Diagnostics ──
    val memoryInfo = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        mi
    }
    val totalRamGb = String.format(Locale.US, "%.2f GB", memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
    val availRamGb = String.format(Locale.US, "%.2f GB", memoryInfo.availMem / (1024.0 * 1024.0 * 1024.0))
    val usedRamGb = String.format(Locale.US, "%.2f GB", (memoryInfo.totalMem - memoryInfo.availMem) / (1024.0 * 1024.0 * 1024.0))

    // ── Storage Diagnostics ──
    val (totalStorageGb, availStorageGb, usedStorageGb) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBytes = stat.blockCountLong * blockSize
            val freeBytes = stat.availableBlocksLong * blockSize
            val usedBytes = totalBytes - freeBytes
            Triple(
                String.format(Locale.US, "%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0)),
                String.format(Locale.US, "%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0)),
                String.format(Locale.US, "%.1f GB", usedBytes / (1024.0 * 1024.0 * 1024.0))
            )
        } catch (_: Exception) {
            Triple("--", "--", "--")
        }
    }

    // ── Battery Telemetry ──
    val batteryIntent = remember {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val batteryPct = batteryIntent?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else null
    } ?: 80

    val isCharging = batteryIntent?.let {
        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } ?: false

    val batteryTempC = batteryIntent?.let {
        val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        temp / 10.0
    } ?: 32.0

    val batteryVoltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 3850
    val batteryHealth = batteryIntent?.let {
        when (it.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }
    } ?: "Good"

    // ── Display Metrics ──
    val displayMetrics = context.resources.displayMetrics
    val screenRes = "${displayMetrics.widthPixels} × ${displayMetrics.heightPixels} px"
    val screenDpi = "${displayMetrics.densityDpi} DPI"

    // ── System ──
    val uptimeHours = SystemClock.elapsedRealtime() / (1000 * 60 * 60)
    val uptimeMins = (SystemClock.elapsedRealtime() / (1000 * 60)) % 60
    val uptimeStr = "${uptimeHours}h ${uptimeMins}m"
    val radioVersion = try { Build.getRadioVersion() ?: "Unknown" } catch (_: Exception) { "Unknown" }
    val kernelVersion = System.getProperty("os.version") ?: "Linux"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                actions = {
                    if (onNavigateToSettings != null) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
        ) {
            // ── Prominent Permission Banner (at TOP of Device Info) ──
            if (missingPermissions.isNotEmpty()) {
                DeviceInfoPermissionBanner(
                    missingPermissions = missingPermissions,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onGrantPermissions = onGrantPermissions,
                    onOpenSettings = onOpenSettings
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Horizontal Tabs: Overview | SIM 1 | SIM 2 | Cellular | Battery | Hardware | Software
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                items(DeviceInfoTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTab = tab
                            // SIM / Cellular tabs show telemetry that needs Phone + Location
                            if (tab == DeviceInfoTab.SIM1 || tab == DeviceInfoTab.SIM2 || tab == DeviceInfoTab.CELLULAR) {
                                refreshWithPermissions()
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                when (selectedTab) {
                    DeviceInfoTab.OVERVIEW -> {
                        OverviewSection(
                            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            ramSummary = "$usedRamGb / $totalRamGb",
                            storageSummary = "$usedStorageGb / $totalStorageGb",
                            batterySummary = "$batteryPct% • ${if (isCharging) "Charging" else "Discharging"}",
                            activeSim = activeSim,
                            carrierDisplay = carrierDisplay,
                            dataSimDisplay = dataSimDisplay,
                            hasPhonePermission = hasPhonePermission,
                            hasLocationPermission = hasLocationPermission,
                            networkState = networkState,
                            onGoToCellular = { selectedTab = DeviceInfoTab.CELLULAR }
                        )
                    }
                    DeviceInfoTab.SIM1 -> {
                        SimDetailsSection(
                            title = "SIM 1 TELEMETRY",
                            sim = sim1,
                            hasPhonePermission = hasPhonePermission,
                            isPermanentlyDenied = isPermanentlyDenied,
                            onRequestPermission = onGrantPermissions,
                            onOpenSettings = onOpenSettings
                        )
                    }
                    DeviceInfoTab.SIM2 -> {
                        SimDetailsSection(
                            title = "SIM 2 TELEMETRY",
                            sim = sim2,
                            hasPhonePermission = hasPhonePermission,
                            isPermanentlyDenied = isPermanentlyDenied,
                            onRequestPermission = onGrantPermissions,
                            onOpenSettings = onOpenSettings
                        )
                    }
                    DeviceInfoTab.CELLULAR -> {
                        CellularRadioSection(
                            sim = activeSim ?: sim1,
                            rat = currentRat,
                            hasLocationPermission = hasLocationPermission,
                            neighborSort = neighborSort,
                            onSortChanged = { networkViewModel.setNeighborSort(it) },
                            onRefresh = refreshWithPermissions
                        )
                    }
                    DeviceInfoTab.BATTERY -> {
                        BatterySection(
                            percent = batteryPct,
                            isCharging = isCharging,
                            tempC = batteryTempC,
                            voltageMv = batteryVoltageMv,
                            health = batteryHealth
                        )
                    }
                    DeviceInfoTab.HARDWARE -> {
                        HardwareSection(
                            screenRes = screenRes,
                            screenDpi = screenDpi,
                            cpuCores = Runtime.getRuntime().availableProcessors(),
                            abis = Build.SUPPORTED_ABIS.joinToString(", "),
                            ramTotal = totalRamGb,
                            ramAvail = availRamGb,
                            storageTotal = totalStorageGb,
                            storageAvail = availStorageGb
                        )
                    }
                    DeviceInfoTab.SOFTWARE -> {
                        SoftwareSection(
                            androidRelease = Build.VERSION.RELEASE,
                            sdkInt = Build.VERSION.SDK_INT,
                            buildNumber = Build.DISPLAY,
                            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
                            kernel = kernelVersion,
                            radio = radioVersion,
                            uptime = uptimeStr
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OverviewSection(
    deviceName: String,
    androidVersion: String,
    ramSummary: String,
    storageSummary: String,
    batterySummary: String,
    activeSim: SimSubscription?,
    carrierDisplay: String,
    dataSimDisplay: String,
    hasPhonePermission: Boolean,
    hasLocationPermission: Boolean,
    networkState: com.netdiag.tool.domain.model.NetworkState,
    onGoToCellular: () -> Unit
) {
    Text(
        text = "DEVICE TELEMETRY SUMMARY",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Model", value = deviceName, isMonospace = false)
            KeyValueRow(label = "OS", value = androidVersion, isMonospace = false)
            KeyValueRow(label = "RAM Used", value = ramSummary)
            KeyValueRow(label = "Storage Used", value = storageSummary)
            KeyValueRow(label = "Battery", value = batterySummary, isMonospace = false)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "ACTIVE NETWORK",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Transport", value = networkState.transport)
            KeyValueRow(label = "Carrier", value = carrierDisplay, isMonospace = false)
            val phoneDisplay = when {
                activeSim != null && !activeSim.info.msisdn.isNullOrBlank() -> activeSim.info.msisdn
                !hasPhonePermission -> "Permission Required"
                else -> "Unavailable (Not provided by carrier / SIM)"
            }
            KeyValueRow(label = "Phone Number", value = phoneDisplay, isMonospace = false)
            KeyValueRow(label = "Data SIM", value = dataSimDisplay)
            KeyValueRow(label = "IPv4 Address", value = networkState.ipv4 ?: "Not assigned")
            KeyValueRow(label = "Default Gateway", value = networkState.gateway ?: "N/A")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val cell = activeSim?.servingCell
    Text(
        text = "RADIO SIGNAL (2×2 GRID)",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Signal2x2Grid(cell = cell, hasLocationPermission = hasLocationPermission)

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onGoToCellular,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(Icons.Outlined.CellTower, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("VIEW DETAILED CELLULAR TELEMETRY", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
fun Signal2x2Grid(cell: CellData?, hasLocationPermission: Boolean = true) {
    val rsrpVal = cell?.ssRsrp ?: cell?.rsrp
    val rsrqVal = cell?.ssRsrq ?: cell?.rsrq
    val rssiVal = cell?.rssi
    val sinrVal = cell?.ssSinr ?: cell?.sinr

    val fallbackText = if (!hasLocationPermission) "Permission Required" else "Unavailable"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCell(label = "RSRP", value = rsrpVal?.let { "$it dBm" } ?: fallbackText, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                MetricCell(label = "RSRQ", value = rsrqVal?.let { "$it dB" } ?: fallbackText, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCell(label = "RSSI", value = rssiVal?.let { "$it dBm" } ?: fallbackText, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                MetricCell(label = "SINR", value = sinrVal?.let { "${it.toInt()} dB" } ?: fallbackText, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
private fun DeviceInfoPermissionBanner(
    missingPermissions: List<String>,
    isPermanentlyDenied: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cellular Permissions Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isPermanentlyDenied) {
                Text(
                    text = "Please enable the required permissions from Android Settings.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                missingPermissions.forEach { perm ->
                    Text(
                        text = perm,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Open Settings", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            } else {
                Text(
                    text = "Cellular diagnostics require the following permissions to show SIM and network information.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        lineHeight = 18.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                missingPermissions.forEach { perm ->
                    Text(
                        text = perm,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onGrantPermissions,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Grant Permissions", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun SimDetailsSection(
    title: String,
    sim: SimSubscription,
    hasPhonePermission: Boolean,
    isPermanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (sim.info.subscriptionId == -1) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!hasPhonePermission) {
                    Text(
                        text = "Phone permission is required to detect SIM card subscriptions in this slot.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (isPermanentlyDenied) {
                        Button(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Open Settings", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Grant Phone Permission", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    Text(
                        text = "No SIM card inserted or subscription inactive in this slot.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        return
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Carrier", value = sim.info.carrierName, isMonospace = false)
            val phoneDisplay = if (!sim.info.msisdn.isNullOrBlank()) sim.info.msisdn else "Restricted by carrier / SIM"
            KeyValueRow(label = "Phone Number", value = phoneDisplay, isMonospace = false)
            KeyValueRow(label = "MCC / MNC", value = "${sim.info.mcc ?: "N/A"} / ${sim.info.mnc ?: "N/A"}")
            KeyValueRow(label = "PLMN", value = sim.info.plmn ?: "N/A")
            KeyValueRow(label = "Registration", value = sim.registrationState)
            KeyValueRow(label = "Roaming", value = if (sim.info.isRoaming) "Roaming" else "Home")
            KeyValueRow(label = "Default Data SIM", value = if (sim.info.isDefaultData) "Yes" else "No")
            KeyValueRow(label = "RAT", value = sim.rat.label)
            KeyValueRow(label = "IMS Registered", value = if (sim.isImsRegistered) "Registered ✓" else "Not Registered")
            KeyValueRow(label = "VoLTE Available", value = if (sim.isVoLTE) "Available ✓" else "Unavailable")
            KeyValueRow(label = "VoWiFi Available", value = if (sim.isVoWiFi) "Available ✓" else "Unavailable")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "SERVING CELL",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    val cell = sim.servingCell
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "PCI", value = cell?.pci?.toString() ?: "N/A")
            KeyValueRow(label = "TAC", value = cell?.tac?.toString() ?: "N/A")
            KeyValueRow(label = "Cell ID", value = cell?.cellId?.toString() ?: "N/A")
            KeyValueRow(label = "Band", value = cell?.band ?: "N/A")
            KeyValueRow(label = "EARFCN / ARFCN", value = cell?.arfcn?.toString() ?: "N/A")
            KeyValueRow(label = "Bandwidth", value = cell?.bandwidthKhz?.let { "${it / 1000} MHz" } ?: "N/A")
            KeyValueRow(label = "Timing Advance", value = cell?.timingAdvance?.let { "$it ($it steps)" } ?: "N/A")
        }
    }
}

@Composable
private fun CellularRadioSection(
    sim: SimSubscription,
    rat: RadioAccessTechnology,
    hasLocationPermission: Boolean,
    neighborSort: NeighborSort,
    onSortChanged: (NeighborSort) -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LIVE RF METRICS (${rat.label})",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Signal2x2Grid(cell = sim.servingCell, hasLocationPermission = hasLocationPermission)

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "SERVING CELL & CARRIERS",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    val cell = sim.servingCell
    val fallback = if (!hasLocationPermission) "Permission Required" else "N/A"
    val archValue = when {
        !hasLocationPermission && (rat == RadioAccessTechnology.NR_SA || rat == RadioAccessTechnology.NR_NSA) -> "Permission Required"
        sim.nsaMode != null -> sim.nsaMode
        rat == RadioAccessTechnology.NR_SA -> "Standalone (SA)"
        rat == RadioAccessTechnology.NR_NSA -> "Non-Standalone (NSA)"
        else -> "N/A"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "5G Architecture", value = archValue)
            KeyValueRow(label = "Primary Band", value = cell?.band ?: fallback)
            KeyValueRow(label = "Physical Cell ID (PCI)", value = cell?.pci?.toString() ?: fallback)
            KeyValueRow(label = "Tracking Area Code (TAC)", value = cell?.tac?.toString() ?: fallback)
            KeyValueRow(label = "Cell Identity (ECI/NCI)", value = cell?.cellId?.toString() ?: fallback)
            KeyValueRow(label = "eNodeB / gNodeB ID", value = cell?.enbId?.toString() ?: fallback)
            KeyValueRow(label = "Sector ID", value = cell?.sectorId?.toString() ?: fallback)
            KeyValueRow(label = "ARFCN / EARFCN", value = cell?.arfcn?.toString() ?: fallback)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Neighbour Cells
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NEIGHBOUR CELLS (${sim.neighborCells.size})",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            NeighborSort.values().forEach { sort ->
                val isSel = neighborSort == sort
                Text(
                    text = sort.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.clickable { onSortChanged(sort) }.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (sim.neighborCells.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (!hasLocationPermission) "Location permission is required to detect neighbour cell towers."
                       else "No neighbour cells reported by modem. Modem may restrict neighbour scanning to conserve battery.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(14.dp)
            )
        }
    } else {
        sim.neighborCells.take(15).forEach { n ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = n.rat.shortLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PCI ${n.pci ?: "--"} • ${n.band ?: (n.arfcn?.let { "CH $it" } ?: "")}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }

                    Text(
                        text = "${n.ssRsrp ?: n.rsrp ?: n.rssi ?: "--"} dBm",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun BatterySection(
    percent: Int,
    isCharging: Boolean,
    tempC: Double,
    voltageMv: Int,
    health: String
) {
    Text(
        text = "BATTERY TELEMETRY",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Battery Level", value = "$percent%")
            KeyValueRow(label = "Charging State", value = if (isCharging) "Charging" else "Discharging", isMonospace = false)
            KeyValueRow(label = "Temperature", value = String.format(Locale.US, "%.1f °C", tempC))
            KeyValueRow(label = "Voltage", value = "$voltageMv mV")
            KeyValueRow(label = "Health", value = health, isMonospace = false)
        }
    }
}

@Composable
private fun HardwareSection(
    screenRes: String,
    screenDpi: String,
    cpuCores: Int,
    abis: String,
    ramTotal: String,
    ramAvail: String,
    storageTotal: String,
    storageAvail: String
) {
    Text(
        text = "HARDWARE & DISPLAY",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Screen Resolution", value = screenRes)
            KeyValueRow(label = "Screen Density", value = screenDpi)
            KeyValueRow(label = "CPU Cores", value = "$cpuCores Cores")
            KeyValueRow(label = "Architecture (ABIs)", value = abis, isMonospace = false)
            KeyValueRow(label = "Total RAM", value = ramTotal)
            KeyValueRow(label = "Available RAM", value = ramAvail)
            KeyValueRow(label = "Total Storage", value = storageTotal)
            KeyValueRow(label = "Available Storage", value = storageAvail)
        }
    }
}

@Composable
private fun SoftwareSection(
    androidRelease: String,
    sdkInt: Int,
    buildNumber: String,
    securityPatch: String,
    kernel: String,
    radio: String,
    uptime: String
) {
    Text(
        text = "SOFTWARE & SYSTEM",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "Android Version", value = androidRelease)
            KeyValueRow(label = "API / SDK Level", value = sdkInt.toString())
            KeyValueRow(label = "Build Number", value = buildNumber, isMonospace = false)
            KeyValueRow(label = "Security Patch", value = securityPatch)
            KeyValueRow(label = "Kernel Version", value = kernel, isMonospace = false)
            KeyValueRow(label = "Baseband Radio", value = radio, isMonospace = false)
            KeyValueRow(label = "System Uptime", value = uptime, isMonospace = false)
        }
    }
}
