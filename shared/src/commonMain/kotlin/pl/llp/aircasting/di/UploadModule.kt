package pl.llp.aircasting.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.dsl.module
import pl.llp.aircasting.data.network.upload.MobileMeasurementUploader
import pl.llp.aircasting.data.network.upload.MobileSessionUpload
import pl.llp.aircasting.data.network.upload.MobileUploadLoop
import pl.llp.aircasting.data.network.upload.SessionUploader
import pl.llp.aircasting.domain.session.SessionRepository
import pl.llp.aircasting.domain.session.UploadOutbox

val uploadModule = module {
  single { MobileMeasurementUploader(get(), get(), get(), get()) }
  single<SessionUploader> { MobileSessionUpload(get(), get(), get(), get(), get()) }
  single {
    MobileUploadLoop(
      sessionsWithPending = get<UploadOutbox>().observeSessionsWithPending(),
      sessionsAwaitingFinish = get<SessionRepository>().observeAwaitingFinish(),
      uploader = get(),
    )
  }
}
