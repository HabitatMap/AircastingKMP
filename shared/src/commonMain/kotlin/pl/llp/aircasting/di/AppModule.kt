package pl.llp.aircasting.di

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pl.llp.aircasting.auth.AuthRepository
import pl.llp.aircasting.auth.AuthViewModel
import pl.llp.aircasting.auth.ForgotPasswordViewModel
import pl.llp.aircasting.auth.NetworkAuthRepository
import pl.llp.aircasting.bluetooth.AirBeamLink
import pl.llp.aircasting.bluetooth.DefaultAirBeamLink
import pl.llp.aircasting.bluetooth.transport.ble.BleAirBeamConnector
import pl.llp.aircasting.data.auth.AuthSession
import pl.llp.aircasting.data.auth.StoredAuthSession
import pl.llp.aircasting.data.network.AccountApi
import pl.llp.aircasting.data.network.AppConfigApi
import pl.llp.aircasting.data.network.AuthApi
import pl.llp.aircasting.data.network.DefaultBackendUrl
import pl.llp.aircasting.data.network.ExploreMapApi
import pl.llp.aircasting.data.network.ExportApi
import pl.llp.aircasting.data.network.FixedStationsApi
import pl.llp.aircasting.data.network.FixedStationsRepository
import pl.llp.aircasting.data.network.HttpServerProbe
import pl.llp.aircasting.data.network.MobileSessionsApi
import pl.llp.aircasting.data.network.ServerProbe
import pl.llp.aircasting.data.network.ThresholdAlertsApi
import pl.llp.aircasting.data.network.ThresholdSetsApi
import pl.llp.aircasting.data.network.createAircastingHttpClient
import pl.llp.aircasting.data.network.upload.MobileUploadLoop
import pl.llp.aircasting.domain.ThresholdDefaultsSync
import pl.llp.aircasting.explore.ExploreMapRepository
import pl.llp.aircasting.explore.ExploreViewModel
import pl.llp.aircasting.explore.NetworkExploreMapRepository
import pl.llp.aircasting.explore.NetworkStationDetailsRepository
import pl.llp.aircasting.explore.SampleStationMeasurementsRepository
import pl.llp.aircasting.explore.StationDetailsRepository
import pl.llp.aircasting.explore.StationDetailsViewModel
import pl.llp.aircasting.explore.StationGraphViewModel
import pl.llp.aircasting.explore.StationMeasurementsRepository
import pl.llp.aircasting.home.FakeHomeRepository
import pl.llp.aircasting.home.HomeRepository
import pl.llp.aircasting.home.HomeViewModel
import pl.llp.aircasting.onboarding.OnboardingRepository
import pl.llp.aircasting.onboarding.StoredOnboardingRepository
import pl.llp.aircasting.record.NewSessionViewModel
import pl.llp.aircasting.record.RecordingViewModel
import pl.llp.aircasting.recording.RecordingEngine
import pl.llp.aircasting.scan.ScanViewModel
import pl.llp.aircasting.settings.account.AccountRepository
import pl.llp.aircasting.settings.account.AccountViewModel
import pl.llp.aircasting.settings.account.NetworkAccountRepository
import pl.llp.aircasting.settings.app.AppSettingsRepository
import pl.llp.aircasting.settings.app.AppSettingsViewModel
import pl.llp.aircasting.settings.app.StoredAppSettingsRepository
import pl.llp.aircasting.settings.help.HelpRepository
import pl.llp.aircasting.settings.help.HelpViewModel
import pl.llp.aircasting.settings.help.NetworkHelpRepository
import pl.llp.aircasting.settings.server.CustomDataServerViewModel
import kotlin.time.Clock

val recordModule = module {
  single {
    RecordingEngine(
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      get(),
      TimeZone.currentSystemDefault()
    )
  }
  viewModelOf(::NewSessionViewModel)
  viewModelOf(::RecordingViewModel)
}

val onboardingModule = module {
  single<OnboardingRepository> { StoredOnboardingRepository(get()) }
}

val bleModule = module {
  single { BleAirBeamConnector() }
  // Process-lifetime scope: the link outlives every screen, like the upload loop.
  single<AirBeamLink> { DefaultAirBeamLink(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
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
  single<ServerProbe> { HttpServerProbe(platformHttpEngine()) }
  // Modern v3 APIs
  single { MobileSessionsApi(get()) }
  // Search & Regional Exploration (Home screen)
  single { FixedStationsApi(get()) }
  single { FixedStationsRepository(get(), get()) }
  // Alerts & Export
  single { ThresholdAlertsApi(get()) }
  // Thresholds: server defaults, refreshed on app start
  single { ThresholdSetsApi(get()) }
  single { ThresholdDefaultsSync(get(), get()) }
  single { ExportApi(get()) }
  single<Clock> { Clock.System }
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

val exploreModule = module {
  single { ExploreMapApi(get()) }
  single<ExploreMapRepository> { NetworkExploreMapRepository(get()) }
  viewModelOf(::ExploreViewModel)
  single<StationDetailsRepository> { NetworkStationDetailsRepository(get()) }
  viewModel { (uuid: String) -> StationDetailsViewModel(uuid, get(), get()) }
  viewModel { (uuid: String) -> StationDetailsViewModel(uuid, get(), get()) }
  // TODO: network implementation once the endpoint shape is settled.
  single<StationMeasurementsRepository> { SampleStationMeasurementsRepository(get()) }
  viewModel { (streamId: Long) -> StationGraphViewModel(streamId, get()) }
}

val helpModule = module {
  single { AppConfigApi(get()) }
  single<HelpRepository> { NetworkHelpRepository(get()) }
  viewModelOf(::HelpViewModel)
}

fun initKoin(extra: KoinAppDeclaration = {}) = startKoin {
  extra()
  modules(
    platformModule(),
    databaseModule,
    bleModule,
    networkModule,
    accountModule,
    appSettingsModule,
    onboardingModule,
    recordModule,
    uploadModule,
    exploreModule,
    helpModule,
  )
}.also {
  startUploadLoop(it.koin)
  startRecordingEngine(it.koin)
  refreshThresholdDefaults(it.koin)
}

private fun startRecordingEngine(koin: Koin) {
  val engine = koin.get<RecordingEngine>()
  CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { engine.run() }
}


/** Runs for the life of the process; nothing cancels it. */
private fun startUploadLoop(koin: Koin) {
  val loop = koin.get<MobileUploadLoop>()
  CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { loop.run() }
}

/** One-shot, fire-and-forget. On failure the stored defaults (first launch: the bundled seed) stay. */
private fun refreshThresholdDefaults(koin: Koin) {
  val sync = koin.get<ThresholdDefaultsSync>()
  CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
    try {
      sync.refresh()
    } catch (e: Exception) {
      Logger.withTag("Thresholds").w(e) { "threshold_sets refresh failed; keeping stored defaults" }
    }
  }
}