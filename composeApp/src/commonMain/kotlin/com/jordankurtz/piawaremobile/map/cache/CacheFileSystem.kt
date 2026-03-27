package com.jordankurtz.piawaremobile.map.cache

/**
 * Abstraction over file-system operations needed by [FileTileCache].
 *
 * Keys use relative paths with the convention `{zoom}/{col}/{row}.png`
 * for tile data and `{zoom}/{col}/{row}.access` for LRU sidecar files.
 * Implementations handle directory creation internally on [write].
 */
interface CacheFileSystem {
    /** Read file contents for [key], or null if the file does not exist. */
    fun read(key: String): ByteArray?

    /** Write [data] to [key], creating parent directories as needed. */
    fun write(
        key: String,
        data: ByteArray,
    )

    /** Delete the file at [key]. No-op if it does not exist. */
    fun delete(key: String)

    /** Return all cache keys (relative paths) for files under the cache root. */
    fun list(): List<String>

    /** Return the last-modified time in epoch milliseconds for [key], or -1 if not found. */
    fun lastModified(key: String): Long

    /** Set the last-modified time for [key]. Creates the file if it does not exist. */
    fun setLastModified(
        key: String,
        timeMs: Long,
    )

    /** Total size in bytes of all `.png` files in the cache. */
    fun sizeBytes(): Long
}
