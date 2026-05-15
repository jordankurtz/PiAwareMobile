package com.jordankurtz.piawaremobile.settings.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import com.jordankurtz.piawaremobile.map.offline.DownloadStatus
import com.jordankurtz.piawaremobile.map.offline.OfflineRegion
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.createTempFile
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class OfflineMapsScreenTest {
    @Test
    fun placeholderIconShownWhenThumbnailPathIsNull() =
        runComposeUiTest {
            val region =
                OfflineRegion(
                    id = 1L,
                    name = "Test Region",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 40.0,
                    maxLat = 41.0,
                    minLon = -75.0,
                    maxLon = -74.0,
                    providerId = "osm",
                    createdAt = 0L,
                    status = DownloadStatus.COMPLETE,
                    thumbnailPath = null,
                )
            setContent {
                OfflineRegionItem(
                    region = region,
                    onDelete = {},
                    onRetry = {},
                    onCancel = {},
                )
            }
            onNodeWithContentDescription("Map thumbnail placeholder").assertIsDisplayed()
        }

    @Test
    fun asyncImageShownWhenThumbnailPathIsSet() =
        runComposeUiTest {
            val tmpFile = createTempFile(suffix = ".png").toFile()
            val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
            ImageIO.write(img, "png", tmpFile)

            val region =
                OfflineRegion(
                    id = 2L,
                    name = "Test Region With Thumbnail",
                    minZoom = 8,
                    maxZoom = 14,
                    minLat = 40.0,
                    maxLat = 41.0,
                    minLon = -75.0,
                    maxLon = -74.0,
                    providerId = "osm",
                    createdAt = 0L,
                    status = DownloadStatus.COMPLETE,
                    thumbnailPath = tmpFile.absolutePath,
                )
            setContent {
                OfflineRegionItem(
                    region = region,
                    onDelete = {},
                    onRetry = {},
                    onCancel = {},
                )
            }
            onNodeWithContentDescription("Map thumbnail").assertIsDisplayed()

            tmpFile.delete()
        }
}
