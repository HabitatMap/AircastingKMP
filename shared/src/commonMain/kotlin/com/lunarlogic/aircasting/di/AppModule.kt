package com.lunarlogic.aircasting.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val bleModule = module {
//  single<AirBeamConnector> { KableAirBeamConnector(get()) }
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule)
}