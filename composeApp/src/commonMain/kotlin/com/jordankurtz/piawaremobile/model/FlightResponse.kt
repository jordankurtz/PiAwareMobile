package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightResponse(
    val links: Links?,
    @SerialName("num_pages")
    val numPages: Int,
    val flights: List<Flight>
)
