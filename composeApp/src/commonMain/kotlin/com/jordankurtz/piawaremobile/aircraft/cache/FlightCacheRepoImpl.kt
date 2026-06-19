package com.jordankurtz.piawaremobile.aircraft.cache

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.map.cache.Cached_flight
import com.jordankurtz.piawaremobile.map.cache.FlightCacheQueries
import com.jordankurtz.piawaremobile.map.cache.Flight_cache_region
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Instant

@Single(binds = [FlightCacheRepo::class])
class FlightCacheRepoImpl(
    private val queries: FlightCacheQueries,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
) : FlightCacheRepo {
    override suspend fun getCachedFlight(ident: String): CachedFlightEntry? =
        withContext(ioDispatcher) {
            queries.getCachedFlight(ident).executeAsOneOrNull()?.toCachedFlightEntry()
        }

    override suspend fun cacheFlight(flight: Flight) =
        withContext(ioDispatcher) {
            queries.upsertFlight(
                flight = flight,
                cachedAt = Clock.System.now().epochSeconds,
                individuallyCached = 1L,
            )
        }

    override suspend fun bulkCacheFlights(
        flights: List<Flight>,
        regionId: Long,
    ) = withContext(ioDispatcher) {
        queries.transaction {
            flights.forEach { flight ->
                queries.upsertPrefetchFlight(flight = flight, cachedAt = Clock.System.now().epochSeconds)
                queries.linkFlightToRegion(region_id = regionId, ident = flight.ident)
            }
        }
    }

    override suspend fun getAllRegions(): List<CachedRegion> =
        withContext(ioDispatcher) {
            queries.selectAllFlightRegions().executeAsList().map { it.toCachedRegion() }
        }

    override suspend fun insertRegion(
        name: String,
        box: BoundingBox,
        daysAhead: Int,
    ): Long =
        withContext(ioDispatcher) {
            queries.transactionWithResult {
                queries.insertFlightRegion(
                    name = name,
                    min_lat = box.minLat,
                    max_lat = box.maxLat,
                    min_lon = box.minLon,
                    max_lon = box.maxLon,
                    days_ahead = daysAhead.toLong(),
                    cached_at = Clock.System.now().epochSeconds,
                )
                queries.lastInsertedFlightRegionId().executeAsOne()
            }
        }

    override suspend fun updateRegionFlightCount(
        regionId: Long,
        count: Int,
    ) = withContext(ioDispatcher) {
        queries.updateRegionFlightCount(flight_count = count.toLong(), id = regionId)
    }

    override suspend fun deleteRegion(regionId: Long) =
        withContext(ioDispatcher) {
            queries.transaction {
                queries.deleteFlightRegion(regionId)
                queries.deleteOrphanedPrefetchFlights()
            }
        }
}

private fun Cached_flight.toFlight(): Flight =
    Flight(
        ident = ident,
        identIcao = ident_icao,
        identIata = ident_iata,
        faFlightId = fa_flight_id,
        operator = operator_,
        operatorIcao = operator_icao,
        operatorIata = operator_iata,
        flightNumber = flight_number,
        registration = registration,
        atcIdent = atc_ident,
        inboundFaFlightId = inbound_fa_flight_id,
        codeshares = codeshares?.split(",")?.filter { it.isNotBlank() },
        codesharesIata = codeshares_iata?.split(",")?.filter { it.isNotBlank() },
        blocked = blocked != 0L,
        diverted = diverted != 0L,
        cancelled = cancelled != 0L,
        positionOnly = position_only != 0L,
        origin =
            if (origin_code != null || origin_name != null) {
                FlightAirportRef(
                    code = origin_code,
                    codeIcao = origin_code_icao,
                    codeIata = origin_code_iata,
                    codeLid = null,
                    timezone = origin_timezone,
                    name = origin_name,
                    city = origin_city,
                    airportInfoUrl = null,
                )
            } else {
                null
            },
        destination =
            if (destination_code != null || destination_name != null) {
                FlightAirportRef(
                    code = destination_code,
                    codeIcao = destination_code_icao,
                    codeIata = destination_code_iata,
                    codeLid = null,
                    timezone = destination_timezone,
                    name = destination_name,
                    city = destination_city,
                    airportInfoUrl = null,
                )
            } else {
                null
            },
        departureDelay = departure_delay?.toInt(),
        arrivalDelay = arrival_delay?.toInt(),
        filedEte = filed_ete?.toInt(),
        progressPercent = progress_percent?.toInt(),
        status = status,
        aircraftType = aircraft_type,
        routeDistance = route_distance?.toInt(),
        filedAirspeed = filed_airspeed?.toInt(),
        filedAltitude = filed_altitude?.toInt(),
        route = route,
        baggageClaim = baggage_claim,
        seatsCabinBusiness = null,
        seatsCabinCoach = null,
        seatsCabinFirst = null,
        gateOrigin = gate_origin,
        gateDestination = gate_destination,
        terminalOrigin = terminal_origin,
        terminalDestination = terminal_destination,
        type = type,
        scheduledOut = scheduled_out?.let { Instant.fromEpochSeconds(it) },
        estimatedOut = estimated_out?.let { Instant.fromEpochSeconds(it) },
        actualOut = actual_out?.let { Instant.fromEpochSeconds(it) },
        scheduledOff = scheduled_off?.let { Instant.fromEpochSeconds(it) },
        estimatedOff = estimated_off?.let { Instant.fromEpochSeconds(it) },
        actualOff = actual_off?.let { Instant.fromEpochSeconds(it) },
        scheduledOn = scheduled_on?.let { Instant.fromEpochSeconds(it) },
        estimatedOn = estimated_on?.let { Instant.fromEpochSeconds(it) },
        actualOn = actual_on?.let { Instant.fromEpochSeconds(it) },
        scheduledIn = scheduled_in?.let { Instant.fromEpochSeconds(it) },
        estimatedIn = estimated_in?.let { Instant.fromEpochSeconds(it) },
        actualIn = actual_in?.let { Instant.fromEpochSeconds(it) },
        foresightPredictionsAvailable = foresight_predictions_available != 0L,
        actualRunwayOff = actual_runway_off,
        actualRunwayOn = actual_runway_on,
    )

private fun Cached_flight.toCachedFlightEntry() =
    CachedFlightEntry(
        flight = toFlight(),
        cachedAt = Instant.fromEpochSeconds(cached_at),
    )

private fun Flight_cache_region.toCachedRegion() =
    CachedRegion(
        id = id,
        name = name,
        box = BoundingBox(minLat = min_lat, maxLat = max_lat, minLon = min_lon, maxLon = max_lon),
        daysAhead = days_ahead.toInt(),
        flightCount = flight_count.toInt(),
        cachedAt = Instant.fromEpochSeconds(cached_at),
    )

private fun FlightCacheQueries.upsertPrefetchFlight(
    flight: Flight,
    cachedAt: Long,
) {
    upsertPrefetchFlight(
        ident = flight.ident,
        ident_icao = flight.identIcao,
        ident_iata = flight.identIata,
        fa_flight_id = flight.faFlightId,
        operator_ = flight.operator,
        operator_icao = flight.operatorIcao,
        operator_iata = flight.operatorIata,
        flight_number = flight.flightNumber,
        registration = flight.registration,
        atc_ident = flight.atcIdent,
        inbound_fa_flight_id = flight.inboundFaFlightId,
        codeshares = flight.codeshares?.joinToString(","),
        codeshares_iata = flight.codesharesIata?.joinToString(","),
        blocked = if (flight.blocked) 1L else 0L,
        diverted = if (flight.diverted) 1L else 0L,
        cancelled = if (flight.cancelled) 1L else 0L,
        position_only = if (flight.positionOnly) 1L else 0L,
        origin_code = flight.origin?.code,
        origin_code_icao = flight.origin?.codeIcao,
        origin_code_iata = flight.origin?.codeIata,
        origin_timezone = flight.origin?.timezone,
        origin_name = flight.origin?.name,
        origin_city = flight.origin?.city,
        destination_code = flight.destination?.code,
        destination_code_icao = flight.destination?.codeIcao,
        destination_code_iata = flight.destination?.codeIata,
        destination_timezone = flight.destination?.timezone,
        destination_name = flight.destination?.name,
        destination_city = flight.destination?.city,
        scheduled_out = flight.scheduledOut?.epochSeconds,
        estimated_out = flight.estimatedOut?.epochSeconds,
        actual_out = flight.actualOut?.epochSeconds,
        scheduled_off = flight.scheduledOff?.epochSeconds,
        estimated_off = flight.estimatedOff?.epochSeconds,
        actual_off = flight.actualOff?.epochSeconds,
        scheduled_on = flight.scheduledOn?.epochSeconds,
        estimated_on = flight.estimatedOn?.epochSeconds,
        actual_on = flight.actualOn?.epochSeconds,
        scheduled_in = flight.scheduledIn?.epochSeconds,
        estimated_in = flight.estimatedIn?.epochSeconds,
        actual_in = flight.actualIn?.epochSeconds,
        departure_delay = flight.departureDelay?.toLong(),
        arrival_delay = flight.arrivalDelay?.toLong(),
        progress_percent = flight.progressPercent?.toLong(),
        status = flight.status,
        aircraft_type = flight.aircraftType,
        route_distance = flight.routeDistance?.toLong(),
        filed_airspeed = flight.filedAirspeed?.toLong(),
        filed_altitude = flight.filedAltitude?.toLong(),
        filed_ete = flight.filedEte?.toLong(),
        route = flight.route,
        baggage_claim = flight.baggageClaim,
        gate_origin = flight.gateOrigin,
        gate_destination = flight.gateDestination,
        terminal_origin = flight.terminalOrigin,
        terminal_destination = flight.terminalDestination,
        type = flight.type,
        actual_runway_off = flight.actualRunwayOff,
        actual_runway_on = flight.actualRunwayOn,
        foresight_predictions_available = if (flight.foresightPredictionsAvailable) 1L else 0L,
        cached_at = cachedAt,
        individually_cached = 0L,
    )
}

private fun FlightCacheQueries.upsertFlight(
    flight: Flight,
    cachedAt: Long,
    individuallyCached: Long,
) {
    upsertFlight(
        ident = flight.ident,
        ident_icao = flight.identIcao,
        ident_iata = flight.identIata,
        fa_flight_id = flight.faFlightId,
        operator_ = flight.operator,
        operator_icao = flight.operatorIcao,
        operator_iata = flight.operatorIata,
        flight_number = flight.flightNumber,
        registration = flight.registration,
        atc_ident = flight.atcIdent,
        inbound_fa_flight_id = flight.inboundFaFlightId,
        codeshares = flight.codeshares?.joinToString(","),
        codeshares_iata = flight.codesharesIata?.joinToString(","),
        blocked = if (flight.blocked) 1L else 0L,
        diverted = if (flight.diverted) 1L else 0L,
        cancelled = if (flight.cancelled) 1L else 0L,
        position_only = if (flight.positionOnly) 1L else 0L,
        origin_code = flight.origin?.code,
        origin_code_icao = flight.origin?.codeIcao,
        origin_code_iata = flight.origin?.codeIata,
        origin_timezone = flight.origin?.timezone,
        origin_name = flight.origin?.name,
        origin_city = flight.origin?.city,
        destination_code = flight.destination?.code,
        destination_code_icao = flight.destination?.codeIcao,
        destination_code_iata = flight.destination?.codeIata,
        destination_timezone = flight.destination?.timezone,
        destination_name = flight.destination?.name,
        destination_city = flight.destination?.city,
        scheduled_out = flight.scheduledOut?.epochSeconds,
        estimated_out = flight.estimatedOut?.epochSeconds,
        actual_out = flight.actualOut?.epochSeconds,
        scheduled_off = flight.scheduledOff?.epochSeconds,
        estimated_off = flight.estimatedOff?.epochSeconds,
        actual_off = flight.actualOff?.epochSeconds,
        scheduled_on = flight.scheduledOn?.epochSeconds,
        estimated_on = flight.estimatedOn?.epochSeconds,
        actual_on = flight.actualOn?.epochSeconds,
        scheduled_in = flight.scheduledIn?.epochSeconds,
        estimated_in = flight.estimatedIn?.epochSeconds,
        actual_in = flight.actualIn?.epochSeconds,
        departure_delay = flight.departureDelay?.toLong(),
        arrival_delay = flight.arrivalDelay?.toLong(),
        progress_percent = flight.progressPercent?.toLong(),
        status = flight.status,
        aircraft_type = flight.aircraftType,
        route_distance = flight.routeDistance?.toLong(),
        filed_airspeed = flight.filedAirspeed?.toLong(),
        filed_altitude = flight.filedAltitude?.toLong(),
        filed_ete = flight.filedEte?.toLong(),
        route = flight.route,
        baggage_claim = flight.baggageClaim,
        gate_origin = flight.gateOrigin,
        gate_destination = flight.gateDestination,
        terminal_origin = flight.terminalOrigin,
        terminal_destination = flight.terminalDestination,
        type = flight.type,
        actual_runway_off = flight.actualRunwayOff,
        actual_runway_on = flight.actualRunwayOn,
        foresight_predictions_available = if (flight.foresightPredictionsAvailable) 1L else 0L,
        cached_at = cachedAt,
        individually_cached = individuallyCached,
    )
}
