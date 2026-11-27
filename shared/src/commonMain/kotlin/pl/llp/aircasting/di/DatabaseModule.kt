package pl.llp.aircasting.di

import org.koin.dsl.module
import pl.llp.aircasting.active.ActiveSessionsRepository
import pl.llp.aircasting.active.SessionDetailsRepository
import pl.llp.aircasting.data.local.LocalActiveSessionsRepository
import pl.llp.aircasting.data.local.LocalMeasurementRepository
import pl.llp.aircasting.data.local.LocalNoteOutbox
import pl.llp.aircasting.data.local.LocalNoteRepository
import pl.llp.aircasting.data.local.LocalMeasurementStreamRepository
import pl.llp.aircasting.data.local.LocalPastSessionsRepository
import pl.llp.aircasting.data.local.LocalSessionDetailsRepository
import pl.llp.aircasting.data.local.LocalSessionRepository
import pl.llp.aircasting.data.local.LocalUploadOutbox
import pl.llp.aircasting.data.local.createDatabase
import pl.llp.aircasting.data.local.db.AppDatabase
import pl.llp.aircasting.domain.LocalThresholdsRepository
import pl.llp.aircasting.domain.ThresholdsRepository
import pl.llp.aircasting.domain.session.MeasurementRepository
import pl.llp.aircasting.domain.session.MeasurementStreamRepository
import pl.llp.aircasting.domain.session.NoteOutbox
import pl.llp.aircasting.domain.session.NoteRepository
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.UploadOutbox
import pl.llp.aircasting.mydata.PastSessionsRepository

val databaseModule = module {
  single { createDatabase(get()) }
  single { get<AppDatabase>().pendingUploadQueries }
  single { get<AppDatabase>().sensorThresholdQueries }
  single { get<AppDatabase>().sessionQueries }
  single { get<AppDatabase>().measurementStreamQueries }
  single { get<AppDatabase>().measurementQueries }
  single { get<AppDatabase>().fixedSessionQueries }
  single { get<AppDatabase>().noteQueries }

  single<ThresholdsRepository> { LocalThresholdsRepository(get()) }
  single<SessionRepository> { LocalSessionRepository(get()) }
  single<ActiveSessionsRepository> { LocalActiveSessionsRepository(get()) }
  single<MeasurementStreamRepository> { LocalMeasurementStreamRepository(get()) }
  single<MeasurementRepository> { LocalMeasurementRepository(get(), get()) }
  single<UploadOutbox> { LocalUploadOutbox(get()) }
  single<ActiveSessionsRepository> { LocalActiveSessionsRepository(get()) }
  single<SessionDetailsRepository> { LocalSessionDetailsRepository(get()) }
  single<PastSessionsRepository> { LocalPastSessionsRepository(get()) }
  single<NoteRepository> { LocalNoteRepository(get()) }
  single<NoteOutbox> { LocalNoteOutbox(get()) }
}