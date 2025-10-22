package com.jordankurtz.piawaremobile.map

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jordankurtz.piawaremobile.UrlHandler
import com.jordankurtz.piawaremobile.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.GetReceiverLocationUseCase
import com.jordankurtz.piawaremobile.aircraft.usecase.LoadAircraftTypesUseCase
import com.jordankurtz.piawaremobile.location.LocationService
import com.jordankurtz.piawaremobile.location.LocationState
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScroll
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import piawaremobile.composeapp.generated.resources.ic_receiver
import piawaremobile.composeapp.generated.resources.ic_user_location
import piawaremobile.composeapp.generated.resources.user_location_content_description
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val MAX_LEVEL = 16
private const val MIN_LEVEL = 1
private const val X0 = -2.0037508342789248E7
private const val USER_LOCATION_MARKER_ID = "user_location"
private val mapSize = mapSizeAtLevel(MAX_LEVEL, tileSize = 256)
private val dateFormatter = LocalDateTime.Format {
    year()
    monthNumber()
    dayOfMonth()
}

@OptIn(FlowPreview::class)
class MapViewModel(
    private val mapProvider: TileStreamProvider,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val urlHandler: UrlHandler,
    private val locationService: LocationService,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase,
    private val loadAircraftTypesUseCase: LoadAircraftTypesUseCase,
    private val getAircraftWithDetailsUseCase: GetAircraftWithDetailsUseCase,
    private val getReceiverLocationUseCase: GetReceiverLocationUseCase,
) : ViewModel() {

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val numberOfPlanes: StateFlow<Int>
        get() = _numberOfPlanes
    private val _numberOfPlanes = MutableStateFlow(0)

    private var pollingJob: Job? = null
    private var saveStateJob: Job? = null

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 16) {
        minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
    }.apply {
        addLayer(mapProvider)

        onMarkerClick { id, _, _ ->
            if (!id.startsWith("fake")) {
                openFlightPage(id)
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadSettingsUseCase().collect {
                if (it is Async.Success) {
                    onSettingsLoaded(it.data)
                }
            }
        }

        currentLocation.onEach {
            it?.let(::updateUserLocationMarker)
        }.launchIn(viewModelScope)
    }

    private fun onSettingsLoaded(settings: Settings) {
        pollingJob?.cancel()
        saveStateJob?.cancel()

        if (settings.showReceiverLocations) {
            loadReceiverLocations(settings.servers)
        }

        pollingJob = viewModelScope.launch {
            pollServers(
                settings.servers.map { it.address },
                settings.refreshInterval
            )
        }

        if (settings.restoreMapStateOnStart) {
            saveStateJob = viewModelScope.launch {
                val savedState = getSavedMapStateUseCase()
                println("Restored map state $savedState")
                state.setScroll(savedState.scrollX, savedState.scrollY)
                state.scale = savedState.zoom

                if (settings.centerMapOnUserOnStart) {
                    requestLocationPermission()
                }

                snapshotFlow { Pair(state.scroll, state.scale) }
                    .debounce(500.milliseconds)
                    .onEach { (scroll, scale) ->
                        if (scroll.x > 0.0 && scroll.y > 0.0) {
                            saveMapStateUseCase(scroll.x, scroll.y, scale)
                            println("Saved map state $scroll, $scale")
                        }
                    }.launchIn(this)
            }
        } else if (settings.centerMapOnUserOnStart) {
            requestLocationPermission()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadReceiverLocations(servers: List<Server>) {
        viewModelScope.launch {
            val locations =
                servers.map { server -> async { server to getReceiverLocationUseCase(server.address) } }
                    .awaitAll().filter { it.second != null }
                    .toMap() as Map<Server, Location> // we already filtered out the nulls but type checking doesn't know that

            locations.forEach(::addReceiverToMap)
        }
    }

    private fun addReceiverToMap(receiver: Map.Entry<Server, Location>) = viewModelScope.launch {
        val (x, y) = receiver.value.projected
        state.addMarker(
            receiver.key.id.toString(), x, y
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_receiver),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }

    private suspend fun pollServers(servers: List<String>, refreshInterval: Int) {
        if (servers.isEmpty()) return

        loadAircraftTypesUseCase(servers)
        val infoHost = servers.first()

        val previousMarkerIds = mutableSetOf<String>()

        while (coroutineContext.isActive) {
            println("Refreshing")
            val aircraft = getAircraftWithDetailsUseCase(servers, infoHost)

            _numberOfPlanes.value = aircraft.count()

            previousMarkerIds.forEach(state::removeMarker)
            previousMarkerIds.clear()

            aircraft.forEach { (plane, _) ->
                val location = doProjection(plane.lat, plane.lon)

                state.addMarker(
                    (plane.flight ?: "fake=${plane.hex}").also { previousMarkerIds.add(it) },
                    location.first,
                    location.second
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp)
                            .rotate(plane.track),
                        colorFilter = ColorFilter.tint(getColorForAltitude(plane.altitude))
                    )
                }
            }

            delay(refreshInterval.seconds.inWholeMilliseconds)
        }
    }

    fun requestLocationPermission() {
        _locationState.value = LocationState.RequestingPermission
        locationService.requestPermissions { granted ->
            if (granted) {
                _locationState.value = LocationState.PermissionGranted
                startLocationUpdates(::recenterOnLocation)
            } else {
                _locationState.value = LocationState.PermissionDenied
            }
        }
    }

    fun startLocationUpdates(onFirstLocation: ((Location) -> Unit)? = null) {
        _locationState.value = LocationState.TrackingLocation
        var isFirstUpdate = true
        locationService.startLocationUpdates { location ->
            _currentLocation.value = location
            if (isFirstUpdate) {
                onFirstLocation?.invoke(location)
                isFirstUpdate = false
            }
        }
    }

    fun stopLocationUpdates() {
        locationService.stopLocationUpdates()
        _locationState.value = LocationState.Idle
        _currentLocation.value = null
        state.removeMarker(USER_LOCATION_MARKER_ID)
    }

    fun recenterOnLocation(location: Location) {
        viewModelScope.launch {
            val (x, y) = location.projected
            println("Scrolling map to $x, $y")
            state.scrollTo(x, y)
        }
    }

    private fun updateUserLocationMarker(location: Location) {
        val (x, y) = location.projected
        state.removeMarker(USER_LOCATION_MARKER_ID)
        state.addMarker(
            id = USER_LOCATION_MARKER_ID,
            x = x,
            y = y
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_user_location),
                contentDescription = stringResource(Res.string.user_location_content_description),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    val Location.projected: Pair<Double, Double>
        get() = doProjection(latitude, longitude)

    private fun doProjection(latitude: Double, longitude: Double): Pair<Double, Double> {
        if (abs(latitude) > 90 || abs(longitude) > 180) {
            error("Invalid latitude or longitude")
        }

        val num = longitude * 0.017453292519943295 // 2*pi / 360
        val a = latitude * 0.017453292519943295

        val x = normalize(6378137.0 * num, min = X0, max = -X0)
        val y = normalize(3189068.5 * ln((1.0 + sin(a)) / (1.0 - sin(a))), min = -X0, max = X0)

        return Pair(x, y)
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    private fun openFlightPage(flight: String) {
        val dateString = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        urlHandler.openUrl(
            "https://www.flightaware.com/live/flight/$flight/history/${
                dateFormatter.format(
                    dateString
                )
            }"
        )
    }

    private fun getColorForAltitude(altitude: String): Color {
        return when (altitude.toIntOrNull() ?: 0) {
            in 0..250 -> Color(255, 64, 0)
            in 251..500 -> Color(255, 128, 0)
            in 501..750 -> Color(255, 160, 0)
            in 751..1000 -> Color(255, 192, 0)
            in 1001..1500 -> Color(255, 224, 0)
            in 1501..2000 -> Color(255, 255, 0)
            in 2001..3000 -> Color(192, 255, 0)
            in 3001..4000 -> Color(128, 255, 0)
            in 4001..5000 -> Color(64, 255, 0)
            in 5001..6000 -> Color(0, 255, 64)
            in 6001..7000 -> Color(0, 255, 128)
            in 7001..8000 -> Color(0, 255, 192)
            in 8001..9000 -> Color(0, 255, 224)
            in 9001..10000 -> Color(0, 255, 255)
            in 10001..15000 -> Color(0, 224, 255)
            in 15001..20000 -> Color(0, 192, 255)
            in 20001..25000 -> Color(0, 160, 255)
            in 25001..30000 -> Color(0, 128, 255)
            in 30001..35000 -> Color(0, 64, 255)
            in 35001..40000 -> Color(0, 0, 255)
            in 40001..45000 -> Color(64, 0, 255)
            in 45001..50000 -> Color(128, 0, 255)
            else -> Color(192, 0, 255)
        }
    }
}

private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}
