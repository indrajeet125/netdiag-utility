package com.netdiag.tool.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.netdiag.tool.ui.MainActivity

class OneTapAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var triggerButton: TextView? = null
    private var expandedMenu: LinearLayout? = null
    private var isExpanded = false

    private lateinit var layoutParams: WindowManager.LayoutParams
    private var screenWidth = 1080

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager?.defaultDisplay?.getMetrics(metrics)
        screenWidth = metrics.widthPixels

        createFloatingOverlay()
    }

    private fun createFloatingOverlay() {
        if (floatingView != null) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - 160
            y = 400
        }

        val root = FrameLayout(this)
        floatingView = root

        // 1. Floating Trigger Button (Compact Teal Pill)
        val triggerBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF141A23.toInt())
            setStroke(3, 0xFF00E5FF.toInt())
        }

        triggerButton = TextView(this).apply {
            text = "⚡"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(0xFF00E5FF.toInt())
            background = triggerBg
            layoutParams = FrameLayout.LayoutParams(130, 130).apply {
                gravity = Gravity.CENTER
            }
            elevation = 16f
        }
        root.addView(triggerButton)

        // 2. Expanded Menu Card
        val menuBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 32f
            setColor(0xFF141A23.toInt())
            setStroke(2, 0xFF283446.toInt())
        }

        expandedMenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = menuBg
            setPadding(32, 28, 32, 28)
            visibility = View.GONE
            elevation = 24f
            layoutParams = FrameLayout.LayoutParams(520, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        // Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = "ONE TAP UTILITY"
            textSize = 12f
            setTextColor(0xFF00E5FF.toInt())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(0xFF8E9BAE.toInt())
            setPadding(16, 8, 16, 8)
            setOnClickListener { collapseMenu() }
        }
        headerRow.addView(titleText)
        headerRow.addView(closeBtn)
        expandedMenu?.addView(headerRow)

        // Action Buttons
        addActionItem("⚡  Ping Diagnostics", "App & Socket Latency") {
            launchApp("tests")
        }
        addActionItem("📶  Cellular Telemetry", "RF Tower & Signal") {
            launchApp("network")
        }
        addActionItem("📱  Dual SIM State", "Carrier & Registration") {
            launchApp("sim_identity")
        }
        addActionItem("🌐  Network Settings", "Android Radio Policies") {
            openSystemSettings()
        }
        addActionItem("🧹  Cache Cleaner", "Storage & Cache Inspection") {
            launchApp("cache_cleaner")
        }
        addActionItem("⚡  Quick Recents", "System Task Overview") {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            Toast.makeText(this, "OneTap: Recents Opened", Toast.LENGTH_SHORT).show()
            collapseMenu()
        }

        root.addView(expandedMenu)

        // Drag & Touch Handling
        setupTouchListener()

        try {
            windowManager?.addView(root, layoutParams)
        } catch (_: Exception) {
            // Fallback to accessibility overlay type if application overlay not permitted
            layoutParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            try { windowManager?.addView(root, layoutParams) } catch (_: Exception) { }
        }
    }

    private fun addActionItem(title: String, subtitle: String, onClick: () -> Unit) {
        val itemBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            setColor(0xFF1C2430.toInt())
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = itemBg
            setPadding(24, 18, 24, 18)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            layoutParams = lp
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onClick()
                collapseMenu()
            }
        }

        val titleTv = TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subTv = TextView(this).apply {
            text = subtitle
            textSize = 11f
            setTextColor(0xFF8E9BAE.toInt())
            setPadding(0, 4, 0, 0)
        }
        row.addView(titleTv)
        row.addView(subTv)
        expandedMenu?.addView(row)
    }

    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        triggerButton?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 15 || Math.abs(dy) > 15) {
                        isMoving = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        try { windowManager?.updateViewLayout(floatingView, layoutParams) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        toggleMenu()
                    } else {
                        // Snap to edge
                        snapToEdge()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge() {
        val middle = screenWidth / 2
        layoutParams.x = if (layoutParams.x + 65 < middle) 16 else screenWidth - 150
        try { windowManager?.updateViewLayout(floatingView, layoutParams) } catch (_: Exception) { }
    }

    private fun toggleMenu() {
        if (isExpanded) {
            collapseMenu()
        } else {
            expandMenu()
        }
    }

    private fun expandMenu() {
        isExpanded = true
        triggerButton?.visibility = View.GONE
        expandedMenu?.visibility = View.VISIBLE
        // Adjust position so menu is visible on screen
        if (layoutParams.x + 520 > screenWidth) {
            layoutParams.x = screenWidth - 540
        }
        if (layoutParams.x < 16) {
            layoutParams.x = 16
        }
        try { windowManager?.updateViewLayout(floatingView, layoutParams) } catch (_: Exception) { }
    }

    private fun collapseMenu() {
        isExpanded = false
        expandedMenu?.visibility = View.GONE
        triggerButton?.visibility = View.VISIBLE
        snapToEdge()
    }

    private fun launchApp(route: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", route)
        }
        startActivity(intent)
    }

    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { startActivity(intent) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
    }

    companion object {
        var isAutoClearActive = true

        // State fencing
        @Volatile
        var targetCleaningPackage: String? = null

        @Volatile
        var currentStep: A11yCleanStep = A11yCleanStep.IDLE

        @Volatile
        var isActionInProgress: Boolean = false

        fun resetAutomationState() {
            targetCleaningPackage = null
            currentStep = A11yCleanStep.IDLE
            isActionInProgress = false
        }

        fun beginCleaning(packageName: String) {
            targetCleaningPackage = packageName
            currentStep = A11yCleanStep.WAITING_FOR_APP_INFO
            isActionInProgress = false
            android.util.Log.d("CACHE", "[CACHE][A11Y] package=$packageName state=WAITING_FOR_APP_INFO action=AUTOMATION_INIT")
        }
    }

    enum class A11yCleanStep {
        IDLE,
        WAITING_FOR_APP_INFO,
        WAITING_FOR_STORAGE,
        STORAGE_CLICKED,
        WAITING_FOR_CLEAR_CACHE,
        CLEAR_CACHE_CLICKED,
        WAIT_AFTER_CLEAR,
        RETURNING_TO_APP
    }

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var retryRunnable: Runnable? = null
    private var retryCount = 0
    private val MAX_RETRIES = 12 // ~12 * 400ms = 4.8s timeout

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAutoClearActive || event == null) return
        val targetPkg = targetCleaningPackage ?: return

        // State fencing: only react when relevant Settings or package window is active
        val eventPkg = event.packageName?.toString() ?: ""
        val isSettingsWindow = eventPkg.contains("settings", ignoreCase = true) || eventPkg.contains("packageinstaller", ignoreCase = true)
        
        if (!isSettingsWindow && currentStep != A11yCleanStep.IDLE) {
            // Not in settings
            return
        }

        // Prevent duplicate action dispatch
        if (isActionInProgress) return

        when (currentStep) {
            A11yCleanStep.WAITING_FOR_APP_INFO, A11yCleanStep.WAITING_FOR_STORAGE -> {
                attemptFindAndClickStorage(targetPkg)
            }
            A11yCleanStep.WAITING_FOR_CLEAR_CACHE -> {
                attemptFindAndClickClearCache(targetPkg)
            }
            else -> {
                // Ignore other events while settling or returning
            }
        }
    }

    private fun attemptFindAndClickStorage(targetPkg: String) {
        if (isActionInProgress) return
        val rootNode = rootInActiveWindow ?: return

        val storageNode = findNodeByText(rootNode, listOf("Storage & cache", "Storage and cache", "Storage", "Internal shared storage"))
        if (storageNode != null) {
            android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg state=WAITING_FOR_STORAGE nodeFound=true")
            isActionInProgress = true
            currentStep = A11yCleanStep.STORAGE_CLICKED

            // Human delay: 800-1100ms before clicking storage
            mainHandler.postDelayed({
                val clicked = performNodeClick(storageNode)
                android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=CLICK_STORAGE success=$clicked")
                if (clicked) {
                    currentStep = A11yCleanStep.WAITING_FOR_CLEAR_CACHE
                    // Delay 1000-1200ms for Storage screen to open and settle
                    mainHandler.postDelayed({
                        isActionInProgress = false
                        scheduleClearCacheRetry(targetPkg)
                    }, 1100)
                } else {
                    isActionInProgress = false
                    currentStep = A11yCleanStep.WAITING_FOR_STORAGE
                }
            }, 850)
        } else {
            // UI element not yet rendered: schedule quick retry
            scheduleStorageRetry(targetPkg)
        }
    }

    private fun scheduleStorageRetry(targetPkg: String) {
        if (retryRunnable != null || isActionInProgress) return
        if (currentStep != A11yCleanStep.WAITING_FOR_APP_INFO && currentStep != A11yCleanStep.WAITING_FOR_STORAGE) return

        retryCount++
        if (retryCount > MAX_RETRIES) {
            android.util.Log.w("CACHE", "[CACHE][A11Y] package=$targetPkg state=WAITING_FOR_STORAGE error=UI_ELEMENT_NOT_FOUND (timeout)")
            retryCount = 0
            triggerFallbackReturn(targetPkg)
            return
        }

        retryRunnable = Runnable {
            retryRunnable = null
            attemptFindAndClickStorage(targetPkg)
        }
        mainHandler.postDelayed(retryRunnable!!, 350)
    }

    private fun attemptFindAndClickClearCache(targetPkg: String) {
        if (isActionInProgress) return
        val rootNode = rootInActiveWindow ?: return

        val clearCacheNode = findNodeByText(rootNode, listOf("Clear cache", "Clear Cache"))
        if (clearCacheNode != null) {
            val isEnabled = clearCacheNode.isEnabled || clearCacheNode.parent?.isEnabled == true
            android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg state=WAITING_FOR_CLEAR_CACHE nodeFound=true enabled=$isEnabled")

            if (!isEnabled) {
                // Button is disabled, meaning cache is already cleared (0 B)
                android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=ALREADY_CLEARED_DISABLED")
                isActionInProgress = true
                currentStep = A11yCleanStep.WAIT_AFTER_CLEAR
                mainHandler.postDelayed({
                    triggerDoubleBackReturn(targetPkg)
                }, 800)
                return
            }

            isActionInProgress = true
            currentStep = A11yCleanStep.CLEAR_CACHE_CLICKED

            // Human delay: 500-750ms before clicking "Clear cache"
            mainHandler.postDelayed({
                val clicked = performNodeClick(clearCacheNode)
                android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=CLICK_CLEAR_CACHE success=$clicked")
                currentStep = A11yCleanStep.WAIT_AFTER_CLEAR

                if (clicked) {
                    Toast.makeText(this, "Cache Cleared ✓", Toast.LENGTH_SHORT).show()
                }

                // CRITICAL TIMING: Wait 1800-2200ms after clicking clear cache so Android
                // finishes deletion, updates system stats & flushes UI
                android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=WAIT_AFTER_CLEAR")
                mainHandler.postDelayed({
                    triggerDoubleBackReturn(targetPkg)
                }, 1900)
            }, 600)
        } else {
            scheduleClearCacheRetry(targetPkg)
        }
    }

    private fun scheduleClearCacheRetry(targetPkg: String) {
        if (retryRunnable != null || isActionInProgress) return
        if (currentStep != A11yCleanStep.WAITING_FOR_CLEAR_CACHE) return

        retryCount++
        if (retryCount > MAX_RETRIES) {
            android.util.Log.w("CACHE", "[CACHE][A11Y] package=$targetPkg state=WAITING_FOR_CLEAR_CACHE error=UI_ELEMENT_NOT_FOUND (timeout)")
            retryCount = 0
            triggerFallbackReturn(targetPkg)
            return
        }

        retryRunnable = Runnable {
            retryRunnable = null
            attemptFindAndClickClearCache(targetPkg)
        }
        mainHandler.postDelayed(retryRunnable!!, 400)
    }

    private fun triggerDoubleBackReturn(targetPkg: String) {
        currentStep = A11yCleanStep.RETURNING_TO_APP
        android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=RETURN_TO_APP")

        // First back: from Storage screen to App Info
        performGlobalAction(GLOBAL_ACTION_BACK)

        // Wait 600ms before second back: from App Info to NetDiag
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            mainHandler.postDelayed({
                resetAutomationState()
            }, 500)
        }, 600)
    }

    private fun triggerFallbackReturn(targetPkg: String) {
        currentStep = A11yCleanStep.RETURNING_TO_APP
        android.util.Log.d("CACHE", "[CACHE][A11Y] package=$targetPkg action=FALLBACK_RETURN")
        performGlobalAction(GLOBAL_ACTION_BACK)
        mainHandler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            resetAutomationState()
        }, 500)
    }

    private fun findNodeByText(root: android.view.accessibility.AccessibilityNodeInfo, keywords: List<String>): android.view.accessibility.AccessibilityNodeInfo? {
        for (kw in keywords) {
            val list = root.findAccessibilityNodeInfosByText(kw)
            for (node in list) {
                val nodeText = node.text?.toString() ?: ""
                val contentDesc = node.contentDescription?.toString() ?: ""
                if (nodeText.contains(kw, ignoreCase = true) || contentDesc.contains(kw, ignoreCase = true)) {
                    return node
                }
            }
        }
        return null
    }

    private fun performNodeClick(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        }
        var p = node.parent
        while (p != null) {
            if (p.isClickable) {
                return p.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
            }
            p = p.parent
        }
        return false
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) { }
        }
        floatingView = null
    }
}
