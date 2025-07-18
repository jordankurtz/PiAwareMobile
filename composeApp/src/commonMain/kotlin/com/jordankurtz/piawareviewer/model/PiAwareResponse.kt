package com.jordankurtz.piawareviewer.model

import kotlinx.serialization.Serializable

@Serializable
data class PiAwareResponse(
    val aircraft: List<Aircraft>
)