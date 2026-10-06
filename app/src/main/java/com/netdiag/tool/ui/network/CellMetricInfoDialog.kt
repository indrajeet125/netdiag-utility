package com.netdiag.tool.ui.network

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.theme.*

data class MetricExplanation(
    val acronym: String,
    val fullName: String,
    val category: String,
    val explanation: String,
    val optimalRange: String,
    val whyItMatters: String
)

object CellMetricGlossary {
    val GLOSSARY = mapOf(
        "PCI" to MetricExplanation(
            acronym = "PCI",
            fullName = "Physical Cell ID",
            category = "Cellular Identity",
            explanation = "A physical-layer identifier (0 to 1007 in 5G NR, 0 to 503 in 4G LTE) that uniquely tells your phone which specific antenna sector of a cell tower it is communicating with.",
            optimalRange = "Configured by carrier RF planners so adjacent towers do not share identical PCI numbers.",
            whyItMatters = "Allows you to verify whether your device is handing over to a different tower antenna or remaining on the same sector."
        ),
        "RSRP" to MetricExplanation(
            acronym = "RSRP",
            fullName = "Reference Signal Received Power",
            category = "Signal Strength",
            explanation = "Measures the pure power level of the pilot/reference signal broadcast by the cell tower, expressed in negative dBm.",
            optimalRange = "> -80 dBm: Excellent\n-80 to -95 dBm: Good\n-95 to -105 dBm: Fair\n< -110 dBm: Poor / Call drop risk",
            whyItMatters = "The primary measurement of cellular signal coverage. Lower values cause slower downloads and battery drain."
        ),
        "RSRQ" to MetricExplanation(
            acronym = "RSRQ",
            fullName = "Reference Signal Received Quality",
            category = "Signal Quality",
            explanation = "Measures the quality of the received signal by taking into account co-channel interference and background noise across the channel bandwidth.",
            optimalRange = "> -10 dB: Excellent\n-10 to -15 dB: Good\n-15 to -20 dB: Fair\n< -20 dB: Heavy interference",
            whyItMatters = "Even when RSRP is strong, poor RSRQ means heavy tower congestion or interfering neighbor towers."
        ),
        "SINR" to MetricExplanation(
            acronym = "SINR",
            fullName = "Signal to Interference & Noise Ratio",
            category = "Channel Quality",
            explanation = "The ratio of desired signal power to unwanted background noise and carrier interference. Measured in dB.",
            optimalRange = "> 20 dB: Blazing fast throughput\n13 to 20 dB: Very Good\n5 to 13 dB: Average\n< 0 dB: Noise dominates (slow data)",
            whyItMatters = "Directly determines max achievable download speeds and low-latency gaming/streaming stability."
        ),
        "ARFCN" to MetricExplanation(
            acronym = "ARFCN",
            fullName = "Absolute Radio Frequency Channel Number",
            category = "Radio Frequency",
            explanation = "A standardized carrier number designating the exact transmission frequency of the mobile channel (e.g. 634080 corresponds to ~3.5 GHz 5G Band n78).",
            optimalRange = "Carrier specific frequency plan (e.g. 5G SA n78, 4G B3 1800MHz).",
            whyItMatters = "Verifies the exact RF frequency slice being broadcast by the cellular base station."
        ),
        "BAND" to MetricExplanation(
            acronym = "BAND",
            fullName = "Operating Frequency Band",
            category = "Spectrum Allocation",
            explanation = "The designated radio spectrum band used by the carrier (e.g., n78 for 3500 MHz 5G, n28 for 700 MHz, B3 for 1800 MHz LTE).",
            optimalRange = "Mid-band (n78/n41) = Gigabit speeds\nLow-band (n28/B20) = Deep indoor coverage",
            whyItMatters = "Tells you whether you are connected to high-capacity speed bands or long-range coverage bands."
        ),
        "TAC" to MetricExplanation(
            acronym = "TAC",
            fullName = "Tracking Area Code",
            category = "Core Network",
            explanation = "A geographic cluster of cellular base stations grouped by the mobile core network to track your phone for incoming calls and notifications.",
            optimalRange = "Determined by carrier geographic network zones.",
            whyItMatters = "Helps verify network core routing zones and paging updates across city regions."
        ),
        "GNB" to MetricExplanation(
            acronym = "gNodeB / eNB",
            fullName = "Next-Gen NodeB (5G) / Evolved NodeB (4G)",
            category = "Cell Tower ID",
            explanation = "The unique base station ID of the physical cell tower mast serving your mobile connection.",
            optimalRange = "Carrier assigned base station identifier.",
            whyItMatters = "Allows cellular enthusiasts to map the exact physical tower mast you are connected to."
        ),
        "TA" to MetricExplanation(
            acronym = "TA",
            fullName = "Timing Advance",
            category = "Tower Distance",
            explanation = "The transmission time offset applied by your phone so uplink packets arrive synchronized at the antenna.",
            optimalRange = "0 to 63 steps in LTE (each step is roughly 78 meters). 0-1 indicates tower is very close.",
            whyItMatters = "Provides an accurate physical distance estimate from your phone to the cellular tower."
        ),
        "IMS" to MetricExplanation(
            acronym = "IMS",
            fullName = "IP Multimedia Subsystem",
            category = "Voice & Calling",
            explanation = "The carrier IP core responsible for delivering high-definition voice and video calls over mobile data networks without dropping to 2G/3G.",
            optimalRange = "Must be Registered for 4G/5G voice calls to function properly.",
            whyItMatters = "Essential for modern HD Voice (VoLTE, Vo5G) and Wi-Fi Calling (VoWiFi)."
        ),
        "VONR" to MetricExplanation(
            acronym = "VoNR / Vo5G",
            fullName = "Voice over New Radio (5G Voice)",
            category = "5G Calling",
            explanation = "The native voice architecture for 5G Standalone (SA) networks, running voice traffic directly over 5G NR channels.",
            optimalRange = "Active on compatible 5G Standalone carriers (e.g. Jio True5G).",
            whyItMatters = "Instant call setup (< 1.5s), superior HD audio, and uninterrupted high-speed 5G data during calls."
        ),
        "VOLTE" to MetricExplanation(
            acronym = "VoLTE",
            fullName = "Voice over LTE",
            category = "4G Calling",
            explanation = "Carries packetized voice over 4G LTE data channels instead of legacy 2G/3G circuit-switched networks.",
            optimalRange = "Supported on all modern 4G/5G networks.",
            whyItMatters = "Enables simultaneous phone calls while downloading data at high speeds."
        ),
        "VOWIFI" to MetricExplanation(
            acronym = "VoWiFi",
            fullName = "Voice over Wi-Fi (Wi-Fi Calling)",
            category = "Wi-Fi Calling",
            explanation = "Securely tunnels phone calls and SMS over any Wi-Fi network using an encrypted connection to your mobile carrier.",
            optimalRange = "Active when connected to Wi-Fi with cellular Wi-Fi Calling toggled on.",
            whyItMatters = "Allows crystal clear calling in basements or rural areas with zero cellular reception."
        ),
        "PLMN" to MetricExplanation(
            acronym = "PLMN",
            fullName = "Public Land Mobile Network",
            category = "Operator Code",
            explanation = "The global code identifying your mobile carrier, combining Mobile Country Code (MCC, e.g. 405) and Mobile Network Code (MNC, e.g. 840/854).",
            optimalRange = "Assigned internationally by ITU.",
            whyItMatters = "Verifies your SIM is registered to its authorized home network operator."
        )
    )
}

@Composable
fun InfoIconButton(
    metricKey: String,
    onShowInfo: (MetricExplanation) -> Unit,
    modifier: Modifier = Modifier
) {
    val explanation = CellMetricGlossary.GLOSSARY[metricKey.uppercase()] ?: return

    IconButton(
        onClick = { onShowInfo(explanation) },
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "What is ${explanation.acronym}?",
            tint = AccentCyan.copy(alpha = 0.8f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun CellMetricInfoDialog(
    metric: MetricExplanation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AccentCyan.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ℹ",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = metric.acronym,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = metric.fullName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentCyan,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceBase,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WHAT IS THIS?",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metric.explanation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                lineHeight = 19.sp,
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceBase,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "OPTIMAL / NORMAL RANGE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metric.optimalRange,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceBase,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WHY IT MATTERS TO YOU",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StatusAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metric.whyItMatters,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = CanvasNavy
                )
            ) {
                Text("Got It", fontWeight = FontWeight.Bold)
            }
        }
    )
}
