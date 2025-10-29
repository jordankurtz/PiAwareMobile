package com.jordankurtz.piawaremobile

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication

actual class UrlHandlerImpl :
    UrlHandler {
    actual override fun openUrlInternally(url: String) {
        val nsURL = NSURL.URLWithString(url) ?: return
        val safariViewController = SFSafariViewController(nsURL)
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            safariViewController,
            animated = true,
            completion = null
        )

    }

    actual override fun openUrlExternally(url: String) {
        val nsURL = NSURL.URLWithString(url)
        nsURL?.let {
            UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), {})
        }
    }
}
