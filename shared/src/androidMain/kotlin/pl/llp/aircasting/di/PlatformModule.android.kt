package pl.llp.aircasting.di

import android.bluetooth.BluetoothManager
import android.content.Context
import pl.llp.aircasting.bluetooth.AirBeamConnector
import pl.llp.aircasting.bluetooth.transport.CompositeAirBeamConnector
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.bluetooth.transport.classic.ClassicAirBeamConnector
import pl.llp.aircasting.home.AndroidLocationProvider
import pl.llp.aircasting.home.LocationProvider
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import pl.llp.aircasting.AppVersion

actual fun platformModule() = module {
  single<AirBeamConnector> {
    CompositeAirBeamConnector(listOf(get<BleAirBeamConnector>(), get<ClassicAirBeamConnector>()))
  }
  single { ClassicAirBeamConnector(get(), get<BluetoothManager>().adapter, get()) }
  single { get<Context>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
  single<LocationProvider> { AndroidLocationProvider(get()) }
  single {
    val ctx = get<Context>()
    @Suppress("DEPRECATION") // PackageInfoFlags overload needs API 33; minSdk here is 28
    AppVersion(
      ctx.packageManager.getPackageInfo(
        ctx.packageName,
        0
      ).versionName.orEmpty()
    )
  }
}

actual fun platformHttpEngine(): HttpClientEngine {
  return OkHttp.create()
}