package com.jordankurtz.piawaremobile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {

    companion object {
        internal var activity: Activity? = null
    }

    init {
        activity = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

actual fun _openUrl(url: String) {
    MainActivity.activity?.startActivity(
        Intent(
            Intent.ACTION_VIEW, Uri.parse(url)
        )
    )
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}