package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Receiver(
    @SerialName("lat")
    val latitude: Float,
    @SerialName("lon")
    val longitude: Float
)
