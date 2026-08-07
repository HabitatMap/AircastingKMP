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
import pl.llp.aircasting.data.auth.AuthTokenStore
import pl.llp.aircasting.data.auth.InMemoryAuthTokenStore
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.HttpServerProbe
import pl.llp.aircasting.data.network.ServerProbe
import pl.llp.aircasting.settings.account.AccountRepository
import pl.llp.aircasting.settings.account.AccountViewModel
import pl.llp.aircasting.settings.account.NetworkAccountRepository
import pl.llp.aircasting.settings.app.AppSettingsRepository
import pl.llp.aircasting.settings.app.AppSettingsViewModel
import pl.llp.aircasting.settings.app.StoredAppSettingsRepository
import pl.llp.aircasting.settings.server.CustomDataServerViewModel
import pl.llp.aircasting.data.network.DefaultBackendUrl
import kotlin.time.Clock

val bleModule = module {
  single<AirBeamCredentials> { StubAirBeamCredentials }
  single { BleAirBeamConnector(get()) }
  viewModelOf(::ScanViewModel)
}

val appSettingsModule = module {
  single<AppSettingsRepository> { StoredAppSettingsRepository(get()) }
  viewModelOf(::AppSettingsViewModel)
  viewModelOf(::CustomDataServerViewModel)
}

val networkModule = module {
  single {
    val settings = get<AppSettingsRepository>()
    createAircastingHttpClient(platformHttpEngine()) {
      settings.preferences.value.dataServerUrl ?: DefaultBackendUrl
    }
  }
  single { HttpServerProbe(platformHttpEngine()) as ServerProbe }
  single { createAircastingHttpClient(platformHttpEngine()) }
  single { FixedStationsApi(get()) }
  single<Clock> { Clock.System }
  single { FixedStationsRepository(get(), get()) }
//  single<HomeRepository> { NetworkHomeRepository(get()) } TODO: restore
  single<HomeRepository> { FakeHomeRepository() }
  viewModelOf(::HomeViewModel)
}

val accountModule = module {
  // TODO(login): swap for a persistent store once a login screen writes a real token.
  single<AuthTokenStore> { InMemoryAuthTokenStore() }
  single { AccountApi(get()) }
  single<AccountRepository> { NetworkAccountRepository(get(), get()) }
  viewModelOf(::AccountViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(platformModule(), bleModule, networkModule, accountModule, appSettingsModule)
}