package com.netdiag.tool.ui.tests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

enum class WebProtocol {
    HTTP,
    HTTPS,
    FTP
}

data class HttpTimingResult(
    val dnsMs: Long,
    val tcpMs: Long,
    val tlsMs: Long?,
    val ttfbMs: Long,
    val totalMs: Long,
    val statusCode: Int,
    val statusMessage: String,
    val responseSizeBytes: Long,
    val tlsVersion: String? = null,
    val certStatus: String? = null,
    val isSuccess: Boolean = true
)

data class FtpTimingResult(
    val tcpConnectMs: Long,
    val ftpHandshakeMs: Long,
    val serverBanner: String,
    val isSuccess: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebTestSection(
    historyRepository: TestHistoryRepository? = null
) {
    var selectedProtocol by remember { mutableStateOf(WebProtocol.HTTPS) }
    var targetInput by remember { mutableStateOf("https://example.com") }
    var ftpHostInput by remember { mutableStateOf("test.rebex.net") }
    var ftpPortInput by remember { mutableStateOf("21") }
    var timeoutSec by remember { mutableStateOf("5") }
    var httpMethod by remember { mutableStateOf("GET") }

    var isRunning by remember { mutableStateOf(false) }
    var httpResult by remember { mutableStateOf<HttpTimingResult?>(null) }
    var ftpResult by remember { mutableStateOf<FtpTimingResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    fun runHttpTest() {
        val rawTarget = targetInput.trim()
        val timeoutMs = (timeoutSec.toIntOrNull() ?: 5) * 1000
        if (rawTarget.isBlank() || isRunning) return

        isRunning = true
        httpResult = null
        ftpResult = null
        errorMessage = null

        scope.launch {
            try {
                val fullUrlStr = if (!rawTarget.startsWith("http://") && !rawTarget.startsWith("https://")) {
                    if (selectedProtocol == WebProtocol.HTTPS) "https://$rawTarget" else "http://$rawTarget"
                } else rawTarget

                val url = URL(fullUrlStr)
                val host = url.host
                val port = if (url.port != -1) url.port else (if (url.protocol.equals("https", true)) 443 else 80)

                val result = withContext(Dispatchers.IO) {
                    val overallStart = System.currentTimeMillis()

                    // 1. DNS Timing
                    val dnsStart = System.currentTimeMillis()
                    val inetAddr = InetAddress.getByName(host)
                    val dnsMs = System.currentTimeMillis() - dnsStart

                    // 2. TCP Timing
                    val tcpStart = System.currentTimeMillis()
                    val socket = Socket()
                    socket.connect(InetSocketAddress(inetAddr, port), timeoutMs)
                    val tcpMs = System.currentTimeMillis() - tcpStart
                    socket.close()

                    // 3. TLS Timing (if HTTPS)
                    var tlsMs: Long? = null
                    var tlsVersion: String? = null
                    var certStatus: String? = null
                    if (url.protocol.equals("https", true)) {
                        val tlsStart = System.currentTimeMillis()
                        val sslSocket = SSLSocketFactory.getDefault().createSocket(inetAddr, port) as SSLSocket
                        sslSocket.soTimeout = timeoutMs
                        sslSocket.startHandshake()
                        tlsMs = System.currentTimeMillis() - tlsStart
                        tlsVersion = sslSocket.session.protocol
                        certStatus = if (sslSocket.session.isValid) "Valid" else "Invalid"
                        sslSocket.close()
                    }

                    // 4. HTTP Connection & TTFB
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = timeoutMs
                    conn.readTimeout = timeoutMs
                    conn.requestMethod = httpMethod
                    conn.instanceFollowRedirects = true

                    val ttfbStart = System.currentTimeMillis()
                    conn.connect()
                    val statusCode = conn.responseCode
                    val statusMsg = conn.responseMessage ?: "OK"
                    val ttfbMs = System.currentTimeMillis() - ttfbStart

                    val totalMs = System.currentTimeMillis() - overallStart
                    val size = conn.contentLengthLong.takeIf { it >= 0 } ?: conn.inputStream.use { it.readBytes().size.toLong() }
                    conn.disconnect()

                    HttpTimingResult(
                        dnsMs = dnsMs,
                        tcpMs = tcpMs,
                        tlsMs = tlsMs,
                        ttfbMs = ttfbMs,
                        totalMs = totalMs,
                        statusCode = statusCode,
                        statusMessage = statusMsg,
                        responseSizeBytes = size,
                        tlsVersion = tlsVersion,
                        certStatus = certStatus,
                        isSuccess = statusCode in 200..399
                    )
                }

                httpResult = result
                isRunning = false

                // Record into history
                historyRepository?.addEntry(
                    TestHistoryEntry(
                        category = TestCategory.WEB,
                        title = if (selectedProtocol == WebProtocol.HTTPS) "HTTPS" else "HTTP",
                        target = "$host:$port",
                        networkSummary = if (selectedProtocol == WebProtocol.HTTPS) "TLS Encrypted" else "Plain HTTP",
                        resultStatus = "${result.statusCode} ${result.statusMessage}",
                        latencyMs = result.totalMs,
                        details = "DNS ${result.dnsMs}ms • TCP ${result.tcpMs}ms • TTFB ${result.ttfbMs}ms",
                        isSuccess = result.isSuccess
                    )
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to connect"
                isRunning = false
            }
        }
    }

    fun runFtpTest() {
        val host = ftpHostInput.trim()
        val port = ftpPortInput.trim().toIntOrNull() ?: 21
        val timeoutMs = (timeoutSec.toIntOrNull() ?: 5) * 1000
        if (host.isBlank() || isRunning) return

        isRunning = true
        httpResult = null
        ftpResult = null
        errorMessage = null

        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    val socket = Socket()
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    val tcpMs = System.currentTimeMillis() - start

                    val hsStart = System.currentTimeMillis()
                    socket.soTimeout = timeoutMs
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val bannerLine = reader.readLine() ?: "Unknown Server"
                    val hsMs = System.currentTimeMillis() - hsStart

                    // Close connection without authenticating
                    try {
                        socket.getOutputStream().write("QUIT\r\n".toByteArray())
                        socket.getOutputStream().flush()
                    } catch (_: Exception) { }
                    socket.close()

                    FtpTimingResult(
                        tcpConnectMs = tcpMs,
                        ftpHandshakeMs = hsMs,
                        serverBanner = bannerLine,
                        isSuccess = true
                    )
                }

                ftpResult = res
                isRunning = false

                historyRepository?.addEntry(
                    TestHistoryEntry(
                        category = TestCategory.WEB,
                        title = "FTP",
                        target = "$host:$port",
                        networkSummary = "Port $port Handshake",
                        resultStatus = "BANNER RECEIVED",
                        latencyMs = res.tcpConnectMs + res.ftpHandshakeMs,
                        details = "TCP ${res.tcpConnectMs}ms • Handshake ${res.ftpHandshakeMs}ms • ${res.serverBanner.take(40)}",
                        isSuccess = true
                    )
                )
            } catch (e: Exception) {
                errorMessage = e.message ?: "FTP connection failed"
                isRunning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "WEB & APPLICATION PROTOCOL TESTER",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
        )
        Text(
            text = "Measures stepwise connection overhead: DNS resolution, TCP handshake, TLS negotiation, and Time-To-First-Byte (TTFB).",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Protocol Selector
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                WebProtocol.values().forEach { proto ->
                    val isSelected = selectedProtocol == proto
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                selectedProtocol = proto
                                if (proto == WebProtocol.HTTP && targetInput.startsWith("https://")) {
                                    targetInput = targetInput.replace("https://", "http://")
                                } else if (proto == WebProtocol.HTTPS && targetInput.startsWith("http://")) {
                                    targetInput = targetInput.replace("http://", "https://")
                                }
                            }
                    ) {
                        Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = proto.name,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedProtocol != WebProtocol.FTP) {
            // HTTP / HTTPS inputs
            OutlinedTextField(
                value = targetInput,
                onValueChange = { targetInput = it },
                label = { Text("Target URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = httpMethod,
                    onValueChange = { httpMethod = it.uppercase() },
                    label = { Text("Method") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = timeoutSec,
                    onValueChange = { timeoutSec = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Timeout (sec)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        } else {
            // FTP inputs
            OutlinedTextField(
                value = ftpHostInput,
                onValueChange = { ftpHostInput = it },
                label = { Text("FTP Host") },
                placeholder = { Text("192.168.1.20 or ftp.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ftpPortInput,
                    onValueChange = { ftpPortInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = timeoutSec,
                    onValueChange = { timeoutSec = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Timeout (sec)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = {
                if (selectedProtocol == WebProtocol.FTP) runFtpTest() else runHttpTest()
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth().height(50.dp),
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
                Text("Executing Probe...", style = MaterialTheme.typography.labelLarge)
            } else {
                Text(
                    text = if (selectedProtocol == WebProtocol.FTP) "TEST FTP" else "TEST ${selectedProtocol.name}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { err ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StatusRedBg,
                border = BorderStroke(1.dp, StatusRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Probe Failed: $err",
                    color = StatusRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // HTTP Result Breakdown Card
        httpResult?.let { res ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (res.isSuccess) AccentCyanBorder else StatusRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LATENCY BREAKDOWN",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (res.isSuccess) StatusGreenBg else StatusRedBg
                        ) {
                            Text(
                                text = "${res.statusCode} ${res.statusMessage}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (res.isSuccess) StatusGreen else StatusRed,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TimingRow("DNS Resolution", "${res.dnsMs} ms")
                    TimingRow("TCP Connect", "${res.tcpMs} ms")
                    if (res.tlsMs != null) {
                        TimingRow("TLS Handshake", "${res.tlsMs} ms")
                    }
                    TimingRow("TTFB (First Byte)", "${res.ttfbMs} ms")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline)
                    TimingRow("TOTAL DURATION", "${res.totalMs} ms", isBold = true)

                    Spacer(modifier = Modifier.height(12.dp))

                    val sizeKb = String.format("%.2f KB", res.responseSizeBytes / 1024.0)
                    TimingRow("Payload Size", sizeKb)
                    if (res.tlsVersion != null) {
                        TimingRow("TLS Version", res.tlsVersion)
                    }
                    if (res.certStatus != null) {
                        TimingRow("Certificate Status", res.certStatus)
                    }
                }
            }
        }

        // FTP Result Card
        ftpResult?.let { ftp ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, AccentCyanBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "FTP HANDSHAKE RESULT",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    TimingRow("TCP CONNECT", "${ftp.tcpConnectMs} ms")
                    TimingRow("FTP HANDSHAKE", "${ftp.ftpHandshakeMs} ms")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SERVER BANNER:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = ftp.serverBanner,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authentication: Not tested (Anonymous login only after explicit action)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimingRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
