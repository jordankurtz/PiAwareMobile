package com.jordankurtz.logger

interface LogWriter {
    fun log(priority: Int, tag: String, message: String, throwable: Throwable? = null)
}
