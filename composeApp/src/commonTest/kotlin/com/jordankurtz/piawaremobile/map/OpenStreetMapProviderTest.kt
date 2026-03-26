package com.jordankurtz.piawaremobile.map

import com.jordankurtz.piawaremobile.map.cache.TileCache
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OpenStreetMapProviderTest {
    private val tileBytes = byteArrayOf(89, 80, 78, 71, 13, 10, 26, 10) // fake PNG header
    private lateinit var tileCache: TileCache
    private lateinit var provider: OpenStreetMapProvider

    private fun createProvider(httpClient: HttpClient) {
        tileCache = mock()
        provider =
            OpenStreetMapProvider(
                httpClient = httpClient,
                tileCache = tileCache,
            )
    }

    private fun mockHttpClient(responseBytes: ByteArray = tileBytes): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = String(responseBytes, Charsets.ISO_8859_1).encodeToByteArray(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }
            }
        }

    private fun readAllBytes(source: kotlinx.io.RawSource): ByteArray {
        val buffer = Buffer()
        source.use { raw ->
            while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                // read until exhausted
            }
        }
        return buffer.readByteArray()
    }

    @Test
    fun returnsCachedTileWhenCacheHits() =
        runTest {
            createProvider(mockHttpClient())
            everySuspend { tileCache.get(any(), any(), any()) } returns tileBytes

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNotNull(result)
            assertContentEquals(tileBytes, readAllBytes(result))
            // Should not attempt to put since it was a cache hit
            verifySuspend(VerifyMode.not) { tileCache.put(any(), any(), any(), any()) }
        }

    @Test
    fun fetchesFromNetworkOnCacheMissAndStoresInCache() =
        runTest {
            createProvider(mockHttpClient())
            everySuspend { tileCache.get(any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any()) } returns Unit

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNotNull(result)
            verifySuspend { tileCache.put(5, 1, 0, any()) }
        }

    @Test
    fun returnsNullOnNetworkFailureWithCacheMiss() =
        runTest {
            val failingClient =
                HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            throw RuntimeException("Network error")
                        }
                    }
                }
            createProvider(failingClient)
            everySuspend { tileCache.get(any(), any(), any()) } returns null

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNull(result)
            verifySuspend(VerifyMode.not) { tileCache.put(any(), any(), any(), any()) }
        }

    @Test
    fun passesCorrectCoordinatesToCache() =
        runTest {
            createProvider(mockHttpClient())
            everySuspend { tileCache.get(any(), any(), any()) } returns tileBytes

            provider.getTileStream(row = 42, col = 7, zoomLvl = 12)

            verifySuspend { tileCache.get(12, 7, 42) }
        }
}
