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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

    // Custom Target Port (for TCP and HTTP)
    var portInput by remember { mutableStateOf(state.config.port.toString()) }
    var isCustomPort by remember { mutableStateOf(false) }

    // Attempts / Probe Count
    var attemptCount by remember { mutableStateOf(10) }
    var isCustomCount by remember { mutableStateOf(false) }
    var customCountInput by remember { mutableStateOf("10") }

    // ICMP Payload Bytes
    var payloadBytes by remember { mutableStateOf(64) }
    var isCustomPayload by remember { mutableStateOf(false) }
    var customPayloadInput by remember { mutableStateOf("64") }

    // Timing: Timeout (ms) & Interval (ms)
    var timeoutMs by remember { mutableStateOf(2000) }
    var isCustomTimeout by remember { mutableStateOf(false) }
    var customTimeoutInput by remember { mutableStateOf("2000") }

    var intervalMs by remember { mutableStateOf(500L) }
    var isCustomInterval by remember { mutableStateOf(false) }
    var customIntervalInput by remember { mutableStateOf("500") }

    // Advanced / Custom Parameters Card Expand State
    var showAdvancedOptions by remember { mutableStateOf(false) }

    val effectiveCount = if (isCustomCount) {
        customCountInput.toIntOrNull()?.coerceIn(1, 1000) ?: attemptCount
    } else {
        attemptCount
    }

    val effectivePort = if (isCustomPort) {
        portInput.toIntOrNull()?.coerceIn(1, 65535) ?: 443
    } else {
        portInput.toIntOrNull()?.coerceIn(1, 65535) ?: if (selectedTab == PingType.TCP) 443 else 443
    }

    val effectivePayload = if (isCustomPayload) {
        customPayloadInput.toIntOrNull()?.coerceIn(16, 65507) ?: payloadBytes
    } else {
        payloadBytes
    }

    val effectiveTimeout = if (isCustomTimeout) {
        customTimeoutInput.toIntOrNull()?.coerceIn(100, 30000) ?: timeoutMs
    } else {
        timeoutMs
    }

    val effectiveInterval = if (isCustomInterval) {
        customIntervalInput.toLongOrNull()?.coerceIn(20L, 10000L) ?: intervalMs
    } else {
        intervalMs
    }

    LaunchedEffect(selectedTab) {
        viewModel.updateConfig(state.config.copy(type = selectedTab))
    }

    val report = state.report
    val probes = state.liveProbes
    val isRunning = state.isRunning

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ping Diagnostics",
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Config",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (isRunning) {
                        Button(
                            onClick = { viewModel.stopPing() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusRed,
                                contentColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = "STOP TEST",
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
                                        type = selectedTab,
                                        port = effectivePort,
                                        count = effectiveCount,
                                        payloadBytes = effectivePayload,
                                        timeoutMs = effectiveTimeout,
                                        intervalMs = effectiveInterval
                                    )
                                )
                                viewModel.startPing()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (report != null) "START TEST AGAIN" else "START TEST",
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

                // Segmented Tabs: HTTP / TCP / ICMP
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        val tabs = listOf(
                            PingType.HTTP to "HTTP",
                            PingType.TCP to "TCP",
                            PingType.ICMP to "ICMP"
                        )
                        tabs.forEach { (type, label) ->
                            val isSelected = selectedTab == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedTab = type }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Target Input Field with Leading & Trailing Icons
                OutlinedTextField(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = { Text("Target Host / Domain / IP", style = MaterialTheme.typography.bodySmall) },
                    placeholder = { Text("google.com, 1.1.1.1, 192.168.1.1") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (targetInput.isNotEmpty()) {
                            IconButton(onClick = { targetInput = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Target Presets Row
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf("google.com", "cloudflare.com", "1.1.1.1", "8.8.8.8", "192.168.1.1")
                    items(presets.size) { idx ->
                        val domain = presets[idx]
                        SuggestionChip(
                            onClick = { targetInput = domain },
                            label = { Text(domain, fontSize = 11.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // Port Selector (For TCP and HTTP)
                if (selectedTab != PingType.ICMP) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TARGET PORT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val portPresets = listOf(
                        443 to "443 (HTTPS)",
                        80 to "80 (HTTP)",
                        8080 to "8080 (Proxy)",
                        53 to "53 (DNS)",
                        22 to "22 (SSH)"
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(portPresets.size) { idx ->
                            val (presetPort, presetLabel) = portPresets[idx]
                            val isSelected = !isCustomPort && portInput == presetPort.toString()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    portInput = presetPort.toString()
                                    isCustomPort = false
                                },
                                label = { Text(presetLabel, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isCustomPort,
                                onClick = { isCustomPort = true },
                                label = { Text("Custom Port", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    if (isCustomPort) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("Custom Port (1 - 65535)", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g. 8443, 3000, 21") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Attempts / Requests Chips + Custom Value
                Text(
                    text = if (selectedTab == PingType.HTTP) "REQUESTS (PROBES)" else "ATTEMPTS (PROBES)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val attemptOptions = listOf(5, 10, 20, 50, 100)
                    items(attemptOptions.size) { idx ->
                        val count = attemptOptions[idx]
                        val isSelected = !isCustomCount && attemptCount == count
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                attemptCount = count
                                isCustomCount = false
                            },
                            label = { Text("$count", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = isCustomCount,
                            onClick = { isCustomCount = true },
                            label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                if (isCustomCount) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customCountInput,
                        onValueChange = { customCountInput = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Custom Probe Count (1 - 1000 probes)", style = MaterialTheme.typography.bodySmall) },
                        placeholder = { Text("e.g. 15, 30, 200") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // ICMP Payload Chips + Custom Value (Only for ICMP)
                if (selectedTab == PingType.ICMP) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ICMP PAYLOAD (BYTES)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val payloadOptions = listOf(32, 64, 128, 256, 512, 1024, 1400)
                        items(payloadOptions.size) { idx ->
                            val bytes = payloadOptions[idx]
                            val isSelected = !isCustomPayload && payloadBytes == bytes
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    payloadBytes = bytes
                                    isCustomPayload = false
                                },
                                label = { Text("$bytes B", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = isCustomPayload,
                                onClick = { isCustomPayload = true },
                                label = { Text("Custom", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    if (isCustomPayload) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customPayloadInput,
                            onValueChange = { customPayloadInput = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("Custom Payload (16 - 65507 Bytes)", style = MaterialTheme.typography.bodySmall) },
                            placeholder = { Text("e.g. 56, 1472, 1500") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Expandable Custom Timing & Advanced Profile Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedOptions = !showAdvancedOptions },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Custom Timing & Diagnostic Profiles",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = "Timeout: ${effectiveTimeout}ms • Interval: ${effectiveInterval}ms",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showAdvancedOptions) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = "Toggle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedOptions) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Profiles
                                Text(
                                    text = "PRESET PROFILES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item {
                                        AssistChip(
                                            onClick = {
                                                attemptCount = 5
                                                isCustomCount = false
                                                intervalMs = 200L
                                                isCustomInterval = false
                                                timeoutMs = 1000
                                                isCustomTimeout = false
                                            },
                                            label = { Text("Fast · 5 probes", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = {
                                                attemptCount = 10
                                                isCustomCount = false
                                                intervalMs = 500L
                                                isCustomInterval = false
                                                timeoutMs = 2000
                                                isCustomTimeout = false
                                            },
                                            label = { Text("Standard · 10 probes", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        AssistChip(
                                            onClick = {
                                                attemptCount = 30
                                                isCustomCount = false
                                                intervalMs = 250L
                                                isCustomInterval = false
                                                timeoutMs = 2000
                                                isCustomTimeout = false
                                            },
                                            label = { Text("Deep · 30 probes", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Timeout (ms)
                                Text(
                                    text = "PROBE TIMEOUT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val timeoutOptions = listOf(1000, 2000, 3000, 5000)
                                    items(timeoutOptions.size) { idx ->
                                        val t = timeoutOptions[idx]
                                        val isSelected = !isCustomTimeout && timeoutMs == t
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                timeoutMs = t
                                                isCustomTimeout = false
                                            },
                                            label = { Text("${t}ms", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = isCustomTimeout,
                                            onClick = { isCustomTimeout = true },
                                            label = { Text("Custom", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                if (isCustomTimeout) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = customTimeoutInput,
                                        onValueChange = { customTimeoutInput = it.filter { c -> c.isDigit() }.take(5) },
                                        label = { Text("Custom Timeout (100 - 30000 ms)", fontSize = 11.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Interval (ms)
                                Text(
                                    text = "PROBE INTERVAL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val intervalOptions = listOf(100L, 200L, 500L, 1000L)
                                    items(intervalOptions.size) { idx ->
                                        val iv = intervalOptions[idx]
                                        val isSelected = !isCustomInterval && intervalMs == iv
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                intervalMs = iv
                                                isCustomInterval = false
                                            },
                                            label = { Text("${iv}ms", fontSize = 11.sp) }
                                        )
                                    }
                                    item {
                                        FilterChip(
                                            selected = isCustomInterval,
                                            onClick = { isCustomInterval = true },
                                            label = { Text("Custom", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                if (isCustomInterval) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = customIntervalInput,
                                        onValueChange = { customIntervalInput = it.filter { c -> c.isDigit() }.take(5) },
                                        label = { Text("Custom Interval (20 - 10000 ms)", fontSize = 11.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                                MetricChip(label = "JITTER", value = "${report?.jitterMs?.toInt() ?: 0} ms")
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

    // Fixed-width window: points keep their spacing and slide left as new probes arrive,
    // and the vertical scale eases to a new maximum instead of snapping.
    val window = 30
    val recentProbes = probes.takeLast(window + 1)
    val targetMax = (recentProbes.maxOfOrNull { it.totalTimeMs } ?: 80L).coerceAtLeast(60L).toFloat() * 1.15f
    val animatedMax by androidx.compose.animation.core.animateFloatAsState(
        targetValue = targetMax,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 450),
        label = "pingScale"
    )
    val slide = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(probes.size) {
        slide.snapTo(0f)
        slide.animateTo(1f, androidx.compose.animation.core.tween(durationMillis = 300))
    }

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

                if (recentProbes.size < 2) return@Canvas

                val stepX = w / (window - 1)
                // When the window is full the oldest point scrolls off the left edge during the slide
                val isFull = recentProbes.size > window
                val baseX = if (isFull) -stepX * slide.value else 0f
                val points = recentProbes.mapIndexed { i, p ->
                    val yFraction = (p.totalTimeMs / animatedMax).coerceIn(0.05f, 0.95f)
                    androidx.compose.ui.geometry.Offset(baseX + i * stepX, h - h * yFraction)
                }

                val path = com.netdiag.tool.ui.components.smoothLinePath(points)
                val fillPath = com.netdiag.tool.ui.components.fillUnder(path, points, h)

                clipRect {

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
                drawCircle(
                    color = if (lastProbe.isSuccess) accentColor else statusRedColor,
                    radius = 4.dp.toPx(),
                    center = points.last()
                )
                }
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
