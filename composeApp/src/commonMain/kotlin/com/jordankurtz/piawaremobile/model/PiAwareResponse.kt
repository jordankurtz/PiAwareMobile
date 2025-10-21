package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.Serializable

@Serializable
data class PiAwareResponse(
    val aircraft: List<Aircraft>
)
