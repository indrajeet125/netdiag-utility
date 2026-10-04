package com.netdiag.tool.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.components.EmptyState
import com.netdiag.tool.ui.theme.*

data class HistoryItem(
    val id: String,
    val time: String,
    val type: String,
    val target: String,
    val network: String,
    val result: String,
    val isSuccess: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Ping", "Cellular")

    // Placeholder sample history items for display
    val sampleItems = remember {
        listOf(
            HistoryItem("1", "21:42", "HTTP Ping", "google.com", "SIM 1 • 5G NSA", "58 ms (0% loss)", true),
            HistoryItem("2", "21:37", "TCP Socket", "1.1.1.1:443", "Wi-Fi 5GHz", "32 ms (0% loss)", true),
            HistoryItem("3", "20:15", "HTTP Ping", "8.8.8.8", "SIM 1 • LTE", "74 ms (10% loss)", true)
        )
    }

    val filteredItems = when (selectedFilter) {
        "Ping" -> sampleItems.filter { it.type.contains("Ping") || it.type.contains("Socket") }
        "Cellular" -> sampleItems.filter { it.network.contains("SIM") }
        else -> sampleItems
    }

    Scaffold(
        containerColor = CanvasNavy,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Diagnostic History",
                        style = MaterialTheme.typography.titleLarge.copy(color = TextPrimary)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasNavy)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyanBg,
                            selectedLabelColor = AccentCyan,
                            containerColor = SurfaceBase,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AccentCyanBorder else BorderSubtle
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredItems.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "No Test History",
                    description = "Run HTTP or TCP socket tests to generate persistent network diagnostic logs."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredItems, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SurfaceBase,
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.type,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.time,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 11.sp,
                                                color = TextTertiary
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${item.target} • ${item.network}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Text(
                                    text = item.result,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isSuccess) StatusGreen else StatusRed,
                                        fontSize = 12.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
