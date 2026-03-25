package com.jordankurtz.piawaremobile.extensions

import app.cash.turbine.test
import com.jordankurtz.piawaremobile.model.Async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CoroutineExtensionsTest {
    @Test
    fun asyncEmitsLoadingThenSuccess() =
        runTest {
            flowOf("data").async().test {
                assertIs<Async.Loading>(awaitItem())
                val success = awaitItem()
                assertIs<Async.Success<String>>(success)
                assertEquals("data", success.data)
                awaitComplete()
            }
        }

    @Test
    fun asyncEmitsLoadingThenError() =
        runTest {
            flow<String> { throw RuntimeException("fail") }.async().test {
                assertIs<Async.Loading>(awaitItem())
                val error = awaitItem()
                assertIs<Async.Error>(error)
                assertEquals("fail", error.message)
                awaitComplete()
            }
        }

    @Test
    fun asyncEmitsMultipleSuccesses() =
        runTest {
            flowOf("a", "b", "c").async().test {
                assertIs<Async.Loading>(awaitItem())
                assertEquals("a", (awaitItem() as Async.Success).data)
                assertEquals("b", (awaitItem() as Async.Success).data)
                assertEquals("c", (awaitItem() as Async.Success).data)
                awaitComplete()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun stateInStartsWithNotStartedAndEmitsValues() =
        runTest(UnconfinedTestDispatcher()) {
            val source = MutableSharedFlow<Async<String>>()
            val state = source.stateIn(backgroundScope)

            assertIs<Async.NotStarted>(state.value)

            val collected = mutableListOf<Async<String>>()
            val job = launch { state.collect { collected.add(it) } }

            source.emit(Async.Success("hello"))
            assertEquals(Async.Success("hello"), state.value)

            job.cancel()
        }
}
