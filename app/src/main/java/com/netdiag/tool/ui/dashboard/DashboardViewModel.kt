package com.netdiag.tool.ui.dashboard

import android.os.Build
import androidx.lifecycle.ViewModel
import com.netdiag.tool.data.repository.NetworkRepository
import com.netdiag.tool.data.repository.TelephonyRepository
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val networkRepo: NetworkRepository,
    private val telephonyRepo: TelephonyRepository,
    private val preferencesRepo: com.netdiag.tool.data.repository.AppPreferencesRepository,
    private val historyRepo: com.netdiag.tool.data.repository.TestHistoryRepository
) : ViewModel() {

    val networkState: StateFlow<NetworkState> = networkRepo.networkState
    val sim1: StateFlow<SimSubscription> = telephonyRepo.sim1
    val sim2: StateFlow<SimSubscription> = telephonyRepo.sim2
    val currentRat: StateFlow<RadioAccessTechnology> = telephonyRepo.currentRat
    val dashboardCategories: StateFlow<List<com.netdiag.tool.domain.model.DashboardCategory>> = preferencesRepo.dashboardCategories
    val favoriteServices: StateFlow<Set<String>> = preferencesRepo.favoriteServices
    val oneTapEnabled: StateFlow<Boolean> = preferencesRepo.oneTapEnabled
    val historyList: StateFlow<List<com.netdiag.tool.data.repository.TestHistoryEntry>> = historyRepo.history

    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun refresh() {
        networkRepo.refreshNetworkState()
        telephonyRepo.refresh()
    }

    fun updateCategoriesOrder(order: List<com.netdiag.tool.domain.model.DashboardCategory>) {
        preferencesRepo.setDashboardCategories(order)
    }

    fun resetCategoriesOrder() {
        preferencesRepo.resetDashboardCategories()
    }

    fun toggleFavoriteService(serviceId: String) {
        preferencesRepo.toggleFavoriteService(serviceId)
    }

    fun setOneTapEnabled(enabled: Boolean) {
        preferencesRepo.setOneTapEnabled(enabled)
    }
}
