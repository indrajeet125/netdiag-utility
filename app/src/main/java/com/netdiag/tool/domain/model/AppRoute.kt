package com.netdiag.tool.domain.model

/**
 * Type-safe application routes adhering to Single Responsibility Principle.
 * Eliminates magic string duplication across OneTapAccessibilityService,
 * SearchRunnerSheet, Drawer, and Jetpack Compose NavHost.
 */
sealed class AppRoute(val route: String) {
    object Splash : AppRoute("splash")
    object Welcome : AppRoute("welcome")
    object OnboardingSlides : AppRoute("onboarding_slides")
    object PermissionChecklist : AppRoute("permission_checklist")
    object OneTapSetup : AppRoute("onetap_setup")
    object SetupComplete : AppRoute("setup_complete")

    // Main Destinations
    object Home : AppRoute("home")
    object Tests : AppRoute("tests")
    object Network : AppRoute("network")
    object Tools : AppRoute("tools")

    // Feature Telemetry Screens
    object SpeedTest : AppRoute("speed_test")
    object DeviceInfo : AppRoute("device_info")
    object SimIdentity : AppRoute("sim_identity")
    object CacheCleaner : AppRoute("cache_cleaner")
    object NetworkMode : AppRoute("network_mode")
    object Settings : AppRoute("settings")
}
