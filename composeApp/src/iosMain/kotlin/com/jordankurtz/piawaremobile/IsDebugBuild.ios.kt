package com.jordankurtz.piawaremobile

import platform.Foundation.NSBundle
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform as NativePlatform

@OptIn(ExperimentalNativeApi::class)
actual val isDebugBuild: Boolean
    get() = NativePlatform.isDebugBinary || isInstalledFromTestFlight()

private fun isInstalledFromTestFlight(): Boolean {
    val receiptPath = NSBundle.mainBundle.appStoreReceiptURL?.path ?: return false
    return receiptPath.contains("sandboxReceipt")
}
