package com.jordankurtz.piawaremobile

interface UrlHandler {
    fun openUrl(url: String)
}

expect class UrlHandlerImpl: UrlHandler {
    override fun openUrl(url: String)
}
