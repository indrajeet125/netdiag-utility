package com.netdiag.tool.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netdiag.tool.data.repository.TelephonyRepository
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LiveRefreshRate(val label: String, val intervalMs: Long) {
    AUTO("Auto", 2000L),
    ONE_SEC("1 sec", 1000L),
    TWO_SEC("2 sec", 2000L),
    FIVE_SEC("5 sec", 5000L),
    MANUAL("Manual", 0L)
}

enum class NeighborSort(val label: String) {
    SIGNAL("Signal"),
    TECHNOLOGY("Technology"),
    PCI("PCI"),
    FREQUENCY("Frequency")
}

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val telephonyRepo: TelephonyRepository
) : ViewModel() {

    val sim1: StateFlow<SimSubscription> = telephonyRepo.sim1
    val sim2: StateFlow<SimSubscription> = telephonyRepo.sim2
    val currentRat: StateFlow<RadioAccessTechnology> = telephonyRepo.currentRat
    val networkMode: StateFlow<NetworkModeInfo> = telephonyRepo.networkMode

    private val _selectedSimSlot = MutableStateFlow(0)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _refreshRate = MutableStateFlow(LiveRefreshRate.AUTO)
    val refreshRate: StateFlow<LiveRefreshRate> = _refreshRate.asStateFlow()

    private val _neighborSort = MutableStateFlow(NeighborSort.SIGNAL)
    val neighborSort: StateFlow<NeighborSort> = _neighborSort.asStateFlow()

    private val _showTechnicalDetails = MutableStateFlow(false)
    val showTechnicalDetails: StateFlow<Boolean> = _showTechnicalDetails.asStateFlow()

    private val _lastUpdatedMs = MutableStateFlow(System.currentTimeMillis())
    val lastUpdatedMs: StateFlow<Long> = _lastUpdatedMs.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refresh()
        startLivePolling()
    }

    fun selectSim(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun setRefreshRate(rate: LiveRefreshRate) {
        _refreshRate.value = rate
        startLivePolling()
    }

    fun setNeighborSort(sort: NeighborSort) {
        _neighborSort.value = sort
    }

    fun toggleTechnicalDetails(show: Boolean) {
        _showTechnicalDetails.value = show
    }

    fun refresh() {
        telephonyRepo.refresh()
        _lastUpdatedMs.value = System.currentTimeMillis()
    }

    private fun startLivePolling() {
        pollingJob?.cancel()
        val interval = _refreshRate.value.intervalMs
        if (interval > 0) {
            pollingJob = viewModelScope.launch {
                while (isActive) {
                    delay(interval)
                    refresh()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
