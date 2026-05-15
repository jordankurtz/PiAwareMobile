package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

class IosThumbnailGenerator(
    private val tileCacheDir: String,
    private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {

    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    override suspend fun generate(
        bounds: BoundingBox,
        providerId: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean = withContext(ioDispatcher) {
        val (colMin, rowMin) = latLonToTile(bounds.maxLat, bounds.minLon, thumbnailZoom)
        val (colMax, rowMax) = latLonToTile(bounds.minLat, bounds.maxLon, thumbnailZoom)
        val gridW = colMax - colMin + 1
        val gridH = rowMax - rowMin + 1
        val stitchedW = (gridW * TILE_PX).toDouble()
        val stitchedH = (gridH * TILE_PX).toDouble()

        // Build stitched image
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(stitchedW, stitchedH), false, 1.0)
        var allTilesPresent = true
        for (col in colMin..colMax) {
            for (row in rowMin..rowMax) {
                val tilePath = "$tileCacheDir/$providerId/$thumbnailZoom/$col/$row.png"
                val data = NSData.dataWithContentsOfFile(tilePath)
                val tile = data?.let { UIImage.imageWithData(it) }
                if (tile == null) {
                    allTilesPresent = false
                    break
                }
                val x = ((col - colMin) * TILE_PX).toDouble()
                val y = ((row - rowMin) * TILE_PX).toDouble()
                tile.drawInRect(CGRectMake(x, y, TILE_PX.toDouble(), TILE_PX.toDouble()))
            }
            if (!allTilesPresent) break
        }
        val stitched = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        if (!allTilesPresent || stitched == null) return@withContext false

        // Scale to OUTPUT_PX × OUTPUT_PX
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(OUTPUT_PX.toDouble(), OUTPUT_PX.toDouble()), false, 1.0)
        stitched.drawInRect(CGRectMake(0.0, 0.0, OUTPUT_PX.toDouble(), OUTPUT_PX.toDouble()))
        val thumbnail = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        thumbnail ?: return@withContext false

        val pngData = UIImagePNGRepresentation(thumbnail) ?: return@withContext false

        // Ensure output directory exists
        val outDir = outputPath.substringBeforeLast("/")
        NSFileManager.defaultManager.createDirectoryAtPath(
            outDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
        pngData.writeToFile(outputPath, atomically = true)
        true
    }

    private companion object {
        const val TILE_PX = 256
        const val OUTPUT_PX = 256
    }
}
