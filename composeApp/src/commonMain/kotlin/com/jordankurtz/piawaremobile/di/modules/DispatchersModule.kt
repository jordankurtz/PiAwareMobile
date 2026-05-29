package com.jordankurtz.piawaremobile.di.modules

import com.jordankurtz.piawaremobile.di.annotations.IODispatcher
import com.jordankurtz.piawaremobile.di.annotations.MainDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@Configuration
object DispatchersModule {
    @Single
    @IODispatcher
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Single
    @MainDispatcher
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Single
    fun providesApplicationScope(
        @IODispatcher ioDispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
}
