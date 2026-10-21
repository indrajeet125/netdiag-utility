package com.netdiag.tool.ui.network

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.*
import com.netdiag.tool.ui.components.*
import com.netdiag.tool.ui.theme.*

// ── 3GPP Cellular Quality Classification ──

enum class MetricQuality(val label: String, val color: Color, val bgColor: Color) {
    EXCELLENT("Excellent", StatusGreen, StatusGreenBg),
    GOOD("Good", AccentCyanDark, AccentCyanBgDark),
    FAIR("Fair", StatusAmber, StatusAmberBg),
    POOR("Poor", StatusRed, StatusRedBg),
    NA("N/A", TextSecondaryDark, Color.Transparent)
}

fun classifyRsrp(rsrp: Int?): MetricQuality {
    return when {
        rsrp == null || rsrp == Int.MAX_VALUE || rsrp == 0 || rsrp <= -140 -> MetricQuality.NA
        rsrp >= -80 -> MetricQuality.EXCELLENT
        rsrp >= -95 -> MetricQuality.GOOD
        rsrp >= -110 -> MetricQuality.FAIR
        else -> MetricQuality.POOR
    }
}

fun classifyRsrq(rsrq: Int?): MetricQuality {
    return when {
        rsrq == null || rsrq == Int.MAX_VALUE -> MetricQuality.NA
        rsrq >= -10 -> MetricQuality.EXCELLENT
        rsrq >= -15 -> MetricQuality.GOOD
        rsrq >= -20 -> MetricQuality.FAIR
        else -> MetricQuality.POOR
    }
}

fun classifySinr(sinr: Double?): MetricQuality {
    return when {
        sinr == null || sinr <= -30.0 -> MetricQuality.NA
        sinr >= 20.0 -> MetricQuality.EXCELLENT
        sinr >= 13.0 -> MetricQuality.GOOD
        sinr >= 0.0 -> MetricQuality.FAIR
        else -> MetricQuality.POOR
    }
}

fun classifyRscp(rscp: Int?): MetricQuality {
    return when {
        rscp == null || rscp == Int.MAX_VALUE || rscp == 0 || rscp <= -140 -> MetricQuality.NA
        rscp >= -75 -> MetricQuality.EXCELLENT
        rscp >= -85 -> MetricQuality.GOOD
        rscp >= -100 -> MetricQuality.FAIR
        else -> MetricQuality.POOR
    }
}

fun classifyRssi(rssi: Int?): MetricQuality {
    return when {
        rssi == null || rssi == Int.MAX_VALUE || rssi == 0 || rssi <= -140 -> MetricQuality.NA
        rssi >= -65 -> MetricQuality.EXCELLENT
        rssi >= -75 -> MetricQuality.GOOD
        rssi >= -85 -> MetricQuality.FAIR
        else -> MetricQuality.POOR
    }
}

// ── Cellular Diagnostics Main Screen ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val sim1 by viewModel.sim1.collectAsState()
    val sim2 by viewModel.sim2.collectAsState()
    val activeRat by viewModel.currentRat.collectAsState()
    val selectedSlot by viewModel.selectedSimSlot.collectAsState()
    val refreshRate by viewModel.refreshRate.collectAsState()
    val neighborSort by viewModel.neighborSort.collectAsState()
    val showTechDetails by viewModel.showTechnicalDetails.collectAsState()
    val lastUpdatedMs by viewModel.lastUpdatedMs.collectAsState()

    var showRefreshMenu by remember { mutableStateOf(false) }

    var expandSignal by remember { mutableStateOf(true) }
    var expandServing by remember { mutableStateOf(true) }
    var expandIdentifiers by remember { mutableStateOf(false) }
    var expandIms by remember { mutableStateOf(false) }
    var expandNeighbors by remember { mutableStateOf(false) }
    var selectedMetricExplanation by remember { mutableStateOf<MetricExplanation?>(null) }

    val context = LocalContext.current
    val hasPermissions by viewModel.hasPermissions.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val hasPhonePermission by viewModel.hasPhonePermission.collectAsState()

    val permissionGate = rememberPermissionGate()
    val refreshWithPermissions: () -> Unit = {
        permissionGate.require(AppPermission.PHONE, AppPermission.LOCATION, AppPermission.LOCATION_SERVICE) {
            viewModel.checkPermissions()
            viewModel.refresh()
        }
    }

    // Every metric on this screen depends on cell data, so ask as soon as it opens
    LaunchedEffect(Unit) { refreshWithPermissions() }

    // Android Runtime Permission Launcher for Multiple Permissions
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.checkPermissions()
        viewModel.refresh()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    viewModel.checkPermissions()
                    viewModel.startLiveMonitoring()
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> viewModel.stopLiveMonitoring()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLiveMonitoring()
        }
    }

    val currentSub = if (selectedSlot == 0) sim1 else sim2
    val hasSubscription = currentSub.info.subscriptionId != -1
    val serving = currentSub.servingCell
    val secondaryServing = currentSub.secondaryServingCell

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cellular Diagnostics",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Live RF & Baseband Telemetry",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                actions = {
                    // Live polling rate dropdown
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceHighlight,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable { showRefreshMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (refreshRate != LiveRefreshRate.MANUAL) StatusGreen else TextSecondary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = refreshRate.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showRefreshMenu,
                            onDismissRequest = { showRefreshMenu = false },
                            containerColor = SurfaceElevated
                        ) {
                            LiveRefreshRate.values().forEach { rate ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = rate.label,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (refreshRate == rate) AccentCyan else TextPrimary,
                                                fontWeight = if (refreshRate == rate) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    onClick = {
                                        viewModel.setRefreshRate(rate)
                                        showRefreshMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = refreshWithPermissions) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh",
                            tint = AccentCyan
                        )
                    }

                    IconButton(onClick = { viewModel.toggleTechnicalDetails(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = "Technical Details",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // ── 0. RUNTIME PERMISSION REQUIRED WARNING BANNER ──
            if (!hasPermissions) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceElevated,
                        border = BorderStroke(1.dp, StatusAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Cellular Permissions Required",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Android requires Phone & Location permissions to inspect serving cell towers, PCI, RSRP signal quality, and carrier identity without simulating fake values.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val permsToRequest = mutableListOf<String>()
                                        if (!hasPhonePermission) {
                                            permsToRequest.add(android.Manifest.permission.READ_PHONE_STATE)
                                        }
                                        if (!hasLocationPermission) {
                                            permsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                            permsToRequest.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                        }
                                        if (permsToRequest.isNotEmpty()) {
                                            permissionLauncher.launch(permsToRequest.toTypedArray())
                                        }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentCyan,
                                        contentColor = CanvasNavy
                                    )
                                ) {
                                    Text(
                                        text = "Grant Permissions",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = android.net.Uri.fromParts("package", context.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, BorderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                                ) {
                                    Text(
                                        text = "Open Settings",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // ── 1. DUAL SIM SELECTOR TABS ──
            item {
                Spacer(modifier = Modifier.height(6.dp))
                DualSimSelector(
                    sim1 = sim1,
                    sim2 = sim2,
                    selectedSlot = selectedSlot,
                    onSelectSlot = { viewModel.selectSim(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))

                // ── Expand & Shrink Quick Control Bar ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CARDS & TELEMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceHighlight,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable {
                                expandSignal = false
                                expandServing = false
                                expandIdentifiers = false
                                expandIms = false
                                expandNeighbors = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.UnfoldLess,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Shrink All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SurfaceHighlight,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier.clickable {
                                expandSignal = true
                                expandServing = true
                                expandIdentifiers = true
                                expandIms = true
                                expandNeighbors = true
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.UnfoldMore,
                                    contentDescription = null,
                                    tint = AccentCyan,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Expand All",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AccentCyan,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!hasSubscription) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.CellTower,
                        title = "No Active Subscription",
                        description = "SIM slot ${selectedSlot + 1} has no active carrier or subscription card inserted."
                    )
                }
            } else {
                // ── 2. TOP CARRIER & REGISTRATION SUMMARY ──
                item {
                    CarrierSummaryBanner(sub = currentSub, activeRat = activeRat)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 3. SIGNAL SUMMARY (COMPACT & EXPANDABLE 2X2 GRID) ──
                item {
                    SignalSummaryCard(
                        servingCell = serving,
                        overallDbm = currentSub.signalDbm,
                        isExpanded = expandSignal,
                        onToggleExpand = { expandSignal = !expandSignal },
                        onShowMetricInfo = { selectedMetricExplanation = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 4. SERVING CELL SECTION (EXPANDABLE) ──
                item {
                    ServingCellSection(
                        serving = serving,
                        secondaryServing = secondaryServing,
                        sub = currentSub,
                        isExpanded = expandServing,
                        onToggleExpand = { expandServing = !expandServing },
                        onShowMetricInfo = { selectedMetricExplanation = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 5. CURRENT RADIO & NETWORK IDENTIFIERS (EXPANDABLE) ──
                item {
                    NetworkIdentifiersSection(
                        sub = currentSub,
                        serving = serving,
                        isExpanded = expandIdentifiers,
                        onToggleExpand = { expandIdentifiers = !expandIdentifiers },
                        onShowMetricInfo = { selectedMetricExplanation = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 6. IMS / VOICE & ADVANCED CAPABILITIES (EXPANDABLE) ──
                item {
                    ImsVoiceSection(
                        sub = currentSub,
                        isExpanded = expandIms,
                        onToggleExpand = { expandIms = !expandIms },
                        onShowMetricInfo = { selectedMetricExplanation = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 7. NEIGHBOUR CELLS (EXPANDABLE) ──
                item {
                    NeighbourCellsSection(
                        neighbors = currentSub.neighborCells,
                        currentSort = neighborSort,
                        onSortChange = { viewModel.setNeighborSort(it) },
                        isExpanded = expandNeighbors,
                        onToggleExpand = { expandNeighbors = !expandNeighbors }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 8. ADVANCED TECHNICAL DETAILS TRIGGER ──
                item {
                    OutlinedButton(
                        onClick = { viewModel.toggleTechnicalDetails(true) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceBase,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All Technical Details & Raw CellInfo",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }
    }

    // ── Technical Details Bottom Sheet ──
    if (showTechDetails) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleTechnicalDetails(false) },
            containerColor = SurfaceBase,
            scrimColor = Color.Black.copy(alpha = 0.6f)
        ) {
            TechnicalDetailsSheetContent(
                sub = currentSub,
                serving = serving,
                onDismiss = { viewModel.toggleTechnicalDetails(false) }
            )
        }
    }

    // ── Cell Metric Info Dialog ──
    selectedMetricExplanation?.let { explanation ->
        CellMetricInfoDialog(
            metric = explanation,
            onDismiss = { selectedMetricExplanation = null }
        )
    }
}

// ── SIM 1 / SIM 2 Tabs Component ──

@Composable
fun DualSimSelector(
    sim1: SimSubscription,
    sim2: SimSubscription,
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            listOf(0 to sim1, 1 to sim2).forEach { (slot, sub) ->
                val isSelected = selectedSlot == slot
                val hasSim = sub.info.subscriptionId != -1
                val carrier = if (hasSim) sub.info.carrierName.take(12) else "Empty"
                val isData = sub.info.isDefaultData

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) SurfaceHighlight else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectSlot(slot) }
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "SIM ${slot + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) AccentCyan else TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• $carrier",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.5.sp
                            ),
                            maxLines = 1
                        )
                        if (isData) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = StatusGreenBg
                            ) {
                                Text(
                                    text = "DATA",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Top Summary Banner ──

@Composable
fun CarrierSummaryBanner(sub: SimSubscription, activeRat: RadioAccessTechnology) {
    val rat = sub.rat.takeIf { it != RadioAccessTechnology.UNKNOWN } ?: activeRat
    val isRegistered = sub.registrationState.contains("REGISTERED", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sub.info.carrierName.ifBlank { "Cellular Network" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = if (sub.info.isRoaming) "Roaming Active" else "Home Network",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (sub.info.isRoaming) StatusAmber else TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                // RAT Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentCyanBg,
                    border = BorderStroke(1.dp, AccentCyanBorder)
                ) {
                    Text(
                        text = sub.nsaMode?.let { "5G NSA" } ?: rat.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(if (isRegistered) StatusGreen else StatusAmber, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRegistered) "Registered" else "Searching...",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isRegistered) StatusGreen else StatusAmber,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${sub.dataState.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (sub.dataState == "CONNECTED") TextPrimary else TextSecondary,
                            fontSize = 11.5.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // One badge for what voice is using right now (from the voice network type, not a guess)
                    val (voiceBadge, voiceOk) = when {
                        sub.isVoNR -> "VoNR" to true
                        sub.isVoLTE -> "VoLTE" to true
                        sub.isVoWiFi -> "Wi-Fi Calling" to true
                        sub.voiceNetwork.startsWith("2G") || sub.voiceNetwork.startsWith("3G") -> "No VoLTE" to false
                        else -> "Voice ?" to false
                    }
                    Text(
                        text = if (voiceOk) "$voiceBadge ✓" else voiceBadge,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (voiceOk) StatusGreen else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

// ── Signal Summary: Prominent 2x2 Grid with Level & Units ──

@Composable
fun SignalSummaryCard(
    servingCell: CellData?,
    overallDbm: Int?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShowMetricInfo: (MetricExplanation) -> Unit
) {
    val rat = servingCell?.rat ?: RadioAccessTechnology.UNKNOWN
    val is2G = rat == RadioAccessTechnology.EDGE || rat == RadioAccessTechnology.GPRS
    val is3G = rat == RadioAccessTechnology.UMTS || rat == RadioAccessTechnology.HSPA || rat == RadioAccessTechnology.HSPAP
    val is5G = rat == RadioAccessTechnology.NR_SA || rat == RadioAccessTechnology.NR_NSA

    // 4G & General Fallback Values
    val rsrp = servingCell?.ssRsrp ?: servingCell?.rsrp ?: overallDbm
    val rsrq = servingCell?.ssRsrq ?: servingCell?.rsrq
    val sinr = servingCell?.ssSinr ?: servingCell?.sinr
    val rssi = servingCell?.rssi

    val rsrpQuality = classifyRsrp(rsrp)
    val rsrqQuality = classifyRsrq(rsrq)
    val sinrQuality = classifySinr(sinr)

    val cell1Label: String
    val cell1Val: String
    val cell1Quality: MetricQuality
    val cell1InfoKey: String

    val cell2Label: String
    val cell2Val: String
    val cell2Quality: MetricQuality
    val cell2InfoKey: String

    val cell3Label: String
    val cell3Val: String
    val cell3Quality: MetricQuality
    val cell3InfoKey: String

    val cell4Label: String
    val cell4Val: String
    val cell4Quality: MetricQuality
    val cell4InfoKey: String

    val primaryQuality: MetricQuality

    when {
        is2G -> {
            val rssiVal = servingCell?.rssi ?: overallDbm
            val q = classifyRssi(rssiVal)
            cell1Label = "RSSI"
            cell1Val = rssiVal?.let { "$it dBm" } ?: "N/A"
            cell1Quality = q
            cell1InfoKey = "RSRP"

            cell2Label = "BER"
            cell2Val = servingCell?.ber?.let { "$it" } ?: "N/A"
            cell2Quality = MetricQuality.NA
            cell2InfoKey = "RSRQ"

            cell3Label = "TA"
            cell3Val = servingCell?.timingAdvance?.let { "$it" } ?: "N/A"
            cell3Quality = MetricQuality.NA
            cell3InfoKey = "TA"

            cell4Label = "BSIC"
            cell4Val = servingCell?.bsic?.let { "$it" } ?: "N/A"
            cell4Quality = MetricQuality.NA
            cell4InfoKey = "PCI"

            primaryQuality = q
        }
        is3G -> {
            val rscpVal = servingCell?.rscp ?: servingCell?.rssi ?: overallDbm
            val q = classifyRscp(rscpVal)
            cell1Label = "RSCP"
            cell1Val = rscpVal?.let { "$it dBm" } ?: "N/A"
            cell1Quality = q
            cell1InfoKey = "RSRP"

            cell2Label = "Ec/No"
            cell2Val = servingCell?.ecNo?.let { "${String.format("%.1f", it)} dB" } ?: "N/A"
            cell2Quality = MetricQuality.NA
            cell2InfoKey = "RSRQ"

            cell3Label = "RSSI"
            cell3Val = servingCell?.rssi?.let { "$it dBm" } ?: "N/A"
            cell3Quality = classifyRssi(servingCell?.rssi)
            cell3InfoKey = "RSRP"

            cell4Label = "PSC"
            cell4Val = servingCell?.pci?.let { "$it" } ?: "N/A"
            cell4Quality = MetricQuality.NA
            cell4InfoKey = "PCI"

            primaryQuality = q
        }
        is5G -> {
            val ssRsrpVal = servingCell?.ssRsrp ?: servingCell?.rsrp ?: overallDbm
            val ssRsrqVal = servingCell?.ssRsrq ?: servingCell?.rsrq
            val ssSinrVal = servingCell?.ssSinr ?: servingCell?.sinr
            val q = classifyRsrp(ssRsrpVal)

            cell1Label = "SS-RSRP"
            cell1Val = ssRsrpVal?.let { "$it dBm" } ?: "N/A"
            cell1Quality = q
            cell1InfoKey = "RSRP"

            cell2Label = "SS-RSRQ"
            cell2Val = ssRsrqVal?.let { "$it dB" } ?: "N/A"
            cell2Quality = classifyRsrq(ssRsrqVal)
            cell2InfoKey = "RSRQ"

            cell3Label = "CSI-RSRP"
            cell3Val = servingCell?.csiRsrp?.let { "$it dBm" } ?: (rssi?.let { "$it dBm" } ?: "N/A")
            cell3Quality = MetricQuality.NA
            cell3InfoKey = "RSRP"

            cell4Label = "SS-SINR"
            cell4Val = ssSinrVal?.let { "${String.format("%.1f", it)} dB" } ?: "N/A"
            cell4Quality = classifySinr(ssSinrVal)
            cell4InfoKey = "SINR"

            primaryQuality = q
        }
        else -> {
            // 4G LTE default
            cell1Label = "RSRP"
            cell1Val = rsrp?.let { "$it dBm" } ?: "N/A"
            cell1Quality = rsrpQuality
            cell1InfoKey = "RSRP"

            cell2Label = "RSRQ"
            cell2Val = rsrq?.let { "$it dB" } ?: "N/A"
            cell2Quality = rsrqQuality
            cell2InfoKey = "RSRQ"

            cell3Label = "RSSI"
            cell3Val = rssi?.let { "$it dBm" } ?: "N/A"
            cell3Quality = MetricQuality.NA
            cell3InfoKey = "RSRP"

            cell4Label = "SINR"
            cell4Val = sinr?.let { "${String.format("%.1f", it)} dB" } ?: "N/A"
            cell4Quality = sinrQuality
            cell4InfoKey = "SINR"

            primaryQuality = rsrpQuality
        }
    }

    val signalLevel = servingCell?.signalLevel ?: 0

    // Timing Advance & Tower Distance Calculation
    val ta = servingCell?.timingAdvance
    val towerDistanceText = if (ta != null && ta >= 0) {
        val metersPerTa = if (is2G) 554.0 else 78.12
        val meters = (ta * metersPerTa).toInt()
        if (meters >= 1000) {
            String.format("%.2f km (TA: %d)", meters / 1000.0, ta)
        } else {
            "$meters m (TA: $ta)"
        }
    } else null

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Signal Header with Level Meter & Expand/Shrink Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIGNAL METRICS (${rat.label})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            fontSize = 11.sp
                        )
                    )
                }

                // Bars Level (0 - 4 / 5) & Toggle Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalBars(level = signalLevel, maxBars = 4)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Level $signalLevel/4 • ${primaryQuality.label}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = primaryQuality.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "$cell1Label: $cell1Val  •  $cell2Label: $cell2Val  •  $cell4Label: $cell4Val",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // 2x2 High Information Density Grid (RAT Adaptive)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(1.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetricCell(
                            label = cell1Label,
                            value = cell1Val,
                            quality = cell1Quality,
                            modifier = Modifier.weight(1f),
                            infoKey = cell1InfoKey,
                            onShowInfo = onShowMetricInfo
                        )
                        Box(modifier = Modifier.width(1.dp).height(54.dp).background(BorderSubtle))
                        MetricCell(
                            label = cell2Label,
                            value = cell2Val,
                            quality = cell2Quality,
                            modifier = Modifier.weight(1f),
                            infoKey = cell2InfoKey,
                            onShowInfo = onShowMetricInfo
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        MetricCell(
                            label = cell3Label,
                            value = cell3Val,
                            quality = cell3Quality,
                            modifier = Modifier.weight(1f),
                            infoKey = cell3InfoKey,
                            onShowInfo = onShowMetricInfo
                        )
                        Box(modifier = Modifier.width(1.dp).height(54.dp).background(BorderSubtle))
                        MetricCell(
                            label = cell4Label,
                            value = cell4Val,
                            quality = cell4Quality,
                            modifier = Modifier.weight(1f),
                            infoKey = cell4InfoKey,
                            onShowInfo = onShowMetricInfo
                        )
                    }
                }

                // Tower Distance Badge
                if (towerDistanceText != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AccentCyanBg,
                        border = BorderStroke(1.dp, AccentCyanBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CellTower,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Serving Tower Distance: ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = towerDistanceText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCell(
    label: String,
    value: String,
    quality: MetricQuality,
    modifier: Modifier = Modifier,
    infoKey: String? = null,
    onShowInfo: ((MetricExplanation) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .background(SurfaceBase)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (infoKey != null && onShowInfo != null) {
                    val explanation = CellMetricGlossary.GLOSSARY[infoKey.uppercase()]
                    if (explanation != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(AccentCyan.copy(alpha = 0.15f), CircleShape)
                                .clickable { onShowInfo(explanation) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "i",
                                color = AccentCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            if (quality != MetricQuality.NA) {
                Text(
                    text = quality.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = quality.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (value == "N/A") TextSecondary else TextPrimary,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
fun SignalBars(level: Int, maxBars: Int = 4) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.height(14.dp)
    ) {
        for (i in 1..maxBars) {
            val isActive = i <= level
            val heightFraction = i / maxBars.toFloat()
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight(heightFraction)
                    .background(
                        if (isActive) AccentCyan else BorderSubtle,
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

// ── Serving Cell Details Section ──

@Composable
fun ServingCellSection(
    serving: CellData?,
    secondaryServing: CellData?,
    sub: SimSubscription,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShowMetricInfo: (MetricExplanation) -> Unit
) {
    val techLabel = serving?.band ?: serving?.rat?.label ?: "Unknown"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SERVING CELL",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SurfaceHighlight
                    ) {
                        Text(
                            text = techLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                val pciText = serving?.pci?.let { "PCI $it" }
                val gnbText = serving?.enbId?.let { if (serving.rat == RadioAccessTechnology.NR_SA || serving.rat == RadioAccessTechnology.NR_NSA) "gNB $it" else "eNB $it" }
                val arfcnText = serving?.arfcn?.let { "ARFCN $it" }
                val bandText = serving?.band
                val compactSummary = listOfNotNull(bandText, pciText, gnbText, arfcnText).filter { it.isNotBlank() }.joinToString("  •  ")

                Text(
                    text = compactSummary.ifBlank { "Serving Cell Details" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                if (serving == null) {
                    Text(
                        text = "No serving cell reported by Android radio framework.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                } else {
                    val rat = serving.rat
                    val is2G = rat == RadioAccessTechnology.EDGE || rat == RadioAccessTechnology.GPRS
                    val is3G = rat == RadioAccessTechnology.UMTS || rat == RadioAccessTechnology.HSPA || rat == RadioAccessTechnology.HSPAP
                    val isNr = rat == RadioAccessTechnology.NR_SA || rat == RadioAccessTechnology.NR_NSA

                    when {
                        is2G -> {
                            serving.lac?.let { TechnicalRow(label = "Location Area Code (LAC)", value = "$it", infoKey = "TAC", onShowInfo = onShowMetricInfo) }
                            serving.cellId?.let { TechnicalRow(label = "Cell ID (CID)", value = "$it", infoKey = "PCI", onShowInfo = onShowMetricInfo) }
                            serving.arfcn?.let { TechnicalRow(label = "ARFCN (BCCH Channel)", value = "$it", infoKey = "ARFCN", onShowInfo = onShowMetricInfo) }
                            serving.bsic?.let { TechnicalRow(label = "Base Station ID (BSIC)", value = "$it") }
                            serving.timingAdvance?.let {
                                TechnicalRow(label = "Timing Advance (TA)", value = "$it (~${it * 554}m)", infoKey = "TA", onShowInfo = onShowMetricInfo)
                            }
                            serving.ber?.let { TechnicalRow(label = "Bit Error Rate (BER)", value = "$it") }
                        }
                        is3G -> {
                            serving.lac?.let { TechnicalRow(label = "Location Area Code (LAC)", value = "$it", infoKey = "TAC", onShowInfo = onShowMetricInfo) }
                            serving.cellId?.let { TechnicalRow(label = "Cell ID (CID / UCID)", value = "$it", infoKey = "PCI", onShowInfo = onShowMetricInfo) }
                            serving.pci?.let { TechnicalRow(label = "Primary Scrambling Code (PSC)", value = "$it", infoKey = "PCI", onShowInfo = onShowMetricInfo) }
                            serving.arfcn?.let { TechnicalRow(label = "UARFCN", value = "$it", infoKey = "ARFCN", onShowInfo = onShowMetricInfo) }
                            serving.rscp?.let { TechnicalRow(label = "RSCP", value = "$it dBm", infoKey = "RSRP", onShowInfo = onShowMetricInfo) }
                            serving.ecNo?.let { TechnicalRow(label = "Ec/No", value = "${String.format("%.1f", it)} dB", infoKey = "RSRQ", onShowInfo = onShowMetricInfo) }
                        }
                        isNr -> {
                            serving.pci?.let { TechnicalRow(label = "Physical Cell ID (PCI)", value = "$it", infoKey = "PCI", onShowInfo = onShowMetricInfo) }
                            serving.tac?.let { TechnicalRow(label = "Tracking Area Code (TAC)", value = "$it", infoKey = "TAC", onShowInfo = onShowMetricInfo) }
                            serving.cellId?.let { TechnicalRow(label = "NR Cell ID (NCI)", value = "$it") }
                            serving.enbId?.let { TechnicalRow(label = "gNodeB ID", value = "$it (Sector ${serving.sectorId ?: 0})", infoKey = "GNB", onShowInfo = onShowMetricInfo) }
                            serving.arfcn?.let { TechnicalRow(label = "NR-ARFCN", value = "$it", infoKey = "ARFCN", onShowInfo = onShowMetricInfo) }
                            serving.bandwidthKhz?.let {
                                TechnicalRow(label = "Channel Bandwidth", value = "${it / 1000} MHz (${it} kHz)", infoKey = "BAND", onShowInfo = onShowMetricInfo)
                            }
                            serving.timingAdvance?.let {
                                val approxMeters = (it * 78.12).toInt()
                                TechnicalRow(label = "Timing Advance (TA)", value = "$it (~${approxMeters}m)", infoKey = "TA", onShowInfo = onShowMetricInfo)
                            }
                            serving.csiRsrp?.let { TechnicalRow(label = "CSI-RSRP", value = "$it dBm", infoKey = "RSRP", onShowInfo = onShowMetricInfo) }
                            serving.csiRsrq?.let { TechnicalRow(label = "CSI-RSRQ", value = "$it dB", infoKey = "RSRQ", onShowInfo = onShowMetricInfo) }
                            serving.csiSinr?.let { TechnicalRow(label = "CSI-SINR", value = "${String.format("%.1f", it)} dB", infoKey = "SINR", onShowInfo = onShowMetricInfo) }
                        }
                        else -> {
                            // 4G LTE
                            serving.pci?.let { TechnicalRow(label = "Physical Cell ID (PCI)", value = "$it", infoKey = "PCI", onShowInfo = onShowMetricInfo) }
                            serving.tac?.let { TechnicalRow(label = "Tracking Area Code (TAC)", value = "$it", infoKey = "TAC", onShowInfo = onShowMetricInfo) }
                            serving.cellId?.let { TechnicalRow(label = "Cell ID (ECI)", value = "$it") }
                            serving.enbId?.let { TechnicalRow(label = "eNodeB ID", value = "$it (Sector ${serving.sectorId ?: 0})", infoKey = "GNB", onShowInfo = onShowMetricInfo) }
                            serving.arfcn?.let { TechnicalRow(label = "EARFCN", value = "$it", infoKey = "ARFCN", onShowInfo = onShowMetricInfo) }
                            serving.bandwidthKhz?.let {
                                TechnicalRow(label = "Channel Bandwidth", value = "${it / 1000} MHz (${it} kHz)", infoKey = "BAND", onShowInfo = onShowMetricInfo)
                            }
                            serving.timingAdvance?.let {
                                val approxMeters = (it * 78.12).toInt()
                                TechnicalRow(label = "Timing Advance (TA)", value = "$it (~${approxMeters}m)", infoKey = "TA", onShowInfo = onShowMetricInfo)
                            }
                            serving.cqi?.let { TechnicalRow(label = "Channel Quality (CQI)", value = "$it") }
                        }
                    }
                }

                // Secondary Serving Cell (e.g. NR secondary carrier in 5G NSA)
                if (secondaryServing != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECONDARY CARRIER (NR NSA)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                        Text(
                            text = secondaryServing.band ?: secondaryServing.rat.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TechnicalRow(label = "NR PCI", value = secondaryServing.pci?.toString() ?: "N/A", infoKey = "PCI", onShowInfo = onShowMetricInfo)
                    TechnicalRow(label = "NR-ARFCN", value = secondaryServing.arfcn?.toString() ?: "N/A", infoKey = "ARFCN", onShowInfo = onShowMetricInfo)
                    secondaryServing.ssRsrp?.let { TechnicalRow(label = "SS-RSRP", value = "$it dBm", infoKey = "RSRP", onShowInfo = onShowMetricInfo) }
                    secondaryServing.ssSinr?.let { TechnicalRow(label = "SS-SINR", value = "${it.toInt()} dB", infoKey = "SINR", onShowInfo = onShowMetricInfo) }
                }
            }
        }
    }
}

// ── Network Identifiers Section ──

@Composable
fun NetworkIdentifiersSection(
    sub: SimSubscription,
    serving: CellData?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShowMetricInfo: (MetricExplanation) -> Unit
) {
    val info = sub.info
    val mcc = info.mcc ?: serving?.mcc ?: "N/A"
    val mnc = info.mnc ?: serving?.mnc ?: "N/A"
    val plmn = info.plmn ?: if (mcc != "N/A" && mnc != "N/A") "$mcc$mnc" else "N/A"

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NETWORK IDENTIFIERS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PLMN $plmn  •  ${if (info.isRoaming) "Roaming" else "Home Network"}  •  ${if (info.isEmbedded) "eSIM" else "Physical SIM"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                TechnicalRow(label = "Mobile Country Code (MCC)", value = mcc)
                TechnicalRow(label = "Mobile Network Code (MNC)", value = mnc)
                TechnicalRow(label = "PLMN Numeric", value = plmn, infoKey = "PLMN", onShowInfo = onShowMetricInfo)
                TechnicalRow(label = "Country ISO", value = info.countryIso.uppercase().ifBlank { "N/A" })
                TechnicalRow(label = "Roaming", value = if (info.isRoaming) "Yes (Roaming)" else "No (Home)")
                TechnicalRow(label = "Embedded SIM (eSIM)", value = if (info.isEmbedded) "Yes" else "No")
            }
        }
    }
}

// ── IMS / Voice Section ──

@Composable
fun ImsVoiceSection(
    sub: SimSubscription,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onShowMetricInfo: (MetricExplanation) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IMS & VOICE SERVICES",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Voice now: ${sub.voiceNetwork}" + (sub.lastCallVoice?.let { "  •  Last call: $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    ),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Live: which network voice uses right now
                ImsCapabilityRow(
                    label = "Voice right now",
                    isAvailable = sub.isVoLTE || sub.isVoNR || sub.isVoWiFi,
                    statusText = sub.voiceNetwork,
                    infoKey = "IMS",
                    onShowInfo = onShowMetricInfo
                )
                // Confirmation: what the last real call used (measured during the call)
                ImsCapabilityRow(
                    label = "Last call",
                    isAvailable = sub.lastCallVoice?.contains("confirmed") == true,
                    statusText = sub.lastCallVoice?.let { result ->
                        val time = sub.lastCallTimeMs?.let {
                            java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
                        }
                        if (time != null) "$result • $time" else result
                    } ?: "Make a call to confirm"
                )

                // Carrier support for this SIM (carrier config); "Not declared" when the carrier doesn't say
                ImsCapabilityRow(
                    label = "VoLTE supported",
                    isAvailable = sub.volteSupported == true,
                    statusText = supportText(sub.volteSupported, inUse = sub.isVoLTE),
                    infoKey = "VOLTE",
                    onShowInfo = onShowMetricInfo
                )
                ImsCapabilityRow(
                    label = "VoNR / Vo5G supported",
                    isAvailable = sub.vonrSupported == true,
                    statusText = supportText(sub.vonrSupported, inUse = sub.isVoNR),
                    infoKey = "VONR",
                    onShowInfo = onShowMetricInfo
                )
                ImsCapabilityRow(
                    label = "Wi-Fi Calling supported",
                    isAvailable = sub.vowifiSupported == true,
                    statusText = supportText(sub.vowifiSupported, inUse = sub.isVoWiFi),
                    infoKey = "VOWIFI",
                    onShowInfo = onShowMetricInfo
                )
                ImsCapabilityRow(
                    label = "Video Calling (ViLTE)",
                    isAvailable = sub.videoCallingSupported,
                    statusText = if (sub.videoCallingSupported) "Supported by carrier ✓" else "Not declared by carrier"
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"Supported\" comes from your carrier's settings for this SIM. Android doesn't let normal apps " +
                        "read IMS registration directly, so the last call is the real confirmation.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                )
            }
        }
    }
}

private fun supportText(supported: Boolean?, inUse: Boolean): String = when {
    inUse -> "In use now ✓"
    supported == true -> "Supported by carrier ✓"
    supported == false -> "Not supported by carrier"
    else -> "Not declared by carrier"
}

@Composable
fun ImsCapabilityRow(
    label: String,
    isAvailable: Boolean,
    statusText: String,
    infoKey: String? = null,
    onShowInfo: ((MetricExplanation) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
            if (infoKey != null && onShowInfo != null) {
                val explanation = CellMetricGlossary.GLOSSARY[infoKey.uppercase()]
                if (explanation != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(AccentCyan.copy(alpha = 0.15f), CircleShape)
                            .clickable { onShowInfo(explanation) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isAvailable) StatusGreen else TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        )
    }
}

// ── Neighbour Cells Section ──

@Composable
fun NeighbourCellsSection(
    neighbors: List<CellData>,
    currentSort: NeighborSort,
    onSortChange: (NeighborSort) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    // Sort neighbors
    val sortedNeighbors = remember(neighbors, currentSort) {
        when (currentSort) {
            NeighborSort.SIGNAL -> neighbors.sortedByDescending { it.rsrp ?: it.ssRsrp ?: it.rssi ?: -999 }
            NeighborSort.TECHNOLOGY -> neighbors.sortedBy { it.rat.label }
            NeighborSort.PCI -> neighbors.sortedBy { it.pci ?: 99999 }
            NeighborSort.FREQUENCY -> neighbors.sortedBy { it.arfcn ?: 99999 }
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with Title and Expand/Collapse Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEIGHBOUR CELLS (${neighbors.size})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (!isExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (neighbors.isEmpty()) "No neighbour cells detected nearby" else "${neighbors.size} detected nearby (Tap to view details & channels)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))

                // Sort selector chips on a dedicated horizontal row with non-wrapping text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sort:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    NeighborSort.values().forEach { sort ->
                        val isSelected = currentSort == sort
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) SurfaceHighlight else Color.Transparent,
                            border = BorderStroke(0.5.dp, if (isSelected) AccentCyanBorder else BorderSubtle),
                            modifier = Modifier.clickable { onSortChange(sort) }
                        ) {
                            Text(
                                text = sort.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isSelected) AccentCyan else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (sortedNeighbors.isEmpty()) {
                    Text(
                        text = "No neighbour cells reported by modem. Some OEM chipsets restrict neighbour scanning during active cellular standby to conserve battery.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        )
                    )
                } else {
                    sortedNeighbors.forEachIndexed { index, cell ->
                        val rsrp = cell.rsrp ?: cell.ssRsrp ?: cell.rssi
                        val quality = classifyRsrp(rsrp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = SurfaceHighlight
                                    ) {
                                        Text(
                                            text = cell.rat.shortLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = AccentCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PCI ${cell.pci ?: "N/A"}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                Text(
                                    text = cell.band ?: "ARFCN ${cell.arfcn ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = rsrp?.let { "$it dBm" } ?: "N/A",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = quality.color,
                                        fontSize = 13.sp
                                    )
                                )
                                val rsrq = cell.rsrq ?: cell.ssRsrq
                                if (rsrq != null) {
                                    Text(
                                        text = "RSRQ $rsrq dB",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary,
                                            fontSize = 10.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        if (index < sortedNeighbors.size - 1) {
                            HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

// ── Technical Key-Value Row Helper ──

@Composable
fun TechnicalRow(
    label: String,
    value: String,
    highlightColor: Color? = null,
    infoKey: String? = null,
    onShowInfo: ((MetricExplanation) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
            if (infoKey != null && onShowInfo != null) {
                val explanation = CellMetricGlossary.GLOSSARY[infoKey.uppercase()]
                if (explanation != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(AccentCyan.copy(alpha = 0.15f), CircleShape)
                            .clickable { onShowInfo(explanation) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            color = AccentCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = highlightColor ?: if (value == "N/A") TextSecondary else TextPrimary,
                fontWeight = if (value == "N/A") FontWeight.Normal else FontWeight.Medium,
                fontSize = 12.sp
            )
        )
    }
}

// ── Technical Details Bottom Sheet Content ──

@Composable
fun TechnicalDetailsSheetContent(
    sub: SimSubscription,
    serving: CellData?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Telephony Framework Details",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TechnicalRow(label = "Subscription ID", value = "${sub.info.subscriptionId}")
        TechnicalRow(label = "SIM Hardware Slot", value = "${sub.info.slotIndex}")
        TechnicalRow(label = "Display Name", value = sub.info.displayName)
        TechnicalRow(label = "Connection Status", value = serving?.connectionStatus ?: "Reported Active")
        TechnicalRow(label = "Timestamp (Nanoseconds)", value = serving?.timestampNs?.takeIf { it > 0 }?.toString() ?: "N/A")
        TechnicalRow(label = "Data Network State", value = sub.dataState)
        TechnicalRow(label = "Signal Measurement Level", value = "${serving?.signalLevel ?: 0} / 4")
        TechnicalRow(label = "Default Data Subscription", value = if (sub.info.isDefaultData) "True" else "False")

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Values are extracted directly from Android TelephonyManager and SubscriptionManager APIs without artificial estimation.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        )
    }
}

// ── Network Mode Screen (Preserved) ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkModeScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentRat by viewModel.currentRat.collectAsState()
    val sim1 by viewModel.sim1.collectAsState()

    val modes = listOf("Auto (5G/4G/3G)", "4G / LTE Only", "5G Preferred", "5G Only")
    val defaultMode = when {
        currentRat == RadioAccessTechnology.NR_SA -> "5G Only"
        currentRat == RadioAccessTechnology.NR_NSA -> "5G Preferred"
        currentRat == RadioAccessTechnology.LTE || currentRat == RadioAccessTechnology.LTE_CA -> "4G / LTE Only"
        else -> "Auto (5G/4G/3G)"
    }
    var selectedMode by remember(currentRat) { mutableStateOf(defaultMode) }

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Text(
                        text = "Network Mode",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceBase,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RADIO PREFERENCE STATE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextSecondary,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KeyValueRow(label = "Observed RAT", value = currentRat.label, highlightColor = AccentCyan)
                    KeyValueRow(label = "Active Carrier", value = sim1.info.carrierName)
                    KeyValueRow(label = "Current Policy", value = selectedMode)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceElevated,
                border = BorderStroke(1.dp, StatusAmberBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Carrier / System Restricted",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = StatusAmber,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Direct radio band lock requires system privileges (MODIFY_PHONE_STATE). Standard apps can launch Android's carrier settings.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "REQUEST PREFERRED RAT",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            modes.forEach { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) SurfaceHighlight else SurfaceBase,
                    border = BorderStroke(1.dp, if (isSelected) AccentCyanBorder else BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedMode = mode }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = mode,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isSelected) AccentCyan else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedMode = mode },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentCyan,
                                unselectedColor = TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try { context.startActivity(fallback) } catch (_: Exception) { }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = CanvasNavy
                )
            ) {
                Text(
                    text = "Open Android Cellular Settings",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
