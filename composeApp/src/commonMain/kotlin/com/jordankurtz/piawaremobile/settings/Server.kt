package com.jordankurtz.piawaremobile.settings

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class Server(
    val id: Uuid = Uuid.random(),
    val name: String,
    val address: String
)
