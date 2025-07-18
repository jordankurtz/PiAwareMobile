package com.jordankurtz.piawareviewer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform