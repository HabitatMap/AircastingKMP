package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val bleModule = module {
  single<AirBeamConnector> { BleAirBeamConnector() }
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule)
}