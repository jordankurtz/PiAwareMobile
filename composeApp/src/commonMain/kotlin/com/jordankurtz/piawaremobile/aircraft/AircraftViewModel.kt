package com.jordankurtz.piawaremobile.aircraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import kotlin.time.Duration.Companion.seconds

@Factory
class AircraftViewModel(
    private val loadAircraftTypesUseCase: LoadAircraftTypesUseCase,
    private val getAircraftWithDetailsUseCase: GetAircraftWithDetailsUseCase,
    private val getReceiverLocationUseCase: GetReceiverLocationUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
) : ViewModel() {

    private val _aircraft = MutableStateFlow<List<Pair<Aircraft, AircraftInfo?>>>(emptyList())
    val aircraft: StateFlow<List<Pair<Aircraft, AircraftInfo?>>> = _aircraft.asStateFlow()

    private val _receiverLocations = MutableStateFlow<Map<Server, Location>>(emptyMap())
    val receiverLocations: StateFlow<Map<Server, Location>> = _receiverLocations.asStateFlow()

    val numberOfPlanes: StateFlow<Int>
        get() = _numberOfPlanes
    private val _numberOfPlanes = MutableStateFlow(0)

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSettingsUseCase().collect {
                if (it is Async.Success) {
                    val settings = it.data
                    if (settings.showReceiverLocations) {
                        loadReceiverLocations(settings.servers)
                    }
                    startPolling(settings.servers, settings.refreshInterval)
                }
            }
        }
    }

    private fun startPolling(servers: List<Server>, refreshInterval: Int) {
        pollingJob?.cancel()
        pollingJob = pollServers(
            servers = servers.map { it.address },
            refreshInterval = refreshInterval
        )
    }

    fun stopPolling() {
        pollingJob?.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadReceiverLocations(servers: List<Server>) {
        viewModelScope.launch {
            val locations =
                servers.map { server -> async { server to getReceiverLocationUseCase(server.address) } }
                    .awaitAll().filter { it.second != null }
                    .toMap() as Map<Server, Location> // we already filtered out the nulls but type checking doesn't know that

            _receiverLocations.value = locations
        }
    }

    private fun pollServers(servers: List<String>, refreshInterval: Int): Job? {
        if (servers.isEmpty() || refreshInterval <= 0) return null

        val infoHost = servers.first()

        return flow {
            loadAircraftTypesUseCase(servers)
            while (true) {
                emit(Unit)
                delay(refreshInterval.seconds)
            }
        }.onEach {
            println("Refreshing")
            val aircraftList = getAircraftWithDetailsUseCase(servers, infoHost)

            _numberOfPlanes.value = aircraftList.count()
            _aircraft.value = aircraftList

        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }
}
