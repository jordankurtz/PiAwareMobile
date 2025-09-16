package com.jordankurtz.piawaremobile.settings

import kotlinx.serialization.Serializable

@Serializable
data class Server (
    val name: String,
    val address: String
)
