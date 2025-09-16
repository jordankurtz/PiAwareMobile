package com.jordankurtz.piawaremobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform