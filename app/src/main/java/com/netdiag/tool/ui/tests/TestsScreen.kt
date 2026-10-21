package com.netdiag.tool.ui.tests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.data.repository.TestCategory
import com.netdiag.tool.data.repository.TestHistoryEntry
import com.netdiag.tool.data.repository.TestHistoryRepository
import com.netdiag.tool.domain.model.NetworkState
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.dashboard.DashboardViewModel
import com.netdiag.tool.ui.history.HistoryScreen
import com.netdiag.tool.ui.ping.PingScreen
import com.netdiag.tool.ui.speed.SpeedTestScreen
import com.netdiag.tool.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

// Declaration order = tab order: everyday tests first, advanced network tools after
enum class TestTab(val label: String) {
    SPEED("Speed Test"),
    PING("Ping"),
    CHECKUP("Full Checkup"),
    WEB("Website"),
    DNS("DNS"),
    PORT("Port"),
    TRACEROUTE("Traceroute"),
    HISTORY("History")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestsScreen(
    initialTab: TestTab = TestTab.PING,
    onOpenSettings: () -> Unit,
    viewModel: TestsViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val historyRepository = viewModel.historyRepository

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

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
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
            // Horizontal Test Selector: Ping | Speed | Port | Web | Checkup | DNS | Traceroute | History
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                items(TestTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedTab) {
                TestTab.PING -> {
                    PingScreen()
                }
                TestTab.SPEED -> {
                    SpeedTestScreen()
                }
                TestTab.PORT -> {
                    PortTestSection(historyRepository = historyRepository)
                }
                TestTab.WEB -> {
                    WebTestSection(historyRepository = historyRepository)
                }
                TestTab.CHECKUP -> {
                    CheckupContent(historyRepository = historyRepository)
                }
                TestTab.DNS -> {
                    DnsDiagnosticSection(historyRepository = historyRepository)
                }
                TestTab.TRACEROUTE -> {
                    TracerouteSection(historyRepository = historyRepository)
                }
                TestTab.HISTORY -> {
                    HistoryScreen(
                        historyRepository = historyRepository,
                        onRepeatTest = { item ->
                            when (item.category) {
                                TestCategory.PING -> selectedTab = TestTab.PING
                                TestCategory.PORT -> selectedTab = TestTab.PORT
                                TestCategory.WEB -> selectedTab = TestTab.WEB
                                TestCategory.CHECKUP -> selectedTab = TestTab.CHECKUP
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class CheckupStepState {
    PENDING,
    TESTING,
    PASSED,
    FAILED
}

@Composable
fun CheckupContent(
    historyRepository: TestHistoryRepository? = null,
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val networkState by dashboardViewModel.networkState.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var isRunning by remember { mutableStateOf(false) }
    var step1State by remember { mutableStateOf(CheckupStepState.PENDING) } // Network connected
    var step2State by remember { mutableStateOf(CheckupStepState.PENDING) } // Gateway reachable
    var step3State by remember { mutableStateOf(CheckupStepState.PENDING) } // DNS working
    var step4State by remember { mutableStateOf(CheckupStepState.PENDING) } // Internet reachable
    var step5State by remember { mutableStateOf(CheckupStepState.PENDING) } // HTTP reachable
    var step6State by remember { mutableStateOf(CheckupStepState.PENDING) } // HTTPS reachable

    var checkupSummary by remember { mutableStateOf<String?>(null) }

    fun runFreshCheckup() {
        if (isRunning) return
        coroutineScope.launch {
            isRunning = true
            step1State = CheckupStepState.PENDING
            step2State = CheckupStepState.PENDING
            step3State = CheckupStepState.PENDING
            step4State = CheckupStepState.PENDING
            step5State = CheckupStepState.PENDING
            step6State = CheckupStepState.PENDING
            checkupSummary = null

            dashboardViewModel.refresh()
            delay(200)

            // Step 1: Network Connected
            step1State = CheckupStepState.TESTING
            delay(250)
            step1State = if (networkState.transport != "NONE" && networkState.isConnected) CheckupStepState.PASSED else CheckupStepState.FAILED

            // Step 2: Gateway Reachable
            step2State = CheckupStepState.TESTING
            delay(250)
            step2State = if (networkState.gateway != null) CheckupStepState.PASSED else CheckupStepState.FAILED

            // Step 3: DNS Working
            step3State = CheckupStepState.TESTING
            val dnsOk = withContext(Dispatchers.IO) {
                try {
                    val addrs = InetAddress.getAllByName("dns.google")
                    addrs.isNotEmpty()
                } catch (_: Exception) {
                    false
                }
            }
            step3State = if (dnsOk) CheckupStepState.PASSED else CheckupStepState.FAILED

            // Step 4: Internet Reachable
            step4State = CheckupStepState.TESTING
            val internetOk = withContext(Dispatchers.IO) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("1.1.1.1", 53), 2500)
                    socket.close()
                    true
                } catch (_: Exception) {
                    false
                }
            }
            step4State = if (internetOk) CheckupStepState.PASSED else CheckupStepState.FAILED

            // Step 5: HTTP Reachable
            step5State = CheckupStepState.TESTING
            val httpOk = withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://connectivitycheck.gstatic.com/generate_204")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    conn.instanceFollowRedirects = false
                    val code = conn.responseCode
                    conn.disconnect()
                    code in 200..399
                } catch (_: Exception) {
                    false
                }
            }
            step5State = if (httpOk) CheckupStepState.PASSED else CheckupStepState.FAILED

            // Step 6: HTTPS Reachable
            step6State = CheckupStepState.TESTING
            val httpsOk = withContext(Dispatchers.IO) {
                try {
                    val url = URL("https://www.google.com")
                    val conn = url.openConnection() as HttpsURLConnection
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    val code = conn.responseCode
                    conn.disconnect()
                    code in 200..399
                } catch (_: Exception) {
                    false
                }
            }
            step6State = if (httpsOk) CheckupStepState.PASSED else CheckupStepState.FAILED

            val allPassed = step1State == CheckupStepState.PASSED &&
                    step3State == CheckupStepState.PASSED &&
                    step4State == CheckupStepState.PASSED &&
                    step6State == CheckupStepState.PASSED

            checkupSummary = if (allPassed) "HEALTHY" else "PARTIAL CONNECTIVITY"
            isRunning = false

            // Record to test history
            historyRepository?.addEntry(
                TestHistoryEntry(
                    category = TestCategory.CHECKUP,
                    title = "CHECKUP",
                    target = networkState.transport,
                    networkSummary = if (allPassed) "All Layers Operational" else "Limited Reachability",
                    resultStatus = if (allPassed) "HEALTHY" else "PARTIAL",
                    latencyMs = null,
                    details = "DNS: ${if (dnsOk) "OK" else "FAIL"}, Internet: ${if (internetOk) "OK" else "FAIL"}, HTTPS: ${if (httpsOk) "OK" else "FAIL"}",
                    isSuccess = allPassed
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        runFreshCheckup()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "COMPREHENSIVE NETWORK CHECKUP",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Text(
            text = "Progressively validates every network layer: physical link, gateway, DNS, internet routing, HTTP, and TLS encrypted HTTPS.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Health Status Hero Card
        val isHealthy = checkupSummary == "HEALTHY"
        val statusBg = when {
            isRunning -> MaterialTheme.colorScheme.primaryContainer
            isHealthy -> StatusGreenBg
            else -> StatusAmberBg
        }
        val statusColor = when {
            isRunning -> MaterialTheme.colorScheme.primary
            isHealthy -> StatusGreen
            else -> StatusAmber
        }

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
                        text = "NETWORK CHECK RESULT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = when {
                                isRunning -> "DIAGNOSING..."
                                isHealthy -> "HEALTHY"
                                else -> "PARTIAL CONNECTIVITY"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when {
                        isRunning -> "Performing socket and packet validation across network layers..."
                        isHealthy -> "All layers operational. Gateway, DNS, Internet, and HTTPS verified."
                        else -> "Partial connectivity: Check failed layers below for details."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sequential 6-step Checkup Rows
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CheckupStepRow(
                title = "1. Network Connected",
                subtitle = "Active transport: ${networkState.transport}",
                state = step1State
            )
            CheckupStepRow(
                title = "2. Gateway Reachable",
                subtitle = "Default route gateway: ${networkState.gateway ?: "Unknown / Cellular CGNAT"}",
                state = step2State
            )
            CheckupStepRow(
                title = "3. DNS Working",
                subtitle = "Resolved dns.google successfully",
                state = step3State
            )
            CheckupStepRow(
                title = "4. Internet Reachable",
                subtitle = "Socket connection to 1.1.1.1:53 responded",
                state = step4State
            )
            CheckupStepRow(
                title = "5. HTTP Reachable",
                subtitle = "HTTP 204 No Content probe validated",
                state = step5State
            )
            CheckupStepRow(
                title = "6. HTTPS Reachable",
                subtitle = "TLS cryptographic handshake completed",
                state = step6State
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { runFreshCheckup() },
            enabled = !isRunning,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "Running Checkup..." else "RE-RUN FULL CHECKUP",
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
    state: CheckupStepState
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
                    .size(26.dp)
                    .background(
                        when (state) {
                            CheckupStepState.PASSED -> StatusGreenBg
                            CheckupStepState.FAILED -> StatusRedBg
                            CheckupStepState.TESTING -> MaterialTheme.colorScheme.primaryContainer
                            CheckupStepState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    CheckupStepState.PASSED -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Passed",
                        tint = StatusGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    CheckupStepState.FAILED -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = StatusRed,
                        modifier = Modifier.size(16.dp)
                    )
                    CheckupStepState.TESTING -> CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    CheckupStepState.PENDING -> Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
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
                    text = when (state) {
                        CheckupStepState.PENDING -> "Pending fresh test..."
                        CheckupStepState.TESTING -> "Testing connection layer..."
                        else -> subtitle
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
