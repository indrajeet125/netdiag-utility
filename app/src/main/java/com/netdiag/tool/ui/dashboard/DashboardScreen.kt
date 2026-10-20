package com.netdiag.tool.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.DashboardCategory
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
    onNavigateToDeviceInfo: () -> Unit = {},
    onNavigateToSimIdentity: () -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val networkState by viewModel.networkState.collectAsState()
    val sim1 by viewModel.sim1.collectAsState()
    val sim2 by viewModel.sim2.collectAsState()
    val activeSim = if (sim1.info.isDefaultData) sim1 else if (sim2.info.isDefaultData) sim2 else sim1
    val categoriesOrder by viewModel.dashboardCategories.collectAsState()
    val favoriteServices by viewModel.favoriteServices.collectAsState()

    var showPrioritizeSheet by remember { mutableStateOf(false) }
    var showSearchRunner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Network Tools",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { showSearchRunner = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search & Run Diagnostics",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            // ── Search & Run Diagnostic Command Bar ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSearchRunner = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Search or Run Services (Speed, Ping, IMSI...)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "RUN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Favorite / Pinned Services Chips Row ──
            val allServices = remember { com.netdiag.tool.domain.model.AppService.ALL_SERVICES }
            val pinnedServices = remember(favoriteServices) {
                allServices.filter { favoriteServices.contains(it.id) }
            }

            if (pinnedServices.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PINNED FAVORITES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Manage / Add",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier
                            .clickable { showSearchRunner = true }
                            .padding(4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pinnedServices.size) { idx ->
                        val service = pinnedServices[idx]
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onNavigateToRoute(service.route) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = StatusAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = service.title.take(16),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.toggleFavoriteService(service.id) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text(
                                        text = "✕",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Header Row: Status label + Prioritise Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OVERVIEW",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.clickable { showPrioritizeSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Prioritise",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Primary action: one obvious first step for every user ──
            Button(
                onClick = { onNavigateToRoute("tests?tab=CHECKUP") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Outlined.NetworkCheck, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Check my connection", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Secondary shortcuts ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HomeShortcut(Icons.Outlined.Speed, "Speed test", Modifier.weight(1f)) { onNavigateToRoute("tests?tab=SPEED") }
                HomeShortcut(Icons.Outlined.NetworkPing, "Ping", Modifier.weight(1f)) { onNavigateToRoute("tests?tab=PING") }
                HomeShortcut(Icons.Outlined.CellTower, "Signal", Modifier.weight(1f), onNavigateToNetwork)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Recent Test Card ──
            val historyList by viewModel.historyList.collectAsState()
            val latestTest = historyList.firstOrNull()

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT TEST",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                        )
                        if (latestTest != null) {
                            Text(
                                text = latestTest.formattedTime,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (latestTest != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${latestTest.title} ${latestTest.target}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${latestTest.latencyMs?.let { "$it ms • " } ?: ""}${latestTest.networkSummary}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (latestTest.isSuccess) StatusGreenBg else StatusRedBg
                            ) {
                                Text(
                                    text = latestTest.resultStatus,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (latestTest.isSuccess) StatusGreen else StatusRed,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "VIEW ALL IN TESTS →",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.clickable { onNavigateToRoute("tests?tab=HISTORY") }
                            )
                        }
                    } else {
                        Text(
                            text = "No tests yet. Tap \"Check my connection\" above to start.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Dynamically Render Categories in User's Prioritized Order
            categoriesOrder.forEach { category ->
                when (category) {
                    DashboardCategory.NETWORK_STATUS -> {
                        Column {
                            NetworkStatusCard(
                                networkState = networkState,
                                simSub = activeSim.takeIf { it.info.subscriptionId != -1 },
                                onDetailsClick = onNavigateToNetwork
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onNavigateToSimIdentity) {
                                    Icon(
                                        imageVector = Icons.Outlined.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "View IMSI & SIM Security",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                    DashboardCategory.TOPOLOGY -> {
                        NetworkTopologyFlowCard(
                            networkState = networkState,
                            simSub = activeSim.takeIf { it.info.subscriptionId != -1 }
                        )
                    }
                    DashboardCategory.HEALTH_CHECKUP -> {
                        ConnectionHealthCard(
                            networkState = networkState,
                            onRunCheck = { onNavigateToRoute("tests?tab=CHECKUP") }
                        )
                    }
                    DashboardCategory.QUICK_ACTIONS -> {
                        QuickActionGrid(
                            onNavigateToPing = onNavigateToPing,
                            onNavigateToNetwork = onNavigateToNetwork,
                            onNavigateToSim = onNavigateToSim,
                            onNavigateToMode = onNavigateToNetworkMode,
                            onNavigateToOneTap = onNavigateToOneTap
                        )
                    }
                    DashboardCategory.DEVICE_INFO -> {
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
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onNavigateToDeviceInfo,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhoneAndroid,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "VIEW FULL HARDWARE & RAM TELEMETRY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Prioritize Categories Drag & Drop Sheet
    if (showPrioritizeSheet) {
        PrioritizeCategoriesSheet(
            currentOrder = categoriesOrder,
            onSaveOrder = { newOrder -> viewModel.updateCategoriesOrder(newOrder) },
            onResetDefault = { viewModel.resetCategoriesOrder() },
            onDismiss = { showPrioritizeSheet = false }
        )
    }

    // Universal Search & Favorite Runner Sheet
    if (showSearchRunner) {
        ServiceSearchRunnerSheet(
            favoriteServiceIds = favoriteServices,
            onToggleFavorite = { viewModel.toggleFavoriteService(it) },
            onRunService = { route ->
                onNavigateToRoute(route)
            },
            onDismiss = { showSearchRunner = false }
        )
    }
}

@Composable
private fun HomeShortcut(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
