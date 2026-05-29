package com.jordankurtz.piawaremobile.settings

import kotlinx.serialization.Serializable

@Serializable
data class CustomProviderConfig(
    val id: String,
    val displayName: String,
    val urlTemplate: String,
)
