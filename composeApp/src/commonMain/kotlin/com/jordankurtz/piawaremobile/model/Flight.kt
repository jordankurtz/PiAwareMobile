package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Flight(
    val ident: String,
    @SerialName("ident_icao")
    val identIcao: String?,
    @SerialName("ident_iata")
    val identIata: String?,
    @SerialName("actual_runway_off")
    val actualRunwayOff: String?,
    @SerialName("actual_runway_on")
    val actualRunwayOn: String?,
    @SerialName("fa_flight_id")
    val faFlightId: String,
    val operator: String?,
    @SerialName("operator_icao")
    val operatorIcao: String?,
    @SerialName("operator_iata")
    val operatorIata: String?,
    @SerialName("flight_number")
    val flightNumber: String?,
    val registration: String?,
    @SerialName("atc_ident")
    val atcIdent: String?,
    @SerialName("inbound_fa_flight_id")
    val inboundFaFlightId: String?,
    val codeshares: List<String>?,
    @SerialName("codeshares_iata")
    val codesharesIata: List<String>?,
    val blocked: Boolean,
    val diverted: Boolean,
    val cancelled: Boolean,
    @SerialName("position_only")
    val positionOnly: Boolean,
    val origin: FlightAirportRef?,
    val destination: FlightAirportRef?,
    @SerialName("departure_delay")
    val departureDelay: Int?,
    @SerialName("arrival_delay")
    val arrivalDelay: Int?,
    @SerialName("filed_ete")
    val filedEte: Int?,
    @SerialName("progress_percent")
    val progressPercent: Int?,
    val status: String,
    @SerialName("aircraft_type")
    val aircraftType: String?,
    @SerialName("route_distance")
    val routeDistance: Int?,
    @SerialName("filed_airspeed")
    val filedAirspeed: Int?,
    @SerialName("filed_altitude")
    val filedAltitude: Int?,
    val route: String?,
    @SerialName("baggage_claim")
    val baggageClaim: String?,
    @SerialName("seats_cabin_business")
    val seatsCabinBusiness: Int?,
    @SerialName("seats_cabin_coach")
    val seatsCabinCoach: Int?,
    @SerialName("seats_cabin_first")
    val seatsCabinFirst: Int?,
    @SerialName("gate_origin")
    val gateOrigin: String?,
    @SerialName("gate_destination")
    val gateDestination: String?,
    @SerialName("terminal_origin")
    val terminalOrigin: String?,
    @SerialName("terminal_destination")
    val terminalDestination: String?,
    val type: String,
    @SerialName("scheduled_out")
    val scheduledOut: Instant?,
    @SerialName("estimated_out")
    val estimatedOut: Instant?,
    @SerialName("actual_out")
    val actualOut: Instant?,
    @SerialName("scheduled_off")
    val scheduledOff: Instant?,
    @SerialName("estimated_off")
    val estimatedOff: Instant?,
    @SerialName("actual_off")
    val actualOff: Instant?,
    @SerialName("scheduled_on")
    val scheduledOn: Instant?,
    @SerialName("estimated_on")
    val estimatedOn: Instant?,
    @SerialName("actual_on")
    val actualOn: Instant?,
    @SerialName("scheduled_in")
    val scheduledIn: Instant?,
    @SerialName("estimated_in")
    val estimatedIn: Instant?,
    @SerialName("actual_in")
    val actualIn: Instant?,
    @SerialName("foresight_predictions_available")
    val foresightPredictionsAvailable: Boolean
)
