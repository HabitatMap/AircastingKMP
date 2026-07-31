package pl.llp.aircasting.di

import pl.llp.aircasting.bluetooth.AirBeamCredentials
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.data.network.FixedStationsApi
import pl.llp.aircasting.data.network.FixedStationsRepository
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.home.FakeHomeRepository
import pl.llp.aircasting.home.HomeRepository
import pl.llp.aircasting.home.HomeViewModel
import pl.llp.aircasting.home.NetworkHomeRepository
import pl.llp.aircasting.scan.ScanViewModel
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
//  single<HomeRepository> { NetworkHomeRepository(get()) } TODO: restore
  single<HomeRepository> { FakeHomeRepository() }
  viewModelOf(::HomeViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule, networkModule)
}