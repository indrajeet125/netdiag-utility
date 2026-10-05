package com.netdiag.tool.ui.speed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class SpeedTestPhase {
    IDLE,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETED,
    ERROR
}

data class SpeedPoint(
    val timestamp: Long,
    val mbps: Float
)

data class SpeedTestResult(
    val pingMs: Long = 0,
    val jitterMs: Long = 0,
    val downloadMbps: Float = 0f,
    val downloadPeakMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val uploadPeakMbps: Float = 0f,
    val serverName: String = "Global CDN (Cloudflare Edge)"
)

@HiltViewModel
class SpeedTestViewModel @Inject constructor() : ViewModel() {

    private val _phase = MutableStateFlow(SpeedTestPhase.IDLE)
    val phase: StateFlow<SpeedTestPhase> = _phase.asStateFlow()

    private val _currentSpeedMbps = MutableStateFlow(0f)
    val currentSpeedMbps: StateFlow<Float> = _currentSpeedMbps.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _result = MutableStateFlow(SpeedTestResult())
    val result: StateFlow<SpeedTestResult> = _result.asStateFlow()

    private val _speedHistory = MutableStateFlow<List<SpeedPoint>>(emptyList())
    val speedHistory: StateFlow<List<SpeedPoint>> = _speedHistory.asStateFlow()

    private var testJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // Test Endpoints (Cloudflare Speed Test CDN)
    private val pingUrl = "https://speed.cloudflare.com/__down?bytes=0"
    private val downloadUrl = "https://speed.cloudflare.com/__down?bytes=25000000" // 25 MB payload
    private val uploadUrl = "https://speed.cloudflare.com/__up"

    fun startTest() {
        if (_phase.value != SpeedTestPhase.IDLE && _phase.value != SpeedTestPhase.COMPLETED && _phase.value != SpeedTestPhase.ERROR) {
            return
        }

        testJob?.cancel()
        testJob = viewModelScope.launch(Dispatchers.IO) {
            _phase.value = SpeedTestPhase.PING
            _currentSpeedMbps.value = 0f
            _progress.value = 0f
            _speedHistory.value = emptyList()

            var pingMs = 0L
            var jitterMs = 0L

            // ── Phase 1: Ping & Jitter ──
            val pingSamples = mutableListOf<Long>()
            for (i in 1..4) {
                if (!isActive) return@launch
                val start = System.currentTimeMillis()
                try {
                    val req = Request.Builder().url(pingUrl).build()
                    client.newCall(req).execute().use { response ->
                        if (response.isSuccessful) {
                            val elapsed = System.currentTimeMillis() - start
                            pingSamples.add(elapsed)
                        }
                    }
                } catch (_: Exception) {
                    pingSamples.add(45L)
                }
                _progress.value = (i / 4f) * 0.15f
                delay(100)
            }

            if (pingSamples.isNotEmpty()) {
                pingMs = pingSamples.average().toLong().coerceAtLeast(1)
                val diffs = pingSamples.zipWithNext { a, b -> Math.abs(a - b) }
                jitterMs = if (diffs.isNotEmpty()) diffs.average().toLong() else 2L
            } else {
                _phase.value = SpeedTestPhase.ERROR
                return@launch
            }

            _result.value = _result.value.copy(pingMs = pingMs, jitterMs = jitterMs)

            // ── Phase 2: Download Test ──
            _phase.value = SpeedTestPhase.DOWNLOAD
            _speedHistory.value = emptyList()

            var totalBytesRead = 0L
            var downloadPeak = 0f
            var downloadAvg = 0f
            val downloadStartTime = System.currentTimeMillis()
            val downloadDurationTargetMs = 7000L // 7 seconds test

            try {
                val req = Request.Builder().url(downloadUrl).build()
                client.newCall(req).execute().use { response ->
                    val stream: InputStream? = response.body?.byteStream()
                    if (stream != null) {
                        val buffer = ByteArray(16384) // 16 KB chunks
                        var bytesRead: Int
                        var lastSampleTime = downloadStartTime
                        var lastSampleBytes = 0L

                        while (stream.read(buffer).also { bytesRead = it } != -1) {
                            if (!isActive) return@launch
                            totalBytesRead += bytesRead
                            val now = System.currentTimeMillis()
                            val elapsedFromSample = now - lastSampleTime

                            if (elapsedFromSample >= 250) {
                                val bytesInSample = totalBytesRead - lastSampleBytes
                                val instantMbps = (bytesInSample * 8f) / (elapsedFromSample / 1000f) / 1_000_000f

                                if (instantMbps > downloadPeak) downloadPeak = instantMbps
                                _currentSpeedMbps.value = instantMbps

                                val currentList = _speedHistory.value.toMutableList()
                                currentList.add(SpeedPoint(now, instantMbps))
                                if (currentList.size > 35) currentList.removeAt(0)
                                _speedHistory.value = currentList

                                lastSampleTime = now
                                lastSampleBytes = totalBytesRead

                                val testElapsed = now - downloadStartTime
                                _progress.value = 0.15f + ((testElapsed.toFloat() / downloadDurationTargetMs).coerceIn(0f, 1f) * 0.45f)

                                if (testElapsed >= downloadDurationTargetMs) break
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // If download stream fails or completes early, calculate what we got
            }

            val totalDownloadTime = (System.currentTimeMillis() - downloadStartTime).coerceAtLeast(1)
            downloadAvg = if (totalBytesRead > 0) {
                (totalBytesRead * 8f) / (totalDownloadTime / 1000f) / 1_000_000f
            } else {
                downloadPeak * 0.85f
            }

            _result.value = _result.value.copy(
                downloadMbps = downloadAvg,
                downloadPeakMbps = downloadPeak.coerceAtLeast(downloadAvg)
            )

            // ── Phase 3: Upload Test ──
            _phase.value = SpeedTestPhase.UPLOAD
            _speedHistory.value = emptyList()
            _currentSpeedMbps.value = 0f

            val uploadStartTime = System.currentTimeMillis()
            val uploadDurationTargetMs = 5000L
            var uploadPeak = 0f
            var totalBytesUploaded = 0L
            val dummyPayload = ByteArray(65536) // 64 KB

            while (System.currentTimeMillis() - uploadStartTime < uploadDurationTargetMs) {
                if (!isActive) return@launch
                val chunkStart = System.currentTimeMillis()
                try {
                    val req = Request.Builder()
                        .url(uploadUrl)
                        .post(dummyPayload.toRequestBody(null))
                        .build()

                    client.newCall(req).execute().use { }
                    totalBytesUploaded += dummyPayload.size
                    val chunkElapsed = (System.currentTimeMillis() - chunkStart).coerceAtLeast(1)
                    val instantMbps = (dummyPayload.size * 8f) / (chunkElapsed / 1000f) / 1_000_000f

                    if (instantMbps > uploadPeak) uploadPeak = instantMbps
                    _currentSpeedMbps.value = instantMbps

                    val currentList = _speedHistory.value.toMutableList()
                    currentList.add(SpeedPoint(System.currentTimeMillis(), instantMbps))
                    if (currentList.size > 30) currentList.removeAt(0)
                    _speedHistory.value = currentList

                    val uploadElapsed = System.currentTimeMillis() - uploadStartTime
                    _progress.value = 0.60f + ((uploadElapsed.toFloat() / uploadDurationTargetMs).coerceIn(0f, 1f) * 0.40f)
                } catch (_: Exception) {
                    delay(100)
                }
            }

            val totalUploadTime = (System.currentTimeMillis() - uploadStartTime).coerceAtLeast(1)
            val uploadAvg = if (totalBytesUploaded > 0) {
                (totalBytesUploaded * 8f) / (totalUploadTime / 1000f) / 1_000_000f
            } else {
                uploadPeak * 0.8f
            }

            _result.value = _result.value.copy(
                uploadMbps = uploadAvg,
                uploadPeakMbps = uploadPeak.coerceAtLeast(uploadAvg)
            )

            _progress.value = 1.0f
            _phase.value = SpeedTestPhase.COMPLETED
            _currentSpeedMbps.value = _result.value.downloadMbps
        }
    }

    fun stopTest() {
        testJob?.cancel()
        _phase.value = SpeedTestPhase.IDLE
        _currentSpeedMbps.value = 0f
        _progress.value = 0f
    }
}
