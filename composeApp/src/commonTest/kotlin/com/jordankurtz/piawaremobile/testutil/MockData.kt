package com.jordankurtz.piawaremobile.testutil

import com.jordankurtz.piawaremobile.model.Aircraft
import com.jordankurtz.piawaremobile.model.Async
import com.jordankurtz.piawaremobile.model.Flight
import com.jordankurtz.piawaremobile.model.FlightAirportRef
import com.jordankurtz.piawaremobile.model.Receiver
import com.jordankurtz.piawaremobile.settings.Server
import com.jordankurtz.piawaremobile.settings.ServerType
import com.jordankurtz.piawaremobile.settings.Settings
import com.jordankurtz.piawaremobile.settings.TrailDisplayMode
import kotlin.uuid.Uuid

fun mockSettings(
    servers: List<Server> = listOf(mockServer()),
    refreshInterval: Int = 5,
    centerMapOnUserOnStart: Boolean = false,
    restoreMapStateOnStart: Boolean = true,
    showReceiverLocations: Boolean = true,
    showUserLocationOnMap: Boolean = true,
    trailDisplayMode: TrailDisplayMode = TrailDisplayMode.ALL,
    showMinimapTrails: Boolean = true,
    openUrlsExternally: Boolean = false,
    enableFlightAwareApi: Boolean = false,
    flightAwareApiKey: String = "",
): Settings =
    Settings(
        servers = servers,
        refreshInterval = refreshInterval,
        centerMapOnUserOnStart = centerMapOnUserOnStart,
        restoreMapStateOnStart = restoreMapStateOnStart,
        showReceiverLocations = showReceiverLocations,
        showUserLocationOnMap = showUserLocationOnMap,
        trailDisplayMode = trailDisplayMode,
        showMinimapTrails = showMinimapTrails,
        openUrlsExternally = openUrlsExternally,
        enableFlightAwareApi = enableFlightAwareApi,
        flightAwareApiKey = flightAwareApiKey,
    )

fun mockServer(
    id: Uuid = Uuid.random(),
    name: String = "Test PiAware",
    address: String = "piaware.local",
    type: ServerType = ServerType.PIAWARE,
): Server =
    Server(
        id = id,
        name = name,
        address = address,
        type = type,
    )

fun mockAircraft(
    hex: String = "A1B2C3",
    flight: String? = "TST101  ",
    lat: Double = 40.0,
    lon: Double = -100.0,
    altBaro: String? = "35000",
    gs: Float? = 450f,
    track: Float? = 180f,
    squawk: String? = "1200",
    rssi: Float? = -3.5f,
    seen: Float? = 1.2f,
): Aircraft =
    Aircraft(
        hex = hex,
        lat = lat,
        lon = lon,
        flight = flight,
        altBaro = altBaro,
        gs = gs,
        track = track,
        squawk = squawk,
        rssi = rssi,
        seen = seen,
    )

fun mockAircraftList(): List<Aircraft> =
    listOf(
        mockAircraft(hex = "A1B2C3", flight = "TST101  "),
        mockAircraft(hex = "D4E5F6", flight = "TST202  ", lat = 40.5, lon = -100.5),
        mockAircraft(hex = "789ABC", flight = "TST303  ", lat = 39.5, lon = -99.5),
    )

fun mockFlight(): Flight =
    Flight(
        ident = "TST101",
        identIcao = "TST101",
        identIata = "TT101",
        actualRunwayOff = null,
        actualRunwayOn = null,
        faFlightId = "TST101-1234567890-schedule-0001",
        operator = "Test Airlines",
        operatorIcao = "TST",
        operatorIata = "TT",
        flightNumber = "101",
        registration = "N12345",
        atcIdent = "TST101",
        inboundFaFlightId = null,
        codeshares = emptyList(),
        codesharesIata = emptyList(),
        blocked = false,
        diverted = false,
        cancelled = false,
        positionOnly = false,
        origin =
            mockAirportRef(
                code = "KTST",
                name = "Test Origin Airport",
                city = "Test City",
            ),
        destination =
            mockAirportRef(
                code = "KDST",
                name = "Test Destination Airport",
                city = "Destination City",
            ),
        departureDelay = 0,
        arrivalDelay = 0,
        filedEte = 7200,
        progressPercent = 50,
        status = "En Route",
        aircraftType = "A320",
        routeDistance = 800,
        filedAirspeed = 450,
        filedAltitude = 350,
        route = null,
        baggageClaim = null,
        seatsCabinBusiness = null,
        seatsCabinCoach = null,
        seatsCabinFirst = null,
        gateOrigin = "A12",
        gateDestination = "B7",
        terminalOrigin = "1",
        terminalDestination = "2",
        type = "Airline",
        scheduledOut = null,
        estimatedOut = null,
        actualOut = null,
        scheduledOff = null,
        estimatedOff = null,
        actualOff = null,
        scheduledOn = null,
        estimatedOn = null,
        actualOn = null,
        scheduledIn = null,
        estimatedIn = null,
        actualIn = null,
        foresightPredictionsAvailable = false,
    )

fun mockAirportRef(
    code: String = "KTST",
    name: String = "Test Airport",
    city: String = "Test City",
): FlightAirportRef =
    FlightAirportRef(
        code = code,
        codeIcao = code,
        codeIata = code.drop(1),
        codeLid = code,
        timezone = "America/Chicago",
        name = name,
        city = city,
        airportInfoUrl = "/airports/$code",
    )

fun mockReceiver(): Receiver =
    Receiver(
        latitude = 40.0f,
        longitude = -100.0f,
        history = 120,
    )

fun mockSettingsAsync(settings: Settings = mockSettings()): Async<Settings> = Async.Success(settings)
