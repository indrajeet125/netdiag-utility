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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
    GOOD("Good", AccentCyan, AccentCyanBg),
    FAIR("Fair", StatusAmber, StatusAmberBg),
    POOR("Poor", StatusRed, StatusRedBg),
    NA("N/A", TextSecondary, Color.Transparent)
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

                    IconButton(onClick = { viewModel.refresh() }) {
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
            // ── 1. DUAL SIM SELECTOR TABS ──
            item {
                Spacer(modifier = Modifier.height(6.dp))
                DualSimSelector(
                    sim1 = sim1,
                    sim2 = sim2,
                    selectedSlot = selectedSlot,
                    onSelectSlot = { viewModel.selectSim(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 3. SIGNAL SUMMARY — VERY IMPORTANT (PROMINENT COMPACT 2X2 GRID) ──
                item {
                    SignalSummaryCard(servingCell = serving, overallDbm = currentSub.signalDbm)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 4. SERVING CELL SECTION ──
                item {
                    ServingCellSection(serving = serving, secondaryServing = secondaryServing, sub = currentSub)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 5. CURRENT RADIO & NETWORK IDENTIFIERS ──
                item {
                    NetworkIdentifiersSection(sub = currentSub, serving = serving)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 6. IMS / VOICE & ADVANCED CAPABILITIES ──
                item {
                    ImsVoiceSection(sub = currentSub)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ── 7. NEIGHBOUR CELLS ──
                item {
                    NeighbourCellsSection(
                        neighbors = currentSub.neighborCells,
                        currentSort = neighborSort,
                        onSortChange = { viewModel.setNeighborSort(it) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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

            // Sub-status chips
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
                }

                Text(
                    text = "Data: ${sub.dataState.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (sub.dataState == "CONNECTED") TextPrimary else TextSecondary,
                        fontSize = 11.5.sp
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "VoLTE ${if (sub.isVoLTE) "✓" else "✕"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (sub.isVoLTE) StatusGreen else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VoWiFi ${if (sub.isVoWiFi) "✓" else "✕"}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (sub.isVoWiFi) StatusGreen else TextSecondary,
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
fun SignalSummaryCard(servingCell: CellData?, overallDbm: Int?) {
    // Determine live metrics (checking NR SS values or LTE values)
    val rsrp = servingCell?.ssRsrp ?: servingCell?.rsrp ?: overallDbm
    val rsrq = servingCell?.ssRsrq ?: servingCell?.rsrq
    val sinr = servingCell?.ssSinr ?: servingCell?.sinr
    val rssi = servingCell?.rssi

    val rsrpQuality = classifyRsrp(rsrp)
    val rsrqQuality = classifyRsrq(rsrq)
    val sinrQuality = classifySinr(sinr)

    val signalLevel = servingCell?.signalLevel ?: 0

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Signal Header with Level Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SIGNAL METRICS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            fontSize = 11.sp
                        )
                    )
                }

                // Bars Level (0 - 4 / 5)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignalBars(level = signalLevel, maxBars = 4)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Level $signalLevel/4 • ${rsrpQuality.label}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = rsrpQuality.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2x2 High Information Density Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(1.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        label = "RSRP",
                        value = rsrp?.let { "$it dBm" } ?: "N/A",
                        quality = rsrpQuality,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(54.dp).background(BorderSubtle))
                    MetricCell(
                        label = "RSRQ",
                        value = rsrq?.let { "$it dB" } ?: "N/A",
                        quality = rsrqQuality,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderSubtle))

                Row(modifier = Modifier.fillMaxWidth()) {
                    MetricCell(
                        label = "RSSI",
                        value = rssi?.let { "$it dBm" } ?: "N/A",
                        quality = MetricQuality.NA,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(54.dp).background(BorderSubtle))
                    MetricCell(
                        label = "SINR",
                        value = sinr?.let { "${String.format("%.1f", it)} dB" } ?: "N/A",
                        quality = sinrQuality,
                        modifier = Modifier.weight(1f)
                    )
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
    modifier: Modifier = Modifier
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
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
    sub: SimSubscription
) {
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
                Text(
                    text = "SERVING CELL",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        fontSize = 11.sp
                    )
                )

                val techLabel = serving?.band ?: serving?.rat?.label ?: "Unknown"
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (serving == null) {
                Text(
                    text = "No serving cell reported by Android radio framework.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            } else {
                // Technical Grid of Serving Identifiers
                TechnicalRow(label = "Physical Cell ID (PCI)", value = serving.pci?.toString() ?: "N/A")
                TechnicalRow(label = "Tracking Area Code (TAC)", value = serving.tac?.toString() ?: "N/A")

                val isNr = serving.rat == RadioAccessTechnology.NR_SA || serving.rat == RadioAccessTechnology.NR_NSA
                if (isNr) {
                    TechnicalRow(label = "NR Cell ID (NCI)", value = serving.cellId?.toString() ?: "N/A")
                    serving.enbId?.let { TechnicalRow(label = "gNodeB ID", value = "$it (Sector ${serving.sectorId ?: 0})") }
                    TechnicalRow(label = "NR-ARFCN", value = serving.arfcn?.toString() ?: "N/A")
                } else {
                    TechnicalRow(label = "Cell ID (ECI)", value = serving.cellId?.toString() ?: "N/A")
                    serving.enbId?.let { TechnicalRow(label = "eNodeB ID", value = "$it (Sector ${serving.sectorId ?: 0})") }
                    TechnicalRow(label = "EARFCN", value = serving.arfcn?.toString() ?: "N/A")
                }

                serving.bandwidthKhz?.let {
                    TechnicalRow(label = "Channel Bandwidth", value = "${it / 1000} MHz (${it} kHz)")
                }

                serving.timingAdvance?.let {
                    val approxMeters = it * 78 // approx 78 meters per TA unit for LTE
                    TechnicalRow(label = "Timing Advance (TA)", value = "$it (~${approxMeters}m)")
                }

                serving.cqi?.let {
                    TechnicalRow(label = "Channel Quality (CQI)", value = "$it")
                }

                // 5G CSI metrics if NR
                if (isNr) {
                    TechnicalRow(
                        label = "CSI-RSRP",
                        value = serving.csiRsrp?.let { "$it dBm" } ?: "N/A"
                    )
                    TechnicalRow(
                        label = "CSI-RSRQ",
                        value = serving.csiRsrq?.let { "$it dB" } ?: "N/A"
                    )
                    TechnicalRow(
                        label = "CSI-SINR",
                        value = serving.csiSinr?.let { "${String.format("%.1f", it)} dB" } ?: "N/A"
                    )
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
                TechnicalRow(label = "NR PCI", value = secondaryServing.pci?.toString() ?: "N/A")
                TechnicalRow(label = "NR-ARFCN", value = secondaryServing.arfcn?.toString() ?: "N/A")
                secondaryServing.ssRsrp?.let { TechnicalRow(label = "SS-RSRP", value = "$it dBm") }
                secondaryServing.ssSinr?.let { TechnicalRow(label = "SS-SINR", value = "${it.toInt()} dB") }
            }
        }
    }
}

// ── Network Identifiers Section ──

@Composable
fun NetworkIdentifiersSection(sub: SimSubscription, serving: CellData?) {
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
            Text(
                text = "NETWORK IDENTIFIERS",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            TechnicalRow(label = "Mobile Country Code (MCC)", value = mcc)
            TechnicalRow(label = "Mobile Network Code (MNC)", value = mnc)
            TechnicalRow(label = "PLMN Numeric", value = plmn)
            TechnicalRow(label = "Country ISO", value = info.countryIso.uppercase().ifBlank { "N/A" })
            TechnicalRow(label = "Roaming", value = if (info.isRoaming) "Yes (Roaming)" else "No (Home)")
            TechnicalRow(label = "Embedded SIM (eSIM)", value = if (info.isEmbedded) "Yes" else "No")
        }
    }
}

// ── IMS / Voice Section ──

@Composable
fun ImsVoiceSection(sub: SimSubscription) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "IMS & VOICE SERVICES",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp,
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            ImsCapabilityRow(
                label = "IMS Registration",
                isAvailable = sub.isImsRegistered,
                statusText = if (sub.isImsRegistered) "Registered ✓" else "Not registered"
            )
            ImsCapabilityRow(
                label = "VoLTE (Voice over LTE)",
                isAvailable = sub.isVoLTE,
                statusText = if (sub.isVoLTE) "Available ✓" else "Unavailable"
            )
            ImsCapabilityRow(
                label = "VoWiFi / IWLAN (Wi-Fi Calling)",
                isAvailable = sub.isVoWiFi,
                statusText = if (sub.isVoWiFi) "Active ✓" else "Inactive"
            )
            ImsCapabilityRow(
                label = "Video Calling (ViLTE)",
                isAvailable = sub.videoCallingSupported,
                statusText = if (sub.videoCallingSupported) "Supported ✓" else "Unavailable"
            )
        }
    }
}

@Composable
fun ImsCapabilityRow(label: String, isAvailable: Boolean, statusText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 12.sp
            )
        )
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
    onSortChange: (NeighborSort) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                // Sort selector chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

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

// ── Technical Key-Value Row Helper ──

@Composable
fun TechnicalRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                fontSize = 12.sp
            )
        )
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
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentRat by viewModel.currentRat.collectAsState()
    val sim1 by viewModel.sim1.collectAsState()

    var selectedMode by remember { mutableStateOf("Auto") }
    val modes = listOf("Auto (5G/4G/3G)", "4G / LTE Only", "5G Preferred", "5G Only")

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
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
                    .height(48.dp),
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
