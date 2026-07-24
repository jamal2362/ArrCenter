package com.jamal2367.arrcenter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jamal2367.arrcenter.data.AppSettings
import com.jamal2367.arrcenter.data.ServiceEndpoints
import com.jamal2367.arrcenter.data.SettingsRepository
import com.jamal2367.arrcenter.data.ThemeMode
import com.jamal2367.arrcenter.model.ServiceType
import com.jamal2367.arrcenter.net.EndpointResult
import com.jamal2367.arrcenter.net.ProbeResult
import com.jamal2367.arrcenter.net.normalizeUrl
import com.jamal2367.arrcenter.net.probeEndpoint
import com.jamal2367.arrcenter.net.resolveEndpoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the service screen should currently show. */
sealed interface ServiceState {
    data object Idle : ServiceState
    data object Resolving : ServiceState
    data class Ready(val url: String, val usedFallback: Boolean) : ServiceState
    data object NotConfigured : ServiceState

    /** @param reason the underlying network error, so the screen can say what went wrong. */
    data class Unreachable(val reason: String?) : ServiceState
}

/** Outcome of the manual connection test in the settings. */
sealed interface ConnectionTest {
    data object Running : ConnectionTest
    data class Ok(val usedFallback: Boolean) : ConnectionTest
    data class Failed(val reason: String?) : ConnectionTest
    data object Empty : ConnectionTest
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _serviceStates = MutableStateFlow<Map<ServiceType, ServiceState>>(emptyMap())
    val serviceStates: StateFlow<Map<ServiceType, ServiceState>> = _serviceStates.asStateFlow()

    private val _connectionTests = MutableStateFlow<Map<ServiceType, ConnectionTest>>(emptyMap())
    val connectionTests: StateFlow<Map<ServiceType, ConnectionTest>> = _connectionTests.asStateFlow()

    private val resolveJobs = mutableMapOf<ServiceType, Job>()

    /** Which addresses the current state of a service was resolved for. */
    private val resolvedFor = mutableMapOf<ServiceType, ServiceEndpoints>()

    fun stateOf(type: ServiceType): ServiceState = _serviceStates.value[type] ?: ServiceState.Idle

    /**
     * Resolves the address for [type].
     *
     * Keyed on [endpoints]: changing an address in the settings re-resolves automatically,
     * without relying on the state being cleared before the new settings arrive.
     */
    fun resolve(type: ServiceType, endpoints: ServiceEndpoints, force: Boolean = false) {
        val unchanged = resolvedFor[type] == endpoints
        if (!force && unchanged && stateOf(type) !is ServiceState.Idle) return
        if (!force && unchanged && resolveJobs[type]?.isActive == true) return

        resolveJobs[type]?.cancel()
        resolvedFor[type] = endpoints
        resolveJobs[type] = viewModelScope.launch {
            setState(type, ServiceState.Resolving)
            setState(
                type,
                when (val result = resolveEndpoint(endpoints)) {
                    is EndpointResult.Reachable ->
                        ServiceState.Ready(result.url, result.usedFallback)
                    EndpointResult.NotConfigured -> ServiceState.NotConfigured
                    is EndpointResult.Unreachable -> ServiceState.Unreachable(result.reason)
                },
            )
        }
    }

    fun testConnection(type: ServiceType, endpoints: ServiceEndpoints) {
        viewModelScope.launch {
            val primary = normalizeUrl(endpoints.primary)
            val secondary = normalizeUrl(endpoints.secondary)
            if (primary == null && secondary == null) {
                setTest(type, ConnectionTest.Empty)
                return@launch
            }

            setTest(type, ConnectionTest.Running)

            val primaryResult = primary?.let { probeEndpoint(it) }
            if (primaryResult is ProbeResult.Ok) {
                setTest(type, ConnectionTest.Ok(usedFallback = false))
                return@launch
            }

            val secondaryResult = secondary?.let { probeEndpoint(it) }
            if (secondaryResult is ProbeResult.Ok) {
                setTest(type, ConnectionTest.Ok(usedFallback = true))
                return@launch
            }

            setTest(
                type,
                ConnectionTest.Failed(
                    (primaryResult as? ProbeResult.Failed)?.reason
                        ?: (secondaryResult as? ProbeResult.Failed)?.reason
                ),
            )
        }
    }

    fun clearConnectionTests() {
        _connectionTests.value = emptyMap()
    }

    fun saveEndpoints(endpoints: Map<ServiceType, ServiceEndpoints>, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveEndpoints(endpoints)
            onSaved()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.saveThemeMode(mode) }
    }

    fun setStartService(type: ServiceType) {
        viewModelScope.launch { repository.saveStartService(type) }
    }

    private fun setState(type: ServiceType, state: ServiceState) {
        _serviceStates.update { it + (type to state) }
    }

    private fun setTest(type: ServiceType, test: ConnectionTest) {
        _connectionTests.update { it + (type to test) }
    }
}
