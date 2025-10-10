package com.jordankurtz.piawaremobile

import java.awt.Desktop
import java.net.URI

actual class UrlHandler {
    actual fun openUrl(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: Exception) {
            println("Failed to open URL: $url with error: ${e.message}")
        }
    }
}
