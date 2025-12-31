package com.jordankurtz.piawaremobile

import com.jordankurtz.logger.Logger
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import org.koin.core.annotation.Factory
import java.awt.Desktop
import java.net.URI

@Factory(binds = [UrlHandler::class])
actual class UrlHandlerImpl actual constructor(private val contextWrapper: ContextWrapper) :
    UrlHandler {
    actual override fun openUrlInternally(url: String) {
        open(url)
    }

    actual override fun openUrlExternally(url: String) {
        open(url)
    }

    private fun open(url: String) {
        try {
            Desktop.getDesktop().browse(URI(url))
        } catch (e: Exception) {
            Logger.e("Failed to open URL: $url", e)
        }
    }
}
