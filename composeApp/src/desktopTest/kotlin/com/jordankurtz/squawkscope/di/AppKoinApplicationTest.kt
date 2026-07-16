package com.jordankurtz.squawkscope.di

import com.jordankurtz.squawkscope.aircraft.usecase.GetAircraftWithDetailsUseCase
import com.jordankurtz.squawkscope.map.cache.TileCache
import com.jordankurtz.squawkscope.map.offline.OfflineTileStore
import com.jordankurtz.squawkscope.settings.usecase.LoadSettingsUseCase
import com.jordankurtz.squawkscope.settings.usecase.SettingsService
import org.koin.core.context.stopKoin
import org.koin.plugin.module.dsl.startKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Boots the real, annotation-scanned Koin graph (via the koin-compiler-plugin) and resolves a
 * representative sample of leaf dependencies across the app's DI modules. Compilation alone
 * doesn't guarantee the compiler plugin actually wired every @Single/@ComponentScan binding
 * correctly — only a real startKoin() + get() catches a missing definition.
 */
class AppKoinApplicationTest {
    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun appGraphResolvesRepresentativeDependencies() {
        val koin = startKoin<AppKoinApplication>().koin

        assertNotNull(koin.getOrNull<SettingsService>(), "SettingsService should resolve")
        assertNotNull(koin.getOrNull<LoadSettingsUseCase>(), "LoadSettingsUseCase should resolve")
        assertNotNull(koin.getOrNull<TileCache>(), "TileCache should resolve")
        assertNotNull(koin.getOrNull<OfflineTileStore>(), "OfflineTileStore should resolve")
        assertNotNull(
            koin.getOrNull<GetAircraftWithDetailsUseCase>(),
            "GetAircraftWithDetailsUseCase should resolve",
        )
    }
}
