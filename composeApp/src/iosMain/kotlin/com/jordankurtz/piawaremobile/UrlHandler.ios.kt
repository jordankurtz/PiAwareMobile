package com.jordankurtz.piawaremobile

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class UrlHandlerImpl: UrlHandler {
    actual override fun openUrl(url: String) {
        val nsURL = NSURL.URLWithString(url)
        nsURL?.let {
            UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), {})
        }
    }
}
