package com.jordankurtz.piawaremobile

import io.ktor.client.HttpClient

expect fun getKtorClient(): HttpClient

class KtorClient {
    val client: HttpClient
        get() = getKtorClient()
}