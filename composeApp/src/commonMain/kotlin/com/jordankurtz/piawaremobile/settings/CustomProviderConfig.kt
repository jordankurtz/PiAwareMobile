package com.jordankurtz.piawaremobile.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CustomProviderConfig(
    val id: String,
    val displayName: String,
    @SerialName("urlTemplate") val styleUrl: String,
)
