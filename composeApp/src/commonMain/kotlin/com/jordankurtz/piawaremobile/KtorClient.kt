package com.jordankurtz.piawaremobile

import io.ktor.client.HttpClient

expect fun getKtorClient(): HttpClient

object HttpTimeoutDefaults {
    const val REQUEST_TIMEOUT_MS = 10_000L
    const val CONNECT_TIMEOUT_MS = 5_000L
    const val SOCKET_TIMEOUT_MS = 10_000L
}

class KtorClient {
    val client: HttpClient
        get() = getKtorClient()
}
