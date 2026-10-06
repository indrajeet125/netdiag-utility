package com.netdiag.tool.ui.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
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
import com.netdiag.tool.ui.components.KeyValueRow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToSettings: (() -> Unit)? = null,
    networkViewModel: NetworkViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedTab by remember { mutableStateOf(DeviceInfoTab.OVERVIEW) }

    val sim1 by networkViewModel.sim1.collectAsState()
    val sim2 by networkViewModel.sim2.collectAsState()
    val networkState by dashboardViewModel.networkState.collectAsState()
    val currentRat by networkViewModel.currentRat.collectAsState()
    val neighborSort by networkViewModel.neighborSort.collectAsState()

    val activeSim = if (sim1.info.isDefaultData) sim1 else if (sim2.info.isDefaultData) sim2 else sim1

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Only run live RF/cell monitoring if user is on Cellular/SIM tabs AND screen is visible
    val needsLiveMonitoring = selectedTab in listOf(DeviceInfoTab.CELLULAR, DeviceInfoTab.SIM1, DeviceInfoTab.SIM2)

    DisposableEffect(lifecycleOwner, needsLiveMonitoring) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && needsLiveMonitoring) {
                networkViewModel.startLiveMonitoring()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                networkViewModel.stopLiveMonitoring()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
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
                        onClick = { selectedTab = tab },
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

            Spacer(modifier = Modifier.height(6.dp))

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
                            networkState = networkState,
                            onGoToCellular = { selectedTab = DeviceInfoTab.CELLULAR }
                        )
                    }
                    DeviceInfoTab.SIM1 -> {
                        SimDetailsSection(title = "SIM 1 TELEMETRY", sim = sim1)
                    }
                    DeviceInfoTab.SIM2 -> {
                        SimDetailsSection(title = "SIM 2 TELEMETRY", sim = sim2)
                    }
                    DeviceInfoTab.CELLULAR -> {
                        CellularRadioSection(
                            sim = activeSim,
                            rat = currentRat,
                            neighborSort = neighborSort,
                            onSortChanged = { networkViewModel.setNeighborSort(it) },
                            onRefresh = { networkViewModel.refresh() }
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
    activeSim: SimSubscription,
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
            KeyValueRow(label = "Carrier", value = activeSim.info.carrierName.ifBlank { "Unknown" }, isMonospace = false)
            val phoneDisplay = if (!activeSim.info.msisdn.isNullOrBlank()) activeSim.info.msisdn else "Restricted by carrier / SIM"
            KeyValueRow(label = "Phone Number", value = phoneDisplay, isMonospace = false)
            KeyValueRow(label = "Data SIM", value = "SIM ${activeSim.info.slotIndex + 1}")
            KeyValueRow(label = "IPv4 Address", value = networkState.ipv4 ?: "Not assigned")
            KeyValueRow(label = "Default Gateway", value = networkState.gateway ?: "N/A")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    val cell = activeSim.servingCell
    Text(
        text = "RADIO SIGNAL (2×2 GRID)",
        style = MaterialTheme.typography.titleSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp
        )
    )

    Spacer(modifier = Modifier.height(10.dp))

    Signal2x2Grid(cell = cell)

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
fun Signal2x2Grid(cell: CellData?) {
    val rsrpVal = cell?.ssRsrp ?: cell?.rsrp
    val rsrqVal = cell?.ssRsrq ?: cell?.rsrq
    val rssiVal = cell?.rssi
    val sinrVal = cell?.ssSinr ?: cell?.sinr

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCell(label = "RSRP", value = rsrpVal?.let { "$it dBm" } ?: "N/A", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                MetricCell(label = "RSRQ", value = rsrqVal?.let { "$it dB" } ?: "N/A", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricCell(label = "RSSI", value = rssiVal?.let { "$it dBm" } ?: "N/A", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                MetricCell(label = "SINR", value = sinrVal?.let { "${it.toInt()} dB" } ?: "N/A", modifier = Modifier.weight(1f))
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
private fun SimDetailsSection(title: String, sim: SimSubscription) {
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
            Text(
                text = "No SIM card inserted or subscription inactive in this slot.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
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

    Signal2x2Grid(cell = sim.servingCell)

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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            KeyValueRow(label = "5G Architecture", value = sim.nsaMode ?: (if (rat == RadioAccessTechnology.NR_SA) "Standalone SA" else "N/A"))
            KeyValueRow(label = "Primary Band", value = cell?.band ?: "N/A")
            KeyValueRow(label = "Physical Cell ID (PCI)", value = cell?.pci?.toString() ?: "N/A")
            KeyValueRow(label = "Tracking Area Code (TAC)", value = cell?.tac?.toString() ?: "N/A")
            KeyValueRow(label = "Cell Identity (ECI/NCI)", value = cell?.cellId?.toString() ?: "N/A")
            KeyValueRow(label = "eNodeB / gNodeB ID", value = cell?.enbId?.toString() ?: "N/A")
            KeyValueRow(label = "Sector ID", value = cell?.sectorId?.toString() ?: "N/A")
            KeyValueRow(label = "ARFCN / EARFCN", value = cell?.arfcn?.toString() ?: "N/A")
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
                text = "No neighbour cells reported by modem. Modem may restrict neighbour scanning to conserve battery.",
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
