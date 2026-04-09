package com.jordankurtz.piawaremobile.map.offline

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.jordankurtz.piawaremobile.di.modules.ContextWrapper
import org.koin.core.annotation.Single

@Single(binds = [NotificationPermissionService::class])
class AndroidNotificationPermissionService(
    private val contextWrapper: ContextWrapper,
) : NotificationPermissionService {
    var permissionLauncher: (() -> Unit)? = null

    override fun requestIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted =
            ActivityCompat.checkSelfPermission(
                contextWrapper.context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher?.invoke()
        }
    }
}
