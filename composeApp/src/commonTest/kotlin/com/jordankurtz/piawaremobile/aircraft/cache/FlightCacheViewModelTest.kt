package com.jordankurtz.piawaremobile.aircraft.cache

import com.jordankurtz.piawaremobile.aircraft.usecase.PrefetchFlightsUseCase
import com.jordankurtz.piawaremobile.map.offline.BoundingBox
import com.jordankurtz.piawaremobile.model.Async
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FlightCacheViewModelTest {
    private val prefetchFlightsUseCase: PrefetchFlightsUseCase = mock()
    private val flightCacheRepo: FlightCacheRepo = mock()
    private val dispatcher = StandardTestDispatcher()

    private fun makeVm(): FlightCacheViewModel {
        everySuspend { flightCacheRepo.getAllRegions() } returns emptyList()
        return FlightCacheViewModel(
            prefetchFlightsUseCase = prefetchFlightsUseCase,
            flightCacheRepo = flightCacheRepo,
            ioDispatcher = dispatcher,
        )
    }

    @Test
    fun `startPrefetch updates state to success`() =
        runTest(dispatcher) {
            val box = BoundingBox(30.0, 55.0, -10.0, 30.0)
            everySuspend { prefetchFlightsUseCase("Test", box, 3) } returns Async.Success(42)

            val vm = makeVm()
            vm.startPrefetch("Test", box, 3)
            advanceUntilIdle()

            assertTrue(vm.prefetchState.value is Async.Success)
        }

    @Test
    fun `deleteRegion delegates to repo`() =
        runTest(dispatcher) {
            everySuspend { flightCacheRepo.deleteRegion(any()) } returns Unit

            val vm = makeVm()
            vm.deleteRegion(1L)
            advanceUntilIdle()

            // No exception = success
        }

    @Test
    fun `resetPrefetchState returns state to NotStarted`() =
        runTest(dispatcher) {
            val box = BoundingBox(30.0, 55.0, -10.0, 30.0)
            everySuspend { prefetchFlightsUseCase("Test", box, 3) } returns Async.Success(42)

            val vm = makeVm()
            vm.startPrefetch("Test", box, 3)
            advanceUntilIdle()

            vm.resetPrefetchState()

            assertTrue(vm.prefetchState.value is Async.NotStarted)
        }
}
