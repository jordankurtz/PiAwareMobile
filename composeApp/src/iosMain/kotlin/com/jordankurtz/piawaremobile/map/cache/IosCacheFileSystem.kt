package com.jordankurtz.piawaremobile.map.cache

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.io.IOException
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.writeToFile
import platform.posix.memcpy

/**
 * iOS implementation of [CacheFileSystem] backed by `NSFileManager` and `NSData`.
 *
 * @param cacheDir Absolute path to the cache root directory
 *   (typically `NSCachesDirectory/map_tiles/`).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosCacheFileSystem(private val cacheDir: String) : CacheFileSystem {
    private val fileManager: NSFileManager = NSFileManager()

    private fun fullPath(key: String): String {
        // Build path by appending each component
        var path = cacheDir
        for (component in key.split("/")) {
            @Suppress("CAST_NEVER_SUCCEEDS")
            path = (path as NSString).stringByAppendingPathComponent(component)
        }
        return path
    }

    private fun parentPath(path: String): String {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else cacheDir
    }

    override fun read(key: String): ByteArray? {
        val path = fullPath(key)
        if (!fileManager.fileExistsAtPath(path)) return null
        val data = NSData.dataWithContentsOfFile(path) ?: return null
        return data.toByteArray()
    }

    override fun write(
        key: String,
        data: ByteArray,
    ) {
        val path = fullPath(key)
        val parent = parentPath(path)
        val dirCreated =
            fileManager.createDirectoryAtPath(
                parent,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        if (!dirCreated) {
            throw IOException("Failed to create directory: $parent")
        }
        val nsData = data.toNSData()
        val written = nsData.writeToFile(path, atomically = true)
        if (!written) {
            throw IOException("Failed to write file: $path")
        }
    }

    override fun delete(key: String) {
        val path = fullPath(key)
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        if (size == 0) return byteArrayOf()
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
        return bytes
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
}
