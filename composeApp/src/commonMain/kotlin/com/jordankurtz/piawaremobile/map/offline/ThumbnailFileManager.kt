package com.jordankurtz.piawaremobile.map.offline

interface ThumbnailFileManager {
    /** Returns the absolute path where the thumbnail for [regionId] should be written. */
    fun thumbnailPath(regionId: Long): String

    /** Deletes the thumbnail file for [regionId] if it exists. */
    fun delete(regionId: Long)

    /** Returns true if the file at [path] exists on disk. */
    fun exists(path: String): Boolean
}
