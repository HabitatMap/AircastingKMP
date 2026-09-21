package pl.llp.aircasting.di

import org.koin.dsl.module
import pl.llp.aircasting.data.local.createDatabase
import pl.llp.aircasting.data.local.db.AppDatabase
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.ThresholdsRepository

val databaseModule = module {
  single { createDatabase(get()) }
  single { get<AppDatabase>().sensorThresholdQueries }
  single<ThresholdsRepository> { LocalThresholdsRepository(get()) }
}