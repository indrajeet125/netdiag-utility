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
import androidx.compose.ui.graphics.Color
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Diagnostic Progress Tracker (e.g. "Running in 10 • Status 20%")
                    DiagnosticProgressBar(
                        currentStep = probes.size,
                        totalSteps = state.config.count,
                        targetHost = state.config.target,
                        lastRttMs = probes.lastOrNull()?.totalTimeMs,
                        isRunning = isRunning
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time Waveform Graph
                    PingOscilloscopeChart(probes = probes)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Packet Stream Matrix
                    PacketMatrixGrid(probes = probes, totalCount = state.config.count)

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

@Composable
fun PingOscilloscopeChart(
    probes: List<SinglePingProbe>,
    modifier: Modifier = Modifier
) {
    if (probes.isEmpty()) return

    val accentColor = AccentCyan
    val statusRedColor = StatusRed
    val gridColor = BorderSubtle.copy(alpha = 0.4f)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REAL-TIME LATENCY OSCILLOSCOPE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                val lastMs = probes.lastOrNull()?.totalTimeMs ?: 0
                Text(
                    text = "$lastMs ms",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (lastMs > 100) StatusAmber else AccentCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val w = size.width
                val h = size.height

                // Draw 3 horizontal grid lines
                for (i in 0..3) {
                    val y = h * (i / 3f)
                    drawLine(
                        color = gridColor,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                val recentProbes = probes.takeLast(30)
                if (recentProbes.size < 2) return@Canvas

                val maxLatency = (recentProbes.maxOfOrNull { it.totalTimeMs } ?: 80L).coerceAtLeast(60L).toFloat()
                val stepX = w / (recentProbes.size - 1)

                val path = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()

                recentProbes.forEachIndexed { i, p ->
                    val x = i * stepX
                    val yFraction = (p.totalTimeMs / maxLatency).coerceIn(0.08f, 0.92f)
                    val y = h - (h * yFraction)

                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                }

                fillPath.lineTo((recentProbes.size - 1) * stepX, h)
                fillPath.close()

                // Gradient fill under waveform
                drawPath(
                    path = fillPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )

                // Stroke curve
                drawPath(
                    path = path,
                    color = accentColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                // Current tracking dot
                val lastProbe = recentProbes.last()
                val lastX = (recentProbes.size - 1) * stepX
                val lastY = h - (h * (lastProbe.totalTimeMs / maxLatency).coerceIn(0.08f, 0.92f))
                drawCircle(
                    color = if (lastProbe.isSuccess) accentColor else statusRedColor,
                    radius = 4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(lastX, lastY)
                )
            }
        }
    }
}

@Composable
fun PacketMatrixGrid(
    probes: List<SinglePingProbe>,
    totalCount: Int = 10,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceElevated,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PACKET STREAM MATRIX",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "${probes.size} / $totalCount Packets",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = AccentCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (seq in 1..totalCount) {
                    val probe = probes.find { it.sequence == seq }
                    val color = when {
                        probe == null -> BorderSubtle.copy(alpha = 0.35f)
                        !probe.isSuccess -> StatusRed
                        probe.totalTimeMs < 45 -> StatusGreen
                        probe.totalTimeMs < 90 -> AccentCyan
                        else -> StatusAmber
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(22.dp)
                            .background(color, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (probe != null) {
                            Text(
                                text = "$seq",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
