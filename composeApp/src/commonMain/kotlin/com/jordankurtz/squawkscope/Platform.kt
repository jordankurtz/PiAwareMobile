package com.jordankurtz.squawkscope

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
