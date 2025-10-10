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
import com.jordankurtz.piawaremobile.api.PiAwareApi
import com.jordankurtz.piawaremobile.location.Location
import com.jordankurtz.piawaremobile.location.LocationService
import com.jordankurtz.piawaremobile.location.LocationState
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.compose.resources.painterResource
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.api.removeAllMarkers
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.api.scroll
import ovh.plrapps.mapcompose.api.scrollTo
import ovh.plrapps.mapcompose.api.setScroll
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import piawaremobile.composeapp.generated.resources.Res
import piawaremobile.composeapp.generated.resources.ic_plane
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val MAX_LEVEL = 16
private const val MIN_LEVEL = 1
private const val X0 = -2.0037508342789248E7
private val mapSize = mapSizeAtLevel(MAX_LEVEL, tileSize = 256)
private val dateFormatter = LocalDateTime.Format {
    year()
    monthNumber()
    dayOfMonth()
}

@OptIn(FlowPreview::class)
class MapViewModel(
    private val piAwareApi: PiAwareApi,
    private val mapProvider: TileStreamProvider,
    private val loadSettingsUseCase: LoadSettingsUseCase,
    private val urlHandler: UrlHandler,
    private val locationService: LocationService,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase
) : ViewModel() {

    private lateinit var aircraftTypes: Map<String, ICAOAircraftType>

    private val aircraftInfoCache = mutableMapOf<String, AircraftInfo>()

    // Merged from LocationViewModel
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val numberOfPlanes: StateFlow<Int>
        get() = _numberOfPlanes
    private val _numberOfPlanes = MutableStateFlow(0)

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 16) {
        minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
    }.apply {
        addLayer(mapProvider)

        onMarkerClick { id, x, y ->
            if (!id.startsWith("fake")) {
                openFlightPage(id)
            }
        }
    }


    init {
        viewModelScope.launch {
            val savedState = getSavedMapStateUseCase()
            println("Restored map state $savedState")
            state.setScroll(savedState.scrollX, savedState.scrollY)
            state.scale = savedState.zoom

            // wait to center the map on the user's location until after we restore state
            requestLocationPermission()

            snapshotFlow { Pair(state.scroll, state.scale) }
                .debounce(500.milliseconds)
                .onEach { (scroll, scale) ->
                    // This check prevents saving the initial state (0.0, 0.0) before the map is ready
                    if (scroll.x > 0.0 && scroll.y > 0.0) {
                        saveMapStateUseCase(scroll.x, scroll.y, scale)
                        println("Saved map state $scroll, $scale")
                    }
                }.launchIn(viewModelScope)
        }

        viewModelScope.launch(Dispatchers.IO) {
            loadSettingsUseCase().collect {
                val result = it
                when (result) {
                    is Async.Success -> pollServers(
                        result.data.servers.map { it.address },
                        result.data.refreshInterval
                    )

                    else -> {}
                }
            }

        }
    }

    private suspend fun pollServers(servers: List<String>, refreshInterval: Int) {
        aircraftTypes = loadAircraftTypes(servers)
        while (true) {
            println("Refreshing")
            val aircraft = servers.map { processAircraft(it) }.flatten()

            _numberOfPlanes.value = aircraft.count()

            state.removeAllMarkers()

            aircraft.forEach {
                val location = doProjection(it.first.lat, it.first.lon)

                state.addMarker(
                    it.first.flight ?: "fake=${it.first.hex}", location.first, location.second
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_plane),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp).rotate(it.first.track),
                        colorFilter = ColorFilter.tint(getColorForAltitude(it.first.altitude))
                    )
                }
            }

            delay(refreshInterval.seconds.inWholeMilliseconds)
        }
    }

    private suspend fun loadAircraftTypes(servers: List<String>) =
        servers.map { piAwareApi.getAircraftTypes(it) }.flatten()

    private suspend fun processAircraft(server: String): List<Pair<Aircraft, AircraftInfo?>> {
        return coroutineScope {
            return@coroutineScope piAwareApi.getAircraft(server).map {
                async {
                    Pair(it, getAircraftInfo(server, it.hex.uppercase()))
                }
            }.awaitAll()
        }
    }

    private suspend fun getAircraftInfo(host: String, hex: String): AircraftInfo? {
        if (aircraftInfoCache.containsKey(hex)) return aircraftInfoCache[hex]!!

        return lookupAircraftInfo(host, hex.replace("~", ""))
    }

    private suspend fun lookupAircraftInfo(
        host: String,
        hex: String,
        level: Int = 1
    ): AircraftInfo? {
        val bkey = hex.substring(0, level)
        val dkey = hex.substring(level)

        val data = piAwareApi.getAircraftInfo(host, bkey) ?: return null

        if (data.containsKey(dkey)) {
            val info = data[dkey]!!
            val icaoAircraftType = lookAircraftType(info)
            return AircraftInfo(
                registration = info.jsonObject["i"]?.toString(),
                icaoType = info.jsonObject["t"]?.toString(),
                typeDescription = icaoAircraftType?.desc,
                wtc = icaoAircraftType?.wtc
            )
        }

        if (data.containsKey("children")) {
            val subkey = bkey + dkey.substring(0, 1)
            if (data["children"]?.let { Json.decodeFromJsonElement<List<String>>(it) }
                    ?.contains(subkey) == true) {
                return lookupAircraftInfo(host, hex, level + 1)
            }
        }

        return null
    }

    private fun lookAircraftType(info: JsonElement): ICAOAircraftType? {
        return info.let { it.jsonObject["t"]?.toString() }
            ?.let { aircraftTypes[it.uppercase()] }
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
    }

    fun recenterOnLocation(location: Location) {
        viewModelScope.launch {
            val (x, y) = doProjection(location.latitude, location.longitude)
            println("Scrolling map to $x, $y")
            state.scrollTo(x, y)
        }
    }

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
        urlHandler.openUrl(getFlightAwareUrl(flight))
    }

    private fun getFlightAwareUrl(flight: String): String {
        return "https://www.flightaware.com/live/flight/$flight/history/${dateFormatter}.format(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))"
    }

    private fun getColorForAltitude(altitude: String): Color {
        return when (altitude.toIntOrNull() ?: 0) {
            in 0..250 -> Color(255, 64, 0)   // Dark Orange
            in 251..500 -> Color(255, 128, 0)   // Orange
            in 501..750 -> Color(255, 160, 0)   // Light Orange
            in 751..1000 -> Color(255, 192, 0)  // Yellowish Orange
            in 1001..1500 -> Color(255, 224, 0) // Light Yellow Orange
            in 1501..2000 -> Color(255, 255, 0) // Yellow
            in 2001..3000 -> Color(192, 255, 0) // Yellow Green
            in 3001..4000 -> Color(128, 255, 0) // Greenish Yellow
            in 4001..5000 -> Color(64, 255, 0)  // Light Green
            in 5001..6000 -> Color(0, 255, 64)  // Slightly Darker Green
            in 6001..7000 -> Color(0, 255, 128) // Light Green
            in 7001..8000 -> Color(0, 255, 192) // Aqua Green
            in 8001..9000 -> Color(0, 255, 224) // Light Cyan Green
            in 9001..10000 -> Color(0, 255, 255) // Cyan
            in 10001..15000 -> Color(0, 224, 255) // Light Blue Cyan
            in 15001..20000 -> Color(0, 192, 255) // Blueish Cyan
            in 20001..25000 -> Color(0, 160, 255) // Lighter Blue
            in 25001..30000 -> Color(0, 128, 255) // Blue
            in 30001..35000 -> Color(0, 64, 255)  // Dark Blue
            in 35001..40000 -> Color(0, 0, 255)  // Deep Blue
            in 40001..45000 -> Color(64, 0, 255) // Dark Violet Blue
            in 45001..50000 -> Color(128, 0, 255) // Violet
            else -> Color(192, 0, 255) // Brighter Violet for 50,000+
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}

private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}

fun <K, V> List<Map<K, V>>.flatten(): Map<K, V> {
    val result = mutableMapOf<K, V>()
    forEach { result.putAll(it) }
    return result
}
