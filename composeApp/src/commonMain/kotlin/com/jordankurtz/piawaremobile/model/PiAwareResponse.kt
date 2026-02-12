package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.Serializable

@Serializable
data class PiAwareResponse(
    val now: Double? = null,
    val aircraft: List<Aircraft>
)
