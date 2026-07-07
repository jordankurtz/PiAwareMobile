package com.jordankurtz.squawkscope.settings

import kotlinx.serialization.Serializable

@Serializable
enum class ServerType {
    PIAWARE,
    READSB,
}
