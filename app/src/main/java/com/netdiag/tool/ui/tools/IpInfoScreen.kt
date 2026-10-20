package com.netdiag.tool.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.system.OsConstants
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.components.KeyValueRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL

// ───────────────────────────────────────────────────────────
// MODELS
// ───────────────────────────────────────────────────────────

data class AddressInfo(val address: String, val prefix: Int, val kind: String) {
    val display: String get() = if (prefix > 0) "$address/$prefix" else address
}

/** One network Android exposes to apps (internet, and on some phones VoLTE/IMS, MMS…). */
data class NetworkConnectionInfo(
    val role: String,
    val isDefault: Boolean,
    val interfaceName: String,
    val addresses: List<AddressInfo>,
    val gateway: String?,
    val dns: List<String>,
    val privateDns: String?,
    val searchDomains: String?,
    val proxy: String?,
    val mtu: Int?,
    val nat64Prefix: String?,
    val assignment: List<String>,
    val isMetered: Boolean,
    val isValidated: Boolean,
    val downKbps: Int,
    val upKbps: Int
)

/** A kernel interface with addresses; catches connections Android hides from apps (e.g. VoLTE/IMS). */
data class InterfaceInfo(val name: String, val role: String, val addresses: List<AddressInfo>, val mtu: Int)

data class PublicIpInfo(
    val ipv4: String? = null,
    val ipv6: String? = null,
    val isp: String? = null,
    val location: String? = null,
    val error: String? = null
)

// ───────────────────────────────────────────────────────────
// SCREEN
// ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpInfoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var refreshKey by remember { mutableStateOf(0) }
    val networks = remember(refreshKey) { collectNetworks(context) }
    val interfaces = remember(refreshKey, networks) { collectInterfaces(networks) }

    var publicInfo by remember { mutableStateOf(PublicIpInfo()) }
    var isFetchingPublic by remember { mutableStateOf(false) }

    fun refreshAll() {
        refreshKey++
        coroutineScope.launch {
            isFetchingPublic = true
            publicInfo = withContext(Dispatchers.IO) { fetchPublicIpInfo() }
            isFetchingPublic = false
        }
    }

    LaunchedEffect(Unit) { refreshAll() }

    val internet = networks.firstOrNull { it.isDefault } ?: networks.firstOrNull()
    val natType = describeNat(internet, publicInfo.ipv4)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IP Information",
                        style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { refreshAll() }, enabled = !isFetchingPublic) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        val text = buildReport(publicInfo, natType, networks, interfaces)
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("IP Info", text))
                        Toast.makeText(context, "Copied IP info to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. Public IP (what websites see) ──
            InfoCard(icon = Icons.Outlined.Public, title = "Public IP", subtitle = "What websites and servers see") {
                if (isFetchingPublic) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Looking up public IP…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(
                        text = publicInfo.ipv4 ?: publicInfo.ipv6 ?: (publicInfo.error ?: "Unavailable"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (publicInfo.ipv4 != null || publicInfo.ipv6 != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    KeyValueRow(label = "Public IPv4", value = publicInfo.ipv4 ?: "None")
                    KeyValueRow(label = "Public IPv6", value = publicInfo.ipv6 ?: "None (no IPv6 internet)")
                    KeyValueRow(label = "Provider (ISP)", value = publicInfo.isp ?: "Unknown", isMonospace = false)
                    KeyValueRow(label = "Approx. location", value = publicInfo.location ?: "Unknown", isMonospace = false)
                    KeyValueRow(label = "NAT type", value = natType, isMonospace = false)
                }
            }

            // ── 2. Every network Android exposes (internet, and VoLTE/IMS when visible) ──
            if (networks.isEmpty()) {
                InfoCard(icon = Icons.Outlined.WifiOff, title = "No active connection", subtitle = "Turn on Wi-Fi or mobile data") {}
            }
            networks.forEach { net ->
                InfoCard(
                    icon = roleIcon(net.role),
                    title = net.role + if (net.isDefault) "  •  in use" else "",
                    subtitle = "Interface ${net.interfaceName}"
                ) {
                    net.addresses.forEach { a ->
                        KeyValueRow(label = a.kind, value = a.display, highlightColor = MaterialTheme.colorScheme.onSurface)
                    }
                    if (net.addresses.isEmpty()) KeyValueRow(label = "Address", value = "None assigned")
                    net.addresses.firstOrNull { it.address.contains('.') && it.prefix in 1..32 }?.let {
                        KeyValueRow(label = "Subnet mask", value = prefixToNetmask(it.prefix))
                    }
                    KeyValueRow(label = "Gateway", value = net.gateway ?: "None", highlightColor = MaterialTheme.colorScheme.primary)
                    net.dns.forEachIndexed { i, dns ->
                        KeyValueRow(label = if (i == 0) "DNS server" else "DNS server ${i + 1}", value = dns)
                    }
                    if (net.dns.isEmpty()) KeyValueRow(label = "DNS server", value = "None reported")
                    net.privateDns?.let { KeyValueRow(label = "Private DNS (DoT)", value = it) }
                    net.searchDomains?.let { KeyValueRow(label = "Search domain", value = it) }
                    net.proxy?.let { KeyValueRow(label = "HTTP proxy", value = it) }
                    net.nat64Prefix?.let { KeyValueRow(label = "NAT64 prefix", value = it) }
                    net.mtu?.let { KeyValueRow(label = "MTU", value = "$it bytes") }
                    KeyValueRow(label = "Metered", value = if (net.isMetered) "Yes (counts toward data plan)" else "No", isMonospace = false)
                    KeyValueRow(label = "Internet verified", value = if (net.isValidated) "Yes" else "No", isMonospace = false)
                    if (net.downKbps > 0) {
                        KeyValueRow(label = "Est. bandwidth", value = "↓ ${formatKbps(net.downKbps)}  ↑ ${formatKbps(net.upKbps)}")
                    }

                    // How the addresses were handed out, in plain language
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("HOW THIS IP WAS ASSIGNED", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    net.assignment.forEach { line ->
                        Text("• $line", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }

            // ── 3. All interfaces (shows VoLTE/IMS and other hidden connections) ──
            InfoCard(icon = Icons.Outlined.Lan, title = "All network interfaces", subtitle = "Includes connections Android hides from apps, like VoLTE") {
                if (interfaces.isEmpty()) {
                    Text("No interfaces with an IP address", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                interfaces.forEachIndexed { i, iface ->
                    if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${iface.name}  —  ${iface.role}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    iface.addresses.forEach { a -> KeyValueRow(label = a.kind, value = a.display) }
                    if (iface.mtu > 0) KeyValueRow(label = "MTU", value = "${iface.mtu} bytes")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "VoLTE uses its own mobile connection (the \"IMS\" APN) with a separate IP. Android only lets system apps confirm it, so a second mobile interface is shown as \"likely VoLTE / IMS\".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

private fun roleIcon(role: String): ImageVector = when {
    role.contains("VoLTE") -> Icons.Outlined.Call
    role.contains("Wi-Fi") -> Icons.Outlined.Wifi
    role.contains("Mobile") || role.contains("MMS") -> Icons.Outlined.CellTower
    role.contains("VPN") -> Icons.Outlined.VpnKey
    else -> Icons.Outlined.Lan
}

// ───────────────────────────────────────────────────────────
// DATA COLLECTION
// ───────────────────────────────────────────────────────────

@Suppress("DEPRECATION") // allNetworks / dhcpInfo: still the only way to list every network and read the DHCP lease
private fun collectNetworks(context: Context): List<NetworkConnectionInfo> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return emptyList()
    val defaultNet = cm.activeNetwork
    return cm.allNetworks.mapNotNull { network ->
        val lp = cm.getLinkProperties(network) ?: return@mapNotNull null
        val caps = cm.getNetworkCapabilities(network) ?: return@mapNotNull null
        val role = networkRole(caps)
        val addresses = lp.linkAddresses.mapNotNull { la ->
            val addr = la.address
            if (addr.isLoopbackAddress) null
            else AddressInfo(cleanHost(addr), la.prefixLength, classifyAddress(addr, la.flags))
        }

        val dhcpServer: String? = if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val fromLp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) lp.dhcpServerAddress?.hostAddress else null
            fromLp ?: (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                ?.dhcpInfo?.serverAddress?.takeIf { it != 0 }?.let { intToIpv4(it) }
        } else null
        val leaseSec = if (dhcpServer != null) {
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.dhcpInfo?.leaseDuration ?: 0
        } else 0
        val nat64 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) lp.nat64Prefix?.toString() else null

        NetworkConnectionInfo(
            role = role,
            isDefault = network == defaultNet,
            interfaceName = lp.interfaceName ?: "unknown",
            addresses = addresses,
            gateway = defaultGateway(lp),
            dns = lp.dnsServers.map { cleanHost(it) },
            privateDns = privateDnsLabel(lp),
            searchDomains = lp.domains?.takeIf { it.isNotBlank() },
            proxy = lp.httpProxy?.let { "${it.host}:${it.port}" },
            mtu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) lp.mtu.takeIf { it > 0 } else null,
            nat64Prefix = nat64,
            assignment = explainAssignment(caps, addresses, dhcpServer, leaseSec, nat64),
            isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            downKbps = caps.linkDownstreamBandwidthKbps,
            upKbps = caps.linkUpstreamBandwidthKbps
        )
    }.sortedByDescending { it.isDefault }
}

private fun networkRole(caps: NetworkCapabilities): String {
    val transport = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Network"
    }
    return when {
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS) -> "VoLTE / IMS"
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS) && !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "MMS"
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "Internet ($transport)"
        else -> transport
    }
}

private fun explainAssignment(
    caps: NetworkCapabilities,
    addresses: List<AddressInfo>,
    dhcpServer: String?,
    leaseSec: Int,
    nat64: String?
): List<String> {
    val hasV4 = addresses.any { it.address.contains('.') }
    val hasGlobalV6 = addresses.any { it.kind.startsWith("IPv6 public") || it.kind.startsWith("IPv6 temporary") }
    val lines = mutableListOf<String>()
    when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
            lines += "Addresses come from the VPN app / VPN server, not from your Wi-Fi or carrier."
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
            if (dhcpServer != null) {
                val lease = if (leaseSec > 0) " for ${formatDuration(leaseSec)}" else ""
                lines += "IPv4 leased by DHCP from $dhcpServer (usually your router)$lease."
            } else if (hasV4) {
                lines += "IPv4 is static (set manually in Wi-Fi settings) or the DHCP server is hidden."
            }
        }
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
            lines += "Given by your carrier when this mobile data connection (APN / PDN session) was set up. Mobile data does not use DHCP."
            if (addresses.any { it.kind.contains("CGNAT") || it.kind.contains("private") }) {
                lines += "The IPv4 is private: the carrier shares one public IP among many users (carrier-grade NAT)."
            }
            if (!hasV4 && hasGlobalV6) {
                lines += "This is an IPv6-only connection; IPv4 apps work through 464XLAT translation" +
                    (nat64?.let { " (NAT64 prefix $it)." } ?: ".")
            }
        }
    }
    if (hasGlobalV6) {
        lines += "IPv6 was self-configured (SLAAC) from the prefix your router / carrier announces. Temporary addresses rotate for privacy."
    }
    if (lines.isEmpty()) lines += "No address assigned yet."
    return lines
}

private fun collectInterfaces(networks: List<NetworkConnectionInfo>): List<InterfaceInfo> {
    val knownRoles = networks.associate { it.interfaceName to it.role }
    val ifaces = try { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() } catch (_: Exception) { emptyList() }
    return ifaces.mapNotNull { ni ->
        if (ni.isLoopback || ni.name.startsWith("dummy")) return@mapNotNull null
        val addrs = ni.interfaceAddresses.mapNotNull { ia ->
            val addr = ia.address ?: return@mapNotNull null
            AddressInfo(cleanHost(addr), ia.networkPrefixLength.toInt(), classifyAddress(addr, 0))
        }
        if (addrs.isEmpty()) return@mapNotNull null
        InterfaceInfo(
            name = ni.name,
            role = knownRoles[ni.name] ?: guessInterfaceRole(ni.name),
            addresses = addrs,
            mtu = try { ni.mtu } catch (_: Exception) { 0 }
        )
    }.sortedBy { it.name }
}

private fun guessInterfaceRole(name: String): String = when {
    name.startsWith("v4-") -> "464XLAT (IPv4 over IPv6)"
    name.startsWith("wlan") -> "Wi-Fi"
    name.startsWith("swlan") || name.startsWith("ap") -> "Hotspot"
    name.startsWith("p2p") -> "Wi-Fi Direct"
    name.startsWith("tun") || name.startsWith("ppp") || name.startsWith("ipsec") -> "VPN"
    name.startsWith("rndis") || name.startsWith("usb") || name.startsWith("ncm") -> "USB tethering"
    name.startsWith("bt-pan") -> "Bluetooth tethering"
    // A mobile interface not used by any visible network is almost always the hidden IMS (VoLTE) PDN
    name.startsWith("rmnet") || name.startsWith("ccmni") || name.startsWith("seth") || name.startsWith("radio") ->
        "Other mobile connection (likely VoLTE / IMS)"
    else -> "Other"
}

private fun classifyAddress(addr: InetAddress, flags: Int): String = when (addr) {
    is Inet4Address -> {
        val b = addr.address.map { it.toInt() and 0xFF }
        when {
            b[0] == 100 && b[1] in 64..127 -> "IPv4 CGNAT (carrier shared)"
            b[0] == 192 && b[1] == 0 && b[2] == 0 -> "IPv4 464XLAT"
            addr.isSiteLocalAddress -> "IPv4 private"
            addr.isLinkLocalAddress -> "IPv4 link-local"
            else -> "IPv4 public"
        }
    }
    is Inet6Address -> when {
        addr.isLinkLocalAddress -> "IPv6 link-local"
        (addr.address[0].toInt() and 0xFE) == 0xFC -> "IPv6 private (ULA)"
        flags and OsConstants.IFA_F_TEMPORARY != 0 -> "IPv6 temporary (privacy)"
        else -> "IPv6 public"
    }
    else -> "Address"
}

private fun defaultGateway(lp: LinkProperties): String? =
    (lp.routes.firstOrNull { it.isDefaultRoute && it.gateway?.isAnyLocalAddress == false }
        ?: lp.routes.firstOrNull { it.gateway != null && it.gateway?.isAnyLocalAddress == false })
        ?.gateway?.let { cleanHost(it) }

private fun privateDnsLabel(lp: LinkProperties): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return when {
        !lp.isPrivateDnsActive -> "Off"
        lp.privateDnsServerName != null -> "On (${lp.privateDnsServerName})"
        else -> "On (automatic)"
    }
}

private fun fetchPublicIpInfo(): PublicIpInfo {
    // api4/api6 force the address family, so both public addresses are reported when present
    val v4 = httpGetLine("https://api4.ipify.org") ?: httpGetLine("https://ipv4.icanhazip.com")
    val v6 = httpGetLine("https://api6.ipify.org")
    var isp: String? = null
    var location: String? = null
    try {
        httpGetBody("https://ipinfo.io/json")?.let { body ->
            val json = JSONObject(body)
            isp = json.optString("org").takeIf { it.isNotBlank() }
            location = listOf(json.optString("city"), json.optString("region"), json.optString("country"))
                .filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) { }
    return PublicIpInfo(
        ipv4 = v4,
        ipv6 = v6,
        isp = isp,
        location = location,
        error = if (v4 == null && v6 == null) "Unable to reach the internet" else null
    )
}

private fun httpGetLine(url: String): String? = httpGetBody(url)?.lineSequence()?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }

private fun httpGetBody(url: String): String? = try {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 4000
    conn.readTimeout = 4000
    try { conn.inputStream.bufferedReader().use { it.readText() } } finally { conn.disconnect() }
} catch (_: Exception) {
    null
}

private fun describeNat(internet: NetworkConnectionInfo?, publicV4: String?): String {
    if (internet == null) return "Unknown (offline)"
    val v4 = internet.addresses.filter { it.address.contains('.') }
    return when {
        publicV4 != null && v4.any { it.address == publicV4 } -> "None: this phone has the public IP directly"
        v4.any { it.kind.contains("CGNAT") } -> "Carrier-grade NAT (shared with other customers)"
        v4.isEmpty() && internet.addresses.isNotEmpty() -> "IPv6-only with 464XLAT / NAT64 for IPv4"
        internet.role.contains("Mobile") && v4.isNotEmpty() -> "Carrier NAT (private mobile IP)"
        v4.isNotEmpty() -> "Router NAT (your router shares one public IP)"
        else -> "Unknown"
    }
}

private fun buildReport(
    pub: PublicIpInfo,
    nat: String,
    networks: List<NetworkConnectionInfo>,
    interfaces: List<InterfaceInfo>
): String = buildString {
    appendLine("=== IP Information ===")
    appendLine("Public IPv4: ${pub.ipv4 ?: "None"}")
    appendLine("Public IPv6: ${pub.ipv6 ?: "None"}")
    appendLine("ISP: ${pub.isp ?: "Unknown"}  Location: ${pub.location ?: "Unknown"}")
    appendLine("NAT: $nat")
    networks.forEach { n ->
        appendLine()
        appendLine("[${n.role}${if (n.isDefault) ", in use" else ""}] ${n.interfaceName}")
        n.addresses.forEach { appendLine("  ${it.kind}: ${it.display}") }
        appendLine("  Gateway: ${n.gateway ?: "None"}")
        appendLine("  DNS: ${n.dns.joinToString(", ").ifEmpty { "None" }}")
        n.privateDns?.let { appendLine("  Private DNS: $it") }
        n.mtu?.let { appendLine("  MTU: $it") }
        n.assignment.forEach { appendLine("  - $it") }
    }
    appendLine()
    appendLine("Interfaces:")
    interfaces.forEach { i -> appendLine("  ${i.name} (${i.role}): ${i.addresses.joinToString { it.display }}") }
}

// ───────────────────────────────────────────────────────────
// FORMATTING HELPERS
// ───────────────────────────────────────────────────────────

/** Host part without the IPv6 scope suffix (fe80::1%wlan0 → fe80::1). */
private fun cleanHost(addr: InetAddress): String = addr.hostAddress?.substringBefore('%') ?: "?"

/** WifiManager.dhcpInfo packs IPv4 little-endian into an Int. */
private fun intToIpv4(ip: Int): String =
    "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"

private fun formatDuration(seconds: Int): String = when {
    seconds >= 86_400 -> "${seconds / 86_400} day(s)"
    seconds >= 3_600 -> "${seconds / 3_600} hour(s)"
    else -> "${seconds / 60} min"
}

private fun formatKbps(kbps: Int): String = if (kbps >= 1000) "${kbps / 1000} Mbps" else "$kbps kbps"

private fun prefixToNetmask(prefix: Int): String {
    if (prefix <= 0 || prefix > 32) return "N/A"
    val mask = (-1 shl (32 - prefix))
    return "${(mask ushr 24) and 0xFF}.${(mask ushr 16) and 0xFF}.${(mask ushr 8) and 0xFF}.${mask and 0xFF}"
}
