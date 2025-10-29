package com.jordankurtz.piawaremobile

interface UrlHandler {
    fun openUrlInternally(url: String)
    fun openUrlExternally(url: String)
}

expect class UrlHandlerImpl: UrlHandler {
    override fun openUrlInternally(url: String)
    override fun openUrlExternally(url: String)
}
