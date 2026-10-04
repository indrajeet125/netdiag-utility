package com.netdiag.tool.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.ui.components.*
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPing: (type: String) -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToSim: () -> Unit,
    onNavigateToNetworkMode: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOneTap: () -> Unit,
    onOpenDrawer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val networkState by viewModel.networkState.collectAsState()
    val sim1 by viewModel.sim1.collectAsState()
    val sim2 by viewModel.sim2.collectAsState()
    val activeSim = if (sim1.info.isDefaultData) sim1 else if (sim2.info.isDefaultData) sim2 else sim1

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Network Tools",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToOneTap) {
                        Icon(
                            imageVector = Icons.Outlined.TouchApp,
                            contentDescription = "OneTap",
                            tint = AccentCyan
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 1. Current Connection Hero
            Text(
                text = "CURRENT CONNECTION",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = TextTertiary,
                    letterSpacing = 1.2.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            NetworkStatusCard(
                networkState = networkState,
                simSub = activeSim.takeIf { it.info.subscriptionId != -1 },
                onDetailsClick = onNavigateToNetwork
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Connection Health Card (inspired by Checkup)
            ConnectionHealthCard(
                networkState = networkState,
                onRunCheck = { viewModel.refresh() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quick Action Grid (2 columns)
            QuickActionGrid(
                onNavigateToPing = onNavigateToPing,
                onNavigateToNetwork = onNavigateToNetwork,
                onNavigateToSim = onNavigateToSim,
                onNavigateToMode = onNavigateToNetworkMode,
                onNavigateToOneTap = onNavigateToOneTap
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Device & System Snapshot
            ExpandableSection(
                title = "Device System Information",
                isInitiallyExpanded = false
            ) {
                KeyValueRow(label = "Device", value = viewModel.deviceName, isMonospace = false)
                KeyValueRow(label = "Platform", value = viewModel.androidVersion, isMonospace = false)
                KeyValueRow(label = "Transport", value = networkState.transport)
                KeyValueRow(label = "Metered", value = if (networkState.isMetered) "Yes" else "No (Unmetered)")
                if (networkState.gateway != null) {
                    KeyValueRow(label = "Default Gateway", value = networkState.gateway ?: "--")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
