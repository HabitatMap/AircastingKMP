package com.lunarlogic.aircasting.di

import android.bluetooth.BluetoothManager
import android.content.Context
import com.lunarlogic.aircasting.bluetooth.AirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.CompositeAirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import com.lunarlogic.aircasting.bluetooth.transport.classic.ClassicAirBeamConnector
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

actual fun platformModule() = module {
  single<AirBeamConnector> {
    CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>(), get<ClassicAirBeamConnector>()))
  }
  single { ClassicAirBeamConnector(get(), get<BluetoothManager>().adapter, get()) }
  single { get<Context>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
}

actual fun platformHttpEngine(): HttpClientEngine {
  return OkHttp.create()
}