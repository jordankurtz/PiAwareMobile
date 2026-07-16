package com.jordankurtz.squawkscope.model

@Suppress("UNCHECKED_CAST")
sealed class Async<out T> {
    object NotStarted : Async<Nothing>()

    object Loading : Async<Nothing>()

    data class Success<T>(
        val data: T,
    ) : Async<T>()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : Async<Nothing>()

    fun getValue(): T? =
        when (this) {
            is Error, Loading, NotStarted -> null
            is Success<*> -> data as? T
        }
}
