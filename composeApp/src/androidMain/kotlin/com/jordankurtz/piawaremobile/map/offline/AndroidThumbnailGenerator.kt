package com.jordankurtz.piawaremobile.map.offline

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidThumbnailGenerator(
    private val tileCacheDir: File,
    private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {

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

        val stitched = Bitmap.createBitmap(gridW * TILE_PX, gridH * TILE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(stitched)

        for (col in colMin..colMax) {
            for (row in rowMin..rowMax) {
                val tileFile = File(tileCacheDir, "$providerId/$thumbnailZoom/$col/$row.png")
                if (!tileFile.exists()) {
                    stitched.recycle()
                    return@withContext false
                }
                val tile = BitmapFactory.decodeFile(tileFile.absolutePath) ?: run {
                    stitched.recycle()
                    return@withContext false
                }
                canvas.drawBitmap(tile, ((col - colMin) * TILE_PX).toFloat(), ((row - rowMin) * TILE_PX).toFloat(), null)
                tile.recycle()
            }
        }

        val thumbnail = Bitmap.createScaledBitmap(stitched, OUTPUT_PX, OUTPUT_PX, true)
        stitched.recycle()

        File(outputPath).also { it.parentFile?.mkdirs() }.let { outFile ->
            FileOutputStream(outFile).use { out -> thumbnail.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }
        thumbnail.recycle()
        true
    }

    private companion object {
        const val TILE_PX = 256
        const val OUTPUT_PX = 256
    }
}
