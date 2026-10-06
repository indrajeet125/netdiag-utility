package com.netdiag.tool.ui.cache

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

enum class CacheSortOption(val label: String) {
    CACHE_SIZE("Cache size"),
    APP_NAME("App name"),
    TOTAL_SIZE("Total size")
}

enum class CacheFilterOption(val label: String) {
    ALL("All"),
    OVER_10_MB(">10 MB"),
    OVER_50_MB(">50 MB"),
    OVER_100_MB(">100 MB"),
    SELECTED("Selected"),
    IGNORED("Ignored")
}

@HiltViewModel
class CacheViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _storageSummary = MutableStateFlow(StorageSpaceSummary())
    val storageSummary: StateFlow<StorageSpaceSummary> = _storageSummary.asStateFlow()

    private val _scanSummary = MutableStateFlow(CacheScanSummary())
    val scanSummary: StateFlow<CacheScanSummary> = _scanSummary.asStateFlow()

    private val _appList = MutableStateFlow<List<AppStorageInfo>>(emptyList())
    val appList: StateFlow<List<AppStorageInfo>> = _appList.asStateFlow()

    private val _hasUsageAccess = MutableStateFlow(checkUsageAccess())
    val hasUsageAccess: StateFlow<Boolean> = _hasUsageAccess.asStateFlow()

    private val _sortOption = MutableStateFlow(CacheSortOption.CACHE_SIZE)
    val sortOption: StateFlow<CacheSortOption> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(CacheFilterOption.ALL)
    val filterOption: StateFlow<CacheFilterOption> = _filterOption.asStateFlow()

    private val _selectedDetailApp = MutableStateFlow<AppStorageInfo?>(null)
    val selectedDetailApp: StateFlow<AppStorageInfo?> = _selectedDetailApp.asStateFlow()

    // ── Persistent Selection State (Independent of search/filter) ──
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    // ── Search Query ──
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── Batch Cleanup State Machine ──
    private val _batchSession = MutableStateFlow(BatchCleanupSession())
    val batchSession: StateFlow<BatchCleanupSession> = _batchSession.asStateFlow()

    init {
        refreshStorageSpace()
        scanInstalledApps()
    }

    fun checkUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun refreshUsageAccess() {
        val granted = checkUsageAccess()
        _hasUsageAccess.value = granted
        if (granted && _appList.value.isEmpty()) {
            scanInstalledApps()
        }
    }

    fun refreshStorageSpace() {
        try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            val totalBytes = statFs.totalBytes
            val freeBytes = statFs.availableBytes
            val usedBytes = totalBytes - freeBytes
            _storageSummary.value = StorageSpaceSummary(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes
            )
        } catch (_: Exception) { }
    }

    private var scanJob: kotlinx.coroutines.Job? = null

    fun scanInstalledApps(force: Boolean = false) {
        if (!force && _appList.value.isNotEmpty() && !_scanSummary.value.isScanning) {
            // Re-use cached app scan unless explicitly requested or empty
            return
        }
        if (_scanSummary.value.isScanning && scanJob?.isActive == true) return

        scanJob = viewModelScope.launch {
            android.util.Log.d("SCAN", "[SCAN] app scan START")
            _scanSummary.value = _scanSummary.value.copy(isScanning = true)
            val startTime = System.currentTimeMillis()

            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                val uuid = StorageManager.UUID_DEFAULT
                val hasStats = checkUsageAccess()

                packages.mapNotNull { appInfo ->
                    try {
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        val icon = pm.getApplicationIcon(appInfo)
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                        var cacheSize = 0L
                        var dataSize = 0L
                        var codeSize = 0L

                        if (hasStats && storageStatsManager != null) {
                            try {
                                val stats = storageStatsManager.queryStatsForPackage(uuid, appInfo.packageName, Process.myUserHandle())
                                cacheSize = stats.cacheBytes
                                dataSize = stats.dataBytes
                                codeSize = stats.appBytes
                            } catch (_: Exception) { }
                        }

                        val totalSize = cacheSize + dataSize + codeSize

                        AppStorageInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            versionName = try { pm.getPackageInfo(appInfo.packageName, 0).versionName ?: "1.0" } catch (_: Exception) { "1.0" },
                            appIcon = icon,
                            cacheSize = cacheSize,
                            dataSize = dataSize,
                            codeSize = codeSize,
                            totalSize = totalSize,
                            isSystemApp = isSystem,
                            isSelected = cacheSize > 0,
                            clearCapability = when {
                                cacheSize <= 10240 -> ClearCapability.ALREADY_CLEAN
                                isSystem -> ClearCapability.MANUAL_SETTINGS_FALLBACK
                                else -> ClearCapability.ACCESSIBILITY_AUTOMATED
                            }
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            val totalCache = apps.sumOf { it.cacheSize }
            val appsWithCache = apps.count { it.cacheSize > 0 }
            val largestApp = apps.maxByOrNull { it.cacheSize }
            val duration = System.currentTimeMillis() - startTime

            _appList.value = apps

            // If user hasn't made selections yet, default to apps with cache > 0
            if (_selectedPackages.value.isEmpty()) {
                val initialSelected = apps.filter { it.cacheSize > 0 && !it.isIgnored }.map { it.packageName }.toSet()
                _selectedPackages.value = initialSelected
            } else {
                // Keep only valid installed packages
                val installedPkgs = apps.map { it.packageName }.toSet()
                _selectedPackages.value = _selectedPackages.value.filter { installedPkgs.contains(it) }.toSet()
            }

            android.util.Log.d("SCAN", "[SCAN] app scan COMPLETE (count=${apps.size}, duration=${duration}ms)")
            _scanSummary.value = CacheScanSummary(
                totalApps = apps.size,
                appsWithCache = appsWithCache,
                totalCacheSize = totalCache,
                largestAppCache = largestApp,
                scanDurationMs = duration,
                isScanning = false
            )
        }
    }

    fun queryAppCacheBytes(packageName: String): Long {
        return try {
            if (!checkUsageAccess()) return 0L
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager ?: return 0L
            val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, Process.myUserHandle())
            stats.cacheBytes
        } catch (_: Exception) {
            0L
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current

        // Also sync _appList isSelected for backward compatibility
        _appList.value = _appList.value.map {
            if (it.packageName == packageName) it.copy(isSelected = current.contains(it.packageName)) else it
        }
    }

    fun selectAll(select: Boolean) {
        if (select) {
            val allPkgs = _appList.value.filter { !it.isIgnored }.map { it.packageName }.toSet()
            _selectedPackages.value = allPkgs
            _appList.value = _appList.value.map { it.copy(isSelected = !it.isIgnored) }
        } else {
            _selectedPackages.value = emptySet()
            _appList.value = _appList.value.map { it.copy(isSelected = false) }
        }
    }

    fun selectApps(packageNames: Collection<String>, select: Boolean) {
        val current = _selectedPackages.value.toMutableSet()
        val targetSet = packageNames.toSet()
        if (select) {
            val valid = _appList.value.filter { targetSet.contains(it.packageName) && !it.isIgnored }.map { it.packageName }
            current.addAll(valid)
        } else {
            current.removeAll(targetSet)
        }
        _selectedPackages.value = current

        _appList.value = _appList.value.map {
            if (targetSet.contains(it.packageName)) {
                it.copy(isSelected = current.contains(it.packageName))
            } else {
                it
            }
        }
    }

    fun toggleIgnoreApp(packageName: String) {
        _appList.value = _appList.value.map {
            if (it.packageName == packageName) {
                val newIgnored = !it.isIgnored
                if (newIgnored) {
                    _selectedPackages.value = _selectedPackages.value - packageName
                }
                it.copy(isIgnored = newIgnored, isSelected = if (newIgnored) false else _selectedPackages.value.contains(it.packageName))
            } else it
        }
    }

    fun setSortOption(option: CacheSortOption) {
        _sortOption.value = option
    }

    fun setFilterOption(option: CacheFilterOption) {
        _filterOption.value = option
    }

    fun selectDetailApp(app: AppStorageInfo?) {
        _selectedDetailApp.value = app
    }

    fun openAppSystemSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // ───────────────────────────────────────────────────────────
    // BATCH CLEANUP ENGINE
    // ───────────────────────────────────────────────────────────

    fun startBatchCleanup(targets: List<AppStorageInfo>) {
        val queue = targets.filter { !it.isIgnored }
        if (queue.isEmpty()) return

        Log.d("CACHE", "[CACHE][START] selectedCount=${queue.size}")
        _batchSession.value = BatchCleanupSession(
            step = BatchCleanupStep.PREPARING,
            queue = queue,
            currentIndex = 0,
            results = emptyList(),
            totalBytesFreed = 0L,
            isCancelled = false
        )

        processCurrentQueueItem()
    }

    private fun processCurrentQueueItem() {
        val session = _batchSession.value
        if (session.isCancelled) return

        if (session.currentIndex >= session.queue.size) {
            finalizeBatch()
            return
        }

        val app = session.queue[session.currentIndex]
        Log.d("CACHE", "[CACHE][APP_START] package=${app.packageName} index=${session.currentIndex} total=${session.queue.size}")

        when (app.clearCapability) {
            ClearCapability.ALREADY_CLEAN -> {
                Log.d("CACHE", "[CACHE][BEFORE] package=${app.packageName} cacheBytes=${app.cacheSize}")
                Log.d("CACHE", "[CACHE][REQUEST] package=${app.packageName} method=ALREADY_CLEAN")
                val result = CacheCleanupResult(
                    packageName = app.packageName,
                    appName = app.appName,
                    status = CleanupStatus.ALREADY_CLEAN,
                    bytesReleased = 0L,
                    detailMessage = "Cache already clean"
                )
                Log.d("CACHE", "[CACHE][SYSTEM_RESULT] package=${app.packageName} result=ALREADY_CLEAN")
                Log.d("CACHE", "[CACHE][SUCCESS] package=${app.packageName}")
                advanceQueue(result)
            }
            ClearCapability.SYSTEM_RESTRICTED -> {
                Log.d("CACHE", "[CACHE][BEFORE] package=${app.packageName} cacheBytes=${app.cacheSize}")
                Log.d("CACHE", "[CACHE][REQUEST] package=${app.packageName} method=SYSTEM_RESTRICTED")
                val result = CacheCleanupResult(
                    packageName = app.packageName,
                    appName = app.appName,
                    status = CleanupStatus.UNSUPPORTED,
                    bytesReleased = 0L,
                    detailMessage = "System restricted application"
                )
                Log.d("CACHE", "[CACHE][FAILED] package=${app.packageName} reason=SYSTEM_RESTRICTED")
                advanceQueue(result)
            }
            ClearCapability.ACCESSIBILITY_AUTOMATED, ClearCapability.MANUAL_SETTINGS_FALLBACK -> {
                val beforeBytes = queryAppCacheBytes(app.packageName).takeIf { it > 0 } ?: app.cacheSize
                Log.d("CACHE", "[CACHE][BEFORE] package=${app.packageName} cacheBytes=$beforeBytes")
                if (beforeBytes <= 10240L) {
                    Log.d("CACHE", "[CACHE][REQUEST] package=${app.packageName} method=ALREADY_CLEAN_CHECK")
                    val result = CacheCleanupResult(
                        packageName = app.packageName,
                        appName = app.appName,
                        status = CleanupStatus.ALREADY_CLEAN,
                        bytesReleased = 0L,
                        detailMessage = "Cache already clean (<=10KB)"
                    )
                    Log.d("CACHE", "[CACHE][SUCCESS] package=${app.packageName}")
                    advanceQueue(result)
                } else {
                    _batchSession.value = session.copy(
                        step = BatchCleanupStep.WAITING_FOR_RESULT,
                        currentPackageName = app.packageName,
                        currentBeforeCacheBytes = beforeBytes
                    )
                    Log.d("CACHE", "[CACHE][REQUEST] package=${app.packageName} method=SETTINGS_AUTOMATION")
                    com.netdiag.tool.service.OneTapAccessibilityService.beginCleaning(app.packageName)
                    openAppSystemSettings(app.packageName)
                }
            }
        }
    }

    /**
     * Called on Activity ON_RESUME when user/accessibility returns from Settings.
     * Implements human-like wait (1000-1500ms), cache verification, retry if unchanged, and state fencing.
     */
    fun onAppReturnedFromSettings() {
        val session = _batchSession.value
        if (!session.isRunning || session.step != BatchCleanupStep.WAITING_FOR_RESULT) {
            return
        }
        val targetPkg = session.currentPackageName ?: return
        val targetIndex = session.currentIndex
        val app = session.queue.getOrNull(targetIndex) ?: return
        if (app.packageName != targetPkg) return

        Log.d("CACHE", "[CACHE][SYSTEM_RESULT] package=$targetPkg result=RETURNED_FROM_SETTINGS")

        // Immediately update step to prevent duplicate execution
        _batchSession.value = session.copy(step = BatchCleanupStep.RESULT_RECEIVED)

        viewModelScope.launch {
            // Requirement: On resume, wait 1000-1500ms before reading cache stats
            delay(1200)

            var afterBytes = queryAppCacheBytes(targetPkg)
            val beforeBytes = session.currentBeforeCacheBytes
            var freed = (beforeBytes - afterBytes).coerceAtLeast(0L)

            // If unchanged, retry verification after 1000ms delay to account for async Android storage daemon updates
            if (freed == 0L && afterBytes > 20480L) {
                Log.d("CACHE", "[CACHE][VERIFY] package=$targetPkg retry=WAIT_1000MS_FOR_STORAGE_DAEMON")
                delay(1000)
                afterBytes = queryAppCacheBytes(targetPkg)
                freed = (beforeBytes - afterBytes).coerceAtLeast(0L)
            }

            val status = when {
                freed > 0 && (afterBytes <= 20480L || freed >= (beforeBytes * 0.7).toLong()) -> CleanupStatus.SUCCESS
                freed > 0 -> CleanupStatus.PARTIAL
                afterBytes <= 20480L -> CleanupStatus.ALREADY_CLEAN
                else -> CleanupStatus.SKIPPED
            }

            Log.d("CACHE", "[CACHE][VERIFY] package=$targetPkg before=$beforeBytes after=$afterBytes freed=$freed status=$status")
            if (status == CleanupStatus.SUCCESS || status == CleanupStatus.ALREADY_CLEAN) {
                Log.d("CACHE", "[CACHE][SUCCESS] package=$targetPkg")
            } else {
                Log.d("CACHE", "[CACHE][FAILED] package=$targetPkg reason=${status.name}")
            }

            val result = CacheCleanupResult(
                packageName = app.packageName,
                appName = app.appName,
                status = status,
                bytesReleased = freed,
                detailMessage = if (freed > 0) "Freed ${formatBytes(freed)}" else "No change detected"
            )

            advanceQueue(result)
        }
    }

    private fun advanceQueue(result: CacheCleanupResult) {
        val session = _batchSession.value
        val updatedResults = session.results + result
        val totalFreed = session.totalBytesFreed + result.bytesReleased
        val nextIndex = session.currentIndex + 1

        if (nextIndex < session.queue.size && !session.isCancelled) {
            Log.d("CACHE", "[CACHE][NEXT] nextIndex=$nextIndex nextPackage=${session.queue[nextIndex].packageName}")
            _batchSession.value = session.copy(
                step = BatchCleanupStep.NEXT_APP,
                currentIndex = nextIndex,
                currentPackageName = null,
                currentBeforeCacheBytes = 0L,
                results = updatedResults,
                totalBytesFreed = totalFreed
            )

            viewModelScope.launch {
                // Requirement 10: Wait 1000-1500ms before starting next app
                delay(1200)
                processCurrentQueueItem()
            }
        } else {
            finalizeBatch(updatedResults, totalFreed)
        }
    }

    private fun finalizeBatch(
        finalResults: List<CacheCleanupResult>? = null,
        finalFreed: Long? = null
    ) {
        val session = _batchSession.value
        val res = finalResults ?: session.results
        val freed = finalFreed ?: session.totalBytesFreed

        val successCount = res.count { it.status == CleanupStatus.SUCCESS || it.status == CleanupStatus.ALREADY_CLEAN }
        val failedCount = res.size - successCount
        Log.d("CACHE", "[CACHE][COMPLETE] success=$successCount failed=$failedCount totalFreed=${formatBytes(freed)}")

        _batchSession.value = session.copy(
            step = BatchCleanupStep.COMPLETE,
            currentPackageName = null,
            currentBeforeCacheBytes = 0L,
            results = res,
            totalBytesFreed = freed
        )

        // Remove successfully cleaned apps from persistent selection
        val successfulPkgs = res.filter { it.status == CleanupStatus.SUCCESS || it.status == CleanupStatus.ALREADY_CLEAN }
            .map { it.packageName }
            .toSet()
        if (successfulPkgs.isNotEmpty()) {
            _selectedPackages.value = _selectedPackages.value - successfulPkgs
        }

        refreshStorageSpace()
        scanInstalledApps()
    }

    fun cancelBatchCleanup() {
        val session = _batchSession.value
        if (!session.isRunning) return

        Log.d("BATCH", "cancel requested at index=${session.currentIndex}")
        val remainingCancelled = (session.currentIndex until session.queue.size).map { i ->
            val app = session.queue[i]
            CacheCleanupResult(
                packageName = app.packageName,
                appName = app.appName,
                status = CleanupStatus.CANCELLED,
                bytesReleased = 0L,
                detailMessage = "Cancelled by user"
            )
        }

        _batchSession.value = session.copy(
            step = BatchCleanupStep.CANCELLED,
            isCancelled = true,
            results = session.results + remainingCancelled,
            currentPackageName = null
        )

        refreshStorageSpace()
        scanInstalledApps()
    }

    fun dismissBatchResults() {
        _batchSession.value = BatchCleanupSession(step = BatchCleanupStep.IDLE)
        refreshStorageSpace()
        scanInstalledApps()
    }

    // ───────────────────────────────────────────────────────────
    // DIRECT INTERNAL CACHE CLEAN
    // ───────────────────────────────────────────────────────────

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    private val _lastCleanResult = MutableStateFlow<String?>(null)
    val lastCleanResult: StateFlow<String?> = _lastCleanResult.asStateFlow()

    fun cleanDirectDeviceCache() {
        viewModelScope.launch {
            _isCleaning.value = true
            var bytesFreed = 0L

            withContext(Dispatchers.IO) {
                try {
                    val cacheDir = context.cacheDir
                    bytesFreed += getFolderSize(cacheDir)
                    deleteFolderContents(cacheDir)
                } catch (_: Exception) {}

                try {
                    val codeCache = context.codeCacheDir
                    bytesFreed += getFolderSize(codeCache)
                    deleteFolderContents(codeCache)
                } catch (_: Exception) {}

                try {
                    context.externalCacheDir?.let {
                        bytesFreed += getFolderSize(it)
                        deleteFolderContents(it)
                    }
                } catch (_: Exception) {}

                try {
                    val externalFiles = context.getExternalFilesDirs(null)
                    for (dir in externalFiles) {
                        if (dir != null && dir.exists()) {
                            val tempDirs = dir.listFiles { f -> f.isDirectory && (f.name.contains("cache", ignoreCase = true) || f.name.contains("temp", ignoreCase = true)) }
                            tempDirs?.forEach {
                                bytesFreed += getFolderSize(it)
                                deleteFolderContents(it)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            _isCleaning.value = false
            val formatted = formatBytes(bytesFreed)
            _lastCleanResult.value = "Cleaned $formatted of temporary cache files."
            refreshStorageSpace()
            scanInstalledApps()
        }
    }

    private fun getFolderSize(file: java.io.File?): Long {
        if (file == null || !file.exists()) return 0L
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFolderSize(it) }
        return size
    }

    private fun deleteFolderContents(file: java.io.File?) {
        if (file == null || !file.exists()) return
        file.listFiles()?.forEach { child ->
            if (child.isDirectory) deleteFolderContents(child)
            try { child.delete() } catch (_: Exception) {}
        }
    }

    fun clearResults() {
        _lastCleanResult.value = null
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
            return String.format("%.2f %s", value, units[digitGroups])
        }
    }
}
