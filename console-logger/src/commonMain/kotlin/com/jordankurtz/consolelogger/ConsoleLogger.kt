package com.jordankurtz.consolelogger

import com.jordankurtz.logger.LogWriter

expect class ConsoleLogger() : LogWriter {
    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?)
}
