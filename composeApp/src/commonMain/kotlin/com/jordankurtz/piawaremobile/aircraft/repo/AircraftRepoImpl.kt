package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.api.PiAwareApi
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single
import kotlin.time.Instant

@Single(binds = [AircraftRepo::class])
class AircraftRepoImpl(
    private val piAwareApi: PiAwareApi,
    private val aeroApi: AeroApi
) : AircraftRepo {

    private val aircraftInfoCache = mutableMapOf<String, AircraftInfo>()

    private var aircraftTypes: Map<String, ICAOAircraftType>? = null

    override suspend fun getAircraft(servers: List<String>): List<Aircraft> =
        coroutineScope {
            servers.map { server ->
                async {
                    try {
                        piAwareApi.getAircraft(server)
                    } catch (e: Exception) {
                        // Log the error and return an empty list for the failed server
                        println("Failed to fetch aircraft from server $server: ${e.message}")
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
            println("Failed to fetch flight for ident $ident: ${e.message}")
            Async.Error("Failed to fetch flight for ident $ident", e)
        }
    }

    internal suspend fun lookupAircraftInfoRecursive(
        host: String,
        hex: String,
        level: Int = 1
    ): AircraftInfo? {
        val uppercaseHex = hex.uppercase()
        val bkey = uppercaseHex.substring(0, level)
        val dkey = uppercaseHex.substring(level)

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
            val subkey = bkey + dkey.substring(0, 1)
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

private fun Instant.toFormattedString(): String {
    val localDateTime = toLocalDateTime(TimeZone.currentSystemDefault())

    // Define a custom format using the DSL
    val formatter = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char('T')
        hour()
        char(':')
        minute()
        char(':')
        second()
        char('Z')
    }
    return formatter.format(localDateTime)
}
