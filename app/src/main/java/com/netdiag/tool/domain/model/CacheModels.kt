package com.netdiag.tool.domain.model

import android.graphics.drawable.Drawable

enum class ClearCapability {
    ACCESSIBILITY_AUTOMATED,
    MANUAL_SETTINGS_FALLBACK,
    SYSTEM_RESTRICTED,
    ALREADY_CLEAN
}

enum class CleanupStatus {
    SUCCESS,
    PARTIAL,
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    SKIPPED,
    FAILED,
    ALREADY_CLEAN,
    CANCELLED
}

data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val versionName: String = "1.0",
    val appIcon: Drawable? = null,
    val cacheSize: Long = 0L,
    val dataSize: Long = 0L,
    val codeSize: Long = 0L,
    val totalSize: Long = 0L,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false,
    val isIgnored: Boolean = false,
    val clearCapability: ClearCapability = ClearCapability.MANUAL_SETTINGS_FALLBACK
)

data class CacheScanSummary(
    val totalApps: Int = 0,
    val appsWithCache: Int = 0,
    val totalCacheSize: Long = 0L,
    val largestAppCache: AppStorageInfo? = null,
    val scanDurationMs: Long = 0L,
    val isScanning: Boolean = false
)

data class CacheCleanupResult(
    val packageName: String,
    val appName: String,
    val status: CleanupStatus,
    val bytesReleased: Long = 0L,
    val detailMessage: String? = null
)

data class StorageSpaceSummary(
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val usedBytes: Long = 0L
)

enum class BatchCleanupStep {
    IDLE,
    PREPARING,
    CLEANING_APP,
    WAITING_FOR_RESULT,
    RESULT_RECEIVED,
    NEXT_APP,
    COMPLETE,
    CANCELLED
}

data class BatchCleanupSession(
    val step: BatchCleanupStep = BatchCleanupStep.IDLE,
    val queue: List<AppStorageInfo> = emptyList(),
    val currentIndex: Int = 0,
    val currentPackageName: String? = null,
    val currentBeforeCacheBytes: Long = 0L,
    val results: List<CacheCleanupResult> = emptyList(),
    val totalBytesFreed: Long = 0L,
    val isCancelled: Boolean = false
) {
    val currentApp: AppStorageInfo?
        get() = queue.getOrNull(currentIndex)
    val totalApps: Int
        get() = queue.size
    val isRunning: Boolean
        get() = step != BatchCleanupStep.IDLE && step != BatchCleanupStep.COMPLETE && step != BatchCleanupStep.CANCELLED
}
