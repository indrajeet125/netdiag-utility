package com.netdiag.tool.ui.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.theme.*
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ── RAM Diagnostics ──
    val memoryInfo = remember {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        mi
    }

    val totalRamBytes = memoryInfo.totalMem
    val availRamBytes = memoryInfo.availMem
    val usedRamBytes = totalRamBytes - availRamBytes
    val ramUsedPercent = if (totalRamBytes > 0) ((usedRamBytes.toDouble() / totalRamBytes) * 100).toInt() else 0

    val totalRamGb = String.format(Locale.US, "%.2f GB", totalRamBytes / (1024.0 * 1024.0 * 1024.0))
    val availRamGb = String.format(Locale.US, "%.2f GB", availRamBytes / (1024.0 * 1024.0 * 1024.0))
    val usedRamGb = String.format(Locale.US, "%.2f GB", usedRamBytes / (1024.0 * 1024.0 * 1024.0))

    // ── Storage Diagnostics ──
    val (totalStorageGb, availStorageGb, usedStorageGb, storageUsedPercent) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val usedPct = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

            Tuple4(
                String.format(Locale.US, "%.1f GB", totalBytes / (1024.0 * 1024.0 * 1024.0)),
                String.format(Locale.US, "%.1f GB", freeBytes / (1024.0 * 1024.0 * 1024.0)),
                String.format(Locale.US, "%.1f GB", usedBytes / (1024.0 * 1024.0 * 1024.0)),
                usedPct
            )
        } catch (_: Exception) {
            Tuple4("--", "--", "--", 0)
        }
    }

    // ── Battery Telemetry ──
    val batteryIntent = remember {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val batteryPct = batteryIntent?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else null
    } ?: 80

    val isCharging = batteryIntent?.let {
        val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } ?: false

    val batteryTempC = batteryIntent?.let {
        val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        temp / 10.0
    } ?: 32.0

    val batteryVoltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 3850
    val batteryTech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

    // ── Display Metrics ──
    val displayMetrics = context.resources.displayMetrics
    val screenRes = "${displayMetrics.widthPixels} × ${displayMetrics.heightPixels} px"
    val screenDpi = "${displayMetrics.densityDpi} DPI"

    // ── OS & Hardware ──
    val cpuCores = Runtime.getRuntime().availableProcessors()
    val abis = Build.SUPPORTED_ABIS.joinToString(", ")
    val radioVersion = try { Build.getRadioVersion() ?: "Unknown" } catch (_: Exception) { "Unknown" }
    val kernelVersion = System.getProperty("os.version") ?: "Linux"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Device & Hardware Telemetry",
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
            // ── Device Model Hero Card ──
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "Hardware: ${Build.HARDWARE} • Board: ${Build.BOARD}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 1: Memory (RAM) & Storage ──
            SectionHeader(title = "SYSTEM MEMORY & STORAGE", icon = Icons.Outlined.Memory)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // RAM Progress Bar
                    UsageProgressRow(
                        title = "RAM Usage",
                        usedText = "$usedRamGb / $totalRamGb",
                        percentage = ramUsedPercent,
                        color = if (ramUsedPercent > 85) StatusRed else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Internal Storage Progress Bar
                    UsageProgressRow(
                        title = "Internal Storage",
                        usedText = "$usedStorageGb / $totalStorageGb",
                        percentage = storageUsedPercent,
                        color = if (storageUsedPercent > 90) StatusAmber else StatusGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 2: Android OS & Firmware ──
            SectionHeader(title = "OPERATING SYSTEM & PLATFORM", icon = Icons.Outlined.Android)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KeyValueRow(label = "Android Version", value = "Android ${Build.VERSION.RELEASE}", isMonospace = false)
                    KeyValueRow(label = "API Level", value = "API ${Build.VERSION.SDK_INT}")
                    KeyValueRow(label = "Security Patch", value = Build.VERSION.SECURITY_PATCH.ifBlank { "N/A" })
                    KeyValueRow(label = "Build Number", value = Build.DISPLAY.take(24))
                    KeyValueRow(label = "Baseband Radio", value = radioVersion.take(24))
                    KeyValueRow(label = "Kernel Version", value = kernelVersion.take(24))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 3: Processor & Display ──
            SectionHeader(title = "PROCESSOR & DISPLAY", icon = Icons.Outlined.DeveloperBoard)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KeyValueRow(label = "CPU Cores", value = "$cpuCores Cores Active")
                    KeyValueRow(label = "Architecture (ABIs)", value = abis)
                    KeyValueRow(label = "Screen Resolution", value = screenRes)
                    KeyValueRow(label = "Screen Density", value = screenDpi)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Section 4: Battery Telemetry ──
            SectionHeader(title = "BATTERY TELEMETRY", icon = Icons.Outlined.BatteryChargingFull)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    KeyValueRow(
                        label = "Battery Level",
                        value = "$batteryPct%",
                        highlightColor = if (batteryPct > 20) StatusGreen else StatusRed
                    )
                    KeyValueRow(label = "Charging Status", value = if (isCharging) "Charging (Active)" else "Discharging")
                    KeyValueRow(label = "Temperature", value = "$batteryTempC °C")
                    KeyValueRow(label = "Voltage", value = "$batteryVoltageMv mV")
                    KeyValueRow(label = "Technology", value = batteryTech)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
fun UsageProgressRow(
    title: String,
    usedText: String,
    percentage: Int,
    color: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = usedText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
