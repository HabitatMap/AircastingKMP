package pl.llp.aircasting.di

import org.koin.dsl.module
import pl.llp.aircasting.data.local.LocalMeasurementRepository
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.createDatabase
import pl.llp.aircasting.data.local.db.AppDatabase
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.ThresholdsRepository
import pl.llp.aircasting.domain.session.MeasurementRepository
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.SessionRepository

val databaseModule = module {
  single { createDatabase(get()) }
  single { get<AppDatabase>().sensorThresholdQueries }
  single { get<AppDatabase>().sessionQueries }
  single { get<AppDatabase>().measurementStreamQueries }
  single { get<AppDatabase>().measurementQueries }

  single<ThresholdsRepository> { LocalThresholdsRepository(get()) }
  single<SessionRepository> { LocalSessionRepository(get()) }
  single<MeasurementStreamRepository> { LocalMeasurementStreamRepository(get()) }
  single<MeasurementRepository> { LocalMeasurementRepository(get()) }
}