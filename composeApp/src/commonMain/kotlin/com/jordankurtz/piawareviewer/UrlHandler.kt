package com.jordankurtz.piawareviewer

expect fun _openUrl(url: String)

class UrlHandler {
    fun openUrl(url: String) {
        _openUrl(url)
    }
}