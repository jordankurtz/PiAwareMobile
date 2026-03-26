package com.jordankurtz.piawaremobile.aircraft.repo

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.aircraft.api.AeroApi
import com.jordankurtz.piawaremobile.aircraft.api.AircraftDataSourceFactory
import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.AircraftInfo
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.FlightResponse
import com.jordankurtz.piawaremobile.model.ICAOAircraftType
import com.jordankurtz.piawaremobile.model.PiAwareResponse
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.model.ReceiverType
import com.jordankurtz.piawaremobile.settings.Server
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.annotation.Single

@Single(binds = [AircraftRepo::class])
class AircraftRepoImpl(
    private val dataSourceFactory: AircraftDataSourceFactory,
    private val aeroApi: AeroApi,
    private val trailManager: AircraftTrailManager,
) : AircraftRepo {
    private val aircraftInfoCache = mutableMapOf<String, AircraftInfo>()

    private var aircraftTypes: Map<String, ICAOAircraftType>? = null

    override suspend fun getAircraftWithServers(servers: List<Server>): Map<Aircraft, Set<Server>> {
        val result =
            coroutineScope {
                // Fetch aircraft from each server with the server name
                val aircraftByServer =
                    servers.map { server ->
                        async {
                            try {
                                val dataSource = dataSourceFactory.getDataSource(server.type)
                                dataSource.getAircraft(server).map { aircraft -> aircraft to server }
                            } catch (e: Exception) {
                                Logger.e("Failed to fetch aircraft from server $server", e)
                                emptyList()
                            }
                        }
                    }.awaitAll().flatten()

                // Group by hex and merge - keep freshest aircraft data, accumulate servers
                val mergedAircraft = mutableMapOf<String, Pair<Aircraft, MutableSet<Server>>>()
                for ((aircraft, server) in aircraftByServer) {
                    if (!aircraft.hasPosition) continue

                    val existing = mergedAircraft[aircraft.hex]
                    if (existing == null) {
                        mergedAircraft[aircraft.hex] = aircraft to mutableSetOf(server)
                    } else {
                        existing.second.add(server)
                        // Keep the fresher aircraft data (lower `seen` value means more recent)
                        val existingSeenTime = existing.first.seen ?: Float.MAX_VALUE
                        val newSeenTime = aircraft.seen ?: Float.MAX_VALUE
                        if (newSeenTime < existingSeenTime) {
                            mergedAircraft[aircraft.hex] = aircraft to existing.second
                        }
                    }
                }

                mergedAircraft.values.associate { (aircraft, servers) -> aircraft to servers.toSet() }
            }
        trailManager.updateTrailsFromAircraft(result.keys.toList())
        return result
    }

    override suspend fun loadAircraftTypes(servers: List<Server>) {
        if (aircraftTypes == null) {
            aircraftTypes =
                servers.map { server ->
                    dataSourceFactory.getDataSource(server.type).getAircraftTypes(server)
                }.flatten()
        }
    }

    override suspend fun findAircraftInfo(
        server: Server,
        hex: String,
    ): AircraftInfo? {
        if (aircraftInfoCache.containsKey(hex)) return aircraftInfoCache[hex]

        val info = lookupAircraftInfoRecursive(server, hex.replace("~", ""))
        info?.let { aircraftInfoCache[hex] = it }
        return info
    }

    override suspend fun getReceiverInfo(
        server: Server,
        receiverType: ReceiverType,
    ): Receiver? {
        val dataSource = dataSourceFactory.getDataSource(server.type)
        return when (receiverType) {
            ReceiverType.DUMP_1090 -> dataSource.getReceiverInfo(server)
            ReceiverType.DUMP_978 -> {
                // dump978 receiver info is only available on PiAware servers
                // For other server types, return null
                dataSource.getReceiverInfo(server)
            }
        }
    }

    override suspend fun lookupFlight(ident: String): Async<FlightResponse> {
        return try {
            val response =
                aeroApi.getFlight(
                    ident = ident,
                )
            Async.Success(response)
        } catch (e: Exception) {
            Logger.e("Failed to fetch flight for ident $ident", e)
            Async.Error("Failed to fetch flight for ident $ident", e)
        }
    }

    override suspend fun fetchAndMergeHistory(server: Server) {
        val dataSource = dataSourceFactory.getDataSource(server.type)
        val receiver = dataSource.getReceiverInfo(server) ?: return
        val historyCount = receiver.history ?: return
        if (historyCount <= 0) return

        coroutineScope {
            val historyResults =
                (0 until historyCount).map { index ->
                    async {
                        fetchHistoryWithRetry(server, index)
                    }
                }.awaitAll()

            trailManager.mergeHistoryResponses(historyResults.filterNotNull())
        }
    }

    private suspend fun fetchHistoryWithRetry(
        server: Server,
        index: Int,
        maxRetries: Int = 3,
    ): PiAwareResponse? {
        val dataSource = dataSourceFactory.getDataSource(server.type)
        repeat(maxRetries) { attempt ->
            val result = dataSource.getHistory(server, index)
            if (result != null) return result

            if (attempt < maxRetries - 1) {
                val delayMs = (attempt + 1) * 500L
                delay(delayMs)
            }
        }
        return null
    }

    internal suspend fun lookupAircraftInfoRecursive(
        server: Server,
        hex: String,
        level: Int = 1,
    ): AircraftInfo? {
        val uppercaseHex = hex.uppercase()
        val bkey = uppercaseHex.take(level)
        val dkey = uppercaseHex.drop(level)

        val dataSource = dataSourceFactory.getDataSource(server.type)
        val data = dataSource.getAircraftInfo(server, bkey) ?: return null

        if (data.containsKey(dkey)) {
            val info = data[dkey]!!
            val icaoAircraftType = lookAircraftType(info)
            return AircraftInfo(
                registration = info.jsonObject["i"]?.jsonPrimitive?.content,
                icaoType = info.jsonObject["t"]?.jsonPrimitive?.content,
                typeDescription = icaoAircraftType?.desc,
                wtc = icaoAircraftType?.wtc,
            )
        }

        if (dkey.isNotEmpty() && data.containsKey("children")) {
            val subkey = bkey + dkey.first()
            if (data["children"]?.let { Json.decodeFromJsonElement<List<String>>(it) }
                    ?.contains(subkey) == true
            ) {
                return lookupAircraftInfoRecursive(server, hex, level + 1)
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
    return this.filter { it.hasPosition }
}
