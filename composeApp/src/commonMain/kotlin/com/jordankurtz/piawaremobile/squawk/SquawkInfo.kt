package com.jordankurtz.piawaremobile.squawk

enum class SquawkSeverity {
    EMERGENCY,
    CAUTION,
    INFO,
    NORMAL,
}

data class SquawkInfo(
    val name: String,
    val description: String,
    val severity: SquawkSeverity,
)
