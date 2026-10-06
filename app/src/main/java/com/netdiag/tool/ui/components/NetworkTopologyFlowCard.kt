package com.netdiag.tool.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.domain.model.NetworkState
import com.netdiag.tool.domain.model.SimSubscription
import com.netdiag.tool.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetAddress

/**
 * Interactive Network Topology Flow Diagram.
 * Visualizes the complete route:
 * [ Device IP ] ──> [ Gateway IP ] ──> [ ISP / WAN ] ──> [ Global DNS / Internet ]
 *
 * Includes Play / Pause (Battery Saver Mode) toggle to stop polling and conserve CPU/battery.
 */
@Composable
fun NetworkTopologyFlowCard(
    networkState: NetworkState,
    simSub: SimSubscription?,
    modifier: Modifier = Modifier
) {
    // Battery Saver / Live Monitoring State
    var isMonitoringActive by remember { mutableStateOf(true) }
    var isScreenResumed by remember { mutableStateOf(true) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isScreenResumed = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            isScreenResumed = false
        }
    }

    var selectedNodeIndex by remember { mutableStateOf<Int?>(null) }
    var gatewayLatencyMs by remember { mutableStateOf<Long?>(null) }

    val isConnected = networkState.isConnected
    val isWifi = networkState.transport == "Wi-Fi"
    val gatewayIp = networkState.gateway ?: if (isWifi) "192.168.1.1" else "Radio Core"
    val localIp = networkState.ipv4 ?: if (isWifi) "192.168.1.x" else "10.x.x.x"
    val carrierName = simSub?.info?.carrierName?.takeIf { it.isNotBlank() && it != "Unknown" } ?: if (isWifi) "Broadband ISP" else "Cellular Core"
    val dnsIp = networkState.dns.firstOrNull() ?: "1.1.1.1"

    // Periodic lightweight Gateway ping when monitoring is active AND screen is visible
    LaunchedEffect(isMonitoringActive, isScreenResumed, gatewayIp, isConnected) {
        if (isMonitoringActive && isScreenResumed && isConnected && isWifi && networkState.gateway != null) {
            while (true) {
                try {
                    val start = System.currentTimeMillis()
                    val reachable = withContext(Dispatchers.IO) {
                        try {
                            InetAddress.getByName(networkState.gateway).isReachable(1000)
                        } catch (_: Exception) {
                            false
                        }
                    }
                    if (reachable) {
                        gatewayLatencyMs = (System.currentTimeMillis() - start).coerceAtLeast(1)
                    }
                } catch (_: Exception) { }
                delay(3000) // 3s interval for battery efficiency
            }
        } else {
            gatewayLatencyMs = null
        }
    }

    // Animation transition for data packet pulses (disabled when paused to save battery!)
    val infiniteTransition = rememberInfiniteTransition(label = "PacketPulse")
    val pulseProgress by if (isMonitoringActive && isConnected) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulseProgress"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, if (isConnected) AccentCyanBorder else BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Battery Saver / Play / Stop Control Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NETWORK TOPOLOGY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isMonitoringActive && isConnected) StatusGreen else StatusAmber,
                                    CircleShape
                                )
                        )
                    }
                    Text(
                        text = if (isMonitoringActive) "Live Connection Route" else "Monitoring Paused (Battery Saver)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                // Play / Pause Toggle Button
                OutlinedButton(
                    onClick = { isMonitoringActive = !isMonitoringActive },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isMonitoringActive) StatusAmber.copy(alpha = 0.6f) else StatusGreen.copy(alpha = 0.6f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMonitoringActive) StatusAmberBg else StatusGreenBg
                    )
                ) {
                    Icon(
                        imageVector = if (isMonitoringActive) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isMonitoringActive) "Stop" else "Start",
                        tint = if (isMonitoringActive) StatusAmber else StatusGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isMonitoringActive) "Stop" else "Start Live",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isMonitoringActive) StatusAmber else StatusGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Horizontal 4-Node Flow Diagram ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(vertical = 14.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Node 1: Device
                TopologyNode(
                    icon = Icons.Outlined.Smartphone,
                    label = "Device",
                    subLabel = localIp,
                    isSelected = selectedNodeIndex == 0,
                    onClick = { selectedNodeIndex = if (selectedNodeIndex == 0) null else 0 }
                )

                // Connector 1 (Device -> Gateway)
                TopologyConnector(
                    isActive = isMonitoringActive && isConnected,
                    pulseProgress = pulseProgress,
                    latency = gatewayLatencyMs?.let { "${it}ms" } ?: if (!isWifi && simSub?.servingCell?.timingAdvance != null) "TA: ${simSub.servingCell?.timingAdvance}" else null
                )

                // Node 2: Gateway / Base Station
                TopologyNode(
                    icon = if (isWifi) Icons.Outlined.Router else Icons.Outlined.CellTower,
                    label = if (isWifi) "Gateway" else "Tower",
                    subLabel = if (isWifi) gatewayIp else "Cell",
                    isSelected = selectedNodeIndex == 1,
                    onClick = { selectedNodeIndex = if (selectedNodeIndex == 1) null else 1 }
                )

                // Connector 2 (Gateway -> ISP)
                TopologyConnector(
                    isActive = isMonitoringActive && isConnected,
                    pulseProgress = pulseProgress,
                    latency = "WAN"
                )

                // Node 3: ISP / Carrier
                TopologyNode(
                    icon = Icons.Outlined.Business,
                    label = "ISP",
                    subLabel = carrierName.take(8),
                    isSelected = selectedNodeIndex == 2,
                    onClick = { selectedNodeIndex = if (selectedNodeIndex == 2) null else 2 }
                )

                // Connector 3 (ISP -> Internet)
                TopologyConnector(
                    isActive = isMonitoringActive && isConnected,
                    pulseProgress = pulseProgress,
                    latency = "DNS"
                )

                // Node 4: Global Internet / DNS
                TopologyNode(
                    icon = Icons.Outlined.Public,
                    label = "Internet",
                    subLabel = dnsIp,
                    isSelected = selectedNodeIndex == 3,
                    onClick = { selectedNodeIndex = if (selectedNodeIndex == 3) null else 3 }
                )
            }

            // Node Detail Dropdown Card
            AnimatedVisibility(visible = selectedNodeIndex != null) {
                selectedNodeIndex?.let { index ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceHighlight,
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            when (index) {
                                0 -> {
                                    Text(
                                        text = "DEVICE NODE DETAILS",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "IPv4: $localIp", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace))
                                    networkState.ipv6?.let {
                                        Text(text = "IPv6: $it", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp))
                                    }
                                    Text(text = "Transport: ${networkState.transport}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                }
                                1 -> {
                                    Text(
                                        text = if (isWifi) "ROUTER GATEWAY DETAILS" else "SERVING CELL TOWER DETAILS",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Address: $gatewayIp", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace))
                                    gatewayLatencyMs?.let {
                                        Text(text = "Gateway Ping Response: ${it} ms", style = MaterialTheme.typography.bodySmall.copy(color = StatusGreen, fontWeight = FontWeight.SemiBold))
                                    }
                                    simSub?.servingCell?.timingAdvance?.let {
                                        Text(text = "Timing Advance: $it (~${(it * 78.12).toInt()}m distance)", style = MaterialTheme.typography.bodySmall.copy(color = AccentCyan))
                                    }
                                }
                                2 -> {
                                    Text(
                                        text = "SERVICE PROVIDER (ISP) DETAILS",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Operator / ISP: $carrierName", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
                                    simSub?.info?.plmn?.let {
                                        Text(text = "PLMN: $it", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace))
                                    }
                                    Text(text = "Metered Network: ${if (networkState.isMetered) "Yes" else "No (Unmetered)"}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                                }
                                3 -> {
                                    Text(
                                        text = "GLOBAL INTERNET & RESOLVER",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentCyan, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Primary DNS: $dnsIp", style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary, fontFamily = FontFamily.Monospace))
                                    if (networkState.dns.size > 1) {
                                        Text(text = "Secondary DNS: ${networkState.dns[1]}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontFamily = FontFamily.Monospace))
                                    }
                                    Text(
                                        text = if (isConnected) "Internet Connectivity: Verified Online" else "Disconnected",
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (isConnected) StatusGreen else StatusRed, fontWeight = FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopologyNode(
    icon: ImageVector,
    label: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) AccentCyanBg else SurfaceBase,
            border = BorderStroke(1.5.dp, if (isSelected) AccentCyan else BorderSubtle),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) AccentCyan else TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) AccentCyan else TextPrimary,
                fontSize = 11.sp
            ),
            maxLines = 1
        )

        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun TopologyConnector(
    isActive: Boolean,
    pulseProgress: Float,
    latency: String?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.width(36.dp)
    ) {
        if (latency != null) {
            Text(
                text = latency,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    color = if (isActive) AccentCyan else TextTertiary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Track with animated pulse dot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(BorderSubtle, CircleShape),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = pulseProgress.coerceIn(0.1f, 1f))
                        .height(2.dp)
                        .background(AccentCyan, CircleShape)
                )
            }
        }
    }
}
