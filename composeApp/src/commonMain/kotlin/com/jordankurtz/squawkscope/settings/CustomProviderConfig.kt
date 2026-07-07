package com.jordankurtz.squawkscope.settings

import kotlinx.serialization.Serializable

@Serializable
data class CustomProviderConfig(
    val id: String,
    val displayName: String,
    val urlTemplate: String,
)
