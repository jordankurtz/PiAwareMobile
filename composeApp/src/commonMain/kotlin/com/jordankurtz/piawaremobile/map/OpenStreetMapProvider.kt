package com.jordankurtz.piawaremobile.map

import com.jordankurtz.logger.Logger
import io.ktor.client.HttpClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.readByteArray
import org.koin.core.annotation.Single
import ovh.plrapps.mapcompose.core.TileStreamProvider

@Single(binds = [TileStreamProvider::class])
class OpenStreetMapProvider(
    private val httpClient: HttpClient,
    private val maxCacheEntries: Int = DEFAULT_MAX_CACHE_ENTRIES,
    private val fetchTile: suspend (HttpClient, String) -> RawSource = ::getStream,
) : TileStreamProvider {
    private val cache = linkedMapOf<String, ByteArray>()
    private val mutex = Mutex()

    override suspend fun getTileStream(
        row: Int,
        col: Int,
        zoomLvl: Int,
    ): RawSource? {
        val key = "$zoomLvl/$col/$row"
        val url = "https://tile.openstreetmap.org/$zoomLvl/$col/$row.png"

        mutex.withLock {
            cache[key]?.let { cached ->
                // Move to end (most recently used) by reinserting
                cache.remove(key)
                cache[key] = cached
                return Buffer().apply { write(cached) }
            }
        }

        return try {
            val source = fetchTile(httpClient, url)
            val buffer = Buffer()
            source.use { raw ->
                while (raw.readAtMostTo(buffer, Long.MAX_VALUE) != -1L) {
                    // read until exhausted
                }
            }
            val bytes = buffer.readByteArray()
            mutex.withLock {
                cache[key] = bytes
                while (cache.size > maxCacheEntries) {
                    val evictKey = cache.keys.first()
                    cache.remove(evictKey)
                }
            }
            Buffer().apply { write(bytes) }
        } catch (e: Exception) {
            Logger.e("Failed to load tile", e)
            null
        }
    }

    companion object {
        const val DEFAULT_MAX_CACHE_ENTRIES = 256
    }
}
