package com.netdiag.tool.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.domain.model.NetworkState
import com.netdiag.tool.domain.model.SimSubscription
import com.netdiag.tool.ui.theme.*

@Composable
fun NetworkStatusCard(
    networkState: NetworkState,
    simSub: SimSubscription?,
    onDetailsClick: () -> Unit
) {
    val ratLabel = when {
        networkState.transport == "Wi-Fi" -> "Wi-Fi"
        simSub != null && simSub.rat.label != "Unknown" -> simSub.rat.label
        else -> networkState.transport
    }

    val servingCell = simSub?.servingCell
    val isConnected = networkState.isConnected

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, if (isConnected) AccentCyanBorder else BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isConnected) StatusGreen else StatusRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (isConnected) StatusGreen else StatusRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentCyanBg,
                    border = BorderStroke(1.dp, AccentCyanBorder)
                ) {
                    Text(
                        text = ratLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = simSub?.info?.carrierName?.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?: (if (networkState.transport == "Wi-Fi") "Local Wi-Fi Network" else "Cellular Network"),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = buildString {
                    append(if (simSub != null) "SIM ${simSub.info.slotIndex + 1} • " else "")
                    append(networkState.ipv4 ?: "No IP assigned")
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            )

            if (servingCell != null && (servingCell.rsrp != null || servingCell.ssRsrp != null)) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val rsrpVal = servingCell.ssRsrp ?: servingCell.rsrp
                    val rsrqVal = servingCell.ssRsrq ?: servingCell.rsrq
                    val sinrVal = servingCell.ssSinr ?: servingCell.sinr

                    MetricChip(label = "RSRP", value = rsrpVal?.let { "$it dBm" } ?: "--")
                    MetricChip(label = "RSRQ", value = rsrqVal?.let { "$it dB" } ?: "--")
                    MetricChip(label = "SINR", value = sinrVal?.let { "${it.toInt()} dB" } ?: "--")
                }
            }
        }
    }
}

@Composable
fun ConnectionHealthCard(
    networkState: NetworkState,
    onRunCheck: () -> Unit
) {
    val isGood = networkState.isConnected && networkState.isValidated

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONNECTION HEALTH",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.2.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGood) StatusGreenBg else StatusAmberBg
                ) {
                    Text(
                        text = if (isGood) "GOOD" else "CHECK REQUIRED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isGood) StatusGreen else StatusAmber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Diagnostic checks list inspired by Checkup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthItem(label = "Internet", isPassed = networkState.isConnected)
                HealthItem(label = "DNS", isPassed = networkState.dns.isNotEmpty())
                HealthItem(label = "Validated", isPassed = networkState.isValidated)
                HealthItem(label = "Gateway", isPassed = networkState.gateway != null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRunCheck,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceElevated,
                    contentColor = AccentCyan
                )
            ) {
                Text(
                    text = "Run full check",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@Composable
fun HealthItem(label: String, isPassed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(if (isPassed) StatusGreenBg else StatusRedBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isPassed) StatusGreen else StatusRed,
                modifier = Modifier.size(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                color = if (isPassed) TextPrimary else TextSecondary
            )
        )
    }
}

@Composable
fun MetricChip(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                fontSize = 10.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 13.sp
            )
        )
    }
}

@Composable
fun QuickActionGrid(
    onNavigateToPing: (type: String) -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSim: () -> Unit,
    onNavigateToMode: () -> Unit,
    onNavigateToOneTap: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "QUICK TOOLS",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextTertiary,
                letterSpacing = 1.2.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                icon = Icons.Outlined.Speed,
                title = "HTTP Ping",
                subtitle = "App layer",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToPing("http") }
            )
            QuickActionItem(
                icon = Icons.Outlined.SettingsEthernet,
                title = "TCP Socket",
                subtitle = "Port ping",
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToPing("tcp") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                icon = Icons.Outlined.CellTower,
                title = "Cellular",
                subtitle = "Tower RF info",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToNetwork
            )
            QuickActionItem(
                icon = Icons.Outlined.SimCard,
                title = "Dual SIM",
                subtitle = "SIM 1 & 2 state",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSim
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionItem(
                icon = Icons.Outlined.SettingsInputAntenna,
                title = "Network Mode",
                subtitle = "4G / 5G policy",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMode
            )
            QuickActionItem(
                icon = Icons.Outlined.TouchApp,
                title = "OneTap",
                subtitle = "Floating tool",
                modifier = Modifier.weight(1f),
                onClick = onNavigateToOneTap
            )
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(SurfaceElevated, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    isInitiallyExpanded: Boolean = true,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceBase,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.2.sp
                    )
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    isMonospace: Boolean = true,
    highlightColor: Color? = null
) {
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
            ),
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = highlightColor ?: TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1.6f, fill = false)
        )
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(SurfaceElevated, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = CanvasNavy
                )
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
