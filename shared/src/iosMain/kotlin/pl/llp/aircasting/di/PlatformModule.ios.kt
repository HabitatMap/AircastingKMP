package pl.llp.aircasting.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
import pl.llp.aircasting.bluetooth.BluetoothRadio
import pl.llp.aircasting.bluetooth.IosBluetoothRadio
import pl.llp.aircasting.data.local.DatabaseDriverFactory
import pl.llp.aircasting.data.local.IosDatabaseDriverFactory
import pl.llp.aircasting.data.network.upload.IosUploadBackstop
import pl.llp.aircasting.data.network.upload.UploadBackstop
import pl.llp.aircasting.recording.BackgroundSessionHost
import pl.llp.aircasting.recording.PhoneLocation
import pl.llp.aircasting.recording.TrackedPhoneLocation
import pl.llp.aircasting.recording.iosLocationFixes
import pl.llp.aircasting.settings.mic.IosMicrophoneLevelSource
import pl.llp.aircasting.settings.mic.MicrophoneLevelSource
import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

actual fun platformModule() = module {
  single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
  single<MicrophoneLevelSource> { IosMicrophoneLevelSource() }
  single<AirBeamConnector> { CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>())) }
  single<BluetoothRadio> { IosBluetoothRadio() }
  single<LocationProvider> { IosLocationProvider() }
  single<DatabaseDriverFactory> { IosDatabaseDriverFactory() }
  single<PhoneLocation> { TrackedPhoneLocation(iosLocationFixes(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
  // bluetooth-central + location background modes keep the process alive; nothing to start.
  single<BackgroundSessionHost> {
    object : BackgroundSessionHost {
      override fun start() {}
      override fun stop() {}
    }
  }
  single<UploadBackstop> { IosUploadBackstop(get()) }
  single {
    AppVersion(
      NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "",
    )
  }
}

actual fun platformHttpEngine(): HttpClientEngine {
  return Darwin.create()
}