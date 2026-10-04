package com.netdiag.tool.ui.ping

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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.*
import com.netdiag.tool.ui.components.*
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(
    pingType: String = "http",
    viewModel: PingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableStateOf(if (pingType == "tcp") PingType.TCP else PingType.HTTP) }
    var targetInput by remember { mutableStateOf(state.config.target) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        viewModel.updateConfig(state.config.copy(type = selectedTab))
    }

    val report = state.report
    val probes = state.liveProbes
    val isRunning = state.isRunning

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ping Diagnostics",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Config",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceBase,
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.stopPing() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed,
                                contentColor = CanvasNavy
                            )
                        ) {
                            Text(
                                text = "Stop",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.updateConfig(
                                    state.config.copy(
                                        target = targetInput.trim().ifEmpty { "google.com" },
                                        type = selectedTab
                                    )
                                )
                                viewModel.startPing()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = CanvasNavy
                            )
                        ) {
                            Text(
                                text = if (report != null) "Test again" else "Start test",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Segmented Tabs: HTTP / TCP
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        val tabs = listOf(PingType.HTTP to "HTTP PING", PingType.TCP to "TCP SOCKET")
                        tabs.forEach { (type, label) ->
                            val isSelected = selectedTab == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) SurfaceHighlight else SurfaceElevated,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedTab = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) AccentCyan else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Input Field
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Host / URL", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Live Diagnostic Telemetry Hero
            if (isRunning || report != null) {
                item {
                    val avgLatency = report?.avgMs?.toInt() 
                        ?: if (probes.isNotEmpty()) (probes.map { it.totalTimeMs }.average()).toInt() else 0
                    val minLatency = report?.minMs ?: probes.minOfOrNull { it.totalTimeMs } ?: 0
                    val maxLatency = report?.maxMs ?: probes.maxOfOrNull { it.totalTimeMs } ?: 0
                    val lossPercent = report?.lossPercent 
                        ?: if (probes.isNotEmpty()) {
                            val failed = probes.count { !it.isSuccess }
                            ((failed.toFloat() / probes.size) * 100)
                        } else 0f

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SurfaceBase,
                        border = BorderStroke(1.dp, if (isRunning) AccentCyanBorder else BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isRunning) "RUNNING DIAGNOSTIC" else "TEST COMPLETE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isRunning) AccentCyan else StatusGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "$avgLatency ms",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    color = if (lossPercent > 20f) StatusAmber else TextPrimary
                                )
                            )

                            Text(
                                text = "AVERAGE LATENCY",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricChip(label = "MIN", value = "$minLatency ms")
                                MetricChip(label = "AVG", value = "$avgLatency ms")
                                MetricChip(label = "MAX", value = "$maxLatency ms")
                                MetricChip(label = "LOSS", value = "${lossPercent.toInt()}%")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "PROBE SEQUENCE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextTertiary,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(probes, key = { it.sequence }) { probe ->
                    ProbeRow(probe = probe)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Collapsible Technical Waterfall Breakdown
                if (report != null && probes.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(14.dp))
                        val sampleProbe = probes.firstOrNull { it.isSuccess } ?: probes.first()

                        ExpandableSection(title = "Technical Timing Breakdown") {
                            sampleProbe.dnsTimeMs?.let {
                                KeyValueRow(label = "DNS Resolution", value = "$it ms")
                            }
                            sampleProbe.connectTimeMs?.let {
                                KeyValueRow(label = "TCP Handshake", value = "$it ms")
                            }
                            sampleProbe.tlsTimeMs?.let {
                                KeyValueRow(label = "TLS / SSL", value = "$it ms")
                            }
                            sampleProbe.ttfbMs?.let {
                                KeyValueRow(label = "Time To First Byte (TTFB)", value = "$it ms")
                            }
                            KeyValueRow(
                                label = "Total Latency",
                                value = "${sampleProbe.totalTimeMs} ms",
                                highlightColor = AccentCyan
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Speed,
                        title = "Ready to Test",
                        description = "Choose protocol (HTTP or TCP Socket) and press Start Test to begin network latency probes."
                    )
                }
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Probe Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary)
                )
            },
            text = {
                Column {
                    KeyValueRow(label = "Probe Count", value = "${state.config.count}")
                    KeyValueRow(label = "Timeout", value = "${state.config.timeoutMs} ms")
                    KeyValueRow(label = "Port (TCP)", value = "${state.config.port}")
                    KeyValueRow(label = "Interval", value = "${state.config.intervalMs} ms")
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("OK", color = AccentCyan)
                }
            },
            containerColor = SurfaceElevated
        )
    }
}

@Composable
fun ProbeRow(probe: SinglePingProbe) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(if (probe.isSuccess) StatusGreenBg else StatusRedBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (probe.isSuccess) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (probe.isSuccess) StatusGreen else StatusRed,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Seq ${probe.sequence}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }

            Text(
                text = if (probe.isSuccess) "${probe.totalTimeMs} ms" else (probe.failureReason ?: "Timeout"),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (probe.isSuccess) TextPrimary else StatusRed
                )
            )
        }
    }
}
