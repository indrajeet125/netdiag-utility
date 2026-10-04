package com.netdiag.tool.ui.ping

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netdiag.tool.data.repository.PingRepository
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PingUiState(
    val isRunning: Boolean = false,
    val config: PingConfiguration = PingConfiguration(),
    val report: PingReport? = null,
    val liveProbes: List<SinglePingProbe> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PingViewModel @Inject constructor(
    private val pingRepo: PingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PingUiState())
    val state: StateFlow<PingUiState> = _state.asStateFlow()

    private var pingJob: Job? = null

    fun updateConfig(config: PingConfiguration) {
        _state.value = _state.value.copy(config = config)
    }

    fun startPing() {
        if (_state.value.isRunning) return
        val config = _state.value.config
        _state.value = _state.value.copy(isRunning = true, liveProbes = emptyList(), report = null, error = null)

        pingJob = viewModelScope.launch {
            try {
                val report = when (config.type) {
                    PingType.HTTP -> pingRepo.executeHttpPing(config)
                    PingType.TCP -> pingRepo.executeTcpPing(config)
                    PingType.ICMP -> pingRepo.executeIcmpPing(config)
                }
                _state.value = _state.value.copy(
                    isRunning = false,
                    report = report,
                    liveProbes = report.probes
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    fun stopPing() {
        pingJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }
}
