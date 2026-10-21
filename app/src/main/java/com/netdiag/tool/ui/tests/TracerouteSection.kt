package com.netdiag.tool.ui.tests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class TracerouteHop(
    val hopNumber: Int,
    val ip: String,
    val latencyMs: Long?,
    val isDestination: Boolean = false,
    val isTimeout: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerouteSection(
    historyRepository: TestHistoryRepository? = null
) {
    var targetInput by remember { mutableStateOf("1.1.1.1") }
    var maxHopsInput by remember { mutableStateOf("20") }
    var isRunning by remember { mutableStateOf(false) }
    var hops by remember { mutableStateOf<List<TracerouteHop>>(emptyList()) }
    var traceJob by remember { mutableStateOf<Job?>(null) }

    val scope = rememberCoroutineScope()

    fun stopTrace() {
        traceJob?.cancel()
        isRunning = false
    }

    fun startTrace() {
        val target = targetInput.trim()
        val maxHops = maxHopsInput.trim().toIntOrNull() ?: 20
        if (target.isBlank() || isRunning) return

        isRunning = true
        hops = emptyList()

        traceJob = scope.launch {
            try {
                // Resolve destination first
                val destIp = withContext(Dispatchers.IO) {
                    try { InetAddress.getByName(target).hostAddress ?: target } catch (_: Exception) { target }
                }

                val currentHops = mutableListOf<TracerouteHop>()

                for (ttl in 1..maxHops) {
                    if (!isRunning) break

                    val hop = withContext(Dispatchers.IO) {
                        val start = System.currentTimeMillis()
                        try {
                            val process = ProcessBuilder(
                                "ping",
                                "-c", "1",
                                "-t", ttl.toString(),
                                "-W", "1",
                                destIp
                            ).redirectErrorStream(true).start()

                            val output = process.inputStream.bufferedReader().readText()
                            process.waitFor()
                            val duration = System.currentTimeMillis() - start

                            // Check if reached destination or intermediate router
                            val fromMatch = "From ([\\d.]+)".toRegex(RegexOption.IGNORE_CASE).find(output)
                                ?: "from ([\\d.]+)".toRegex().find(output)
                            val ip = fromMatch?.groupValues?.get(1)

                            val timeMatch = "time=([\\d.]+)".toRegex().find(output)
                            val pingTime = timeMatch?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: duration

                            if (ip != null) {
                                TracerouteHop(
                                    hopNumber = ttl,
                                    ip = ip,
                                    latencyMs = pingTime,
                                    isDestination = ip == destIp,
                                    isTimeout = false
                                )
                            } else if (output.contains("1 packets transmitted, 1 received") || output.contains("1 packets transmitted, 1 packets received")) {
                                TracerouteHop(
                                    hopNumber = ttl,
                                    ip = destIp,
                                    latencyMs = pingTime,
                                    isDestination = true,
                                    isTimeout = false
                                )
                            } else {
                                TracerouteHop(
                                    hopNumber = ttl,
                                    ip = "* * *",
                                    latencyMs = null,
                                    isDestination = false,
                                    isTimeout = true
                                )
                            }
                        } catch (e: Exception) {
                            TracerouteHop(
                                hopNumber = ttl,
                                ip = "* * *",
                                latencyMs = null,
                                isDestination = false,
                                isTimeout = true
                            )
                        }
                    }

                    currentHops.add(hop)
                    hops = currentHops.toList()

                    if (hop.isDestination) {
                        break
                    }
                }

                isRunning = false

                historyRepository?.addEntry(
                    TestHistoryEntry(
                        category = TestCategory.CHECKUP,
                        title = "TRACEROUTE",
                        target = target,
                        networkSummary = "${currentHops.size} Hops Traced",
                        resultStatus = if (currentHops.lastOrNull()?.isDestination == true) "DEST REACHED" else "PARTIAL",
                        latencyMs = currentHops.lastOrNull()?.latencyMs,
                        details = "Final Hop: ${currentHops.lastOrNull()?.ip ?: "Unknown"}",
                        isSuccess = currentHops.lastOrNull()?.isDestination == true
                    )
                )
            } catch (_: Exception) {
                isRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "ICMP TTL TRACEROUTE",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Text(
            text = "Discovers network path hops by stepwise incrementing packet TTL headers.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it },
                label = { Text("Target Host / IP") },
                singleLine = true,
                modifier = Modifier.weight(2f),
                shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value = maxHopsInput,
                onValueChange = { maxHopsInput = it.filter { ch -> ch.isDigit() } },
                label = { Text("Max Hops") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isRunning) {
            Button(
                onClick = { stopTrace() },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusRed,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text("STOP TRACEROUTE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        } else {
            Button(
                onClick = { startTrace() },
                enabled = targetInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("START TRACEROUTE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (hops.isNotEmpty()) {
            Text(
                text = "NETWORK PATH HOPS (${hops.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(hops) { hop ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (hop.isDestination) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (hop.isDestination) AccentCyanBorder else MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${hop.hopNumber}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.width(36.dp)
                                )
                                Text(
                                    text = hop.ip,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = if (hop.isTimeout) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (hop.isDestination) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }

                            Text(
                                text = hop.latencyMs?.let { "$it ms" } ?: "*",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = if (hop.latencyMs != null) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
