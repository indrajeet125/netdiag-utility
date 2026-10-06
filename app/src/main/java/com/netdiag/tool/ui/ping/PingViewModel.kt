package com.netdiag.tool.ui.ping

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.netdiag.tool.data.repository.PingRepository
import com.netdiag.tool.domain.model.*
import com.netdiag.tool.domain.strategy.PingStrategyFactory
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
    private val pingRepo: PingRepository,
    private val pingStrategyFactory: PingStrategyFactory,
    private val historyRepo: com.netdiag.tool.data.repository.TestHistoryRepository
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
                // Strategy Pattern Execution
                val strategy = pingStrategyFactory.getStrategy(config.type)
                val liveList = mutableListOf<SinglePingProbe>()
                
                strategy.execute(config).collect { probe ->
                    liveList.add(probe)
                    _state.value = _state.value.copy(liveProbes = liveList.toList())
                }

                // Compute final report when stream completes
                val transmitted = liveList.size
                val received = liveList.count { it.isSuccess }
                val lossPercent = if (transmitted > 0) ((transmitted - received).toFloat() / transmitted) * 100f else 0f
                val latencies = liveList.filter { it.isSuccess }.map { it.totalTimeMs }
                val minMs = latencies.minOrNull() ?: 0L
                val maxMs = latencies.maxOrNull() ?: 0L
                val avgMs = if (latencies.isNotEmpty()) latencies.average() else 0.0
                val jitterMs = if (latencies.size > 1) {
                    latencies.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average()
                } else 0.0

                val report = PingReport(
                    config = config,
                    probes = liveList.toList(),
                    transmitted = transmitted,
                    received = received,
                    lossPercent = lossPercent,
                    minMs = minMs,
                    avgMs = avgMs,
                    maxMs = maxMs,
                    jitterMs = jitterMs
                )

                _state.value = _state.value.copy(
                    isRunning = false,
                    report = report,
                    liveProbes = liveList.toList()
                )

                // Save to persistent test history
                historyRepo.addEntry(
                    com.netdiag.tool.data.repository.TestHistoryEntry(
                        category = com.netdiag.tool.data.repository.TestCategory.PING,
                        title = config.type.name,
                        target = config.target,
                        networkSummary = "${transmitted} pkts • ${lossPercent.toInt()}% loss",
                        resultStatus = "${avgMs.toInt()} ms",
                        latencyMs = avgMs.toLong(),
                        details = "Min: ${minMs}ms, Max: ${maxMs}ms, Jitter: ${jitterMs.toInt()}ms",
                        isSuccess = received > 0
                    )
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
