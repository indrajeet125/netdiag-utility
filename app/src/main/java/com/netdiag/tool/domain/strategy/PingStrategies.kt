package com.netdiag.tool.domain.strategy

import com.netdiag.tool.data.repository.PingRepository
import com.netdiag.tool.domain.model.PingConfiguration
import com.netdiag.tool.domain.model.PingType
import com.netdiag.tool.domain.model.SinglePingProbe
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete Strategy: HTTP / TLS Probe Strategy
 */
class HttpPingStrategy(
    private val pingRepository: PingRepository
) : DiagnosticStrategy<PingConfiguration, SinglePingProbe> {
    override val name: String = "HTTP / TLS Handshake Strategy"
    override val description: String = "Measures DNS, TLS establishment, TTFB, and HTTP response latency"

    override suspend fun execute(configuration: PingConfiguration): Flow<SinglePingProbe> {
        return pingRepository.startPing(configuration.copy(type = PingType.HTTP))
    }
}

/**
 * Concrete Strategy: TCP Socket Connect Strategy
 */
class TcpPingStrategy(
    private val pingRepository: PingRepository
) : DiagnosticStrategy<PingConfiguration, SinglePingProbe> {
    override val name: String = "TCP Raw Socket Strategy"
    override val description: String = "Performs raw TCP SYN/ACK socket handshakes to target port"

    override suspend fun execute(configuration: PingConfiguration): Flow<SinglePingProbe> {
        return pingRepository.startPing(configuration.copy(type = PingType.TCP))
    }
}

/**
 * Concrete Strategy: ICMP Echo Probe Strategy
 */
class IcmpPingStrategy(
    private val pingRepository: PingRepository
) : DiagnosticStrategy<PingConfiguration, SinglePingProbe> {
    override val name: String = "ICMP Echo Strategy"
    override val description: String = "Low-level ICMP ping probe fallback"

    override suspend fun execute(configuration: PingConfiguration): Flow<SinglePingProbe> {
        return pingRepository.startPing(configuration.copy(type = PingType.ICMP))
    }
}

/**
 * Factory Pattern: Diagnostic Strategy Factory
 * Resolves the appropriate strategy instance dynamically.
 */
@Singleton
class PingStrategyFactory @Inject constructor(
    private val pingRepository: PingRepository
) {
    private val httpStrategy = HttpPingStrategy(pingRepository)
    private val tcpStrategy = TcpPingStrategy(pingRepository)
    private val icmpStrategy = IcmpPingStrategy(pingRepository)

    fun getStrategy(type: PingType): DiagnosticStrategy<PingConfiguration, SinglePingProbe> {
        return when (type) {
            PingType.HTTP -> httpStrategy
            PingType.TCP -> tcpStrategy
            PingType.ICMP -> icmpStrategy
        }
    }
}
