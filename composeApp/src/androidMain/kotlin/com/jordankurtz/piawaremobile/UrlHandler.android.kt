package com.jordankurtz.piawaremobile

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import org.koin.core.annotation.Factory

@Factory(binds = [UrlHandler::class])
actual class UrlHandlerImpl actual constructor(private val contextWrapper: ContextWrapper) : UrlHandler {
    actual override fun openUrlInternally(url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(contextWrapper.context, Uri.parse(url))
    }

    actual override fun openUrlExternally(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        contextWrapper.context.startActivity(intent)
    }
}
