package com.jordankurtz.piawaremobile.map.offline

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class DesktopThumbnailGenerator(
    private val tileCacheDir: File,
    private val ioDispatcher: CoroutineDispatcher,
) : ThumbnailGenerator {
    override suspend fun generate(
        bounds: BoundingBox,
        providerId: String,
        thumbnailZoom: Int,
        outputPath: String,
    ): Boolean =
        withContext(ioDispatcher) {
            val (colMin, rowMin) = latLonToTile(bounds.maxLat, bounds.minLon, thumbnailZoom)
            val (colMax, rowMax) = latLonToTile(bounds.minLat, bounds.maxLon, thumbnailZoom)
            val gridW = colMax - colMin + 1
            val gridH = rowMax - rowMin + 1

            val stitched = BufferedImage(gridW * TILE_PX, gridH * TILE_PX, BufferedImage.TYPE_INT_ARGB)
            val g = stitched.createGraphics()

            for (col in colMin..colMax) {
                for (row in rowMin..rowMax) {
                    val tileFile = File(tileCacheDir, "$providerId/$thumbnailZoom/$col/$row.png")
                    if (!tileFile.exists()) {
                        g.dispose()
                        return@withContext false
                    }
                    val tile =
                        ImageIO.read(tileFile) ?: run {
                            g.dispose()
                            return@withContext false
                        }
                    g.drawImage(tile, (col - colMin) * TILE_PX, (row - rowMin) * TILE_PX, null)
                }
            }
            g.dispose()

            val thumbnail = BufferedImage(OUTPUT_PX, OUTPUT_PX, BufferedImage.TYPE_INT_ARGB)
            val g2 = thumbnail.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.drawImage(stitched, 0, 0, OUTPUT_PX, OUTPUT_PX, null)
            g2.dispose()

            val outFile = File(outputPath).also { it.parentFile?.mkdirs() }
            ImageIO.write(thumbnail, "PNG", outFile)
            true
        }

    private companion object {
        const val TILE_PX = 256
        const val OUTPUT_PX = 256
    }
}
