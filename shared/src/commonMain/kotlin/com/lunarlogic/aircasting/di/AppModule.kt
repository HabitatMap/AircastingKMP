package com.lunarlogic.aircasting.di

import com.lunarlogic.aircasting.bluetooth.AirBeamCredentials
import com.lunarlogic.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import com.lunarlogic.aircasting.data.network.FixedStationsApi
import com.lunarlogic.aircasting.data.network.FixedStationsRepository
import com.lunarlogic.aircasting.data.network.createAircastingHttpClient
import com.lunarlogic.aircasting.ui.scan.ScanViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

val bleModule = module {
  single<AirBeamCredentials> { StubAirBeamCredentials }
  single { BleAirBeamConnector(get()) }
  viewModelOf(::ScanViewModel)
}

val networkModule = module {
  single { createAircastingHttpClient(platformHttpEngine()) }
  single { FixedStationsApi(get()) }
  single<Clock> { Clock.System }
  single { FixedStationsRepository(get(), get()) }
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule, networkModule)
}