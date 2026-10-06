package com.netdiag.tool.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.theme.StatusGreen
import com.netdiag.tool.ui.theme.StatusGreenBg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.net.URL

data class LocalIpDetails(
    val interfaceName: String = "N/A",
    val ipv4: String = "N/A",
    val ipv4Prefix: Int = 0,
    val ipv6: String = "N/A",
    val gateway: String = "N/A",
    val dnsServers: List<String> = emptyList(),
    val transport: String = "Unknown",
    val mtu: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpInfoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var publicIp by remember { mutableStateOf<String?>(null) }
    var isFetchingPublicIp by remember { mutableStateOf(false) }
    var publicIpError by remember { mutableStateOf<String?>(null) }

    val localDetails = remember { fetchLocalIpDetails(context) }

    fun refreshPublicIp() {
        coroutineScope.launch {
            isFetchingPublicIp = true
            publicIpError = null
            try {
                val ip = withContext(Dispatchers.IO) {
                    val url = URL("https://api.ipify.org")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.requestMethod = "GET"
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val result = reader.readLine()?.trim()
                    reader.close()
                    conn.disconnect()
                    result
                }
                publicIp = ip
            } catch (e: Exception) {
                // Secondary fallback probe
                try {
                    val ip = withContext(Dispatchers.IO) {
                        val url = URL("https://icanhazip.com")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 4000
                        conn.readTimeout = 4000
                        conn.requestMethod = "GET"
                        val reader = BufferedReader(InputStreamReader(conn.inputStream))
                        val result = reader.readLine()?.trim()
                        reader.close()
                        conn.disconnect()
                        result
                    }
                    publicIp = ip
                } catch (e2: Exception) {
                    publicIpError = "Unable to fetch (Offline or timed out)"
                }
            } finally {
                isFetchingPublicIp = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPublicIp()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IP Information",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val text = buildString {
                                appendLine("=== IP Information ===")
                                appendLine("Public IP: ${publicIp ?: "N/A"}")
                                appendLine("Local IPv4: ${localDetails.ipv4}/${localDetails.ipv4Prefix}")
                                appendLine("Local IPv6: ${localDetails.ipv6}")
                                appendLine("Gateway: ${localDetails.gateway}")
                                appendLine("DNS Servers: ${localDetails.dnsServers.joinToString(", ")}")
                                appendLine("Interface: ${localDetails.interfaceName} (${localDetails.transport}, MTU: ${localDetails.mtu})")
                            }
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("IP Info", text))
                            Toast.makeText(context, "Copied IP info to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy",
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Public IP Hero Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
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
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PUBLIC IP ADDRESS",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        letterSpacing = 1.2.sp
                                    )
                                )
                                Text(
                                    text = "External Internet Endpoint",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { refreshPublicIp() },
                            enabled = !isFetchingPublicIp
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isFetchingPublicIp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Resolving public IP...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else if (publicIp != null) {
                        Text(
                            text = publicIp ?: "",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                    } else {
                        Text(
                            text = publicIpError ?: "Unavailable",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Local Network Interface Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "LOCAL NETWORK DETAILS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KeyValueRow(
                        label = "Local IPv4",
                        value = if (localDetails.ipv4 != "N/A" && localDetails.ipv4Prefix > 0) "${localDetails.ipv4}/${localDetails.ipv4Prefix}" else localDetails.ipv4,
                        highlightColor = MaterialTheme.colorScheme.onSurface
                    )

                    KeyValueRow(
                        label = "Subnet Mask",
                        value = prefixToNetmask(localDetails.ipv4Prefix)
                    )

                    KeyValueRow(
                        label = "Default Gateway",
                        value = localDetails.gateway,
                        highlightColor = MaterialTheme.colorScheme.primary
                    )

                    KeyValueRow(
                        label = "Interface Name",
                        value = localDetails.interfaceName
                    )

                    KeyValueRow(
                        label = "Network Type",
                        value = localDetails.transport
                    )

                    KeyValueRow(
                        label = "MTU Size",
                        value = if (localDetails.mtu > 0) "${localDetails.mtu} Bytes" else "Default / N/A"
                    )

                    KeyValueRow(
                        label = "MAC Address",
                        value = "Restricted (Android Privacy API)"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── IPv6 Addressing Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "IPV6 ADDRESS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = localDetails.ipv6,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── DNS Servers Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ACTIVE DNS RESOLVERS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (localDetails.dnsServers.isEmpty()) {
                        Text(
                            text = "No DNS servers reported by active network",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    } else {
                        localDetails.dnsServers.forEachIndexed { index, dns ->
                            KeyValueRow(
                                label = if (index == 0) "Primary DNS" else "Secondary DNS ${index}",
                                value = dns
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

private fun fetchLocalIpDetails(context: Context): LocalIpDetails {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return LocalIpDetails()
    val activeNet = cm.activeNetwork ?: return LocalIpDetails()
    val linkProps = cm.getLinkProperties(activeNet)
    val caps = cm.getNetworkCapabilities(activeNet)

    val ifaceName = linkProps?.interfaceName ?: "Unknown"

    var ipv4 = "N/A"
    var ipv4Prefix = 0
    var ipv6 = "N/A"

    linkProps?.linkAddresses?.forEach { linkAddr ->
        val addr = linkAddr.address
        if (addr is Inet4Address && !addr.isLoopbackAddress) {
            ipv4 = addr.hostAddress ?: "N/A"
            ipv4Prefix = linkAddr.prefixLength
        } else if (addr is Inet6Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
            if (ipv6 == "N/A") {
                ipv6 = addr.hostAddress?.split("%")?.firstOrNull() ?: "N/A"
            }
        }
    }

    // Gateway
    val gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
        ?: linkProps?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress
        ?: "N/A"

    // DNS
    val dnsList = linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()

    // Transport
    val transport = when {
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (802.11)"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Cellular Radio"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
        caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN Tunnel"
        else -> "Active Interface"
    }

    // MTU
    val mtu = try {
        NetworkInterface.getByName(ifaceName)?.mtu ?: 0
    } catch (_: Exception) {
        0
    }

    return LocalIpDetails(
        interfaceName = ifaceName,
        ipv4 = ipv4,
        ipv4Prefix = ipv4Prefix,
        ipv6 = ipv6,
        gateway = gateway,
        dnsServers = dnsList,
        transport = transport,
        mtu = mtu
    )
}

private fun prefixToNetmask(prefix: Int): String {
    if (prefix <= 0 || prefix > 32) return "N/A"
    val mask = (-1 shl (32 - prefix))
    return "${(mask ushr 24) and 0xFF}.${(mask ushr 16) and 0xFF}.${(mask ushr 8) and 0xFF}.${mask and 0xFF}"
}
