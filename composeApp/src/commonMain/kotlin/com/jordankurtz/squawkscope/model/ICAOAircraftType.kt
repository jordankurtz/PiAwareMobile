package com.jordankurtz.squawkscope.model

import kotlinx.serialization.Serializable

@Serializable
data class ICAOAircraftType(
    val desc: String,
    val wtc: String,
)
