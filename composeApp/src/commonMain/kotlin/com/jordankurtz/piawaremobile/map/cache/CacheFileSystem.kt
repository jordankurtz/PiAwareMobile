package com.jordankurtz.piawaremobile.map.cache

/**
 * Abstraction over file-system operations needed by [FileTileCache].
 *
 * Keys use relative paths with the convention `{provider_id}/{zoom}/{col}/{row}.png`.
 * Implementations handle directory creation internally on [write].
 *
 * Metadata tracking (size, timestamps, LRU order) is handled by the database,
 * so this interface only provides raw byte operations.
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
}
