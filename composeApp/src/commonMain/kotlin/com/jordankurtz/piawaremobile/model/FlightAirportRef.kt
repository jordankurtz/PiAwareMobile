package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightAirportRef(
    val code: String?,
    @SerialName("code_icao")
    val codeIcao: String?,
    @SerialName("code_iata")
    val codeIata: String?,
    @SerialName("code_lid")
    val codeLid: String?,
    val timezone: String?,
    val name: String?,
    val city: String?,
    @SerialName("airport_info_url")
    val airportInfoUrl: String?
)
