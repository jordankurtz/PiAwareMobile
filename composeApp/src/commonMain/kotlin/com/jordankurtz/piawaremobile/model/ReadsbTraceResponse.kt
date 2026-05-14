package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

@Serializable
data class ReadsbTraceResponse(
    val icao: String,
    val timestamp: Double,
    val trace: List<JsonArray>,
)
