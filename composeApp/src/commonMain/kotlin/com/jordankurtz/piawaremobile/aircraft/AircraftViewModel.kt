package com.jordankurtz.piawaremobile.aircraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LookupFlightUseCase
import com.jordankurtz.piawaremobile.di.annotations.MainDispatcher
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Factory
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds


private val dateFormatter = LocalDateTime.Format {
    year()
    monthNumber()
    day()
}

@Factory
class AircraftViewModel(
    private val loadAircraftTypesUseCase: LoadAircraftTypesUseCase,
    private val getAircraftWithDetailsUseCase: GetAircraftWithDetailsUseCase,
    private val getReceiverLocationUseCase: GetReceiverLocationUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val lookupFlightUseCase: LookupFlightUseCase,
    private val urlHandler: UrlHandler,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher,
) : ViewModel() {

    var settings: Settings? = null

    private val _aircraft = MutableStateFlow<List<Pair<Aircraft, AircraftInfo?>>>(emptyList())
    val aircraft: StateFlow<List<Pair<Aircraft, AircraftInfo?>>> = _aircraft.asStateFlow()

    private val _flightDetails = MutableStateFlow<Async<Flight>>(Async.NotStarted)
    val flightDetails: StateFlow<Async<Flight>> = _flightDetails.asStateFlow()

    private val _receiverLocations = MutableStateFlow<Map<Server, Location>>(emptyMap())
    val receiverLocations: StateFlow<Map<Server, Location>> = _receiverLocations.asStateFlow()

    private val _numberOfPlanes = MutableStateFlow(0)
    val numberOfPlanes: StateFlow<Int> = _numberOfPlanes.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSettingsUseCase().collect {
                when (it) {
                    is Async.Success -> {
                        with(it.data) {
                            settings = this
                            if (showReceiverLocations) {
                                loadReceiverLocations(servers)
                            }
                            startPolling(servers, refreshInterval)
                        }
                    }
                    is Async.Error -> {
                        Logger.e("Failed to load settings", it.throwable)
                    }
                    else -> {
                        // No-op
                    }
                }
            }
        }
    }

    private fun lookupFlight(flight: String) {
        viewModelScope.launch {
            _flightDetails.value = Async.Loading
            _flightDetails.value = lookupFlightUseCase(flight)
        }
    }

    private fun resetLookup() {
        _flightDetails.value = Async.NotStarted
    }

    fun onFlightDetailsDismissed() {
        resetLookup()
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
            Logger.d("Refreshing")
            val aircraftList = getAircraftWithDetailsUseCase(servers, infoHost)

            _numberOfPlanes.value = aircraftList.count()
            _aircraft.value = aircraftList

        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    fun openFlightInformation(selectedAircraft: String?) {
        if (selectedAircraft == null) {
            resetLookup()
            return
        }
        val aircraft = _aircraft.value.firstOrNull { it.first.hex == selectedAircraft }?.first
        if (aircraft?.flight.isNullOrBlank()) {
            _flightDetails.value = Async.Error("Flight information not available for this aircraft.")
            return
        }
        if (settings?.enableFlightAwareApi == true && settings?.flightAwareApiKey?.isNotEmpty() == true) {
            lookupFlight(aircraft.flight)
        } else {
            openFlightPage(aircraft.flight)
        }
    }

    private fun openFlightPage(flight: String) {
        //todo add toast that we can't look it up
        viewModelScope.launch(mainDispatcher) {
            val dateString = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val url = "https://www.flightaware.com/live/flight/$flight/history/${
                dateFormatter.format(
                    dateString
                )
            }"

            if (settings?.openUrlsExternally == true) {
                urlHandler.openUrlExternally(url)
            } else {
                urlHandler.openUrlInternally(url)
            }
        }
    }
}
