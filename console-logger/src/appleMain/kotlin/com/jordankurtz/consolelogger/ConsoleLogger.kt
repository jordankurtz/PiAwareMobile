package com.jordankurtz.consolelogger

import com.jordankurtz.logger.LogWriter
import platform.Foundation.NSLog

actual class ConsoleLogger : LogWriter {
    actual override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        NSLog("%s: %s", tag, message)
        throwable?.let { NSLog("%s", it.stackTraceToString()) }
    }
}
