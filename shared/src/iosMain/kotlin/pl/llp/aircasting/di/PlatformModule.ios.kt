package pl.llp.aircasting.di

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.transport.CompositeAirBeamConnector
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.home.IosLocationProvider
import pl.llp.aircasting.home.LocationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.dsl.module
import pl.llp.aircasting.AppVersion
import pl.llp.aircasting.settings.mic.IosMicrophoneLevelSource
import pl.llp.aircasting.settings.mic.MicrophoneLevelSource
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
  single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
  single<MicrophoneLevelSource> { IosMicrophoneLevelSource() }
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