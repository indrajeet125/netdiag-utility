package com.netdiag.tool.ui.cache

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.netdiag.tool.domain.model.*
import com.netdiag.tool.ui.components.EmptyState
import com.netdiag.tool.ui.components.KeyValueRow
import com.netdiag.tool.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheCleanerScreen(
    onNavigateBack: () -> Unit,
    viewModel: CacheViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val storageSummary by viewModel.storageSummary.collectAsState()
    val scanSummary by viewModel.scanSummary.collectAsState()
    val appList by viewModel.appList.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val selectedDetailApp by viewModel.selectedDetailApp.collectAsState()

    var showClearConfirmation by remember { mutableStateOf(false) }
    var isClearAllConfirmed by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageAccess()
                viewModel.refreshStorageSpace()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Filter and Sort List
    val filteredList = remember(appList, filterOption, sortOption) {
        var result = when (filterOption) {
            CacheFilterOption.ALL -> appList
            CacheFilterOption.LARGE_CACHE -> appList.filter { it.cacheSize >= 10 * 1024 * 1024 }
            CacheFilterOption.SELECTED -> appList.filter { it.isSelected }
            CacheFilterOption.IGNORED -> appList.filter { it.isIgnored }
        }

        result = when (sortOption) {
            CacheSortOption.CACHE_SIZE -> result.sortedByDescending { it.cacheSize }
            CacheSortOption.APP_NAME -> result.sortedBy { it.appName.lowercase() }
            CacheSortOption.TOTAL_SIZE -> result.sortedByDescending { it.totalSize }
        }
        result
    }

    val selectedApps = appList.filter { it.isSelected && !it.isIgnored }
    val selectedCacheBytes = selectedApps.sumOf { it.cacheSize }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cache Cleaner",
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
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        CacheSortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedApps.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedApps.size} apps selected",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Deselect",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.selectAll(false) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "${CacheViewModel.formatBytes(selectedCacheBytes)} removable cache",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Button(
                            onClick = {
                                isClearAllConfirmed = false
                                showClearConfirmation = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "Clear cache",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Usage Access Warning Card if not granted
            if (!hasUsageAccess) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, StatusAmberBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = StatusAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Usage Access Required",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = StatusAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Android requires Usage Access permission to read exact per-application cache and data sizes.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.openUsageAccessSettings() },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = "Grant Usage Access in Settings",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 1. Compact Storage Summary Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INTERNAL STORAGE",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.2.sp
                                )
                            )
                            Text(
                                text = "${CacheViewModel.formatBytes(storageSummary.freeBytes)} free",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = StatusGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val usedPercent = if (storageSummary.totalBytes > 0) {
                            storageSummary.usedBytes.toFloat() / storageSummary.totalBytes.toFloat()
                        } else 0f

                        LinearProgressIndicator(
                            progress = { usedPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Cache Found",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = CacheViewModel.formatBytes(scanSummary.totalCacheSize),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = "Apps Scanned",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "${scanSummary.totalApps}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = "Largest Cache",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = scanSummary.largestAppCache?.let {
                                        "${it.appName.take(8)} (${CacheViewModel.formatBytes(it.cacheSize)})"
                                    } ?: "--",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scan & Batch Clean Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.scanInstalledApps() },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (scanSummary.isScanning) "Scanning..." else "Scan again",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            isClearAllConfirmed = true
                            showClearConfirmation = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Clean all supported",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CacheFilterOption.values().forEach { filter ->
                        val isSelected = filterOption == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilterOption(filter) },
                            label = {
                                Text(
                                    text = filter.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Select All / Deselect All Control Bar
            if (filteredList.isNotEmpty()) {
                val nonIgnoredFiltered = filteredList.filter { !it.isIgnored }
                val allFilteredSelected = nonIgnoredFiltered.isNotEmpty() && nonIgnoredFiltered.all { it.isSelected }
                val selectedCount = nonIgnoredFiltered.count { it.isSelected }

                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable {
                                        viewModel.selectApps(filteredList.map { it.packageName }, !allFilteredSelected)
                                    }
                            ) {
                                Checkbox(
                                    checked = allFilteredSelected,
                                    onCheckedChange = { isChecked ->
                                        viewModel.selectApps(filteredList.map { it.packageName }, isChecked)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (allFilteredSelected) "Deselect All" else "Select All",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    Text(
                                        text = "$selectedCount of ${filteredList.size} apps selected",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.selectApps(filteredList.map { it.packageName }, true) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Select All",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.selectApps(filteredList.map { it.packageName }, false) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "Deselect All",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // App List
            if (filteredList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.CleaningServices,
                        title = "No Unnecessary Cache Found",
                        description = "Your installed applications currently report little or no removable cache.",
                        actionLabel = "Scan again",
                        onAction = { viewModel.scanInstalledApps() }
                    )
                }
            } else {
                items(filteredList, key = { it.packageName }) { app ->
                    AppCacheItemRow(
                        app = app,
                        onToggleSelect = { viewModel.toggleAppSelection(app.packageName) },
                        onClick = { viewModel.selectDetailApp(app) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // App Detail Bottom Sheet
    selectedDetailApp?.let { app ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectDetailApp(null) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    app.appIcon?.let { icon ->
                        Image(
                            bitmap = icon.toBitmap(110, 110).asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    Column {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        KeyValueRow(
                            label = "Cache Size",
                            value = CacheViewModel.formatBytes(app.cacheSize),
                            highlightColor = MaterialTheme.colorScheme.primary
                        )
                        KeyValueRow(
                            label = "User Data",
                            value = CacheViewModel.formatBytes(app.dataSize),
                            highlightColor = StatusAmber
                        )
                        KeyValueRow(
                            label = "App Code",
                            value = CacheViewModel.formatBytes(app.codeSize)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        KeyValueRow(
                            label = "Total Storage",
                            value = CacheViewModel.formatBytes(app.totalSize),
                            highlightColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Notice: Only temporary cache files are targeted for removal. User data (accounts, local databases, downloads) remains completely intact.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.toggleIgnoreApp(app.packageName)
                            viewModel.selectDetailApp(null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = if (app.isIgnored) "Un-ignore" else "Ignore app",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.openAppSystemSettings(app.packageName)
                            viewModel.selectDetailApp(null)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Clear cache",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Dialog
    if (showClearConfirmation) {
        val targetCount = if (isClearAllConfirmed) appList.count { it.cacheSize > 0 && !it.isIgnored } else selectedApps.size
        val targetBytes = if (isClearAllConfirmed) appList.filter { !it.isIgnored }.sumOf { it.cacheSize } else selectedCacheBytes

        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = {
                Text(
                    text = if (isClearAllConfirmed) "Clear supported app caches?" else "Clear cache for selected apps?",
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Target: $targetCount applications (${CacheViewModel.formatBytes(targetBytes)} estimated cache).",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The cleaner will only perform cache removal operations supported by Android on this device. User data and settings will not be touched.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmation = false
                        // Launch system details for the first app as part of assisted flow
                        val firstApp = if (isClearAllConfirmed) appList.firstOrNull { it.cacheSize > 0 && !it.isIgnored } else selectedApps.firstOrNull()
                        firstApp?.let { viewModel.openAppSystemSettings(it.packageName) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AppCacheItemRow(
    app: AppStorageInfo,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (app.isSelected && !app.isIgnored) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = app.isSelected && !app.isIgnored,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            app.appIcon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.5.sp
                        ),
                        maxLines = 1
                    )
                    if (app.isIgnored) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(Ignored)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary, fontSize = 10.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Cache: ${CacheViewModel.formatBytes(app.cacheSize)} • Data: ${CacheViewModel.formatBytes(app.dataSize)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Text(
                text = CacheViewModel.formatBytes(app.cacheSize),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (app.cacheSize > 10 * 1024 * 1024) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            )
        }
    }
}
