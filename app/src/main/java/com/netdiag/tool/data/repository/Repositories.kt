package com.netdiag.tool.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.LinkProperties
import android.os.Build
import android.telephony.*
import android.telephony.SubscriptionManager
import com.netdiag.tool.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    fun refreshNetworkState() {
        val network = connectivityManager.activeNetwork
        val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val linkProps = network?.let { connectivityManager.getLinkProperties(it) }

        val transport = when {
            caps == null -> "NONE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }

        val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isMetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) != true

        val addresses = linkProps?.linkAddresses ?: emptyList()
        val ipv4 = addresses.firstOrNull { it.address is java.net.Inet4Address }?.address?.hostAddress
        val ipv6 = addresses.firstOrNull { it.address is java.net.Inet6Address }?.address?.hostAddress
        val dnsServers = linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
        val gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress

        _networkState.value = NetworkState(
            isConnected = isConnected && isValidated,
            transport = transport,
            isValidated = isValidated,
            isMetered = isMetered,
            ipv4 = ipv4,
            ipv6 = ipv6,
            dns = dnsServers,
            gateway = gateway
        )
    }
}

@Singleton
class TelephonyRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val _sim1 = MutableStateFlow(SimSubscription())
    val sim1: StateFlow<SimSubscription> = _sim1.asStateFlow()

    private val _sim2 = MutableStateFlow(SimSubscription())
    val sim2: StateFlow<SimSubscription> = _sim2.asStateFlow()

    private val _currentRat = MutableStateFlow(RadioAccessTechnology.UNKNOWN)
    val currentRat: StateFlow<RadioAccessTechnology> = _currentRat.asStateFlow()

    private val _networkMode = MutableStateFlow(NetworkModeInfo())
    val networkMode: StateFlow<NetworkModeInfo> = _networkMode.asStateFlow()

    @Suppress("MissingPermission")
    fun refresh() {
        try {
            val subManager = SubscriptionManager.from(context)
            val subs = try { subManager.activeSubscriptionInfoList ?: emptyList() } catch (e: SecurityException) { emptyList() }
            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()

            subs.forEachIndexed { index, subInfo ->
                val sub = buildSimSubscription(subInfo, defaultDataSubId)
                if (index == 0) _sim1.value = sub
                else if (index == 1) _sim2.value = sub
            }

            // Current RAT from active subscription
            val rat = mapNetworkType(telephonyManager.dataNetworkType)
            _currentRat.value = rat
            _sim1.value = _sim1.value.copy(rat = if (subs.isNotEmpty()) rat else RadioAccessTechnology.UNKNOWN)

            // Network mode info
            _networkMode.value = NetworkModeInfo(
                currentMode = "Auto",
                observedRat = rat,
                canChange = false,
                restrictionReason = "Normal app privileges - MODIFY_PHONE_STATE or carrier privileges required",
                availableModes = listOf("Auto", "4G/3G/2G", "4G Only", "5G Preferred", "5G Only")
            )
        } catch (e: Exception) {
            // Handle gracefully
        }
    }

    @Suppress("MissingPermission")
    private fun buildSimSubscription(subInfo: SubscriptionInfo, defaultDataSubId: Int): SimSubscription {
        val subTm = telephonyManager.createForSubscriptionId(subInfo.subscriptionId)
        val mcc = subInfo.mccString
        val mnc = subInfo.mncString
        val plmn = if (!mcc.isNullOrBlank() && !mnc.isNullOrBlank()) "$mcc$mnc" else subTm.networkOperator.takeIf { it.isNotBlank() }

        val isRoaming = try { subTm.isNetworkRoaming } catch (_: Exception) { false }

        // Extract sensitive identifiers (ICCID, MSISDN, IMSI)
        var iccId: String? = try { subInfo.iccId?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        if (iccId.isNullOrBlank()) {
            iccId = try { subTm.simSerialNumber?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        }

        var msisdn: String? = try { subInfo.number?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        if (msisdn.isNullOrBlank()) {
            msisdn = try { subTm.line1Number?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        }

        val imsi: String? = try { subTm.subscriberId?.takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        val cardId: Int = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) subInfo.cardId else -1
        } catch (_: Exception) { -1 }

        val simInfo = SimInfo(
            subscriptionId = subInfo.subscriptionId,
            slotIndex = subInfo.simSlotIndex,
            displayName = subInfo.displayName?.toString() ?: "",
            carrierName = subInfo.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: subTm.networkOperatorName.takeIf { it.isNotBlank() } ?: "Unknown",
            mcc = mcc,
            mnc = mnc,
            plmn = plmn,
            countryIso = subInfo.countryIso.ifBlank { subTm.networkCountryIso },
            isDefaultData = subInfo.subscriptionId == defaultDataSubId,
            isRoaming = isRoaming,
            isEmbedded = subInfo.isEmbedded,
            iccId = iccId,
            imsi = imsi,
            msisdn = msisdn,
            cardId = cardId
        )

        // Get cell info
        var servingCell: CellData? = null
        var secondaryServingCell: CellData? = null
        val neighborCells = mutableListOf<CellData>()
        var signalDbm: Int? = null

        try {
            val cellInfoList = subTm.allCellInfo ?: emptyList()
            for (ci in cellInfoList) {
                val cellData = parseCellInfo(ci)
                if (cellData != null) {
                    if (ci.isRegistered) {
                        if (servingCell == null) {
                            servingCell = cellData.copy(isServing = true)
                            signalDbm = cellData.rsrp ?: cellData.ssRsrp ?: cellData.rssi
                        } else {
                            secondaryServingCell = cellData.copy(isServing = true)
                        }
                    } else {
                        neighborCells.add(cellData)
                    }
                }
            }
        } catch (_: SecurityException) { }

        val dataState = when (subTm.dataState) {
            TelephonyManager.DATA_CONNECTED -> "CONNECTED"
            TelephonyManager.DATA_CONNECTING -> "CONNECTING"
            TelephonyManager.DATA_SUSPENDED -> "SUSPENDED"
            else -> "DISCONNECTED"
        }

        var rat = mapNetworkType(subTm.dataNetworkType)
        if (rat == RadioAccessTechnology.UNKNOWN && servingCell != null) {
            rat = servingCell.rat
        }

        // 5G NSA vs SA determination
        var nsaMode: String? = null
        if (rat == RadioAccessTechnology.NR_SA || servingCell?.rat == RadioAccessTechnology.NR_SA || secondaryServingCell?.rat == RadioAccessTechnology.NR_SA) {
            val hasLte = servingCell?.rat == RadioAccessTechnology.LTE || secondaryServingCell?.rat == RadioAccessTechnology.LTE || neighborCells.any { it.rat == RadioAccessTechnology.LTE }
            if (hasLte) {
                rat = RadioAccessTechnology.NR_NSA
                nsaMode = "NR + LTE Anchor"
            } else {
                nsaMode = "Standalone NR"
            }
        }

        // IMS / VoLTE / VoWiFi
        var isVoLTE = false
        var isVoWiFi = false
        var isIms = false
        try {
            val networkType = subTm.networkType
            val dataNetType = subTm.dataNetworkType
            if (networkType == TelephonyManager.NETWORK_TYPE_LTE || dataNetType == TelephonyManager.NETWORK_TYPE_LTE || rat == RadioAccessTechnology.LTE || rat == RadioAccessTechnology.NR_NSA) {
                isVoLTE = true
                isIms = true
            }
            if (dataNetType == TelephonyManager.NETWORK_TYPE_IWLAN) {
                isVoWiFi = true
                isIms = true
            }
        } catch (_: Exception) { }

        return SimSubscription(
            info = simInfo,
            rat = rat,
            servingCell = servingCell,
            secondaryServingCell = secondaryServingCell,
            neighborCells = neighborCells.sortedByDescending { it.rsrp ?: it.ssRsrp ?: it.rssi ?: -999 },
            registrationState = if (servingCell != null) "REGISTERED" else "NOT REGISTERED",
            dataState = dataState,
            isVoLTE = isVoLTE,
            isVoWiFi = isVoWiFi,
            isImsRegistered = isIms,
            videoCallingSupported = isIms,
            signalDbm = signalDbm,
            nsaMode = nsaMode
        )
    }

    private fun parseCellInfo(cellInfo: CellInfo): CellData? {
        return when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                val ss = cellInfo.cellSignalStrength
                val earfcn = id.earfcn.takeIf { it != Int.MAX_VALUE }
                val ci = id.ci.toLong().takeIf { it != Int.MAX_VALUE.toLong() && it > 0 }
                val enb = ci?.let { (it shr 8).toInt() }
                val sector = ci?.let { (it and 0xFF).toInt() }
                val bw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    id.bandwidth.takeIf { it != Int.MAX_VALUE && it > 0 }
                } else null
                val ta = ss.timingAdvance.takeIf { it != Int.MAX_VALUE && it >= 0 }
                val connStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (cellInfo.cellConnectionStatus) {
                        CellInfo.CONNECTION_PRIMARY_SERVING -> "PRIMARY_SERVING"
                        CellInfo.CONNECTION_SECONDARY_SERVING -> "SECONDARY_SERVING"
                        CellInfo.CONNECTION_NONE -> "NEIGHBOR"
                        else -> null
                    }
                } else null

                CellData(
                    rat = RadioAccessTechnology.LTE,
                    pci = id.pci.takeIf { it != Int.MAX_VALUE },
                    tac = id.tac.takeIf { it != Int.MAX_VALUE },
                    cellId = ci,
                    enbId = enb,
                    sectorId = sector,
                    arfcn = earfcn,
                    band = RadioBandUtils.calculateLteBand(earfcn),
                    bandwidthKhz = bw,
                    timingAdvance = ta,
                    rsrp = ss.rsrp.takeIf { it != Int.MAX_VALUE },
                    rsrq = ss.rsrq.takeIf { it != Int.MAX_VALUE },
                    sinr = ss.rssnr.takeIf { it != Int.MAX_VALUE }?.toDouble(),
                    rssi = ss.rssi.takeIf { it != Int.MAX_VALUE },
                    cqi = ss.cqi.takeIf { it != Int.MAX_VALUE },
                    signalLevel = ss.level,
                    mcc = id.mccString,
                    mnc = id.mncString,
                    connectionStatus = connStatus,
                    timestampNs = cellInfo.timestampMillis * 1_000_000L
                )
            }
            is CellInfoNr -> {
                val id = cellInfo.cellIdentity as CellIdentityNr
                val ss = cellInfo.cellSignalStrength as CellSignalStrengthNr
                val nrarfcn = id.nrarfcn.takeIf { it != Int.MAX_VALUE }
                val nci = id.nci.takeIf { it != Long.MAX_VALUE && it > 0L }
                val gnb = nci?.let { (it shr 14).toInt() }
                val sector = nci?.let { (it and 0x3FFF).toInt() }
                val connStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    when (cellInfo.cellConnectionStatus) {
                        CellInfo.CONNECTION_PRIMARY_SERVING -> "PRIMARY_SERVING"
                        CellInfo.CONNECTION_SECONDARY_SERVING -> "SECONDARY_SERVING"
                        CellInfo.CONNECTION_NONE -> "NEIGHBOR"
                        else -> null
                    }
                } else null

                CellData(
                    rat = RadioAccessTechnology.NR_SA,
                    pci = id.pci.takeIf { it != Int.MAX_VALUE },
                    tac = id.tac.takeIf { it != Int.MAX_VALUE },
                    cellId = nci,
                    enbId = gnb,
                    sectorId = sector,
                    arfcn = nrarfcn,
                    band = RadioBandUtils.calculateNrBand(nrarfcn),
                    ssRsrp = ss.ssRsrp.takeIf { it != Int.MAX_VALUE },
                    ssRsrq = ss.ssRsrq.takeIf { it != Int.MAX_VALUE },
                    ssSinr = ss.ssSinr.takeIf { it != Int.MAX_VALUE }?.toDouble(),
                    csiRsrp = ss.csiRsrp.takeIf { it != Int.MAX_VALUE },
                    csiRsrq = ss.csiRsrq.takeIf { it != Int.MAX_VALUE },
                    csiSinr = ss.csiSinr.takeIf { it != Int.MAX_VALUE }?.toDouble(),
                    signalLevel = ss.level,
                    mcc = id.mccString,
                    mnc = id.mncString,
                    connectionStatus = connStatus,
                    timestampNs = cellInfo.timestampMillis * 1_000_000L
                )
            }
            is CellInfoGsm -> {
                val id = cellInfo.cellIdentity
                val ss = cellInfo.cellSignalStrength
                val ta = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val m = ss.javaClass.getMethod("getTimingAdvance")
                        (m.invoke(ss) as? Int)?.takeIf { it != Int.MAX_VALUE && it >= 0 }
                    } catch (_: Exception) { null }
                } else null
                val bsic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    id.bsic.takeIf { it != Int.MAX_VALUE }
                } else null
                val ber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ss.bitErrorRate.takeIf { it != Int.MAX_VALUE && it >= 0 }
                } else null

                CellData(
                    rat = RadioAccessTechnology.EDGE,
                    cellId = id.cid.toLong().takeIf { it != Int.MAX_VALUE.toLong() && it > 0 },
                    lac = id.lac.takeIf { it != Int.MAX_VALUE },
                    bsic = bsic,
                    arfcn = id.arfcn.takeIf { it != Int.MAX_VALUE },
                    rssi = ss.dbm.takeIf { it != Int.MAX_VALUE },
                    timingAdvance = ta,
                    ber = ber,
                    signalLevel = ss.level,
                    mcc = id.mccString,
                    mnc = id.mncString,
                    timestampNs = cellInfo.timestampMillis * 1_000_000L
                )
            }
            is CellInfoWcdma -> {
                val id = cellInfo.cellIdentity
                val ss = cellInfo.cellSignalStrength
                val ecNo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ss.ecNo.takeIf { it != Int.MAX_VALUE }?.toDouble()
                } else null

                CellData(
                    rat = RadioAccessTechnology.UMTS,
                    pci = id.psc.takeIf { it != Int.MAX_VALUE },
                    cellId = id.cid.toLong().takeIf { it != Int.MAX_VALUE.toLong() && it > 0 },
                    lac = id.lac.takeIf { it != Int.MAX_VALUE },
                    arfcn = id.uarfcn.takeIf { it != Int.MAX_VALUE },
                    rscp = ss.dbm.takeIf { it != Int.MAX_VALUE },
                    rssi = ss.dbm.takeIf { it != Int.MAX_VALUE },
                    ecNo = ecNo,
                    signalLevel = ss.level,
                    mcc = id.mccString,
                    mnc = id.mncString,
                    timestampNs = cellInfo.timestampMillis * 1_000_000L
                )
            }
            else -> null
        }
    }

    private fun mapNetworkType(type: Int): RadioAccessTechnology {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS -> RadioAccessTechnology.GPRS
            TelephonyManager.NETWORK_TYPE_EDGE -> RadioAccessTechnology.EDGE
            TelephonyManager.NETWORK_TYPE_UMTS -> RadioAccessTechnology.UMTS
            TelephonyManager.NETWORK_TYPE_HSPA -> RadioAccessTechnology.HSPA
            TelephonyManager.NETWORK_TYPE_HSPAP -> RadioAccessTechnology.HSPAP
            TelephonyManager.NETWORK_TYPE_LTE -> RadioAccessTechnology.LTE
            TelephonyManager.NETWORK_TYPE_NR -> RadioAccessTechnology.NR_SA
            TelephonyManager.NETWORK_TYPE_IWLAN -> RadioAccessTechnology.IWLAN
            else -> RadioAccessTechnology.UNKNOWN
        }
    }
}

object RadioBandUtils {
    fun calculateLteBand(earfcn: Int?): String? {
        if (earfcn == null) return null
        return when (earfcn) {
            in 0..599 -> "B1 (2100 MHz)"
            in 600..1199 -> "B2 (1900 MHz)"
            in 1200..1949 -> "B3 (1800 MHz)"
            in 1950..2399 -> "B4 (1700/2100 MHz)"
            in 2400..2649 -> "B5 (850 MHz)"
            in 2750..3449 -> "B7 (2600 MHz)"
            in 3450..3799 -> "B8 (900 MHz)"
            in 5000..5179 -> "B12 (700 MHz)"
            in 6500..6699 -> "B17 (700 MHz)"
            in 8690..9039 -> "B20 (800 MHz)"
            in 9210..9659 -> "B25 (1900 MHz)"
            in 9870..9919 -> "B28 (700 MHz)"
            in 37750..38249 -> "B38 (2600 MHz)"
            in 38650..39649 -> "B40 (2300 MHz)"
            in 39650..41589 -> "B41 (2500 MHz)"
            in 41590..43589 -> "B42 (3500 MHz)"
            in 43590..45589 -> "B43 (3700 MHz)"
            else -> "LTE ($earfcn)"
        }
    }

    fun calculateNrBand(nrarfcn: Int?): String? {
        if (nrarfcn == null) return null
        return when (nrarfcn) {
            in 151600..160600 -> "n28 (700 MHz)"
            in 422000..434000 -> "n1 (2100 MHz)"
            in 499200..537999 -> "n41 (2500 MHz)"
            in 620000..653333 -> "n78 (3500 MHz)"
            in 653334..710000 -> "n77 (3700 MHz)"
            in 2054166..2104165 -> "n258 (26 GHz)"
            else -> "NR ($nrarfcn)"
        }
    }
}

@Singleton
class PingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun startPing(config: PingConfiguration): Flow<SinglePingProbe> = flow {
        for (i in 1..config.count) {
            val probe = when (config.type) {
                PingType.HTTP -> doHttpProbe(i, config)
                PingType.TCP -> doTcpProbe(i, config)
                PingType.ICMP -> doIcmpProbe(i, config)
            }
            emit(probe)
            if (i < config.count) {
                kotlinx.coroutines.delay(config.intervalMs)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun executeHttpPing(config: PingConfiguration): PingReport = withContext(Dispatchers.IO) {
        val probes = mutableListOf<SinglePingProbe>()
        for (i in 1..config.count) {
            val probe = doHttpProbe(i, config)
            probes.add(probe)
            if (i < config.count) {
                kotlinx.coroutines.delay(config.intervalMs)
            }
        }
        buildReport(config, probes)
    }

    suspend fun executeTcpPing(config: PingConfiguration): PingReport = withContext(Dispatchers.IO) {
        val probes = mutableListOf<SinglePingProbe>()
        for (i in 1..config.count) {
            val probe = doTcpProbe(i, config)
            probes.add(probe)
            if (i < config.count) {
                kotlinx.coroutines.delay(config.intervalMs)
            }
        }
        buildReport(config, probes)
    }

    suspend fun executeIcmpPing(config: PingConfiguration): PingReport = withContext(Dispatchers.IO) {
        val probes = mutableListOf<SinglePingProbe>()
        for (i in 1..config.count) {
            val probe = doIcmpProbe(i, config)
            probes.add(probe)
            if (i < config.count) {
                kotlinx.coroutines.delay(config.intervalMs)
            }
        }
        buildReport(config, probes)
    }

    private fun doHttpProbe(seq: Int, config: PingConfiguration): SinglePingProbe {
        val start = System.currentTimeMillis()
        try {
            val scheme = if (config.port == 443 || config.port == 8443) "https" else "http"
            val urlStr = "$scheme://${config.target}:${config.port}"
            val url = URL(urlStr)

            val dnsStart = System.currentTimeMillis()
            InetAddress.getByName(config.target)
            val dnsTime = System.currentTimeMillis() - dnsStart

            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = config.timeoutMs
            conn.readTimeout = config.timeoutMs
            conn.requestMethod = "HEAD"
            conn.instanceFollowRedirects = false

            val connectStart = System.currentTimeMillis()
            conn.connect()
            val connectTime = System.currentTimeMillis() - connectStart

            val responseCode = conn.responseCode
            val totalTime = System.currentTimeMillis() - start

            conn.disconnect()

            return SinglePingProbe(
                sequence = seq,
                dnsTimeMs = dnsTime,
                connectTimeMs = connectTime,
                totalTimeMs = totalTime,
                responseCode = responseCode,
                isSuccess = responseCode in 200..399
            )
        } catch (e: java.net.SocketTimeoutException) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "TIMEOUT")
        } catch (e: java.net.UnknownHostException) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "DNS FAILURE")
        } catch (e: Exception) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = e.message?.take(50) ?: "ERROR")
        }
    }

    private fun doTcpProbe(seq: Int, config: PingConfiguration): SinglePingProbe {
        val start = System.currentTimeMillis()
        try {
            val dnsStart = System.currentTimeMillis()
            val addr = InetAddress.getByName(config.target)
            val dnsTime = System.currentTimeMillis() - dnsStart

            val socket = Socket()
            val connectStart = System.currentTimeMillis()
            socket.connect(InetSocketAddress(addr, config.port), config.timeoutMs)
            val connectTime = System.currentTimeMillis() - connectStart
            socket.close()

            return SinglePingProbe(
                sequence = seq,
                dnsTimeMs = dnsTime,
                connectTimeMs = connectTime,
                totalTimeMs = System.currentTimeMillis() - start,
                isSuccess = true
            )
        } catch (e: java.net.SocketTimeoutException) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "TIMEOUT")
        } catch (e: java.net.ConnectException) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "CONNECTION REFUSED")
        } catch (e: java.net.UnknownHostException) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "DNS FAILURE")
        } catch (e: Exception) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = e.message?.take(50) ?: "ERROR")
        }
    }

    private fun doIcmpProbe(seq: Int, config: PingConfiguration): SinglePingProbe {
        val start = System.currentTimeMillis()
        try {
            val process = ProcessBuilder("ping", "-c", "1", "-W", (config.timeoutMs / 1000).coerceAtLeast(1).toString(), config.target)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            val totalTime = System.currentTimeMillis() - start

            if (exitCode == 0) {
                // Parse time from output: time=XX.X ms
                val timeRegex = "time=([\\d.]+)".toRegex()
                val matchResult = timeRegex.find(output)
                val pingTime = matchResult?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: totalTime

                return SinglePingProbe(
                    sequence = seq,
                    totalTimeMs = pingTime,
                    isSuccess = true
                )
            } else {
                return SinglePingProbe(seq, totalTimeMs = totalTime, isSuccess = false, failureReason = "HOST UNREACHABLE")
            }
        } catch (e: Exception) {
            return SinglePingProbe(seq, totalTimeMs = System.currentTimeMillis() - start, isSuccess = false, failureReason = "ICMP RESTRICTED: ${e.message?.take(30)}")
        }
    }

    private fun buildReport(config: PingConfiguration, probes: List<SinglePingProbe>): PingReport {
        val successful = probes.filter { it.isSuccess }
        val received = successful.size
        val loss = if (probes.isEmpty()) 0f else ((probes.size - received).toFloat() / probes.size * 100f)
        val latencies = successful.map { it.totalTimeMs }

        return PingReport(
            config = config,
            probes = probes,
            transmitted = probes.size,
            received = received,
            lossPercent = loss,
            minMs = latencies.minOrNull() ?: 0,
            avgMs = if (latencies.isEmpty()) 0.0 else latencies.average(),
            maxMs = latencies.maxOrNull() ?: 0
        )
    }
}
