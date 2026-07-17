package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.CompositeAirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import org.koin.dsl.module

actual fun platformModule() = module {
  single<AirBeamConnector> { CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>())) }
}