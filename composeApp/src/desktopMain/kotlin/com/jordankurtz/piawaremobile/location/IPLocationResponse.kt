package com.jordankurtz.piawaremobile.location

import kotlinx.serialization.Serializable

@Serializable
data class IPLocationResponse(
    val status: String,
    val lat: Double,
    val lon: Double,
    val city: String? = null,
    val country: String? = null
)