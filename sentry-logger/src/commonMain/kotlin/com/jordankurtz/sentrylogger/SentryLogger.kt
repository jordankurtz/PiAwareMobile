package com.jordankurtz.sentrylogger

import com.jordankurtz.logger.Logger
import com.jordankurtz.logger.LogWriter
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel

class SentryLogger(val dsn: String) : LogWriter {

    init {
        Sentry.init { options ->
            options.dsn = dsn
            options.sendDefaultPii = true
            options.attachThreads = true
            options.attachThreads = true
            options.attachStackTrace = true
        }
    }
    override fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (priority < Logger.ERROR) {
            return
        }

        val exception = throwable ?: Throwable(message)
        Sentry.captureException(exception) {
            it.setTag("tag", tag)
            if (message.isNotEmpty() && throwable != null) {
                it.setExtra("message", message)
            }
        }
    }
}
