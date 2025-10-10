package com.jordankurtz.piawaremobile

import android.content.Context
import android.content.Intent
import android.net.Uri

actual class UrlHandler(private val context: Context) {
    actual fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // This flag is needed when starting an activity from outside an activity context
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}