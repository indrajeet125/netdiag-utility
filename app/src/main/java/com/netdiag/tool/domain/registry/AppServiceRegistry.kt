package com.netdiag.tool.domain.registry

import com.netdiag.tool.domain.model.AppService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry Pattern for App Diagnostic Services.
 *
 * Implements Open/Closed Principle:
 * New diagnostic features can register themselves with the service bus
 * without altering UI or navigation logic.
 */
@Singleton
class AppServiceRegistry @Inject constructor() {

    private val services = mutableListOf<AppService>()

    init {
        // Initialize default diagnostic suite
        registerDefaults()
    }

    private fun registerDefaults() {
        services.addAll(AppService.ALL_SERVICES)
    }

    fun getAllServices(): List<AppService> = services.toList()

    fun getServiceById(id: String): AppService? = services.find { it.id == id }

    fun getServicesByCategory(category: String): List<AppService> =
        services.filter { it.category.equals(category, ignoreCase = true) }

    fun searchServices(query: String): List<AppService> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return services.toList()
        return services.filter { service ->
            service.title.lowercase().contains(q) ||
            service.description.lowercase().contains(q) ||
            service.keywords.any { keyword -> keyword.lowercase().contains(q) } ||
            service.category.lowercase().contains(q)
        }
    }

    fun registerService(service: AppService) {
        if (services.none { it.id == service.id }) {
            services.add(service)
        }
    }
}
