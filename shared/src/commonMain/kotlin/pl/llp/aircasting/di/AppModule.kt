package pl.llp.aircasting.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pl.llp.aircasting.auth.AuthRepository
import pl.llp.aircasting.auth.AuthViewModel
import pl.llp.aircasting.auth.ForgotPasswordViewModel
import pl.llp.aircasting.auth.NetworkAuthRepository
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.AuthApi
import pl.llp.aircasting.data.network.DefaultBackendUrl
import pl.llp.aircasting.data.network.FixedStationsApi
import pl.llp.aircasting.data.network.FixedStationsRepository
import pl.llp.aircasting.data.network.HttpServerProbe
import pl.llp.aircasting.data.network.ServerProbe
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.home.FakeHomeRepository
import pl.llp.aircasting.home.HomeRepository
import pl.llp.aircasting.home.HomeViewModel
import pl.llp.aircasting.onboarding.OnboardingRepository
import pl.llp.aircasting.onboarding.StoredOnboardingRepository
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.scan.ScanViewModel
import pl.llp.aircasting.settings.account.AccountRepository
import pl.llp.aircasting.settings.account.AccountViewModel
import pl.llp.aircasting.settings.account.NetworkAccountRepository
import pl.llp.aircasting.settings.app.AppSettingsRepository
import pl.llp.aircasting.settings.app.AppSettingsViewModel
import pl.llp.aircasting.settings.app.StoredAppSettingsRepository
import pl.llp.aircasting.settings.server.CustomDataServerViewModel
import kotlin.time.Clock

val recordModule = module {
  viewModelOf(::NewSessionViewModel)
}

val onboardingModule = module {
  single<OnboardingRepository> { StoredOnboardingRepository(get()) }
}

val bleModule = module {
  single { BleAirBeamConnector() }
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
  single<AuthSession> { StoredAuthSession(get()) }
  single { AccountApi(get()) }
  single { AuthApi(get()) }
  single<AuthRepository> { NetworkAuthRepository(get(), get()) }
  viewModelOf(::AuthViewModel)
  single<AccountRepository> { NetworkAccountRepository(get(), get()) }
  viewModelOf(::AccountViewModel)
  viewModelOf(::ForgotPasswordViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(
    platformModule(),
    bleModule,
    networkModule,
    accountModule,
    appSettingsModule,
    onboardingModule,
    recordModule,
  )
}