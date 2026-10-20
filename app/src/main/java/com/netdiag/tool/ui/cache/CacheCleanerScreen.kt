package com.netdiag.tool.ui.cache

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.netdiag.tool.ui.components.AppPermission
import com.netdiag.tool.ui.components.EmptyState
import com.netdiag.tool.ui.components.rememberPermissionGate
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
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val hasUsageAccess by viewModel.hasUsageAccess.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val selectedDetailApp by viewModel.selectedDetailApp.collectAsState()
    val isCleaning by viewModel.isCleaning.collectAsState()
    val lastCleanResult by viewModel.lastCleanResult.collectAsState()

    var showClearConfirmation by remember { mutableStateOf(false) }
    var isClearAllConfirmed by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAccessibilityPrompt by remember { mutableStateOf(false) }

    // Scanning / sizing app caches needs Usage access
    val permissionGate = rememberPermissionGate()

    val batchSession by viewModel.batchSession.collectAsState()
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsageAccess()
                viewModel.refreshStorageSpace()
                viewModel.onAppReturnedFromSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Step 1: All Apps -> Step 2: Search -> Step 3: Size / Status Filter -> Step 4: Sort
    val filteredList = remember(appList, searchQuery, filterOption, sortOption, selectedPackages) {
        // Step 1: All Apps
        var result = appList

        // Step 2: Search (by App name or Package name, case-insensitive)
        val q = searchQuery.trim()
        if (q.isNotEmpty()) {
            result = result.filter {
                it.appName.contains(q, ignoreCase = true) || it.packageName.contains(q, ignoreCase = true)
            }
        }

        // Step 3: Size / Status Filter
        result = when (filterOption) {
            CacheFilterOption.ALL -> result
            CacheFilterOption.OVER_10_MB -> result.filter { it.cacheSize >= 10L * 1024 * 1024 }
            CacheFilterOption.OVER_50_MB -> result.filter { it.cacheSize >= 50L * 1024 * 1024 }
            CacheFilterOption.OVER_100_MB -> result.filter { it.cacheSize >= 100L * 1024 * 1024 }
            CacheFilterOption.SELECTED -> result.filter { selectedPackages.contains(it.packageName) }
            CacheFilterOption.IGNORED -> result.filter { it.isIgnored }
        }

        // Step 4: Sort
        result = when (sortOption) {
            CacheSortOption.CACHE_SIZE -> result.sortedByDescending { it.cacheSize }
            CacheSortOption.APP_NAME -> result.sortedBy { it.appName.lowercase() }
            CacheSortOption.TOTAL_SIZE -> result.sortedByDescending { it.totalSize }
        }
        result
    }

    // Persistent Selection across ALL apps (not just currently filtered/searched)
    val selectedApps = remember(appList, selectedPackages) {
        appList.filter { selectedPackages.contains(it.packageName) && !it.isIgnored }
    }
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
                                permissionGate.require(AppPermission.USAGE_ACCESS) {
                                    isClearAllConfirmed = false
                                    showClearConfirmation = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = "Clean Selected",
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

                // Quick Direct Clean & Result Banner
                if (lastCleanResult != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusGreenBg,
                        border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = StatusGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = lastCleanResult ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = StatusGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                            IconButton(onClick = { viewModel.clearResults() }, modifier = Modifier.size(20.dp)) {
                                Text("✕", color = StatusGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.cleanDirectDeviceCache() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isCleaning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CleaningServices,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCleaning) "Cleaning Cache Files..." else "⚡ Clean App & System Cache",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scan & Batch Clean Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            permissionGate.require(AppPermission.USAGE_ACCESS) { viewModel.scanInstalledApps(force = true) }
                        },
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
                            permissionGate.require(AppPermission.USAGE_ACCESS) {
                                isClearAllConfirmed = true
                                showClearConfirmation = true
                            }
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

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    placeholder = {
                        Text(
                            text = "Search installed apps...",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(CacheFilterOption.values()) { filter ->
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

            // Select All / Deselect All Control Bar (Applies to CURRENTLY VISIBLE filtered/search results)
            if (filteredList.isNotEmpty()) {
                val nonIgnoredFiltered = filteredList.filter { !it.isIgnored }
                val allFilteredSelected = nonIgnoredFiltered.isNotEmpty() && nonIgnoredFiltered.all { selectedPackages.contains(it.packageName) }
                val visibleSelectedCount = nonIgnoredFiltered.count { selectedPackages.contains(it.packageName) }

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
                                        text = if (allFilteredSelected) "Deselect Visible" else "Select Visible",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.5.sp
                                        )
                                    )
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) {
                                            "${filteredList.size} results • $visibleSelectedCount selected"
                                        } else {
                                            "$visibleSelectedCount of ${filteredList.size} apps selected"
                                        },
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
                                        text = "Clear Selection",
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
                        icon = if (searchQuery.isNotEmpty()) Icons.Outlined.SearchOff else Icons.Outlined.CleaningServices,
                        title = if (searchQuery.isNotEmpty()) "No matching apps found" else "No Unnecessary Cache Found",
                        description = if (searchQuery.isNotEmpty()) "No applications matched '$searchQuery'. Try a different search term or clear the search." else "Your installed applications currently report little or no removable cache.",
                        actionLabel = if (searchQuery.isNotEmpty()) "Clear search" else "Scan again",
                        onAction = {
                            if (searchQuery.isNotEmpty()) viewModel.setSearchQuery("") else viewModel.scanInstalledApps()
                        }
                    )
                }
            } else {
                items(filteredList, key = { it.packageName }) { app ->
                    val isAppSelected = selectedPackages.contains(app.packageName) && !app.isIgnored
                    AppCacheItemRow(
                        app = app,
                        isSelected = isAppSelected,
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
                        val targets = if (isClearAllConfirmed) {
                            appList.filter { it.cacheSize > 0 && !it.isIgnored }
                        } else {
                            selectedApps
                        }
                        val hasAutomatedTargets = targets.any { it.clearCapability == ClearCapability.ACCESSIBILITY_AUTOMATED }
                        val isA11yEnabled = com.netdiag.tool.ui.onboarding.PermissionChecker.isAccessibilityEnabled(context)
                        if (hasAutomatedTargets && !isA11yEnabled) {
                            showAccessibilityPrompt = true
                        } else {
                            viewModel.startBatchCleanup(targets)
                        }
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

    // ── Accessibility Permission Required Dialog ──
    if (showAccessibilityPrompt) {
        AlertDialog(
            onDismissRequest = { showAccessibilityPrompt = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Accessibility Service Needed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column {
                    Text(
                        text = "Automated cache cleaning requires the 'OneTap Utility' Accessibility Service to navigate Android Settings and click 'Clear cache' on your behalf without manual intervention.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tap 'Enable Service' below, locate 'OneTap Utility' under Downloaded apps/services, and switch it ON.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessibilityPrompt = false
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Enable Service")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAccessibilityPrompt = false
                    // Fallback: Proceed with manual settings flow
                    val targets = if (isClearAllConfirmed) {
                        appList.filter { it.cacheSize > 0 && !it.isIgnored }
                    } else {
                        selectedApps
                    }
                    viewModel.startBatchCleanup(targets)
                }) {
                    Text("Proceed Manually", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Batch Progress Dialog ──
    if (batchSession.isRunning) {
        val currentApp = batchSession.currentApp
        AlertDialog(
            onDismissRequest = { /* Modal during active batch */ },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cleaning (${batchSession.currentIndex + 1} / ${batchSession.totalApps})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column {
                    if (currentApp != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            currentApp.appIcon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap(64, 64).asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Column {
                                Text(
                                    text = currentApp.appName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Target: ${CacheViewModel.formatBytes(batchSession.currentBeforeCacheBytes.takeIf { it > 0 } ?: currentApp.cacheSize)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val progressFraction = if (batchSession.totalApps > 0) {
                        batchSession.currentIndex.toFloat() / batchSession.totalApps.toFloat()
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when (batchSession.step) {
                            BatchCleanupStep.PREPARING -> "Preparing batch queue..."
                            BatchCleanupStep.CLEANING_APP, BatchCleanupStep.WAITING_FOR_RESULT -> "Opening settings & executing cache purge..."
                            BatchCleanupStep.RESULT_RECEIVED -> "Verifying cleared cache bytes..."
                            BatchCleanupStep.NEXT_APP -> "Advancing to next application..."
                            else -> "Processing..."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            },
            confirmButton = {
                OutlinedButton(
                    onClick = { viewModel.cancelBatchCleanup() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Batch")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    // ── Batch Summary Modal ──
    if (batchSession.step == BatchCleanupStep.COMPLETE || batchSession.step == BatchCleanupStep.CANCELLED) {
        val successCount = batchSession.results.count { it.status == CleanupStatus.SUCCESS || it.status == CleanupStatus.ALREADY_CLEAN }
        val partialCount = batchSession.results.count { it.status == CleanupStatus.PARTIAL }
        val unsupportedCount = batchSession.results.count { it.status == CleanupStatus.UNSUPPORTED }
        val failedCount = batchSession.results.count { it.status == CleanupStatus.FAILED || it.status == CleanupStatus.SKIPPED }
        val cancelledCount = batchSession.results.count { it.status == CleanupStatus.CANCELLED }

        AlertDialog(
            onDismissRequest = { viewModel.dismissBatchResults() },
            title = {
                Text(
                    text = if (batchSession.step == BatchCleanupStep.CANCELLED) "Cleanup Interrupted" else "Cleanup Complete",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Released: ${CacheViewModel.formatBytes(batchSession.totalBytesFreed)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Success: $successCount • Partial: $partialCount • Unsupported: $unsupportedCount • Skipped: $failedCount" + if (cancelledCount > 0) " • Cancelled: $cancelledCount" else "",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(batchSession.results) { res ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = when (res.status) {
                                            CleanupStatus.SUCCESS, CleanupStatus.ALREADY_CLEAN -> "✓"
                                            CleanupStatus.PARTIAL -> "⚠"
                                            CleanupStatus.UNSUPPORTED -> "⊘"
                                            CleanupStatus.CANCELLED -> "✕"
                                            else -> "•"
                                        },
                                        color = when (res.status) {
                                            CleanupStatus.SUCCESS, CleanupStatus.ALREADY_CLEAN -> StatusGreen
                                            CleanupStatus.PARTIAL -> StatusAmber
                                            CleanupStatus.CANCELLED, CleanupStatus.FAILED -> StatusRed
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.appName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1
                                    )
                                }

                                Text(
                                    text = if (res.bytesReleased > 0) CacheViewModel.formatBytes(res.bytesReleased) else res.status.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = if (res.bytesReleased > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissBatchResults() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Done")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AppCacheItemRow(
    app: AppStorageInfo,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected && !app.isIgnored) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline),
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
                checked = isSelected && !app.isIgnored,
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
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )

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
