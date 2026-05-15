package com.jordankurtz.piawaremobile.map.offline

import java.io.File

class AndroidThumbnailFileManager(
    private val thumbnailCacheDir: File,
) : ThumbnailFileManager {
    override fun thumbnailPath(regionId: Long): String =
        File(thumbnailCacheDir, "$regionId.png").absolutePath

    override fun delete(regionId: Long) {
        File(thumbnailCacheDir, "$regionId.png").delete()
    }

    override fun exists(path: String): Boolean = File(path).exists()
}
