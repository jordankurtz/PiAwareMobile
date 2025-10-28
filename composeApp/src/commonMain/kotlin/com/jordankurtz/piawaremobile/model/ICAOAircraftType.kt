package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.Serializable

@Serializable
data class ICAOAircraftType(
    val desc: String,
    val wtc: String
)
