package com.netdiag.tool.ui.device

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.netdiag.tool.domain.model.SimSubscription
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.dashboard.DashboardViewModel
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimIdentityScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val sim1 by viewModel.sim1.collectAsState()
    val sim2 by viewModel.sim2.collectAsState()
    var selectedSlot by remember { mutableStateOf(0) }

    val currentSub = if (selectedSlot == 0) sim1 else sim2
    val info = currentSub.info
    val hasSim = info.subscriptionId != -1
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Eye visibility toggle states for sensitive identities
    var showImsi by remember { mutableStateOf(false) }
    var showMsisdn by remember { mutableStateOf(false) }
    var showIccid by remember { mutableStateOf(false) }

    fun copyToClipboard(label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SIM Security & Identity",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── Dual SIM Slot Selector ──
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    listOf(0 to sim1, 1 to sim2).forEach { (slot, sub) ->
                        val isSelected = selectedSlot == slot
                        val carrier = if (sub.info.subscriptionId != -1) sub.info.carrierName.take(12) else "Empty Slot"

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedSlot = slot }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SIM ${slot + 1}: $carrier",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Privacy & Masking Shield Notice Banner ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Security Shield",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "PRIVACY MASKING ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "IMSI and MSISDN are masked to prevent shoulder surfing. Tap the Eye icon to unmask or copy securely.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 1: Sensitive Mobile Subscriber Telemetry ──
            SectionHeader(title = "SUBSCRIBER IDENTITY (IMSI & PHONE)", icon = Icons.Outlined.Badge)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. IMSI (International Mobile Subscriber Identity)
                    val rawImsi = info.imsi ?: if (info.mcc != null && info.mnc != null) "${info.mcc}${info.mnc}000000000" else null
                    val maskedImsi = maskValue(rawImsi, prefixCount = 5, suffixCount = 2)

                    MaskedIdentityRow(
                        label = "IMSI (Subscriber ID)",
                        maskedValue = maskedImsi,
                        rawValue = rawImsi ?: "Not Accessible (Restricted by Carrier)",
                        isVisible = showImsi,
                        onToggleVisibility = { showImsi = !showImsi },
                        onCopy = { rawImsi?.let { copyToClipboard("IMSI", it) } }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. MSISDN (Phone Number)
                    val rawMsisdn = info.msisdn
                    val maskedMsisdn = maskValue(rawMsisdn, prefixCount = 3, suffixCount = 3)

                    MaskedIdentityRow(
                        label = "MSISDN (Phone Number)",
                        maskedValue = maskedMsisdn,
                        rawValue = rawMsisdn ?: "Not Provisioned on SIM",
                        isVisible = showMsisdn,
                        onToggleVisibility = { showMsisdn = !showMsisdn },
                        onCopy = { rawMsisdn?.let { copyToClipboard("Phone Number", it) } }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. ICCID (SIM Serial Number / Chip ID)
                    val rawIccid = info.iccId
                    val maskedIccid = maskValue(rawIccid, prefixCount = 6, suffixCount = 2)

                    MaskedIdentityRow(
                        label = "ICCID (SIM Chip Serial)",
                        maskedValue = maskedIccid,
                        rawValue = rawIccid ?: "Carrier Protected",
                        isVisible = showIccid,
                        onToggleVisibility = { showIccid = !showIccid },
                        onCopy = { rawIccid?.let { copyToClipboard("ICCID", it) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 2: Mobile Network Operator Identifiers ──
            SectionHeader(title = "NETWORK OPERATOR & PLMN", icon = Icons.Outlined.CellTower)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KeyValueRow(label = "Carrier Name", value = info.carrierName, isMonospace = false)
                    KeyValueRow(label = "Country ISO", value = info.countryIso.uppercase())
                    KeyValueRow(label = "MCC (Mobile Country)", value = info.mcc ?: "--")
                    KeyValueRow(label = "MNC (Mobile Network)", value = info.mnc ?: "--")
                    KeyValueRow(label = "PLMN Identifier", value = info.plmn ?: "--")
                    KeyValueRow(label = "Subscription ID", value = "#${info.subscriptionId}")
                    KeyValueRow(label = "Physical Slot Index", value = "Slot ${info.slotIndex}")
                    KeyValueRow(label = "SIM Form Factor", value = if (info.isEmbedded) "eSIM (eUICC)" else "Physical nano-SIM")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Reusable Masked Identity Row with Eye Toggle & Copy action
 */
@Composable
fun MaskedIdentityRow(
    label: String,
    maskedValue: String,
    rawValue: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit
) {
    val displayValue = if (isVisible) rawValue else maskedValue

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (displayValue.startsWith("Not") || displayValue.startsWith("Carrier")) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = if (!isVisible && !displayValue.startsWith("Not")) 1.5.sp else 0.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Eye button to reveal / mask full identity
                IconButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isVisible) "Hide" else "Show Full Value",
                        tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy button
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Safely masks middle digits of sensitive numbers
 */
fun maskValue(value: String?, prefixCount: Int = 4, suffixCount: Int = 2): String {
    if (value.isNullOrBlank()) return "Not Available"
    if (value.length <= (prefixCount + suffixCount)) return "••••••••"
    val prefix = value.take(prefixCount)
    val suffix = value.takeLast(suffixCount)
    return "$prefix •••••••• $suffix"
}
