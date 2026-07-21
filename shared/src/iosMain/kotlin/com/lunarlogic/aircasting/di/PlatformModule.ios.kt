package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.CompositeAirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module

actual fun platformModule() = module {
  single<AirBeamConnector> { CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>())) }
}

actual fun platformHttpEngine(): HttpClientEngine {
  return Darwin.create()
}