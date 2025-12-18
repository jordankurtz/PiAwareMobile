package com.jordankurtz.logger

internal actual fun createTag(): String {
    return Throwable().stackTrace
        .first { it.className !in listOf(
            "com.jordankurtz.logger.Logger",
            "com.jordankurtz.logger.LoggerKt",
            "com.jordankurtz.logger.TagKt"
        ) }
        .let { stack -> stack.className.substringAfterLast('.') }
}
