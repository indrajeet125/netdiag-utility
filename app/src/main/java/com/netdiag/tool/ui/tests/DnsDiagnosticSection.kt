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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

data class DnsRecordResult(
    val type: String, // A, AAAA, CNAME
    val value: String,
    val timeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsDiagnosticSection(
    historyRepository: TestHistoryRepository? = null
) {
    var hostInput by remember { mutableStateOf("google.com") }
    var isRunning by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf<List<DnsRecordResult>>(emptyList()) }
    var lookupTotalTime by remember { mutableStateOf<Long?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun runDnsLookup() {
        val host = hostInput.trim()
        if (host.isBlank() || isRunning) return

        isRunning = true
        records = emptyList()
        lookupTotalTime = null
        errorMsg = null

        scope.launch {
            try {
                val start = System.currentTimeMillis()
                val list = withContext(Dispatchers.IO) {
                    val addrs = InetAddress.getAllByName(host)
                    val elapsed = System.currentTimeMillis() - start
                    addrs.map { addr ->
                        val type = when (addr) {
                            is Inet4Address -> "A"
                            is Inet6Address -> "AAAA"
                            else -> "IP"
                        }
                        DnsRecordResult(type, addr.hostAddress ?: "", elapsed)
                    }
                }
                val totalMs = System.currentTimeMillis() - start
                records = list
                lookupTotalTime = totalMs
                isRunning = false

                historyRepository?.addEntry(
                    TestHistoryEntry(
                        category = TestCategory.CHECKUP,
                        title = "DNS LOOKUP",
                        target = host,
                        networkSummary = "${list.size} Records Resolved",
                        resultStatus = "RESOLVED",
                        latencyMs = totalMs,
                        details = list.joinToString(", ") { "${it.type}: ${it.value}" },
                        isSuccess = true
                    )
                )
            } catch (e: Exception) {
                errorMsg = e.message ?: "Failed to resolve hostname"
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
            text = "DNS DIAGNOSTICS",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Text(
            text = "Resolves A (IPv4) and AAAA (IPv6) records and measures true name server lookup duration.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Hostname") },
            placeholder = { Text("google.com") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { runDnsLookup() },
            enabled = !isRunning && hostInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resolving...")
            } else {
                Text("RESOLVE DNS", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMsg?.let { err ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusRedBg,
                border = BorderStroke(1.dp, StatusRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DNS Resolution Failed: $err",
                    color = StatusRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (records.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RECORDS (${records.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                lookupTotalTime?.let {
                    Text(
                        text = "Total: $it ms",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(records) { rec ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = rec.type,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = rec.value,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                            Text(
                                text = "${rec.timeMs} ms",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
