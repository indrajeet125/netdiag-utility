package com.netdiag.tool.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import com.netdiag.tool.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()

    private val _selectedPermissionDetail = MutableStateFlow<PermissionItem?>(null)
    val selectedPermissionDetail: StateFlow<PermissionItem?> = _selectedPermissionDetail.asStateFlow()

    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _canDrawOverlays = MutableStateFlow(false)
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _permissions.value = PermissionChecker.checkAll(context)
        _isAccessibilityActive.value = PermissionChecker.isAccessibilityEnabled(context)
        _canDrawOverlays.value = PermissionChecker.canDrawOverlays(context)
    }

    fun selectPermissionDetail(item: PermissionItem?) {
        _selectedPermissionDetail.value = item
    }

    fun completeOnboarding() {
        preferencesRepository.setOnboardingCompleted(true)
    }

    val requiredGrantedCount: Int
        get() = _permissions.value.count { it.isRequired && it.status == PermissionStatus.GRANTED }

    val totalRequiredCount: Int
        get() = _permissions.value.count { it.isRequired }

    val areRequiredPermissionsGranted: Boolean
        get() = requiredGrantedCount == totalRequiredCount
}
