package com.netdiag.tool.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppThemeMode {
    SYSTEM_DEFAULT,
    LIGHT,
    DARK
}

@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("netdiag_prefs", Context.MODE_PRIVATE)

    private val _onboardingCompleted = MutableStateFlow(
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    )
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _oneTapEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ONETAP_ENABLED, false)
    )
    val oneTapEnabled: StateFlow<Boolean> = _oneTapEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(
        try {
            AppThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM_DEFAULT.name) ?: AppThemeMode.SYSTEM_DEFAULT.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM_DEFAULT
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
    )
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    private val _dashboardCategories = MutableStateFlow(loadDashboardCategories())
    val dashboardCategories: StateFlow<List<com.netdiag.tool.domain.model.DashboardCategory>> = _dashboardCategories.asStateFlow()

    private fun loadDashboardCategories(): List<com.netdiag.tool.domain.model.DashboardCategory> {
        val raw = prefs.getString(KEY_DASHBOARD_CATEGORIES, null) ?: return com.netdiag.tool.domain.model.DashboardCategory.DEFAULT_ORDER
        return try {
            val list = raw.split(",").mapNotNull { name ->
                try {
                    com.netdiag.tool.domain.model.DashboardCategory.valueOf(name.trim())
                } catch (_: Exception) {
                    null
                }
            }
            if (list.size == com.netdiag.tool.domain.model.DashboardCategory.values().size) {
                list
            } else {
                com.netdiag.tool.domain.model.DashboardCategory.DEFAULT_ORDER
            }
        } catch (_: Exception) {
            com.netdiag.tool.domain.model.DashboardCategory.DEFAULT_ORDER
        }
    }

    fun setDashboardCategories(order: List<com.netdiag.tool.domain.model.DashboardCategory>) {
        val serialized = order.joinToString(",") { it.name }
        prefs.edit().putString(KEY_DASHBOARD_CATEGORIES, serialized).apply()
        _dashboardCategories.value = order
    }

    fun resetDashboardCategories() {
        prefs.edit().remove(KEY_DASHBOARD_CATEGORIES).apply()
        _dashboardCategories.value = com.netdiag.tool.domain.model.DashboardCategory.DEFAULT_ORDER
    }

    private val _favoriteServices = MutableStateFlow(
        prefs.getStringSet(KEY_FAVORITE_SERVICES, setOf("speed_test", "cell_telemetry", "sim_identity", "device_telemetry"))
            ?: setOf("speed_test", "cell_telemetry", "sim_identity", "device_telemetry")
    )
    val favoriteServices: StateFlow<Set<String>> = _favoriteServices.asStateFlow()

    fun toggleFavoriteService(serviceId: String) {
        val current = _favoriteServices.value.toMutableSet()
        if (current.contains(serviceId)) {
            current.remove(serviceId)
        } else {
            current.add(serviceId)
        }
        prefs.edit().putStringSet(KEY_FAVORITE_SERVICES, current).apply()
        _favoriteServices.value = current
    }

    fun setOnboardingCompleted(completed: Boolean) {
        android.util.Log.d("Onboarding", "[Onboarding] persisted completed = $completed")
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).commit()
        _onboardingCompleted.value = completed
    }

    fun setOneTapEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ONETAP_ENABLED, enabled).apply()
        _oneTapEnabled.value = enabled
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColorEnabled.value = enabled
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
        _onboardingCompleted.value = false
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_ONETAP_ENABLED = "key_onetap_enabled"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_DASHBOARD_CATEGORIES = "key_dashboard_categories"
        private const val KEY_FAVORITE_SERVICES = "key_favorite_services"
    }
}
