package com.jordankurtz.piawaremobile

import android.content.pm.ApplicationInfo
import com.jordankurtz.logger.Logger
import org.koin.core.context.GlobalContext

/**
 * Returns true if the app was installed as a debuggable build (debug build type).
 * Reads ApplicationInfo.FLAG_DEBUGGABLE via Koin's registered application context,
 * so this must only be accessed after Koin has started (e.g. from a composable).
 */
actual val isDebugBuild: Boolean by lazy {
    try {
        val context = GlobalContext.get().get<android.content.Context>()
        context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    } catch (e: Exception) {
        Logger.e("Failed to detect debug build; defaulting to false", e)
        false
    }
}
