package com.jordankurtz.piawaremobile.map.cache

/**
 * In-memory fake implementation of [CacheFileSystem] for unit testing.
 *
 * Stores file data, last-modified timestamps, and tracks size of `.png` files.
 * Supports injecting exceptions to test error handling paths.
 */
class FakeCacheFileSystem : CacheFileSystem {
    private val files = mutableMapOf<String, ByteArray>()
    private val modifiedTimes = mutableMapOf<String, Long>()

    /** When true, [read] will throw an exception. */
    var throwOnRead: Boolean = false

    /** When true, [write] will throw an exception. */
    var throwOnWrite: Boolean = false

    /** Track deleted keys for assertion purposes. */
    val deletedKeys = mutableListOf<String>()

    override fun read(key: String): ByteArray? {
        if (throwOnRead) throw RuntimeException("Simulated read failure")
        return files[key]?.copyOf()
    }

    override fun write(
        key: String,
        data: ByteArray,
    ) {
        if (throwOnWrite) throw RuntimeException("Simulated write failure")
        files[key] = data.copyOf()
        // Set modification time to current time if not already set
        if (!modifiedTimes.containsKey(key)) {
            modifiedTimes[key] = kotlin.time.Clock.System.now().toEpochMilliseconds()
        }
    }

    override fun delete(key: String) {
        files.remove(key)
        modifiedTimes.remove(key)
        deletedKeys.add(key)
    }

    override fun list(): List<String> = files.keys.toList()

    override fun lastModified(key: String): Long =
        if (files.containsKey(key)) {
            modifiedTimes[key] ?: -1L
        } else {
            -1L
        }

    override fun setLastModified(
        key: String,
        timeMs: Long,
    ) {
        if (!files.containsKey(key)) {
            // Create the file if it doesn't exist (matches real implementations)
            files[key] = byteArrayOf()
        }
        modifiedTimes[key] = timeMs
    }

    override fun fileSize(key: String): Long = files[key]?.size?.toLong() ?: 0L

    override fun sizeBytes(): Long =
        files.entries
            .filter { it.key.endsWith(".png") }
            .sumOf { it.value.size.toLong() }

    /** Set the modification time for a key that already exists. */
    fun setModifiedTime(
        key: String,
        timeMs: Long,
    ) {
        modifiedTimes[key] = timeMs
    }

    /** Check if a key exists in the fake file system. */
    fun exists(key: String): Boolean = files.containsKey(key)

    /** Get the current modification time of a key. */
    fun getModifiedTime(key: String): Long = modifiedTimes[key] ?: -1L
}
