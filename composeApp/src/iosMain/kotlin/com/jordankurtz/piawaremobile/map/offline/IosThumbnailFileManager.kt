package com.jordankurtz.piawaremobile.map.offline

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
class IosThumbnailFileManager(
    private val thumbnailCacheDir: String,
) : ThumbnailFileManager {
    override fun thumbnailPath(regionId: Long): String = "$thumbnailCacheDir/$regionId.png"

    override fun delete(regionId: Long) {
        NSFileManager.defaultManager.removeItemAtPath(thumbnailPath(regionId), error = null)
    }

    override fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)
}
