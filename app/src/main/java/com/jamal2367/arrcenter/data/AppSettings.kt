package com.jamal2367.arrcenter.data

import com.jamal2367.arrcenter.model.ServiceType

/** The two addresses configured for one service. */
data class ServiceEndpoints(
    val primary: String = "",
    val secondary: String = "",
) {
    val isConfigured: Boolean get() = primary.isNotBlank() || secondary.isNotBlank()
}

enum class ThemeMode(val id: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: System
    }
}

data class AppSettings(
    val endpoints: Map<ServiceType, ServiceEndpoints> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.System,
    val startService: ServiceType = ServiceType.Seerr,
) {
    fun endpointsFor(type: ServiceType): ServiceEndpoints =
        endpoints[type] ?: ServiceEndpoints()
}
