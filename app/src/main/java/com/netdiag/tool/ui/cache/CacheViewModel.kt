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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    LARGE_CACHE("Large cache (>10 MB)"),
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

    private val _cleaningProgress = MutableStateFlow<Pair<Int, Int>?>(null) // current to total
    val cleaningProgress: StateFlow<Pair<Int, Int>?> = _cleaningProgress.asStateFlow()

    private val _cleanupResults = MutableStateFlow<List<CacheCleanupResult>?>(null)
    val cleanupResults: StateFlow<List<CacheCleanupResult>?> = _cleanupResults.asStateFlow()

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
        if (granted) {
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

    fun scanInstalledApps() {
        viewModelScope.launch {
            _scanSummary.value = _scanSummary.value.copy(isScanning = true)
            val startTime = System.currentTimeMillis()

            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
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

    fun toggleAppSelection(packageName: String) {
        _appList.value = _appList.value.map {
            if (it.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAll(select: Boolean) {
        _appList.value = _appList.value.map {
            it.copy(isSelected = select && !it.isIgnored)
        }
    }

    fun toggleIgnoreApp(packageName: String) {
        _appList.value = _appList.value.map {
            if (it.packageName == packageName) {
                val newIgnored = !it.isIgnored
                it.copy(isIgnored = newIgnored, isSelected = if (newIgnored) false else it.isSelected)
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

    fun clearResults() {
        _cleanupResults.value = null
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
