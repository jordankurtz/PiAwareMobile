package com.jordankurtz.piawaremobile.map

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

/**
 * Fetches the response body from [path] as a [ByteArray] using an HTTP GET request.
 */
suspend fun getStream(
    client: HttpClient,
    path: String,
): ByteArray {
    val buffer = Buffer()
    client.prepareGet(path).execute { httpResponse ->
        val channel: ByteReadChannel = httpResponse.body()
        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.exhausted()) {
                val bytes = packet.readByteArray()
                buffer.write(bytes, 0, bytes.size)
            }
        }
    }
    return buffer.readByteArray()
}

private const val DEFAULT_BUFFER_SIZE = 8192
