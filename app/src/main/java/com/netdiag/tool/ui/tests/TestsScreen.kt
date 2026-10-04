package com.netdiag.tool.ui.tests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.NetworkState
import com.netdiag.tool.ui.components.*
import com.netdiag.tool.ui.dashboard.DashboardViewModel
import com.netdiag.tool.ui.history.HistoryScreen
import com.netdiag.tool.ui.ping.PingScreen
import com.netdiag.tool.ui.theme.*

enum class TestTab {
    PING,
    CHECKUP,
    HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestsScreen(
    initialTab: TestTab = TestTab.PING,
    onOpenSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Network Tests",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
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
            // Segmented Top Navigation: PING | CHECKUP | HISTORY
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    TestTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedTab) {
                TestTab.PING -> {
                    PingScreen()
                }
                TestTab.CHECKUP -> {
                    CheckupContent()
                }
                TestTab.HISTORY -> {
                    HistoryScreen()
                }
            }
        }
    }
}

@Composable
fun CheckupContent(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val networkState by viewModel.networkState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "GUIDED CONNECTION CHECKUP",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Health Status Hero Card
        val isHealthy = networkState.isConnected && networkState.isValidated
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, if (isHealthy) StatusGreenBorder else StatusAmberBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OVERALL STATUS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isHealthy) StatusGreenBg else StatusAmberBg
                    ) {
                        Text(
                            text = if (isHealthy) "HEALTHY" else "LIMITED CONNECTIVITY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isHealthy) StatusGreen else StatusAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isHealthy) "Internet connection is fully functional and validated."
                    else "Connection check detected issues reaching external gateway or DNS.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CHECKUP DIAGNOSTIC STACK",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        CheckupStepRow(
            title = "Physical Link / Transport",
            subtitle = "Active transport: ${networkState.transport}",
            isPassed = networkState.transport != "NONE"
        )

        Spacer(modifier = Modifier.height(8.dp))

        CheckupStepRow(
            title = "Default Gateway",
            subtitle = networkState.gateway?.let { "Gateway IP: $it" } ?: "No default gateway route detected",
            isPassed = networkState.gateway != null
        )

        Spacer(modifier = Modifier.height(8.dp))

        CheckupStepRow(
            title = "DNS Server Resolution",
            subtitle = if (networkState.dns.isNotEmpty()) "Configured: ${networkState.dns.joinToString(", ")}" else "No DNS servers configured",
            isPassed = networkState.dns.isNotEmpty()
        )

        Spacer(modifier = Modifier.height(8.dp))

        CheckupStepRow(
            title = "Internet Access & Validation",
            subtitle = if (networkState.isValidated) "Network capabilities validated by OS" else "Validation pending",
            isPassed = networkState.isValidated
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Re-run Diagnostics",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CheckupStepRow(
    title: String,
    subtitle: String,
    isPassed: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isPassed) StatusGreenBg else StatusRedBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPassed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isPassed) StatusGreen else StatusRed,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
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
        }
    }
}
