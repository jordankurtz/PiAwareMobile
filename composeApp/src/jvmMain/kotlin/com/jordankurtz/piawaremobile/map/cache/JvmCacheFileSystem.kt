package com.jordankurtz.piawaremobile.map.cache

import java.io.File

/**
 * JVM implementation of [CacheFileSystem] backed by [java.io.File].
 * Used for both Android and Desktop targets.
 */
class JvmCacheFileSystem(private val cacheDir: File) : CacheFileSystem {
    override fun read(key: String): ByteArray? {
        val file = File(cacheDir, key)
        if (!file.exists()) return null
        return file.readBytes()
    }

    override fun write(
        key: String,
        data: ByteArray,
    ) {
        val file = File(cacheDir, key)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    override fun delete(key: String) {
        File(cacheDir, key).delete()
    }
}
