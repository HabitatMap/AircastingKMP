package pl.llp.aircasting.data.network.upload

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// Red stub: lets MobileUploadLoopTest compile. Replaced by the green step.
class MobileUploadLoop(
  private val sessionsWithPending: Flow<List<String>>,
  private val sessionsAwaitingFinish: Flow<List<String>>,
  private val uploader: SessionUploader,
  private val flushInterval: Duration = 60.seconds,
  private val initialBackoff: Duration = 30.seconds,
  private val maxBackoff: Duration = 10.minutes,
) {
  suspend fun run(): Nothing = TODO()
  fun flushNow(): Unit = TODO()
}
