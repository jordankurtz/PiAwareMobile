package com.jordankurtz.squawkscope.model

data class AircraftInfo(
    val registration: String?,
    val icaoType: String?,
    val typeDescription: String?,
    val wtc: String?,
) {
    val subtitle: String
        get() =
            buildList {
                registration?.let { add(it) }
                typeDescription?.let { add(it) }
            }.joinToString(" - ")
}
