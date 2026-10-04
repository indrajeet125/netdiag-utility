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
    private val telephonyRepo: TelephonyRepository
) : ViewModel() {

    val networkState: StateFlow<NetworkState> = networkRepo.networkState
    val sim1: StateFlow<SimSubscription> = telephonyRepo.sim1
    val sim2: StateFlow<SimSubscription> = telephonyRepo.sim2
    val currentRat: StateFlow<RadioAccessTechnology> = telephonyRepo.currentRat

    val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

    fun refresh() {
        networkRepo.refreshNetworkState()
        telephonyRepo.refresh()
    }
}
