package com.jordankurtz.piawaremobile.squawk

enum class SquawkSeverity(val label: String) {
    EMERGENCY("Emergency"),
    CAUTION("Caution"),
    INFO("Info"),
    NORMAL(""),
}

data class SquawkInfo(
    val name: String,
    val description: String,
    val severity: SquawkSeverity,
)
