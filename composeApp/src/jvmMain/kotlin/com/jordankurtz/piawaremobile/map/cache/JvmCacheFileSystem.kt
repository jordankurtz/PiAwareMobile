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

    override fun write(key: String, data: ByteArray) {
        val file = File(cacheDir, key)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }

    override fun delete(key: String) {
        File(cacheDir, key).delete()
    }

    override fun list(): List<String> =
        cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(cacheDir).path }
            .toList()

    override fun lastModified(key: String): Long {
        val file = File(cacheDir, key)
        if (!file.exists()) return -1L
        return file.lastModified()
    }

    override fun setLastModified(key: String, timeMs: Long) {
        val file = File(cacheDir, key)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }
        file.setLastModified(timeMs)
    }

    override fun sizeBytes(): Long =
        cacheDir.walkTopDown()
            .filter { it.isFile && it.extension == "png" }
            .sumOf { it.length() }
}
