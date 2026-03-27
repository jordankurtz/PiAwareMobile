package com.jordankurtz.piawaremobile.map.cache

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringByAppendingPathComponent
import platform.Foundation.timeIntervalSince1970
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
    private val fileManager: NSFileManager get() = NSFileManager.defaultManager

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
        fileManager.createDirectoryAtPath(
            parent,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        val nsData = data.toNSData()
        nsData.writeToFile(path, atomically = true)
    }

    override fun delete(key: String) {
        val path = fullPath(key)
        if (fileManager.fileExistsAtPath(path)) {
            fileManager.removeItemAtPath(path, error = null)
        }
    }

    override fun list(): List<String> {
        if (!fileManager.fileExistsAtPath(cacheDir)) return emptyList()
        val enumerator = fileManager.enumeratorAtPath(cacheDir) ?: return emptyList()
        return generateSequence { enumerator.nextObject()?.toString() }
            .filter { relativePath ->
                val path = fullPath(relativePath)
                val attrs = fileManager.attributesOfItemAtPath(path, error = null)
                val fileType = attrs?.get("NSFileType")
                fileType?.toString() != "NSFileTypeDirectory"
            }
            .toList()
    }

    override fun lastModified(key: String): Long {
        val path = fullPath(key)
        if (!fileManager.fileExistsAtPath(path)) return -1L
        val attrs = fileManager.attributesOfItemAtPath(path, error = null) ?: return -1L
        val date = attrs[NSFileModificationDate] as? NSDate ?: return -1L
        return (date.timeIntervalSince1970 * 1000).toLong()
    }

    override fun setLastModified(
        key: String,
        timeMs: Long,
    ) {
        val path = fullPath(key)
        if (!fileManager.fileExistsAtPath(path)) {
            // Create the file (and parent dirs) if it doesn't exist
            val parent = parentPath(path)
            fileManager.createDirectoryAtPath(
                parent,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            fileManager.createFileAtPath(path, contents = null, attributes = null)
        }
        // NSDate reference date is 2001-01-01, Unix epoch offset is 978307200 seconds
        val timeIntervalSinceRefDate = (timeMs / 1000.0) - NSDATE_REFERENCE_DATE_OFFSET
        val date = NSDate(timeIntervalSinceReferenceDate = timeIntervalSinceRefDate)
        val attrs = mapOf<Any?, Any?>(NSFileModificationDate to date)
        fileManager.setAttributes(attrs, ofItemAtPath = path, error = null)
    }

    override fun sizeBytes(): Long {
        if (!fileManager.fileExistsAtPath(cacheDir)) return 0L
        val enumerator = fileManager.enumeratorAtPath(cacheDir) ?: return 0L
        return generateSequence { enumerator.nextObject()?.toString() }
            .filter { it.endsWith(".png") }
            .sumOf { relativePath ->
                val path = fullPath(relativePath)
                val attrs = fileManager.attributesOfItemAtPath(path, error = null)
                val size = attrs?.get(NSFileSize)
                size?.toString()?.toLongOrNull() ?: 0L
            }
    }

    companion object {
        /** Seconds between Unix epoch (1970-01-01) and NSDate reference date (2001-01-01). */
        private const val NSDATE_REFERENCE_DATE_OFFSET = 978307200.0
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
