package com.jordankurtz.consolelogger

import android.util.Log
import com.jordankurtz.logger.LogWriter

actual class ConsoleLogger : LogWriter {
    actual override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        Log.println(priority, tag, message)
        throwable?.let { Log.println(priority, tag, Log.getStackTraceString(it)) }
    }
}
