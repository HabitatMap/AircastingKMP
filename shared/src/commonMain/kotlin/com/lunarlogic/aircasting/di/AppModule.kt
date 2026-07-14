package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import com.lunarlogic.aircasting.ui.scan.ScanViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val bleModule = module {
  single { BleAirBeamConnector() }
  viewModelOf(::ScanViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule)
}