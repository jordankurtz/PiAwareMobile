package com.jordankurtz.piawaremobile.model

sealed class Async<out T> {
    object NotStarted : Async<Nothing>()
    object Loading : Async<Nothing>()
    class Success<T>(val data: T) : Async<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Async<Nothing>()

    fun getValue(): T? {
        return when (this) {
            is Error, Loading, NotStarted -> null
            is Success<*> -> data as? T
        }
    }

}