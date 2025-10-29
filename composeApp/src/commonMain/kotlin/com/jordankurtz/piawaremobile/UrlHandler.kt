package com.jordankurtz.piawaremobile

import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import org.koin.core.annotation.Factory

interface UrlHandler {
    fun openUrlInternally(url: String)
    fun openUrlExternally(url: String)
}

@Factory(binds = [UrlHandler::class])
expect class UrlHandlerImpl(contextWrapper: ContextWrapper): UrlHandler {
    override fun openUrlInternally(url: String)
    override fun openUrlExternally(url: String)
}
