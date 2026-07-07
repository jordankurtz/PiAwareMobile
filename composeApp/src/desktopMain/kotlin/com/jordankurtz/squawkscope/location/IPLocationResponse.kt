package com.jordankurtz.squawkscope.location

import kotlinx.serialization.Serializable

@Serializable
data class IPLocationResponse(
    val success: Boolean,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String? = null,
    val country: String? = null,
)
