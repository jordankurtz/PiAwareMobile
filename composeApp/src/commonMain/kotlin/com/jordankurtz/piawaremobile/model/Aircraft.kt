package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Aircraft (
    val hex: String,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val flight: String? = null,
    val track: Float = 0f,
    val category: String? = null,
    @SerialName("alt_baro")
    val altitude: String = ""
)