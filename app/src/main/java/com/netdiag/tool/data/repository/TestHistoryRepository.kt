package com.netdiag.tool.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class TestCategory {
    PING,
    PORT,
    WEB,
    CHECKUP
}

data class TestHistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: TestCategory,
    val title: String,          // e.g. "HTTPS", "TCP", "ICMP", "PORT", "CHECKUP"
    val target: String,         // e.g. "google.com:443", "192.168.1.10:80"
    val networkSummary: String, // e.g. "Cellular • SIM 1", "Wi-Fi"
    val resultStatus: String,   // e.g. "SUCCESS", "OPEN", "CLOSED", "HEALTHY", "PARTIAL"
    val latencyMs: Long? = null,
    val details: String = "",
    val isSuccess: Boolean = true
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
}

@Singleton
class TestHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("netdiag_test_history", Context.MODE_PRIVATE)
    private val _history = MutableStateFlow<List<TestHistoryEntry>>(loadHistory())
    val history: StateFlow<List<TestHistoryEntry>> = _history.asStateFlow()

    private fun loadHistory(): List<TestHistoryEntry> {
        val raw = prefs.getString("history_items", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(raw)
            val list = mutableListOf<TestHistoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TestHistoryEntry(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        category = try {
                            TestCategory.valueOf(obj.optString("category", TestCategory.PING.name))
                        } catch (_: Exception) {
                            TestCategory.PING
                        },
                        title = obj.optString("title", "Test"),
                        target = obj.optString("target", "--"),
                        networkSummary = obj.optString("networkSummary", "Default Network"),
                        resultStatus = obj.optString("resultStatus", "DONE"),
                        latencyMs = if (obj.has("latencyMs")) obj.optLong("latencyMs") else null,
                        details = obj.optString("details", ""),
                        isSuccess = obj.optBoolean("isSuccess", true)
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveHistory(list: List<TestHistoryEntry>) {
        try {
            val jsonArray = JSONArray()
            list.take(100).forEach { entry ->
                val obj = JSONObject().apply {
                    put("id", entry.id)
                    put("timestamp", entry.timestamp)
                    put("category", entry.category.name)
                    put("title", entry.title)
                    put("target", entry.target)
                    put("networkSummary", entry.networkSummary)
                    put("resultStatus", entry.resultStatus)
                    entry.latencyMs?.let { put("latencyMs", it) }
                    put("details", entry.details)
                    put("isSuccess", entry.isSuccess)
                }
                jsonArray.put(obj)
            }
            prefs.edit().putString("history_items", jsonArray.toString()).apply()
        } catch (_: Exception) { }
    }

    fun addEntry(entry: TestHistoryEntry) {
        val current = _history.value.toMutableList()
        current.add(0, entry)
        val trimmed = current.take(100)
        _history.value = trimmed
        saveHistory(trimmed)
    }

    fun deleteEntry(id: String) {
        val updated = _history.value.filterNot { it.id == id }
        _history.value = updated
        saveHistory(updated)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("history_items").apply()
    }

    fun getLatest(): TestHistoryEntry? = _history.value.firstOrNull()
}
