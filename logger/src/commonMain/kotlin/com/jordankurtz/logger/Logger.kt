package com.jordankurtz.logger

object Logger {
    private val writers = mutableListOf<LogWriter>()

    fun addWriter(writer: LogWriter) {
        writers.add(writer)
    }

    fun clearWriters() {
        writers.clear()
    }

    fun v(message: String) = log(VERBOSE, message)
    fun d(message: String) = log(DEBUG, message)
    fun i(message: String) = log(INFO, message)
    fun w(message: String) = log(WARN, message)
    fun e(message: String) = log(ERROR, message)
    fun e(throwable: Throwable) = log(ERROR, "", throwable)
    fun e(message: String, throwable: Throwable?) = log(ERROR, message, throwable)

    private fun log(priority: Int, message: String, throwable: Throwable? = null) {
        writers.forEach { it.log(priority, createTag(), message, throwable) }
    }

    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6
}
