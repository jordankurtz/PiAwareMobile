package com.jordankurtz.piawaremobile.map.cache

/**
 * In-memory fake implementation of [CacheFileSystem] for unit testing.
 *
 * Stores file data in memory. Supports injecting exceptions to test error handling paths.
 */
class FakeCacheFileSystem : CacheFileSystem {
    private val files = mutableMapOf<String, ByteArray>()

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
    }

    override fun delete(key: String) {
        files.remove(key)
        deletedKeys.add(key)
    }

    /** Check if a key exists in the fake file system. */
    fun exists(key: String): Boolean = files.containsKey(key)
}
