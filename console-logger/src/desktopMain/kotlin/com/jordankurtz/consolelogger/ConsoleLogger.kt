package com.jordankurtz.consolelogger

import com.jordankurtz.logger.LogWriter

actual class ConsoleLogger : LogWriter {
    actual override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        println("[$tag] $message")
        throwable?.let { println(it.stackTraceToString()) }
    }
}
