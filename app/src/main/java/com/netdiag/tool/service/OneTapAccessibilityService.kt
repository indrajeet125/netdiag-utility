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
            launchApp("ping/http")
        }
        addActionItem("📶  Cellular Telemetry", "RF Tower & Signal") {
            launchApp("network")
        }
        addActionItem("📱  Dual SIM State", "Carrier & Registration") {
            launchApp("sim_info")
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { }

    override fun onInterrupt() { }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            try { windowManager?.removeView(it) } catch (_: Exception) { }
        }
        floatingView = null
    }
}
