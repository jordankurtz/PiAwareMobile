package com.jordankurtz.piawaremobile

expect fun _openUrl(url: String)

class UrlHandler {
    fun openUrl(url: String) {
        _openUrl(url)
    }
}