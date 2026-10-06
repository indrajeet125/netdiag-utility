package com.netdiag.tool.ui.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.theme.StatusGreen
import kotlin.math.pow

data class SubnetResult(
    val ip: String,
    val prefix: Int,
    val netmask: String,
    val wildcard: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val usableRange: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val ipClass: String,
    val scope: String,
    val binaryIp: String,
    val binaryMask: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubnetCalculatorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var ipInput by remember { mutableStateOf("192.168.1.1") }
    var prefixInput by remember { mutableIntStateOf(24) }

    val result = remember(ipInput, prefixInput) {
        calculateSubnet(ipInput.trim(), prefixInput)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subnet Calculator",
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
                    if (result != null) {
                        IconButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("=== Subnet Calculation ===")
                                    appendLine("IP / Prefix: ${result.ip}/${result.prefix}")
                                    appendLine("Network: ${result.networkAddress}")
                                    appendLine("Broadcast: ${result.broadcastAddress}")
                                    appendLine("Netmask: ${result.netmask}")
                                    appendLine("Wildcard: ${result.wildcard}")
                                    appendLine("Usable Hosts: ${result.usableHosts}")
                                    appendLine("Usable Range: ${result.usableRange}")
                                    appendLine("Class: ${result.ipClass} (${result.scope})")
                                }
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Subnet Info", text))
                                Toast.makeText(context, "Copied calculation to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            // ── Inputs Card ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NETWORK PARAMETERS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ipInput,
                        onValueChange = { ipInput = it },
                        label = { Text("IPv4 Address") },
                        placeholder = { Text("e.g. 192.168.1.1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Prefix Length (CIDR)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "/$prefixInput (${prefixToNetmask(prefixInput)})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Slider(
                        value = prefixInput.toFloat(),
                        onValueChange = { prefixInput = it.toInt() },
                        valueRange = 8f..32f,
                        steps = 23,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(8, 16, 24, 28, 30).forEach { p ->
                            FilterChip(
                                selected = prefixInput == p,
                                onClick = { prefixInput = p },
                                label = { Text("/$p", style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Results ──
            if (result != null) {
                // Key Metrics 2x2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "USABLE HOSTS",
                        value = if (result.usableHosts > 1_000_000) "${result.usableHosts / 1_000_000}M" else result.usableHosts.toString(),
                        subtitle = "Total: ${result.totalHosts}",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "IP CLASS / TYPE",
                        value = result.ipClass,
                        subtitle = result.scope,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Table Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CALCULATED ADDRESS SPACE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        KeyValueRow(label = "Network Address", value = result.networkAddress, highlightColor = MaterialTheme.colorScheme.primary)
                        KeyValueRow(label = "Broadcast Address", value = result.broadcastAddress)
                        KeyValueRow(label = "Subnet Mask", value = result.netmask)
                        KeyValueRow(label = "Wildcard Mask", value = result.wildcard)
                        KeyValueRow(label = "Usable Host Range", value = result.usableRange)
                        KeyValueRow(label = "Usable Host Count", value = result.usableHosts.toString())
                        KeyValueRow(label = "CIDR Notation", value = "${result.ip}/${result.prefix}")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Binary Representation Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "BINARY REPRESENTATION",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "IP Address",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = result.binaryIp,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Subnet Mask",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = result.binaryMask,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invalid IPv4 Address Format",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Please enter four decimal octets between 0 and 255 separated by dots (e.g. 192.168.1.1).",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }
    }
}

private fun prefixToNetmask(prefix: Int): String {
    val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
    return intToIp(mask)
}

private fun intToIp(i: Int): String {
    return "${(i ushr 24) and 0xFF}.${(i ushr 16) and 0xFF}.${(i ushr 8) and 0xFF}.${i and 0xFF}"
}

private fun toBinaryString(i: Int): String {
    val b1 = String.format("%8s", Integer.toBinaryString((i ushr 24) and 0xFF)).replace(' ', '0')
    val b2 = String.format("%8s", Integer.toBinaryString((i ushr 16) and 0xFF)).replace(' ', '0')
    val b3 = String.format("%8s", Integer.toBinaryString((i ushr 8) and 0xFF)).replace(' ', '0')
    val b4 = String.format("%8s", Integer.toBinaryString(i and 0xFF)).replace(' ', '0')
    return "$b1.$b2.$b3.$b4"
}

private fun calculateSubnet(ipStr: String, prefix: Int): SubnetResult? {
    val parts = ipStr.split(".")
    if (parts.size != 4) return null
    val octets = parts.map { it.toIntOrNull() ?: return null }
    if (octets.any { it !in 0..255 }) return null

    val ipInt = (octets[0] shl 24) or (octets[1] shl 16) or (octets[2] shl 8) or octets[3]
    val maskInt = if (prefix == 0) 0 else (-1 shl (32 - prefix))
    val wildcardInt = maskInt.inv()
    val netInt = ipInt and maskInt
    val bcastInt = ipInt or wildcardInt

    val totalHosts = 2.0.pow(32 - prefix).toLong()
    val usableHosts = when {
        prefix == 32 -> 1L
        prefix == 31 -> 2L
        else -> (totalHosts - 2L).coerceAtLeast(0L)
    }

    val usableRange = when {
        prefix == 32 -> intToIp(ipInt)
        prefix == 31 -> "${intToIp(netInt)} - ${intToIp(bcastInt)}"
        usableHosts <= 0 -> "None"
        else -> "${intToIp(netInt + 1)} - ${intToIp(bcastInt - 1)}"
    }

    val firstOctet = octets[0]
    val ipClass = when {
        firstOctet in 1..126 -> "Class A"
        firstOctet == 127 -> "Loopback"
        firstOctet in 128..191 -> "Class B"
        firstOctet in 192..223 -> "Class C"
        firstOctet in 224..239 -> "Class D (Multicast)"
        else -> "Class E (Experimental)"
    }

    val scope = when {
        firstOctet == 10 -> "Private (RFC 1918)"
        firstOctet == 172 && octets[1] in 16..31 -> "Private (RFC 1918)"
        firstOctet == 192 && octets[1] == 168 -> "Private (RFC 1918)"
        firstOctet == 127 -> "Localhost"
        firstOctet == 169 && octets[1] == 254 -> "Link-Local"
        else -> "Public Address"
    }

    return SubnetResult(
        ip = ipStr,
        prefix = prefix,
        netmask = intToIp(maskInt),
        wildcard = intToIp(wildcardInt),
        networkAddress = intToIp(netInt),
        broadcastAddress = intToIp(bcastInt),
        usableRange = usableRange,
        totalHosts = totalHosts,
        usableHosts = usableHosts,
        ipClass = ipClass,
        scope = scope,
        binaryIp = toBinaryString(ipInt),
        binaryMask = toBinaryString(maskInt)
    )
}
