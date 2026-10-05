package com.netdiag.tool.domain.strategy

import kotlinx.coroutines.flow.Flow

/**
 * Strategy Pattern Contract for Network Diagnostics.
 *
 * Adheres to:
 * - Single Responsibility: Each strategy encapsulates one diagnostic protocol.
 * - Open/Closed: New diagnostic algorithms (DNS, Traceroute, Jitter) can be added without altering existing runners.
 * - Liskov Substitution: Any strategy can be plugged into diagnostic orchestrators.
 * - Interface Segregation: Clean, minimal contract returning reactive flows.
 * - Dependency Inversion: Diagnostic coordinators depend on this abstraction.
 */
interface DiagnosticStrategy<Config, ProbeResult> {
    val name: String
    val description: String
    suspend fun execute(configuration: Config): Flow<ProbeResult>
}
