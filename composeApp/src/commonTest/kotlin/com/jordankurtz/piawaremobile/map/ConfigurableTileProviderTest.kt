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
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurableTileProviderTest {
    private val tileBytes = byteArrayOf(89, 80, 78, 71, 13, 10, 26, 10)
    private lateinit var tileCache: TileCache

    private fun mockHttpClient(
        responseBytes: ByteArray = tileBytes,
        captureUrls: MutableList<String>? = null,
    ): HttpClient =
        HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    captureUrls?.add(request.url.toString())
                    respond(
                        content = ByteReadChannel(responseBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }
            }
        }

    private fun createProvider(
        config: TileProviderConfig = TileProviders.OPENSTREETMAP,
        httpClient: HttpClient = mockHttpClient(),
    ): ConfigurableTileProvider {
        tileCache = mock()
        return ConfigurableTileProvider(httpClient, tileCache, MutableStateFlow(config))
    }

    private fun readAllBytes(source: kotlinx.io.RawSource): ByteArray {
        val buffer = Buffer()
        source.use { raw ->
            while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {}
        }
        return buffer.readByteArray()
    }

    @Test
    fun returnsCachedTileOnCacheHit() =
        runTest {
            val provider = createProvider()
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns tileBytes

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNotNull(result)
            assertContentEquals(tileBytes, readAllBytes(result))
            verifySuspend(VerifyMode.not) { tileCache.put(any(), any(), any(), any(), any()) }
        }

    @Test
    fun fetchesFromNetworkAndCachesOnCacheMiss() =
        runTest {
            val provider = createProvider()
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any(), any()) } returns Unit

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNotNull(result)
            verifySuspend { tileCache.put(5, 1, 0, TileProviders.OPENSTREETMAP.id, any()) }
        }

    @Test
    fun returnsNullOnNetworkFailure() =
        runTest {
            val failingClient =
                HttpClient(MockEngine) {
                    engine { addHandler { throw RuntimeException("Network error") } }
                }
            val provider = createProvider(httpClient = failingClient)
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns null

            val result = provider.getTileStream(row = 0, col = 1, zoomLvl = 5)

            assertNull(result)
            verifySuspend(VerifyMode.not) { tileCache.put(any(), any(), any(), any(), any()) }
        }

    @Test
    fun usesOsmUrlForOsmProvider() =
        runTest {
            val urls = mutableListOf<String>()
            val provider =
                createProvider(
                    config = TileProviders.OPENSTREETMAP,
                    httpClient = mockHttpClient(captureUrls = urls),
                )
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any(), any()) } returns Unit

            provider.getTileStream(row = 1, col = 2, zoomLvl = 3)

            assertTrue(urls.single().contains("tile.openstreetmap.org/3/2/1.png"))
        }

    @Test
    fun esriUrlPlacesRowBeforeCol() =
        runTest {
            val urls = mutableListOf<String>()
            val provider =
                createProvider(
                    config = TileProviders.ESRI_SATELLITE,
                    httpClient = mockHttpClient(captureUrls = urls),
                )
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any(), any()) } returns Unit

            provider.getTileStream(row = 1, col = 2, zoomLvl = 3)

            // ESRI template: tile/{z}/{y}/{x} → row=1, col=2 → tile/3/1/2
            assertTrue(urls.single().contains("MapServer/tile/3/1/2"))
        }

    @Test
    fun cartoUrlContainsValidSubdomain() =
        runTest {
            val urls = mutableListOf<String>()
            val provider =
                createProvider(
                    config = TileProviders.CARTO_DARK_ALL,
                    httpClient = mockHttpClient(captureUrls = urls),
                )
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns null
            everySuspend { tileCache.put(any(), any(), any(), any(), any()) } returns Unit

            provider.getTileStream(row = 1, col = 2, zoomLvl = 3)

            val url = urls.single()
            assertTrue(
                TileProviders.CARTO_DARK_ALL.subdomains.any { subdomain ->
                    url.contains("$subdomain.basemaps.cartocdn.com")
                },
            )
        }

    @Test
    fun passesCorrectProviderIdToCacheForEsri() =
        runTest {
            val provider = createProvider(config = TileProviders.ESRI_SATELLITE)
            everySuspend { tileCache.get(any(), any(), any(), any()) } returns tileBytes

            provider.getTileStream(row = 42, col = 7, zoomLvl = 12)

            verifySuspend { tileCache.get(12, 7, 42, TileProviders.ESRI_SATELLITE.id) }
        }
}
