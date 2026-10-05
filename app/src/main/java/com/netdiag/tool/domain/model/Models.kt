package com.netdiag.tool.domain.model

// ── Ping Models ──

enum class PingType(val label: String) {
    HTTP("HTTP PING"),
    TCP("TCP PING"),
    ICMP("ICMP PING")
}

sealed interface NetworkTarget {
    object Default : NetworkTarget
    object Wifi : NetworkTarget
    data class Cellular(val subId: Int, val slotIndex: Int) : NetworkTarget
}

data class PingConfiguration(
    val type: PingType = PingType.HTTP,
    val target: String = "google.com",
    val port: Int = 443,
    val count: Int = 10,
    val timeoutMs: Int = 3000,
    val intervalMs: Long = 1000L,
    val networkTarget: NetworkTarget = NetworkTarget.Default
)

data class SinglePingProbe(
    val sequence: Int,
    val dnsTimeMs: Long? = null,
    val connectTimeMs: Long? = null,
    val tlsTimeMs: Long? = null,
    val ttfbMs: Long? = null,
    val totalTimeMs: Long,
    val responseCode: Int? = null,
    val responseSizeBytes: Long? = null,
    val isSuccess: Boolean,
    val failureReason: String? = null
)

data class PingReport(
    val config: PingConfiguration,
    val probes: List<SinglePingProbe>,
    val transmitted: Int,
    val received: Int,
    val lossPercent: Float,
    val minMs: Long,
    val avgMs: Double,
    val maxMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

// ── Telephony Models ──

enum class RadioAccessTechnology(val label: String, val shortLabel: String, val generation: String) {
    UNKNOWN("Unknown", "?", ""),
    GPRS("GPRS", "2G", "2G"),
    EDGE("EDGE", "2G", "2G"),
    UMTS("UMTS", "3G", "3G"),
    HSPA("HSPA", "3G", "3G"),
    HSPAP("HSPA+", "3G+", "3G"),
    LTE("LTE", "4G", "4G"),
    LTE_CA("LTE-A", "4G+", "4G"),
    NR_NSA("NR NSA", "5G", "5G"),
    NR_SA("NR SA", "5G", "5G"),
    IWLAN("IWLAN", "WiFi", "WiFi")
}

data class SimInfo(
    val subscriptionId: Int = -1,
    val slotIndex: Int = 0,
    val displayName: String = "",
    val carrierName: String = "Unknown",
    val mcc: String? = null,
    val mnc: String? = null,
    val plmn: String? = null,
    val countryIso: String = "",
    val isDefaultData: Boolean = false,
    val isRoaming: Boolean = false,
    val simState: String = "UNKNOWN",
    val isEmbedded: Boolean = false,
    val iccId: String? = null,
    val imsi: String? = null,
    val msisdn: String? = null,
    val cardId: Int = -1
)

data class CellData(
    val rat: RadioAccessTechnology = RadioAccessTechnology.UNKNOWN,
    val pci: Int? = null,
    val tac: Int? = null,
    val cellId: Long? = null,
    val enbId: Int? = null,
    val sectorId: Int? = null,
    val arfcn: Int? = null,
    val band: String? = null,
    val bandwidthKhz: Int? = null,
    val timingAdvance: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Double? = null,
    val rssi: Int? = null,
    val cqi: Int? = null,
    val signalLevel: Int = 0,
    val isServing: Boolean = false,
    val mcc: String? = null,
    val mnc: String? = null,
    // 2G / 3G specific
    val rscp: Int? = null,
    val ecNo: Double? = null,
    val ber: Int? = null,
    val bsic: Int? = null,
    val lac: Int? = null,
    // NR-specific
    val ssRsrp: Int? = null,
    val ssRsrq: Int? = null,
    val ssSinr: Double? = null,
    val csiRsrp: Int? = null,
    val csiRsrq: Int? = null,
    val csiSinr: Double? = null,
    val connectionStatus: String? = null,
    val timestampNs: Long = 0L
)

data class NetworkState(
    val isConnected: Boolean = false,
    val transport: String = "NONE",
    val rat: RadioAccessTechnology = RadioAccessTechnology.UNKNOWN,
    val isValidated: Boolean = false,
    val isMetered: Boolean = true,
    val ipv4: String? = null,
    val ipv6: String? = null,
    val dns: List<String> = emptyList(),
    val gateway: String? = null
)

data class SimSubscription(
    val info: SimInfo = SimInfo(),
    val rat: RadioAccessTechnology = RadioAccessTechnology.UNKNOWN,
    val servingCell: CellData? = null,
    val secondaryServingCell: CellData? = null,
    val neighborCells: List<CellData> = emptyList(),
    val registrationState: String = "UNKNOWN",
    val dataState: String = "DISCONNECTED",
    val isVoLTE: Boolean = false,
    val isVoWiFi: Boolean = false,
    val isImsRegistered: Boolean = false,
    val videoCallingSupported: Boolean = false,
    val signalDbm: Int? = null,
    val nsaMode: String? = null
)

// ── Network Mode Models ──

data class NetworkModeInfo(
    val currentMode: String = "Unknown",
    val observedRat: RadioAccessTechnology = RadioAccessTechnology.UNKNOWN,
    val canChange: Boolean = false,
    val restrictionReason: String? = "Normal app privileges - carrier/system access required",
    val availableModes: List<String> = listOf("Auto", "4G/3G/2G", "4G Only", "5G Preferred", "5G Only")
)

// ── Network Transition Events ──

data class NetworkTransitionEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val transport: String = "",
    val fromState: String = "",
    val toState: String = ""
)

// ── OneTap Actions ──

data class OneTapAction(
    val id: String,
    val label: String,
    val iconName: String,
    val enabled: Boolean = true
)
