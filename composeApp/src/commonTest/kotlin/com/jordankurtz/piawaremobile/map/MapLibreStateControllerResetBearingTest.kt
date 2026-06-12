package com.jordankurtz.piawaremobile.map

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MapLibreStateControllerResetBearingTest {
    @Test
    fun resetBearing_isNoopWhenCameraStateNotSet() =
        runTest {
            val controller = MapLibreStateController()
            // cameraState is null by default — must not throw
            controller.resetBearing()
        }
}
