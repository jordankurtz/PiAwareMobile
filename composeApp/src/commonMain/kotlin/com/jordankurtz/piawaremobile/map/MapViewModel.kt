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
import com.jordankurtz.piawaremobile.map.usecase.GetSavedMapStateUseCase
import com.jordankurtz.piawaremobile.map.usecase.SaveMapStateUseCase
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Location
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.usecase.LoadSettingsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.core.annotation.Factory
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
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_LEVEL = 16
private const val MIN_LEVEL = 1
private const val X0 = -2.0037508342789248E7
private const val USER_LOCATION_MARKER_ID = "user_location"
private val mapSize = mapSizeAtLevel(MAX_LEVEL, tileSize = 256)

@OptIn(FlowPreview::class)
@Factory
class MapViewModel(
    private val mapProvider: TileStreamProvider,
    private val getSavedMapStateUseCase: GetSavedMapStateUseCase,
    private val saveMapStateUseCase: SaveMapStateUseCase,
    private val loadSettingsUseCase: LoadSettingsUseCase,
) : ViewModel() {

    private var saveStateJob: Job? = null
    private var settings: Settings? = null
    private val previousAircraftMarkerIds = mutableSetOf<String>()

    private val _selectedAircraft = MutableStateFlow<String?>(null)
    val selectedAircraft: StateFlow<String?> = _selectedAircraft

    val state = MapState(levelCount = MAX_LEVEL + 1, mapSize, mapSize, workerCount = 16) {
        minimumScaleMode(Forced((1 / 2.0.pow(MAX_LEVEL - MIN_LEVEL))))
    }.apply {
        addLayer(mapProvider)

        onMarkerClick { id, _, _ ->
                if (previousAircraftMarkerIds.contains(id)) {
                    if (_selectedAircraft.value == id) {
                        _selectedAircraft.value = null
                    } else {
                        _selectedAircraft.value = id
                    }
                }
        }
    }

    init {
        viewModelScope.launch {
            loadSettingsUseCase().collect {
                if (it is Async.Success) {
                    settings = it.data
                    onSettingsLoaded(it.data)
                }
            }
        }
    }

    private suspend fun onSettingsLoaded(settings: Settings) {
        saveStateJob?.cancel()
        if (settings.restoreMapStateOnStart) {
            loadMapState()
            startSaveMapStateJob()
        }
    }

    private suspend fun loadMapState() {
        val savedState = getSavedMapStateUseCase()
        println("Restored map state $savedState")
        state.setScroll(savedState.scrollX, savedState.scrollY)
        state.scale = savedState.zoom
    }

    private fun startSaveMapStateJob() {
        saveStateJob = viewModelScope.launch {
            snapshotFlow { Pair(state.scroll, state.scale) }
                .debounce(500.milliseconds)
                .onEach { (scroll, scale) ->
                    if (scroll.x > 0.0 && scroll.y > 0.0) {
                        saveMapStateUseCase(scroll.x, scroll.y, scale)
                        println("Saved map state $scroll, $scale")
                    }
                }.launchIn(this)
        }
    }

    fun onReceiverLocation(receiver: Map.Entry<Server, Location>) = viewModelScope.launch {
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

    fun recenterOnLocation(location: Location) {
        viewModelScope.launch {
            val (x, y) = location.projected
            println("Scrolling map to $x, $y")
            state.scrollTo(x, y)
        }
    }

    fun onUserLocationChanged(location: Location) {
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

    fun onAircraftUpdated(aircraft: List<Pair<Aircraft, Any?>>) {
        previousAircraftMarkerIds.forEach(state::removeMarker)
        previousAircraftMarkerIds.clear()

        aircraft.forEach { (plane, _) ->
            val location = doProjection(plane.lat, plane.lon)

            state.addMarker(
                plane.hex.also { previousAircraftMarkerIds.add(it) },
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
            in 2001..25000 -> Color(0, 160, 255)
            in 25001..30000 -> Color(0, 128, 255)
            in 30001..35000 -> Color(0, 64, 255)
            in 35001..40000 -> Color(0, 0, 255)
            in 40001..45000 -> Color(64, 0, 255)
            in 45001..50000 -> Color(128, 0, 255)
            else -> Color(192, 0, 255)
        }
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


private fun mapSizeAtLevel(wmtsLevel: Int, tileSize: Int): Int {
    return tileSize * 2.0.pow(wmtsLevel).toInt()
}
