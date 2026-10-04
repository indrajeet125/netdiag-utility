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

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
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
    }
}
