package pl.llp.aircasting.di

import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.transport.CompositeAirBeamConnector
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.home.IosLocationProvider
import pl.llp.aircasting.home.LocationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module
import pl.llp.aircasting.AppVersion
import platform.Foundation.NSBundle

actual fun platformModule() = module {
  single<AirBeamConnector> { CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>())) }
  single<LocationProvider> { IosLocationProvider() }
  single {
    AppVersion(
      NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "",
    )
  }
}

actual fun platformHttpEngine(): HttpClientEngine {
  return Darwin.create()
}