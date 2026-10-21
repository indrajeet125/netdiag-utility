package com.netdiag.tool.ui.tests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.data.repository.TestCategory
import com.netdiag.tool.data.repository.TestHistoryEntry
import com.netdiag.tool.data.repository.TestHistoryRepository
import com.netdiag.tool.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class PortStatus {
    OPEN,
    CLOSED,
    TIMEOUT,
    UNREACHABLE
}

data class PortTestResult(
    val port: Int,
    val host: String,
    val status: PortStatus,
    val latencyMs: Long? = null,
    val message: String = ""
)

data class QuickPort(val port: Int, val service: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortTestSection(
    historyRepository: TestHistoryRepository? = null
) {
    var hostInput by remember { mutableStateOf("google.com") }
    var portInput by remember { mutableStateOf("443") }
    var timeoutInput by remember { mutableStateOf("3000") }
    var isRunning by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<PortTestResult?>(null) }

    val scope = rememberCoroutineScope()

    val quickPorts = remember {
        listOf(
            QuickPort(21, "FTP"),
            QuickPort(22, "SSH"),
            QuickPort(53, "DNS"),
            QuickPort(80, "HTTP"),
            QuickPort(443, "HTTPS"),
            QuickPort(8080, "HTTP"),
            QuickPort(8443, "HTTPS")
        )
    }

    fun runTest() {
        val targetHost = hostInput.trim()
        val targetPort = portInput.trim().toIntOrNull() ?: 443
        val timeout = timeoutInput.trim().toIntOrNull() ?: 3000
        if (targetHost.isBlank() || isRunning) return

        isRunning = true
        result = null

        scope.launch {
            val res = withContext(Dispatchers.IO) {
                val start = System.currentTimeMillis()
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(targetHost, targetPort), timeout)
                    val latency = System.currentTimeMillis() - start
                    socket.close()
                    PortTestResult(targetPort, targetHost, PortStatus.OPEN, latency, "Connection established")
                } catch (e: SocketTimeoutException) {
                    PortTestResult(targetPort, targetHost, PortStatus.TIMEOUT, null, "Connection timed out after ${timeout}ms")
                } catch (e: ConnectException) {
                    val latency = System.currentTimeMillis() - start
                    PortTestResult(targetPort, targetHost, PortStatus.CLOSED, latency, "Port refused connection (RST received)")
                } catch (e: UnknownHostException) {
                    PortTestResult(targetPort, targetHost, PortStatus.UNREACHABLE, null, "Hostname could not be resolved via DNS")
                } catch (e: NoRouteToHostException) {
                    PortTestResult(targetPort, targetHost, PortStatus.UNREACHABLE, null, "No IP route to host network")
                } catch (e: Exception) {
                    PortTestResult(targetPort, targetHost, PortStatus.UNREACHABLE, null, e.message ?: "Connection error")
                }
            }

            result = res
            isRunning = false

            // Record to persistent test history
            historyRepository?.addEntry(
                TestHistoryEntry(
                    category = TestCategory.PORT,
                    title = "TCP PORT",
                    target = "$targetHost:$targetPort",
                    networkSummary = "TCP Socket",
                    resultStatus = res.status.name,
                    latencyMs = res.latencyMs,
                    details = res.message,
                    isSuccess = res.status == PortStatus.OPEN
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "TCP PORT TESTER",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Text(
            text = "Inspect single-port TCP reachability to verify firewall and firewall ingress policies.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Host Input
        OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Host / IP Address") },
            placeholder = { Text("e.g. 192.168.1.10 or google.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Port Input
        OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it.filter { ch -> ch.isDigit() } },
            label = { Text("Port (1 - 65535)") },
            placeholder = { Text("443") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Preset Chips
        Text(
            text = "QUICK PRESET PORTS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickPorts) { q ->
                val isSelected = portInput == q.port.toString()
                FilterChip(
                    selected = isSelected,
                    onClick = { portInput = q.port.toString() },
                    label = {
                        Text("${q.port} ${q.service}", style = MaterialTheme.typography.labelSmall)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Button
        Button(
            onClick = { runTest() },
            enabled = !isRunning && hostInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Port...", style = MaterialTheme.typography.labelLarge)
            } else {
                Text("TEST PORT", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Result Card
        result?.let { res ->
            val statusColor = when (res.status) {
                PortStatus.OPEN -> StatusGreen
                PortStatus.CLOSED -> StatusAmber
                PortStatus.TIMEOUT -> StatusAmber
                PortStatus.UNREACHABLE -> StatusRed
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TCP CONNECT RESULT",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Port ${res.port}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = res.status.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = statusColor,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (res.latencyMs != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Latency",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = "${res.latencyMs} ms",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = res.message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}
