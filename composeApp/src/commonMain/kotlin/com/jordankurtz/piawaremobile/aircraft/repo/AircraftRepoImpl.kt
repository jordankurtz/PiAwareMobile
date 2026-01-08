package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.AircraftPosition
import com.jordankurtz.piawaremobile.model.AircraftTrail
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single

@Single(binds = [AircraftRepo::class])
class AircraftRepoImpl(
    private val piAwareApi: PiAwareApi,
    private val aeroApi: AeroApi
) : AircraftRepo {

    private val aircraftInfoCache = mutableMapOf<String, AircraftInfo>()

    private val _aircraftTrails = MutableStateFlow<Map<String, AircraftTrail>>(emptyMap())
    override val aircraftTrails: StateFlow<Map<String, AircraftTrail>> = _aircraftTrails.asStateFlow()

    private val trailPositions = mutableMapOf<String, MutableList<AircraftPosition>>()
    private var currentAircraftHex = setOf<String>()

    private var aircraftTypes: Map<String, ICAOAircraftType>? = null

    override suspend fun getAircraft(servers: List<String>): List<Aircraft> =
        coroutineScope {
            servers.map { server ->
                async {
                    try {
                        piAwareApi.getAircraft(server)
                    } catch (e: Exception) {
                        Logger.e("Failed to fetch aircraft from server $server", e)
                        emptyList()
                    }
                }
            }.awaitAll().flatten().filterNoLocation()
        }

    override suspend fun loadAircraftTypes(servers: List<String>) {
        if (aircraftTypes == null) {
            aircraftTypes = servers.map { piAwareApi.getAircraftTypes(it) }.flatten()
        }
    }

    override suspend fun findAircraftInfo(host: String, hex: String): AircraftInfo? {
        if (aircraftInfoCache.containsKey(hex)) return aircraftInfoCache[hex]

        val info = lookupAircraftInfoRecursive(host, hex.replace("~", ""))
        info?.let { aircraftInfoCache[hex] = it }
        return info
    }

    override suspend fun getReceiverInfo(
        host: String,
        receiverType: ReceiverType
    ): Receiver? {
        return when (receiverType) {
            ReceiverType.DUMP_1090 -> piAwareApi.getDump1090ReceiverInfo(host)
            ReceiverType.DUMP_978 -> piAwareApi.getDump978ReceiverInfo(host)
        }
    }

    override suspend fun lookupFlight(ident: String): Async<FlightResponse> {
        return try {
            val response = aeroApi.getFlight(
                ident = ident
            )
            Async.Success(response)
        } catch (e: Exception) {
            Logger.e("Failed to fetch flight for ident $ident", e)
            Async.Error("Failed to fetch flight for ident $ident", e)
        }
    }

    override suspend fun fetchAndMergeHistory(host: String) {
        val receiver = piAwareApi.getDump1090ReceiverInfo(host) ?: return
        val historyCount = receiver.history ?: return
        if (historyCount <= 0) return

        coroutineScope {
            val historyResults = (0 until historyCount).map { index ->
                async {
                    fetchHistoryWithRetry(host, index)
                }
            }.awaitAll()

            historyResults
                .filterNotNull()
                .flatten()
                .filter { it.lat != 0.0 && it.lon != 0.0 }
                .forEach { aircraft ->
                    val positions = trailPositions.getOrPut(aircraft.hex) { mutableListOf() }
                    val newPosition = AircraftPosition(
                        latitude = aircraft.lat,
                        longitude = aircraft.lon,
                        altitude = aircraft.altBaro,
                        timestamp = Clock.System.now().epochSeconds.toDouble()
                    )
                    if (positions.lastOrNull()?.let { it.latitude == newPosition.latitude && it.longitude == newPosition.longitude } != true) {
                        positions.add(newPosition)
                    }
                }
        }

        updateTrailsStateFlow()
    }

    private suspend fun fetchHistoryWithRetry(
        host: String,
        index: Int,
        maxRetries: Int = 3
    ): List<Aircraft>? {
        repeat(maxRetries) { attempt ->
            val result = piAwareApi.getHistoryFile(host, index)
            if (result != null) return result

            if (attempt < maxRetries - 1) {
                val delayMs = (attempt + 1) * 500L
                kotlinx.coroutines.delay(delayMs)
            }
        }
        return null
    }

    override fun updateTrailsFromAircraft(aircraft: List<Aircraft>) {
        val timestamp = Clock.System.now().epochSeconds.toDouble()
        currentAircraftHex = aircraft.map { it.hex }.toSet()

        aircraft
            .filter { it.lat != 0.0 && it.lon != 0.0 }
            .forEach { plane ->
                val positions = trailPositions.getOrPut(plane.hex) { mutableListOf() }
                val newPosition = AircraftPosition(
                    latitude = plane.lat,
                    longitude = plane.lon,
                    altitude = plane.altBaro,
                    timestamp = timestamp
                )
                if (positions.lastOrNull()?.let { it.latitude == newPosition.latitude && it.longitude == newPosition.longitude } != true) {
                    positions.add(newPosition)
                }
            }

        updateTrailsStateFlow()
    }

    override fun clearTrails() {
        trailPositions.clear()
        currentAircraftHex = emptySet()
        _aircraftTrails.value = emptyMap()
    }

    private fun updateTrailsStateFlow() {
        _aircraftTrails.value = trailPositions
            .filterKeys { currentAircraftHex.contains(it) }
            .mapValues { (hex, positions) ->
                AircraftTrail(hex = hex, positions = positions.toList())
            }
    }

    internal suspend fun lookupAircraftInfoRecursive(
        host: String,
        hex: String,
        level: Int = 1
    ): AircraftInfo? {
        val uppercaseHex = hex.uppercase()
        val bkey = uppercaseHex.take(level)
        val dkey = uppercaseHex.drop(level)

        val data = piAwareApi.getAircraftInfo(host, bkey) ?: return null

        if (data.containsKey(dkey)) {
            val info = data[dkey]!!
            val icaoAircraftType = lookAircraftType(info)
            return AircraftInfo(
                registration = info.jsonObject["i"]?.jsonPrimitive?.content,
                icaoType = info.jsonObject["t"]?.jsonPrimitive?.content,
                typeDescription = icaoAircraftType?.desc,
                wtc = icaoAircraftType?.wtc
            )
        }

        if (dkey.isNotEmpty() && data.containsKey("children")) {
            val subkey = bkey + dkey.first()
            if (data["children"]?.let { Json.decodeFromJsonElement<List<String>>(it) }
                    ?.contains(subkey) == true) {
                return lookupAircraftInfoRecursive(host, hex, level + 1)
            }
        }

        return null
    }

    private fun lookAircraftType(info: JsonElement): ICAOAircraftType? {
        return info.let { it.jsonObject["t"]?.jsonPrimitive?.content }
            ?.let { aircraftTypes?.get(it) }
    }

    fun <K, V> List<Map<K, V>>.flatten(): Map<K, V> {
        val result = mutableMapOf<K, V>()
        forEach { result.putAll(it) }
        return result
    }
}

fun List<Aircraft>.filterNoLocation(): List<Aircraft> {
    return this.filter { it.lat != 0.0 && it.lon != 0.0 }
}
