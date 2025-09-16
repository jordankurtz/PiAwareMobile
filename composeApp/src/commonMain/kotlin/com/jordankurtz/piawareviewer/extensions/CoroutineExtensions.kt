package com.jordankurtz.piawareviewer.extensions

import com.jordankurtz.piawareviewer.model.Async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

fun <T> Flow<T>.async(): Flow<Async<T>> = flow {
    emit(Async.Loading)
    try {
        collect { emit(Async.Success(it)) }
    } catch (exception: Exception) {
        emit(Async.Error(exception.message.toString(), exception))
    }
}

fun <T> Flow<Async<T>>.stateIn(scope: CoroutineScope): StateFlow<Async<T>> = stateIn(
    scope,
    SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
    Async.NotStarted
)